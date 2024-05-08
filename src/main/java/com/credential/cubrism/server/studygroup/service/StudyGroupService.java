package com.credential.cubrism.server.studygroup.service;

import com.credential.cubrism.server.authentication.entity.Users;
import com.credential.cubrism.server.authentication.utils.SecurityUtil;
import com.credential.cubrism.server.common.dto.MessageDto;
import com.credential.cubrism.server.common.exception.CustomException;
import com.credential.cubrism.server.common.exception.ErrorCode;
import com.credential.cubrism.server.notification.entity.FcmTokens;
import com.credential.cubrism.server.notification.repository.FcmRepository;
import com.credential.cubrism.server.notification.utils.FcmUtils;
import com.credential.cubrism.server.studygroup.dto.*;
import com.credential.cubrism.server.studygroup.entity.*;
import com.credential.cubrism.server.studygroup.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyGroupService {
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMembersRepository groupMembersRepository;
    private final PendingMembersRepository pendingMembersRepository;
    private final StudyGroupGoalRepository studyGroupGoalRepository;
    private final UserGoalRepository userGoalRepository;
    private final FcmRepository fcmRepository;

    private final SecurityUtil securityUtil;
    private final FcmUtils fcmUtils;

    // 스터디 그룹 생성
    @Transactional
    public ResponseEntity<MessageDto> createStudyGroup(StudyGroupCreateDto dto) {
        Users currentUser = securityUtil.getCurrentUser();

        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setGroupName(dto.getGroupName());
        studyGroup.setGroupDescription(dto.getGroupDescription());
        studyGroup.setMaxMembers(dto.getMaxMembers());

        List<GroupTags> groupTagsList = dto.getTags().stream()
                .map(tag -> {
                    GroupTags groupTags = new GroupTags();
                    groupTags.setStudyGroup(studyGroup);
                    groupTags.setTagName(tag);
                    return groupTags;
                }).toList();
        studyGroup.setGroupTags(groupTagsList);

        GroupMembers groupMembers = new GroupMembers();
        groupMembers.setUser(currentUser);
        groupMembers.setStudyGroup(studyGroup);
        groupMembers.setAdmin(true);
        studyGroup.setGroupMembers(List.of(groupMembers));

        studyGroupRepository.save(studyGroup);

        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageDto("스터디 그룹을 생성했습니다."));
    }

    // 스터디 그룹 삭제
    @Transactional
    public ResponseEntity<MessageDto> deleteStudyGroup(Long groupId) {
        Users currentUser = securityUtil.getCurrentUser();

        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        GroupMembers groupMembers = groupMembersRepository.findByUserAndStudyGroup(currentUser, studyGroup)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_MEMBER));

        if (!groupMembers.isAdmin()) {
            throw new CustomException(ErrorCode.STUDY_GROUP_NOT_ADMIN);
        }

        // 해당 스터디 그룹에 속한 GroupMembers를 모두 찾아서 삭제
        List<GroupMembers> members = groupMembersRepository.findByStudyGroup(studyGroup);
        groupMembersRepository.deleteAll(members);

        // 스터디 그룹과 관련된 UserGoal을 찾아 삭제
        List<UserGoal> userGoals = userGoalRepository.findByStudyGroup(studyGroup);
        userGoalRepository.deleteAll(userGoals);

        // 스터디 그룹과 관련된 StudyGroupGoal을 찾아 삭제
        List<StudyGroupGoal> studyGroupGoals = studyGroupGoalRepository.findByStudyGroup(studyGroup);
        studyGroupGoalRepository.deleteAll(studyGroupGoals);

        studyGroupRepository.delete(studyGroup);

        return ResponseEntity.status(HttpStatus.OK).body(new MessageDto("스터디 그룹을 삭제했습니다."));
    }

    // 스터디 그룹 목록
    public ResponseEntity<StudyGroupListDto> studyGroupList(Pageable pageable, boolean recruiting) {
        Page<StudyGroup> studyGroup = studyGroupRepository.findAll(pageable);

        StudyGroupListDto studyGroupListDto = new StudyGroupListDto(
                new StudyGroupListDto.Pageable(
                        studyGroup.hasPrevious() ? pageable.getPageNumber() - 1 : null,
                        pageable.getPageNumber(),
                        studyGroup.hasNext() ? pageable.getPageNumber() + 1 : null
                ),
                studyGroup.stream()
                        .filter(group -> !recruiting || group.getGroupMembers().size() < group.getMaxMembers())
                        .map(group -> {
                            boolean isRecruiting = group.getGroupMembers().size() < group.getMaxMembers(); // 모집중 여부
                            return new StudyGroupListDto.StudyGroupList(
                                    group.getGroupId(),
                                    group.getGroupName(),
                                    group.getGroupDescription(),
                                    group.getGroupMembers().size(),
                                    group.getMaxMembers(),
                                    isRecruiting,
                                    group.getGroupTags().stream()
                                            .map(GroupTags::getTagName)
                                            .toList()
                            );
                        }).toList());

        return ResponseEntity.status(HttpStatus.OK).body(studyGroupListDto);
    }

    // 내가 가입한 스터디 그룹 목록
    public ResponseEntity<List<StudyGroupListDto.StudyGroupList>> myStudyGroupList() {
        Users currentUser = securityUtil.getCurrentUser();

        List<StudyGroupListDto.StudyGroupList> studyGroupList = studyGroupRepository.findAllByUserId(currentUser.getUuid())
                .stream()
                .map(group -> {
                    boolean isRecruiting = group.getGroupMembers().size() < group.getMaxMembers();
                    return new StudyGroupListDto.StudyGroupList(
                            group.getGroupId(),
                            group.getGroupName(),
                            group.getGroupDescription(),
                            group.getGroupMembers().size(),
                            group.getMaxMembers(),
                            isRecruiting,
                            group.getGroupTags().stream()
                                    .map(GroupTags::getTagName)
                                    .toList()
                    );
                })
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(studyGroupList);
    }

    // 스터디 그룹 정보
    public ResponseEntity<StudyGroupInfoDto> studyGroupInfo(Long groupId) {
        return studyGroupRepository.findById(groupId)
                .map(group -> {
                    boolean isRecruiting = group.getGroupMembers().size() < group.getMaxMembers();
                    return ResponseEntity.ok().body(new StudyGroupInfoDto(
                            group.getGroupId(),
                            group.getGroupName(),
                            group.getGroupDescription(),
                            group.getGroupMembers().stream()
                                    .filter(GroupMembers::isAdmin)
                                    .map(groupMembers -> groupMembers.getUser().getNickname())
                                    .findFirst()
                                    .orElse(null),
                            group.getGroupMembers().stream()
                                    .filter(GroupMembers::isAdmin)
                                    .map(groupMembers -> Optional.ofNullable(groupMembers.getUser().getImageUrl()))
                                    .findFirst()
                                    .orElse(Optional.empty())
                                    .orElse(null),
                            group.getGroupMembers().size(),
                            group.getMaxMembers(),
                            group.getGroupTags().stream()
                                    .map(GroupTags::getTagName)
                                    .toList(),
                            isRecruiting,
                            group.getGroupMembers().stream()
                                    .map(groupMembers -> groupMembers.getUser().getEmail())
                                    .toList()
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // 스터디 그룹 가입 신청
    @Transactional
    public ResponseEntity<MessageDto> requestJoin(Long groupId) {
        Users currentUser = securityUtil.getCurrentUser();

        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        if (studyGroup.getGroupMembers().stream().anyMatch(groupMembers -> groupMembers.getUser().getUuid().equals(currentUser.getUuid()))) {
            throw new CustomException(ErrorCode.STUDY_GROUP_ALREADY_JOINED);
        }

        if(pendingMembersRepository.findByUserAndStudyGroup(currentUser, studyGroup).isPresent()) {
            throw new CustomException(ErrorCode.STUDY_GROUP_ALREADY_REQUESTED);
        }

        PendingMembers pendingMembers = new PendingMembers();
        pendingMembers.setUser(currentUser);
        pendingMembers.setStudyGroup(studyGroup);
        pendingMembersRepository.save(pendingMembers);

        // 스터디 그룹 관리자에게 알림 전송
        GroupMembers admin = studyGroup.getGroupMembers().stream()
                .filter(GroupMembers::isAdmin)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_ADMIN));

        String fcmToken = fcmRepository.findByUserId(admin.getUser().getUuid())
                .map(FcmTokens::getToken)
                .orElse(null);

        // FCM 토큰이 존재하는 경우 알림 전송
        if (fcmToken != null) {
            // 알림 메시지
            String title = "스터디 그룹 가입 요청이 있습니다.";
            String body = currentUser.getNickname() + "님이 가입을 요청했습니다.";
            String type = "STUDY|" + studyGroup.getGroupId();

            // 알림 전송
            fcmUtils.sendMessageTo(fcmToken, title, body, type);
        }

        return ResponseEntity.status(HttpStatus.OK).body(new MessageDto("스터디 그룹 가입을 요청했습니다."));
    }

    // 스터디 그룹 가입 신청 취소
    @Transactional
    public ResponseEntity<MessageDto> cancelJoinRequest(UUID memberId) {
        Users currentUser = securityUtil.getCurrentUser();

        PendingMembers pendingMember = pendingMembersRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PENDING_MEMBER_NOT_FOUND));

        if (!pendingMember.getUser().getUuid().equals(currentUser.getUuid())) {
            throw new CustomException(ErrorCode.PENDING_MEMBER_NOT_MATCH);
        }

        pendingMembersRepository.delete(pendingMember);

        return ResponseEntity.status(HttpStatus.OK).body(new MessageDto("스터디 그룹 가입 신청을 취소했습니다."));
    }

    // 가입 신청한 스터디 그룹 목록
    public ResponseEntity<List<StudyGroupJoinListDto>> joinRequestList() {
        Users currentUser = securityUtil.getCurrentUser();

        List<StudyGroupJoinListDto> joinList = pendingMembersRepository.findByUserOrderByRequestDateDesc(currentUser).stream()
                .map(pendingMembers -> new StudyGroupJoinListDto(
                        pendingMembers.getMemberId(),
                        pendingMembers.getStudyGroup().getGroupName(),
                        pendingMembers.getStudyGroup().getGroupDescription(),
                        pendingMembers.getStudyGroup().getGroupTags().stream()
                                .map(GroupTags::getTagName)
                                .toList(),
                        pendingMembers.getStudyGroup().getGroupMembers().stream()
                                .filter(GroupMembers::isAdmin)
                                .map(groupMembers -> groupMembers.getUser().getNickname())
                                .findFirst()
                                .orElse(null),
                        pendingMembers.getStudyGroup().getGroupMembers().stream()
                                .filter(GroupMembers::isAdmin)
                                .map(groupMembers -> Optional.ofNullable(groupMembers.getUser().getImageUrl()))
                                .findFirst()
                                .orElse(Optional.empty())
                                .orElse(null),
                        pendingMembers.getRequestDate().toString()
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(joinList);
    }

    // 스터디 그룹 가입 승인
    @Transactional
    public ResponseEntity<MessageDto> approveJoinRequest(UUID memberId) {
        Users currentUser = securityUtil.getCurrentUser();

        PendingMembers pendingMember = pendingMembersRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PENDING_MEMBER_NOT_FOUND));

        groupMembersRepository.findByUserAndStudyGroupAndAdmin(currentUser, pendingMember.getStudyGroup(), true)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_ADMIN));

        GroupMembers newMember = new GroupMembers();
        newMember.setUser(pendingMember.getUser());
        newMember.setStudyGroup(pendingMember.getStudyGroup());
        newMember.setAdmin(false);

        groupMembersRepository.save(newMember);
        pendingMembersRepository.delete(pendingMember);

        // 새로 가입한 멤버에 대해서만 UserGoal 생성
        UserGoal userGoal = new UserGoal();
        userGoal.setUser(newMember.getUser());
        userGoal.setStudyGroup(newMember.getStudyGroup());

        // StudyGroup의 StudyGroupGoal 목록을 가져와 UserGoal의 uncompletedGoals에 추가
        List<StudyGroupGoal> studyGroupGoals = newMember.getStudyGroup().getStudyGroupGoals();
        userGoal.getUncompletedGoals().addAll(studyGroupGoals);

        userGoalRepository.save(userGoal);

        // 가입 요청을 한 유저에게 알림 전송
        String fcmToken = fcmRepository.findByUserId(newMember.getUser().getUuid())
                .map(FcmTokens::getToken)
                .orElse(null);

        // FCM 토큰이 존재하는 경우 알림 전송
        if (fcmToken != null) {
            // 알림 메시지
            String title = "스터디 그룹 가입 승인";
            String body = "'" + newMember.getStudyGroup().getGroupName() + " 스터디 그룹 가입이 승인되었습니다.";
            String type = "STUDY|" + newMember.getStudyGroup().getGroupId();

            // 알림 전송
            fcmUtils.sendMessageTo(fcmToken, title, body, type);
        }

        return ResponseEntity.status(HttpStatus.OK).body(new MessageDto("스터디 그룹 가입을 승인했습니다."));
    }

    // 스터디 그룹 가입 거절
    @Transactional
    public ResponseEntity<MessageDto> denyJoinRequest(UUID memberId) {
        Users currentUser = securityUtil.getCurrentUser();

        PendingMembers pendingMember = pendingMembersRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PENDING_MEMBER_NOT_FOUND));

        groupMembersRepository.findByUserAndStudyGroupAndAdmin(currentUser, pendingMember.getStudyGroup(), true)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_ADMIN));

        pendingMembersRepository.delete(pendingMember);

        // 가입 요청을 한 유저에게 알림 전송
        String fcmToken = fcmRepository.findByUserId(pendingMember.getUser().getUuid())
                .map(FcmTokens::getToken)
                .orElse(null);

        // FCM 토큰이 존재하는 경우 알림 전송
        if (fcmToken != null) {
            // 알림 메시지
            String title = "스터디 그룹 가입 거절";
            String body = "'" + pendingMember.getStudyGroup().getGroupName() + " 스터디 그룹 가입이 거절되었습니다.";

            // 알림 전송
            fcmUtils.sendMessageTo(fcmToken, title, body, null);
        }

        return ResponseEntity.status(HttpStatus.OK).body(new MessageDto("스터디 그룹 가입을 거절했습니다."));
    }

    // 해당 스터디 그룹 가입 신청 목록
    public ResponseEntity<List<StudyGroupJoinRequestDto>> joinRequestList(Long groupId) {
        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        Users currentUser = securityUtil.getCurrentUser();

        groupMembersRepository.findByUserAndStudyGroupAndAdmin(currentUser, studyGroup, true)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_ADMIN));

        List<StudyGroupJoinRequestDto> joinList = pendingMembersRepository.findByStudyGroup(studyGroup).stream()
                .map(pendingMembers -> new StudyGroupJoinRequestDto(
                        pendingMembers.getMemberId(),
                        pendingMembers.getStudyGroup().getGroupName(),
                        pendingMembers.getUser().getNickname(),
                        pendingMembers.getUser().getImageUrl(),
                        pendingMembers.getRequestDate()
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(joinList);
    }

    // 스터디 그룹 목표 추가
    @Transactional
    public ResponseEntity<MessageDto> addStudyGroupGoal(StudyGroupAddGoalDto dto) {
        Long groupId = dto.getStudyGroupId();
        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        StudyGroupGoal goal = new StudyGroupGoal();
        goal.setGoalName(dto.getGoalName());
        goal.setStudyGroup(studyGroup);
        studyGroup.getStudyGroupGoals().add(goal);

        studyGroupGoalRepository.save(goal);

        // 스터디 그룹의 모든 멤버들에게 UserGoal 인스턴스를 생성하고 저장
        for (GroupMembers member : studyGroup.getGroupMembers()) {
            Optional<UserGoal> optionalUserGoal = userGoalRepository.findByUserAndStudyGroup(member.getUser(), studyGroup);
            UserGoal userGoal;
            if (optionalUserGoal.isEmpty()) {
                userGoal = new UserGoal();
                userGoal.setUser(member.getUser());
                userGoal.setStudyGroup(studyGroup);
                userGoal.getUncompletedGoals().add(goal); // 새로운 목표를 uncompletedGoals 리스트에 추가
            } else {
                userGoal = optionalUserGoal.get();
                userGoal.getUncompletedGoals().add(goal); // 이미 존재하는 UserGoal에 새로운 목표를 uncompletedGoals 리스트에 추가
            }
            userGoalRepository.save(userGoal);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageDto("스터디 그룹에 목표를 추가했습니다."));
    }

    // 스터디 그룹 목표 삭제
    @Transactional
    public ResponseEntity<MessageDto> deleteStudyGroupGoal(Long goalId) {
        StudyGroupGoal goal = studyGroupGoalRepository.findById(goalId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_GOAL_NOT_FOUND));

        List<UserGoal> userGoals = userGoalRepository.findByStudyGroup(goal.getStudyGroup());
        for (UserGoal userGoal : userGoals) {
            userGoal.getCompletedGoals().remove(goal);
            userGoal.getUncompletedGoals().remove(goal);
            userGoalRepository.save(userGoal);
        }

        studyGroupGoalRepository.delete(goal);

        return ResponseEntity.status(HttpStatus.OK).body(new MessageDto("스터디 그룹 목표를 삭제했습니다."));
    }

    // 스터디 그룹 목표 수정
    @Transactional
    public ResponseEntity<MessageDto> updateStudyGroupGoal(Long goalId, StudyGroupUpdateGoalDto dto) {
        StudyGroupGoal goal = studyGroupGoalRepository.findById(goalId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_GOAL_NOT_FOUND));

        goal.setGoalName(dto.getGoalName());

        studyGroupGoalRepository.save(goal);

        return ResponseEntity.status(HttpStatus.OK).body(new MessageDto("스터디 그룹 목표를 수정했습니다."));
    }

    public ResponseEntity<List<UserGoalStatusDto>> getUserGoals(Long groupId) {
        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        List<UserGoalStatusDto> userGoalStatusList = studyGroup.getGroupMembers().stream()
                .map(member -> {
                    UserGoal userGoal = userGoalRepository.findByUserAndStudyGroup(member.getUser(), studyGroup)
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_GOAL_NOT_FOUND));
                    double completionPercentage = Optional.of(userGoal)
                            .map(UserGoal::getStudyGroup)
                            .map(StudyGroup::getStudyGroupGoals)
                            .filter(goals -> !goals.isEmpty())
                            .map(goals -> (double) userGoal.getCompletedGoals().size() / goals.size() * 100)
                            .orElse(0.0);

                    // UserGoal의 completedGoals와 uncompletedGoals를 사용하여 StudyGroupGoalDto 목록을 생성
                    List<StudyGroupGoalDto> completedGoals = userGoal.getCompletedGoals().stream()
                            .map(goal -> new StudyGroupGoalDto(goal.getGoalId(), goal.getGoalName()))
                            .collect(Collectors.toList());
                    List<StudyGroupGoalDto> uncompletedGoals = userGoal.getUncompletedGoals().stream()
                            .map(goal -> new StudyGroupGoalDto(goal.getGoalId(), goal.getGoalName()))
                            .collect(Collectors.toList());

                    return new UserGoalStatusDto(member.getUser().getNickname(), completedGoals, uncompletedGoals, completionPercentage);
                })
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(userGoalStatusList);
    }

    @Transactional
    public ResponseEntity<MessageDto> completeStudyGroupGoal(Long goalId) {
        Users currentUser = securityUtil.getCurrentUser();
        StudyGroupGoal goal = studyGroupGoalRepository.findById(goalId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_GOAL_NOT_FOUND));

        UserGoal userGoal = userGoalRepository.findByUserAndStudyGroup(currentUser, goal.getStudyGroup())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_GOAL_NOT_FOUND));

        if (!userGoal.getCompletedGoals().contains(goal)) {
            userGoal.getCompletedGoals().add(goal);
            userGoal.getUncompletedGoals().remove(goal);
            userGoalRepository.save(userGoal);
        }

        return ResponseEntity.status(HttpStatus.OK).body(new MessageDto("목표를 완료했습니다."));
    }

    public ResponseEntity<List<StudyGroupGoalDto>> getStudyGroupGoals(Long groupId) {
        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        List<StudyGroupGoalDto> studyGroupGoalList = studyGroup.getStudyGroupGoals().stream()
                .map(goal -> new StudyGroupGoalDto(goal.getGoalId(), goal.getGoalName()))
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(studyGroupGoalList);
    }

    // 스터디 그룹 D-day 설정
    public ResponseEntity<MessageDto> setStudyGroupDDay(StudyGroupDDayDto dto) {
        Users currentUser = securityUtil.getCurrentUser();
        Long groupId = dto.getGroupId();
        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        GroupMembers groupMembers = groupMembersRepository.findByUserAndStudyGroup(currentUser, studyGroup)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_MEMBER));

        if (!groupMembers.isAdmin()) {
            throw new CustomException(ErrorCode.STUDY_GROUP_NOT_ADMIN);
        }

        StudyGroupDDay studyGroupDDay = new StudyGroupDDay();
        studyGroupDDay.setDDay(dto.getDDay());
        studyGroupDDay.setDName(dto.getDName());
        studyGroupDDay.setStudyGroup(studyGroup);

        studyGroup.setDDay(studyGroupDDay);

        studyGroupRepository.save(studyGroup);

        return ResponseEntity.status(HttpStatus.OK).body(new MessageDto("스터디 그룹 D-day를 설정했습니다."));
    }

    public ResponseEntity<StudyGroupDDayDto> getStudyGroupDDay(Long groupId) {
        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_GROUP_NOT_FOUND));

        StudyGroupDDayDto studyGroupDDayDto = new StudyGroupDDayDto(
                studyGroup.getGroupId(),
                studyGroup.getDDay().getDName(),
                studyGroup.getDDay().getDDay()
        );

        return ResponseEntity.status(HttpStatus.OK).body(studyGroupDDayDto);
    }

//    private long calculateDDay(StudyGroup studyGroup) {
//        return ChronoUnit.DAYS.between(LocalDate.now(), studyGroup.getDDay());
//    }
}

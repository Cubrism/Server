package com.credential.cubrism.server.studygroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class StudyGroupEnterDto {
    List<StudyGroupMemberInfo> members;
    StudyGroupDDayDto day;

}

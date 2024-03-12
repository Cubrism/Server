package com.credential.cubrism.server.posts.controller;

import com.credential.cubrism.server.common.dto.ErrorDTO;
import com.credential.cubrism.server.common.dto.ResultDTO;
import com.credential.cubrism.server.posts.dto.PostAddPostDTO;
import com.credential.cubrism.server.posts.dto.PostDeletePostDTO;
import com.credential.cubrism.server.posts.dto.PostUpdatePostDTO;
import com.credential.cubrism.server.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {
    private final PostService postService;

    @PostMapping("/add")
    public ResponseEntity<?> addPost(
            @RequestBody PostAddPostDTO dto,
            Authentication authentication
    ) {
        try {
            postService.addPost(dto, authentication);
            return ResponseEntity.ok().body(new ResultDTO(true, null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultDTO(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResultDTO(false, e.getMessage()));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deletePost(
            @RequestBody PostDeletePostDTO dto,
            Authentication authentication
    ) {
        try {
            postService.deletePost(dto, authentication);
            return ResponseEntity.ok().body(new ResultDTO(true, null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultDTO(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResultDTO(false, e.getMessage()));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updatePost(
            @RequestPart("data") PostUpdatePostDTO dto,
            Authentication authentication
    ) {
        try {
            postService.updatePost(dto, authentication);
            return ResponseEntity.ok().body(new ResultDTO(true, null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultDTO(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResultDTO(false, e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> postList(
            @RequestParam(required = false) String boardName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            if (boardName == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDTO("'boardName' 파라미터가 필요합니다."));
            }

            limit = Math.max(1, Math.min(limit, 50)); // 한 페이지의 게시글 수를 1~50 사이로 제한
            Pageable pageable = PageRequest.of(page, limit, Sort.by("createdDate").descending()); // 페이징 처리 (날짜순으로 정렬)

            return ResponseEntity.ok().body(postService.postList(pageable, boardName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDTO(e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> myPostList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        try {
            limit = Math.max(1, Math.min(limit, 50));
            Pageable pageable = PageRequest.of(page, limit, Sort.by("createdDate").descending());

            return ResponseEntity.ok().body(postService.myPostList(pageable, authentication));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDTO(e.getMessage()));
        }
    }

    @GetMapping("/view")
    public ResponseEntity<?> postView(
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) String boardName
    ) {
        try {
            if (postId == null || boardName == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDTO("'postId'와 'boardName' 파라미터가 필요합니다."));
            }

            return ResponseEntity.ok().body(postService.postView(postId, boardName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDTO(e.getMessage()));
        }
    }


//    @GetMapping("/my-post-titles")
//    @ResponseBody
//    public List<String> getMyPostTitles(Authentication auth) {
//        Object principal = auth.getPrincipal();
//        PrincipalDetails principalDetails;
//        if (principal instanceof PrincipalDetails) {
//            principalDetails = (PrincipalDetails) principal;
//        } else if (principal instanceof String) {
//            String email = (String) principal;
//            Users user = userRepository.findByEmail(email)
//                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email : " + email));
//            principalDetails = new PrincipalDetails(user, new HashMap<>());
//        } else {
//            throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass().getName());
//        }
//        UUID uuid = principalDetails.getUser().getUuid();
//        return postService.getAllPostTitlesByUuid(uuid);
//    }
//
//    @GetMapping("/{category}")
//    @ResponseBody
//    public List<Posts> getPostsByCategory(@PathVariable String category) {
//        return postService.getPostsByCategory(category);
//    }
//
//    @PostMapping("/{category}/write")
//    @ResponseBody
//    public String writeBoardWithCategory(@PathVariable String category, @RequestBody RegisterPostRequestDTO req, Authentication auth) {
//        try {
//            postService.writeBoardWithCategory(req, auth, category);
//            return "Success(write with category)";
//        } catch (Throwable t) {
//            return String.format("error : %s", t.getMessage());
//        }
//    }
//
//    @GetMapping("/{category}/{postId}")
//    @ResponseBody
//    public Object writeBoardWithCategory(@PathVariable String category, @PathVariable Long postId, Authentication auth) {
//        Posts post = postService.getPostByPostId(postId);
//
//          if (post == null) {
//              return "Post not found";
//          }
//
//        if (!post.getCategory().equals(category)) {
//            return "category not match";
//        }
//
//        return post;
//    }
//
//    @PostMapping("/addFavoriteCategory")
//    @ResponseBody
//    public Object addFavoriteCategory(@RequestBody AddCategoryDTO addCategoryDTO, Authentication auth) {
//        Object principal = auth.getPrincipal();
//        PrincipalDetails principalDetails;
//        if (principal instanceof PrincipalDetails) {
//            principalDetails = (PrincipalDetails) principal;
//        } else if (principal instanceof String) {
//            String email = (String) principal;
//            Users user = userRepository.findByEmail(email)
//                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email : " + email));
//            principalDetails = new PrincipalDetails(user, new HashMap<>());
//            // AddCategoryDTO 객체에서 카테고리를 가져옵니다.
//            String category = addCategoryDTO.getCategory();
//
//            // 사용자의 categories 리스트에 이미 해당 카테고리가 있는지 확인합니다.
//            if (user.getCategories().contains(category)) {
//                // 이미 있다면, 카테고리를 추가하지 않고 메시지를 반환합니다.
//                return "Category already exists in favorites";
//            }
//
//            // 사용자의 categories 리스트에 카테고리를 추가합니다.
//            user.getCategories().add(category);
//
//            // 변경 사항을 저장합니다.
//            userRepository.save(user);
//        } else {
//            throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass().getName());
//        }
//
//        return "Success"+" "+addCategoryDTO.getCategory();
//    }
//
//    @PostMapping("/removeFavoriteCategory")
//    @ResponseBody
//    public Object removeFavoriteCategory(@RequestBody AddCategoryDTO addCategoryDTO, Authentication auth) {
//        Object principal = auth.getPrincipal();
//        PrincipalDetails principalDetails;
//        if (principal instanceof PrincipalDetails) {
//            principalDetails = (PrincipalDetails) principal;
//        } else if (principal instanceof String) {
//            String email = (String) principal;
//            Users user = userRepository.findByEmail(email)
//                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email : " + email));
//            principalDetails = new PrincipalDetails(user, new HashMap<>());
//            // AddCategoryDTO 객체에서 카테고리를 가져옵니다.
//            String category = addCategoryDTO.getCategory();
//
//            // 사용자의 categories 리스트에 해당 카테고리가 있는지 확인합니다.
//            if (!user.getCategories().contains(category)) {
//                // 없다면, 메시지를 반환합니다.
//                return "Category not found in favorites";
//            }
//
//            // 사용자의 categories 리스트에서 카테고리를 삭제합니다.
//            user.getCategories().remove(category);
//
//            // 변경 사항을 저장합니다.
//            userRepository.save(user);
//        } else {
//            throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass().getName());
//        }
//
//        return "Success"+" "+addCategoryDTO.getCategory();
//    }
//
//    @GetMapping("/myFavoriteCategory")
//    @ResponseBody
//    public Object myFavoriteCategory(Authentication auth) {
//        Object principal = auth.getPrincipal();
//        PrincipalDetails principalDetails;
//        if (principal instanceof PrincipalDetails) {
//            principalDetails = (PrincipalDetails) principal;
//        } else if (principal instanceof String) {
//            String email = (String) principal;
//            Users user = userRepository.findByEmail(email)
//                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email : " + email));
//            List<String> categories = user.getCategories();
//
//            return categories;
//        } else {
//            throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass().getName());
//        }
//
//        return "error";
//    }
//
//    @GetMapping("/myFavoriteCategoryPosts")
//    @ResponseBody
//    public Object myFavoriteCategoryPosts(Authentication auth) {
//        Object principal = auth.getPrincipal();
//        PrincipalDetails principalDetails;
//        if (principal instanceof PrincipalDetails) {
//            principalDetails = (PrincipalDetails) principal;
//        } else if (principal instanceof String) {
//            String email = (String) principal;
//            Users user = userRepository.findByEmail(email)
//                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email : " + email));
//            List<String> categories = user.getCategories();
//            List<Posts> posts = postRepository.findByCategoryIn(categories);
//
//            String result = "{";
//            for (Posts post : posts) {
//                result += post.getTitle() + ", ";
//            }
//            result += "}";
//
//            return result; // 나중에 얘 post로 바꾸면 json 형태로 전달될거임
//        } else {
//            throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass().getName());
//        }
//
//        return "error";
//    }

}

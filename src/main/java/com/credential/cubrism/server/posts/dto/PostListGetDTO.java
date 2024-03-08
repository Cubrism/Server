package com.credential.cubrism.server.posts.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PostListGetDTO {
    private Pageable page;
    private List<PostList> postList;

    @Getter
    @AllArgsConstructor
    public static class Pageable {
        private Integer previousPage;
        private int currentPage;
        private Integer nextPage;
    }

    @Getter
    @AllArgsConstructor
    public static class PostList {
        private Long postId;
        private String boardName;
        private String nickname;
        private String title;
        private String content;
        private String createdDate;
    }
}

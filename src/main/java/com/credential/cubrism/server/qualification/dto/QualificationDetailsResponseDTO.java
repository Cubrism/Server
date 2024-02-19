package com.credential.cubrism.server.qualification.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class QualificationDetailsResponseDTO {
    private String name;
    private String code;
    private List<Schedule> schedule;
    private Fee fee;
    private String tendency;
    private List<Standard> standard;
    private List<Question> question;
    private String acquisition;
    private List<Books> books;

    @Getter
    public static class Schedule {
        private String category;
        private String writtenApp;
        private String writtenExam;
        private String writtenExamResult;
        private String practicalApp;
        private String practicalExam;
        private String practicalExamResult;
    }

    @Getter
    public static class Fee {
        private int writtenFee;
        private int practicalFee;
    }

    @Getter
    public static class Standard {
        private String filePath;
        private String fileName;
    }

    @Getter
    public static class Question {
        private String filePath;
        private String fileName;
    }

    @Getter
    public static class Books {
        private List<String> authors;
        private String datetime;
        private String isbn;
        private int price;
        private String publisher;
        private int sale_price;
        private String thumbnail;
        private String title;
        private String url;
    }
}
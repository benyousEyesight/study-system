package com.study.question.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class TextbookParseResult {
    private String fileName;
    private String fileType;
    private int totalPages;
    private String previewText;
    private List<ChapterNode> chapters;

    @Data
    public static class ChapterNode {
        private String name;
        private int level;
        private List<ChapterNode> children;
    }
}

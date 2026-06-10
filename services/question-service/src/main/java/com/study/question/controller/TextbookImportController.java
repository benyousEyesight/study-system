package com.study.question.controller;

import com.study.question.common.Result;
import com.study.question.model.dto.TextbookParseResult;
import com.study.question.model.entity.KnowledgePoint;
import com.study.question.service.KnowledgePointService;
import com.study.question.service.TextbookParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/textbook")
public class TextbookImportController {

    @Autowired
    private TextbookParserService parserService;
    @Autowired
    private KnowledgePointService knowledgePointService;

    @PostMapping("/parse")
    public Result<TextbookParseResult> parse(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return Result.fail(400, "文件为空");
            TextbookParseResult result = parserService.parse(file);
            return Result.ok(result);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            return Result.fail(500, "解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    public Result<?> save(@RequestParam Long subjectId,
                          @RequestParam Long tenantId,
                          @RequestBody List<TextbookParseResult.ChapterNode> chapters) {
        try {
            saveTree(chapters, null, subjectId, tenantId, 0);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(500, "保存失败: " + e.getMessage());
        }
    }

    private int saveTree(List<TextbookParseResult.ChapterNode> nodes, Long parentId,
                         Long subjectId, Long tenantId, int sort) {
        int count = 0;
        for (int i = 0; i < nodes.size(); i++) {
            TextbookParseResult.ChapterNode node = nodes.get(i);

            KnowledgePoint kp = new KnowledgePoint();
            kp.setTenantId(tenantId);
            kp.setSubjectId(subjectId);
            kp.setName(node.getName());
            kp.setParentId(parentId);
            kp.setLevel(node.getLevel());
            kp.setSort(sort + i);
            kp.setStatus(1);
            knowledgePointService.create(kp);

            count++;
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                count += saveTree(node.getChildren(), kp.getId(), subjectId, tenantId, 0);
            }
        }
        return count;
    }
}

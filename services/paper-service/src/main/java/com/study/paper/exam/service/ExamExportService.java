package com.study.paper.exam.service;

import com.study.paper.exam.mapper.ExamAnswerMapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.mapper.KpWeaknessMapper;
import com.study.paper.exam.model.dto.KpWeaknessVO;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.KpWeakness;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.study.paper.exam.model.dto.ExamReportVO;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamExportService {

    @Autowired
    private ExamReportService examReportService;
    @Autowired
    private KpWeaknessService kpWeaknessService;
    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private KpWeaknessMapper weaknessMapper;
    @Autowired
    private ExamAnswerMapper answerMapper;

    public byte[] exportExamReport(Long examId) throws Exception {
        var report = examReportService.getReport(examId);
        if (report == null) throw new IllegalArgumentException("考试不存在");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Styles
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle numStyle = createNumStyle(wb);

            // Sheet 1: 考试概览
            Sheet summarySheet = wb.createSheet("考试概览");
            fillSummarySheet(summarySheet, report, headerStyle, titleStyle, numStyle);

            // Sheet 2: 学生成绩
            Sheet detailSheet = wb.createSheet("学生成绩");
            fillDetailSheet(detailSheet, report, headerStyle, numStyle);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportStudentWeakness(Long studentId) throws Exception {
        List<KpWeaknessVO> data = kpWeaknessService.getStudentWeakness(studentId);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle numStyle = createNumStyle(wb);

            if (data.isEmpty()) {
                Sheet sheet = wb.createSheet("知识点分析");
                Row r = sheet.createRow(0);
                r.createCell(0).setCellValue("暂无可用的知识点数据");
            }

            for (KpWeaknessVO subject : data) {
                Sheet sheet = wb.createSheet(subject.getSubjectName());
                // Title row
                Row titleRow = sheet.createRow(0);
                Cell tc = titleRow.createCell(0);
                tc.setCellValue(subject.getSubjectName() + " - 综合得分率 " + subject.getSubjectAccuracy() + "%");
                tc.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

                // Header row
                Row headerRow = sheet.createRow(1);
                String[] headers = {"知识点", "得分率", "得分/总分", "题目数量"};
                for (int i = 0; i < headers.length; i++) {
                    Cell c = headerRow.createCell(i);
                    c.setCellValue(headers[i]);
                    c.setCellStyle(headerStyle);
                }

                // Data rows
                int rowIdx = 2;
                for (KpWeaknessVO.KpItem item : subject.getItems()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(item.getKnowledgePointName());
                    Cell accCell = row.createCell(1);
                    accCell.setCellValue(item.getAccuracy().doubleValue());
                    accCell.setCellStyle(numStyle);
                    row.createCell(2).setCellValue(item.getEarnedScore() + " / " + item.getTotalScore());
                    row.createCell(3).setCellValue(item.getAttemptCount());
                }

                sheet.autoSizeColumn(0);
                sheet.setColumnWidth(1, 4500);
                sheet.setColumnWidth(2, 4000);
                sheet.setColumnWidth(3, 3000);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    private void fillSummarySheet(Sheet sheet, ExamReportVO report, CellStyle headerStyle, CellStyle titleStyle, CellStyle numStyle) {
        // Title
        Row titleRow = sheet.createRow(0);
        Cell tc = titleRow.createCell(0);
        tc.setCellValue(report.getExamTitle() + " - 考试报告");
        tc.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        // Summary section
        int rowIdx = 2;
        Row labelRow = sheet.createRow(rowIdx);
        String[] labels = {"参考人数", "平均分", "最高分", "最低分", "满分"};
        for (int i = 0; i < labels.length; i++) {
            Cell c = labelRow.createCell(i);
            c.setCellValue(labels[i]);
            c.setCellStyle(headerStyle);
        }
        Row valRow = sheet.createRow(++rowIdx);
        Object[] values = {report.getTotalStudents(), report.getAvgScore(), report.getMaxScore(), report.getMinScore(), report.getFullScore()};
        for (int i = 0; i < values.length; i++) {
            Cell c = valRow.createCell(i);
            if (values[i] instanceof Number) {
                c.setCellValue(((Number) values[i]).doubleValue());
                c.setCellStyle(numStyle);
            } else {
                c.setCellValue(values[i] != null ? values[i].toString() : "-");
            }
        }

        rowIdx += 2;
        Row rateLabel = sheet.createRow(rowIdx);
        String[] rateLabels = {"及格率", "优秀率"};
        for (int i = 0; i < rateLabels.length; i++) {
            Cell c = rateLabel.createCell(i);
            c.setCellValue(rateLabels[i]);
            c.setCellStyle(headerStyle);
        }
        Row rateVal = sheet.createRow(++rowIdx);
        rateVal.createCell(0).setCellValue(report.getPassRate() + "%");
        rateVal.createCell(1).setCellValue(report.getExcellentRate() + "%");

        // Distribution section
        rowIdx += 2;
        Row distHeader = sheet.createRow(rowIdx);
        String[] distLabels = {"分数段", "<60", "60-69", "70-79", "80-89", "90-100"};
        for (int i = 0; i < distLabels.length; i++) {
            Cell c = distHeader.createCell(i);
            c.setCellValue(distLabels[i]);
            c.setCellStyle(headerStyle);
        }
        var dist = report.getDistribution();
        Row distRow = sheet.createRow(++rowIdx);
        distRow.createCell(0).setCellValue("人数");
        int[] counts = {dist.getBelow60(), dist.getBetween60And69(), dist.getBetween70And79(), dist.getBetween80And89(), dist.getBetween90And100()};
        for (int i = 0; i < counts.length; i++) {
            Cell c = distRow.createCell(i + 1);
            c.setCellValue(counts[i]);
            c.setCellStyle(numStyle);
        }

        sheet.autoSizeColumn(0);
        for (int i = 1; i <= 4; i++) sheet.setColumnWidth(i, 4000);
    }

    private void fillDetailSheet(Sheet sheet, ExamReportVO report, CellStyle headerStyle, CellStyle numStyle) {
        // Title
        Row titleRow = sheet.createRow(0);
        Cell tc = titleRow.createCell(0);
        tc.setCellValue("学生成绩排名");
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        // Headers
        Row headerRow = sheet.createRow(1);
        String[] headers = {"排名", "学生ID", "总分", "状态"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        // Data
        int rowIdx = 2;
        for (var student : report.getStudents()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(student.getRank());
            row.createCell(1).setCellValue(student.getUserId());
            Cell scoreCell = row.createCell(2);
            scoreCell.setCellValue(student.getTotalScore() != null ? student.getTotalScore().doubleValue() : 0);
            scoreCell.setCellStyle(numStyle);
            row.createCell(3).setCellValue(student.getStatus());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(2, 4000);
        sheet.setColumnWidth(3, 3000);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createNumStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.0"));
        return style;
    }
}

package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.model.dto.ExamReportVO;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.ExamSession;
import com.study.paper.mapper.PaperMapper;
import com.study.paper.model.entity.Paper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamReportService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private PaperMapper paperMapper;

    public ExamReportVO getReport(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) return null;

        List<ExamSession> graded = sessionMapper.selectList(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getExamId, examId)
                        .eq(ExamSession::getStatus, "GRADED")
                        .orderByDesc(ExamSession::getTotalScore));

        ExamReportVO report = new ExamReportVO();
        report.setExamId(examId);
        report.setExamTitle(exam.getTitle());

        Paper paper = paperMapper.selectById(exam.getPaperId());
        report.setPaperTitle(paper != null ? paper.getTitle() : null);

        if (graded.isEmpty()) {
            report.setTotalStudents(0);
            report.setAvgScore(BigDecimal.ZERO);
            report.setMaxScore(BigDecimal.ZERO);
            report.setMinScore(BigDecimal.ZERO);
            report.setPassRate(BigDecimal.ZERO);
            report.setExcellentRate(BigDecimal.ZERO);
            report.setFullScore(paper != null ? BigDecimal.valueOf(paper.getTotalScore()) : BigDecimal.ZERO);
            report.setDistribution(new ExamReportVO.ScoreDistribution());
            report.setStudents(Collections.emptyList());
            return report;
        }

        report.setTotalStudents(graded.size());
        report.setFullScore(paper != null ? BigDecimal.valueOf(paper.getTotalScore()) : BigDecimal.ZERO);

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal max = graded.get(0).getTotalScore() != null ? graded.get(0).getTotalScore() : BigDecimal.ZERO;
        BigDecimal min = graded.get(graded.size() - 1).getTotalScore() != null ? graded.get(graded.size() - 1).getTotalScore() : BigDecimal.ZERO;
        int passed = 0;
        int excellent = 0;

        ExamReportVO.ScoreDistribution dist = new ExamReportVO.ScoreDistribution();
        for (ExamSession s : graded) {
            BigDecimal score = s.getTotalScore() != null ? s.getTotalScore() : BigDecimal.ZERO;
            sum = sum.add(score);
            if (score.compareTo(min) < 0) min = score;
            if (score.compareTo(max) > 0) max = score;

            int intScore = score.intValue();
            if (intScore >= 60) passed++;
            if (intScore >= 80) excellent++;
            if (intScore < 60) dist.setBelow60(dist.getBelow60() + 1);
            else if (intScore < 70) dist.setBetween60And69(dist.getBetween60And69() + 1);
            else if (intScore < 80) dist.setBetween70And79(dist.getBetween70And79() + 1);
            else if (intScore < 90) dist.setBetween80And89(dist.getBetween80And89() + 1);
            else dist.setBetween90And100(dist.getBetween90And100() + 1);
        }

        report.setAvgScore(sum.divide(BigDecimal.valueOf(graded.size()), 1, RoundingMode.HALF_UP));
        report.setMaxScore(max);
        report.setMinScore(min);
        report.setPassRate(BigDecimal.valueOf(passed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(graded.size()), 1, RoundingMode.HALF_UP));
        report.setExcellentRate(BigDecimal.valueOf(excellent)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(graded.size()), 1, RoundingMode.HALF_UP));
        report.setDistribution(dist);

        // Per-student rankings
        int rank = 1;
        List<ExamReportVO.StudentRankItem> items = new ArrayList<>();
        for (ExamSession s : graded) {
            ExamReportVO.StudentRankItem item = new ExamReportVO.StudentRankItem();
            item.setRank(rank++);
            item.setUserId(s.getUserId());
            item.setTotalScore(s.getTotalScore());
            item.setStatus(s.getStatus());
            items.add(item);
        }
        report.setStudents(items);

        return report;
    }
}

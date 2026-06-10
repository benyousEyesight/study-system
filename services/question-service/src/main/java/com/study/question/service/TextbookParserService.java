package com.study.question.service;

import com.study.question.model.dto.TextbookParseResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TextbookParserService {

    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^(第[一二三四五六七八九十百千]+[章节篇]|[0-9]+\\.[0-9]+\\s|第[0-9]+[章节]|[0-9]+[、\\.\\s]\\s*[\\u4e00-\\u9fa5])",
            Pattern.MULTILINE);

    private static final Pattern CHAPTER_NUM = Pattern.compile(
            "第([一二三四五六七八九十百千0-9]+)([章节篇])");

    private static final String TESSDATA_BASE_URL =
            "https://github.com/tesseract-ocr/tessdata/raw/main/";

    @Value("${ocr.enabled:false}")
    private boolean ocrEnabled;

    @Value("${ocr.tesseract-path:tesseract}")
    private String tesseractPath;

    @Value("${ocr.language:chi_sim}")
    private String ocrLanguage;

    @Value("${ocr.tessdata:./tessdata}")
    private String tessdataPath;

    @Value("${ocr.min-text-length:100}")
    private int minTextLength;

    @Value("${ocr.resolution:300}")
    private int ocrResolution;

    @PostConstruct
    public void initOcr() {
        if (ocrEnabled) {
            ensureTessdataExists();
        }
    }

    public TextbookParseResult parse(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        String ext = "";
        if (fileName != null && fileName.contains(".")) {
            ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }

        String text;
        int totalPages = 0;

        switch (ext) {
            case "pdf":
                var pdfResult = extractFromPdf(file);
                text = pdfResult.getKey();
                totalPages = pdfResult.getValue();
                break;
            case "docx":
                text = extractFromDocx(file);
                break;
            case "txt":
                text = extractFromTxt(file);
                break;
            default:
                throw new IllegalArgumentException("不支持的文件格式: " + ext + "，仅支持 PDF/DOCX/TXT");
        }

        TextbookParseResult result = new TextbookParseResult();
        result.setFileName(fileName);
        result.setFileType(ext);
        result.setTotalPages(totalPages);
        result.setPreviewText(text.length() > 500 ? text.substring(0, 500) : text);
        result.setChapters(parseChapters(text));

        return result;
    }

    private Map.Entry<String, Integer> extractFromPdf(MultipartFile file) throws Exception {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            int pages = doc.getNumberOfPages();

            if (text.trim().length() < minTextLength && ocrEnabled) {
                log.info("PDF text extraction returned only {} chars (threshold: {}), falling back to OCR...",
                        text.trim().length(), minTextLength);
                text = ocrPdfPages(doc);
            }

            return new AbstractMap.SimpleEntry<>(text, pages);
        }
    }

    private String extractFromDocx(MultipartFile file) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String extractFromTxt(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    // ---- OCR fallback for scanned/image-based PDFs ----

    private String ocrPdfPages(PDDocument doc) throws Exception {
        PDFRenderer renderer = new PDFRenderer(doc);
        StringBuilder sb = new StringBuilder();
        int totalPages = doc.getNumberOfPages();

        log.info("Running OCR on {} pages with language '{}' at {} DPI...", totalPages, ocrLanguage, ocrResolution);
        for (int i = 0; i < totalPages; i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, ocrResolution);
            String pageText = runTesseract(image);
            log.debug("OCR page {}/{}: {} chars", i + 1, totalPages, pageText.length());
            sb.append(pageText).append("\n");
        }
        log.info("OCR complete, total {} chars", sb.length());
        return sb.toString();
    }

    private String runTesseract(BufferedImage image) throws Exception {
        File tempInput = File.createTempFile("ocr_page_", ".png");
        try {
            ImageIO.write(image, "png", tempInput);
            ProcessBuilder pb = new ProcessBuilder(
                    tesseractPath, tempInput.getAbsolutePath(), "stdout",
                    "-l", ocrLanguage, "--tessdata-dir", tessdataPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String result;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                result = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Tesseract OCR failed with exit code " + exitCode
                        + ". Ensure tesseract is installed and the language pack '"
                        + ocrLanguage + "' is available.");
            }
            return result;
        } finally {
            if (tempInput.exists()) {
                tempInput.delete();
            }
        }
    }

    private void ensureTessdataExists() {
        Path langFile = Paths.get(tessdataPath, ocrLanguage + ".traineddata");
        if (Files.exists(langFile)) {
            log.info("Tessdata found: {}", langFile);
            return;
        }
        try {
            Files.createDirectories(Paths.get(tessdataPath));
            URL url = new URL(TESSDATA_BASE_URL + ocrLanguage + ".traineddata");
            log.info("Downloading {} to {} ...", url, langFile);
            try (InputStream in = url.openStream()) {
                Files.copy(in, langFile, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Downloaded tessdata: {}", langFile);
        } catch (Exception e) {
            log.warn("Could not download tessdata for '{}': {}. OCR will fail if the " +
                    "language data is not already installed. Place {}.traineddata in {}",
                    ocrLanguage, e.getMessage(), ocrLanguage, tessdataPath);
        }
    }

    private List<TextbookParseResult.ChapterNode> parseChapters(String text) {
        // Split into lines and find chapter/section headings
        String[] lines = text.split("\n");
        List<ChapterHeading> headings = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            Matcher matcher = CHAPTER_PATTERN.matcher(line);
            if (matcher.find()) {
                int level = detectLevel(line);
                headings.add(new ChapterHeading(line.substring(0, Math.min(line.length(), 80)), level));
            }
        }

        // Deduplicate consecutive same-level headings
        List<ChapterHeading> filtered = new ArrayList<>();
        for (ChapterHeading h : headings) {
            if (!filtered.isEmpty()) {
                ChapterHeading last = filtered.get(filtered.size() - 1);
                if (last.level == h.level && last.name.equals(h.name)) continue;
            }
            filtered.add(h);
        }

        return buildTree(filtered);
    }

    private int detectLevel(String line) {
        Matcher matcher = CHAPTER_NUM.matcher(line);
        if (matcher.find()) {
            String unit = matcher.group(2);
            if ("章".equals(unit) || "篇".equals(unit)) return 1;
            if ("节".equals(unit)) return 2;
        }
        // Numbered headings: "1.1 " or "1、" → level 2; "1 " → level 1
        if (line.matches("^[0-9]+\\.[0-9]+\\s.*")) return 2;
        if (line.matches("^第[0-9]+章\\s.*")) return 1;
        if (line.matches("^[0-9]+[、\\.]\\s*[\\u4e00-\\u9fa5].*")) return 2;
        return 3; // default finer level
    }

    private List<TextbookParseResult.ChapterNode> buildTree(List<ChapterHeading> headings) {
        List<TextbookParseResult.ChapterNode> roots = new ArrayList<>();
        TextbookParseResult.ChapterNode currentParent = null;
        int parentLevel = 0;

        for (ChapterHeading h : headings) {
            TextbookParseResult.ChapterNode node = new TextbookParseResult.ChapterNode();
            node.setName(h.name);
            node.setLevel(h.level);
            node.setChildren(new ArrayList<>());

            if (roots.isEmpty() || h.level <= parentLevel) {
                roots.add(node);
                currentParent = node;
                parentLevel = h.level;
            } else if (currentParent != null) {
                currentParent.getChildren().add(node);
            } else {
                roots.add(node);
                currentParent = node;
                parentLevel = h.level;
            }
        }
        return roots;
    }

    private static class ChapterHeading {
        String name;
        int level;

        ChapterHeading(String name, int level) {
            this.name = name;
            this.level = level;
        }
    }
}

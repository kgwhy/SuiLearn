package com.suilearn.api.material.document;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.HWPFOldDocument;
import org.apache.poi.hwpf.OldWordFileFormatException;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

/**
 * Isolated, non-executing adapter that turns validated document bytes into ordered text blocks.
 */
public class DocumentParser {
    private static final long DEFAULT_MAX_TEXT_PARSE_BYTES = 50L * 1024 * 1024;
    private static final int TEXT_CHUNK_CHARACTERS = 64 * 1024;
    private static final byte[] OLE_SIGNATURE = {
        (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private final int ocrTextDensityThreshold;
    private final long maxTextParseBytes;

    public DocumentParser() { this(16, DEFAULT_MAX_TEXT_PARSE_BYTES); }

    /** A page below this many extracted characters is treated as OCR-needed. */
    public DocumentParser(int ocrTextDensityThreshold) { this(ocrTextDensityThreshold, DEFAULT_MAX_TEXT_PARSE_BYTES); }

    /** The text parser shares the validated upload admission bound instead of inventing a lower worker limit. */
    public DocumentParser(int ocrTextDensityThreshold, long maxTextParseBytes) {
        this.ocrTextDensityThreshold = Math.max(0, ocrTextDensityThreshold);
        this.maxTextParseBytes = Math.max(1, maxTextParseBytes);
    }

    public Result parse(byte[] bytes, String fileName, String declaredMimeType) {
        if (bytes == null || fileName == null || declaredMimeType == null) {
            return rejected();
        }

        try {
            Format format = declaredFormat(fileName, declaredMimeType);
            if (format == null || !hasExpectedSignature(format, bytes)) {
                return rejected();
            }
            return switch (format) {
                case MARKDOWN -> parsedMarkdown(decodeUtf8(bytes));
                case TEXT -> parsedText(decodeUtf8(bytes));
                case PDF -> parsedPdf(bytes);
                case DOC -> parsedDoc(bytes);
                case DOCX -> parsedDocx(bytes);
            };
        } catch (IOException | RuntimeException exception) {
            return rejected();
        }
    }

    /** Parses an already-bounded temporary original without loading raw binary data into heap memory. */
    public Result parse(Path file, String fileName, String declaredMimeType) {
        if (file == null || fileName == null || declaredMimeType == null) return rejected();
        try {
            Format format = declaredFormat(fileName, declaredMimeType);
            if (format == null || !hasExpectedSignature(format, file)) return rejected();
            return switch (format) {
                case MARKDOWN -> parsedMarkdownStream(file);
                case TEXT -> parsedTextStream(file);
                case PDF -> parsedPdf(file);
                case DOC -> parsedDoc(file);
                case DOCX -> parsedDocx(file);
            };
        } catch (IOException | RuntimeException exception) {
            return rejected();
        }
    }

    private Result parsedMarkdown(String markdown) {
        List<Block> blocks = new ArrayList<>();
        appendMarkdownBlocks(Parser.builder().build().parse(markdown), new ArrayList<>(), blocks);
        return parsed(1, blocks, List.of());
    }

    private void appendMarkdownBlocks(Node parent, List<String> headings, List<Block> blocks) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNext()) {
            if (node instanceof HtmlBlock) {
                continue;
            }
            if (node instanceof Heading heading) {
                String content = visibleMarkdownText(heading).trim();
                if (!content.isBlank()) {
                    while (headings.size() >= heading.getLevel()) headings.removeLast();
                    headings.add(content);
                    blocks.add(new Block(blocks.size(), 1, String.join(" > ", headings), content));
                }
                continue;
            }
            if (node instanceof org.commonmark.node.Paragraph || node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock) {
                String content = visibleMarkdownText(node).trim();
                if (!content.isBlank()) {
                    blocks.add(new Block(blocks.size(), 1, String.join(" > ", headings), content));
                }
                continue;
            }
            appendMarkdownBlocks(node, headings, blocks);
        }
    }

    private String visibleMarkdownText(Node node) {
        if (node instanceof HtmlInline || node instanceof HtmlBlock) return "";
        if (node instanceof Text text) return text.getLiteral();
        if (node instanceof Code code) return code.getLiteral();
        if (node instanceof FencedCodeBlock fencedCodeBlock) return fencedCodeBlock.getLiteral();
        if (node instanceof IndentedCodeBlock indentedCodeBlock) return indentedCodeBlock.getLiteral();
        if (node instanceof SoftLineBreak || node instanceof HardLineBreak) return "\n";
        StringBuilder content = new StringBuilder();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            content.append(visibleMarkdownText(child));
        }
        return content.toString();
    }

    private Result parsedText(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<Block> blocks = normalized.isEmpty() ? List.of() : List.of(new Block(0, 1, "", normalized));
        return parsed(1, blocks, List.of());
    }

    private Result parsedPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            List<Block> blocks = new ArrayList<>();
            List<Integer> ocrPages = new ArrayList<>();
            int order = 0;
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).trim();
                if (text.length() < ocrTextDensityThreshold) {
                    ocrPages.add(page);
                } else {
                    blocks.add(new Block(order++, page, "", text));
                }
            }
            return parsed(document.getNumberOfPages(), blocks, ocrPages);
        }
    }

    private Result parsedPdf(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            List<Block> blocks = new ArrayList<>();
            List<Integer> ocrPages = new ArrayList<>();
            int order = 0;
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).trim();
                if (text.length() < ocrTextDensityThreshold) ocrPages.add(page);
                else blocks.add(new Block(order++, page, "", text));
            }
            return parsed(document.getNumberOfPages(), blocks, ocrPages);
        }
    }

    private Result parsedDoc(byte[] bytes) throws IOException {
        try {
            try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
                 WordExtractor extractor = new WordExtractor(document)) {
                return parsedOfficeText(extractor.getText());
            }
        } catch (OldWordFileFormatException ignored) {
            try (POIFSFileSystem fileSystem = new POIFSFileSystem(new ByteArrayInputStream(bytes));
                 HWPFOldDocument document = new HWPFOldDocument(fileSystem)) {
                return parsedOfficeText(document.getText().toString());
            }
        }
    }

    private Result parsedDoc(Path file) throws IOException {
        try {
            try (var stream = Files.newInputStream(file);
                 HWPFDocument document = new HWPFDocument(stream);
                 WordExtractor extractor = new WordExtractor(document)) {
                return parsedOfficeText(extractor.getText());
            }
        } catch (OldWordFileFormatException ignored) {
            try (var input = Files.newInputStream(file);
                 POIFSFileSystem fileSystem = new POIFSFileSystem(input);
                 HWPFOldDocument document = new HWPFOldDocument(fileSystem)) {
                return parsedOfficeText(document.getText().toString());
            }
        }
    }

    private Result parsedDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            List<Block> blocks = new ArrayList<>();
            int order = 0;
            for (var paragraph : document.getParagraphs()) {
                String text = paragraph.getText().trim();
                if (!text.isEmpty()) {
                    blocks.add(new Block(order++, 1, "", text));
                }
            }
            return parsed(1, blocks, List.of());
        }
    }

    private Result parsedDocx(Path file) throws IOException {
        try (var input = Files.newInputStream(file); XWPFDocument document = new XWPFDocument(input)) {
            List<Block> blocks = new ArrayList<>();
            int order = 0;
            for (var paragraph : document.getParagraphs()) {
                String text = paragraph.getText().trim();
                if (!text.isEmpty()) blocks.add(new Block(order++, 1, "", text));
            }
            return parsed(1, blocks, List.of());
        }
    }

    private Result parsedOfficeText(String text) {
        List<Block> blocks = new ArrayList<>();
        int order = 0;
        for (String paragraph : text.replace('\r', '\n').split("\n+")) {
            String normalized = paragraph.trim();
            if (!normalized.isEmpty()) {
                blocks.add(new Block(order++, 1, "", normalized));
            }
        }
        return parsed(1, blocks, List.of());
    }

    private String decodeUtf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString();
    }

    private Result parsedMarkdownStream(Path file) throws IOException {
        List<Block> blocks = new ArrayList<>();
        List<String> headings = new ArrayList<>();
        streamTextChunks(file, chunk -> appendMarkdownBlocks(Parser.builder().build().parse(chunk), headings, blocks));
        return parsed(1, blocks, List.of());
    }

    private Result parsedTextStream(Path file) throws IOException {
        List<Block> blocks = new ArrayList<>();
        streamTextChunks(file, chunk -> {
            String normalized = chunk.replace("\r\n", "\n").replace('\r', '\n').trim();
            if (!normalized.isEmpty()) blocks.add(new Block(blocks.size(), 1, "", normalized));
        });
        return parsed(1, blocks, List.of());
    }

    /**
     * Keeps parser heap use bounded: only one decoded 64KiB chunk is held while the 50MiB admission
     * cap is enforced on bytes.  Result blocks remain the intentionally persisted document content.
     */
    private void streamTextChunks(Path file, Consumer<String> consumer) throws IOException {
        if (Files.size(file) > maxTextParseBytes) throw new IOException("Text original exceeds upload admission limit");
        try (InputStream input = new ByteLimitInputStream(Files.newInputStream(file), maxTextParseBytes);
             var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                 .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)))) {
            char[] buffer = new char[8192];
            StringBuilder chunk = new StringBuilder(TEXT_CHUNK_CHARACTERS + buffer.length);
            for (int read; (read = reader.read(buffer)) != -1;) {
                chunk.append(buffer, 0, read);
                while (chunk.length() >= TEXT_CHUNK_CHARACTERS) {
                    int split = chunk.lastIndexOf("\n", TEXT_CHUNK_CHARACTERS);
                    int end = split >= 0 ? split + 1 : TEXT_CHUNK_CHARACTERS;
                    consumer.accept(chunk.substring(0, end));
                    chunk.delete(0, end);
                }
            }
            if (!chunk.isEmpty()) consumer.accept(chunk.toString());
        }
    }

    private static final class ByteLimitInputStream extends FilterInputStream {
        private final long maximum;
        private long consumed;

        private ByteLimitInputStream(InputStream input, long maximum) {
            super(input);
            this.maximum = maximum;
        }

        @Override
        public int read() throws IOException {
            if (consumed >= maximum) return rejectAdditionalByte();
            int value = in.read();
            if (value >= 0) consumed++;
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (consumed >= maximum) return rejectAdditionalByte();
            int allowed = (int) Math.min(length, maximum - consumed);
            int read = in.read(bytes, offset, allowed);
            if (read > 0) consumed += read;
            return read;
        }

        private int rejectAdditionalByte() throws IOException {
            if (in.read() == -1) return -1;
            throw new IOException("Text original exceeds upload admission limit");
        }
    }

    private Format declaredFormat(String fileName, String declaredMimeType) {
        String extension = extensionOf(fileName);
        String mimeType = declaredMimeType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "md", "markdown" -> "text/markdown".equals(mimeType) ? Format.MARKDOWN : null;
            case "txt" -> "text/plain".equals(mimeType) ? Format.TEXT : null;
            case "pdf" -> "application/pdf".equals(mimeType) ? Format.PDF : null;
            case "doc" -> "application/msword".equals(mimeType) ? Format.DOC : null;
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType)
                ? Format.DOCX : null;
            default -> null;
        };
    }

    private String extensionOf(String fileName) {
        int separator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        int dot = fileName.lastIndexOf('.');
        return dot > separator && dot < fileName.length() - 1 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private boolean hasExpectedSignature(Format format, byte[] bytes) throws IOException {
        return switch (format) {
            case MARKDOWN, TEXT -> true;
            case PDF -> beginsWith(bytes, PDF_SIGNATURE);
            case DOC -> beginsWith(bytes, OLE_SIGNATURE);
            case DOCX -> beginsWith(bytes, new byte[] {'P', 'K', 3, 4}) && containsWordDocumentPart(bytes);
        };
    }

    private boolean hasExpectedSignature(Format format, Path file) throws IOException {
        if (format == Format.MARKDOWN || format == Format.TEXT) return true;
        try (var input = Files.newInputStream(file)) {
            byte[] prefix = input.readNBytes(8);
            if (format == Format.PDF) return beginsWith(prefix, PDF_SIGNATURE);
            if (format == Format.DOC) return beginsWith(prefix, OLE_SIGNATURE);
        }
        if (format == Format.DOCX) {
            try (var input = Files.newInputStream(file); ZipInputStream zip = new ZipInputStream(input)) {
                Set<String> parts = new LinkedHashSet<>();
                for (var entry = zip.getNextEntry(); entry != null && parts.size() < 10_000; entry = zip.getNextEntry()) {
                    parts.add(entry.getName());
                }
                return parts.contains("[Content_Types].xml") && parts.contains("word/document.xml");
            }
        }
        return false;
    }

    private boolean beginsWith(byte[] bytes, byte[] signature) {
        return bytes.length >= signature.length && Arrays.equals(Arrays.copyOf(bytes, signature.length), signature);
    }

    private boolean containsWordDocumentPart(byte[] bytes) throws IOException {
        Set<String> parts = new LinkedHashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var entry = zip.getNextEntry(); entry != null && parts.size() < 10_000; entry = zip.getNextEntry()) {
                parts.add(entry.getName());
            }
        }
        return parts.contains("[Content_Types].xml") && parts.contains("word/document.xml");
    }

    private Result parsed(int pageCount, List<Block> blocks, List<Integer> ocrPageNumbers) {
        return new Result("PARSED", pageCount, List.copyOf(blocks), List.copyOf(ocrPageNumbers), false, false);
    }

    private Result rejected() {
        return new Result("REJECTED", 0, List.of(), List.of(), false, false);
    }

    private enum Format {
        MARKDOWN, TEXT, PDF, DOC, DOCX
    }

    public record Result(String disposition, int pageCount, List<Block> blocks, List<Integer> ocrPageNumbers,
                         boolean executedActiveContent, boolean fetchedRemoteResources) {
    }

    public record Block(int order, int pageNumber, String sectionPath, String content) {
    }
}

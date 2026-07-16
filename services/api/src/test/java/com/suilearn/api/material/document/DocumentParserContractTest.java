package com.suilearn.api.material.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/**
 * Contract for Task 3.2.  The production parser is intentionally absent while this test is RED.
 *
 * <p>Task 3.2 supplies {@code com.suilearn.api.material.document.DocumentParser} with a public
 * no-argument constructor and {@code parse(byte[], String, String)}.  Its result exposes
 * {@code disposition()}, {@code pageCount()}, {@code blocks()}, {@code ocrPageNumbers()},
 * {@code executedActiveContent()}, and {@code fetchedRemoteResources()}.  Each block exposes
 * {@code order()}, {@code pageNumber()}, {@code sectionPath()}, and {@code content()}.
 */
class DocumentParserContractTest {
    private static final String PARSER_CLASS = "com.suilearn.api.material.document.DocumentParser";

    @Test
    void providesTheDocumentParserContract() {
        assertThat(parserType())
            .as("Task 3.2 must provide the document parser contract")
            .isNotNull();
    }

    @Test
    void parsesMarkdownIntoOrderedHeadingBlocksWithoutExecutingMarkup() throws Exception {
        var result = parse(resourceFixture("lesson.md", "text/markdown"));

        assertThat(result.disposition()).isEqualTo("PARSED");
        assertThat(result.blocks()).extracting(Block::content)
            .contains("Queue ordering", "FIFO queues preserve arrival order for messages in the same queue.");
        assertThat(result.blocks()).extracting(Block::sectionPath)
            .contains("Queue ordering", "Queue ordering > Retry boundary");
        assertThat(result.executedActiveContent()).isFalse();
        assertThat(result.fetchedRemoteResources()).isFalse();
    }

    @Test
    void parsesMarkdownThroughCommonMarkWithoutPersistingRawHtmlOrDangerousUrls() throws Exception {
        var result = parse(new Fixture("unsafe.md", "text/markdown", ("# Safe heading\n\n"
            + "<script>alert('xss')</script><img src=\"https://evil.example/pixel\">\n\n"
            + "[safe link text](javascript:alert('xss'))").getBytes(StandardCharsets.UTF_8)));

        assertThat(result.disposition()).isEqualTo("PARSED");
        assertThat(result.blocks()).extracting(Block::content)
            .allSatisfy(content -> assertThat(content).doesNotContain("<script", "<img", "javascript:", "https://evil.example"));
        assertThat(result.blocks()).extracting(Block::content).contains("Safe heading", "safe link text");
        assertThat(result.executedActiveContent()).isFalse();
        assertThat(result.fetchedRemoteResources()).isFalse();
    }

    @Test
    void parsesTxtAsOneOrderedTextBlock() throws Exception {
        var result = parse(resourceFixture("lesson.txt", "text/plain"));

        assertThat(result.disposition()).isEqualTo("PARSED");
        assertThat(result.pageCount()).isEqualTo(1);
        assertThat(result.blocks()).singleElement().satisfies(block -> {
            assertThat(block.order()).isEqualTo(0);
            assertThat(block.pageNumber()).isEqualTo(1);
            assertThat(block.content()).contains("Plain text has no heading syntax.");
        });
    }

    @Test
    void streamsPathBasedTxtParsingUpToTheConfiguredAdmissionLimit() throws Exception {
        Path oversized = Files.createTempFile("suilearn-parser-stream-", ".txt");
        try {
            Files.writeString(oversized, "x".repeat(2 * 1024 * 1024), StandardCharsets.UTF_8);

            var result = new DocumentParser().parse(oversized, "oversized.txt", "text/plain");

            assertThat(result.disposition()).isEqualTo("PARSED");
            assertThat(result.blocks().stream().mapToInt(block -> block.content().length()).sum()).isEqualTo(2 * 1024 * 1024);
            String source = Files.readString(Path.of("src/main/java/com/suilearn/api/material/document/DocumentParser.java"));
            assertThat(source).doesNotContain("Files.readString(file, StandardCharsets.UTF_8)");
            assertThat(source).doesNotContain("readNBytes(MAX_TEXT_PARSE_BYTES + 1)");
        } finally {
            Files.deleteIfExists(oversized);
        }
    }

    @Test
    void extractsTextPdfWithoutSchedulingOcr() throws Exception {
        var result = parse(pdfFixture("text.pdf", List.of("Text PDF page one"), "application/pdf"));

        assertThat(result.disposition()).isEqualTo("PARSED");
        assertThat(result.pageCount()).isEqualTo(1);
        assertThat(result.ocrPageNumbers()).isEmpty();
        assertThat(result.blocks()).extracting(Block::pageNumber).containsOnly(1);
        assertThat(result.blocks()).extracting(Block::content).anyMatch(content -> content.contains("Text PDF page one"));
    }

    @Test
    void schedulesOcrForEveryTextlessScannedPdfPage() throws Exception {
        var result = parse(pdfFixture("scanned.pdf", List.of(""), "application/pdf"));

        assertThat(result.disposition()).isEqualTo("PARSED");
        assertThat(result.pageCount()).isEqualTo(1);
        assertThat(result.ocrPageNumbers()).containsExactly(1);
    }

    @Test
    void schedulesOcrOnlyForTextlessPagesOfMixedPdf() throws Exception {
        var result = parse(pdfFixture("mixed.pdf", List.of("Digital first page", ""), "application/pdf"));

        assertThat(result.disposition()).isEqualTo("PARSED");
        assertThat(result.pageCount()).isEqualTo(2);
        assertThat(result.ocrPageNumbers()).containsExactly(2);
        assertThat(result.blocks()).extracting(Block::pageNumber).contains(1);
    }

    @Test
    void schedulesOcrForAPageBelowTheConfiguredTextDensityThreshold() throws Exception {
        var parser = new DocumentParser(40);
        var result = parser.parse(pdfFixture("sparse.pdf", List.of("too sparse"), "application/pdf").bytes(),
            "sparse.pdf", "application/pdf");

        assertThat(result.disposition()).isEqualTo("PARSED");
        assertThat(result.ocrPageNumbers()).containsExactly(1);
    }

    @Test
    void parsesOleDocAndDocxIntoStructuredBlocks() throws Exception {
        var legacyResult = parse(resourceFixture("apache-poi-word6.doc", "application/msword"));
        var docxResult = parse(docxFixture());

        assertThat(legacyResult.disposition()).isEqualTo("PARSED");
        assertThat(legacyResult.blocks()).isNotEmpty();
        assertThat(legacyResult.blocks()).extracting(Block::order).isSorted();
        assertThat(legacyResult.blocks()).extracting(Block::content).anyMatch(content -> !content.isBlank());
        assertThat(docxResult.disposition()).isEqualTo("PARSED");
        assertThat(docxResult.blocks()).extracting(Block::content).anyMatch(content -> content.contains("DOCX lesson"));
        assertThat(docxResult.executedActiveContent()).isFalse();
        assertThat(docxResult.fetchedRemoteResources()).isFalse();
    }

    @Test
    void rejectsDocxArchivesThatExceedTheDecompressionBudget() throws Exception {
        var result = parse(docxWithAdditionalEntry("word/media/large.bin", 51 * 1024 * 1024));

        assertThat(result.disposition()).isEqualTo("REJECTED");
    }

    @Test
    void rejectsDocxArchivesWithExcessivelyNestedEntryPaths() throws Exception {
        var result = parse(docxWithAdditionalEntry("word/media/a/b/c/d/e/f/g/h/i/payload.bin", 16));

        assertThat(result.disposition()).isEqualTo("REJECTED");
    }

    @Test
    void rejectsDocxArchivesWithDuplicateEntryNamesBelowTheEntryLimit() throws Exception {
        assertThat(isSafeDocxArchive(docxWithDuplicateEntries(3).bytes())).isFalse();
    }

    private static boolean isSafeDocxArchive(byte[] bytes) throws Exception {
        var method = DocumentParser.class.getDeclaredMethod("isSafeDocxArchive", java.io.InputStream.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(new DocumentParser(), new ByteArrayInputStream(bytes));
    }

    @Test
    void rejectsDamagedOrExtensionMimeSignatureMismatchedInputWithoutReadyOutcome() throws Exception {
        var damaged = parse(new Fixture("damaged.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8)));
        var forged = parse(new Fixture("misleading.pdf", "application/pdf", docxFixture().bytes()));

        assertThat(damaged.disposition()).isEqualTo("REJECTED");
        assertThat(forged.disposition()).isEqualTo("REJECTED");
    }

    private static Class<?> parserType() {
        try {
            return Class.forName(PARSER_CLASS);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Result parse(Fixture fixture) throws Exception {
        var type = parserType();
        assertThat(type).as("Task 3.2 parser is required for fixture %s", fixture.fileName()).isNotNull();
        var parser = type.getConstructor().newInstance();
        var value = type.getMethod("parse", byte[].class, String.class, String.class)
            .invoke(parser, fixture.bytes(), fixture.fileName(), fixture.declaredMimeType());
        return new Result(
            (String) invoke(value, "disposition"),
            (Integer) invoke(value, "pageCount"),
            blocks(invoke(value, "blocks")),
            ((List<?>) invoke(value, "ocrPageNumbers")).stream().map(valueAt -> (Integer) valueAt).toList(),
            (Boolean) invoke(value, "executedActiveContent"),
            (Boolean) invoke(value, "fetchedRemoteResources")
        );
    }

    private static List<Block> blocks(Object value) {
        return ((List<?>) value).stream().map(block -> new Block(
            (Integer) invoke(block, "order"),
            (Integer) invoke(block, "pageNumber"),
            (String) invoke(block, "sectionPath"),
            (String) invoke(block, "content")
        )).toList();
    }

    private static Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Task 3.2 result is missing " + methodName + "()", exception);
        }
    }

    private static Fixture resourceFixture(String name, String mimeType) throws IOException, URISyntaxException {
        Path resource = Path.of(DocumentParserContractTest.class.getResource("/material-parser/" + name).toURI());
        return new Fixture(name, mimeType, Files.readAllBytes(resource));
    }

    private static Fixture pdfFixture(String fileName, List<String> pageText, String mimeType) throws IOException {
        try (var document = new PDDocument(); var bytes = new ByteArrayOutputStream()) {
            for (String text : pageText) {
                document.addPage(new PDPage());
                if (!text.isBlank()) {
                    try (var stream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages() - 1))) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(72, 720);
                        stream.showText(text);
                        stream.endText();
                    }
                }
            }
            document.save(bytes);
            return new Fixture(fileName, mimeType, bytes.toByteArray());
        }
    }

    private static Fixture docxFixture() throws IOException {
        try (var document = new XWPFDocument(); var bytes = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("DOCX lesson");
            document.write(bytes);
            return new Fixture("lesson.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes.toByteArray());
        }
    }

    private static Fixture docxWithAdditionalEntry(String entryName, int bytes) throws IOException {
        try (var output = new ByteArrayOutputStream(); var zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"/>"
                .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(entryName));
            byte[] buffer = new byte[8192];
            for (int remaining = bytes; remaining > 0;) {
                int written = Math.min(remaining, buffer.length);
                zip.write(buffer, 0, written);
                remaining -= written;
            }
            zip.closeEntry();
            return new Fixture("unsafe.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", output.toByteArray());
        }
    }

    private static Fixture docxWithDuplicateEntries(int count) throws IOException {
        try (var output = new ByteArrayOutputStream(); var zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"/>"
                .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (int index = 0; index < count; index++) {
                zip.putNextEntry(new ZipEntry("word/media/" + String.format("%05d", index) + ".bin"));
                zip.closeEntry();
            }
            zip.finish();
            String archive = output.toString(StandardCharsets.ISO_8859_1)
                .replaceAll("word/media/[0-9]{5}\\.bin", "word/media/00000.bin");
            return new Fixture("duplicate-entries.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                archive.getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private record Fixture(String fileName, String declaredMimeType, byte[] bytes) {
    }

    private record Result(String disposition, int pageCount, List<Block> blocks, List<Integer> ocrPageNumbers,
                          boolean executedActiveContent, boolean fetchedRemoteResources) {
    }

    private record Block(int order, int pageNumber, String sectionPath, String content) {
    }
}

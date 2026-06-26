package com.suilearn.api.material;

import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DefaultMaterialChunker implements MaterialChunker {
    private static final int TARGET_TOKENS = 520;
    private static final int MIN_TOKENS = 120;
    private static final int MAX_TOKENS = 760;
    private static final int OVERLAP_TOKENS = 80;
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*(?:[-*+]\\s+|\\d+[.)]\\s+).+");

    @Override
    public List<MaterialChunk> chunk(LearningMaterial material) {
        var blocks = blocks(material.content());
        var chunks = new ArrayList<MaterialChunk>();
        var current = new StringBuilder();
        for (var block : blocks) {
            var blockTokens = tokenCount(block);
            if (blockTokens > MAX_TOKENS) {
                flush(material, chunks, current);
                for (var window : windows(block, TARGET_TOKENS, OVERLAP_TOKENS)) {
                    chunks.add(newMaterialChunk(material, window, chunks.size()));
                }
                continue;
            }
            if (!current.isEmpty()
                && tokenCount(current.toString()) >= MIN_TOKENS
                && tokenCount(current + "\n\n" + block) > TARGET_TOKENS) {
                flush(material, chunks, current);
            }
            appendBlock(current, block);
        }
        flush(material, chunks, current);
        if (chunks.isEmpty() && material.content() != null && !material.content().isBlank()) {
            chunks.add(newMaterialChunk(material, material.content().trim(), 0));
        }
        return chunks;
    }

    private List<String> blocks(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        var blocks = new ArrayList<String>();
        var headingPath = new ArrayList<String>();
        var paragraph = new StringBuilder();
        var code = new StringBuilder();
        var inCode = false;
        for (var rawLine : content.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            var line = rawLine.stripTrailing();
            if (line.trim().startsWith("```")) {
                if (inCode) {
                    code.append(line).append('\n');
                    blocks.add(withHeading(headingPath, code.toString().trim()));
                    code.setLength(0);
                    inCode = false;
                } else {
                    flushBlock(blocks, headingPath, paragraph);
                    inCode = true;
                    code.append(line).append('\n');
                }
                continue;
            }
            if (inCode) {
                code.append(line).append('\n');
                continue;
            }
            var heading = HEADING.matcher(line.trim());
            if (heading.matches()) {
                flushBlock(blocks, headingPath, paragraph);
                var level = heading.group(1).length();
                while (headingPath.size() >= level) {
                    headingPath.remove(headingPath.size() - 1);
                }
                headingPath.add(heading.group(2).trim());
                continue;
            }
            if (line.isBlank()) {
                flushBlock(blocks, headingPath, paragraph);
                continue;
            }
            if (!paragraph.isEmpty() && !isContinuationLine(line)) {
                paragraph.append('\n');
            } else if (!paragraph.isEmpty()) {
                paragraph.append('\n');
            }
            paragraph.append(line.trim());
        }
        if (inCode && !code.isEmpty()) {
            blocks.add(withHeading(headingPath, code.toString().trim()));
        }
        flushBlock(blocks, headingPath, paragraph);
        return blocks;
    }

    private boolean isContinuationLine(String line) {
        var trimmed = line.trim();
        return LIST_ITEM.matcher(trimmed).matches() || trimmed.startsWith("|");
    }

    private void flushBlock(List<String> blocks, List<String> headingPath, StringBuilder paragraph) {
        if (!paragraph.isEmpty()) {
            blocks.add(withHeading(headingPath, paragraph.toString().trim()));
            paragraph.setLength(0);
        }
    }

    private String withHeading(List<String> headingPath, String content) {
        if (content.isBlank() || headingPath.isEmpty()) {
            return content;
        }
        return String.join(" > ", headingPath) + "\n\n" + content;
    }

    private void appendBlock(StringBuilder current, String block) {
        if (block == null || block.isBlank()) {
            return;
        }
        if (!current.isEmpty()) {
            current.append("\n\n");
        }
        current.append(block.trim());
    }

    private void flush(LearningMaterial material, List<MaterialChunk> chunks, StringBuilder current) {
        if (!current.isEmpty()) {
            chunks.add(newMaterialChunk(material, current.toString().trim(), chunks.size()));
            current.setLength(0);
        }
    }

    private List<String> windows(String content, int targetTokens, int overlapTokens) {
        var tokens = Arrays.stream(content.split("\\s+"))
            .filter(token -> !token.isBlank())
            .toList();
        if (tokens.size() <= targetTokens) {
            return List.of(content.trim());
        }
        var values = new ArrayList<String>();
        var start = 0;
        var step = Math.max(1, targetTokens - overlapTokens);
        while (start < tokens.size()) {
            var end = Math.min(tokens.size(), start + targetTokens);
            values.add(String.join(" ", tokens.subList(start, end)));
            if (end == tokens.size()) {
                break;
            }
            start += step;
        }
        return values;
    }

    private int tokenCount(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        var count = 0;
        var inWord = false;
        for (var index = 0; index < content.length();) {
            var codePoint = content.codePointAt(index);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                count++;
                inWord = false;
            } else if (Character.isLetterOrDigit(codePoint)) {
                if (!inWord) {
                    count++;
                    inWord = true;
                }
            } else {
                inWord = false;
            }
            index += Character.charCount(codePoint);
        }
        return count;
    }

    private MaterialChunk newMaterialChunk(LearningMaterial material, String content, int ordinal) {
        var chunkId = newId("chunk");
        return new MaterialChunk(chunkId, material.id(), content, ordinal, new SourceRef(
            SourceType.MATERIAL_CHUNK,
            chunkId,
            material.knowledgeBaseId(),
            material.title(),
            material.id(),
            chunkId,
            material.status() == MaterialStatus.DELETED,
            truncate(content)
        ));
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 160) {
            return value;
        }
        return value.substring(0, 160);
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

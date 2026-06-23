package com.suilearn.api.knowledgepoint.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class KnowledgePointCandidateExtractor {
    private static final int MAX_CANDIDATES = 16;
    private static final Pattern TECH_TOKEN = Pattern.compile("[A-Za-z][A-Za-z0-9+#]*(?:[._-][A-Za-z0-9+#]+)*");
    private static final Pattern MARKDOWN_PREFIX = Pattern.compile("^(?:#{1,6}\\s*|[-*+>]\\s+|\\d+[.)]\\s+)+");
    private static final Pattern WRAPPER_CHARS = Pattern.compile("^[`*_\\[\\]【】（）()<>《》\"'“”‘’]+|[`*_\\[\\]【】（）()<>《》\"'“”‘’]+$");
    private static final Pattern SEPARATOR_ONLY = Pattern.compile("[-_=~—–]{2,}");
    private static final Pattern SENTENCE_PUNCTUATION = Pattern.compile("[。！？!?；;，,、]");

    private KnowledgePointCandidateExtractor() {
    }

    public static List<String> extract(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        var candidates = new LinkedHashMap<String, String>();
        for (var rawLine : content.replace('\u00A0', ' ').split("\\R")) {
            var line = cleanLine(rawLine);
            if (line.isBlank() || isMarkdownSeparator(line)) {
                continue;
            }
            addCompactTopic(candidates, line);
            for (var segment : line.split("[\\s:：/|]+")) {
                addCandidate(candidates, segment);
            }
            var matcher = TECH_TOKEN.matcher(line);
            while (matcher.find()) {
                addCandidate(candidates, matcher.group());
            }
            if (candidates.size() >= MAX_CANDIDATES) {
                break;
            }
        }
        return new ArrayList<>(candidates.values()).stream()
            .limit(MAX_CANDIDATES)
            .toList();
    }

    private static String cleanLine(String rawLine) {
        var line = rawLine == null ? "" : rawLine.trim();
        line = MARKDOWN_PREFIX.matcher(line).replaceFirst("");
        line = line.replace("**", "").replace("__", "").replace("`", "").trim();
        return WRAPPER_CHARS.matcher(line).replaceAll("").trim();
    }

    private static void addCompactTopic(LinkedHashMap<String, String> candidates, String line) {
        if (line.contains(" ") || line.length() > 18) {
            return;
        }
        addCandidate(candidates, line);
    }

    private static void addCandidate(LinkedHashMap<String, String> candidates, String rawTerm) {
        if (candidates.size() >= MAX_CANDIDATES) {
            return;
        }
        var term = sanitize(rawTerm);
        if (!isCandidate(term)) {
            return;
        }
        candidates.putIfAbsent(normalizeKey(term), term);
    }

    private static String sanitize(String rawTerm) {
        var term = rawTerm == null ? "" : rawTerm.trim();
        term = WRAPPER_CHARS.matcher(term).replaceAll("").trim();
        term = term.replaceAll("[()（）]+$", "").trim();
        return term;
    }

    private static boolean isCandidate(String term) {
        if (term.length() < 2 || term.length() > 32) {
            return false;
        }
        if (!containsMeaningfulCharacter(term)) {
            return false;
        }
        if (isMarkdownSeparator(term) || SENTENCE_PUNCTUATION.matcher(term).find()) {
            return false;
        }
        return !term.contains("——") && !term.contains("--");
    }

    private static boolean isMarkdownSeparator(String term) {
        return SEPARATOR_ONLY.matcher(term).matches();
    }

    private static boolean containsMeaningfulCharacter(String term) {
        return term.codePoints().anyMatch(codePoint ->
            Character.isLetterOrDigit(codePoint) || Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN
        );
    }

    private static String normalizeKey(String term) {
        return term.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}

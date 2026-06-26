package com.suilearn.api.knowledgepoint.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class KnowledgePointCandidateExtractor {
    private static final int MAX_CANDIDATES = 16;
    private static final Pattern TECH_TOKEN = Pattern.compile("[A-Za-z][A-Za-z0-9+#]*(?:[._-][A-Za-z0-9+#]+)*");
    private static final Pattern COMPACT_TECH_PHRASE = Pattern.compile(
        "[A-Za-z][A-Za-z0-9+#._-]*(?:\\s+[A-Za-z][A-Za-z0-9+#._-]*){1,3}"
    );
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]{2,64})]\\([^)]*\\)");
    private static final Pattern MARKDOWN_PREFIX = Pattern.compile("^(?:#{1,6}\\s*|[-*+>]\\s+|\\d+[.)]\\s+)+");
    private static final Pattern WRAPPER_CHARS = Pattern.compile("^[`*_\\[\\](){}<>\"']+|[`*_\\[\\](){}<>\"']+$");
    private static final Pattern TRAILING_WRAPPERS = Pattern.compile("[()]+$");
    private static final Pattern SEPARATOR_ONLY = Pattern.compile("[-_=~*#]{2,}");
    private static final Set<String> ALLOWED_SHORT_TERMS = Set.of(
        "api", "sql", "tcp", "udp", "jvm", "jdk", "jre", "io", "nio", "gc", "ip", "ui", "ux",
        "cpu", "ram", "dns", "tls", "ssl", "jwt", "orm", "mvc", "css", "xml", "json"
    );
    private static final Set<String> GENERIC_SINGLETONS = Set.of(
        "java", "markdown", "pdf", "txt", "doc", "docx", "ppt", "pptx", "http", "https", "www"
    );

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
            for (var segment : line.split("[\\s:|]+")) {
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

    public static String sanitizeName(String rawTerm) {
        var term = rawTerm == null ? "" : rawTerm.trim();
        term = MARKDOWN_LINK.matcher(term).replaceAll("$1");
        term = term.replace('\u00A0', ' ').replaceAll("\\s+", " ");
        term = WRAPPER_CHARS.matcher(term).replaceAll("").trim();
        term = TRAILING_WRAPPERS.matcher(term).replaceAll("").trim();
        return term;
    }

    public static boolean isUsableName(String term) {
        if (term.length() < 2 || term.length() > 32) {
            return false;
        }
        if (!containsMeaningfulCharacter(term)) {
            return false;
        }
        if (isMarkdownSeparator(term) || looksLikeMarkdownOrUrlFragment(term)) {
            return false;
        }
        return !isShortIdentifierLike(term)
            && !isGenericSingleton(term)
            && !hasDisallowedPunctuation(term)
            && !term.contains("--");
    }

    public static String normalizeKey(String term) {
        return term.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static String cleanLine(String rawLine) {
        var line = sanitizeName(rawLine);
        line = MARKDOWN_PREFIX.matcher(line).replaceFirst("");
        line = line.replace("**", "").replace("__", "").replace("`", "").trim();
        return sanitizeName(line);
    }

    private static void addCompactTopic(LinkedHashMap<String, String> candidates, String line) {
        if (line.length() > 32) {
            return;
        }
        if (line.contains(" ")) {
            if (COMPACT_TECH_PHRASE.matcher(line).matches()
                && line.split("\\s+").length <= 4
                && line.replaceAll("\\b[A-Za-z0-9]{2,3}\\b", "").matches(".*[A-Za-z]{4,}.*")) {
                addCandidate(candidates, line);
            }
            return;
        }
        if (line.length() > 18) {
            return;
        }
        addCandidate(candidates, line);
    }

    private static void addCandidate(LinkedHashMap<String, String> candidates, String rawTerm) {
        if (candidates.size() >= MAX_CANDIDATES) {
            return;
        }
        var term = sanitizeName(rawTerm);
        if (!isUsableName(term)) {
            return;
        }
        candidates.putIfAbsent(normalizeKey(term), term);
    }

    private static boolean isMarkdownSeparator(String term) {
        return SEPARATOR_ONLY.matcher(term).matches();
    }

    private static boolean looksLikeMarkdownOrUrlFragment(String term) {
        var lower = term.toLowerCase(Locale.ROOT);
        return lower.contains("://")
            || lower.contains("http")
            || lower.contains("](")
            || term.contains("[")
            || term.contains("]");
    }

    private static boolean isShortIdentifierLike(String term) {
        if (term.length() > 3 || !term.matches("[A-Za-z0-9]+")) {
            return false;
        }
        return !ALLOWED_SHORT_TERMS.contains(term.toLowerCase(Locale.ROOT));
    }

    private static boolean isGenericSingleton(String term) {
        return GENERIC_SINGLETONS.contains(term.toLowerCase(Locale.ROOT));
    }

    private static boolean hasDisallowedPunctuation(String term) {
        if (TECH_TOKEN.matcher(term).matches()) {
            return false;
        }
        return term.codePoints().anyMatch(codePoint -> {
            var type = Character.getType(codePoint);
            return type == Character.OTHER_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION;
        });
    }

    private static boolean containsMeaningfulCharacter(String term) {
        return term.codePoints().anyMatch(codePoint ->
            Character.isLetterOrDigit(codePoint) || Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN
        );
    }
}

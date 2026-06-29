package com.suilearn.api.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 统一的 text-only 检索分词器。
 *
 * <p>对中文按 code point 产出单字，并把相邻汉字组合成 bigram；对拉丁字母和数字
 * 产出连续词元。同一套 token 口径同时用于：
 *
 * <ul>
 *   <li>chunk 写入时生成 {@code search_text}（{@link #searchText}）；</li>
 *   <li>检索召回时生成 PostgreSQL {@code to_tsquery} 输入（{@link #tsquery}）；</li>
 *   <li>Java BM25 fallback 的词频统计（{@link #tokens}）。</li>
 * </ul>
 *
 * 三处共用保证写入、召回与打分的分词完全一致。
 */
@Component
public class TextSearchTokenizer {

    /** 提取去重后的检索 token（中文单字 + 相邻 bigram + 拉丁/数字词元）。 */
    public List<String> tokens(String text) {
        var normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        var values = new ArrayList<String>();
        var token = new StringBuilder();
        String previousHan = null;
        for (var index = 0; index < normalized.length();) {
            var codePoint = normalized.codePointAt(index);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                flushToken(values, token);
                var current = new String(Character.toChars(codePoint));
                values.add(current);
                if (previousHan != null) {
                    values.add(previousHan + current);
                }
                previousHan = current;
            } else if (Character.isLetterOrDigit(codePoint)) {
                token.appendCodePoint(codePoint);
                previousHan = null;
            } else {
                flushToken(values, token);
                previousHan = null;
            }
            index += Character.charCount(codePoint);
        }
        flushToken(values, token);
        return values.stream().distinct().toList();
    }

    /** 生成持久化到 {@code search_text} 列的空格分隔 token 串。 */
    public String searchText(String content) {
        return String.join(" ", tokens(content));
    }

    /**
     * 生成 {@code to_tsquery('simple', ...)} 的输入，token 之间为 OR 语义以保召回，
     * 最终排序交给 {@code ts_rank_cd} 与 BM25。每个 token 用单引号包裹为 lexeme，
     * token 仅含小写字母数字或单个 CJK 字符，不含 tsquery 操作符，避免语法错误与注入。
     *
     * @return 形如 {@code '机器' | '学习' | 'hashmap'} 的字符串；无 token 时返回空串。
     */
    public String tsquery(String query) {
        var tokens = tokens(query);
        if (tokens.isEmpty()) {
            return "";
        }
        var builder = new StringBuilder();
        for (var token : tokens) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append('\'').append(token).append('\'');
        }
        return builder.toString();
    }

    private void flushToken(List<String> values, StringBuilder token) {
        if (!token.isEmpty()) {
            values.add(token.toString());
            token.setLength(0);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}

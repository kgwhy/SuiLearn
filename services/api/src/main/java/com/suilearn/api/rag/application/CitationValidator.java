package com.suilearn.api.rag.application;

import com.suilearn.api.ai.AiProvider.GeneratedAnswer;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CitationValidator {
    private static final Pattern CITATION = Pattern.compile("\\[(\\d+)]");

    public ValidationResult validate(GeneratedAnswer answer, int evidenceCount) {
        if (answer == null || answer.answer() == null || answer.answer().isBlank()) {
            return ValidationResult.failed("资料中没有足够证据生成回答。");
        }
        if (answer.uncertain()) {
            return ValidationResult.ok();
        }
        var cited = citedNumbers(answer.answer());
        if (cited.isEmpty()) {
            return ValidationResult.failed("回答没有引用任何证据。");
        }
        if (cited.stream().anyMatch(number -> number < 1 || number > evidenceCount)) {
            return ValidationResult.failed("回答引用了不存在的证据编号。");
        }
        if (answer.statements() != null && !answer.statements().isEmpty()) {
            for (var statement : answer.statements()) {
                if (statement.text() == null || statement.text().isBlank()) {
                    continue;
                }
                if (statement.citations() == null || statement.citations().isEmpty()) {
                    return ValidationResult.failed("回答中的结论缺少证据引用。");
                }
                if (statement.citations().stream().anyMatch(number -> number < 1 || number > evidenceCount)) {
                    return ValidationResult.failed("回答中的结论引用了不存在的证据编号。");
                }
            }
        }
        return ValidationResult.ok();
    }

    private List<Integer> citedNumbers(String answer) {
        var matcher = CITATION.matcher(answer);
        var values = new java.util.ArrayList<Integer>();
        while (matcher.find()) {
            values.add(Integer.parseInt(matcher.group(1)));
        }
        return values;
    }

    public record ValidationResult(boolean valid, String reason) {
        static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        static ValidationResult failed(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}

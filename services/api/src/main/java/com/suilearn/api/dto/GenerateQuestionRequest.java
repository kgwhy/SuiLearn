package com.suilearn.api.dto;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
/** Deprecated generic request retained for one compatibility cycle. */
public record GenerateQuestionRequest(
    @NotBlank String knowledgeBaseId, @NotEmpty List<SourceRef> sourceRefs, SourceType sourceType, String sourceId,
    QuestionType questionType, String categoryId, String categoryName, List<String> knowledgePointIds, String prompt
) {}

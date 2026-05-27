package com.suilearn.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.dto.CreateKnowledgeBaseRequest;
import com.suilearn.api.dto.GenerateExplanationRequest;
import com.suilearn.api.dto.GenerateQuestionRequest;
import com.suilearn.api.dto.GenerateReviewSuggestionRequest;
import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.dto.ReviewGeneratedContentRequest;
import com.suilearn.api.dto.SaveAiNoteRequest;
import com.suilearn.api.model.AiNoteType;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class SuiLearnV2ServiceTest {
    private final SuiLearnV2Service service = new SuiLearnV2Service(
        Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void keepsKnowledgeBaseScopedContentPendingUntilSavedAndMarksDeletedMaterialSources() {
        var javaKb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var dbKb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Database", "SQL notes"));

        var javaMaterial = service.importMaterial(javaKb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap uses buckets. HashMap collision handling uses linked lists and trees."
        ));
        service.importMaterial(dbKb.id(), new ImportMaterialRequest(
            "MySQL Notes",
            null,
            MaterialSourceType.TXT,
            "MySQL index lookup commonly uses B+Tree structures."
        ));

        assertThat(service.search("HashMap", javaKb.id(), null)).hasSize(1);
        assertThat(service.search("HashMap", dbKb.id(), null)).isEmpty();
        assertThat(service.search("HashMap", null, javaMaterial.id())).hasSize(1);

        var certainAnswer = service.ask("HashMap collision", javaKb.id(), javaMaterial.id());
        assertThat(certainAnswer.uncertain()).isFalse();
        assertThat(certainAnswer.citations())
            .extracting(SourceRef::knowledgeBaseId)
            .containsOnly(javaKb.id());

        var unrelatedAnswer = service.ask("Redis persistence", javaKb.id(), null);
        assertThat(unrelatedAnswer.uncertain()).isTrue();

        var sourceRef = materialSourceRef(javaKb.id(), javaMaterial.id(), javaMaterial.title());
        var draft = service.generateQuestion(new GenerateQuestionRequest(
            javaKb.id(),
            List.of(sourceRef),
            null,
            null,
            QuestionType.SINGLE_CHOICE,
            "java-collections",
            "Java Collections",
            List.of("kp-hashmap"),
            "Generate one HashMap question"
        ));

        assertThat(draft.status()).isEqualTo(GeneratedContentStatus.PENDING_REVIEW);
        assertThat(draft.savedQuestionId()).isNull();
        assertThat(draft.categoryId()).isEqualTo("java-collections");
        assertThat(draft.categoryName()).isEqualTo("Java Collections");
        assertThat(draft.knowledgePointIds()).containsExactly("kp-hashmap");
        assertThat(service.listQuestions(javaKb.id())).isEmpty();

        var saved = service.reviewGeneratedContent(draft.id(), new ReviewGeneratedContentRequest(
            GeneratedContentStatus.SAVED,
            "How does HashMap handle collisions?",
            null,
            null,
            "Users can still edit the draft before saving.",
            null,
            null,
            null,
            null
        ));

        assertThat(saved.savedQuestionId()).isNotBlank();
        assertThat(saved.savedAt()).isNotNull();
        assertThat(service.listQuestions(javaKb.id()))
            .singleElement()
            .satisfies(question -> {
                assertThat(question.id()).isEqualTo(saved.savedQuestionId());
                assertThat(question.sourceRefs()).hasSize(1);
                assertThat(question.categoryId()).isEqualTo("java-collections");
                assertThat(question.categoryName()).isEqualTo("Java Collections");
                assertThat(question.knowledgePointIds()).containsExactly("kp-hashmap");
            });
        assertThat(service.getStatistics(javaKb.id()).questionCount()).isEqualTo(1);

        var pending = service.generateQuestion(new GenerateQuestionRequest(
            javaKb.id(),
            List.of(sourceRef),
            null,
            null,
            QuestionType.SINGLE_CHOICE,
            null,
            null,
            null,
            null
        ));
        var discarded = service.generateQuestion(new GenerateQuestionRequest(
            javaKb.id(),
            List.of(sourceRef),
            null,
            null,
            QuestionType.SINGLE_CHOICE,
            null,
            null,
            null,
            "discard me"
        ));
        service.reviewGeneratedContent(discarded.id(), new ReviewGeneratedContentRequest(
            GeneratedContentStatus.DISCARDED,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));

        assertThat(service.search("HashMap", javaKb.id(), null))
            .extracting("id")
            .doesNotContain(pending.id(), discarded.id());

        var deletion = service.deleteMaterial(javaMaterial.id(), null, null);

        assertThat(deletion.status()).isEqualTo(MaterialStatus.DELETED);
        assertThat(deletion.deletedPendingGeneratedContentCount()).isEqualTo(1);
        assertThat(service.listGeneratedContents(GeneratedContentStatus.DELETED))
            .extracting("id")
            .contains(pending.id());
        assertThat(service.listQuestions(javaKb.id()))
            .singleElement()
            .satisfies(question -> assertThat(question.sourceRefs().get(0).deleted()).isTrue());
        assertThat(service.ask("HashMap collision", javaKb.id(), javaMaterial.id()).uncertain()).isTrue();
    }

    @Test
    void rejectsSearchAndAskWithoutScope() {
        assertThatThrownBy(() -> service.search("HashMap", null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("scope");

        assertThatThrownBy(() -> service.ask("HashMap collision", null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("scope");
    }

    @Test
    void deletedOrDiscardedDraftCannotBeSaved() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap uses buckets."
        ));
        var sourceRef = materialSourceRef(kb.id(), material.id(), material.title());

        var deletedDraft = service.generateQuestion(generateQuestionRequest(kb.id(), sourceRef, null, null, null));
        service.deleteGeneratedContent(deletedDraft.id());
        assertThatThrownBy(() -> service.reviewGeneratedContent(deletedDraft.id(), saveRequest(null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be saved");

        var discardedDraft = service.generateQuestion(generateQuestionRequest(kb.id(), sourceRef, null, null, null));
        service.reviewGeneratedContent(discardedDraft.id(), new ReviewGeneratedContentRequest(
            GeneratedContentStatus.DISCARDED,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
        assertThatThrownBy(() -> service.reviewGeneratedContent(discardedDraft.id(), saveRequest(null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be saved");
    }

    @Test
    void deletedSourcesCannotBeUsedForNewAiGenerationOrNotes() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap buckets collision"
        ));
        var point = service.extractKnowledgePoints(material.id()).get(0);
        var sourceRef = materialSourceRef(kb.id(), material.id(), material.title());
        service.deleteMaterial(material.id(), null, null);

        assertThatThrownBy(() -> service.generateExplanation(new GenerateExplanationRequest(
            kb.id(),
            point.id(),
            List.of(sourceRef),
            null
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("deleted");

        assertThatThrownBy(() -> service.generateReviewSuggestion(new GenerateReviewSuggestionRequest(
            kb.id(),
            List.of(sourceRef),
            null,
            null,
            null
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("deleted");

        assertThatThrownBy(() -> service.saveAiNote(new SaveAiNoteRequest(
            null,
            kb.id(),
            AiNoteType.REVIEW_SUGGESTION,
            "Review",
            "Review content",
            List.of(sourceRef)
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("deleted");
    }

    @Test
    void reviewClassificationOverridesDraftWhenSavingQuestion() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap uses buckets."
        ));
        var draft = service.generateQuestion(generateQuestionRequest(
            kb.id(),
            materialSourceRef(kb.id(), material.id(), material.title()),
            "draft-category",
            "Draft Category",
            List.of("kp-draft")
        ));

        var saved = service.reviewGeneratedContent(
            draft.id(),
            saveRequest("review-category", "Review Category", List.of("kp-review"))
        );

        assertThat(service.listQuestions(kb.id()))
            .singleElement()
            .satisfies(question -> {
                assertThat(question.id()).isEqualTo(saved.savedQuestionId());
                assertThat(question.categoryId()).isEqualTo("review-category");
                assertThat(question.categoryName()).isEqualTo("Review Category");
                assertThat(question.knowledgePointIds()).containsExactly("kp-review");
            });
    }

    private static GenerateQuestionRequest generateQuestionRequest(
        String knowledgeBaseId,
        SourceRef sourceRef,
        String categoryId,
        String categoryName,
        List<String> knowledgePointIds
    ) {
        return new GenerateQuestionRequest(
            knowledgeBaseId,
            List.of(sourceRef),
            null,
            null,
            QuestionType.SINGLE_CHOICE,
            categoryId,
            categoryName,
            knowledgePointIds,
            null
        );
    }

    private static ReviewGeneratedContentRequest saveRequest(
        String categoryId,
        String categoryName,
        List<String> knowledgePointIds
    ) {
        return new ReviewGeneratedContentRequest(
            GeneratedContentStatus.SAVED,
            null,
            null,
            null,
            null,
            categoryId,
            categoryName,
            knowledgePointIds,
            null
        );
    }

    private static SourceRef materialSourceRef(String knowledgeBaseId, String materialId, String title) {
        return new SourceRef(
            SourceType.MATERIAL,
            materialId,
            knowledgeBaseId,
            title,
            materialId,
            null,
            false,
            null
        );
    }
}

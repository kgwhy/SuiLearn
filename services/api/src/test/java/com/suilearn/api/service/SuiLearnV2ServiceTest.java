package com.suilearn.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.ai.FakeAiProvider;
import com.suilearn.api.dto.CreateKnowledgeBaseRequest;
import com.suilearn.api.dto.GenerateExplanationRequest;
import com.suilearn.api.dto.GenerateQuestionRequest;
import com.suilearn.api.dto.GenerateReviewSuggestionRequest;
import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.dto.ReviewGeneratedContentRequest;
import com.suilearn.api.dto.SaveAiNoteRequest;
import com.suilearn.api.dto.UpdateKnowledgePointRequest;
import com.suilearn.api.generation.application.GeneratedContentService;
import com.suilearn.api.knowledgebase.application.KnowledgeBaseService;
import com.suilearn.api.knowledgepoint.application.KnowledgePointService;
import com.suilearn.api.material.DefaultMaterialChunker;
import com.suilearn.api.material.TextMaterialParser;
import com.suilearn.api.material.application.MaterialImportService;
import com.suilearn.api.model.AiNoteType;
import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.model.SearchResultType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.persistence.SuiLearnV2Store;
import com.suilearn.api.pack.application.LearningPackService;
import com.suilearn.api.retrieval.FakeEmbeddingProvider;
import com.suilearn.api.retrieval.KeywordRetriever;
import com.suilearn.api.retrieval.Retriever;
import com.suilearn.api.service.internal.SuiLearnV2Workflow;
import com.suilearn.api.source.application.SourceService;
import com.suilearn.api.task.application.TaskExecutor;
import com.suilearn.api.task.application.TaskService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:suilearn-v2-service-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
})
class SuiLearnV2ServiceTest {
    @Autowired
    private SuiLearnV2Service service;

    @Autowired
    private AiProviderStatusService providerStatusService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private MaterialImportService materialImportService;

    @Autowired
    private KnowledgePointService knowledgePointService;

    @Autowired
    private GeneratedContentService generatedContentService;

    @Autowired
    private LearningPackService learningPackService;

    @SpyBean
    private SuiLearnV2Store store;

    @SpyBean
    private SuiLearnV2Workflow workflow;

    @SpyBean
    private SourceService sourceService;

    @SpyBean
    private TaskExecutor taskExecutor;

    @SpyBean
    private TaskService taskService;

    @Autowired
    private Clock clock;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearDatabase() {
        store.deleteAll();
    }

    @AfterEach
    void clearStoreSpy() {
        clearInvocations(store, workflow, sourceService, taskExecutor, taskService);
    }

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
    void knowledgeBaseApplicationServiceDoesNotProxyWorkflowForDetails() {
        var kb = knowledgeBaseService.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        clearInvocations(workflow);

        var detail = knowledgeBaseService.getKnowledgeBaseDetail(kb.id());

        assertThat(detail.id()).isEqualTo(kb.id());
        assertThat(detail.materialCount()).isZero();
        assertThat(detail.knowledgePointCount()).isZero();
        verifyNoInteractions(workflow);
    }

    @Test
    void controllerFacingApplicationServicesDoNotProxyWorkflow() {
        var kb = knowledgeBaseService.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        clearInvocations(workflow);

        var material = materialImportService.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap buckets collision"
        ));
        var extraction = knowledgePointService.extractKnowledgePoints(material.id());
        var point = extraction.knowledgePoints().get(0);
        var updatedPoint = knowledgePointService.updateKnowledgePoint(
            point.id(),
            new UpdateKnowledgePointRequest("HashMap", "Java collection map")
        );
        var sourceRef = materialSourceRef(kb.id(), material.id(), material.title());
        var draft = generatedContentService.generateQuestion(generateQuestionRequest(
            kb.id(),
            sourceRef,
            "java-collections",
            "Java Collections",
            List.of(updatedPoint.id())
        ));
        var saved = generatedContentService.reviewGeneratedContent(draft.id(), saveRequest(null, null, null));
        generatedContentService.deleteGeneratedContent(draft.id());
        var pack = learningPackService.resolve(kb.id());

        assertThat(material.status()).isEqualTo(MaterialStatus.READY);
        assertThat(knowledgePointService.listKnowledgePoints(kb.id())).extracting("id").contains(updatedPoint.id());
        assertThat(generatedContentService.listGeneratedContents(GeneratedContentStatus.DELETED))
            .extracting("id")
            .contains(saved.id());
        assertThat(pack.knowledgeBaseId()).isEqualTo(kb.id());
        verifyNoInteractions(workflow);
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
        var point = service.extractKnowledgePoints(material.id()).knowledgePoints().get(0);
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

    @Test
    void persistsCoreDataAfterRecreatingService() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap uses buckets. Collision handling uses linked lists."
        ));
        var point = service.extractKnowledgePoints(material.id()).knowledgePoints().get(0);
        var draft = service.generateQuestion(generateQuestionRequest(
            kb.id(),
            materialSourceRef(kb.id(), material.id(), material.title()),
            "java-collections",
            "Java Collections",
            List.of(point.id())
        ));
        var saved = service.reviewGeneratedContent(draft.id(), saveRequest(null, null, null));
        service.saveAiNote(new SaveAiNoteRequest(
            null,
            kb.id(),
            AiNoteType.REVIEW_SUGGESTION,
            "Review",
            "Review HashMap collision handling.",
            List.of(materialSourceRef(kb.id(), material.id(), material.title()))
        ));

        var recreatedService = new SuiLearnV2Service(
            new FakeAiProvider(),
            new TextMaterialParser(),
            new DefaultMaterialChunker(),
            new FakeEmbeddingProvider(),
            keywordRetriever(),
            clock,
            store
        );

        assertThat(recreatedService.listKnowledgeBases()).extracting("id").contains(kb.id());
        assertThat(recreatedService.listMaterials(kb.id())).extracting("id").contains(material.id());
        assertThat(recreatedService.getMaterialDetail(material.id()).chunks()).isNotEmpty();
        assertThat(recreatedService.listKnowledgePoints(kb.id())).extracting("id").contains(point.id());
        assertThat(recreatedService.listGeneratedContents(null)).extracting("id").contains(draft.id());
        assertThat(recreatedService.listQuestions(kb.id())).extracting("id").contains(saved.savedQuestionId());
        assertThat(recreatedService.getKnowledgeBaseDetail(kb.id()).aiNoteCount()).isEqualTo(1);
    }

    @Test
    void fakeAiProviderReturnsStableQuestionExplanationAndReviewSuggestion() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap uses buckets. Collision handling uses linked lists."
        ));
        var point = service.extractKnowledgePoints(material.id()).knowledgePoints().get(0);
        var sourceRef = materialSourceRef(kb.id(), material.id(), material.title());

        var question = service.generateQuestion(generateQuestionRequest(
            kb.id(),
            sourceRef,
            "java-collections",
            "Java Collections",
            List.of(point.id())
        ));
        var explanation = service.generateExplanation(new GenerateExplanationRequest(
            kb.id(),
            point.id(),
            List.of(sourceRef),
            null
        ));
        var suggestion = service.generateReviewSuggestion(new GenerateReviewSuggestionRequest(
            kb.id(),
            List.of(sourceRef),
            List.of(point.id()),
            List.of(question.id()),
            null
        ));

        assertThat(question.stem()).isEqualTo("Fake AI question about HashMap Notes: which statement is most accurate?");
        assertThat(question.options()).containsExactly(
            "A. It should be checked against the cited source.",
            "B. It ignores source traceability.",
            "C. It should replace all existing questions automatically.",
            "D. It does not need user review."
        );
        assertThat(question.answer()).containsExactly("A");
        assertThat(question.explanation()).isEqualTo(
            "Fake AI explanation: review the cited source for HashMap Notes before saving this generated question."
        );
        assertThat(explanation.title()).isEqualTo(point.name() + " explanation");
        assertThat(explanation.content()).contains("Fake AI explanation for " + point.name());
        assertThat(suggestion.title()).isEqualTo("Weak knowledge point review suggestion");
        assertThat(suggestion.content()).contains("Fake AI review suggestion");
    }

    @Test
    void serviceUsesReplaceableAiProviderForGeneratedContent() {
        var customService = new SuiLearnV2Service(
            new TestAiProvider(),
            new TextMaterialParser(),
            new DefaultMaterialChunker(),
            new FakeEmbeddingProvider(),
            keywordRetriever(),
            clock,
            store
        );
        var kb = customService.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = customService.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap uses buckets."
        ));
        var sourceRef = materialSourceRef(kb.id(), material.id(), material.title());

        var draft = customService.generateQuestion(generateQuestionRequest(kb.id(), sourceRef, null, null, null));

        assertThat(draft.stem()).isEqualTo("Provider replacement question");
        assertThat(draft.options()).containsExactly("A. Custom provider option");
        assertThat(draft.answer()).containsExactly("A");
        assertThat(draft.explanation()).isEqualTo("Provider replacement explanation");
    }

    @Test
    void importMaterialPersistsUploadedParsingChunkingReadyStatusFlow() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        clearInvocations(store);

        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.PDF,
            "HashMap uses buckets.\n\nCollision handling uses linked lists."
        ));

        assertThat(material.status()).isEqualTo(MaterialStatus.READY);
        assertThat(service.getMaterialDetail(material.id()).chunks())
            .satisfiesExactly(
                chunk -> {
                    assertThat(chunk.content()).isEqualTo("HashMap uses buckets.");
                    assertThat(chunk.embedding()).hasSize(3);
                    assertThat(chunk.embedding()).startsWith(21.0, 3.0);
                    assertThat(chunk.embeddingStatus()).isEqualTo(EmbeddingStatus.READY);
                    assertThat(chunk.embeddingModel()).isEqualTo("fake-embedding-v1");
                    assertThat(chunk.embeddingDimensions()).isEqualTo(3);
                },
                chunk -> {
                    assertThat(chunk.content()).isEqualTo("Collision handling uses linked lists.");
                    assertThat(chunk.embedding()).hasSize(3);
                    assertThat(chunk.embedding()).startsWith(37.0, 5.0);
                    assertThat(chunk.embeddingStatus()).isEqualTo(EmbeddingStatus.READY);
                    assertThat(chunk.embeddingModel()).isEqualTo("fake-embedding-v1");
                    assertThat(chunk.embeddingDimensions()).isEqualTo(3);
                }
            );
        var detailJson = objectMapper.valueToTree(service.getMaterialDetail(material.id()));
        assertThat(detailJson.path("chunks").get(0).has("embedding")).isFalse();
        assertThat(detailJson.path("chunks").get(0).path("embeddingStatus").asText()).isEqualTo("READY");
        assertThat(detailJson.path("chunks").get(0).path("embeddingModel").asText()).isEqualTo("fake-embedding-v1");
        assertThat(detailJson.path("chunks").get(0).path("embeddingDimensions").asInt()).isEqualTo(3);

        InOrder statusFlow = inOrder(store);
        statusFlow.verify(store).saveMaterial(argThat(saved -> saved.status() == MaterialStatus.UPLOADED));
        statusFlow.verify(store).saveMaterial(argThat(saved -> saved.status() == MaterialStatus.PARSING));
        statusFlow.verify(store).saveMaterial(argThat(saved -> saved.status() == MaterialStatus.CHUNKING));
        statusFlow.verify(store).saveMaterial(argThat(saved -> saved.status() == MaterialStatus.INDEXING));
        statusFlow.verify(store).saveMaterial(argThat(saved -> saved.status() == MaterialStatus.READY));
    }

    @Test
    void importMaterialStoresFailedStatusWhenParserFails() {
        var failingService = new SuiLearnV2Service(
            new FakeAiProvider(),
            request -> {
                throw new IllegalStateException("parse failed");
            },
            new DefaultMaterialChunker(),
            new FakeEmbeddingProvider(),
            keywordRetriever(),
            clock,
            store
        );
        var kb = failingService.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));

        var material = failingService.importMaterial(kb.id(), new ImportMaterialRequest(
            "Broken Notes",
            null,
            MaterialSourceType.PDF,
            "unparseable text"
        ));

        assertThat(material.status()).isEqualTo(MaterialStatus.FAILED);
        assertThat(material.errorMessage()).contains("parse failed");
        assertThat(failingService.getTaskStatus(material.importTaskId()))
            .satisfies(task -> {
                assertThat(task.kind()).isEqualTo(TaskKind.MATERIAL_IMPORT);
                assertThat(task.status()).isEqualTo(TaskLifecycleStatus.FAILED);
                assertThat(task.errorMessage()).contains("parse failed");
            });
        assertThat(store.findMaterial(material.id()))
            .hasValueSatisfying(saved -> assertThat(saved.status()).isEqualTo(MaterialStatus.FAILED));
        assertThat(store.listChunksByMaterial(material.id())).isEmpty();
    }

    @Test
    void serviceUsesReplaceableRetrieverForSearchAndAsk() {
        var customService = new SuiLearnV2Service(
            new FakeAiProvider(),
            new TextMaterialParser(),
            new DefaultMaterialChunker(),
            new FakeEmbeddingProvider(),
            new TestRetriever(),
            clock,
            store
        );
        var kb = customService.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));

        assertThat(customService.search("anything", kb.id(), null))
            .singleElement()
            .satisfies(result -> {
                assertThat(result.id()).isEqualTo("custom_result");
                assertThat(result.type()).isEqualTo(SearchResultType.MATERIAL_CHUNK);
            });

        var answer = customService.ask("anything", kb.id(), null);

        assertThat(answer.uncertain()).isFalse();
        assertThat(answer.citations()).singleElement()
            .satisfies(ref -> assertThat(ref.id()).isEqualTo("custom_chunk"));
    }

    @Test
    void providerStatusReturnsSanitizedFakeProviderMetadata() {
        var status = providerStatusService.getStatus();
        var json = objectMapper.valueToTree(status);

        assertThat(status.providerType()).isEqualTo(AiProviderType.FAKE);
        assertThat(status.configured()).isTrue();
        assertThat(status.available()).isTrue();
        assertThat(status.chatModel()).isEqualTo("fake-chat-v1");
        assertThat(status.embeddingModel()).isEqualTo("fake-embedding-v1");
        assertThat(status.embeddingDimensions()).isEqualTo(3);
        assertThat(json.has("apiKey")).isFalse();
        assertThat(json.has("authorization")).isFalse();
        assertThat(json.has("header")).isFalse();
    }

    @Test
    void importMaterialCreatesSucceededImportAndEmbeddingTasks() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));

        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap uses buckets.\nCollision handling uses linked lists."
        ));

        assertThat(material.importTaskId()).isNotBlank();
        assertThat(material.embeddingTaskId()).isNotBlank();
        assertThat(service.getTaskStatus(material.importTaskId()))
            .satisfies(task -> {
                assertThat(task.kind()).isEqualTo(TaskKind.MATERIAL_IMPORT);
                assertThat(task.status()).isEqualTo(TaskLifecycleStatus.SUCCEEDED);
                assertThat(task.resultRef().type()).isEqualTo("MATERIAL");
                assertThat(task.resultRef().id()).isEqualTo(material.id());
            });
        assertThat(service.getTaskStatus(material.embeddingTaskId()))
            .satisfies(task -> {
                assertThat(task.kind()).isEqualTo(TaskKind.EMBEDDING);
                assertThat(task.status()).isEqualTo(TaskLifecycleStatus.SUCCEEDED);
                assertThat(task.model()).isEqualTo("fake-embedding-v1");
                assertThat(task.resultRef().type()).isEqualTo("MATERIAL_CHUNKS");
                assertThat(task.resultRef().count()).isEqualTo(2);
            });
    }

    @Test
    void importMaterialRunsThroughTaskExecutorTemplate() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        clearInvocations(taskExecutor, taskService);

        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap uses buckets.\nCollision handling uses linked lists."
        ));
        service.getTaskStatus(material.importTaskId());

        verify(taskService).createTask(
            eq(TaskKind.MATERIAL_IMPORT),
            eq(kb.id()),
            isNull(),
            isNull(),
            isNull(),
            eq("UPLOADED")
        );
        verify(taskService).createTask(
            eq(TaskKind.EMBEDDING),
            eq(kb.id()),
            eq(material.id()),
            isNull(),
            eq("fake-embedding-v1"),
            eq("INDEXING")
        );
        verify(taskExecutor).runManagedTask(
            argThat(task -> task.kind() == TaskKind.MATERIAL_IMPORT),
            eq("UPLOADED"),
            any(),
            any()
        );
        verify(taskExecutor).runManagedTask(
            argThat(task -> task.kind() == TaskKind.EMBEDDING),
            eq("INDEXING"),
            any(),
            any()
        );
        verify(taskService).getTaskStatus(material.importTaskId());
    }

    @Test
    void importMaterialFailureStoresFailedTaskAndMaterialError() {
        var failingService = new SuiLearnV2Service(
            new FakeAiProvider(),
            request -> {
                throw new IllegalStateException("parse failed before chunking");
            },
            new DefaultMaterialChunker(),
            new FakeEmbeddingProvider(),
            keywordRetriever(),
            clock,
            store
        );
        var kb = failingService.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));

        var material = failingService.importMaterial(kb.id(), new ImportMaterialRequest(
            "Broken Notes",
            null,
            MaterialSourceType.PDF,
            "unparseable text"
        ));

        assertThat(material.status()).isEqualTo(MaterialStatus.FAILED);
        assertThat(material.errorMessage()).contains("parse failed before chunking");
        assertThat(failingService.getTaskStatus(material.importTaskId()))
            .satisfies(task -> {
                assertThat(task.status()).isEqualTo(TaskLifecycleStatus.FAILED);
                assertThat(task.errorCode()).isEqualTo("MATERIAL_IMPORT_FAILED");
                assertThat(task.errorMessage()).contains("parse failed before chunking");
            });
    }

    @Test
    void aiGenerationCreatesSucceededTasksWithResultRefs() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap buckets collision"
        ));
        var extraction = service.extractKnowledgePoints(material.id());
        var point = extraction.knowledgePoints().get(0);
        var sourceRef = materialSourceRef(kb.id(), material.id(), material.title());

        var question = service.generateQuestion(generateQuestionRequest(kb.id(), sourceRef, null, null, List.of(point.id())));
        var explanation = service.generateExplanation(new GenerateExplanationRequest(kb.id(), point.id(), List.of(sourceRef), null));
        var suggestion = service.generateReviewSuggestion(new GenerateReviewSuggestionRequest(kb.id(), List.of(sourceRef), List.of(point.id()), List.of(question.id()), null));

        assertThat(store.listTasks())
            .anySatisfy(task -> {
                assertThat(task.kind()).isEqualTo(TaskKind.QUESTION_GENERATION);
                assertThat(task.status()).isEqualTo(TaskLifecycleStatus.SUCCEEDED);
                assertThat(task.generatedContentId()).isEqualTo(question.id());
                assertThat(task.resultRef().id()).isEqualTo(question.id());
            })
            .anySatisfy(task -> {
                assertThat(task.kind()).isEqualTo(TaskKind.EXPLANATION_GENERATION);
                assertThat(task.status()).isEqualTo(TaskLifecycleStatus.SUCCEEDED);
                assertThat(task.resultRef().id()).isEqualTo(explanation.id());
            })
            .anySatisfy(task -> {
                assertThat(task.kind()).isEqualTo(TaskKind.REVIEW_SUGGESTION_GENERATION);
                assertThat(task.status()).isEqualTo(TaskLifecycleStatus.SUCCEEDED);
                assertThat(task.resultRef().id()).isEqualTo(suggestion.id());
            });
    }

    @Test
    void aiGenerationDelegatesSourceNormalizationToSourceService() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap buckets collision"
        ));
        var sourceRef = new SourceRef(SourceType.MATERIAL, material.id(), null, null, null, null, false, null);
        clearInvocations(sourceService);

        var question = service.generateQuestion(generateQuestionRequest(kb.id(), sourceRef, null, null, null));

        assertThat(question.sourceRefs()).singleElement()
            .satisfies(normalized -> {
                assertThat(normalized.knowledgeBaseId()).isEqualTo(kb.id());
                assertThat(normalized.materialId()).isEqualTo(material.id());
                assertThat(normalized.title()).isEqualTo(material.title());
            });
        verify(sourceService).normalize(eq(kb.id()), eq(List.of(sourceRef)));
        verify(sourceService).ensureUsable(argThat(refs ->
            refs.size() == 1
                && kb.id().equals(refs.get(0).knowledgeBaseId())
                && material.id().equals(refs.get(0).materialId())
        ));
    }

    @Test
    void deletingMaterialInvalidatesChunkEmbeddingsAndExcludesSearch() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap buckets collision"
        ));

        assertThat(service.search("HashMap", kb.id(), null)).isNotEmpty();
        var deletion = service.deleteMaterial(material.id(), null, null);

        assertThat(deletion.invalidatedChunkCount()).isEqualTo(1);
        assertThat(store.listChunksByMaterial(material.id()))
            .singleElement()
            .satisfies(chunk -> {
                assertThat(chunk.embeddingStatus()).isEqualTo(EmbeddingStatus.INVALIDATED);
                assertThat(chunk.embedding()).isNull();
            });
        assertThat(service.search("HashMap", kb.id(), null)).isEmpty();
        assertThat(service.ask("HashMap", kb.id(), material.id()).uncertain()).isTrue();
    }

    @Test
    void statisticsUsePersistedCountsWithoutPlaceholders() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap buckets collision"
        ));
        var point = service.extractKnowledgePoints(material.id()).knowledgePoints().get(0);
        var sourceRef = materialSourceRef(kb.id(), material.id(), material.title());
        service.generateQuestion(generateQuestionRequest(kb.id(), sourceRef, null, null, List.of(point.id())));
        service.saveAiNote(new SaveAiNoteRequest(
            null,
            kb.id(),
            AiNoteType.REVIEW_SUGGESTION,
            "Review",
            "Review HashMap collision handling.",
            List.of(sourceRef)
        ));

        var statistics = service.getStatistics(kb.id());

        assertThat(statistics.materialCount()).isEqualTo(1);
        assertThat(statistics.readyMaterialCount()).isEqualTo(1);
        assertThat(statistics.knowledgePointCount()).isGreaterThanOrEqualTo(1);
        assertThat(statistics.pendingGeneratedContentCount()).isEqualTo(1);
        assertThat(statistics.savedAiNoteCount()).isEqualTo(1);
        assertThat(statistics.answerCount()).isZero();
        assertThat(statistics.correctRate()).isNull();
    }

    @Test
    void generatedQuestionAndAiNoteDraftsSerializeGenerationTaskId() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        var material = service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap buckets collision"
        ));
        var point = service.extractKnowledgePoints(material.id()).knowledgePoints().get(0);
        var sourceRef = materialSourceRef(kb.id(), material.id(), material.title());

        var question = service.generateQuestion(generateQuestionRequest(kb.id(), sourceRef, null, null, List.of(point.id())));
        var reviewed = service.reviewGeneratedContent(question.id(), saveRequest(null, null, null));
        var listed = service.listGeneratedContents(null).get(0);
        var explanation = service.generateExplanation(new GenerateExplanationRequest(kb.id(), point.id(), List.of(sourceRef), null));
        var suggestion = service.generateReviewSuggestion(new GenerateReviewSuggestionRequest(kb.id(), List.of(sourceRef), List.of(point.id()), List.of(question.id()), null));

        assertThat(question.generationTaskId()).isNotBlank();
        assertThat(reviewed.generationTaskId()).isEqualTo(question.generationTaskId());
        assertThat(listed.generationTaskId()).isEqualTo(question.generationTaskId());
        assertThat(objectMapper.valueToTree(question).path("generationTaskId").asText()).isEqualTo(question.generationTaskId());
        assertThat(explanation.generationTaskId()).isNotBlank();
        assertThat(suggestion.generationTaskId()).isNotBlank();
        assertThat(objectMapper.valueToTree(explanation).path("generationTaskId").asText()).isEqualTo(explanation.generationTaskId());
        assertThat(objectMapper.valueToTree(suggestion).path("generationTaskId").asText()).isEqualTo(suggestion.generationTaskId());
    }

    @Test
    void searchResultsSerializeRequiredScore() {
        var kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Java", "Interview notes"));
        service.importMaterial(kb.id(), new ImportMaterialRequest(
            "HashMap Notes",
            null,
            MaterialSourceType.MARKDOWN,
            "HashMap buckets collision"
        ));

        var result = service.search("HashMap", kb.id(), null).get(0);
        var json = objectMapper.valueToTree(result);

        assertThat(result.score()).isBetween(0.0, 1.0);
        assertThat(json.has("score")).isTrue();
        assertThat(json.path("score").isNumber()).isTrue();
        assertThat(json.path("score").asDouble()).isEqualTo(result.score());
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

    private KeywordRetriever keywordRetriever() {
        return new KeywordRetriever(new FakeEmbeddingProvider(), store);
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    private static class TestAiProvider implements AiProvider {
        @Override
        public GeneratedQuestion generateQuestion(QuestionGenerationPrompt prompt) {
            return new GeneratedQuestion(
                prompt.questionType(),
                prompt.categoryId(),
                prompt.categoryName(),
                prompt.knowledgePointIds(),
                "Provider replacement question",
                List.of("A. Custom provider option"),
                List.of("A"),
                "Provider replacement explanation"
            );
        }

        @Override
        public GeneratedNote generateKnowledgePointExplanation(KnowledgePointExplanationPrompt prompt) {
            return new GeneratedNote("Provider replacement explanation note", "Provider replacement explanation content");
        }

        @Override
        public GeneratedNote generateReviewSuggestion(ReviewSuggestionPrompt prompt) {
            return new GeneratedNote("Provider replacement review note", "Provider replacement review content");
        }
    }

    private static class TestRetriever implements Retriever {
        @Override
        public List<SearchResult> search(RetrievalRequest request) {
            return List.of(new SearchResult(
                "custom_result",
                SearchResultType.MATERIAL_CHUNK,
                "Custom result",
                "Custom summary",
                1.0,
                request.knowledgeBaseId(),
                List.of(),
                List.of(sourceRef(request))
            ));
        }

        @Override
        public List<MaterialChunk> retrieveEvidence(RetrievalRequest request, int limit) {
            return List.of(new MaterialChunk("custom_chunk", "custom_material", "Custom evidence", 0, sourceRef(request)));
        }

        private static SourceRef sourceRef(RetrievalRequest request) {
            return new SourceRef(
                SourceType.MATERIAL_CHUNK,
                "custom_chunk",
                request.knowledgeBaseId(),
                "Custom material",
                "custom_material",
                "custom_chunk",
                false,
                "Custom evidence"
            );
        }
    }
}

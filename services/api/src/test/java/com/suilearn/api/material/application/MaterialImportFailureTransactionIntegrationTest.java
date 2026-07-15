package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.dto.CreateKnowledgeBaseRequest;
import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.knowledgebase.application.KnowledgeBaseService;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.persistence.SuiLearnV2Store;
import com.suilearn.api.task.application.TaskService;
import com.suilearn.api.task.application.TaskRetryRoutingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
    "spring.datasource.url=${SUILEARN_TEST_DB_URL:jdbc:postgresql://localhost:5432/suilearn_test}",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.username=${SUILEARN_TEST_DB_USERNAME:suilearn}",
    "spring.datasource.password=${SUILEARN_TEST_DB_PASSWORD:suilearn_dev_password}",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "suilearn.ai.provider=openai-compatible",
    "suilearn.ai.base-url=https://ai.example.test/v1",
    "suilearn.ai.api-key=test-api-key",
    "suilearn.ai.chat-model=test-chat-model",
    "suilearn.ai.embedding-model=test-embedding-model"
})
class MaterialImportFailureTransactionIntegrationTest {
    @Autowired
    private MaterialImportService materialImports;

    @Autowired
    private KnowledgeBaseService knowledgeBases;

    @Autowired
    private MaterialStore materials;

    @Autowired
    private TaskService tasks;

    @Autowired
    private TaskRetryRoutingState retryRoutingState;

    @Autowired
    private SuiLearnV2Store store;

    @Autowired
    private PlatformTransactionManager transactions;

    @BeforeEach
    void clearDatabase() {
        store.deleteAll();
    }

    @Test
    void commitsMaterialAndTaskFailureBeforeTheOuterDeliveryTransactionRollsBack() {
        var knowledgeBase = knowledgeBases.createKnowledgeBase(new CreateKnowledgeBaseRequest("Transactions", "Failure handling"));
        var material = materialImports.importMaterial(knowledgeBase.id(), new ImportMaterialRequest(
            "Unreadable document", "empty.txt", MaterialSourceType.TXT, ""
        ));

        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(outer ->
            materialImports.consumeQueuedMaterialImport(material.id(), material.importTaskId())
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Material content is required");

        assertThat(materials.find(material.id()).orElseThrow())
            .extracting(value -> value.status(), value -> value.errorMessage())
            .containsExactly(MaterialStatus.FAILED, "Material content is required");
        assertThat(tasks.getTaskStatus(material.importTaskId()))
            .extracting(value -> value.status(), value -> value.errorCode(), value -> value.errorMessage(), value -> value.retryCount())
            .containsExactly(TaskLifecycleStatus.FAILED, "MATERIAL_IMPORT_FAILED", "Material content is required", 0);

        new TransactionTemplate(transactions).executeWithoutResult(outer -> {
            retryRoutingState.retryAccepted(material.importTaskId(), 1);
            outer.setRollbackOnly();
        });

        assertThat(tasks.getTaskStatus(material.importTaskId()))
            .extracting(value -> value.status(), value -> value.errorCode(), value -> value.errorMessage(), value -> value.retryCount())
            .containsExactly(TaskLifecycleStatus.QUEUED, null, null, 1);
    }
}

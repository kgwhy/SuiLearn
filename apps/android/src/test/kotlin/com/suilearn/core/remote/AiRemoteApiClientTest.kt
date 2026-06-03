package com.suilearn.core.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiRemoteApiClientTest {
    @Test
    fun `parses provider status`() {
        val status = AiRemoteApiClient.parseProviderStatus(
            """
            {
              "providerType": "OPENAI_COMPATIBLE",
              "configured": true,
              "available": false,
              "chatModel": "gpt-4.1-mini",
              "embeddingModel": "text-embedding-3-small",
              "embeddingDimensions": 1536,
              "message": "provider unreachable"
            }
            """.trimIndent()
        )

        assertEquals("OPENAI_COMPATIBLE", status.providerType)
        assertTrue(status.configured)
        assertFalse(status.available)
        assertEquals("gpt-4.1-mini", status.chatModel)
        assertEquals("text-embedding-3-small", status.embeddingModel)
        assertEquals(1536, status.embeddingDimensions)
        assertEquals("provider unreachable", status.message)
    }

    @Test
    fun `parses generated question drafts`() {
        val drafts = AiRemoteApiClient.parseGeneratedContents(
            """
            [
              {
                "id": "generated-1",
                "knowledgeBaseId": "kb-default",
                "generationTaskId": "task-1",
                "status": "PENDING_REVIEW",
                "questionType": "SINGLE_CHOICE",
                "stem": "HashMap 扩容时容量如何变化？",
                "answer": ["A"],
                "explanation": "容量按 2 倍扩容。",
                "categoryId": "java",
                "categoryName": "Java",
                "knowledgePointIds": ["kp-hashmap"],
                "sourceRefs": []
              },
              {
                "id": "generated-2",
                "knowledgeBaseId": "kb-default",
                "generationTaskId": "task-2",
                "status": "SAVED",
                "questionType": "SHORT_ANSWER",
                "stem": "volatile 的可见性含义是什么？",
                "answer": ["可见性"],
                "explanation": "写入对其他线程可见。",
                "categoryId": "java",
                "categoryName": "Java",
                "knowledgePointIds": ["kp-jmm"],
                "sourceRefs": []
              }
            ]
            """.trimIndent()
        )

        assertEquals(2, drafts.size)
        assertEquals("generated-1", drafts[0].id)
        assertEquals("kb-default", drafts[0].knowledgeBaseId)
        assertEquals("task-1", drafts[0].generationTaskId)
        assertEquals("PENDING_REVIEW", drafts[0].status)
        assertEquals("HashMap 扩容时容量如何变化？", drafts[0].stem)
        assertEquals("SAVED", drafts[1].status)
    }

    @Test
    fun `parses task status`() {
        val taskStatus = AiRemoteApiClient.parseTaskStatus(
            """
            {
              "id": "task-1",
              "kind": "QUESTION_GENERATION",
              "status": "FAILED",
              "currentStep": "GENERATING",
              "errorMessage": "provider unavailable",
              "createdAt": "2026-06-03T00:00:00Z",
              "updatedAt": "2026-06-03T00:00:01Z"
            }
            """.trimIndent()
        )

        assertEquals("task-1", taskStatus.id)
        assertEquals("QUESTION_GENERATION", taskStatus.kind)
        assertEquals("FAILED", taskStatus.status)
        assertEquals("GENERATING", taskStatus.currentStep)
        assertEquals("provider unavailable", taskStatus.errorMessage)
    }
}

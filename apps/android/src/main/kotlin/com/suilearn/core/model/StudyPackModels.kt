package com.suilearn.core.model

data class StudyPack(
    val packId: String,
    val name: String,
    val description: String,
    val packVersion: Int,
    val schemaVersion: Int,
    val importedAt: Long,
)

data class Category(
    val categoryId: String,
    val packId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
)

data class KnowledgePoint(
    val knowledgePointId: String,
    val packId: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
)


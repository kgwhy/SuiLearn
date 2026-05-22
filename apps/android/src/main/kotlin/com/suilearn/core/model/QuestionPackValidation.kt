package com.suilearn.core.model

object QuestionPackValidation {
    private val allowedCategoryIds = setOf(
        "jvm",
        "java_basics",
        "collections",
        "concurrency",
        "spring",
        "mysql",
        "redis",
        "computer_networks",
        "operating_system",
        "design_patterns",
        "project_scenarios",
    )

    fun validate(pack: QuestionPack): List<String> {
        val errors = mutableListOf<String>()

        if (pack.schemaVersion <= 0) {
            errors += "schemaVersion must be positive."
        }
        if (pack.packId.isBlank()) {
            errors += "packId must not be blank."
        }
        if (pack.packName.isBlank()) {
            errors += "packName must not be blank."
        }
        if (pack.packVersion <= 0) {
            errors += "packVersion must be positive."
        }

        val categoryIds = pack.categories.map { it.categoryId }.toSet()
        val knowledgePointIds = pack.knowledgePoints.map { it.knowledgePointId }.toSet()

        if (pack.categories.any { it.categoryId !in allowedCategoryIds }) {
            errors += "categoryId contains unsupported built-in category."
        }

        pack.knowledgePoints.forEach { point ->
            if (point.categoryId !in categoryIds) {
                errors += "Knowledge point ${point.knowledgePointId} references missing categoryId ${point.categoryId}."
            }
        }

        pack.questions.forEach { question ->
            if (question.categoryId !in categoryIds) {
                errors += "Question ${question.questionId} references missing categoryId ${question.categoryId}."
            }
            if (question.knowledgePointIds.any { it !in knowledgePointIds }) {
                errors += "Question ${question.questionId} references missing knowledge point."
            }
            validateQuestion(question).forEach { errors += "Question ${question.questionId}: $it" }
        }

        return errors
    }

    private fun validateQuestion(question: QuestionPackQuestion): List<String> {
        val errors = mutableListOf<String>()
        if (question.questionId.isBlank()) {
            errors += "questionId must not be blank."
        }
        if (question.stem.isBlank()) {
            errors += "stem must not be blank."
        }
        if (question.difficulty !in 1..5) {
            errors += "difficulty must be within 1..5."
        }
        if (question.type == QuestionType.SINGLE_CHOICE || question.type == QuestionType.TRUE_FALSE) {
            if (question.answer.size != 1) {
                errors += "single choice and true/false questions must have exactly one answer."
            }
        }

        if (question.type == QuestionType.MULTIPLE_CHOICE && question.answer.size < 2) {
            errors += "multiple choice questions must have more than one answer."
        }
        if (question.options.any { it.key.isBlank() || it.content.isBlank() }) {
            errors += "options must not contain blank keys or content."
        }
        if (question.type == QuestionType.SHORT_ANSWER && question.explanation.isBlank()) {
            errors += "short answer questions must include explanation."
        }
        if (question.type != QuestionType.SHORT_ANSWER && question.options.isEmpty()) {
            errors += "objective questions must include options."
        }
        return errors
    }
}

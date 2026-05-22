package com.suilearn.core.model

fun calculateMasteryLevel(
    objectiveQuestionCount: Int,
    practicedQuestionCount: Int,
    totalObjectiveAnswers: Int,
    totalObjectiveCorrect: Int,
    activeWrongCount: Int,
): MasteryLevel {
    if (objectiveQuestionCount <= 0 || totalObjectiveAnswers <= 0) {
        return MasteryLevel.NOT_STARTED
    }

    val accuracy = totalObjectiveCorrect.toDouble() / totalObjectiveAnswers
    return when {
        activeWrongCount > 0 || accuracy < 0.6 -> MasteryLevel.WEAK
        accuracy >= 0.8 && practicedQuestionCount == objectiveQuestionCount -> MasteryLevel.MASTERED
        else -> MasteryLevel.LEARNING
    }
}

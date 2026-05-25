package com.suilearn.di

import com.suilearn.core.common.SystemClock
import com.suilearn.core.common.UuidIdGenerator
import com.suilearn.core.common.AppResult
import com.suilearn.core.database.SuiLearnDatabase
import com.suilearn.core.importer.QuestionPackJsonParser
import com.suilearn.core.importer.QuestionPackSource
import com.suilearn.core.repository.AnswerRecordRepository
import com.suilearn.core.repository.FavoriteRepository
import com.suilearn.core.repository.InMemoryAnswerRecordRepository
import com.suilearn.core.repository.InMemoryFavoriteRepository
import com.suilearn.core.repository.InMemoryPracticeSessionRepository
import com.suilearn.core.repository.InMemoryQuestionRepository
import com.suilearn.core.repository.InMemorySearchRepository
import com.suilearn.core.repository.InMemorySettingsRepository
import com.suilearn.core.repository.InMemoryStatisticsRepository
import com.suilearn.core.repository.InMemoryStudyPackRepository
import com.suilearn.core.repository.InMemoryWrongQuestionRepository
import com.suilearn.core.repository.PracticeSessionRepository
import com.suilearn.core.repository.QuestionRepository
import com.suilearn.core.repository.RoomAnswerRecordRepository
import com.suilearn.core.repository.RoomFavoriteRepository
import com.suilearn.core.repository.RoomQuestionRepository
import com.suilearn.core.repository.RoomPracticeSessionRepository
import com.suilearn.core.repository.RoomSearchRepository
import com.suilearn.core.repository.RoomSettingsRepository
import com.suilearn.core.repository.RoomStatisticsRepository
import com.suilearn.core.repository.RoomStudyPackRepository
import com.suilearn.core.repository.RoomWrongQuestionRepository
import com.suilearn.core.repository.SettingsRepository
import com.suilearn.core.repository.StatisticsRepository
import com.suilearn.core.repository.StudyPackRepository
import com.suilearn.core.repository.WrongQuestionRepository
import com.suilearn.core.usecase.BuildPracticeSessionUseCase
import com.suilearn.core.usecase.EvaluateShortAnswerUseCase
import com.suilearn.core.usecase.GetHomeSummaryUseCase
import com.suilearn.core.usecase.GetStatisticsSummaryUseCase
import com.suilearn.core.usecase.InitializeQuestionPackUseCase
import com.suilearn.core.usecase.MarkWrongQuestionMasteredUseCase
import com.suilearn.core.usecase.ResetLearningDataUseCase
import com.suilearn.core.usecase.ResumePracticeSessionUseCase
import com.suilearn.core.usecase.SearchLearningContentUseCase
import com.suilearn.core.usecase.StartPracticeFromQuestionUseCase
import com.suilearn.core.usecase.SubmitAnswerUseCase
import com.suilearn.core.usecase.ToggleFavoriteQuestionUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppDependencies(
    private val questionPackSource: QuestionPackSource,
    private val database: SuiLearnDatabase? = null,
) {
    private val seedMutex = Mutex()
    private val refreshFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val refreshEvents: SharedFlow<Unit> = refreshFlow.asSharedFlow()

    val studyPackRepository: StudyPackRepository =
        database?.studyPackDao()?.let { RoomStudyPackRepository(it) }
            ?: InMemoryStudyPackRepository()
    val questionRepository: QuestionRepository =
        database?.questionDao()?.let { RoomQuestionRepository(it) }
            ?: InMemoryQuestionRepository()
    val answerRecordRepository: AnswerRecordRepository =
        database?.learningDao()?.let { RoomAnswerRecordRepository(it) }
            ?: InMemoryAnswerRecordRepository()
    val practiceSessionRepository: PracticeSessionRepository =
        database?.learningDao()?.let { RoomPracticeSessionRepository(it) }
            ?: InMemoryPracticeSessionRepository()
    val wrongQuestionRepository: WrongQuestionRepository =
        database?.learningDao()?.let { RoomWrongQuestionRepository(it) }
            ?: InMemoryWrongQuestionRepository()
    val favoriteRepository: FavoriteRepository =
        database?.learningDao()?.let { RoomFavoriteRepository(it) }
            ?: InMemoryFavoriteRepository()
    val settingsRepository: SettingsRepository =
        database?.appSettingDao()?.let { RoomSettingsRepository(it) }
            ?: InMemorySettingsRepository()
    val statisticsRepository: StatisticsRepository =
        if (database != null) {
            RoomStatisticsRepository(
                questionRepository,
                studyPackRepository,
                answerRecordRepository,
                wrongQuestionRepository,
                practiceSessionRepository,
            )
        } else {
            InMemoryStatisticsRepository(
                questionRepository,
                studyPackRepository,
                answerRecordRepository,
                wrongQuestionRepository,
                practiceSessionRepository,
            )
        }

    val initializeQuestionPackUseCase = InitializeQuestionPackUseCase(
        studyPackRepository,
        questionRepository,
        settingsRepository,
        clock = SystemClock,
    )
    val getHomeSummaryUseCase = GetHomeSummaryUseCase(
        studyPackRepository,
        questionRepository,
        answerRecordRepository,
        wrongQuestionRepository,
        practiceSessionRepository,
    )
    val getStatisticsSummaryUseCase = GetStatisticsSummaryUseCase(statisticsRepository)
    val buildPracticeSessionUseCase = BuildPracticeSessionUseCase(
        questionRepository,
        practiceSessionRepository,
        wrongQuestionRepository,
        favoriteRepository,
        clock = SystemClock,
        idGenerator = UuidIdGenerator,
    )
    val resumePracticeSessionUseCase = ResumePracticeSessionUseCase(
        questionRepository,
        practiceSessionRepository,
    )
    val submitAnswerUseCase = SubmitAnswerUseCase(
        questionRepository,
        answerRecordRepository,
        wrongQuestionRepository,
        favoriteRepository,
        clock = SystemClock,
        idGenerator = UuidIdGenerator,
    )
    val evaluateShortAnswerUseCase = EvaluateShortAnswerUseCase(
        questionRepository,
        answerRecordRepository,
        wrongQuestionRepository,
        favoriteRepository,
        clock = SystemClock,
        idGenerator = UuidIdGenerator,
    )
    val startPracticeFromQuestionUseCase = StartPracticeFromQuestionUseCase(
        questionRepository,
        practiceSessionRepository,
        clock = SystemClock,
        idGenerator = UuidIdGenerator,
    )
    val searchLearningContentUseCase = SearchLearningContentUseCase(
        if (database != null) {
            RoomSearchRepository(database.questionDao(), database.studyPackDao(), answerRecordRepository, wrongQuestionRepository)
        } else {
            InMemorySearchRepository(questionRepository, studyPackRepository, answerRecordRepository, wrongQuestionRepository)
        }
    )
    val toggleFavoriteQuestionUseCase = ToggleFavoriteQuestionUseCase(favoriteRepository)
    val markWrongQuestionMasteredUseCase = MarkWrongQuestionMasteredUseCase(wrongQuestionRepository)
    val resetLearningDataUseCase = ResetLearningDataUseCase(
        settingsRepository,
        practiceSessionRepository,
        answerRecordRepository,
        wrongQuestionRepository,
        favoriteRepository,
    )

    suspend fun ensureSeeded() {
        seedMutex.withLock {
            val pack = QuestionPackJsonParser.parse(questionPackSource.loadQuestionPackJson())
            val currentPack = studyPackRepository.getCurrentPack()
            val currentPackId = settingsRepository.getCurrentPackId()
            val currentQuestionCount = questionRepository.listQuestions().size
            val isCurrentContent =
                currentPackId == pack.packId &&
                    currentPack?.packVersion == pack.packVersion &&
                    currentPack.schemaVersion == pack.schemaVersion &&
                    currentQuestionCount == pack.questions.size

            if (isCurrentContent) {
                return
            }

            resetLearningDataUseCase.execute()
            val result = initializeQuestionPackUseCase.execute(pack)
            if (result is AppResult.Success) {
                notifyDataChanged()
            }
        }
    }

    fun notifyDataChanged() {
        refreshFlow.tryEmit(Unit)
    }
}

package com.suilearn.api.generation.infrastructure;

import com.suilearn.api.model.AnswerRecord;
import com.suilearn.api.model.QuestionSummary;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionStore {
    private final SuiLearnV2Store store;

    public QuestionStore(SuiLearnV2Store store) {
        this.store = store;
    }

    public List<QuestionSummary> list(String knowledgeBaseId) {
        return store.listQuestions(knowledgeBaseId);
    }

    public List<QuestionSummary> list() {
        return store.listQuestions();
    }

    public QuestionSummary save(QuestionSummary question) {
        return store.saveQuestion(question);
    }

    public List<AnswerRecord> listAnswerRecords(String knowledgeBaseId) {
        return store.listAnswerRecords(knowledgeBaseId);
    }

    public List<AnswerRecord> listAnswerRecordsByQuestion(String questionId) {
        return store.listAnswerRecordsByQuestion(questionId);
    }

    public AnswerRecord saveAnswerRecord(AnswerRecord answerRecord) {
        return store.saveAnswerRecord(answerRecord);
    }

    public void delete(String questionId) {
        store.deleteQuestion(questionId);
    }
}

package com.suilearn.api.generation.infrastructure;

import com.suilearn.api.model.AiNoteDraft;
import com.suilearn.api.model.SavedAiNote;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AiNoteStore {
    private final SuiLearnV2Store store;

    public AiNoteStore(SuiLearnV2Store store) {
        this.store = store;
    }

    public List<AiNoteDraft> listDrafts(String knowledgeBaseId) {
        return store.listAiNoteDrafts(knowledgeBaseId);
    }

    public AiNoteDraft saveDraft(AiNoteDraft note) {
        return store.saveAiNoteDraft(note);
    }

    public List<SavedAiNote> listSaved(String knowledgeBaseId) {
        return store.listAiNotes(knowledgeBaseId);
    }

    public List<SavedAiNote> listSaved() {
        return store.listAiNotes();
    }

    public SavedAiNote save(SavedAiNote note) {
        return store.saveAiNote(note);
    }

    public void delete(String noteId) {
        store.deleteAiNote(noteId);
    }
}

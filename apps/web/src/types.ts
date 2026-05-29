export type MaterialSourceType = "MARKDOWN" | "TXT" | "PDF";
export type MaterialStatus = "UPLOADED" | "PARSING" | "CHUNKING" | "INDEXING" | "READY" | "FAILED" | "DELETED";
export type GeneratedContentStatus = "PENDING_REVIEW" | "SAVED" | "DISCARDED" | "DELETED";
export type QuestionType = "SINGLE_CHOICE" | "MULTIPLE_CHOICE" | "TRUE_FALSE" | "SHORT_ANSWER";
export type SourceType = "KNOWLEDGE_POINT" | "WRONG_QUESTION" | "MATERIAL" | "MATERIAL_CHUNK" | "KNOWLEDGE_BASE" | "ANSWER_HISTORY";
export type AiNoteType = "KNOWLEDGE_POINT_EXPLANATION" | "REVIEW_SUGGESTION" | "RAG_ANSWER" | "MANUAL_NOTE";

export interface KnowledgeBase {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeBaseDetail extends KnowledgeBase {
  materialCount?: number;
  knowledgePointCount?: number;
  questionCount?: number;
  generatedContentCount?: number;
  aiNoteCount?: number;
}

export interface KnowledgeBaseStatistics {
  knowledgeBaseId: string;
  questionCount: number;
  answeredQuestionCount: number;
  answerCount: number;
  correctRate?: number | null;
  wrongQuestionCount: number;
  weakKnowledgePointIds?: string[];
}

export interface MaterialMetadata {
  id: string;
  knowledgeBaseId: string;
  title: string;
  sourceType: MaterialSourceType;
  status: MaterialStatus;
  createdAt: string;
  deletedAt?: string | null;
}

export interface MaterialChunk {
  id: string;
  materialId: string;
  content: string;
  ordinal: number;
  sourceRef?: SourceRef;
}

export interface MaterialDetail extends MaterialMetadata {
  content?: string;
  contentPreview?: string;
  chunks?: MaterialChunk[];
  extractedKnowledgePoints?: KnowledgePoint[];
}

export interface KnowledgePoint {
  id: string;
  knowledgeBaseId: string;
  name: string;
  description: string;
  sourceMaterialId?: string;
  sourceRefs?: SourceRef[];
}

export interface SourceRef {
  type: SourceType;
  id: string;
  knowledgeBaseId?: string;
  title?: string;
  materialId?: string | null;
  chunkId?: string | null;
  deleted?: boolean;
  excerpt?: string | null;
}

export interface MaterialDeletionResult {
  materialId: string;
  status: MaterialStatus;
  savedContentPolicy: "KEEP_SAVED_CONTENT" | "DELETE_SAVED_CONTENT";
  pendingContentPolicy: "DELETE_PENDING_GENERATED_CONTENT" | "KEEP_PENDING_GENERATED_CONTENT";
  invalidatedChunkCount: number;
  deletedPendingGeneratedContentCount: number;
  retainedSavedQuestionCount?: number;
  retainedAiNoteCount?: number;
  deletedAt?: string | null;
}

export interface GeneratedQuestionDraft {
  id: string;
  knowledgeBaseId: string;
  status: GeneratedContentStatus;
  sourceRefs: SourceRef[];
  sourceType?: SourceType;
  sourceId?: string;
  questionType: QuestionType;
  categoryId: string;
  categoryName: string;
  knowledgePointIds: string[];
  stem: string;
  options?: string[];
  answer: string[];
  explanation: string;
  savedQuestionId?: string | null;
  savedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface QuestionSummary {
  id: string;
  knowledgeBaseId: string;
  questionType: QuestionType;
  stem: string;
  categoryId: string;
  categoryName: string;
  difficulty?: number | null;
  knowledgePointIds: string[];
  answeredCount?: number;
  correctRate?: number | null;
  sourceRefs: SourceRef[];
  createdAt: string;
  savedAt?: string | null;
}

export interface AiNoteDraft {
  id: string;
  knowledgeBaseId: string;
  type: AiNoteType;
  title: string;
  content: string;
  sourceRefs: SourceRef[];
  createdAt: string;
}

export interface SavedAiNote {
  id: string;
  knowledgeBaseId: string;
  type: AiNoteType;
  title: string;
  content: string;
  sourceRefs: SourceRef[];
  savedAt: string;
}

export interface SearchResult {
  id: string;
  type: "QUESTION" | "KNOWLEDGE_POINT" | "MATERIAL_CHUNK" | "GENERATED_CONTENT";
  title: string;
  summary: string;
  knowledgeBaseId?: string;
  knowledgePointIds?: string[];
  sourceRefs?: SourceRef[];
}

export interface RagAnswer {
  answer: string;
  uncertain: boolean;
  citations: SourceRef[];
  evidenceChunks?: MaterialChunk[];
  savedAiNoteId?: string | null;
}

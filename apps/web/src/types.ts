export type MaterialSourceType = "MARKDOWN" | "TXT" | "PDF" | "DOC" | "DOCX" | "LEGACY_TEXT";
export type MaterialStatus = "UPLOADED" | "PROCESSING" | "READY" | "FAILED" | "DELETED";
export type MaterialAssetType = "ORIGINAL" | "READING" | "PREVIEW" | "OCR_PAGE";
export type AssetAvailability = "PENDING" | "AVAILABLE" | "FAILED" | "UNAVAILABLE" | "DELETED";
export type AssetUnavailableReason = "LEGACY_NO_ORIGINAL" | "PROCESSING" | "GENERATION_FAILED" | "DELETED";
export type GeneratedContentStatus = "PENDING_REVIEW" | "SAVED" | "DISCARDED" | "DELETED";
export type QuestionType = "SINGLE_CHOICE" | "MULTIPLE_CHOICE" | "TRUE_FALSE" | "SHORT_ANSWER";
export type QuestionDifficulty = "EASY" | "MEDIUM" | "HARD";
export type KnowledgePointStatus = "DRAFT" | "CONFIRMED" | "REJECTED" | "ARCHIVED";
export type SourceType = "KNOWLEDGE_POINT" | "WRONG_QUESTION" | "MATERIAL" | "MATERIAL_CHUNK" | "KNOWLEDGE_BASE" | "ANSWER_HISTORY";
export type AiNoteType = "KNOWLEDGE_POINT_EXPLANATION" | "REVIEW_SUGGESTION" | "RAG_ANSWER" | "MANUAL_NOTE";
export type AiProviderType = "FAKE" | "OPENAI_COMPATIBLE";
export type EmbeddingStatus = "PENDING" | "INDEXING" | "READY" | "FAILED" | "INVALIDATED" | "TEXT_ONLY";
export type TaskKind =
  | "MATERIAL_IMPORT"
  | "EMBEDDING"
  | "QUESTION_GENERATION"
  | "MATERIAL_REPROCESS"
  | "KNOWLEDGE_POINT_GENERATION"
  | "KNOWLEDGE_POINT_QUESTION_GENERATION"
  | "KNOWLEDGE_POINT_EXTRACTION"
  | "EXPLANATION_GENERATION"
  | "REVIEW_SUGGESTION_GENERATION";
export type TaskLifecycleStatus = "QUEUED" | "RUNNING" | "RETRY_WAIT" | "SUCCEEDED" | "FAILED" | "CANCELLED";

export interface AiProviderStatus {
  providerType: AiProviderType;
  configured: boolean;
  available: boolean;
  baseUrl?: string;
  chatModel?: string;
  embeddingModel?: string;
  embeddingDimensions?: number;
  apiKeyEnvName?: string;
  timeoutMs?: number;
  maxRetries?: number;
  message?: string;
}

export interface TaskResultRef {
  type: "MATERIAL" | "DOCUMENT_REVISION" | "MATERIAL_CHUNKS" | "GENERATED_CONTENT" | "AI_NOTE_DRAFT" | "KNOWLEDGE_POINTS" | "KNOWLEDGE_POINT_BATCH" | "QUESTION_DRAFT_BATCH";
  href: string;
  id?: string;
  materialId?: string;
  revisionId?: string;
  count?: number;
}

export interface TaskStatus {
  id: string;
  kind: TaskKind;
  status: TaskLifecycleStatus;
  knowledgeBaseId?: string;
  materialId?: string;
  generatedContentId?: string;
  providerType?: AiProviderType;
  model?: string;
  progress?: {
    percent: number;
    completedUnits?: number;
    totalUnits?: number;
    message?: string;
  };
  progressPercent?: number;
  currentStep?: string;
  attempt?: number;
  maxAttempts?: number;
  nextRetryAt?: string;
  correlationId?: string;
  processingVersion?: string;
  errorCode?: string;
  errorMessage?: string;
  retryCount?: number;
  resultRef?: TaskResultRef;
  resultRefs?: TaskResultRef[];
  createdAt: string;
  startedAt?: string;
  finishedAt?: string;
  updatedAt: string;
}

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
  materialCount?: number;
  readyMaterialCount?: number;
  knowledgePointCount?: number;
  pendingGeneratedContentCount?: number;
  savedAiNoteCount?: number;
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
  /** @deprecated Transitional alias for processingTaskId consumed by the pre-Batch-E panel. */
  importTaskId: string;
  processingTaskId?: string;
  currentRevisionId?: string;
  embeddingTaskId?: string;
  errorMessage?: string;
  originalAvailable: boolean;
  originalUnavailableReason?: AssetUnavailableReason;
  createdAt: string;
  deletedAt?: string | null;
}

export interface MaterialChunk {
  id: string;
  knowledgeBaseId: string;
  materialId: string;
  content: string;
  ordinal: number;
  titlePath?: string[];
  tokenEstimate?: number;
  embeddingStatus: EmbeddingStatus;
  embeddingModel?: string;
  embeddingDimensions?: number;
  sourceRef?: SourceRef;
}

export interface MaterialDetail extends MaterialMetadata {
  content?: string;
  contentPreview?: string;
  chunks?: MaterialChunk[];
  extractedKnowledgePoints?: KnowledgePoint[];
  processingTask?: TaskStatus;
  currentRevision?: DocumentRevisionSummary;
  assets: MaterialAsset[];
}

export interface MaterialAsset {
  id?: string;
  materialId: string;
  revisionId?: string;
  type: MaterialAssetType;
  availability: AssetAvailability;
  unavailableReason?: AssetUnavailableReason;
  mediaType?: string;
  sizeBytes?: number;
  href?: string;
  downloadHref?: string;
  createdAt?: string;
}

export interface DocumentRevisionSummary {
  id: string;
  materialId: string;
  origin: "FILE_IMPORT" | "REPROCESS" | "LEGACY_TEXT_MIGRATION";
  processingVersion: string;
  blockCount?: number;
  pageCount?: number;
  createdByTaskId?: string;
  createdAt: string;
}

export interface DocumentBlock {
  id: string;
  revisionId: string;
  ordinal: number;
  content: string;
  pageNumber?: number;
  sectionPath?: string[];
}

export interface MaterialReadingDocument {
  materialId: string;
  revisionId: string;
  origin: "FILE_IMPORT" | "REPROCESS" | "LEGACY_TEXT_MIGRATION";
  mediaType: "text/html" | "text/plain";
  content: string;
  blocks: DocumentBlock[];
}

export interface TaskSubmission {
  taskId: string;
  status: TaskLifecycleStatus;
  taskHref: string;
}

export interface MaterialTaskSubmission extends TaskSubmission {
  materialId: string;
  materialHref: string;
}

export interface KnowledgePointExtractionResult {
  taskId: string;
  task: TaskStatus;
  knowledgePoints: KnowledgePoint[];
}

export interface KnowledgePointBase {
  id: string;
  knowledgeBaseId: string;
  status: KnowledgePointStatus;
  sourceOutdated: boolean;
  legacy: boolean;
  /** @deprecated Transitional display projection; Batch E UI consumes structured fields. */
  name: string;
  /** @deprecated Transitional display projection; Batch E UI consumes structured fields. */
  description: string;
  sourceMaterialId?: string;
  sourceRefs?: SourceRef[];
}

export interface SourceCitation {
  materialId: string;
  revisionId: string;
  pageNumber?: number;
  blockId?: string;
  excerpt: string;
  deleted?: boolean;
}

export interface StructuredKnowledgePoint extends KnowledgePointBase {
  legacy: false;
  generationTaskId: string;
  title: string;
  shortSummary: string;
  definition: string;
  principles: string[];
  applicationScenarios: string[];
  pitfalls: string[];
  citations: SourceCitation[];
}

export interface LegacyKnowledgePoint extends KnowledgePointBase {
  legacy: true;
  name: string;
  description: string;
}

export type KnowledgePoint = StructuredKnowledgePoint | LegacyKnowledgePoint;

export interface GenerateKnowledgePointQuestionsRequest {
  count?: number;
  difficulty?: QuestionDifficulty;
  questionType?: QuestionType;
}

export interface ReviewKnowledgePointQuestionDraftRequest {
  contentKind: "KNOWLEDGE_POINT_INTERVIEW_QUESTION";
  action: "SAVE" | "DISCARD";
  stem?: string;
  options?: string[];
  answer?: string[];
  explanation?: string;
  difficulty?: QuestionDifficulty;
  questionType?: QuestionType;
  categoryId?: string;
  categoryName?: string;
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
  generationTaskId: string;
  status: GeneratedContentStatus;
  /** @deprecated Generic-draft evidence projection. Knowledge-point drafts use citations. */
  sourceRefs: SourceRef[];
  sourceType?: SourceType;
  sourceId?: string;
  questionType: QuestionType;
  categoryId: string;
  categoryName: string;
  /** @deprecated Generic-draft attribution projection. Knowledge-point drafts use knowledgePointId. */
  knowledgePointIds: string[];
  knowledgePointId?: string;
  citations?: SourceCitation[];
  difficulty?: QuestionDifficulty;
  difficultyValue?: 2 | 3 | 4;
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
  generationTaskId: string;
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
  score: number;
  knowledgeBaseId?: string;
  knowledgePointIds?: string[];
  sourceRefs?: SourceRef[];
}

export interface RagAnswer {
  answer: string;
  uncertain: boolean;
  uncertaintyReason?: string;
  citations: SourceRef[];
  evidenceChunks?: MaterialChunk[];
  savedAiNoteId?: string | null;
}

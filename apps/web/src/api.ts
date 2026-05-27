import type {
  AiNoteDraft,
  AiNoteType,
  GeneratedContentStatus,
  GeneratedQuestionDraft,
  KnowledgeBase,
  KnowledgeBaseDetail,
  KnowledgeBaseStatistics,
  KnowledgePoint,
  MaterialDetail,
  MaterialMetadata,
  MaterialSourceType,
  QuestionSummary,
  QuestionType,
  RagAnswer,
  SavedAiNote,
  SearchResult,
  SourceRef
} from "./types";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api/v2";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: init?.body instanceof FormData ? init.headers : { "Content-Type": "application/json", ...init?.headers },
    ...init
  });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `${response.status} ${response.statusText}`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export const api = {
  listKnowledgeBases: () => request<KnowledgeBase[]>("/knowledge-bases"),
  createKnowledgeBase: (payload: { name: string; description?: string }) =>
    request<KnowledgeBase>("/knowledge-bases", { method: "POST", body: JSON.stringify(payload) }),
  updateKnowledgeBase: (id: string, payload: { name: string; description?: string }) =>
    request<KnowledgeBase>(`/knowledge-bases/${id}`, { method: "PATCH", body: JSON.stringify(payload) }),
  deleteKnowledgeBase: (id: string) => request<void>(`/knowledge-bases/${id}`, { method: "DELETE" }),
  getKnowledgeBase: (id: string) => request<KnowledgeBaseDetail>(`/knowledge-bases/${id}`),
  getStatistics: (id: string) => request<KnowledgeBaseStatistics>(`/knowledge-bases/${id}/statistics`),
  listMaterials: (knowledgeBaseId: string) =>
    request<MaterialMetadata[]>(`/knowledge-bases/${knowledgeBaseId}/materials`),
  importMaterial: (
    knowledgeBaseId: string,
    payload: { title: string; fileName?: string; sourceType: MaterialSourceType; content: string }
  ) =>
    request<MaterialMetadata>(`/knowledge-bases/${knowledgeBaseId}/materials`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  getMaterial: (materialId: string) => request<MaterialDetail>(`/materials/${materialId}`),
  deleteMaterial: (materialId: string) => request(`/materials/${materialId}`, { method: "DELETE" }),
  extractKnowledgePoints: (materialId: string) =>
    request<KnowledgePoint[]>(`/materials/${materialId}/extract-knowledge-points`, { method: "POST" }),
  listKnowledgePoints: (knowledgeBaseId: string) =>
    request<KnowledgePoint[]>(`/knowledge-bases/${knowledgeBaseId}/knowledge-points`),
  listQuestions: (knowledgeBaseId: string) =>
    request<QuestionSummary[]>(`/knowledge-bases/${knowledgeBaseId}/questions`),
  generateQuestion: (payload: {
    knowledgeBaseId: string;
    sourceRefs: SourceRef[];
    questionType: QuestionType;
    categoryId?: string;
    categoryName?: string;
    knowledgePointIds?: string[];
    prompt?: string;
  }) =>
    request<GeneratedQuestionDraft>("/ai/generated-questions", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  listGeneratedContents: (status?: GeneratedContentStatus) =>
    request<GeneratedQuestionDraft[]>(`/ai/generated-contents${status ? `?status=${status}` : ""}`),
  reviewGeneratedContent: (
    id: string,
    payload: {
      status: GeneratedContentStatus;
      stem?: string;
      options?: string[];
      answer?: string[];
      explanation?: string;
      categoryId?: string;
      categoryName?: string;
      knowledgePointIds?: string[];
      sourceRefs?: SourceRef[];
    }
  ) =>
    request<GeneratedQuestionDraft>(`/ai/generated-contents/${id}`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  deleteGeneratedContent: (id: string) => request<void>(`/ai/generated-contents/${id}`, { method: "DELETE" }),
  generateExplanation: (payload: {
    knowledgeBaseId: string;
    knowledgePointId: string;
    sourceRefs: SourceRef[];
    prompt?: string;
  }) =>
    request<AiNoteDraft>("/ai/knowledge-point-explanations", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  generateReviewSuggestion: (payload: {
    knowledgeBaseId: string;
    sourceRefs: SourceRef[];
    weakKnowledgePointIds?: string[];
    wrongQuestionIds?: string[];
    prompt?: string;
  }) =>
    request<AiNoteDraft>("/ai/review-suggestions", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  saveAiNote: (payload: {
    draftId?: string;
    knowledgeBaseId: string;
    type: AiNoteType;
    title: string;
    content: string;
    sourceRefs: SourceRef[];
  }) =>
    request<SavedAiNote>("/ai/notes", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  search: (params: { q: string; knowledgeBaseId?: string; materialId?: string }) => {
    const query = new URLSearchParams({ q: params.q });
    if (params.knowledgeBaseId) query.set("knowledgeBaseId", params.knowledgeBaseId);
    if (params.materialId) query.set("materialId", params.materialId);
    return request<SearchResult[]>(`/search?${query.toString()}`);
  },
  ask: (payload: { question: string; knowledgeBaseId?: string; materialId?: string }) =>
    request<RagAnswer>("/rag/ask", { method: "POST", body: JSON.stringify(payload) })
};

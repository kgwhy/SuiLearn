import {
  AlertCircle,
  Archive,
  BookOpen,
  Bot,
  Check,
  ChevronRight,
  Database,
  FileText,
  Loader2,
  MessageSquareText,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  Send,
  Sparkles,
  Trash2,
  Upload,
  X
} from "lucide-react";
import { ChangeEvent, DragEvent, FormEvent, useEffect, useMemo, useState } from "react";
import { api } from "./api";
import type {
  AiProviderStatus,
  AiNoteDraft,
  GeneratedContentStatus,
  GeneratedQuestionDraft,
  KnowledgeBase,
  KnowledgeBaseDetail,
  KnowledgeBaseStatistics,
  KnowledgePoint,
  MaterialDetail,
  MaterialMetadata,
  MaterialReadingDocument,
  MaterialSourceType,
  QuestionDifficulty,
  QuestionSummary,
  QuestionType,
  RagAnswer,
  SearchResult,
  SourceCitation,
  SourceRef,
  TaskStatus
} from "./types";

type Section = "overview" | "materials" | "generate" | "search";
type ToastTone = "info" | "success" | "error";
type ContentFilter = GeneratedContentStatus | "ALL";
type FieldErrors = Record<string, string>;
type TaskStatusMap = Record<string, TaskStatus>;
type TaskErrorMap = Record<string, string>;

const sourceTypes: MaterialSourceType[] = ["MARKDOWN", "TXT", "PDF", "DOC", "DOCX"];
const questionTypes: QuestionType[] = ["SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER"];

export function App() {
  const [section, setSection] = useState<Section>("overview");
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState("");
  const [detail, setDetail] = useState<KnowledgeBaseDetail | null>(null);
  const [statistics, setStatistics] = useState<KnowledgeBaseStatistics | null>(null);
  const [materials, setMaterials] = useState<MaterialMetadata[]>([]);
  const [materialDetail, setMaterialDetail] = useState<MaterialDetail | null>(null);
  const [materialReading, setMaterialReading] = useState<MaterialReadingDocument | null>(null);
  const [knowledgePoints, setKnowledgePoints] = useState<KnowledgePoint[]>([]);
  const [selectedKnowledgePointId, setSelectedKnowledgePointId] = useState("");
  const [questions, setQuestions] = useState<QuestionSummary[]>([]);
  const [drafts, setDrafts] = useState<GeneratedQuestionDraft[]>([]);
  const [questionDraftsByTask, setQuestionDraftsByTask] = useState<Record<string, GeneratedQuestionDraft[]>>({});
  const [questionGenerationTasks, setQuestionGenerationTasks] = useState<Record<string, string>>({});
  const [selectedDraftId, setSelectedDraftId] = useState("");
  const [contentFilter, setContentFilter] = useState<ContentFilter>("PENDING_REVIEW");
  const [aiNoteDraft, setAiNoteDraft] = useState<AiNoteDraft | null>(null);
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [ragAnswer, setRagAnswer] = useState<RagAnswer | null>(null);
  const [aiProviderStatus, setAiProviderStatus] = useState<AiProviderStatus | null>(null);
  const [aiProviderError, setAiProviderError] = useState("");
  const [providerLoading, setProviderLoading] = useState(false);
  const [taskStatuses, setTaskStatuses] = useState<TaskStatusMap>({});
  const [taskErrors, setTaskErrors] = useState<TaskErrorMap>({});
  const [taskLoading, setTaskLoading] = useState<Record<string, boolean>>({});
  const [extractionTasks, setExtractionTasks] = useState<Record<string, TaskStatus>>({});
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<{ tone: ToastTone; message: string } | null>(null);
  const [formErrors, setFormErrors] = useState<FieldErrors>({});
  const [hasSearched, setHasSearched] = useState(false);
  const [hasAsked, setHasAsked] = useState(false);

  const [kbForm, setKbForm] = useState({ name: "Java 面试", description: "Spring、JVM、集合与数据库资料" });
  const [kbManageForm, setKbManageForm] = useState({ name: "", description: "" });
  const [materialForm, setMaterialForm] = useState({
    title: "HashMap 资料",
    fileName: "",
    sourceType: "MARKDOWN" as MaterialSourceType
  });
  const [selectedMaterialFile, setSelectedMaterialFile] = useState<File | null>(null);
  const [generationForm, setGenerationForm] = useState({
    sourceKind: "material" as "material" | "knowledgePoint",
    sourceId: "",
    questionType: "SINGLE_CHOICE" as QuestionType,
    categoryId: "java-interview",
    categoryName: "Java 面试",
    prompt: "生成一道贴近面试追问的练习题"
  });
  const [searchForm, setSearchForm] = useState({ query: "HashMap", question: "HashMap 如何处理哈希冲突？" });

  const selectedKnowledgeBase = knowledgeBases.find((item) => item.id === selectedKnowledgeBaseId) ?? null;
  const selectedKnowledgePoint = knowledgePoints.find((item) => item.id === selectedKnowledgePointId) ?? null;
  const activeMaterials = useMemo(
    () => materials.filter((item) => item.status !== "DELETED"),
    [materials]
  );
  const visibleDrafts = useMemo(
    () => drafts.filter((draft) => contentFilter === "ALL" || draft.status === contentFilter),
    [contentFilter, drafts]
  );
  const selectedDraft = visibleDrafts.find((item) => item.id === selectedDraftId) ?? visibleDrafts[0] ?? null;
  const selectedSource = useMemo(() => {
    if (generationForm.sourceKind === "knowledgePoint") {
      return knowledgePoints.find((item) => item.id === generationForm.sourceId) ?? knowledgePoints[0] ?? null;
    }
    return activeMaterials.find((item) => item.id === generationForm.sourceId) ?? activeMaterials[0] ?? null;
  }, [activeMaterials, generationForm.sourceId, generationForm.sourceKind, knowledgePoints]);

  useEffect(() => {
    void loadKnowledgeBases();
    void loadAiProviderStatus();
  }, []);

  useEffect(() => {
    if (selectedKnowledgeBaseId) {
      void loadWorkbench(selectedKnowledgeBaseId);
    }
  }, [selectedKnowledgeBaseId]);

  useEffect(() => {
    if (selectedKnowledgeBase) {
      setKbManageForm({
        name: selectedKnowledgeBase.name,
        description: selectedKnowledgeBase.description ?? ""
      });
    } else {
      setKbManageForm({ name: "", description: "" });
    }
  }, [selectedKnowledgeBase?.id, selectedKnowledgeBase?.name, selectedKnowledgeBase?.description]);

  useEffect(() => {
    const firstMaterialId = activeMaterials[0]?.id;
    if (firstMaterialId && !generationForm.sourceId) {
      setGenerationForm((current) => ({ ...current, sourceId: firstMaterialId }));
    }
  }, [activeMaterials, generationForm.sourceId]);

  useEffect(() => {
    if (selectedKnowledgePointId && !knowledgePoints.some((item) => item.id === selectedKnowledgePointId)) {
      setSelectedKnowledgePointId("");
    }
  }, [knowledgePoints, selectedKnowledgePointId]);

  async function run<T>(task: () => Promise<T>, success?: string): Promise<T | null> {
    setLoading(true);
    setToast(null);
    try {
      const result = await task();
      if (success) setToast({ tone: "success", message: success });
      return result;
    } catch (error) {
      setToast({ tone: "error", message: error instanceof Error ? error.message : "操作失败" });
      return null;
    } finally {
      setLoading(false);
    }
  }

  function setError(nextErrors: FieldErrors) {
    setFormErrors(nextErrors);
    const firstError = Object.values(nextErrors)[0];
    if (firstError) setToast({ tone: "error", message: firstError });
  }

  async function submitCreateKnowledgeBase() {
    if (!kbForm.name.trim()) {
      setError({ kbName: "请输入知识库名称后再创建。" });
      return;
    }
    setFormErrors((current) => ({ ...current, kbName: "" }));
    const created = await run(
      () => api.createKnowledgeBase({ name: kbForm.name.trim(), description: kbForm.description.trim() || undefined }),
      "知识库已创建"
    );
    if (!created) return;
    setSelectedKnowledgeBaseId(created.id);
    await loadKnowledgeBases(created.id);
  }

  async function loadKnowledgeBases(preferredId?: string) {
    const bases = await run(() => api.listKnowledgeBases());
    if (!bases) return;
    setKnowledgeBases(bases);
    const nextSelectedId = preferredId ?? selectedKnowledgeBaseId;
    if (bases.length === 0) {
      setSelectedKnowledgeBaseId("");
      setDetail(null);
      setStatistics(null);
      setMaterials([]);
      setMaterialDetail(null);
      setMaterialReading(null);
      setKnowledgePoints([]);
      setSelectedKnowledgePointId("");
      setQuestions([]);
      setDrafts([]);
      setQuestionDraftsByTask({});
      setQuestionGenerationTasks({});
    } else if (!nextSelectedId || !bases.some((item) => item.id === nextSelectedId)) {
      setSelectedKnowledgeBaseId(bases[0].id);
    } else {
      setSelectedKnowledgeBaseId(nextSelectedId);
    }
  }

  async function loadWorkbench(knowledgeBaseId = selectedKnowledgeBaseId) {
    if (!knowledgeBaseId) return;
    const [nextDetail, nextStats, nextMaterials, nextKnowledgePoints, nextQuestions, nextDrafts] = await Promise.all([
      api.getKnowledgeBase(knowledgeBaseId),
      api.getStatistics(knowledgeBaseId),
      api.listMaterials(knowledgeBaseId),
      api.listKnowledgePoints(knowledgeBaseId),
      api.listQuestions(knowledgeBaseId),
      api.listGeneratedContents()
    ]);
    setDetail(nextDetail);
    setStatistics(nextStats);
    setMaterials(nextMaterials.filter((item) => item.status !== "DELETED"));
    setKnowledgePoints(nextKnowledgePoints);
    setQuestions(nextQuestions);
    setDrafts(nextDrafts.filter((draft) => draft.knowledgeBaseId === knowledgeBaseId));
  }

  async function loadAiProviderStatus() {
    setProviderLoading(true);
    setAiProviderError("");
    try {
      setAiProviderStatus(await api.getAiProviderStatus());
    } catch (error) {
      setAiProviderStatus(null);
      setAiProviderError(error instanceof Error ? error.message : "AI Provider 状态读取失败");
    } finally {
      setProviderLoading(false);
    }
  }

  async function refreshWorkbench() {
    await Promise.all([loadWorkbench(), loadAiProviderStatus()]);
  }

  async function viewTaskStatus(taskId: string) {
    setTaskLoading((current) => ({ ...current, [taskId]: true }));
    setTaskErrors((current) => ({ ...current, [taskId]: "" }));
    try {
      const status = await api.getTaskStatus(taskId);
      setTaskStatuses((current) => ({ ...current, [taskId]: status }));
      if (status.status === "SUCCEEDED") await loadTaskQuestionDrafts(taskId);
    } catch (error) {
      setTaskErrors((current) => ({
        ...current,
        [taskId]: error instanceof Error ? error.message : "任务状态读取失败"
      }));
    } finally {
      setTaskLoading((current) => ({ ...current, [taskId]: false }));
    }
  }

  async function createKnowledgeBase(event: FormEvent) {
    event.preventDefault();
    await submitCreateKnowledgeBase();
  }

  async function updateKnowledgeBase(event: FormEvent) {
    event.preventDefault();
    if (!selectedKnowledgeBaseId) return;
    if (!kbManageForm.name.trim()) {
      setError({ kbManageName: "请输入当前知识库名称后再更新。" });
      return;
    }
    setFormErrors((current) => ({ ...current, kbManageName: "" }));
    const updated = await run(
      () =>
        api.updateKnowledgeBase(selectedKnowledgeBaseId, {
          name: kbManageForm.name.trim(),
          description: kbManageForm.description.trim() || undefined
        }),
      "知识库已更新"
    );
    if (!updated) return;
    await loadKnowledgeBases(updated.id);
    await loadWorkbench(updated.id);
  }

  async function deleteKnowledgeBase() {
    if (!selectedKnowledgeBaseId || !selectedKnowledgeBase) return;
    const confirmed = window.confirm(`删除知识库「${selectedKnowledgeBase.name}」？其中资料、知识点和题目会一并删除。`);
    if (!confirmed) return;
    await run(() => api.deleteKnowledgeBase(selectedKnowledgeBaseId), "知识库已删除");
    await loadKnowledgeBases();
  }

  function selectMaterialFile(file: File) {
    const extension = file.name.split(".").pop()?.toUpperCase();
    const sourceType = extension === "PDF" || extension === "DOC" || extension === "DOCX" || extension === "TXT"
      ? extension
      : "MARKDOWN";
    setSelectedMaterialFile(file);
    setMaterialForm((current) => ({
      ...current,
      title: current.title.trim() ? current.title : file.name.replace(/\.[^.]+$/, ""),
      fileName: file.name,
      sourceType: sourceType as MaterialSourceType
    }));
  }

  async function loadTaskQuestionDrafts(taskId: string) {
    try {
      const taskDrafts = await api.listTaskQuestionDrafts(taskId);
      setQuestionDraftsByTask((current) => ({ ...current, [taskId]: taskDrafts }));
      setDrafts((current) => {
        const preserved = current.filter((draft) => draft.generationTaskId !== taskId);
        return [...taskDrafts, ...preserved];
      });
      if (taskDrafts[0]) setSelectedDraftId(taskDrafts[0].id);
    } catch {
      // A task can be visible before its batch result is published; refresh it again after completion.
    }
  }

  async function readMaterialFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) selectMaterialFile(file);
  }

  function dropMaterialFile(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    const file = event.dataTransfer.files?.[0];
    if (file) selectMaterialFile(file);
  }

  async function importMaterial(event: FormEvent) {
    event.preventDefault();
    if (!selectedKnowledgeBaseId) {
      setError({ material: "请先创建或选择一个知识库。" });
      return;
    }
    const errors: FieldErrors = {};
    if (!materialForm.title.trim()) errors.materialTitle = "请输入资料标题。";
    if (!selectedMaterialFile) errors.materialFile = "请选择 Markdown、TXT、PDF、DOC 或 DOCX 原始文件。";
    if (Object.keys(errors).length > 0) {
      setError(errors);
      return;
    }
    if (!selectedMaterialFile) return;
    setFormErrors((current) => ({ ...current, material: "", materialTitle: "", materialFile: "" }));
    const submission = await run(
      () => api.uploadMaterial(selectedKnowledgeBaseId, {
        file: selectedMaterialFile,
        title: materialForm.title.trim(),
        sourceType: materialForm.sourceType
      }),
      "资料已发送处理任务"
    );
    if (!submission) return;
    setSelectedMaterialFile(null);
    setGenerationForm((current) => ({ ...current, sourceKind: "material", sourceId: submission.materialId }));
    setSection("materials");
    await Promise.all([loadWorkbench(), openMaterial(submission.materialId), viewTaskStatus(submission.taskId)]);
  }

  async function openMaterial(materialId: string, revisionId?: string) {
    const nextDetail = await run(() => api.getMaterial(materialId));
    if (!nextDetail) return;
    setMaterialDetail(nextDetail);
    setMaterialReading(null);
    if (nextDetail.status === "READY" && (revisionId ?? nextDetail.currentRevisionId)) {
      try {
        setMaterialReading(await api.getMaterialReading(materialId, revisionId ?? nextDetail.currentRevisionId));
      } catch {
        // Detail metadata remains usable while the reading rendition is unavailable.
      }
    }
    setGenerationForm((current) => ({ ...current, sourceKind: "material", sourceId: materialId }));
  }

  async function reprocessMaterial(materialId: string) {
    const submission = await run(() => api.reprocessMaterial(materialId), "已重新提交资料处理任务");
    if (!submission) return;
    await Promise.all([loadWorkbench(), openMaterial(materialId), viewTaskStatus(submission.taskId)]);
  }

  function openCitationBlock(blockId?: string, pageNumber?: number) {
    const target = blockId
      ? document.getElementById(`document-block-${blockId}`)
      : pageNumber ? document.querySelector(`[data-page-number="${pageNumber}"]`) : null;
    target?.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  async function extractKnowledgePoints(materialId: string) {
    const extraction = await run(() => api.extractKnowledgePoints(materialId), "知识点已提取");
    const firstExtracted = extraction?.knowledgePoints[0];
    if (firstExtracted) setSelectedKnowledgePointId(firstExtracted.id);
    if (extraction) {
      setExtractionTasks((current) => ({ ...current, [materialId]: extraction.task }));
      setMaterialDetail((current) =>
        current?.id === materialId
          ? { ...current, extractedKnowledgePoints: extraction.knowledgePoints }
          : current
      );
    }
    await loadWorkbench();
  }

  async function openKnowledgePoint(knowledgePointId: string) {
    const point = await run(() => api.getKnowledgePoint(knowledgePointId));
    if (point) {
      setKnowledgePoints((current) => current.map((item) => item.id === point.id ? point : item));
    }
    setSelectedKnowledgePointId(knowledgePointId);
    setSection("overview");
  }

  async function updateKnowledgePoint(knowledgePointId: string, payload: Parameters<typeof api.updateKnowledgePoint>[1]) {
    const point = await run(() => api.updateKnowledgePoint(knowledgePointId, payload), "知识点已更新");
    if (!point) return;
    setKnowledgePoints((current) => current.map((item) => item.id === point.id ? point : item));
  }

  async function reviewKnowledgePoint(knowledgePoint: KnowledgePoint, action: "confirm" | "reject") {
    if (knowledgePoint.legacy) return;
    const point = await run(
      () => action === "confirm" ? api.confirmKnowledgePoint(knowledgePoint.id) : api.rejectKnowledgePoint(knowledgePoint.id),
      action === "confirm" ? "知识点已确认" : "知识点已拒绝"
    );
    if (!point) return;
    setKnowledgePoints((current) => current.map((item) => item.id === point.id ? point : item));
  }

  async function generateKnowledgePointQuestions(point: KnowledgePoint, settings: { count: number; difficulty: QuestionDifficulty; questionType: QuestionType }) {
    if (!isKnowledgePointQuestionEligible(point)) {
      setError({ knowledgePointGeneration: "仅可从来源仍有效的已确认结构化知识点生成面试题。" });
      return;
    }
    if (!Number.isInteger(settings.count) || settings.count < 1 || settings.count > 10) {
      setError({ knowledgePointQuestionCount: "数量必须是 1 到 10 之间的整数。" });
      return;
    }
    const submission = await run(
      () => api.generateKnowledgePointQuestions(point.id, settings),
      "面试题生成任务已提交，可继续阅读知识点。"
    );
    if (!submission) return;
    setQuestionGenerationTasks((current) => ({ ...current, [point.id]: submission.taskId }));
    await viewTaskStatus(submission.taskId);
  }

  async function openKnowledgePointCitation(citation: SourceCitation) {
    setSection("materials");
    await openMaterial(citation.materialId, citation.revisionId);
    requestAnimationFrame(() => openCitationBlock(citation.blockId, citation.pageNumber));
  }

  async function deleteMaterial(materialId: string) {
    const confirmed = window.confirm("删除资料后，它不会再参与后续搜索、问答和生成。继续？");
    if (!confirmed) return;
    const deletion = await run(() => api.deleteMaterial(materialId), "资料已删除");
    if (!deletion) return;
    setMaterials((current) => current.filter((item) => item.id !== materialId));
    if (materialDetail?.id === materialId) setMaterialDetail(null);
    setGenerationForm((current) => current.sourceId === materialId ? { ...current, sourceId: "" } : current);
    await loadWorkbench();
  }

  function buildSourceRef(): SourceRef | null {
    if (!selectedKnowledgeBaseId || !selectedSource) return null;
    if ("sourceRefs" in selectedSource && selectedSource.sourceRefs?.[0]) {
      return selectedSource.sourceRefs[0];
    }
    const title = "name" in selectedSource ? selectedSource.name : selectedSource.title;
    const type = generationForm.sourceKind === "knowledgePoint" ? "KNOWLEDGE_POINT" : "MATERIAL";
    return {
      type,
      id: selectedSource.id,
      knowledgeBaseId: selectedKnowledgeBaseId,
      title,
      materialId: "sourceMaterialId" in selectedSource ? selectedSource.sourceMaterialId ?? null : selectedSource.id,
      chunkId: null,
      deleted: false,
      excerpt: "description" in selectedSource ? selectedSource.description : undefined
    };
  }

  async function generateQuestion(event: FormEvent) {
    event.preventDefault();
    const sourceRef = buildSourceRef();
    if (!selectedKnowledgeBaseId || !sourceRef) {
      setError({ generationSource: "请先选择资料或知识点作为生成来源。" });
      return;
    }
    if (generationForm.sourceKind === "knowledgePoint") {
      if (!("legacy" in selectedSource) || !isKnowledgePointQuestionEligible(selectedSource)) {
        setError({ generationSource: "请先确认来源仍有效的结构化知识点，再生成面试题。" });
        return;
      }
      const submission = await run(
        () => api.generateKnowledgePointQuestions(selectedSource.id, { count: 1, difficulty: "MEDIUM", questionType: "SHORT_ANSWER" }),
        "面试题生成任务已提交，可继续阅读知识点。"
      );
      if (!submission) return;
      setQuestionGenerationTasks((current) => ({ ...current, [selectedSource.id]: submission.taskId }));
      await viewTaskStatus(submission.taskId);
      return;
    }
    const errors: FieldErrors = {};
    if (!generationForm.categoryId.trim()) errors.categoryId = "请输入分类 ID。";
    if (!generationForm.prompt.trim()) errors.prompt = "请输入生成要求。";
    if (Object.keys(errors).length > 0) {
      setError(errors);
      return;
    }
    setFormErrors((current) => ({ ...current, generationSource: "", categoryId: "", prompt: "" }));
    const pointIds = knowledgePoints.slice(0, 2).map((item) => item.id);
    const draft = await run(
      () =>
        api.generateQuestion({
          knowledgeBaseId: selectedKnowledgeBaseId,
          sourceRefs: [sourceRef],
          questionType: generationForm.questionType,
          categoryId: generationForm.categoryId.trim(),
          categoryName: generationForm.categoryName.trim(),
          knowledgePointIds: pointIds,
          prompt: generationForm.prompt.trim()
        }),
      "题目草稿已生成"
    );
    if (!draft) return;
    setSelectedDraftId(draft.id);
    await loadWorkbench();
    setSection("generate");
  }

  async function reviewDraft(status: GeneratedContentStatus, draft: DraftEditState) {
    if (!selectedDraft) return;
    if (status === "SAVED") {
      const errors: FieldErrors = {};
      if (!draft.stem.trim()) errors.draftStem = "题干不能为空。";
      if (!draft.answer.length) errors.draftAnswer = "至少保留一个答案。";
      if (!draft.explanation.trim()) errors.draftExplanation = "解析不能为空。";
      if (!draft.categoryId.trim()) errors.draftCategoryId = "分类 ID 不能为空。";
      if (!draft.categoryName.trim()) errors.draftCategoryName = "分类名称不能为空。";
      if (Object.keys(errors).length > 0) {
        setError(errors);
        return;
      }
    }
    setFormErrors((current) => ({
      ...current,
      draftStem: "",
      draftAnswer: "",
      draftExplanation: "",
      draftCategoryId: "",
      draftCategoryName: ""
    }));
    await run(
      () => selectedDraft.knowledgePointId
        ? api.reviewKnowledgePointQuestionDraft(selectedDraft.id, {
          contentKind: "KNOWLEDGE_POINT_INTERVIEW_QUESTION",
          action: status === "SAVED" ? "SAVE" : "DISCARD",
          stem: draft.stem.trim(),
          options: draft.options,
          answer: draft.answer,
          explanation: draft.explanation.trim(),
          difficulty: draft.difficulty,
          questionType: draft.questionType,
          categoryId: draft.categoryId.trim(),
          categoryName: draft.categoryName.trim()
        })
        : api.reviewGeneratedContent(selectedDraft.id, {
          status,
          stem: draft.stem.trim(),
          options: draft.options,
          answer: draft.answer,
          explanation: draft.explanation.trim(),
          categoryId: draft.categoryId.trim(),
          categoryName: draft.categoryName.trim(),
          knowledgePointIds: draft.knowledgePointIds,
            sourceRefs: selectedDraft.sourceRefs ?? []
        }),
      status === "SAVED" ? "题目已保存" : "草稿已处理"
    );
    setSelectedDraftId("");
    await loadWorkbench();
  }

  async function deleteDraft() {
    if (!selectedDraft) return;
    const confirmed = window.confirm("删除这条 AI 生成内容？");
    if (!confirmed) return;
    await run(() => api.deleteGeneratedContent(selectedDraft.id), "生成内容已删除");
    setSelectedDraftId("");
    await loadWorkbench();
  }

  async function generateExplanation() {
    const point = knowledgePoints[0];
    if (!selectedKnowledgeBaseId || !point) {
      setError({ aiNote: "请先从资料中提取至少一个知识点。" });
      return;
    }
    const note = await run(() =>
      api.generateExplanation({
        knowledgeBaseId: selectedKnowledgeBaseId,
        knowledgePointId: point.id,
        sourceRefs: point.sourceRefs?.length ? point.sourceRefs : [sourceRefForKnowledgePoint(point)],
        prompt: "生成简短解释"
      })
    );
    if (note) setAiNoteDraft(note);
  }

  async function generateReviewSuggestion() {
    const sourceRef = buildSourceRef();
    if (!selectedKnowledgeBaseId || !sourceRef) {
      setError({ aiNote: "请先选择可用于生成建议的资料或知识点。" });
      return;
    }
    const note = await run(() =>
      api.generateReviewSuggestion({
        knowledgeBaseId: selectedKnowledgeBaseId,
        sourceRefs: [sourceRef],
        weakKnowledgePointIds: knowledgePoints.slice(0, 3).map((item) => item.id),
        prompt: "生成下一步复习动作"
      })
    );
    if (note) setAiNoteDraft(note);
  }

  async function saveAiNote() {
    if (!aiNoteDraft) return;
    await run(
      () =>
        api.saveAiNote({
          draftId: aiNoteDraft.id,
          knowledgeBaseId: aiNoteDraft.knowledgeBaseId,
          type: aiNoteDraft.type,
          title: aiNoteDraft.title,
          content: aiNoteDraft.content,
          sourceRefs: aiNoteDraft.sourceRefs
        }),
      "笔记已保存"
    );
    setAiNoteDraft(null);
    await loadWorkbench();
  }

  async function submitSearch(event: FormEvent) {
    event.preventDefault();
    if (!selectedKnowledgeBaseId) {
      setError({ search: "请先创建或选择一个知识库。" });
      return;
    }
    if (!searchForm.query.trim()) {
      setError({ searchQuery: "请输入搜索关键词或自然语言问题。" });
      return;
    }
    setFormErrors((current) => ({ ...current, search: "", searchQuery: "" }));
    setHasSearched(true);
    const results = await run(() => api.search({ q: searchForm.query.trim(), knowledgeBaseId: selectedKnowledgeBaseId, limit: 10 }));
    if (results) setSearchResults(results);
  }

  async function askQuestion(event: FormEvent) {
    event.preventDefault();
    if (!selectedKnowledgeBaseId) {
      setError({ ask: "请先创建或选择一个知识库。" });
      return;
    }
    if (!searchForm.question.trim()) {
      setError({ askQuestion: "请输入要向资料提问的问题。" });
      return;
    }
    setFormErrors((current) => ({ ...current, ask: "", askQuestion: "" }));
    setHasAsked(true);
    const answer = await run(() => api.ask({ question: searchForm.question.trim(), knowledgeBaseId: selectedKnowledgeBaseId }));
    if (answer) setRagAnswer(answer);
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark"><BookOpen size={22} /></div>
          <div>
            <h1>SuiLearn</h1>
            <span>Knowledge Workbench</span>
          </div>
        </div>

        <form className="create-form" onSubmit={createKnowledgeBase}>
          <label>
            知识库名称
            <input
              value={kbForm.name}
              aria-invalid={Boolean(formErrors.kbName)}
              onChange={(event) => setKbForm({ ...kbForm, name: event.target.value })}
            />
            <FormError message={formErrors.kbName} />
          </label>
          <label>
            说明
            <input
              value={kbForm.description}
              onChange={(event) => setKbForm({ ...kbForm, description: event.target.value })}
            />
          </label>
          <button type="submit" className="primary-button" disabled={loading || !kbForm.name.trim()}>
            <Plus size={16} /> 新建知识库
          </button>
        </form>

        <nav className="kb-list" aria-label="知识库列表">
          {knowledgeBases.map((item) => (
            <button
              key={item.id}
              className={item.id === selectedKnowledgeBaseId ? "kb-item active" : "kb-item"}
              onClick={() => setSelectedKnowledgeBaseId(item.id)}
              type="button"
            >
              <Database size={17} />
              <span>{item.name}</span>
              <ChevronRight size={16} />
            </button>
          ))}
        </nav>

        <KnowledgeBaseManager
          form={kbManageForm}
          setForm={setKbManageForm}
          updateKnowledgeBase={updateKnowledgeBase}
          deleteKnowledgeBase={deleteKnowledgeBase}
          disabled={!selectedKnowledgeBaseId || loading}
          error={formErrors.kbManageName}
        />
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">第二版工作台</p>
            <h2>{selectedKnowledgeBase?.name ?? "暂无知识库"}</h2>
          </div>
          <button className="ghost-button" onClick={() => void refreshWorkbench()} disabled={!selectedKnowledgeBaseId || loading || providerLoading}>
            {loading || providerLoading ? <Loader2 className="spin" size={17} /> : <RefreshCw size={17} />} 刷新
          </button>
        </header>

        <div className="section-tabs" role="tablist">
          <TabButton active={section === "overview"} icon={<Archive size={17} />} label="总览" onClick={() => setSection("overview")} />
          <TabButton active={section === "materials"} icon={<FileText size={17} />} label="资料" onClick={() => setSection("materials")} />
          <TabButton active={section === "generate"} icon={<Sparkles size={17} />} label="AI 生成" onClick={() => setSection("generate")} />
          <TabButton active={section === "search"} icon={<Search size={17} />} label="搜索问答" onClick={() => setSection("search")} />
        </div>

        {toast && (
          <div className={`toast ${toast.tone}`} role="status">
            {toast.tone === "error" ? <AlertCircle size={18} /> : <Check size={18} />}
            {toast.message}
          </div>
        )}

        {!selectedKnowledgeBaseId && (
          <EmptyState
            icon={<Database size={22} />}
            title={loading ? "正在加载知识库" : "还没有知识库"}
            description={loading ? "正在连接二版 API，稍等片刻。" : "先创建一个知识库，再导入资料、生成题目或进行搜索问答。"}
            actionLabel={loading ? undefined : "创建默认知识库"}
            onAction={loading ? undefined : () => void submitCreateKnowledgeBase()}
          />
        )}

        {selectedKnowledgeBaseId && section === "overview" && (
          <OverviewPanel
            detail={detail}
            statistics={statistics}
            materials={activeMaterials}
            knowledgePoints={knowledgePoints}
            selectedKnowledgePoint={selectedKnowledgePoint}
            questions={questions}
            drafts={drafts}
            aiProviderStatus={aiProviderStatus}
            aiProviderError={aiProviderError}
            providerLoading={providerLoading}
            loading={loading}
            openKnowledgePoint={openKnowledgePoint}
            updateKnowledgePoint={updateKnowledgePoint}
            reviewKnowledgePoint={reviewKnowledgePoint}
            generateKnowledgePointQuestions={generateKnowledgePointQuestions}
            questionGenerationTasks={questionGenerationTasks}
            taskQuestionDrafts={questionDraftsByTask}
            taskStatuses={taskStatuses}
            taskErrors={taskErrors}
            taskLoading={taskLoading}
            viewTaskStatus={viewTaskStatus}
            openCitation={openKnowledgePointCitation}
            errors={formErrors}
            goToMaterials={() => setSection("materials")}
            goToGenerate={() => setSection("generate")}
          />
        )}

        {selectedKnowledgeBaseId && section === "materials" && (
          <MaterialsPanel
            materialForm={materialForm}
            setMaterialForm={setMaterialForm}
            readMaterialFile={readMaterialFile}
            dropMaterialFile={dropMaterialFile}
            importMaterial={importMaterial}
            materials={activeMaterials}
            materialDetail={materialDetail}
            materialReading={materialReading}
            openMaterial={openMaterial}
            openKnowledgePoint={openKnowledgePoint}
            extractKnowledgePoints={extractKnowledgePoints}
            deleteMaterial={deleteMaterial}
            reprocessMaterial={reprocessMaterial}
            openCitationBlock={openCitationBlock}
            taskStatuses={taskStatuses}
            taskErrors={taskErrors}
            taskLoading={taskLoading}
            extractionTasks={extractionTasks}
            viewTaskStatus={viewTaskStatus}
            loading={loading}
            errors={formErrors}
          />
        )}

        {selectedKnowledgeBaseId && section === "generate" && (
          <GeneratePanel
            form={generationForm}
            setForm={setGenerationForm}
            materials={activeMaterials}
            knowledgePoints={knowledgePoints}
            questionTypes={questionTypes}
            generateQuestion={generateQuestion}
            drafts={visibleDrafts}
            contentFilter={contentFilter}
            setContentFilter={setContentFilter}
            selectedDraft={selectedDraft}
            setSelectedDraftId={setSelectedDraftId}
            reviewDraft={reviewDraft}
            deleteDraft={deleteDraft}
            generateExplanation={generateExplanation}
            generateReviewSuggestion={generateReviewSuggestion}
            aiNoteDraft={aiNoteDraft}
            saveAiNote={saveAiNote}
            taskStatuses={taskStatuses}
            taskErrors={taskErrors}
            taskLoading={taskLoading}
            viewTaskStatus={viewTaskStatus}
            taskQuestionDrafts={questionDraftsByTask}
            openCitation={openKnowledgePointCitation}
            loading={loading}
            errors={formErrors}
            goToMaterials={() => setSection("materials")}
          />
        )}

        {selectedKnowledgeBaseId && section === "search" && (
          <SearchPanel
            form={searchForm}
            setForm={setSearchForm}
            submitSearch={submitSearch}
            askQuestion={askQuestion}
            searchResults={searchResults}
            ragAnswer={ragAnswer}
            knowledgeBases={knowledgeBases}
            knowledgePoints={knowledgePoints}
            loading={loading}
            errors={formErrors}
            hasSearched={hasSearched}
            hasAsked={hasAsked}
          />
        )}
      </main>
    </div>
  );
}

function TabButton({ active, icon, label, onClick }: { active: boolean; icon: React.ReactNode; label: string; onClick: () => void }) {
  return (
    <button className={active ? "tab-button active" : "tab-button"} onClick={onClick} type="button">
      {icon}
      {label}
    </button>
  );
}

function KnowledgeBaseManager(props: {
  form: { name: string; description: string };
  setForm: (value: { name: string; description: string }) => void;
  updateKnowledgeBase: (event: FormEvent) => Promise<void>;
  deleteKnowledgeBase: () => Promise<void>;
  disabled: boolean;
  error?: string;
}) {
  return (
    <form className="create-form" onSubmit={props.updateKnowledgeBase}>
      <div className="mini-heading">
        <Pencil size={16} />
        <span>管理当前知识库</span>
      </div>
      <label>
        名称
        <input
          value={props.form.name}
          onChange={(event) => props.setForm({ ...props.form, name: event.target.value })}
          disabled={props.disabled}
          aria-invalid={Boolean(props.error)}
        />
        <FormError message={props.error} />
      </label>
      <label>
        说明
        <input
          value={props.form.description}
          onChange={(event) => props.setForm({ ...props.form, description: event.target.value })}
          disabled={props.disabled}
        />
      </label>
      <div className="button-row compact-actions">
        <button type="submit" className="ghost-button" disabled={props.disabled || !props.form.name.trim()}>
          <Save size={16} /> 更新
        </button>
        <button type="button" className="danger-button" disabled={props.disabled} onClick={() => void props.deleteKnowledgeBase()}>
          <Trash2 size={16} /> 删除
        </button>
      </div>
    </form>
  );
}

function OverviewPanel({
  detail,
  statistics,
  materials,
  knowledgePoints,
  selectedKnowledgePoint,
  questions,
  drafts,
  aiProviderStatus,
  aiProviderError,
  providerLoading,
  loading,
  openKnowledgePoint,
  updateKnowledgePoint,
  reviewKnowledgePoint,
  generateKnowledgePointQuestions,
  questionGenerationTasks,
  taskQuestionDrafts,
  taskStatuses,
  taskErrors,
  taskLoading,
  viewTaskStatus,
  openCitation,
  errors,
  goToMaterials,
  goToGenerate
}: {
  detail: KnowledgeBaseDetail | null;
  statistics: KnowledgeBaseStatistics | null;
  materials: MaterialMetadata[];
  knowledgePoints: KnowledgePoint[];
  selectedKnowledgePoint: KnowledgePoint | null;
  questions: QuestionSummary[];
  drafts: GeneratedQuestionDraft[];
  aiProviderStatus: AiProviderStatus | null;
  aiProviderError: string;
  providerLoading: boolean;
  loading: boolean;
  openKnowledgePoint: (id: string) => Promise<void>;
  updateKnowledgePoint: (id: string, payload: Parameters<typeof api.updateKnowledgePoint>[1]) => Promise<void>;
  reviewKnowledgePoint: (point: KnowledgePoint, action: "confirm" | "reject") => Promise<void>;
  generateKnowledgePointQuestions: (point: KnowledgePoint, settings: { count: number; difficulty: QuestionDifficulty; questionType: QuestionType }) => Promise<void>;
  questionGenerationTasks: Record<string, string>;
  taskQuestionDrafts: Record<string, GeneratedQuestionDraft[]>;
  openCitation: (citation: SourceCitation) => Promise<void>;
  taskStatuses: TaskStatusMap;
  taskErrors: TaskErrorMap;
  taskLoading: Record<string, boolean>;
  viewTaskStatus: (id: string) => Promise<void>;
  errors: FieldErrors;
  goToMaterials: () => void;
  goToGenerate: () => void;
}) {
  return (
    <section className="panel-grid">
      <div className="metrics">
        {loading && !detail ? (
          <LoadingMetrics />
        ) : (
          <>
            <Metric label="资料" value={statistics?.materialCount ?? detail?.materialCount ?? materials.length} />
            <Metric label="就绪资料" value={statistics?.readyMaterialCount ?? materials.filter((item) => item.status === "READY").length} />
            <Metric label="知识点" value={statistics?.knowledgePointCount ?? detail?.knowledgePointCount ?? knowledgePoints.length} />
            <Metric label="待确认" value={statistics?.pendingGeneratedContentCount ?? drafts.filter((draft) => draft.status === "PENDING_REVIEW").length} />
          </>
        )}
      </div>
      <div className="panel wide">
        <div className="panel-heading">
          <h3>AI Provider</h3>
          <Bot size={18} />
        </div>
        <ProviderStatusCard status={aiProviderStatus} error={aiProviderError} loading={providerLoading} />
      </div>
      <div className="panel wide">
        <div className="panel-heading">
          <h3>最近资料</h3>
          <FileText size={18} />
        </div>
        <div className="item-list">
          {loading && materials.length === 0 && <LoadingRows count={3} />}
          {!loading && materials.length === 0 && (
            <EmptyState
              compact
              icon={<FileText size={20} />}
              title="还没有资料"
              description="导入 Markdown、TXT 或已转成文本的 PDF 内容后，才能提取知识点和生成题目。"
              actionLabel="去导入资料"
              onAction={goToMaterials}
            />
          )}
          {materials.slice(0, 5).map((item) => (
            <div className="row-item" key={item.id}>
              <span>{item.title}</span>
              <StatusPill label={item.status} />
            </div>
          ))}
        </div>
      </div>
      <div className="panel">
        <div className="panel-heading">
          <h3>知识点</h3>
          <Database size={18} />
        </div>
        <div className="tag-cloud">
          {knowledgePoints.slice(0, 12).map((item) => (
            <KnowledgePointChip
              key={item.id}
              point={item}
              active={selectedKnowledgePoint?.id === item.id}
              onOpen={openKnowledgePoint}
            />
          ))}
          {!loading && knowledgePoints.length === 0 && (
            <EmptyState
              compact
              icon={<Sparkles size={20} />}
              title="还没有知识点"
              description="从资料列表中点击提取按钮，生成可编辑的知识点。"
              actionLabel="查看资料"
              onAction={goToMaterials}
            />
          )}
        </div>
        {selectedKnowledgePoint && (
          <KnowledgePointDetail
            point={selectedKnowledgePoint}
            updateKnowledgePoint={updateKnowledgePoint}
            reviewKnowledgePoint={reviewKnowledgePoint}
            generateQuestions={generateKnowledgePointQuestions}
            generationTaskId={questionGenerationTasks[selectedKnowledgePoint.id]}
            taskDrafts={questionGenerationTasks[selectedKnowledgePoint.id] ? taskQuestionDrafts[questionGenerationTasks[selectedKnowledgePoint.id]] ?? [] : []}
            taskStatuses={taskStatuses}
            taskErrors={taskErrors}
            taskLoading={taskLoading}
            viewTaskStatus={viewTaskStatus}
            openCitation={openCitation}
            errors={errors}
          />
        )}
      </div>
      <div className="panel">
        <div className="panel-heading">
          <h3>已保存题</h3>
          <BookOpen size={18} />
        </div>
        <div className="item-list compact">
          {questions.slice(0, 4).map((item) => <div className="row-item" key={item.id}>{item.stem}</div>)}
          {!loading && questions.length === 0 && (
            <EmptyState
              compact
              icon={<BookOpen size={20} />}
              title="还没有保存题目"
              description="先生成草稿并确认保存，题目会在这里出现。"
              actionLabel="去生成"
              onAction={goToGenerate}
            />
          )}
        </div>
      </div>
    </section>
  );
}

function MaterialsPanel(props: {
  materialForm: { title: string; fileName: string; sourceType: MaterialSourceType };
  setMaterialForm: (value: { title: string; fileName: string; sourceType: MaterialSourceType }) => void;
  readMaterialFile: (event: ChangeEvent<HTMLInputElement>) => Promise<void>;
  dropMaterialFile: (event: DragEvent<HTMLDivElement>) => void;
  importMaterial: (event: FormEvent) => Promise<void>;
  materials: MaterialMetadata[];
  materialDetail: MaterialDetail | null;
  materialReading: MaterialReadingDocument | null;
  openMaterial: (id: string) => Promise<void>;
  openKnowledgePoint: (id: string) => void;
  extractKnowledgePoints: (id: string) => Promise<void>;
  deleteMaterial: (id: string) => Promise<void>;
  reprocessMaterial: (id: string) => Promise<void>;
  openCitationBlock: (blockId?: string) => void;
  taskStatuses: TaskStatusMap;
  taskErrors: TaskErrorMap;
  taskLoading: Record<string, boolean>;
  extractionTasks: Record<string, TaskStatus>;
  viewTaskStatus: (id: string) => Promise<void>;
  loading: boolean;
  errors: FieldErrors;
}) {
  const { materialForm, setMaterialForm } = props;
  return (
    <section className="two-column">
      <form className="panel form-panel" onSubmit={props.importMaterial}>
        <div className="panel-heading">
          <h3>导入资料</h3>
          <FileText size={18} />
        </div>
        <div className="upload-dropzone" onDragOver={(event) => event.preventDefault()} onDrop={props.dropMaterialFile}>
          <Upload size={22} />
          <strong>拖放原始资料到这里</strong>
          <span>支持 Markdown、TXT、PDF、DOC、DOCX，文件将直接上传而不会在浏览器中转为文本。</span>
          <label className="ghost-button file-picker">
            选择文件
            <input type="file" accept=".md,.markdown,.txt,.pdf,.doc,.docx,text/markdown,text/plain,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document" onChange={props.readMaterialFile} />
          </label>
        </div>
        {materialForm.fileName && <div className="file-hint"><Upload size={15} /> {materialForm.fileName}</div>}
        <FormError message={props.errors.materialFile} />
        <label>
          标题
          <input
            value={materialForm.title}
            aria-invalid={Boolean(props.errors.materialTitle)}
            onChange={(event) => setMaterialForm({ ...materialForm, title: event.target.value })}
          />
          <FormError message={props.errors.materialTitle} />
        </label>
        <label>
          类型
          <select
            value={materialForm.sourceType}
            onChange={(event) => setMaterialForm({ ...materialForm, sourceType: event.target.value as MaterialSourceType })}
          >
            {sourceTypes.map((item) => <option key={item}>{item}</option>)}
          </select>
        </label>
        <button className="primary-button" disabled={props.loading || !materialForm.title.trim() || !materialForm.fileName}>
          {props.loading ? <Loader2 className="spin" size={16} /> : <Save size={16} />} 导入
        </button>
      </form>

      <div className="panel">
        <div className="panel-heading">
          <h3>资料列表</h3>
          <Archive size={18} />
        </div>
        <div className="item-list">
          {props.loading && props.materials.length === 0 && <LoadingRows count={3} />}
          {props.materials.map((item) => (
            <div className="row-item action-row" key={item.id}>
              <button type="button" onClick={() => void props.openMaterial(item.id)}>
                <FileText size={16} />
                <span>
                  {readableMaterialText(item.title)}
                  <small>import {shortId(item.importTaskId)}{item.embeddingTaskId ? ` · embed ${shortId(item.embeddingTaskId)}` : ""}</small>
                </span>
              </button>
              <div className="row-actions">
                <StatusPill label={item.sourceType} />
                <StatusPill label={item.status} />
                <button title="提取知识点" aria-label="提取知识点" onClick={() => void props.extractKnowledgePoints(item.id)}>
                  <Sparkles size={16} />
                </button>
                <button title="删除资料" aria-label="删除资料" onClick={() => void props.deleteMaterial(item.id)}>
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))}
          {!props.loading && props.materials.length === 0 && (
            <EmptyState
              compact
              icon={<Upload size={20} />}
              title="还没有资料"
              description="在左侧粘贴资料正文，或选择本地 Markdown/TXT/PDF 文本内容后导入。"
            />
          )}
        </div>
        {props.materialDetail && (
          <div className="detail-box">
            <h4>{readableMaterialText(props.materialDetail.title)}</h4>
            <div className="metadata-grid">
              <MetaItem label="处理任务" value={props.materialDetail.processingTaskId ?? props.materialDetail.importTaskId} />
              {props.materialDetail.embeddingTaskId && <MetaItem label="Embedding 任务" value={props.materialDetail.embeddingTaskId} />}
              <MetaItem label="资料状态" value={props.materialDetail.status} />
              {props.materialDetail.errorMessage && <MetaItem label="错误" value={props.materialDetail.errorMessage} />}
            </div>
            <div className="button-row task-actions">
              <TaskStatusButton
                taskId={props.materialDetail.processingTaskId ?? props.materialDetail.importTaskId}
                label="查看处理任务"
                loading={props.taskLoading[props.materialDetail.processingTaskId ?? props.materialDetail.importTaskId]}
                onView={props.viewTaskStatus}
              />
              {props.materialDetail.embeddingTaskId && (
                <TaskStatusButton
                  taskId={props.materialDetail.embeddingTaskId}
                  label="查看 Embedding 任务"
                  loading={props.taskLoading[props.materialDetail.embeddingTaskId]}
                  onView={props.viewTaskStatus}
                />
              )}
            </div>
            <TaskStatusCard
              task={props.taskStatuses[props.materialDetail.processingTaskId ?? props.materialDetail.importTaskId]}
              error={props.taskErrors[props.materialDetail.processingTaskId ?? props.materialDetail.importTaskId]}
            />
            {props.materialDetail.embeddingTaskId && (
              <TaskStatusCard
                task={props.taskStatuses[props.materialDetail.embeddingTaskId]}
                error={props.taskErrors[props.materialDetail.embeddingTaskId]}
              />
            )}
            <TaskStatusCard task={props.extractionTasks[props.materialDetail.id]} />
            {(props.materialDetail.status === "FAILED" || props.taskStatuses[props.materialDetail.processingTaskId ?? props.materialDetail.importTaskId]?.status === "RETRY_WAIT") && (
              <button className="ghost-button" onClick={() => props.materialDetail && void props.reprocessMaterial(props.materialDetail.id)}>
                <RefreshCw size={16} /> 重试处理
              </button>
            )}
            <MaterialContentReader
              detail={props.materialDetail}
              reading={props.materialReading}
              onCitation={props.openCitationBlock}
            />
            <KnowledgePointSummaryList
              points={props.materialDetail.extractedKnowledgePoints ?? []}
              onOpen={props.openKnowledgePoint}
              emptyLabel="这份资料还没有提取知识点"
            />
          </div>
        )}
      </div>
    </section>
  );
}

function GeneratePanel(props: {
  form: {
    sourceKind: "material" | "knowledgePoint";
    sourceId: string;
    questionType: QuestionType;
    categoryId: string;
    categoryName: string;
    prompt: string;
  };
  setForm: (value: GenerationFormState) => void;
  materials: MaterialMetadata[];
  knowledgePoints: KnowledgePoint[];
  questionTypes: QuestionType[];
  generateQuestion: (event: FormEvent) => Promise<void>;
  drafts: GeneratedQuestionDraft[];
  contentFilter: ContentFilter;
  setContentFilter: (status: ContentFilter) => void;
  selectedDraft: GeneratedQuestionDraft | null;
  setSelectedDraftId: (id: string) => void;
  reviewDraft: (status: GeneratedContentStatus, draft: DraftEditState) => Promise<void>;
  deleteDraft: () => Promise<void>;
  generateExplanation: () => Promise<void>;
  generateReviewSuggestion: () => Promise<void>;
  aiNoteDraft: AiNoteDraft | null;
  saveAiNote: () => Promise<void>;
  taskStatuses: TaskStatusMap;
  taskErrors: TaskErrorMap;
  taskLoading: Record<string, boolean>;
  viewTaskStatus: (id: string) => Promise<void>;
  taskQuestionDrafts: Record<string, GeneratedQuestionDraft[]>;
  openCitation: (citation: SourceCitation) => Promise<void>;
  loading: boolean;
  errors: FieldErrors;
  goToMaterials: () => void;
}) {
  const form = props.form;
  const sources = form.sourceKind === "knowledgePoint" ? props.knowledgePoints : props.materials;
  const [draftEdit, setDraftEdit] = useState<DraftEditState>(emptyDraftEdit);

  useEffect(() => {
    if (!props.selectedDraft) {
      setDraftEdit(emptyDraftEdit);
      return;
    }
    setDraftEdit({
      stem: props.selectedDraft.stem,
      optionsText: (props.selectedDraft.options ?? []).join("\n"),
      options: props.selectedDraft.options ?? [],
      answerText: props.selectedDraft.answer.join("\n"),
      answer: props.selectedDraft.answer,
      explanation: props.selectedDraft.explanation,
      categoryId: props.selectedDraft.categoryId,
      categoryName: props.selectedDraft.categoryName,
      difficulty: props.selectedDraft.difficulty ?? "MEDIUM",
      questionType: props.selectedDraft.questionType,
      knowledgePointIdsText: props.selectedDraft.knowledgePointIds.join("\n"),
      knowledgePointIds: props.selectedDraft.knowledgePointIds
    });
  }, [props.selectedDraft?.id]);

  function updateDraftEdit(patch: Partial<DraftEditState>) {
    setDraftEdit((current) => {
      const next = { ...current, ...patch };
      if (patch.optionsText !== undefined) next.options = lines(patch.optionsText);
      if (patch.answerText !== undefined) next.answer = lines(patch.answerText);
      if (patch.knowledgePointIdsText !== undefined) next.knowledgePointIds = lines(patch.knowledgePointIdsText);
      return next;
    });
  }

  return (
    <section className="two-column">
      <form className="panel form-panel" onSubmit={props.generateQuestion}>
        <div className="panel-heading">
          <h3>生成题目</h3>
          <Bot size={18} />
        </div>
        <div className="segmented">
          <button type="button" className={form.sourceKind === "material" ? "active" : ""} onClick={() => props.setForm({ ...form, sourceKind: "material", sourceId: props.materials[0]?.id ?? "" })}>
            资料
          </button>
          <button type="button" className={form.sourceKind === "knowledgePoint" ? "active" : ""} onClick={() => props.setForm({ ...form, sourceKind: "knowledgePoint", sourceId: props.knowledgePoints[0]?.id ?? "" })}>
            知识点
          </button>
        </div>
        <label>
          来源
          <select
            value={form.sourceId}
            disabled={sources.length === 0}
            aria-invalid={Boolean(props.errors.generationSource)}
            onChange={(event) => props.setForm({ ...form, sourceId: event.target.value })}
          >
            {sources.map((item) => <option key={item.id} value={item.id}>{"name" in item ? item.name : item.title}</option>)}
          </select>
          <FormError message={props.errors.generationSource} />
        </label>
        {sources.length === 0 && (
          <EmptyState
            compact
            icon={<FileText size={20} />}
            title={form.sourceKind === "knowledgePoint" ? "还没有知识点来源" : "还没有资料来源"}
            description={form.sourceKind === "knowledgePoint" ? "先在资料页导入资料并提取知识点。" : "先导入一份资料，再用它生成题目。"}
            actionLabel="去资料页"
            onAction={props.goToMaterials}
          />
        )}
        <label>
          题型
          <select value={form.questionType} onChange={(event) => props.setForm({ ...form, questionType: event.target.value as QuestionType })}>
            {props.questionTypes.map((item) => <option key={item}>{item}</option>)}
          </select>
        </label>
        <div className="field-row">
          <label>
            分类 ID
            <input
              value={form.categoryId}
              aria-invalid={Boolean(props.errors.categoryId)}
              onChange={(event) => props.setForm({ ...form, categoryId: event.target.value })}
            />
            <FormError message={props.errors.categoryId} />
          </label>
          <label>
            分类名
            <input value={form.categoryName} onChange={(event) => props.setForm({ ...form, categoryName: event.target.value })} />
          </label>
        </div>
        <label>
          Prompt
          <textarea
            rows={4}
            value={form.prompt}
            aria-invalid={Boolean(props.errors.prompt)}
            onChange={(event) => props.setForm({ ...form, prompt: event.target.value })}
          />
          <FormError message={props.errors.prompt} />
        </label>
        <button className="primary-button" disabled={props.loading || !form.sourceId}>
          {props.loading ? <Loader2 className="spin" size={16} /> : <Sparkles size={16} />} 生成草稿
        </button>
      </form>

      <div className="panel">
        <div className="panel-heading">
          <h3>确认队列</h3>
          <Check size={18} />
        </div>
        <div className="status-filter">
          {(["PENDING_REVIEW", "SAVED", "DISCARDED", "DELETED", "ALL"] as ContentFilter[]).map((status) => (
            <button
              key={status}
              type="button"
              className={props.contentFilter === status ? "active" : ""}
              onClick={() => props.setContentFilter(status)}
            >
              {status}
            </button>
          ))}
        </div>
        <div className="draft-layout">
          <div className="draft-list">
            {props.drafts.map((draft) => (
              <button key={draft.id} type="button" onClick={() => props.setSelectedDraftId(draft.id)}>
                <span>
                  {draft.stem}
                  <small>task {shortId(draft.generationTaskId)}</small>
                </span>
                <StatusPill label={draft.status} />
              </button>
            ))}
            {props.loading && props.drafts.length === 0 && <LoadingRows count={2} />}
            {!props.loading && props.drafts.length === 0 && (
              <EmptyState
                compact
                icon={<Sparkles size={20} />}
                title="没有符合筛选的草稿"
                description="生成题目后，待确认、已保存或已丢弃的内容会按状态出现在这里。"
              />
            )}
          </div>
          {props.selectedDraft && (
            <div className="draft-detail">
              <label>
                题干
                <textarea
                  rows={3}
                  value={draftEdit.stem}
                  aria-invalid={Boolean(props.errors.draftStem)}
                  onChange={(event) => updateDraftEdit({ stem: event.target.value })}
                />
                <FormError message={props.errors.draftStem} />
              </label>
              <div className="metadata-grid">
                <MetaItem label="生成任务" value={props.selectedDraft.generationTaskId} />
                <MetaItem label="草稿状态" value={props.selectedDraft.status} />
                {props.selectedDraft.savedQuestionId && <MetaItem label="保存题目" value={props.selectedDraft.savedQuestionId} />}
              </div>
              <div className="button-row task-actions">
                <TaskStatusButton
                  taskId={props.selectedDraft.generationTaskId}
                  label="查看生成任务"
                  loading={props.taskLoading[props.selectedDraft.generationTaskId]}
                  onView={props.viewTaskStatus}
                />
              </div>
              <TaskStatusCard
                task={props.taskStatuses[props.selectedDraft.generationTaskId]}
                error={props.taskErrors[props.selectedDraft.generationTaskId]}
              />
              <label>
                选项（每行一项）
                <textarea rows={4} value={draftEdit.optionsText} onChange={(event) => updateDraftEdit({ optionsText: event.target.value })} />
              </label>
              <label>
                答案（每行一项）
                <textarea
                  rows={2}
                  value={draftEdit.answerText}
                  aria-invalid={Boolean(props.errors.draftAnswer)}
                  onChange={(event) => updateDraftEdit({ answerText: event.target.value })}
                />
                <FormError message={props.errors.draftAnswer} />
              </label>
              <label>
                解析
                <textarea
                  rows={4}
                  value={draftEdit.explanation}
                  aria-invalid={Boolean(props.errors.draftExplanation)}
                  onChange={(event) => updateDraftEdit({ explanation: event.target.value })}
                />
                <FormError message={props.errors.draftExplanation} />
              </label>
              <div className="field-row">
                {props.selectedDraft.knowledgePointId && <label>
                  难度
                  <select value={draftEdit.difficulty} onChange={(event) => updateDraftEdit({ difficulty: event.target.value as QuestionDifficulty })}><option>EASY</option><option>MEDIUM</option><option>HARD</option></select>
                </label>}
                {props.selectedDraft.knowledgePointId && <label>
                  题型
                  <select value={draftEdit.questionType} onChange={(event) => updateDraftEdit({ questionType: event.target.value as QuestionType })}>{props.questionTypes.map((type) => <option key={type}>{type}</option>)}</select>
                </label>}
                <label>
                  分类 ID
                  <input
                    value={draftEdit.categoryId}
                    aria-invalid={Boolean(props.errors.draftCategoryId)}
                    onChange={(event) => updateDraftEdit({ categoryId: event.target.value })}
                  />
                  <FormError message={props.errors.draftCategoryId} />
                </label>
                <label>
                  分类名
                  <input
                    value={draftEdit.categoryName}
                    aria-invalid={Boolean(props.errors.draftCategoryName)}
                    onChange={(event) => updateDraftEdit({ categoryName: event.target.value })}
                  />
                  <FormError message={props.errors.draftCategoryName} />
                </label>
              </div>
              <label>
                知识点 ID（每行一项）
                <textarea rows={2} value={draftEdit.knowledgePointIdsText} onChange={(event) => updateDraftEdit({ knowledgePointIdsText: event.target.value })} />
              </label>
              <div className="source-list">
                {(props.selectedDraft.sourceRefs ?? []).map((ref) => (
                  <span key={`${ref.type}-${ref.id}`}>{sourceRefLabel(ref)}</span>
                ))}
                {(props.selectedDraft.citations ?? []).map((citation, index) => (
                  <button key={`${citation.revisionId}-${citation.blockId ?? index}`} type="button" className="citation-anchor" onClick={() => void props.openCitation(citation)}>
                    {citation.pageNumber ? `第 ${citation.pageNumber} 页` : "资料段落"}：{readableMaterialText(citation.excerpt)}
                  </button>
                ))}
              </div>
              <div className="button-row">
                <button className="primary-button" onClick={() => void props.reviewDraft("SAVED", draftEdit)}>
                  {props.loading ? <Loader2 className="spin" size={16} /> : <Save size={16} />} 保存
                </button>
                <button className="ghost-button" onClick={() => void props.reviewDraft("DISCARDED", draftEdit)}>
                  <X size={16} /> 丢弃
                </button>
                <button className="danger-button" type="button" onClick={() => void props.deleteDraft()}>
                  <Trash2 size={16} /> 删除
                </button>
              </div>
            </div>
          )}
        </div>
        <div className="note-actions">
          <button className="ghost-button" onClick={() => void props.generateExplanation()}>
            <MessageSquareText size={16} /> 解释
          </button>
          <button className="ghost-button" onClick={() => void props.generateReviewSuggestion()}>
            <Sparkles size={16} /> 建议
          </button>
        </div>
        {props.aiNoteDraft && (
          <div className="detail-box">
            <h4>{props.aiNoteDraft.title}</h4>
            <div className="metadata-grid">
              <MetaItem label="生成任务" value={props.aiNoteDraft.generationTaskId} />
              <MetaItem label="笔记类型" value={props.aiNoteDraft.type} />
            </div>
            <div className="button-row task-actions">
              <TaskStatusButton
                taskId={props.aiNoteDraft.generationTaskId}
                label="查看笔记任务"
                loading={props.taskLoading[props.aiNoteDraft.generationTaskId]}
                onView={props.viewTaskStatus}
              />
            </div>
            <TaskStatusCard
              task={props.taskStatuses[props.aiNoteDraft.generationTaskId]}
              error={props.taskErrors[props.aiNoteDraft.generationTaskId]}
            />
            <p>{props.aiNoteDraft.content}</p>
            <button className="primary-button" onClick={() => void props.saveAiNote()}>
              <Save size={16} /> 保存笔记
            </button>
          </div>
        )}
      </div>
    </section>
  );
}

type GenerationFormState = {
  sourceKind: "material" | "knowledgePoint";
  sourceId: string;
  questionType: QuestionType;
  categoryId: string;
  categoryName: string;
  prompt: string;
};

type DraftEditState = {
  stem: string;
  optionsText: string;
  options: string[];
  answerText: string;
  answer: string[];
  explanation: string;
  categoryId: string;
  categoryName: string;
  knowledgePointIdsText: string;
  knowledgePointIds: string[];
  difficulty: QuestionDifficulty;
  questionType: QuestionType;
};

const emptyDraftEdit: DraftEditState = {
  stem: "",
  optionsText: "",
  options: [],
  answerText: "",
  answer: [],
  explanation: "",
  categoryId: "",
  categoryName: "",
  knowledgePointIdsText: "",
  knowledgePointIds: [],
  difficulty: "MEDIUM",
  questionType: "SHORT_ANSWER"
};

type KnowledgePointEditState = {
  title: string;
  shortSummary: string;
  definition: string;
  principles: string;
  applicationScenarios: string;
  pitfalls: string;
};

function knowledgePointEditState(point: KnowledgePoint): KnowledgePointEditState {
  if (point.legacy) return { title: point.name, shortSummary: point.description, definition: "", principles: "", applicationScenarios: "", pitfalls: "" };
  return {
    title: point.title,
    shortSummary: point.shortSummary,
    definition: point.definition,
    principles: point.principles.join("\n"),
    applicationScenarios: point.applicationScenarios.join("\n"),
    pitfalls: point.pitfalls.join("\n")
  };
}

function isKnowledgePointQuestionEligible(point: KnowledgePoint) {
  return point.legacy === false
    && point.status === "CONFIRMED"
    && !point.sourceOutdated
    && point.citations.some((citation) => citation.deleted !== true && Boolean(citation.materialId) && Boolean(citation.revisionId) && Boolean(citation.blockId ?? citation.pageNumber));
}

function StructuredKnowledgePointContent({ point }: { point: Extract<KnowledgePoint, { legacy: false }> }) {
  return (
    <>
      <p>{readableMaterialText(point.shortSummary)}</p>
      <div className="knowledge-point-sections">
        <KnowledgePointSection title="定义" content={point.definition} />
        <KnowledgePointSection title="核心原理" items={point.principles} />
        <KnowledgePointSection title="应用场景" items={point.applicationScenarios} />
        <KnowledgePointSection title="易错点" items={point.pitfalls} />
      </div>
    </>
  );
}

function KnowledgePointSection({ title, content, items }: { title: string; content?: string; items?: string[] }) {
  return <section><h5>{title}</h5>{content ? <p>{readableMaterialText(content)}</p> : <ul>{items?.map((item) => <li key={item}>{readableMaterialText(item)}</li>)}</ul>}</section>;
}

function lines(value: string) {
  return value.split(/\r?\n/).map((item) => item.trim()).filter(Boolean);
}

function sourceRefForKnowledgePoint(point: KnowledgePoint): SourceRef {
  return {
    type: "KNOWLEDGE_POINT",
    id: point.id,
    knowledgeBaseId: point.knowledgeBaseId,
    title: point.name,
    excerpt: point.description,
    deleted: false
  };
}

function sourceRefLabel(ref: SourceRef) {
  if (ref.deleted) return "来源已删除";
  const label = readableMaterialText(ref.title ?? "");
  return label || shortId(ref.id);
}

function readableMaterialText(value: string) {
  return decodePercentText(value)
    .replace(/!\[[^\]]*]\s*\([^)]*\)/g, "")
    .replace(/\[([^\]]*)]\s*\(([^)]*)\)/g, (_match, rawLabel: string, rawUrl: string) => {
      const label = decodePercentText(rawLabel).trim();
      if (label && label !== "#") return label;
      return readableUrlLabel(rawUrl);
    })
    .replace(/\((https?:\/\/[^)\s]+)\)/g, (_match, rawUrl: string) => readableUrlLabel(rawUrl))
    .replace(/https?:\/\/[^\s)]+/g, (rawUrl) => readableUrlLabel(rawUrl))
    .replace(/[ \t]{2,}/g, " ")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function decodePercentText(value: string) {
  return value.replace(/(?:%[0-9A-Fa-f]{2})+/g, (encoded) => {
    try {
      return decodeURIComponent(encoded);
    } catch {
      return encoded;
    }
  });
}

function readableUrlLabel(value: string) {
  const decoded = decodePercentText(value);
  const anchor = decoded.includes("#") ? decoded.slice(decoded.indexOf("#") + 1).replace(/^#+/, "").trim() : "";
  if (anchor) return anchor;
  try {
    const url = new URL(decoded);
    return decodePercentText(url.pathname.split("/").filter(Boolean).pop() ?? "").replace(/\.[A-Za-z0-9]+$/, "").trim();
  } catch {
    return "";
  }
}

function MaterialContentReader({
  detail,
  reading,
  onCitation
}: {
  detail: MaterialDetail;
  reading: MaterialReadingDocument | null;
  onCitation: (blockId?: string) => void;
}) {
  const content = detail.content ?? detail.contentPreview ?? "";
  const originalHref = api.resourceUrl(api.materialOriginalHref(detail.id));
  const downloadHref = api.resourceUrl(api.materialDownloadHref(detail.id));
  return (
    <div className="material-content-reader">
      <div className="detail-heading">
        <h4>资料阅读</h4>
        <StatusPill label={reading ? "阅读版" : "处理中"} />
      </div>
      {detail.originalAvailable && (
        <div className="button-row reader-actions">
          <a className="ghost-button" href={originalHref} target="_blank" rel="noreferrer">原始文件</a>
          <a className="ghost-button" href={downloadHref}>下载原件</a>
        </div>
      )}
      {reading?.blocks.length ? (
        <div className="material-reading-document">
          {reading.blocks.map((block) => (
            <article id={`document-block-${block.id}`} data-page-number={block.pageNumber} key={block.id} className="reading-block">
              {block.pageNumber && <span className="reading-page">第 {block.pageNumber} 页</span>}
              <p>{readableMaterialText(block.content)}</p>
              <button type="button" className="citation-anchor" onClick={() => onCitation(block.id)}>定位到此段</button>
            </article>
          ))}
        </div>
      ) : content.trim() ? (
        <pre>{readableMaterialText(content)}</pre>
      ) : (
        <EmptyLine label={detail.originalAvailable ? "阅读版仍在生成，请稍后刷新任务状态。" : "这是旧资料，原始文件不可用。"} />
      )}
    </div>
  );
}

function KnowledgePointChip({
  point,
  active = false,
  onOpen
}: {
  point: KnowledgePoint;
  active?: boolean;
  onOpen: (id: string) => void;
}) {
  return (
    <button
      type="button"
      className={`knowledge-point-chip${active ? " active" : ""}`}
      onClick={() => onOpen(point.id)}
    >
      {readableMaterialText(point.legacy ? point.name : point.title)}
    </button>
  );
}

function KnowledgePointDetail({
  point,
  updateKnowledgePoint,
  reviewKnowledgePoint,
  generateQuestions,
  generationTaskId,
  taskDrafts,
  taskStatuses,
  taskErrors,
  taskLoading,
  viewTaskStatus,
  openCitation,
  errors
}: {
  point: KnowledgePoint;
  updateKnowledgePoint: (id: string, payload: Parameters<typeof api.updateKnowledgePoint>[1]) => Promise<void>;
  reviewKnowledgePoint: (point: KnowledgePoint, action: "confirm" | "reject") => Promise<void>;
  generateQuestions: (point: KnowledgePoint, settings: { count: number; difficulty: QuestionDifficulty; questionType: QuestionType }) => Promise<void>;
  generationTaskId?: string;
  taskDrafts: GeneratedQuestionDraft[];
  taskStatuses: TaskStatusMap;
  taskErrors: TaskErrorMap;
  taskLoading: Record<string, boolean>;
  viewTaskStatus: (id: string) => Promise<void>;
  openCitation: (citation: SourceCitation) => Promise<void>;
  errors: FieldErrors;
}) {
  const [editing, setEditing] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [edit, setEdit] = useState(() => knowledgePointEditState(point));
  const [settings, setSettings] = useState({ count: 1, difficulty: "MEDIUM" as QuestionDifficulty, questionType: "SHORT_ANSWER" as QuestionType });

  useEffect(() => {
    setEditing(false);
    setEdit(knowledgePointEditState(point));
  }, [point]);

  if (point.legacy) {
    return <LegacyKnowledgePointDetail point={point} />;
  }

  const eligible = isKnowledgePointQuestionEligible(point);
  async function save() {
    await updateKnowledgePoint(point.id, {
      title: edit.title.trim(), shortSummary: edit.shortSummary.trim(), definition: edit.definition.trim(),
      principles: lines(edit.principles), applicationScenarios: lines(edit.applicationScenarios), pitfalls: lines(edit.pitfalls)
    });
    setEditing(false);
  }

  return (
    <div className="knowledge-point-detail">
      <div className="detail-heading"><h4>{readableMaterialText(point.title)}</h4><StatusPill label={point.sourceOutdated ? "SOURCE_OUTDATED" : point.status} /></div>
      {editing ? (
        <div className="knowledge-point-editor">
          <label>标题<input value={edit.title} onChange={(event) => setEdit({ ...edit, title: event.target.value })} /></label>
          <label>短总结<textarea rows={2} value={edit.shortSummary} onChange={(event) => setEdit({ ...edit, shortSummary: event.target.value })} /></label>
          <label>定义<textarea rows={3} value={edit.definition} onChange={(event) => setEdit({ ...edit, definition: event.target.value })} /></label>
          <label>核心原理（每行一项）<textarea rows={3} value={edit.principles} onChange={(event) => setEdit({ ...edit, principles: event.target.value })} /></label>
          <label>应用场景（每行一项）<textarea rows={3} value={edit.applicationScenarios} onChange={(event) => setEdit({ ...edit, applicationScenarios: event.target.value })} /></label>
          <label>易错点（每行一项）<textarea rows={3} value={edit.pitfalls} onChange={(event) => setEdit({ ...edit, pitfalls: event.target.value })} /></label>
          <div className="button-row"><button type="button" className="primary-button" onClick={() => void save()}><Save size={16} /> 保存</button><button type="button" className="ghost-button" onClick={() => setEditing(false)}>取消</button></div>
        </div>
      ) : <StructuredKnowledgePointContent point={point} />}
      <div className="citation-list">{point.citations.map((citation, index) => <button key={`${citation.revisionId}-${citation.blockId ?? index}`} type="button" className="citation-anchor" onClick={() => void openCitation(citation)}>{citation.pageNumber ? `第 ${citation.pageNumber} 页` : "资料段落"}：{readableMaterialText(citation.excerpt)}</button>)}</div>
      <div className="button-row knowledge-point-actions">
        {point.status === "DRAFT" && <button type="button" className="ghost-button" onClick={() => setEditing((current) => !current)}><Pencil size={16} /> 编辑</button>}
        {point.status === "DRAFT" && <button type="button" className="primary-button" onClick={() => void reviewKnowledgePoint(point, "confirm")}><Check size={16} /> 确认知识点</button>}
        {point.status === "DRAFT" && <button type="button" className="danger-button" onClick={() => void reviewKnowledgePoint(point, "reject")}><X size={16} /> 拒绝</button>}
      </div>
      <div className="knowledge-point-generation">
        <button type="button" className="primary-button" disabled={!eligible} onClick={() => void generateQuestions(point, { count: 1, difficulty: "MEDIUM", questionType: "SHORT_ANSWER" })}><Sparkles size={16} /> 生成面试题</button>
        <button type="button" className="ghost-button" disabled={!eligible} onClick={() => setShowSettings((current) => !current)}>更多设置</button>
        {!eligible && <FormError message="仅已确认、非旧版、来源未过期且引用有效的知识点可出题。" />}
        {showSettings && eligible && <div className="advanced-question-settings"><label>数量<input type="number" min="1" max="10" value={settings.count} onChange={(event) => setSettings({ ...settings, count: Number(event.target.value) })} /></label><label>难度<select value={settings.difficulty} onChange={(event) => setSettings({ ...settings, difficulty: event.target.value as QuestionDifficulty })}><option>EASY</option><option>MEDIUM</option><option>HARD</option></select></label><label>题型<select value={settings.questionType} onChange={(event) => setSettings({ ...settings, questionType: event.target.value as QuestionType })}>{questionTypes.map((type) => <option key={type}>{type}</option>)}</select></label><button type="button" className="ghost-button" onClick={() => void generateQuestions(point, settings)}>按设置生成</button><FormError message={errors.knowledgePointQuestionCount} /></div>}
      </div>
      {generationTaskId && <div className="task-scoped-drafts"><TaskStatusButton taskId={generationTaskId} label="刷新本次生成任务" loading={taskLoading[generationTaskId]} onView={viewTaskStatus} /><TaskStatusCard task={taskStatuses[generationTaskId]} error={taskErrors[generationTaskId]} />{taskDrafts.map((draft) => <div className="row-item" key={draft.id}><span>{draft.stem}</span><StatusPill label={draft.status} /></div>)}</div>}
    </div>
  );
}

function LegacyKnowledgePointDetail({ point }: { point: Extract<KnowledgePoint, { legacy: true }> }) {
  return (
    <div className="knowledge-point-detail">
      <div className="detail-heading">
        <h4>{readableMaterialText(point.name)}</h4>
        <StatusPill label="知识点" />
      </div>
      <p>{point.description ? readableMaterialText(point.description) : "暂无描述"}</p>
      <KnowledgePointSources point={point} />
    </div>
  );
}

function KnowledgePointSummaryList({
  points,
  activeId,
  onOpen,
  emptyLabel
}: {
  points: KnowledgePoint[];
  activeId?: string;
  onOpen: (id: string) => void;
  emptyLabel: string;
}) {
  if (points.length === 0) {
    return <EmptyLine label={emptyLabel} />;
  }
  return (
    <div className="knowledge-point-list">
      {points.map((point) => (
        <article className={`knowledge-point-card${point.id === activeId ? " active" : ""}`} key={point.id}>
          <div className="detail-heading">
            <button type="button" className="knowledge-point-title-button" onClick={() => onOpen(point.id)}>
              {point.legacy ? readableMaterialText(point.name) : readableMaterialText(point.title)}
              <ChevronRight size={15} />
            </button>
            <StatusPill label={point.legacy ? "LEGACY" : point.sourceOutdated ? "SOURCE_OUTDATED" : point.status} />
          </div>
          <p>{point.legacy ? readableMaterialText(point.description) : readableMaterialText(point.shortSummary)}</p>
          <KnowledgePointSources point={point} />
        </article>
      ))}
    </div>
  );
}

function KnowledgePointSources({ point }: { point: KnowledgePoint }) {
  const sourceRefs = (point.sourceRefs ?? []).slice(0, 3);
  return (
    <>
      <div className="result-meta">
        {point.sourceMaterialId && <span>资料 {shortId(point.sourceMaterialId)}</span>}
        {!point.legacy && <span>{point.citations.length} 条可追溯引用</span>}
        {sourceRefs.map((ref) => (
          <span key={`${ref.type}-${ref.id}`}>{sourceRefLabel(ref)}</span>
        ))}
      </div>
    </>
  );
}

function ProviderStatusCard({
  status,
  error,
  loading
}: {
  status: AiProviderStatus | null;
  error: string;
  loading: boolean;
}) {
  if (loading && !status) {
    return <LoadingRows count={1} />;
  }
  if (error) {
    return (
      <div className="task-card error">
        <StatusPill label="UNAVAILABLE" />
        <p>{error}</p>
      </div>
    );
  }
  if (!status) {
    return <EmptyLine label="暂未读取到 AI Provider 状态" />;
  }
  return (
    <div className="provider-status">
      <div className="provider-summary">
        <StatusPill label={status.available ? "AVAILABLE" : "UNAVAILABLE"} />
        <StatusPill label={status.configured ? "CONFIGURED" : "NOT_CONFIGURED"} />
        <span>{status.message ?? "后端未返回说明"}</span>
      </div>
      <div className="metadata-grid">
        <MetaItem label="Provider" value={status.providerType} />
        <MetaItem label="Chat Model" value={status.chatModel ?? "未配置"} />
        <MetaItem label="Embedding Model" value={status.embeddingModel ?? "未配置"} />
        <MetaItem label="Embedding Dimensions" value={status.embeddingDimensions ?? "未返回"} />
        {status.apiKeyEnvName && <MetaItem label="Key Env" value={status.apiKeyEnvName} />}
      </div>
    </div>
  );
}

function MetaItem({ label, value }: { label: string; value?: string | number | boolean | null }) {
  return (
    <div className="meta-item">
      <span>{label}</span>
      <strong>{value === undefined || value === null || value === "" ? "未返回" : String(value)}</strong>
    </div>
  );
}

function TaskStatusButton({
  taskId,
  label,
  loading,
  onView
}: {
  taskId: string;
  label: string;
  loading?: boolean;
  onView: (id: string) => Promise<void>;
}) {
  return (
    <button type="button" className="ghost-button" onClick={() => void onView(taskId)} disabled={loading}>
      {loading ? <Loader2 className="spin" size={16} /> : <RefreshCw size={16} />} {label}
    </button>
  );
}

function TaskStatusCard({ task, error }: { task?: TaskStatus; error?: string }) {
  if (!task && !error) return null;
  if (error) {
    return (
      <div className="task-card error">
        <StatusPill label="TASK_ERROR" />
        <p>{error}</p>
      </div>
    );
  }
  if (!task) return null;
  return (
    <div className="task-card">
      <div className="task-card-heading">
        <StatusPill label={task.status} />
        <span>{task.kind}</span>
      </div>
      <div className="metadata-grid">
        <MetaItem label="任务 ID" value={task.id} />
        <MetaItem label="进度" value={task.progress?.percent !== undefined ? `${task.progress.percent}%` : "未返回"} />
        <MetaItem label="步骤" value={task.progress?.message ?? task.currentStep ?? "未返回"} />
        <MetaItem label="模型" value={task.model ?? "未返回"} />
        {task.providerType && <MetaItem label="Provider" value={task.providerType} />}
        {task.resultRef?.type && <MetaItem label="结果" value={`${task.resultRef.type}${task.resultRef.count !== undefined ? ` x${task.resultRef.count}` : ""}`} />}
        {task.errorCode && <MetaItem label="错误码" value={task.errorCode} />}
        {task.errorMessage && <MetaItem label="错误" value={task.errorMessage} />}
      </div>
    </div>
  );
}

function shortId(value?: string) {
  if (!value) return "未返回";
  return value.length > 12 ? `${value.slice(0, 8)}...${value.slice(-4)}` : value;
}

function formatScore(score?: number) {
  if (score === undefined || Number.isNaN(score)) return "未返回";
  return score.toFixed(2);
}

function SearchPanel(props: {
  form: { query: string; question: string };
  setForm: (value: { query: string; question: string }) => void;
  submitSearch: (event: FormEvent) => Promise<void>;
  askQuestion: (event: FormEvent) => Promise<void>;
  searchResults: SearchResult[];
  ragAnswer: RagAnswer | null;
  knowledgeBases: KnowledgeBase[];
  knowledgePoints: KnowledgePoint[];
  loading: boolean;
  errors: FieldErrors;
  hasSearched: boolean;
  hasAsked: boolean;
}) {
  const knowledgePointNames = new Map(props.knowledgePoints.map((item) => [item.id, item.name]));
  return (
    <section className="two-column">
      <form className="panel form-panel" onSubmit={props.submitSearch}>
        <div className="panel-heading">
          <h3>语义搜索</h3>
          <Search size={18} />
        </div>
        <label>
          查询
          <input
            value={props.form.query}
            aria-invalid={Boolean(props.errors.searchQuery)}
            onChange={(event) => props.setForm({ ...props.form, query: event.target.value })}
          />
          <FormError message={props.errors.searchQuery ?? props.errors.search} />
        </label>
        <button className="primary-button" disabled={props.loading || !props.form.query.trim()}>
          {props.loading ? <Loader2 className="spin" size={16} /> : <Search size={16} />} 搜索
        </button>
        <div className="result-list">
          {props.loading && props.searchResults.length === 0 && <LoadingRows count={3} />}
          {props.searchResults.map((item) => (
            <div className="result-item" key={`${item.type}-${item.id}`}>
              <StatusPill label={item.type} />
              <h4>{item.title}</h4>
              <p>{item.summary}</p>
              <div className="result-meta">
                <span>score {formatScore(item.score)}</span>
                <span>{props.knowledgeBases.find((base) => base.id === item.knowledgeBaseId)?.name ?? item.knowledgeBaseId ?? "当前知识库"}</span>
                {(item.knowledgePointIds ?? []).map((id) => (
                  <span key={id}>{knowledgePointNames.get(id) ?? id}</span>
                ))}
                {(item.sourceRefs ?? []).slice(0, 2).map((ref) => (
                  <span key={`${ref.type}-${ref.id}`}>{sourceRefLabel(ref)}</span>
                ))}
              </div>
            </div>
          ))}
          {!props.loading && props.searchResults.length === 0 && (
            <EmptyState
              compact
              icon={<Search size={20} />}
              title={props.hasSearched ? "没有匹配结果" : "还没有搜索"}
              description={props.hasSearched ? "换一个更具体的知识点、资料关键词或自然语言问题再试。" : "输入关键词或自然语言问题，在当前知识库范围内查找资料片段、题目和知识点。"}
            />
          )}
        </div>
      </form>

      <form className="panel form-panel" onSubmit={props.askQuestion}>
        <div className="panel-heading">
          <h3>资料问答</h3>
          <Send size={18} />
        </div>
        <label>
          问题
          <textarea
            rows={4}
            value={props.form.question}
            aria-invalid={Boolean(props.errors.askQuestion)}
            onChange={(event) => props.setForm({ ...props.form, question: event.target.value })}
          />
          <FormError message={props.errors.askQuestion ?? props.errors.ask} />
        </label>
        <button className="primary-button" disabled={props.loading || !props.form.question.trim()}>
          {props.loading ? <Loader2 className="spin" size={16} /> : <Send size={16} />} 提问
        </button>
        {props.ragAnswer && (
          <div className="answer-box">
            <StatusPill label={props.ragAnswer.uncertain ? "UNCERTAIN" : "ANSWERED"} />
            <p>{props.ragAnswer.answer}</p>
            <div className="item-list compact">
              {(props.ragAnswer.evidenceChunks ?? []).map((item) => <div className="row-item" key={item.id}>{item.content}</div>)}
            </div>
          </div>
        )}
        {!props.loading && !props.ragAnswer && (
          <EmptyState
            compact
            icon={<MessageSquareText size={20} />}
            title={props.hasAsked ? "暂时没有回答" : "向当前知识库提问"}
            description={props.hasAsked ? "如果资料不足，后端会返回不确定提示；也可以先导入更多资料。" : "回答会带不确定标记和证据片段，方便判断是否值得保存为笔记。"}
          />
        )}
      </form>
    </section>
  );
}

function EmptyState({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  compact = false
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  compact?: boolean;
}) {
  return (
    <div className={compact ? "empty-state compact" : "empty-state"}>
      <div className="empty-icon">{icon}</div>
      <div>
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
      {actionLabel && onAction && (
        <button type="button" className="ghost-button" onClick={onAction}>
          <ChevronRight size={16} /> {actionLabel}
        </button>
      )}
    </div>
  );
}

function LoadingRows({ count }: { count: number }) {
  return (
    <>
      {Array.from({ length: count }, (_, index) => (
        <div className="skeleton-row" key={index}>
          <span />
          <span />
        </div>
      ))}
    </>
  );
}

function LoadingMetrics() {
  return (
    <>
      {Array.from({ length: 4 }, (_, index) => (
        <div className="metric skeleton-metric" key={index}>
          <span />
          <strong />
        </div>
      ))}
    </>
  );
}

function FormError({ message }: { message?: string }) {
  if (!message) return null;
  return <span className="field-error" role="alert">{message}</span>;
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function StatusPill({ label }: { label: string }) {
  return <span className="status-pill">{label}</span>;
}

function EmptyLine({ label }: { label: string }) {
  return <div className="empty-line">{label}</div>;
}

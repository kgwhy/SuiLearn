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
import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";
import { api } from "./api";
import type {
  AiNoteDraft,
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
  SearchResult,
  SourceRef
} from "./types";

type Section = "overview" | "materials" | "generate" | "search";
type ToastTone = "info" | "success" | "error";
type ContentFilter = GeneratedContentStatus | "ALL";

const sourceTypes: MaterialSourceType[] = ["MARKDOWN", "TXT", "PDF"];
const questionTypes: QuestionType[] = ["SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER"];

export function App() {
  const [section, setSection] = useState<Section>("overview");
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState("");
  const [detail, setDetail] = useState<KnowledgeBaseDetail | null>(null);
  const [statistics, setStatistics] = useState<KnowledgeBaseStatistics | null>(null);
  const [materials, setMaterials] = useState<MaterialMetadata[]>([]);
  const [materialDetail, setMaterialDetail] = useState<MaterialDetail | null>(null);
  const [knowledgePoints, setKnowledgePoints] = useState<KnowledgePoint[]>([]);
  const [questions, setQuestions] = useState<QuestionSummary[]>([]);
  const [drafts, setDrafts] = useState<GeneratedQuestionDraft[]>([]);
  const [selectedDraftId, setSelectedDraftId] = useState("");
  const [contentFilter, setContentFilter] = useState<ContentFilter>("PENDING_REVIEW");
  const [aiNoteDraft, setAiNoteDraft] = useState<AiNoteDraft | null>(null);
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [ragAnswer, setRagAnswer] = useState<RagAnswer | null>(null);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<{ tone: ToastTone; message: string } | null>(null);

  const [kbForm, setKbForm] = useState({ name: "Java 面试", description: "Spring、JVM、集合与数据库资料" });
  const [kbManageForm, setKbManageForm] = useState({ name: "", description: "" });
  const [materialForm, setMaterialForm] = useState({
    title: "HashMap 资料",
    fileName: "",
    sourceType: "MARKDOWN" as MaterialSourceType,
    content: "HashMap 使用数组加链表或红黑树处理哈希冲突。扩容会重新分布桶位，面试时需要关注负载因子、扰动函数和线程安全边界。"
  });
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
  const visibleDrafts = useMemo(
    () => drafts.filter((draft) => contentFilter === "ALL" || draft.status === contentFilter),
    [contentFilter, drafts]
  );
  const selectedDraft = visibleDrafts.find((item) => item.id === selectedDraftId) ?? visibleDrafts[0] ?? null;
  const selectedSource = useMemo(() => {
    if (generationForm.sourceKind === "knowledgePoint") {
      return knowledgePoints.find((item) => item.id === generationForm.sourceId) ?? knowledgePoints[0] ?? null;
    }
    return materials.find((item) => item.id === generationForm.sourceId) ?? materials[0] ?? null;
  }, [generationForm.sourceId, generationForm.sourceKind, knowledgePoints, materials]);

  useEffect(() => {
    void loadKnowledgeBases();
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
    const firstMaterialId = materials.find((item) => item.status !== "DELETED")?.id;
    if (firstMaterialId && !generationForm.sourceId) {
      setGenerationForm((current) => ({ ...current, sourceId: firstMaterialId }));
    }
  }, [generationForm.sourceId, materials]);

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
      setKnowledgePoints([]);
      setQuestions([]);
      setDrafts([]);
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
    setMaterials(nextMaterials);
    setKnowledgePoints(nextKnowledgePoints);
    setQuestions(nextQuestions);
    setDrafts(nextDrafts.filter((draft) => draft.knowledgeBaseId === knowledgeBaseId));
  }

  async function createKnowledgeBase(event: FormEvent) {
    event.preventDefault();
    const created = await run(() => api.createKnowledgeBase(kbForm), "知识库已创建");
    if (!created) return;
    setSelectedKnowledgeBaseId(created.id);
    await loadKnowledgeBases(created.id);
  }

  async function updateKnowledgeBase(event: FormEvent) {
    event.preventDefault();
    if (!selectedKnowledgeBaseId) return;
    const updated = await run(
      () => api.updateKnowledgeBase(selectedKnowledgeBaseId, kbManageForm),
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

  async function readMaterialFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    const extension = file.name.split(".").pop()?.toUpperCase();
    const sourceType = extension === "PDF" ? "PDF" : extension === "TXT" ? "TXT" : "MARKDOWN";
    const content = await file.text();
    setMaterialForm((current) => ({
      ...current,
      title: current.title.trim() ? current.title : file.name.replace(/\.[^.]+$/, ""),
      fileName: file.name,
      sourceType,
      content
    }));
  }

  async function importMaterial(event: FormEvent) {
    event.preventDefault();
    if (!selectedKnowledgeBaseId) return;
    const material = await run(
      () => api.importMaterial(selectedKnowledgeBaseId, materialForm),
      "资料已导入"
    );
    if (!material) return;
    setGenerationForm((current) => ({ ...current, sourceKind: "material", sourceId: material.id }));
    await loadWorkbench();
  }

  async function openMaterial(materialId: string) {
    const nextDetail = await run(() => api.getMaterial(materialId));
    if (!nextDetail) return;
    setMaterialDetail(nextDetail);
    setGenerationForm((current) => ({ ...current, sourceKind: "material", sourceId: materialId }));
  }

  async function extractKnowledgePoints(materialId: string) {
    await run(() => api.extractKnowledgePoints(materialId), "知识点已提取");
    await loadWorkbench();
  }

  async function deleteMaterial(materialId: string) {
    const confirmed = window.confirm("删除资料后，它不会再参与后续搜索、问答和生成。继续？");
    if (!confirmed) return;
    await run(() => api.deleteMaterial(materialId), "资料已删除");
    if (materialDetail?.id === materialId) setMaterialDetail(null);
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
      setToast({ tone: "error", message: "请选择生成来源" });
      return;
    }
    const pointIds =
      generationForm.sourceKind === "knowledgePoint" ? [sourceRef.id] : knowledgePoints.slice(0, 2).map((item) => item.id);
    const draft = await run(
      () =>
        api.generateQuestion({
          knowledgeBaseId: selectedKnowledgeBaseId,
          sourceRefs: [sourceRef],
          questionType: generationForm.questionType,
          categoryId: generationForm.categoryId,
          categoryName: generationForm.categoryName,
          knowledgePointIds: pointIds,
          prompt: generationForm.prompt
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
    await run(
      () =>
        api.reviewGeneratedContent(selectedDraft.id, {
          status,
          stem: draft.stem,
          options: draft.options,
          answer: draft.answer,
          explanation: draft.explanation,
          categoryId: draft.categoryId,
          categoryName: draft.categoryName,
          knowledgePointIds: draft.knowledgePointIds,
          sourceRefs: selectedDraft.sourceRefs
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
      setToast({ tone: "error", message: "需要先提取知识点" });
      return;
    }
    const note = await run(() =>
      api.generateExplanation({
        knowledgeBaseId: selectedKnowledgeBaseId,
        knowledgePointId: point.id,
        sourceRefs: point.sourceRefs,
        prompt: "生成简短解释"
      })
    );
    if (note) setAiNoteDraft(note);
  }

  async function generateReviewSuggestion() {
    const sourceRef = buildSourceRef();
    if (!selectedKnowledgeBaseId || !sourceRef) return;
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
    if (!selectedKnowledgeBaseId) return;
    const results = await run(() => api.search({ q: searchForm.query, knowledgeBaseId: selectedKnowledgeBaseId }));
    if (results) setSearchResults(results);
  }

  async function askQuestion(event: FormEvent) {
    event.preventDefault();
    if (!selectedKnowledgeBaseId) return;
    const answer = await run(() => api.ask({ question: searchForm.question, knowledgeBaseId: selectedKnowledgeBaseId }));
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
            <input value={kbForm.name} onChange={(event) => setKbForm({ ...kbForm, name: event.target.value })} />
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
        />
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">第二版工作台</p>
            <h2>{selectedKnowledgeBase?.name ?? "暂无知识库"}</h2>
          </div>
          <button className="ghost-button" onClick={() => void loadWorkbench()} disabled={!selectedKnowledgeBaseId || loading}>
            {loading ? <Loader2 className="spin" size={17} /> : <RefreshCw size={17} />} 刷新
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

        {section === "overview" && (
          <OverviewPanel
            detail={detail}
            statistics={statistics}
            materials={materials}
            knowledgePoints={knowledgePoints}
            questions={questions}
            drafts={drafts}
          />
        )}

        {section === "materials" && (
          <MaterialsPanel
            materialForm={materialForm}
            setMaterialForm={setMaterialForm}
            readMaterialFile={readMaterialFile}
            importMaterial={importMaterial}
            materials={materials}
            materialDetail={materialDetail}
            openMaterial={openMaterial}
            extractKnowledgePoints={extractKnowledgePoints}
            deleteMaterial={deleteMaterial}
            loading={loading}
          />
        )}

        {section === "generate" && (
          <GeneratePanel
            form={generationForm}
            setForm={setGenerationForm}
            materials={materials}
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
            loading={loading}
          />
        )}

        {section === "search" && (
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
        />
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
  questions,
  drafts
}: {
  detail: KnowledgeBaseDetail | null;
  statistics: KnowledgeBaseStatistics | null;
  materials: MaterialMetadata[];
  knowledgePoints: KnowledgePoint[];
  questions: QuestionSummary[];
  drafts: GeneratedQuestionDraft[];
}) {
  return (
    <section className="panel-grid">
      <div className="metrics">
        <Metric label="资料" value={detail?.materialCount ?? 0} />
        <Metric label="知识点" value={detail?.knowledgePointCount ?? 0} />
        <Metric label="已保存题" value={statistics?.questionCount ?? detail?.questionCount ?? 0} />
        <Metric label="待确认" value={drafts.length} />
      </div>
      <div className="panel wide">
        <div className="panel-heading">
          <h3>最近资料</h3>
          <FileText size={18} />
        </div>
        <div className="item-list">
          {materials.slice(0, 5).map((item) => (
            <div className="row-item" key={item.id}>
              <span>{item.title}</span>
              <StatusPill label={item.status} />
            </div>
          ))}
          {materials.length === 0 && <EmptyLine label="暂无资料" />}
        </div>
      </div>
      <div className="panel">
        <div className="panel-heading">
          <h3>知识点</h3>
          <Database size={18} />
        </div>
        <div className="tag-cloud">
          {knowledgePoints.slice(0, 12).map((item) => <span key={item.id}>{item.name}</span>)}
          {knowledgePoints.length === 0 && <EmptyLine label="暂无知识点" />}
        </div>
      </div>
      <div className="panel">
        <div className="panel-heading">
          <h3>已保存题</h3>
          <BookOpen size={18} />
        </div>
        <div className="item-list compact">
          {questions.slice(0, 4).map((item) => <div className="row-item" key={item.id}>{item.stem}</div>)}
          {questions.length === 0 && <EmptyLine label="暂无题目" />}
        </div>
      </div>
    </section>
  );
}

function MaterialsPanel(props: {
  materialForm: { title: string; fileName: string; sourceType: MaterialSourceType; content: string };
  setMaterialForm: (value: { title: string; fileName: string; sourceType: MaterialSourceType; content: string }) => void;
  readMaterialFile: (event: ChangeEvent<HTMLInputElement>) => Promise<void>;
  importMaterial: (event: FormEvent) => Promise<void>;
  materials: MaterialMetadata[];
  materialDetail: MaterialDetail | null;
  openMaterial: (id: string) => Promise<void>;
  extractKnowledgePoints: (id: string) => Promise<void>;
  deleteMaterial: (id: string) => Promise<void>;
  loading: boolean;
}) {
  const { materialForm, setMaterialForm } = props;
  return (
    <section className="two-column">
      <form className="panel form-panel" onSubmit={props.importMaterial}>
        <div className="panel-heading">
          <h3>导入资料</h3>
          <FileText size={18} />
        </div>
        <label>
          本地文件
          <input type="file" accept=".md,.markdown,.txt,.pdf,text/markdown,text/plain,application/pdf" onChange={props.readMaterialFile} />
        </label>
        {materialForm.fileName && <div className="file-hint"><Upload size={15} /> {materialForm.fileName}</div>}
        <label>
          标题
          <input value={materialForm.title} onChange={(event) => setMaterialForm({ ...materialForm, title: event.target.value })} />
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
        <label>
          内容
          <textarea
            value={materialForm.content}
            onChange={(event) => setMaterialForm({ ...materialForm, content: event.target.value })}
            rows={8}
          />
        </label>
        <button className="primary-button" disabled={props.loading || !materialForm.title.trim() || !materialForm.content.trim()}>
          <Save size={16} /> 导入
        </button>
      </form>

      <div className="panel">
        <div className="panel-heading">
          <h3>资料列表</h3>
          <Archive size={18} />
        </div>
        <div className="item-list">
          {props.materials.map((item) => (
            <div className="row-item action-row" key={item.id}>
              <button type="button" onClick={() => void props.openMaterial(item.id)}>
                <FileText size={16} />
                <span>{item.title}</span>
              </button>
              <div className="row-actions">
                <StatusPill label={item.sourceType} />
                <button title="提取知识点" aria-label="提取知识点" onClick={() => void props.extractKnowledgePoints(item.id)}>
                  <Sparkles size={16} />
                </button>
                <button title="删除资料" aria-label="删除资料" onClick={() => void props.deleteMaterial(item.id)}>
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))}
          {props.materials.length === 0 && <EmptyLine label="暂无资料" />}
        </div>
        {props.materialDetail && (
          <div className="detail-box">
            <h4>{props.materialDetail.title}</h4>
            <p>{props.materialDetail.contentPreview ?? props.materialDetail.content}</p>
            <div className="tag-cloud">
              {props.materialDetail.extractedKnowledgePoints.map((item) => <span key={item.id}>{item.name}</span>)}
            </div>
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
  loading: boolean;
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
      optionsText: props.selectedDraft.options.join("\n"),
      options: props.selectedDraft.options,
      answerText: props.selectedDraft.answer.join("\n"),
      answer: props.selectedDraft.answer,
      explanation: props.selectedDraft.explanation,
      categoryId: props.selectedDraft.categoryId,
      categoryName: props.selectedDraft.categoryName,
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
          <select value={form.sourceId} onChange={(event) => props.setForm({ ...form, sourceId: event.target.value })}>
            {sources.map((item) => <option key={item.id} value={item.id}>{"name" in item ? item.name : item.title}</option>)}
          </select>
        </label>
        <label>
          题型
          <select value={form.questionType} onChange={(event) => props.setForm({ ...form, questionType: event.target.value as QuestionType })}>
            {props.questionTypes.map((item) => <option key={item}>{item}</option>)}
          </select>
        </label>
        <div className="field-row">
          <label>
            分类 ID
            <input value={form.categoryId} onChange={(event) => props.setForm({ ...form, categoryId: event.target.value })} />
          </label>
          <label>
            分类名
            <input value={form.categoryName} onChange={(event) => props.setForm({ ...form, categoryName: event.target.value })} />
          </label>
        </div>
        <label>
          Prompt
          <textarea rows={4} value={form.prompt} onChange={(event) => props.setForm({ ...form, prompt: event.target.value })} />
        </label>
        <button className="primary-button" disabled={props.loading || !form.sourceId}>
          <Sparkles size={16} /> 生成草稿
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
                <span>{draft.stem}</span>
                <StatusPill label={draft.status} />
              </button>
            ))}
            {props.drafts.length === 0 && <EmptyLine label="暂无待确认草稿" />}
          </div>
          {props.selectedDraft && (
            <div className="draft-detail">
              <label>
                题干
                <textarea rows={3} value={draftEdit.stem} onChange={(event) => updateDraftEdit({ stem: event.target.value })} />
              </label>
              <label>
                选项（每行一项）
                <textarea rows={4} value={draftEdit.optionsText} onChange={(event) => updateDraftEdit({ optionsText: event.target.value })} />
              </label>
              <label>
                答案（每行一项）
                <textarea rows={2} value={draftEdit.answerText} onChange={(event) => updateDraftEdit({ answerText: event.target.value })} />
              </label>
              <label>
                解析
                <textarea rows={4} value={draftEdit.explanation} onChange={(event) => updateDraftEdit({ explanation: event.target.value })} />
              </label>
              <div className="field-row">
                <label>
                  分类 ID
                  <input value={draftEdit.categoryId} onChange={(event) => updateDraftEdit({ categoryId: event.target.value })} />
                </label>
                <label>
                  分类名
                  <input value={draftEdit.categoryName} onChange={(event) => updateDraftEdit({ categoryName: event.target.value })} />
                </label>
              </div>
              <label>
                知识点 ID（每行一项）
                <textarea rows={2} value={draftEdit.knowledgePointIdsText} onChange={(event) => updateDraftEdit({ knowledgePointIdsText: event.target.value })} />
              </label>
              <div className="source-list">
                {props.selectedDraft.sourceRefs.map((ref) => (
                  <span key={`${ref.type}-${ref.id}`}>{ref.deleted ? "已删除来源" : ref.title}</span>
                ))}
              </div>
              <div className="button-row">
                <button className="primary-button" onClick={() => void props.reviewDraft("SAVED", draftEdit)}>
                  <Save size={16} /> 保存
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
  knowledgePointIds: []
};

function lines(value: string) {
  return value.split(/\r?\n/).map((item) => item.trim()).filter(Boolean);
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
          <input value={props.form.query} onChange={(event) => props.setForm({ ...props.form, query: event.target.value })} />
        </label>
        <button className="primary-button" disabled={props.loading || !props.form.query.trim()}>
          <Search size={16} /> 搜索
        </button>
        <div className="result-list">
          {props.searchResults.map((item) => (
            <div className="result-item" key={`${item.type}-${item.id}`}>
              <StatusPill label={item.type} />
              <h4>{item.title}</h4>
              <p>{item.summary}</p>
              <div className="result-meta">
                <span>{props.knowledgeBases.find((base) => base.id === item.knowledgeBaseId)?.name ?? item.knowledgeBaseId ?? "当前知识库"}</span>
                {item.knowledgePointIds.map((id) => (
                  <span key={id}>{knowledgePointNames.get(id) ?? id}</span>
                ))}
                {item.sourceRefs.slice(0, 2).map((ref) => (
                  <span key={`${ref.type}-${ref.id}`}>{ref.deleted ? "来源已删除" : ref.title}</span>
                ))}
              </div>
            </div>
          ))}
          {props.searchResults.length === 0 && <EmptyLine label="暂无搜索结果" />}
        </div>
      </form>

      <form className="panel form-panel" onSubmit={props.askQuestion}>
        <div className="panel-heading">
          <h3>资料问答</h3>
          <Send size={18} />
        </div>
        <label>
          问题
          <textarea rows={4} value={props.form.question} onChange={(event) => props.setForm({ ...props.form, question: event.target.value })} />
        </label>
        <button className="primary-button" disabled={props.loading || !props.form.question.trim()}>
          <Send size={16} /> 提问
        </button>
        {props.ragAnswer && (
          <div className="answer-box">
            <StatusPill label={props.ragAnswer.uncertain ? "UNCERTAIN" : "ANSWERED"} />
            <p>{props.ragAnswer.answer}</p>
            <div className="item-list compact">
              {props.ragAnswer.evidenceChunks.map((item) => <div className="row-item" key={item.id}>{item.content}</div>)}
            </div>
          </div>
        )}
      </form>
    </section>
  );
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

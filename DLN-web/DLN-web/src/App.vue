<script setup lang="ts">
import { computed, onMounted, provide, reactive, ref, watch } from 'vue'
import {
  clearAuthCookie,
  collectSubtreeIds,
  DEFAULT_CODE_THEME,
  DEFAULT_CONTENT_THEME,
  DEFAULT_CONTENT_THEME_OPTIONS,
  extractTagIds,
  findTreeNodeById,
  formatDateTime,
  LEGACY_TOKEN_KEY,
  loadStoredUser,
  normalizeNote,
  normalizeOptionalText,
  normalizeTagIds,
  resolveAssetUrl,
  setAuthCookie,
  TOKEN_KEY,
} from './app/shared'
import type {
  AuthMode,
  CreateItemType,
  DeleteKind,
  Id,
  KnowledgeBase,
  KnowledgeBaseModalMode,
  KnowledgeGraph,
  NoteDetail,
  NoteHistoryDetail,
  NoteHistoryVersion,
  NoteLinkCandidate,
  NoteLinkOpenHandler,
  NoteLinkPreview,
  NoteSearchResult,
  NoteTemplate,
  NoticeType,
  TagItem,
  TemplateModalMode,
  TreeNode,
  UserInfo,
  ViewMode,
} from './app/shared'
import { useKnowledgeBaseData } from './app/useKnowledgeBaseData'
import { useNoteAutosave } from './app/useNoteAutosave'
import { useWorkspaceChrome } from './app/useWorkspaceChrome'
import { useSessionManager } from './app/useSessionManager'
import type { KnowledgeBaseSearchFilters } from './app/shared'
import AppRail from './components/AppRail.vue'
import AuthPanel from './components/auth/AuthPanel.vue'
import KnowledgeGraphD3View from './components/graph/KnowledgeGraphD3View.vue'
import LibraryPanel from './components/layout/LibraryPanel.vue'
import CreateItemModal from './components/modals/CreateItemModal.vue'
import DeleteConfirmModal from './components/modals/DeleteConfirmModal.vue'
import EditFolderModal from './components/modals/EditFolderModal.vue'
import KnowledgeBaseModalView from './components/modals/KnowledgeBaseModal.vue'
import NoteHistoryModal from './components/modals/NoteHistoryModal.vue'
import NoteTemplateModal from './components/modals/NoteTemplateModal.vue'
import UserSettingsView from './components/settings/UserSettingsView.vue'
import NoteTemplatesView from './components/templates/NoteTemplatesView.vue'
import HomeView from './components/workspace/HomeView.vue'

const token = ref(localStorage.getItem(TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY) || '')
const currentUser = ref<UserInfo | null>(loadStoredUser())
const viewMode = ref<ViewMode>('home')
const userContentTheme = ref(DEFAULT_CONTENT_THEME)
const userCodeTheme = ref(DEFAULT_CODE_THEME)
const contentThemeOptions = ref<string[]>([...DEFAULT_CONTENT_THEME_OPTIONS])
const codeThemeOptions = ref<string[]>([DEFAULT_CODE_THEME])

const knowledgeBases = ref<KnowledgeBase[]>([])
const knowledgeBaseTags = ref<TagItem[]>([])
const noteTemplates = ref<NoteTemplate[]>([])
const treeNodes = ref<TreeNode[]>([])
const knowledgeGraph = ref<KnowledgeGraph | null>(null)
const selectedKnowledgeBaseId = ref<Id | null>(null)
const selectedFolderId = ref<Id | null>(null)
const selectedNoteId = ref<Id | null>(null)
const currentNote = ref<NoteDetail | null>(null)
const selectedNoteTagIds = ref<Id[]>([])
const knowledgeBaseTagCreateTick = ref(0)
const noteTagCreateTick = ref(0)
const noteTitle = ref('')
const noteContent = ref('')
const libraryCollapsed = ref(false)
const workspaceFullscreen = ref(false)

const notice = reactive({
  text: '',
  type: 'success' as NoticeType,
})

let noticeTimer: number | null = null

const loading = reactive({
  auth: false,
  knowledgeBases: false,
  tags: false,
  tagSubmit: false,
  templates: false,
  graph: false,
  tree: false,
  note: false,
  save: false,
  profile: false,
  userSettings: false,
  themeOptions: false,
  avatarUpload: false,
  templateSubmit: false,
})

watch(
  token,
  (value) => {
    if (value.trim()) {
      setAuthCookie(value)
      return
    }

    clearAuthCookie()
  },
  { immediate: true },
)

const authForm = reactive({
  mode: 'login' as AuthMode,
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
})

const profileForm = reactive({
  nickname: '',
  email: '',
  avatarUrl: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const knowledgeBaseModal = reactive({
  open: false,
  mode: 'create' as KnowledgeBaseModalMode,
  id: null as Id | null,
  name: '',
  description: '',
})

const templateModal = reactive({
  open: false,
  mode: 'create' as TemplateModalMode,
  id: null as Id | null,
  name: '',
  description: '',
  templateContent: '',
})

const noteHistoryModal = reactive({
  open: false,
  loadingList: false,
  loadingDetail: false,
  restoring: false,
  deleting: false,
  versions: [] as NoteHistoryVersion[],
  selectedVersionId: null as Id | null,
  selectedDetail: null as NoteHistoryDetail | null,
})

const createItemModal = reactive({
  open: false,
  type: 'folder' as CreateItemType,
  parentId: null as Id | null,
  name: '',
  templateId: null as Id | null,
})

const editFolderModal = reactive({
  open: false,
  id: null as Id | null,
  parentId: null as Id | null,
  name: '',
  busy: false,
})

const deleteModal = reactive({
  open: false,
  kind: 'knowledge-base' as DeleteKind,
  targetId: null as Id | null,
  targetName: '',
  busy: false,
})

const hasToken = computed(() => Boolean(token.value))

const selectedKnowledgeBase = computed(() => {
  return knowledgeBases.value.find((item) => item.id === selectedKnowledgeBaseId.value) ?? null
})

const displayName = computed(() => {
  return currentUser.value?.nickname || currentUser.value?.username || '用户'
})

const displayEmail = computed(() => {
  return currentUser.value?.email || '未设置邮箱'
})

const userInitial = computed(() => {
  return displayName.value.slice(0, 1).toUpperCase()
})

const currentAvatarUrl = computed(() => resolveAssetUrl(currentUser.value?.avatarUrl))
const profileAvatarUrl = computed(() => resolveAssetUrl(profileForm.avatarUrl || currentUser.value?.avatarUrl))

const authTitle = computed(() => {
  return authForm.mode === 'login' ? '欢迎回来' : '创建你的账号'
})

const authSubmitText = computed(() => {
  if (loading.auth) {
    return '提交中...'
  }

  return authForm.mode === 'login' ? '登录' : '注册并登录'
})

const deleteTitle = computed(() => {
  if (deleteModal.kind === 'knowledge-base') {
    return '删除知识库'
  }

  if (deleteModal.kind === 'folder') {
    return '删除文件夹'
  }

  if (deleteModal.kind === 'template') {
    return '删除模板'
  }

  if (deleteModal.kind === 'tag') {
    return '删除标签'
  }

  if (deleteModal.kind === 'history') {
    return '删除历史版本'
  }

  return '删除笔记'
})

const deleteCopy = computed(() => {
  if (deleteModal.kind === 'knowledge-base') {
    return `将删除知识库“${deleteModal.targetName || '当前知识库'}”以及其下所有文件夹和笔记，此操作无法撤销。`
  }

  if (deleteModal.kind === 'tag') {
    return `将删除标签“${deleteModal.targetName || '当前标签'}”，关联到笔记的该标签也会一并移除。`
  }

  if (deleteModal.kind === 'history') {
    return `将删除历史版本“${deleteModal.targetName || '当前版本'}”，此操作无法撤销。`
  }

  return `将删除“${deleteModal.targetName || '当前内容'}”，此操作无法撤销。`
})

const flatTreeNodes = computed(() => {
  const list: TreeNode[] = []

  const walk = (nodes: TreeNode[]) => {
    for (const node of nodes) {
      list.push(node)
      if (node.children?.length) {
        walk(node.children)
      }
    }
  }

  walk(treeNodes.value)
  return list
})

const folderCount = computed(() => {
  return flatTreeNodes.value.filter((node) => node.type === 'folder').length
})

const noteNodes = computed(() => {
  return flatTreeNodes.value.filter((node) => node.type === 'note')
})

const graphNodeCount = computed(() => knowledgeGraph.value?.nodes.length ?? 0)
const graphEdgeCount = computed(() => knowledgeGraph.value?.edges.length ?? 0)

const noteCount = computed(() => {
  return noteNodes.value.length
})

const selectedCreateTemplate = computed(() => {
  return noteTemplates.value.find((item) => item.id === createItemModal.templateId) ?? null
})

const currentFolder = computed(() => {
  if (!selectedFolderId.value) {
    return null
  }

  return flatTreeNodes.value.find((node) => node.type === 'folder' && node.id === selectedFolderId.value) ?? null
})

const {
  fetchUserInfo,
  fetchUserSettings,
  fetchUserThemeOptions,
  handleEditorThemeChange,
  handleSettingsThemeSelectionChange,
  persistUser,
  request,
  saveProfile,
  submitAuth,
  syncProfileForm,
} = useSessionManager({
  token,
  currentUser,
  authForm,
  profileForm,
  loading,
  userContentTheme,
  userCodeTheme,
  contentThemeOptions,
  codeThemeOptions,
  viewMode,
  showNotice,
  onUnauthorized: () => logout(false),
  onAfterAuthSuccess: () => initializeApp(),
})

function showNotice(text: string, type: NoticeType = 'success') {
  notice.text = text
  notice.type = type

  if (noticeTimer) {
    window.clearTimeout(noticeTimer)
  }

  noticeTimer = window.setTimeout(() => {
    notice.text = ''
    noticeTimer = null
  }, 3600)
}

const { autoSaveError, autoSavingNote, clearCurrentNote, createHistoryVersion, noteSaved } = useNoteAutosave({
  request,
  showNotice,
  createHistorySnapshot: (noteId: Id) => createNoteHistorySnapshot(noteId),
  refreshKnowledgeBasesSnapshot: () => refreshKnowledgeBasesSnapshot(),
  currentNote,
  selectedNoteId,
  selectedNoteTagIds,
  noteTitle,
  noteContent,
  treeNodes,
  loading,
})

const {
  fetchKnowledgeBases,
  loadKnowledgeBaseContext,
  loadKnowledgeGraph,
  loadKnowledgeBaseTags,
  loadKnowledgeBaseTree,
  loadNoteTemplates,
  refreshKnowledgeBasesSnapshot,
} = useKnowledgeBaseData({
  request,
  loading,
  viewMode,
  knowledgeBases,
  knowledgeBaseTags,
  noteTemplates,
  treeNodes,
  knowledgeGraph,
  selectedKnowledgeBaseId,
  selectedFolderId,
  selectedNoteTagIds,
  currentNote,
  clearCurrentNote,
})

const { showKnowledgeBaseSidebar, statusbarLeftItems, statusbarRightItems } = useWorkspaceChrome({
  viewMode,
  selectedKnowledgeBaseId,
  currentNote,
  currentFolder,
  selectedKnowledgeBase,
  noteTemplates,
  knowledgeBases,
  loading,
  graphNodeCount,
  graphEdgeCount,
  autoSavingNote,
  autoSaveError,
  noteSaved,
  noteCount,
  folderCount,
  displayName,
})

function toggleLibraryPanel(collapsed?: boolean) {
  libraryCollapsed.value = typeof collapsed === 'boolean' ? collapsed : !libraryCollapsed.value
}

function openHomeDashboard() {
  if (currentNote.value && !noteSaved.value) {
    showNotice('请先保存或关闭当前笔记，再返回知识库首页。', 'error')
    return
  }

  viewMode.value = 'home'
  selectedKnowledgeBaseId.value = null
  knowledgeBaseTags.value = []
  knowledgeGraph.value = null
  selectedFolderId.value = null
  treeNodes.value = []
  clearCurrentNote()
  libraryCollapsed.value = false
}

async function openCurrentKnowledgeBaseWorkspace() {
  const knowledgeBaseId = selectedKnowledgeBaseId.value
  if (!knowledgeBaseId) {
    return
  }

  if (currentNote.value && !noteSaved.value) {
    showNotice('请先保存当前笔记，再返回知识库页面。', 'error')
    return
  }

  clearCurrentNote()
  viewMode.value = 'home'
  libraryCollapsed.value = false

  try {
    await loadKnowledgeBaseContext(knowledgeBaseId)
  } catch (error) {
    showNotice((error as Error).message, 'error')
  }
}

function resetWorkspace() {
  knowledgeBases.value = []
  knowledgeBaseTags.value = []
  noteTemplates.value = []
  treeNodes.value = []
  knowledgeGraph.value = null
  userContentTheme.value = DEFAULT_CONTENT_THEME
  userCodeTheme.value = DEFAULT_CODE_THEME
  contentThemeOptions.value = [...DEFAULT_CONTENT_THEME_OPTIONS]
  codeThemeOptions.value = [DEFAULT_CODE_THEME]
  selectedKnowledgeBaseId.value = null
  selectedFolderId.value = null
  clearCurrentNote()
  closeNoteHistoryModal()
}

function logout(showMessage = true) {
  token.value = ''
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(LEGACY_TOKEN_KEY)
  persistUser(null)
  resetWorkspace()
  libraryCollapsed.value = false
  viewMode.value = 'home'
  authForm.mode = 'login'
  authForm.password = ''
  authForm.confirmPassword = ''

  if (showMessage) {
    showNotice('已退出登录。')
  }
}

async function initializeApp() {
  if (!hasToken.value) {
    resetWorkspace()
    syncProfileForm()
    return
  }

  try {
    await Promise.all([
      fetchKnowledgeBases(),
      fetchUserInfo(),
      fetchUserSettings(),
      fetchUserThemeOptions(),
      loadNoteTemplates(),
    ])
  } catch (error) {
    showNotice((error as Error).message, 'error')
  }
}

async function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) {
    return
  }

  try {
    if (!file.type.startsWith('image/')) {
      throw new Error('请选择图片文件作为头像。')
    }
    if (file.size > 5 * 1024 * 1024) {
      throw new Error('头像大小不能超过 5MB。')
    }
    loading.avatarUpload = true
    const formData = new FormData()
    formData.append('file', file)

    const user = await request<UserInfo>('/user/avatar', {
      method: 'POST',
      body: formData,
    })

    persistUser(user)
    showNotice('头像上传成功。')
  } catch (error) {
    showNotice((error as Error).message, 'error')
  } finally {
    loading.avatarUpload = false
    input.value = ''
  }
}

async function selectKnowledgeBase(knowledgeBaseId: Id) {
  try {
    libraryCollapsed.value = false
    await loadKnowledgeBaseContext(knowledgeBaseId)
  } catch (error) {
    showNotice((error as Error).message, 'error')
  }
}

function applyCurrentNoteDetail(detail: NoteDetail) {
  currentNote.value = detail
  selectedNoteId.value = detail.id
  selectedFolderId.value = detail.folderId
  selectedNoteTagIds.value = extractTagIds(detail.tags)
  noteTitle.value = detail.title
  noteContent.value = detail.markdownContent || ''
  viewMode.value = 'home'
}

async function openNote(noteId: Id) {
  loading.note = true

  try {
    const detail = normalizeNote(await request<NoteDetail>(`/notes/${noteId}`))

    if (selectedKnowledgeBaseId.value !== detail.knowledgeBaseId) {
      await loadKnowledgeBaseContext(detail.knowledgeBaseId)
    }

    applyCurrentNoteDetail(detail)
  } finally {
    loading.note = false
  }
}

async function fetchNoteLinkCandidates(keyword: string) {
  if (!currentNote.value?.id) {
    return []
  }

  return await request<NoteLinkCandidate[]>(
    `/notes/${currentNote.value.id}/links/candidates?keyword=${encodeURIComponent(keyword)}`,
  )
}

async function fetchNoteLinkPreview(title: string) {
  if (!currentNote.value?.id) {
    return null
  }

  return await request<NoteLinkPreview>(
    `/notes/${currentNote.value.id}/links/preview?title=${encodeURIComponent(title)}`,
  )
}

async function searchKnowledgeBaseNotes(filters: KnowledgeBaseSearchFilters) {
  const knowledgeBaseId = selectedKnowledgeBaseId.value
  if (!knowledgeBaseId) {
    return []
  }

  const params = new URLSearchParams()
  params.set('scope', filters.scope || 'all')

  const keyword = filters.keyword?.trim()
  if (keyword) {
    params.set('keyword', keyword)
  }

  if (filters.folderId) {
    params.set('folderId', String(filters.folderId))
  }

  for (const tagId of filters.tagIds ?? []) {
    params.append('tagIds', String(tagId))
  }

  const query = params.toString()
  return await request<NoteSearchResult[]>(
    `/knowledgeBases/${knowledgeBaseId}/search${query ? `?${query}` : ''}`,
  )
}

function resetNoteHistoryModal() {
  noteHistoryModal.loadingList = false
  noteHistoryModal.loadingDetail = false
  noteHistoryModal.restoring = false
  noteHistoryModal.deleting = false
  noteHistoryModal.versions = []
  noteHistoryModal.selectedVersionId = null
  noteHistoryModal.selectedDetail = null
}

function closeNoteHistoryModal() {
  noteHistoryModal.open = false
  resetNoteHistoryModal()
}

async function createNoteHistorySnapshot(noteId: Id) {
  await request<NoteHistoryVersion>(`/notes/${noteId}/histories/snapshot`, {
    method: 'POST',
  })
}

async function selectNoteHistoryVersion(versionId: Id) {
  if (!currentNote.value?.id) {
    return
  }

  noteHistoryModal.loadingDetail = true
  noteHistoryModal.selectedVersionId = versionId

  try {
    noteHistoryModal.selectedDetail = await request<NoteHistoryDetail>(
      `/notes/${currentNote.value.id}/histories/${versionId}`,
    )
  } catch (error) {
    noteHistoryModal.selectedDetail = null
    showNotice((error as Error).message, 'error')
  } finally {
    noteHistoryModal.loadingDetail = false
  }
}

async function loadNoteHistoryVersions(noteId: Id, preferredVersionId: Id | null = null) {
  noteHistoryModal.loadingList = true

  try {
    const versions = await request<NoteHistoryVersion[]>(`/notes/${noteId}/histories`)
    noteHistoryModal.versions = versions

    if (!versions.length) {
      noteHistoryModal.selectedVersionId = null
      noteHistoryModal.selectedDetail = null
      return
    }

    const targetVersionId = versions.some((version) => version.id === preferredVersionId)
      ? preferredVersionId
      : versions[0].id

    if (!targetVersionId) {
      noteHistoryModal.selectedVersionId = null
      noteHistoryModal.selectedDetail = null
      return
    }

    await selectNoteHistoryVersion(targetVersionId)
  } finally {
    noteHistoryModal.loadingList = false
  }
}

async function openNoteHistoryModal() {
  if (!currentNote.value?.id) {
    return
  }

  noteHistoryModal.open = true
  resetNoteHistoryModal()
  try {
    await loadNoteHistoryVersions(currentNote.value.id)
  } catch (error) {
    closeNoteHistoryModal()
    showNotice((error as Error).message, 'error')
  }
}

async function restoreNoteHistoryVersion() {
  if (!currentNote.value?.id || !noteHistoryModal.selectedDetail?.id) {
    return
  }

  noteHistoryModal.restoring = true

  try {
    const restoredDetail = normalizeNote(
      await request<NoteDetail>(
        `/notes/${currentNote.value.id}/histories/${noteHistoryModal.selectedDetail.id}/restore`,
        {
          method: 'POST',
        },
      ),
    )

    if (selectedKnowledgeBaseId.value) {
      await loadKnowledgeBaseTree(selectedKnowledgeBaseId.value)
      await refreshKnowledgeBasesSnapshot()
    }

    applyCurrentNoteDetail(restoredDetail)
    await loadNoteHistoryVersions(restoredDetail.id)
    showNotice('历史版本已恢复。')
  } catch (error) {
    showNotice((error as Error).message, 'error')
  } finally {
    noteHistoryModal.restoring = false
  }
}

async function deleteNoteHistoryVersion() {
  if (!currentNote.value?.id || !noteHistoryModal.selectedDetail?.id) {
    return
  }

  openDeleteModal(
    'history',
    noteHistoryModal.selectedDetail.id,
    `v${noteHistoryModal.selectedDetail.versionNo} · ${noteHistoryModal.selectedDetail.title}`,
  )
}

provide('noteLinkCandidatesFetcher', fetchNoteLinkCandidates)
provide('noteLinkPreviewFetcher', fetchNoteLinkPreview)
provide('noteLinkOpenHandler', (noteId: Id) => openNote(noteId) as ReturnType<NoteLinkOpenHandler>)
provide('currentNoteIdGetter', () => currentNote.value?.id ?? null)

function handleTreeNodeSelect(node: TreeNode) {
  viewMode.value = 'home'

  if (node.type === 'folder') {
    if (currentNote.value) {
      return
    }

    selectedFolderId.value = node.id
    clearCurrentNote()
    return
  }

  void openNote(node.id).catch((error: Error) => {
    showNotice(error.message, 'error')
  })
}

function openGraphD3View() {
  viewMode.value = 'graph-d3'
}

function openKnowledgeBaseModal(mode: KnowledgeBaseModalMode, item?: KnowledgeBase) {
  knowledgeBaseModal.open = true
  knowledgeBaseModal.mode = mode
  knowledgeBaseModal.id = item?.id ?? null
  knowledgeBaseModal.name = item?.name ?? ''
  knowledgeBaseModal.description = item?.description ?? ''
}

function closeKnowledgeBaseModal() {
  knowledgeBaseModal.open = false
}

function openTemplateModal(mode: TemplateModalMode, template?: NoteTemplate) {
  templateModal.open = true
  templateModal.mode = mode
  templateModal.id = template?.id ?? null
  templateModal.name = template?.name ?? ''
  templateModal.description = template?.description ?? ''
  templateModal.templateContent = template?.templateContent ?? ''
}

function closeTemplateModal() {
  templateModal.open = false
  templateModal.id = null
  templateModal.name = ''
  templateModal.description = ''
  templateModal.templateContent = ''
}

async function submitKnowledgeBase() {
  const modalMode = knowledgeBaseModal.mode

  try {
    if (!knowledgeBaseModal.name.trim()) {
      throw new Error('知识库名称不能为空。')
    }

    let targetId: Id | null = knowledgeBaseModal.id

    if (modalMode === 'create') {
      const created = await request<KnowledgeBase>('/knowledgeBases', {
        method: 'POST',
        body: JSON.stringify({
          name: knowledgeBaseModal.name.trim(),
          description: normalizeOptionalText(knowledgeBaseModal.description),
        }),
      })
      targetId = created.id
      selectedKnowledgeBaseId.value = created.id
    } else if (knowledgeBaseModal.id) {
      await request<KnowledgeBase>(`/knowledgeBases/${knowledgeBaseModal.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          name: knowledgeBaseModal.name.trim(),
          description: normalizeOptionalText(knowledgeBaseModal.description),
        }),
      })
    }

    closeKnowledgeBaseModal()
    await fetchKnowledgeBases()

    if (targetId) {
      libraryCollapsed.value = false
      await loadKnowledgeBaseContext(targetId)
    }

    showNotice(modalMode === 'create' ? '知识库创建成功。' : '知识库已更新。')
  } catch (error) {
    showNotice((error as Error).message, 'error')
  }
}

async function createKnowledgeBaseTag(name: string, options: { selectForCurrentNote?: boolean } = {}) {
  const knowledgeBaseId = selectedKnowledgeBaseId.value

  if (!knowledgeBaseId) {
    throw new Error('请先选择一个知识库。')
  }

  const normalizedName = name.trim()

  if (!normalizedName) {
    throw new Error('标签名称不能为空。')
  }

  loading.tagSubmit = true

  try {
    const created = await request<TagItem>(`/knowledgeBases/${knowledgeBaseId}/tags`, {
      method: 'POST',
      body: JSON.stringify({
        name: normalizedName,
      }),
    })

    await loadKnowledgeBaseTags(knowledgeBaseId)

    if (options.selectForCurrentNote && currentNote.value?.knowledgeBaseId === knowledgeBaseId) {
      selectedNoteTagIds.value = normalizeTagIds([...selectedNoteTagIds.value, created.id])
      noteTagCreateTick.value += 1
    } else {
      knowledgeBaseTagCreateTick.value += 1
    }

    await refreshKnowledgeBasesSnapshot()
    showNotice(options.selectForCurrentNote ? '标签已创建并选中。' : '标签创建成功。')
    return created
  } finally {
    loading.tagSubmit = false
  }
}

async function createNoteTag(name: string) {
  try {
    await createKnowledgeBaseTag(name, { selectForCurrentNote: true })
  } catch (error) {
    showNotice((error as Error).message, 'error')
  }
}

async function createWorkspaceTag(name: string) {
  try {
    await createKnowledgeBaseTag(name)
  } catch (error) {
    showNotice((error as Error).message, 'error')
  }
}

function toggleNoteTag(tagId: Id) {
  if (!currentNote.value) {
    return
  }

  selectedNoteTagIds.value = selectedNoteTagIds.value.includes(tagId)
    ? selectedNoteTagIds.value.filter((currentTagId) => currentTagId !== tagId)
    : normalizeTagIds([...selectedNoteTagIds.value, tagId])
}

async function submitTemplate() {
  const modalMode = templateModal.mode

  try {
    if (!templateModal.name.trim()) {
      throw new Error('模板名称不能为空。')
    }

    if (!templateModal.templateContent.trim()) {
      throw new Error('模板内容不能为空。')
    }

    loading.templateSubmit = true

    if (modalMode === 'create') {
      await request<NoteTemplate>('/templates', {
        method: 'POST',
        body: JSON.stringify({
          name: templateModal.name.trim(),
          description: normalizeOptionalText(templateModal.description),
          templateContent: templateModal.templateContent,
        }),
      })
    } else if (templateModal.id) {
      await request<NoteTemplate>(`/templates/${templateModal.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          name: templateModal.name.trim(),
          description: normalizeOptionalText(templateModal.description),
          templateContent: templateModal.templateContent,
        }),
      })
    }

    await loadNoteTemplates()
    closeTemplateModal()
    showNotice(modalMode === 'create' ? '模板创建成功。' : '模板已更新。')
  } catch (error) {
    showNotice((error as Error).message, 'error')
  } finally {
    loading.templateSubmit = false
  }
}

function openCreateItemModal(type: CreateItemType, parentId: Id | null = null) {
  if (!selectedKnowledgeBaseId.value) {
    showNotice('请先创建或选择一个知识库。', 'error')
    return
  }

  createItemModal.open = true
  createItemModal.type = type
  createItemModal.parentId = parentId
  createItemModal.name = ''
  createItemModal.templateId = null
}

function closeCreateItemModal() {
  createItemModal.open = false
  createItemModal.parentId = null
  createItemModal.name = ''
  createItemModal.templateId = null
}

function openEditFolderModal(folder: TreeNode) {
  editFolderModal.open = true
  editFolderModal.id = folder.id
  editFolderModal.parentId = folder.parentId
  editFolderModal.name = folder.name
  editFolderModal.busy = false
}

function closeEditFolderModal() {
  editFolderModal.open = false
  editFolderModal.id = null
  editFolderModal.parentId = null
  editFolderModal.name = ''
  editFolderModal.busy = false
}

async function submitEditFolder() {
  if (!selectedKnowledgeBaseId.value || !editFolderModal.id) {
    return
  }

  try {
    if (!editFolderModal.name.trim()) {
      throw new Error('文件夹名称不能为空。')
    }

    editFolderModal.busy = true

    await request(`/knowledgeBases/folders/${editFolderModal.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        parentId: editFolderModal.parentId,
        name: editFolderModal.name.trim(),
      }),
    })

    await loadKnowledgeBaseTree(selectedKnowledgeBaseId.value)
    await refreshKnowledgeBasesSnapshot()
    closeEditFolderModal()
    showNotice('文件夹已重命名。')
  } catch (error) {
    editFolderModal.busy = false
    showNotice((error as Error).message, 'error')
  }
}

async function submitCreateItem() {
  if (!selectedKnowledgeBaseId.value) {
    return
  }

  try {
    if (!createItemModal.name.trim()) {
      throw new Error(createItemModal.type === 'folder' ? '文件夹名称不能为空。' : '笔记标题不能为空。')
    }

    if (createItemModal.type === 'folder') {
      await request('/knowledgeBases/folders', {
        method: 'POST',
        body: JSON.stringify({
          knowledgeBaseId: selectedKnowledgeBaseId.value,
          parentId: createItemModal.parentId,
          name: createItemModal.name.trim(),
        }),
      })

      await loadKnowledgeBaseTree(selectedKnowledgeBaseId.value)
      await refreshKnowledgeBasesSnapshot()
      closeCreateItemModal()
      showNotice('文件夹创建成功。')
      return
    }

    const created = await request<NoteDetail>('/notes', {
      method: 'POST',
      body: JSON.stringify({
        knowledgeBaseId: selectedKnowledgeBaseId.value,
        folderId: createItemModal.parentId,
        title: createItemModal.name.trim(),
        templateId: createItemModal.templateId,
        markdownContent: '',
      }),
    })

    await loadKnowledgeBaseTree(selectedKnowledgeBaseId.value)
    await refreshKnowledgeBasesSnapshot()
    await openNote(created.id)
    closeCreateItemModal()
    showNotice('笔记创建成功。')
  } catch (error) {
    showNotice((error as Error).message, 'error')
  }
}

function openDeleteModal(kind: DeleteKind, targetId: Id, targetName: string) {
  deleteModal.open = true
  deleteModal.kind = kind
  deleteModal.targetId = targetId
  deleteModal.targetName = targetName
  deleteModal.busy = false
}

function handleDeleteTag(payload: { id: Id; name: string }) {
  openDeleteModal('tag', payload.id, payload.name)
}

function closeDeleteModal() {
  deleteModal.open = false
  deleteModal.busy = false
  deleteModal.targetId = null
  deleteModal.targetName = ''
}

async function confirmDelete() {
  if (!deleteModal.targetId) {
    return
  }

  try {
    deleteModal.busy = true

    if (deleteModal.kind === 'knowledge-base') {
      const deletingCurrent = selectedKnowledgeBaseId.value === deleteModal.targetId
      await request(`/knowledgeBases/${deleteModal.targetId}`, {
        method: 'DELETE',
      })

      if (deletingCurrent) {
        selectedKnowledgeBaseId.value = null
        treeNodes.value = []
        selectedFolderId.value = null
        clearCurrentNote()
      }

      await fetchKnowledgeBases()
      showNotice('知识库已删除。')
      closeDeleteModal()
      return
    }

    if (deleteModal.kind === 'folder') {
      const targetFolderNode = findTreeNodeById(treeNodes.value, deleteModal.targetId)
      const deletingFolderIds = new Set<Id>()
      const deletingNoteIds = new Set<Id>()

      if (targetFolderNode) {
        collectSubtreeIds(targetFolderNode, deletingFolderIds, deletingNoteIds)
      }

      await request(`/knowledgeBases/folders/${deleteModal.targetId}`, {
        method: 'DELETE',
      })

      if (selectedFolderId.value && deletingFolderIds.has(selectedFolderId.value)) {
        selectedFolderId.value = null
      }

      if (selectedNoteId.value && deletingNoteIds.has(selectedNoteId.value)) {
        clearCurrentNote()
      }

      if (selectedKnowledgeBaseId.value) {
        await loadKnowledgeBaseTree(selectedKnowledgeBaseId.value)
      }
      await refreshKnowledgeBasesSnapshot()

      showNotice('文件夹已删除。')
      closeDeleteModal()
      return
    }

    if (deleteModal.kind === 'template') {
      await request(`/templates/${deleteModal.targetId}`, {
        method: 'DELETE',
      })

      await loadNoteTemplates()
      showNotice('模板已删除。')
      closeDeleteModal()
      return
    }

    if (deleteModal.kind === 'tag') {
      await request(`/tags/${deleteModal.targetId}`, {
        method: 'DELETE',
      })

      knowledgeBaseTags.value = knowledgeBaseTags.value.filter((tag) => tag.id !== deleteModal.targetId)
      selectedNoteTagIds.value = selectedNoteTagIds.value.filter((tagId) => tagId !== deleteModal.targetId)

      if (currentNote.value) {
        currentNote.value = {
          ...currentNote.value,
          tags: (currentNote.value.tags ?? []).filter((tag) => tag.id !== deleteModal.targetId),
        }
      }

      await refreshKnowledgeBasesSnapshot()

      showNotice('标签已删除。')
      closeDeleteModal()
      return
    }

    if (deleteModal.kind === 'history') {
      if (!currentNote.value?.id) {
        throw new Error('当前未打开笔记，无法删除历史版本。')
      }

      noteHistoryModal.deleting = true

      const currentIndex = noteHistoryModal.versions.findIndex((version) => version.id === deleteModal.targetId)
      const nextVersion =
        noteHistoryModal.versions[currentIndex + 1] ??
        noteHistoryModal.versions[currentIndex - 1] ??
        null

      await request<void>(`/notes/${currentNote.value.id}/histories/${deleteModal.targetId}`, {
        method: 'DELETE',
      })

      await loadNoteHistoryVersions(currentNote.value.id, nextVersion?.id ?? null)
      noteHistoryModal.deleting = false
      showNotice('历史版本已删除。')
      closeDeleteModal()
      return
    }

    await request(`/notes/${deleteModal.targetId}`, {
      method: 'DELETE',
    })

    if (selectedNoteId.value === deleteModal.targetId) {
      clearCurrentNote()
    }

    if (selectedKnowledgeBaseId.value) {
      await loadKnowledgeBaseTree(selectedKnowledgeBaseId.value)
    }
    await refreshKnowledgeBasesSnapshot()

    showNotice('笔记已删除。')
    closeDeleteModal()
  } catch (error) {
    deleteModal.busy = false
    noteHistoryModal.deleting = false
    showNotice((error as Error).message, 'error')
    return
  }

  noteHistoryModal.deleting = false
}

function openGraphNote(noteId: Id) {
  viewMode.value = 'home'
  void openNote(noteId).catch((error: Error) => {
    showNotice(error.message, 'error')
  })
}

watch(
  () => [viewMode.value, selectedKnowledgeBaseId.value] as const,
  ([mode, knowledgeBaseId]) => {
    if (mode !== 'graph-d3') {
      return
    }

    if (!knowledgeBaseId) {
      knowledgeGraph.value = null
      return
    }

    void loadKnowledgeGraph(knowledgeBaseId).catch((error: Error) => {
      showNotice(error.message, 'error')
    })
  },
  { immediate: true },
)

watch(
  () => currentNote.value?.id ?? null,
  (noteId) => {
    if (!noteId && noteHistoryModal.open) {
      closeNoteHistoryModal()
    }
  },
)

onMounted(() => {
  syncProfileForm()
  void initializeApp()
})
</script>

<template>
  <div
    v-if="hasToken"
    class="shell"
    :class="{
      'library-collapsed': libraryCollapsed || !showKnowledgeBaseSidebar,
      'editor-fullscreen-active': workspaceFullscreen,
    }"
  >
    <AppRail
      :view-mode="viewMode"
      :display-name="displayName"
      :display-email="displayEmail"
      :current-avatar-url="currentAvatarUrl"
      :user-initial="userInitial"
      @open-home="openHomeDashboard"
      @open-templates="viewMode = 'templates'"
      @open-settings="viewMode = 'settings'"
      @open-graph-d3="openGraphD3View"
      @logout="logout()"
    />

    <div v-if="showKnowledgeBaseSidebar && !libraryCollapsed" class="library-column">
      <LibraryPanel
        :user-name="displayName"
        :selected-knowledge-base-id="selectedKnowledgeBaseId"
        :selected-knowledge-base-name="selectedKnowledgeBase?.name || ''"
        :selected-knowledge-base-description="selectedKnowledgeBase?.description || ''"
        :tree-nodes="treeNodes"
        :selected-folder-id="selectedFolderId"
        :selected-note-id="selectedNoteId"
        :loading-tree="loading.tree"
        :has-current-note="Boolean(currentNote)"
        @create-folder="openCreateItemModal('folder', $event)"
        @create-note="openCreateItemModal('note', $event)"
        @edit-folder="openEditFolderModal"
        @select-tree-node="handleTreeNodeSelect"
        @delete-folder="openDeleteModal('folder', $event.id, $event.name)"
        @delete-note="openDeleteModal('note', $event.id, $event.name)"
        @exit-knowledge-base="openHomeDashboard"
        @open-knowledge-base-directory="openCurrentKnowledgeBaseWorkspace"
        @toggle-collapse="toggleLibraryPanel(true)"
      />
    </div>

    <main
      class="workspace"
      :class="{
        'editor-fullscreen-active': workspaceFullscreen,
        'note-page-active': Boolean(currentNote),
      }"
    >
      <div v-if="notice.text" class="notice" :class="notice.type">
        {{ notice.text }}
      </div>

      <HomeView
        v-if="viewMode === 'home'"
        v-model:note-title="noteTitle"
        v-model:note-content="noteContent"
        :loading-note="loading.note"
        :loading-knowledge-bases="loading.knowledgeBases"
        :loading-tags="loading.tags"
        :submitting-tag="loading.tagSubmit"
        :creating-history-version="loading.save"
        :knowledge-base-tag-create-tick="knowledgeBaseTagCreateTick"
        :note-tag-create-tick="noteTagCreateTick"
        :loading-templates="loading.templates"
        :loading-tree="loading.tree"
        :current-note="currentNote"
        :knowledge-base-tags="knowledgeBaseTags"
        :selected-note-tag-ids="selectedNoteTagIds"
        :selected-knowledge-base-id="selectedKnowledgeBaseId"
        :selected-knowledge-base="selectedKnowledgeBase"
        :selected-knowledge-base-name="selectedKnowledgeBase?.name || ''"
        :selected-knowledge-base-description="selectedKnowledgeBase?.description || ''"
        :knowledge-bases="knowledgeBases"
        :note-templates="noteTemplates"
        :tree-nodes="treeNodes"
        :selected-folder-id="selectedFolderId"
        :selected-note-id="selectedNoteId"
        :knowledge-base-count="knowledgeBases.length"
        :folder-count="folderCount"
        :note-count="noteCount"
        :show-library-panel-toggle="showKnowledgeBaseSidebar && libraryCollapsed"
        :editor-content-theme="userContentTheme"
        :editor-code-theme="userCodeTheme"
        :on-create-knowledge-base-tag="createWorkspaceTag"
        :on-create-note-tag="createNoteTag"
        :on-toggle-note-tag="toggleNoteTag"
        :on-delete-tag="handleDeleteTag"
        :on-search-knowledge-base-notes="searchKnowledgeBaseNotes"
        @select-knowledge-base="selectKnowledgeBase"
        @select-tree-node="handleTreeNodeSelect"
        @create-folder="openCreateItemModal('folder', $event)"
        @create-note="openCreateItemModal('note', $event)"
        @edit-folder="openEditFolderModal"
        @open-note-history="openNoteHistoryModal"
        @open-note-link="openGraphNote"
        @toggle-library-panel="toggleLibraryPanel(false)"
        @create-note-history-version="createHistoryVersion"
        @editor-fullscreen-change="workspaceFullscreen = $event"
        @editor-theme-change="handleEditorThemeChange"
        @create-knowledge-base="openKnowledgeBaseModal('create')"
        @delete-folder="openDeleteModal('folder', $event.id, $event.name)"
        @delete-note="openDeleteModal('note', $event.id, $event.name)"
        @edit-knowledge-base="openKnowledgeBaseModal('edit', $event)"
        @delete-knowledge-base="openDeleteModal('knowledge-base', $event.id, $event.name)"
      />
      <UserSettingsView
        v-else-if="viewMode === 'settings'"
        v-model:content-theme="userContentTheme"
        v-model:code-theme="userCodeTheme"
        :loading-profile="loading.profile"
        :loading-avatar-upload="loading.avatarUpload"
        :loading-user-settings="loading.userSettings"
        :loading-theme-options="loading.themeOptions"
        :current-username="currentUser?.username || ''"
        :profile-form="profileForm"
        :display-name="displayName"
        :display-email="displayEmail"
        :user-initial="userInitial"
        :profile-avatar-url="profileAvatarUrl"
        :content-theme-options="contentThemeOptions"
        :code-theme-options="codeThemeOptions"
        @save-profile="saveProfile"
        @avatar-change="handleAvatarChange"
        @theme-selection-change="handleSettingsThemeSelectionChange"
      />

      <NoteTemplatesView
        v-else-if="viewMode === 'templates'"
        :loading-templates="loading.templates"
        :note-templates="noteTemplates"
        :format-date-time="formatDateTime"
        @create-template="openTemplateModal('create')"
        @edit-template="openTemplateModal('edit', $event)"
        @delete-template="openDeleteModal('template', $event.id, $event.name)"
      />
      <div v-else-if="viewMode === 'graph-d3'" class="main-panel large-card graph-page-panel">
        <KnowledgeGraphD3View
          :graph="knowledgeGraph"
          :loading="loading.graph"
          :selected-knowledge-base-id="selectedKnowledgeBaseId"
          :knowledge-base-name="selectedKnowledgeBase?.name || ''"
          @open-note="openGraphNote"
        />
      </div>

      <footer class="workspace-statusbar" aria-label="工作区状态栏">
        <div class="workspace-status-left">
          <span v-for="item in statusbarLeftItems" :key="item" class="statusbar-item">
            {{ item }}
          </span>
        </div>
        <div class="workspace-status-right">
          <span v-for="item in statusbarRightItems" :key="item" class="statusbar-item">
            {{ item }}
          </span>
        </div>
      </footer>
    </main>
  </div>

  <AuthPanel
    v-else
    :auth-form="authForm"
    :auth-title="authTitle"
    :auth-submit-text="authSubmitText"
    :loading-auth="loading.auth"
    :notice-text="notice.text"
    :notice-type="notice.type"
    @submit="submitAuth"
  />
  <KnowledgeBaseModalView
    v-if="knowledgeBaseModal.open"
    :modal="knowledgeBaseModal"
    @close="closeKnowledgeBaseModal"
    @submit="submitKnowledgeBase"
  />

  <NoteTemplateModal
    v-if="templateModal.open"
    :modal="templateModal"
    :submitting="loading.templateSubmit"
    @close="closeTemplateModal"
    @submit="submitTemplate"
  />

  <EditFolderModal
    v-if="editFolderModal.open"
    :modal="editFolderModal"
    @close="closeEditFolderModal"
    @submit="submitEditFolder"
  />

  <NoteHistoryModal
    v-if="noteHistoryModal.open"
    :modal="noteHistoryModal"
    @close="closeNoteHistoryModal"
    @select-version="selectNoteHistoryVersion"
    @delete="deleteNoteHistoryVersion"
    @restore="restoreNoteHistoryVersion"
  />

  <CreateItemModal
    v-if="createItemModal.open"
    :modal="createItemModal"
    :loading-templates="loading.templates"
    :note-templates="noteTemplates"
    :selected-create-template="selectedCreateTemplate"
    @close="closeCreateItemModal"
    @submit="submitCreateItem"
  />

  <DeleteConfirmModal
    v-if="deleteModal.open"
    :modal="deleteModal"
    :title="deleteTitle"
    :copy="deleteCopy"
    @close="closeDeleteModal"
    @confirm="confirmDelete"
  />
</template>

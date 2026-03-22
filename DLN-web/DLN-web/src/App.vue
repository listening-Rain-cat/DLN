<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AuthPanel from './components/auth/AuthPanel.vue'
import LibraryPanel from './components/layout/LibraryPanel.vue'
import HomeView from './components/workspace/HomeView.vue'

type Id = string
type ViewMode = 'home' | 'settings' | 'graph'
type NoticeType = 'success' | 'error'
type AuthMode = 'login' | 'register'
type KnowledgeBaseModalMode = 'create' | 'edit'
type CreateItemType = 'folder' | 'note'
type DeleteKind = 'knowledge-base' | 'folder' | 'note'

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

interface LoginResponse {
  id: Id
  username: string
  email: string
  nickname: string
  avatarUrl?: string | null
  status?: number
  createdTime?: string
  updatedTime?: string
  token: string
}

interface UserInfo {
  id: Id
  username: string
  email: string
  nickname: string
  avatarUrl?: string | null
  status?: number
  createdTime?: string
  updatedTime?: string
}

interface KnowledgeBase {
  id: Id
  name: string
  description?: string | null
  status?: number
  createdTime?: string
  updatedTime?: string
}

interface TreeNode {
  id: Id
  parentId: Id | null
  name: string
  type: 'folder' | 'note'
  knowledgeBaseId: Id
  updatedTime?: string
  children?: TreeNode[]
}

interface TagItem {
  id: Id
  name: string
}

interface AttachmentItem {
  id: Id
  fileName: string
  fileUrl: string
}

interface LinkItem {
  id: Id
  sourceNoteId?: Id
  targetNoteId?: Id | null
  targetNoteName?: string
}

interface NoteDetail {
  id: Id
  knowledgeBaseId: Id
  folderId: Id | null
  title: string
  status?: number
  markdownContent: string
  createdTime?: string
  updatedTime?: string
  tags?: TagItem[]
  attachments?: AttachmentItem[]
  outgoingLinks?: LinkItem[]
  incomingLinks?: LinkItem[]
}

const API_BASE = ((import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080')
  .trim()
  .replace(/\/+$/, '')

const TOKEN_KEY = 'dln-token'
const LEGACY_TOKEN_KEY = 'token'
const USER_KEY = 'dln-user'

function loadStoredUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

function normalizeOptionalText(value: string) {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

function normalizeNote(note: NoteDetail): NoteDetail {
  return {
    ...note,
    markdownContent: note.markdownContent || '',
    tags: note.tags ?? [],
    attachments: note.attachments ?? [],
    outgoingLinks: note.outgoingLinks ?? [],
    incomingLinks: note.incomingLinks ?? [],
  }
}

const token = ref(localStorage.getItem(TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY) || '')
const currentUser = ref<UserInfo | null>(loadStoredUser())
const viewMode = ref<ViewMode>('home')

const knowledgeBases = ref<KnowledgeBase[]>([])
const treeNodes = ref<TreeNode[]>([])
const selectedKnowledgeBaseId = ref<Id | null>(null)
const selectedFolderId = ref<Id | null>(null)
const selectedNoteId = ref<Id | null>(null)
const currentNote = ref<NoteDetail | null>(null)
const noteTitle = ref('')
const noteContent = ref('')

const notice = reactive({
  text: '',
  type: 'success' as NoticeType,
})

let noticeTimer: number | null = null

const loading = reactive({
  auth: false,
  knowledgeBases: false,
  tree: false,
  note: false,
  save: false,
  profile: false,
})

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

const createItemModal = reactive({
  open: false,
  type: 'folder' as CreateItemType,
  parentId: null as Id | null,
  name: '',
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

  return '删除笔记'
})

const deleteCopy = computed(() => {
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

const noteCount = computed(() => {
  return noteNodes.value.length
})

const noteSaved = computed(() => {
  if (!currentNote.value) {
    return false
  }

  return (
    noteTitle.value.trim() === currentNote.value.title &&
    noteContent.value === (currentNote.value.markdownContent || '')
  )
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

function syncProfileForm() {
  profileForm.nickname = currentUser.value?.nickname ?? ''
  profileForm.email = currentUser.value?.email ?? ''
  profileForm.avatarUrl = currentUser.value?.avatarUrl ?? ''
  profileForm.oldPassword = ''
  profileForm.newPassword = ''
  profileForm.confirmPassword = ''
}

function persistUser(user: UserInfo | null) {
  currentUser.value = user

  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
  }

  syncProfileForm()
}

function applyLoginUser(user: LoginResponse) {
  persistUser({
    id: user.id,
    username: user.username,
    email: user.email,
    nickname: user.nickname,
    avatarUrl: user.avatarUrl,
    status: user.status,
    createdTime: user.createdTime,
    updatedTime: user.updatedTime,
  })
}

function clearCurrentNote() {
  currentNote.value = null
  selectedNoteId.value = null
  noteTitle.value = ''
  noteContent.value = ''
}

function resetWorkspace() {
  knowledgeBases.value = []
  treeNodes.value = []
  selectedKnowledgeBaseId.value = null
  selectedFolderId.value = null
  clearCurrentNote()
}

function logout(showMessage = true) {
  token.value = ''
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(LEGACY_TOKEN_KEY)
  persistUser(null)
  resetWorkspace()
  viewMode.value = 'home'
  authForm.mode = 'login'
  authForm.password = ''
  authForm.confirmPassword = ''

  if (showMessage) {
    showNotice('已退出登录。')
  }
}

async function request<T>(path: string, init: RequestInit = {}, withAuth = true): Promise<T> {
  const headers = new Headers(init.headers ?? {})

  if (!(init.body instanceof FormData) && init.body != null && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (withAuth && token.value) {
    headers.set('Authorization', token.value.startsWith('Bearer ') ? token.value : `Bearer ${token.value}`)
  }

  let response: Response

  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers,
    })
  } catch {
    throw new Error('无法连接后端服务，请确认后端已启动并允许当前页面访问。')
  }

  const rawText = await response.text()
  let payload: ApiResult<T> | null = null

  if (rawText) {
    try {
      payload = JSON.parse(rawText) as ApiResult<T>
    } catch {
      payload = null
    }
  }

  if (response.status === 401) {
    logout(false)
    showNotice('登录状态已过期，请重新登录。', 'error')
    throw new Error(payload?.message || '登录状态已过期。')
  }

  if (!response.ok || !payload || payload.code !== 200) {
    throw new Error(payload?.message || `请求失败，状态码 ${response.status}。`)
  }

  return payload.data
}

async function fetchUserInfo() {
  const user = await request<UserInfo>('/userInfo')
  persistUser(user)
}

async function loadKnowledgeBaseTree(knowledgeBaseId: Id) {
  viewMode.value = 'home'
  selectedKnowledgeBaseId.value = knowledgeBaseId

  if (currentNote.value?.knowledgeBaseId !== knowledgeBaseId) {
    selectedFolderId.value = null
    clearCurrentNote()
  }

  loading.tree = true

  try {
    treeNodes.value = await request<TreeNode[]>(`/knowledgeBases/${knowledgeBaseId}/tree`)
  } finally {
    loading.tree = false
  }

  if (currentNote.value?.knowledgeBaseId === knowledgeBaseId) {
    selectedFolderId.value = currentNote.value.folderId
  }
}

async function fetchKnowledgeBases() {
  loading.knowledgeBases = true

  try {
    const data = await request<KnowledgeBase[]>('/knowledgeBases')
    knowledgeBases.value = data

    if (!data.length) {
      selectedKnowledgeBaseId.value = null
      treeNodes.value = []
      selectedFolderId.value = null
      clearCurrentNote()
      return
    }

    const nextId = data.some((item) => item.id === selectedKnowledgeBaseId.value)
      ? selectedKnowledgeBaseId.value
      : data[0].id

    if (nextId) {
      await loadKnowledgeBaseTree(nextId)
    }
  } finally {
    loading.knowledgeBases = false
  }
}

async function initializeApp() {
  if (!hasToken.value) {
    resetWorkspace()
    syncProfileForm()
    return
  }

  try {
    await Promise.all([fetchKnowledgeBases(), fetchUserInfo()])
  } catch (error) {
    showNotice((error as Error).message, 'error')
  }
}

async function submitAuth() {
  const mode = authForm.mode

  try {
    if (!authForm.username.trim() || !authForm.password.trim()) {
      throw new Error('用户名和密码不能为空。')
    }

    loading.auth = true

    if (mode === 'register') {
      if (!authForm.nickname.trim() || !authForm.email.trim()) {
        throw new Error('注册时请填写昵称和邮箱。')
      }

      if (authForm.password !== authForm.confirmPassword) {
        throw new Error('两次输入的密码不一致。')
      }

      await request(
        '/register',
        {
          method: 'POST',
          body: JSON.stringify({
            username: authForm.username.trim(),
            password: authForm.password,
            confirmPassword: authForm.confirmPassword,
            email: authForm.email.trim(),
            nickname: authForm.nickname.trim(),
          }),
        },
        false,
      )
    }

    const loginData = await request<LoginResponse>(
      '/login',
      {
        method: 'POST',
        body: JSON.stringify({
          username: authForm.username.trim(),
          password: authForm.password,
        }),
      },
      false,
    )

    token.value = loginData.token
    localStorage.setItem(TOKEN_KEY, loginData.token)
    localStorage.setItem(LEGACY_TOKEN_KEY, loginData.token)
    applyLoginUser(loginData)
    authForm.mode = 'login'
    authForm.password = ''
    authForm.confirmPassword = ''
    viewMode.value = 'home'
    await initializeApp()
    showNotice(mode === 'login' ? '登录成功。' : '注册成功。')
  } catch (error) {
    showNotice((error as Error).message, 'error')
  } finally {
    loading.auth = false
  }
}

async function saveProfile() {
  try {
    if (!profileForm.nickname.trim() || !profileForm.email.trim()) {
      throw new Error('昵称和邮箱不能为空。')
    }

    if (profileForm.oldPassword || profileForm.newPassword || profileForm.confirmPassword) {
      if (!profileForm.oldPassword) {
        throw new Error('修改密码前请先输入当前密码。')
      }

      if (!profileForm.newPassword) {
        throw new Error('请输入新密码。')
      }

      if (profileForm.newPassword !== profileForm.confirmPassword) {
        throw new Error('两次输入的新密码不一致。')
      }
    }

    loading.profile = true

    await request('/user', {
      method: 'PUT',
      body: JSON.stringify({
        nickname: profileForm.nickname.trim(),
        email: profileForm.email.trim(),
        avatarUrl: normalizeOptionalText(profileForm.avatarUrl),
        oldPassword: normalizeOptionalText(profileForm.oldPassword),
        newPassword: normalizeOptionalText(profileForm.newPassword),
      }),
    })

    await fetchUserInfo()
    profileForm.oldPassword = ''
    profileForm.newPassword = ''
    profileForm.confirmPassword = ''
    showNotice('个人资料已更新。')
  } catch (error) {
    showNotice((error as Error).message, 'error')
  } finally {
    loading.profile = false
  }
}

async function selectKnowledgeBase(knowledgeBaseId: Id) {
  try {
    await loadKnowledgeBaseTree(knowledgeBaseId)
  } catch (error) {
    showNotice((error as Error).message, 'error')
  }
}

async function openNote(noteId: Id) {
  loading.note = true

  try {
    const detail = normalizeNote(await request<NoteDetail>(`/notes/${noteId}`))
    currentNote.value = detail
    selectedNoteId.value = detail.id
    selectedFolderId.value = detail.folderId
    noteTitle.value = detail.title
    noteContent.value = detail.markdownContent || ''
    viewMode.value = 'home'
  } finally {
    loading.note = false
  }
}

function handleTreeNodeSelect(node: TreeNode) {
  viewMode.value = 'home'

  if (node.type === 'folder') {
    selectedFolderId.value = node.id
    clearCurrentNote()
    return
  }

  void openNote(node.id).catch((error: Error) => {
    showNotice(error.message, 'error')
  })
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
      await loadKnowledgeBaseTree(targetId)
    }

    showNotice(modalMode === 'create' ? '知识库创建成功。' : '知识库已更新。')
  } catch (error) {
    showNotice((error as Error).message, 'error')
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
}

function closeCreateItemModal() {
  createItemModal.open = false
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
        markdownContent: `# ${createItemModal.name.trim()}\n\n`,
      }),
    })

    await loadKnowledgeBaseTree(selectedKnowledgeBaseId.value)
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
      await request(`/knowledgeBases/folders/${deleteModal.targetId}`, {
        method: 'DELETE',
      })

      if (selectedFolderId.value === deleteModal.targetId) {
        selectedFolderId.value = null
      }

      if (selectedKnowledgeBaseId.value) {
        await loadKnowledgeBaseTree(selectedKnowledgeBaseId.value)
      }

      showNotice('文件夹已删除。')
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

    showNotice('笔记已删除。')
    closeDeleteModal()
  } catch (error) {
    deleteModal.busy = false
    showNotice((error as Error).message, 'error')
  }
}

async function saveNote() {
  if (!currentNote.value) {
    return
  }

  try {
    if (!noteTitle.value.trim()) {
      throw new Error('笔记标题不能为空。')
    }

    loading.save = true

    const updated = normalizeNote(
      await request<NoteDetail>(`/notes/${currentNote.value.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          folderId: currentNote.value.folderId,
          title: noteTitle.value.trim(),
          markdownContent: noteContent.value,
        }),
      }),
    )

    currentNote.value = updated
    selectedNoteId.value = updated.id
    selectedFolderId.value = updated.folderId
    noteTitle.value = updated.title
    noteContent.value = updated.markdownContent || ''

    if (selectedKnowledgeBaseId.value) {
      await loadKnowledgeBaseTree(selectedKnowledgeBaseId.value)
    }

    showNotice('笔记已保存。')
  } catch (error) {
    showNotice((error as Error).message, 'error')
  } finally {
    loading.save = false
  }
}

function graphNodeStyle(index: number) {
  const total = Math.max(noteNodes.value.length, 1)
  const angle = (Math.PI * 2 * index) / total - Math.PI / 2
  const radius = 150 + (index % 3) * 24

  return {
    transform: `translate(${Math.cos(angle) * radius}px, ${Math.sin(angle) * radius}px)`,
  }
}

function openGraphNote(noteId: Id) {
  viewMode.value = 'home'
  void openNote(noteId).catch((error: Error) => {
    showNotice(error.message, 'error')
  })
}

onMounted(() => {
  syncProfileForm()
  void initializeApp()
})
</script>

<template>
  <div v-if="hasToken" class="shell">
    <aside class="rail">
      <div class="logo-mark">DL</div>

      <button type="button" class="rail-button" :class="{ active: viewMode === 'home' }" @click="viewMode = 'home'">
        主页
      </button>
      <button
        type="button"
        class="rail-button"
        :class="{ active: viewMode === 'settings' }"
        @click="viewMode = 'settings'"
      >
        设置
      </button>
      <button type="button" class="rail-button" :class="{ active: viewMode === 'graph' }" @click="viewMode = 'graph'">
        图谱
      </button>

      <div class="rail-bottom">
        <strong>{{ displayName }}</strong>
        <span>{{ displayEmail }}</span>
      </div>
    </aside>

    <LibraryPanel
      :knowledge-bases="knowledgeBases"
      :selected-knowledge-base-id="selectedKnowledgeBaseId"
      :selected-knowledge-base-name="selectedKnowledgeBase?.name || ''"
      :tree-nodes="treeNodes"
      :selected-folder-id="selectedFolderId"
      :selected-note-id="selectedNoteId"
      :loading-tree="loading.tree"
      :loading-knowledge-bases="loading.knowledgeBases"
      @create-knowledge-base="openKnowledgeBaseModal('create')"
      @edit-knowledge-base="openKnowledgeBaseModal('edit', $event)"
      @delete-knowledge-base="openDeleteModal('knowledge-base', $event.id, $event.name)"
      @select-knowledge-base="selectKnowledgeBase"
      @create-folder="openCreateItemModal('folder', $event)"
      @create-note="openCreateItemModal('note', $event)"
      @select-tree-node="handleTreeNodeSelect"
      @delete-folder="openDeleteModal('folder', $event.id, $event.name)"
      @delete-note="openDeleteModal('note', $event.id, $event.name)"
    />
    <main class="workspace">
      <header class="workspace-header">
        <div>
          <p class="eyebrow">
            {{ viewMode === 'home' ? '工作台' : viewMode === 'settings' ? '用户设置' : '知识图谱' }}
          </p>
          <h1>
            {{
              viewMode === 'home'
                ? currentNote?.title || selectedKnowledgeBase?.name || '准备开始记录'
                : viewMode === 'settings'
                  ? '账号设置'
                  : selectedKnowledgeBase?.name || '图谱视图'
            }}
          </h1>
          <p class="workspace-copy">
            {{
              viewMode === 'home'
                ? '左侧管理目录树，中间编辑 Markdown，右侧可按标题快速跳转。'
                : viewMode === 'settings'
                  ? '这里可以修改个人资料和密码。后端地址已经改为通过项目配置管理，不再在界面中展示。'
                  : '这里展示当前知识库的轻量知识图谱视图。'
            }}
          </p>
        </div>

        <div class="inline-actions">
          <button
            v-if="viewMode === 'home' && currentNote"
            type="button"
            class="soft-button accent"
            :disabled="loading.save"
            @click="saveNote"
          >
            {{ loading.save ? '保存中...' : noteSaved ? '已保存' : '保存笔记' }}
          </button>
          <button type="button" class="soft-button" @click="logout()">退出登录</button>
        </div>
      </header>

      <div v-if="notice.text" class="notice" :class="notice.type">
        {{ notice.text }}
      </div>

      <HomeView
        v-if="viewMode === 'home'"
        v-model:note-title="noteTitle"
        v-model:note-content="noteContent"
        :loading-note="loading.note"
        :current-note="currentNote"
        :selected-knowledge-base-name="selectedKnowledgeBase?.name || ''"
        :knowledge-base-count="knowledgeBases.length"
        :folder-count="folderCount"
        :note-count="noteCount"
      />
      <div v-else-if="viewMode === 'settings'" class="settings-grid">
        <article class="main-panel">
          <div class="panel-heading compact">
            <div>
              <p class="eyebrow">个人资料</p>
              <h3>公开显示信息</h3>
            </div>
            <button type="button" class="soft-button accent" :disabled="loading.profile" @click="saveProfile">
              {{ loading.profile ? '保存中...' : '保存资料' }}
            </button>
          </div>

          <div class="profile-summary">
            <div class="avatar-chip">{{ userInitial }}</div>
            <div>
              <strong>{{ displayName }}</strong>
              <p>{{ displayEmail }}</p>
            </div>
          </div>

          <label class="field">
            <span>昵称</span>
            <input v-model="profileForm.nickname" type="text" />
          </label>

          <label class="field">
            <span>邮箱</span>
            <input v-model="profileForm.email" type="email" />
          </label>

          <label class="field">
            <span>头像地址</span>
            <input v-model="profileForm.avatarUrl" type="text" />
          </label>
        </article>

        <article class="main-panel">
          <div class="panel-heading compact">
            <div>
              <p class="eyebrow">安全</p>
              <h3>密码与会话</h3>
            </div>
            <button type="button" class="soft-button" @click="logout()">退出登录</button>
          </div>

          <label class="field">
            <span>用户名</span>
            <input :value="currentUser?.username || ''" type="text" readonly />
          </label>

          <label class="field">
            <span>当前密码</span>
            <input v-model="profileForm.oldPassword" type="password" />
          </label>

          <label class="field">
            <span>新密码</span>
            <input v-model="profileForm.newPassword" type="password" />
          </label>

          <label class="field">
            <span>确认新密码</span>
            <input v-model="profileForm.confirmPassword" type="password" />
          </label>

          <div class="empty-card compact-card">
            界面中已经不再提供后端地址输入框。如果部署时需要修改接口地址，请配置
            <code>VITE_API_BASE_URL</code>。
          </div>
        </article>
      </div>

      <div v-else class="main-panel large-card">
        <div class="graph-stage">
          <div class="graph-center">
            <p class="eyebrow">知识图谱</p>
            <h3>{{ selectedKnowledgeBase?.name || '请选择知识库' }}</h3>
            <p>当前视图中共展示 {{ noteNodes.length }} 篇笔记。</p>
          </div>

          <button
            v-for="(note, index) in noteNodes"
            :key="note.id"
            type="button"
            class="graph-node"
            :style="graphNodeStyle(index)"
            @click="openGraphNote(note.id)"
          >
            {{ note.name }}
          </button>
        </div>
      </div>
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
  <div v-if="knowledgeBaseModal.open" class="modal-backdrop" @click.self="closeKnowledgeBaseModal">
    <div class="modal-card">
      <div class="modal-heading">
        <p class="eyebrow">知识库</p>
        <h3>{{ knowledgeBaseModal.mode === 'create' ? '新建知识库' : '编辑知识库' }}</h3>
      </div>

      <label class="field">
        <span>名称</span>
        <input v-model="knowledgeBaseModal.name" type="text" placeholder="例如：分布式系统" />
      </label>

      <label class="field">
        <span>描述</span>
        <textarea
          v-model="knowledgeBaseModal.description"
          rows="4"
          placeholder="给这个知识库写一段简短说明"
        ></textarea>
      </label>

      <div class="modal-actions">
        <button type="button" class="soft-button" @click="closeKnowledgeBaseModal">取消</button>
        <button type="button" class="soft-button accent" @click="submitKnowledgeBase">
          {{ knowledgeBaseModal.mode === 'create' ? '创建' : '保存' }}
        </button>
      </div>
    </div>
  </div>

  <div v-if="createItemModal.open" class="modal-backdrop" @click.self="closeCreateItemModal">
    <div class="modal-card">
      <div class="modal-heading">
        <p class="eyebrow">{{ createItemModal.type === 'folder' ? '文件夹' : '笔记' }}</p>
        <h3>{{ createItemModal.type === 'folder' ? '新建文件夹' : '新建笔记' }}</h3>
      </div>

      <label class="field">
        <span>{{ createItemModal.type === 'folder' ? '文件夹名称' : '笔记标题' }}</span>
        <input
          v-model="createItemModal.name"
          type="text"
          :placeholder="createItemModal.type === 'folder' ? '例如：阅读清单' : '例如：Redis 基础'"
        />
      </label>

      <div class="modal-actions">
        <button type="button" class="soft-button" @click="closeCreateItemModal">取消</button>
        <button type="button" class="soft-button accent" @click="submitCreateItem">
          {{ createItemModal.type === 'folder' ? '创建文件夹' : '创建笔记' }}
        </button>
      </div>
    </div>
  </div>
  <div v-if="deleteModal.open" class="modal-backdrop" @click.self="closeDeleteModal">
    <div class="modal-card">
      <div class="modal-heading">
        <p class="eyebrow">确认操作</p>
        <h3>{{ deleteTitle }}</h3>
      </div>

      <p class="modal-copy">{{ deleteCopy }}</p>

      <div class="modal-actions">
        <button type="button" class="soft-button" :disabled="deleteModal.busy" @click="closeDeleteModal">
          取消
        </button>
        <button type="button" class="soft-button modal-danger" :disabled="deleteModal.busy" @click="confirmDelete">
          {{ deleteModal.busy ? '删除中...' : '删除' }}
        </button>
      </div>
    </div>
  </div>
</template>


<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import KnowledgeTreeNode from '../KnowledgeTreeNode.vue'
import VditorEditor from '../VditorEditor.vue'
import OutlineTreeNode from './OutlineTreeNode.vue'

type Id = string

interface LinkItem {
  id: string
}

interface TagItem {
  id: string
  name: string
  createdTime?: string
}

interface AttachmentItem {
  id: string
}

interface NoteDetail {
  id: string
  title: string
  markdownContent: string
  outgoingLinks?: LinkItem[]
  incomingLinks?: LinkItem[]
  tags?: TagItem[]
  attachments?: AttachmentItem[]
}

interface OutlineItem {
  id: string
  text: string
  level: number
}

interface OutlineTreeItem extends OutlineItem {
  children: OutlineTreeItem[]
}

interface EditorExpose {
  scrollToHeading: (id: string) => void
}

interface KnowledgeBase {
  id: Id
  name: string
  description?: string | null
  createdTime?: string
  updatedTime?: string
}

interface NoteTemplate {
  id: Id
  name: string
  description?: string | null
  templateContent: string
  createdTime?: string
  updatedTime?: string
}

interface TreeNode {
  id: Id
  parentId: Id | null
  name: string
  type: 'folder' | 'note'
  knowledgeBaseId: Id
  createdTime?: string
  updatedTime?: string
  tags?: TagItem[]
  children?: TreeNode[]
}

interface TagActionPayload {
  id: Id
  name: string
}

interface EditorThemeSettings {
  contentTheme: string
  codeTheme: string
}

const props = defineProps<{
  loadingNote: boolean
  loadingKnowledgeBases: boolean
  loadingTags: boolean
  submittingTag: boolean
  savingNote: boolean
  noteSaved: boolean
  knowledgeBaseTagCreateTick: number
  noteTagCreateTick: number
  loadingTemplates: boolean
  loadingTree: boolean
  currentNote: NoteDetail | null
  knowledgeBaseTags: TagItem[]
  selectedNoteTagIds: Id[]
  noteTitle: string
  noteContent: string
  selectedKnowledgeBaseId: Id | null
  selectedKnowledgeBase: KnowledgeBase | null
  selectedKnowledgeBaseName: string
  selectedKnowledgeBaseDescription: string
  knowledgeBases: KnowledgeBase[]
  noteTemplates: NoteTemplate[]
  treeNodes: TreeNode[]
  selectedFolderId: Id | null
  selectedNoteId: Id | null
  knowledgeBaseCount: number
  folderCount: number
  noteCount: number
  showLibraryPanelToggle: boolean
  editorContentTheme: string
  editorCodeTheme: string
  onCreateKnowledgeBaseTag?: (name: string) => void | Promise<void>
  onCreateNoteTag?: (name: string) => void | Promise<void>
  onToggleNoteTag?: (id: Id) => void
  onDeleteTag?: (payload: TagActionPayload) => void
}>()

const emit = defineEmits<{
  (e: 'update:noteTitle', value: string): void
  (e: 'update:noteContent', value: string): void
  (e: 'select-knowledge-base', id: Id): void
  (e: 'select-tree-node', node: TreeNode): void
  (e: 'create-folder', parentId: Id | null): void
  (e: 'create-note', parentId: Id | null): void
  (e: 'edit-folder', node: TreeNode): void
  (e: 'create-knowledge-base'): void
  (e: 'delete-folder', payload: { id: Id; name: string }): void
  (e: 'delete-note', payload: { id: Id; name: string }): void
  (e: 'create-knowledge-base-tag', name: string): void
  (e: 'create-note-tag', name: string): void
  (e: 'toggle-note-tag', id: Id): void
  (e: 'delete-tag', payload: TagActionPayload): void
  (e: 'edit-knowledge-base', item: KnowledgeBase): void
  (e: 'delete-knowledge-base', payload: { id: Id; name: string }): void
  (e: 'create-note-template'): void
  (e: 'edit-note-template', item: NoteTemplate): void
  (e: 'delete-note-template', payload: { id: Id; name: string }): void
  (e: 'toggle-library-panel'): void
  (e: 'save-note'): void
  (e: 'editor-fullscreen-change', value: boolean): void
  (e: 'editor-theme-change', value: EditorThemeSettings): void
}>()

const editorRef = ref<EditorExpose | null>(null)
const knowledgeBaseTagInputRef = ref<HTMLInputElement | null>(null)
const noteTagInputRef = ref<HTMLInputElement | null>(null)
const outlineItems = ref<OutlineItem[]>([])
const outlineCollapsed = ref(false)
const activeOutlineId = ref<string | null>(null)
const outlineWidth = ref(310)
const isResizingOutline = ref(false)
const editorFullscreen = ref(false)
const tagDrawerOpen = ref(false)
const knowledgeBaseTagName = ref('')
const noteTagName = ref('')
const knowledgeBaseTagError = ref('')
const noteTagError = ref('')

const OUTLINE_MIN_WIDTH = 220
const OUTLINE_MAX_WIDTH = 420
let resizeStartX = 0
let resizeStartWidth = 0

const workspaceBodyStyle = computed(() => {
  return {
    '--outline-width': `${outlineWidth.value}px`,
  }
})

function handleEditorFullscreenChange(value: boolean) {
  editorFullscreen.value = value
  emit('editor-fullscreen-change', value)
}

const noteTitleModel = computed({
  get: () => props.noteTitle,
  set: (value: string) => emit('update:noteTitle', value),
})

const noteContentModel = computed({
  get: () => props.noteContent,
  set: (value: string) => emit('update:noteContent', value),
})

const selectedNoteTags = computed(() => {
  const selectedIds = new Set(props.selectedNoteTagIds)
  return props.knowledgeBaseTags.filter((tag) => selectedIds.has(tag.id))
})

const previewKnowledgeBaseTags = computed(() => {
  return props.knowledgeBaseTags.slice(0, 4)
})

const hiddenKnowledgeBaseTagCount = computed(() => {
  return Math.max(props.knowledgeBaseTags.length - previewKnowledgeBaseTags.value.length, 0)
})

const noteMetaItems = computed(() => {
  if (!props.currentNote) {
    return []
  }

  return [
    { label: '知识库', value: props.selectedKnowledgeBaseName || '未命名知识库' },
    { label: '出链', value: String(props.currentNote.outgoingLinks?.length || 0) },
    { label: '入链', value: String(props.currentNote.incomingLinks?.length || 0) },
    { label: '标签', value: String(props.selectedNoteTagIds.length || 0) },
    { label: '附件', value: String(props.currentNote.attachments?.length || 0) },
  ]
})

const visibleNoteMetaItems = computed(() => {
  return noteMetaItems.value.slice(0, 3)
})

const workspaceStats = computed(() => {
  return [
    { label: '笔记', value: String(props.noteCount) },
    { label: '模板', value: String(props.noteTemplates.length) },
    { label: '标签', value: String(props.knowledgeBaseTags.length) },
  ]
})

const outlineEmptyMessage = computed(() => {
  if (!props.currentNote) {
    return '打开一篇笔记后，这里会显示文档目录。'
  }

  return '在正文中使用 #、##、### 等 Markdown 标题后，这里会自动生成目录。'
})

const showOutlinePanel = computed(() => Boolean(props.currentNote))
const showKnowledgeBaseManager = computed(() => !props.currentNote && !props.loadingNote && !props.selectedKnowledgeBaseId)
const showKnowledgeBaseWorkspace = computed(() => !props.currentNote && !props.loadingNote && Boolean(props.selectedKnowledgeBaseId))

const outlineTree = computed<OutlineTreeItem[]>(() => {
  const roots: OutlineTreeItem[] = []
  const stack: OutlineTreeItem[] = []

  outlineItems.value.forEach((item) => {
    const node: OutlineTreeItem = {
      ...item,
      children: [],
    }

    while (stack.length && stack[stack.length - 1].level >= node.level) {
      stack.pop()
    }

    if (stack.length) {
      stack[stack.length - 1].children.push(node)
    } else {
      roots.push(node)
    }

    stack.push(node)
  })

  return roots
})

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

function formatDateTime(value?: string | null) {
  if (!value) {
    return '未记录'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return dateTimeFormatter.format(date)
}

function jumpToOutline(id: string) {
  activeOutlineId.value = id
  editorRef.value?.scrollToHeading(id)
}

function submitKnowledgeBaseTag() {
  if (props.submittingTag) {
    return
  }

  const value = knowledgeBaseTagName.value.trim()

  if (!value) {
    knowledgeBaseTagError.value = '请输入标签名称。'
    knowledgeBaseTagInputRef.value?.focus()
    return
  }

  knowledgeBaseTagError.value = ''

  if (props.onCreateKnowledgeBaseTag) {
    void props.onCreateKnowledgeBaseTag(value)
    return
  }

  emit('create-knowledge-base-tag', value)
}

function submitNoteTag() {
  if (props.submittingTag) {
    return
  }

  const value = noteTagName.value.trim()

  if (!value) {
    noteTagError.value = '请输入标签名称。'
    noteTagInputRef.value?.focus()
    return
  }

  noteTagError.value = ''

  if (props.onCreateNoteTag) {
    void props.onCreateNoteTag(value)
    return
  }

  emit('create-note-tag', value)
}

function handleToggleNoteTag(tagId: Id) {
  if (props.onToggleNoteTag) {
    props.onToggleNoteTag(tagId)
    return
  }

  emit('toggle-note-tag', tagId)
}

function handleDeleteTag(payload: TagActionPayload) {
  if (props.onDeleteTag) {
    props.onDeleteTag(payload)
    return
  }

  emit('delete-tag', payload)
}

function clampOutlineWidth(width: number) {
  return Math.min(OUTLINE_MAX_WIDTH, Math.max(OUTLINE_MIN_WIDTH, width))
}

function stopOutlineResize() {
  if (!isResizingOutline.value) {
    return
  }

  isResizingOutline.value = false
  window.removeEventListener('pointermove', handleOutlineResize)
  window.removeEventListener('pointerup', stopOutlineResize)
  window.removeEventListener('pointercancel', stopOutlineResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

function handleOutlineResize(event: PointerEvent) {
  if (!isResizingOutline.value) {
    return
  }

  const delta = resizeStartX - event.clientX
  outlineWidth.value = clampOutlineWidth(resizeStartWidth + delta)
}

function startOutlineResize(event: PointerEvent) {
  if (outlineCollapsed.value || !showOutlinePanel.value) {
    return
  }

  resizeStartX = event.clientX
  resizeStartWidth = outlineWidth.value
  isResizingOutline.value = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('pointermove', handleOutlineResize)
  window.addEventListener('pointerup', stopOutlineResize)
  window.addEventListener('pointercancel', stopOutlineResize)
  event.preventDefault()
}

watch(
  outlineItems,
  (items) => {
    if (!items.length) {
      activeOutlineId.value = null
      return
    }

    if (!items.some((item) => item.id === activeOutlineId.value)) {
      activeOutlineId.value = items[0].id
    }
  },
  { immediate: true },
)

watch(
  () => props.selectedKnowledgeBaseId,
  () => {
    tagDrawerOpen.value = false
    knowledgeBaseTagName.value = ''
    knowledgeBaseTagError.value = ''
  },
)

watch(showKnowledgeBaseWorkspace, (visible) => {
  if (!visible) {
    tagDrawerOpen.value = false
  }
})

watch(
  () => props.currentNote?.id,
  () => {
    tagDrawerOpen.value = false
    noteTagName.value = ''
    noteTagError.value = ''
  },
)

watch(
  () => props.knowledgeBaseTagCreateTick,
  () => {
    knowledgeBaseTagName.value = ''
    knowledgeBaseTagError.value = ''
  },
)

watch(
  () => props.noteTagCreateTick,
  () => {
    noteTagName.value = ''
    noteTagError.value = ''
  },
)

watch(knowledgeBaseTagName, () => {
  if (knowledgeBaseTagError.value) {
    knowledgeBaseTagError.value = ''
  }
})

watch(noteTagName, () => {
  if (noteTagError.value) {
    noteTagError.value = ''
  }
})

onBeforeUnmount(() => {
  if (editorFullscreen.value) {
    emit('editor-fullscreen-change', false)
  }
  stopOutlineResize()
})
</script>

<template>
  <div
    class="workspace-body"
    :style="workspaceBodyStyle"
    :class="{
      'outline-collapsed': outlineCollapsed,
      'outline-hidden': !showOutlinePanel,
      resizing: isResizingOutline,
      'editor-fullscreen-active': editorFullscreen,
    }"
  >
    <section class="main-panel editor-panel">
      <section v-if="showKnowledgeBaseManager" class="knowledge-base-switcher">
        <div class="panel-heading compact">
          <div>
            <p class="eyebrow">主页</p>
            <h3>知识库选择与管理</h3>
            <p class="panel-copy">
              {{
                loadingKnowledgeBases
                  ? '正在加载知识库列表...'
                  : `当前共有 ${knowledgeBaseCount} 个知识库，打开后即可查看资源树并开始记录内容。`
              }}
            </p>
          </div>
          <div class="inline-actions">
            <button type="button" class="soft-button accent" @click="$emit('create-knowledge-base')">新建知识库</button>
          </div>
        </div>

        <div v-if="loadingKnowledgeBases" class="empty-card compact-card">知识库加载中...</div>

        <div v-else-if="knowledgeBases.length" class="knowledge-base-list knowledge-base-management-list">
          <article
            v-for="knowledgeBase in knowledgeBases"
            :key="knowledgeBase.id"
            class="knowledge-base-entry"
            :class="{ active: selectedKnowledgeBaseId === knowledgeBase.id }"
          >
            <button type="button" class="knowledge-base-entry-main" @click="$emit('select-knowledge-base', knowledgeBase.id)">
              <div class="knowledge-base-entry-header">
                <strong>{{ knowledgeBase.name }}</strong>
                <span v-if="selectedKnowledgeBaseId === knowledgeBase.id" class="knowledge-base-entry-state">当前知识库</span>
              </div>
              <p class="knowledge-base-entry-description">{{ knowledgeBase.description || '暂无知识库说明' }}</p>
              <div class="knowledge-base-entry-meta">
                <span>创建于 {{ formatDateTime(knowledgeBase.createdTime) }}</span>
                <span>更新于 {{ formatDateTime(knowledgeBase.updatedTime || knowledgeBase.createdTime) }}</span>
              </div>
            </button>

            <div class="knowledge-base-entry-actions">
              <button type="button" class="mini-button" @click="$emit('edit-knowledge-base', knowledgeBase)">编辑</button>
              <button
                type="button"
                class="mini-button danger"
                @click="$emit('delete-knowledge-base', { id: knowledgeBase.id, name: knowledgeBase.name })"
              >
                删除
              </button>
            </div>
          </article>
        </div>

        <div v-else class="empty-card compact-card">还没有知识库，先新建一个知识库开始整理内容吧。</div>
      </section>

      <div v-if="loadingNote" class="auth-empty">笔记加载中...</div>

      <div v-else-if="currentNote" class="editor-stack">
        <div class="editor-frame">
          <div v-if="showLibraryPanelToggle" class="editor-frame-bookmark-shell">
            <button
            type="button"
            class="editor-frame-bookmark"
            title="展开左侧资源目录"
            @click="$emit('toggle-library-panel')"
          >
            <svg class="editor-frame-bookmark-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M6.25 5.75h11.5A1.25 1.25 0 0 1 19 7v10a1.25 1.25 0 0 1-1.25 1.25H6.25A1.25 1.25 0 0 1 5 17V7a1.25 1.25 0 0 1 1.25-1.25Z"
                fill="none"
                stroke="currentColor"
                stroke-linejoin="round"
                stroke-width="1.6"
              />
              <path d="M9 5.75v12.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.6" />
              <path
                d="m12.25 8.75 2.75 3.25-2.75 3.25"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.8"
              />
            </svg>
            <span>展开目录</span>
          </button>
          </div>

          <div class="editor-frame-header">
            <div class="editor-frame-toolbar">
              <button
                type="button"
                class="soft-button accent note-tag-launcher-button"
                :title="selectedNoteTagIds.length ? `当前已设置 ${selectedNoteTagIds.length} 个标签` : '为当前笔记设置标签'"
                @click="tagDrawerOpen = true"
              >
                <span>设置标签</span>
                <span class="note-tag-launcher-count">{{ selectedNoteTagIds.length }}</span>
              </button>
              <button type="button" class="soft-button accent note-save-button" :disabled="savingNote" @click="$emit('save-note')">
                {{ savingNote ? '保存中...' : noteSaved ? '已保存' : '保存笔记' }}
              </button>
            </div>

            <div class="note-title-row">
              <div class="selected-tag-strip editor-note-tags" :aria-label="selectedNoteTags.length ? '当前笔记标签' : undefined">
                <span v-for="tag in selectedNoteTags" :key="tag.id" class="tag-chip active editor-note-tag"># {{ tag.name }}</span>
              </div>

              <input
                v-model="noteTitleModel"
                class="note-title-input"
                type="text"
                placeholder="请输入笔记标题"
                aria-label="笔记标题"
              />

              <div class="note-title-row-spacer" aria-hidden="true"></div>
            </div>

            <div class="note-stats">
              <span v-for="item in visibleNoteMetaItems" :key="item.label">{{ item.label }}：{{ item.value }}</span>
            </div>

            <section v-if="false" class="note-tag-panel">
              <div class="note-tag-panel-header">
                <div>
                  <p class="eyebrow">笔记标签</p>
                  <h4>从当前知识库标签中选择或新建</h4>
                </div>
                <span class="knowledge-base-entry-state">{{ selectedNoteTagIds.length }} 已选</span>
              </div>

              <div class="tag-creator">
                <input
                  ref="noteTagInputRef"
                  v-model="noteTagName"
                  class="tag-input"
                  :class="{ invalid: Boolean(noteTagError) }"
                  type="text"
                  :disabled="submittingTag"
                  placeholder="输入新标签名，创建后会自动选中"
                  @keydown.enter.stop.prevent="submitNoteTag"
                />
                <button type="button" class="mini-button accent" :disabled="submittingTag" @click.stop.prevent="submitNoteTag">
                  {{ submittingTag ? '创建中...' : '新建并选中' }}
                </button>
              </div>
              <p v-if="noteTagError" class="field-error">{{ noteTagError }}</p>

              <div v-if="selectedNoteTags.length" class="selected-tag-strip">
                <span v-for="tag in selectedNoteTags" :key="tag.id" class="tag-chip active"># {{ tag.name }}</span>
              </div>

              <div v-if="loadingTags" class="empty-card compact-card">标签加载中...</div>

              <div v-else-if="knowledgeBaseTags.length" class="tag-chip-list">
                <button
                  v-for="tag in knowledgeBaseTags"
                  :key="tag.id"
                  type="button"
                  class="tag-chip"
                  :class="{ active: selectedNoteTagIds.includes(tag.id) }"
                  @click.stop="handleToggleNoteTag(tag.id)"
                >
                  # {{ tag.name }}
                </button>
              </div>

              <div v-else class="empty-card compact-card">当前知识库还没有标签，先创建一个标签吧。</div>
            </section>
          </div>

          <div class="editor-frame-body">
            <VditorEditor
              ref="editorRef"
              v-model="noteContentModel"
              :content-theme="editorContentTheme"
              :code-theme="editorCodeTheme"
              @outline-change="outlineItems = $event"
              @fullscreen-change="handleEditorFullscreenChange"
              @theme-change="$emit('editor-theme-change', $event)"
            />
          </div>
        </div>
      </div>

      <div v-else-if="showKnowledgeBaseWorkspace" class="knowledge-base-workbench">
        <section class="workbench-hero">
          <div class="workbench-hero-head">
            <div class="workbench-hero-copy">
            <p class="eyebrow">知识库工作台</p>
            <h3>{{ selectedKnowledgeBaseName || '已打开知识库' }}</h3>
            <p class="panel-copy">
              {{
                selectedKnowledgeBaseDescription ||
                '左侧与中间都提供资源树入口，你可以从任意一处继续浏览、展开目录或新建内容。'
              }}
            </p>
            <p class="workbench-meta">
              最近更新：{{ formatDateTime(selectedKnowledgeBase?.updatedTime || selectedKnowledgeBase?.createdTime) }}
            </p>
            </div>
            <button
              type="button"
              class="soft-button accent workbench-tag-button"
              :title="`当前共有 ${knowledgeBaseTags.length} 个标签`"
              @click="tagDrawerOpen = true"
            >
              标签管理
            </button>
          </div>

          <div class="note-stats workbench-stats">
            <span v-for="item in workspaceStats" :key="item.label">{{ item.label }}：{{ item.value }}</span>
          </div>
        </section>

        <section class="resource-directory-section">
          <div class="panel-heading compact">
            <div>
              <p class="eyebrow">资源目录</p>
              <h3>当前知识库内容</h3>
            </div>
            <div class="inline-actions">
              <button type="button" class="mini-button toolbar-button toolbar-icon-button" title="在根目录新建文件夹" @click="$emit('create-folder', null)">
                <svg class="toolbar-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    d="M4.5 7.5a1.5 1.5 0 0 1 1.5-1.5h4l1.5 2h6a1.5 1.5 0 0 1 1.5 1.5v7.5a1.5 1.5 0 0 1-1.5 1.5H6A1.5 1.5 0 0 1 4.5 17z"
                    fill="none"
                    stroke="currentColor"
                    stroke-linejoin="round"
                    stroke-width="1.7"
                  />
                  <path d="M12 10.5v5M9.5 13h5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.7" />
                </svg>
              </button>
              <button type="button" class="mini-button toolbar-button toolbar-icon-button accent" title="在根目录新建笔记" @click="$emit('create-note', null)">
                <svg class="toolbar-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    d="M7 4.75h7.5L18.25 8.5V18A1.25 1.25 0 0 1 17 19.25H7A1.25 1.25 0 0 1 5.75 18V6A1.25 1.25 0 0 1 7 4.75Z"
                    fill="none"
                    stroke="currentColor"
                    stroke-linejoin="round"
                    stroke-width="1.7"
                  />
                  <path d="M14.5 4.75V8.5h3.75" fill="none" stroke="currentColor" stroke-linejoin="round" stroke-width="1.7" />
                  <path d="M12 10.5v5M9.5 13h5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.7" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="loadingTree" class="empty-card compact-card">资源目录加载中...</div>

          <div v-else-if="treeNodes.length" class="tree-shell-body resource-directory-tree-shell">
            <div class="tree-shell-surface">
              <div class="tree-shell-heading">
                <span>资源树</span>
                <strong>点击节点即可浏览，文件夹支持直接展开或收起</strong>
              </div>

              <div class="tree-list resource-directory-tree">
                <KnowledgeTreeNode
                  v-for="node in treeNodes"
                  :key="`${node.type}-${node.id}`"
                  :node="node"
                  :selected-folder-id="selectedFolderId"
                  :selected-note-id="selectedNoteId"
                  :show-note-meta="true"
                  @select="$emit('select-tree-node', $event)"
                  @create-folder="$emit('create-folder', $event)"
                  @create-note="$emit('create-note', $event)"
                  @edit-folder="$emit('edit-folder', $event)"
                  @delete-folder="$emit('delete-folder', { id: $event.id, name: $event.name })"
                  @delete-note="$emit('delete-note', { id: $event.id, name: $event.name })"
                />
              </div>
            </div>
          </div>

          <div v-else class="empty-card compact-card">当前知识库还没有内容，先在根目录创建一个文件夹或笔记吧。</div>
        </section>

        <section v-if="false" class="tag-management-launcher">
          <div class="tag-management-launcher-head">
            <div class="tag-management-launcher-copy">
              <p class="eyebrow">标签管理</p>
              <h3>通过右侧抽屉整理知识库标签</h3>
              <p class="panel-copy">资源目录继续作为主区域展示，标签统一收进右侧抽屉，页面会更清爽。</p>
            </div>

            <div class="inline-actions">
              <span class="knowledge-base-entry-state">{{ knowledgeBaseTags.length }} 个标签</span>
              <button type="button" class="soft-button accent" @click="tagDrawerOpen = true">打开标签抽屉</button>
            </div>
          </div>

          <div v-if="loadingTags" class="empty-card compact-card">标签加载中...</div>

          <div v-else-if="previewKnowledgeBaseTags.length" class="tag-management-preview">
            <span v-for="tag in previewKnowledgeBaseTags" :key="tag.id" class="tag-chip"># {{ tag.name }}</span>
            <span v-if="hiddenKnowledgeBaseTagCount" class="tag-preview-more">+{{ hiddenKnowledgeBaseTagCount }} 个更多标签</span>
          </div>

          <div v-else class="empty-card compact-card">当前知识库还没有标签，点击右上角按钮后可在抽屉里创建。</div>
        </section>

        <div v-if="tagDrawerOpen" class="tag-management-drawer-layer">
          <button type="button" class="tag-management-backdrop" aria-label="关闭标签抽屉" @click="tagDrawerOpen = false"></button>
          <aside class="tag-management-section tag-management-drawer">
          <template v-if="currentNote">
            <div class="panel-heading compact">
              <div>
                <p class="eyebrow">标签设置</p>
                <h3>当前笔记标签</h3>
                <p class="panel-copy">从当前知识库标签中选择，也可以直接新建并自动选中。</p>
              </div>
              <span class="knowledge-base-entry-state">{{ selectedNoteTagIds.length }} 已选</span>
            </div>

            <div class="tag-management-drawer-actions">
              <button type="button" class="mini-button" @click="tagDrawerOpen = false">关闭抽屉</button>
            </div>

            <div class="tag-creator">
              <input
                ref="noteTagInputRef"
                v-model="noteTagName"
                class="tag-input"
                :class="{ invalid: Boolean(noteTagError) }"
                type="text"
                :disabled="submittingTag"
                placeholder="输入新标签名，创建后会自动选中"
                @keydown.enter.stop.prevent="submitNoteTag"
              />
              <button type="button" class="soft-button accent" :disabled="submittingTag" @click.stop.prevent="submitNoteTag">
                {{ submittingTag ? '创建中...' : '新建并选中' }}
              </button>
            </div>
            <p v-if="noteTagError" class="field-error">{{ noteTagError }}</p>

            <section class="note-tag-drawer-summary">
              <p class="note-tag-drawer-summary-label">已选标签</p>
              <div v-if="selectedNoteTags.length" class="selected-tag-strip">
                <span v-for="tag in selectedNoteTags" :key="tag.id" class="tag-chip active"># {{ tag.name }}</span>
              </div>
              <p v-else class="panel-copy">当前笔记还没有设置标签。</p>
            </section>

            <div v-if="loadingTags" class="empty-card compact-card">标签加载中...</div>

            <div v-else-if="knowledgeBaseTags.length" class="tag-management-list">
              <article
                v-for="tag in knowledgeBaseTags"
                :key="tag.id"
                class="tag-management-item"
                :class="{ active: selectedNoteTagIds.includes(tag.id) }"
              >
                <div class="tag-management-copy">
                  <strong># {{ tag.name }}</strong>
                  <span>{{ selectedNoteTagIds.includes(tag.id) ? '当前笔记已使用这个标签。' : '点击右侧按钮即可添加到当前笔记。' }}</span>
                </div>
                <button
                  type="button"
                  class="mini-button"
                  :class="{ accent: !selectedNoteTagIds.includes(tag.id) }"
                  @click.stop="handleToggleNoteTag(tag.id)"
                >
                  {{ selectedNoteTagIds.includes(tag.id) ? '移除' : '添加' }}
                </button>
              </article>
            </div>

            <div v-else class="empty-card compact-card">当前知识库还没有标签，先创建一个标签吧。</div>
          </template>
          <template v-else>
          <div class="panel-heading compact">
            <div>
              <p class="eyebrow">标签管理</p>
              <h3>当前知识库标签</h3>
              <p class="panel-copy">标签属于知识库，可在知识库内的任意笔记中复用。</p>
            </div>
            <span class="knowledge-base-entry-state">{{ knowledgeBaseTags.length }} 个标签</span>
          </div>

          <div class="tag-management-drawer-actions">
            <button type="button" class="mini-button" @click="tagDrawerOpen = false">关闭抽屉</button>
          </div>

          <div class="tag-creator">
            <input
              ref="knowledgeBaseTagInputRef"
              v-model="knowledgeBaseTagName"
              class="tag-input"
              :class="{ invalid: Boolean(knowledgeBaseTagError) }"
              type="text"
              :disabled="submittingTag"
              placeholder="例如：重点、待复习、架构"
              @keydown.enter.stop.prevent="submitKnowledgeBaseTag"
            />
            <button type="button" class="soft-button accent" :disabled="submittingTag" @click.stop.prevent="submitKnowledgeBaseTag">
              {{ submittingTag ? '创建中...' : '创建标签' }}
            </button>
          </div>
          <p v-if="knowledgeBaseTagError" class="field-error">{{ knowledgeBaseTagError }}</p>

          <div v-if="loadingTags" class="empty-card compact-card">标签加载中...</div>

          <div v-else-if="knowledgeBaseTags.length" class="tag-management-list">
            <article v-for="tag in knowledgeBaseTags" :key="tag.id" class="tag-management-item">
              <div class="tag-management-copy">
                <strong># {{ tag.name }}</strong>
                <span>创建于 {{ formatDateTime(tag.createdTime) }}</span>
              </div>
              <button type="button" class="mini-button danger" @click.stop="handleDeleteTag({ id: tag.id, name: tag.name })">
                删除
              </button>
            </article>
          </div>

          <div v-else class="empty-card compact-card">当前知识库还没有标签，创建后就能在笔记里直接复用。</div>
          </template>
          </aside>
        </div>
      </div>

      <div v-if="currentNote && tagDrawerOpen" class="tag-management-drawer-layer">
        <button type="button" class="tag-management-backdrop" aria-label="关闭标签抽屉" @click="tagDrawerOpen = false"></button>
        <aside class="tag-management-section tag-management-drawer">
          <div class="panel-heading compact">
            <div>
              <p class="eyebrow">标签设置</p>
              <h3>当前笔记标签</h3>
              <p class="panel-copy">从当前知识库标签中选择，也可以直接新建并自动选中。</p>
            </div>
            <span class="knowledge-base-entry-state">{{ selectedNoteTagIds.length }} 已选</span>
          </div>

          <div class="tag-management-drawer-actions">
            <button type="button" class="mini-button" @click="tagDrawerOpen = false">关闭抽屉</button>
          </div>

          <div class="tag-creator">
            <input
              ref="noteTagInputRef"
              v-model="noteTagName"
              class="tag-input"
              :class="{ invalid: Boolean(noteTagError) }"
              type="text"
              :disabled="submittingTag"
              placeholder="输入新标签名，创建后会自动选中"
              @keydown.enter.stop.prevent="submitNoteTag"
            />
            <button type="button" class="soft-button accent" :disabled="submittingTag" @click.stop.prevent="submitNoteTag">
              {{ submittingTag ? '创建中...' : '新建并选中' }}
            </button>
          </div>
          <p v-if="noteTagError" class="field-error">{{ noteTagError }}</p>

          <section class="note-tag-drawer-summary">
            <p class="note-tag-drawer-summary-label">已选标签</p>
            <div v-if="selectedNoteTags.length" class="selected-tag-strip">
              <span v-for="tag in selectedNoteTags" :key="tag.id" class="tag-chip active"># {{ tag.name }}</span>
            </div>
            <p v-else class="panel-copy">当前笔记还没有设置标签。</p>
          </section>

          <div v-if="loadingTags" class="empty-card compact-card">标签加载中...</div>

          <div v-else-if="knowledgeBaseTags.length" class="tag-management-list">
            <article
              v-for="tag in knowledgeBaseTags"
              :key="tag.id"
              class="tag-management-item"
              :class="{ active: selectedNoteTagIds.includes(tag.id) }"
            >
              <div class="tag-management-copy">
                <strong># {{ tag.name }}</strong>
                <span>{{ selectedNoteTagIds.includes(tag.id) ? '当前笔记已使用这个标签。' : '点击右侧按钮即可添加到当前笔记。' }}</span>
              </div>
              <button
                type="button"
                class="mini-button"
                :class="{ accent: !selectedNoteTagIds.includes(tag.id) }"
                @click.stop="handleToggleNoteTag(tag.id)"
              >
                {{ selectedNoteTagIds.includes(tag.id) ? '移除' : '添加' }}
              </button>
            </article>
          </div>

          <div v-else class="empty-card compact-card">当前知识库还没有标签，先创建一个标签吧。</div>
        </aside>
      </div>
    </section>

    <aside v-if="showOutlinePanel" class="outline-panel" :class="{ collapsed: outlineCollapsed, resizing: isResizingOutline }">
      <div
        v-if="!outlineCollapsed"
        class="outline-resize-handle"
        role="separator"
        aria-orientation="vertical"
        aria-label="拖动调整目录栏宽度"
        @pointerdown="startOutlineResize"
      ></div>

      <div class="panel-heading compact">
        <div>
          <p class="eyebrow">目录</p>
          <h3>{{ outlineCollapsed ? '已收起' : '标题导航' }}</h3>
        </div>
        <button type="button" class="mini-button" @click="outlineCollapsed = !outlineCollapsed">
          {{ outlineCollapsed ? '展开' : '收起' }}
        </button>
      </div>

      <div v-if="!outlineCollapsed" class="outline-panel-body">
        <div v-if="!outlineItems.length" class="empty-card compact-card outline-empty">
          {{ outlineEmptyMessage }}
        </div>

        <div v-else class="outline-tree-list">
          <OutlineTreeNode
            v-for="item in outlineTree"
            :key="item.id"
            :item="item"
            :active-id="activeOutlineId"
            @select="jumpToOutline"
          />
        </div>
      </div>
    </aside>
  </div>
</template>

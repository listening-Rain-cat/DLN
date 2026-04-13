import type { Ref } from 'vue'
import { sortTags } from './shared'
import type { Id, KnowledgeBase, KnowledgeGraph, NoteDetail, NoteTemplate, TagItem, TreeNode, ViewMode } from './shared'

interface LoadingState {
  knowledgeBases: boolean
  tags: boolean
  templates: boolean
  graph: boolean
  tree: boolean
}

interface UseKnowledgeBaseDataOptions {
  request: <T>(path: string, init?: RequestInit, withAuth?: boolean) => Promise<T>
  loading: LoadingState
  viewMode: Ref<ViewMode>
  knowledgeBases: Ref<KnowledgeBase[]>
  knowledgeBaseTags: Ref<TagItem[]>
  noteTemplates: Ref<NoteTemplate[]>
  treeNodes: Ref<TreeNode[]>
  knowledgeGraph: Ref<KnowledgeGraph | null>
  selectedKnowledgeBaseId: Ref<Id | null>
  selectedFolderId: Ref<Id | null>
  selectedNoteTagIds: Ref<Id[]>
  currentNote: Ref<NoteDetail | null>
  clearCurrentNote: () => void
}

export function useKnowledgeBaseData(options: UseKnowledgeBaseDataOptions) {
  async function loadKnowledgeBaseTree(knowledgeBaseId: Id) {
    options.viewMode.value = 'home'
    options.selectedKnowledgeBaseId.value = knowledgeBaseId

    if (options.currentNote.value?.knowledgeBaseId !== knowledgeBaseId) {
      options.selectedFolderId.value = null
      options.clearCurrentNote()
    }

    options.loading.tree = true

    try {
      options.treeNodes.value = await options.request<TreeNode[]>(`/knowledgeBases/${knowledgeBaseId}/tree`)
    } finally {
      options.loading.tree = false
    }

    if (options.currentNote.value?.knowledgeBaseId === knowledgeBaseId) {
      options.selectedFolderId.value = options.currentNote.value.folderId
    }
  }

  async function loadKnowledgeBaseTags(knowledgeBaseId: Id) {
    options.loading.tags = true

    try {
      const data = sortTags(await options.request<TagItem[]>(`/knowledgeBases/${knowledgeBaseId}/tags`))
      options.knowledgeBaseTags.value = data

      if (options.currentNote.value?.knowledgeBaseId === knowledgeBaseId) {
        const validTagIds = new Set(data.map((tag) => tag.id))
        options.selectedNoteTagIds.value = options.selectedNoteTagIds.value.filter((tagId) => validTagIds.has(tagId))
      }
    } finally {
      options.loading.tags = false
    }
  }

  async function loadNoteTemplates() {
    options.loading.templates = true

    try {
      options.noteTemplates.value = await options.request<NoteTemplate[]>('/templates')
    } finally {
      options.loading.templates = false
    }
  }

  async function loadKnowledgeBaseContext(knowledgeBaseId: Id) {
    await Promise.all([loadKnowledgeBaseTree(knowledgeBaseId), loadKnowledgeBaseTags(knowledgeBaseId)])
  }

  async function loadKnowledgeGraph(knowledgeBaseId: Id) {
    options.loading.graph = true

    try {
      options.knowledgeGraph.value = await options.request<KnowledgeGraph>(`/knowledgeBases/${knowledgeBaseId}/graph`)
    } finally {
      options.loading.graph = false
    }
  }

  async function fetchKnowledgeBases() {
    options.loading.knowledgeBases = true

    try {
      const data = await options.request<KnowledgeBase[]>('/knowledgeBases')
      options.knowledgeBases.value = data

      if (!data.length) {
        options.selectedKnowledgeBaseId.value = null
        options.knowledgeBaseTags.value = []
        options.knowledgeGraph.value = null
        options.treeNodes.value = []
        options.selectedFolderId.value = null
        options.clearCurrentNote()
        return
      }

      const nextId = data.some((item) => item.id === options.selectedKnowledgeBaseId.value)
        ? options.selectedKnowledgeBaseId.value
        : null

      if (nextId) {
        await loadKnowledgeBaseContext(nextId)
        return
      }

      options.selectedKnowledgeBaseId.value = null
      options.knowledgeBaseTags.value = []
      options.knowledgeGraph.value = null
      options.treeNodes.value = []
      options.selectedFolderId.value = null
      options.clearCurrentNote()
    } finally {
      options.loading.knowledgeBases = false
    }
  }

  async function refreshKnowledgeBasesSnapshot() {
    const data = await options.request<KnowledgeBase[]>('/knowledgeBases')
    options.knowledgeBases.value = data

    if (!data.length) {
      options.selectedKnowledgeBaseId.value = null
      options.knowledgeBaseTags.value = []
      options.knowledgeGraph.value = null
      options.treeNodes.value = []
      options.selectedFolderId.value = null
      options.clearCurrentNote()
      return
    }

    if (
      options.selectedKnowledgeBaseId.value &&
      !data.some((item) => item.id === options.selectedKnowledgeBaseId.value)
    ) {
      options.selectedKnowledgeBaseId.value = null
      options.knowledgeBaseTags.value = []
      options.knowledgeGraph.value = null
      options.treeNodes.value = []
      options.selectedFolderId.value = null
      options.clearCurrentNote()
    }
  }

  return {
    fetchKnowledgeBases,
    loadKnowledgeBaseContext,
    loadKnowledgeBaseTags,
    loadKnowledgeBaseTree,
    loadKnowledgeGraph,
    loadNoteTemplates,
    refreshKnowledgeBasesSnapshot,
  }
}

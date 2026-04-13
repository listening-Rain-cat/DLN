import { computed, onBeforeUnmount, ref, watch } from 'vue'
import type { Ref } from 'vue'
import {
  areSameTagSelection,
  createLocalUpdatedTime,
  extractTagIds,
  sortTags,
  sortTreeNodes,
} from './shared'
import type { Id, NoteDetail, TagItem, TreeNode } from './shared'

interface LoadingState {
  note: boolean
  save: boolean
}

interface UseNoteAutosaveOptions {
  request: <T>(path: string, init?: RequestInit, withAuth?: boolean) => Promise<T>
  showNotice: (text: string, type?: 'success' | 'error') => void
  refreshKnowledgeBasesSnapshot: () => Promise<void>
  currentNote: Ref<NoteDetail | null>
  selectedNoteId: Ref<Id | null>
  selectedNoteTagIds: Ref<Id[]>
  noteTitle: Ref<string>
  noteContent: Ref<string>
  treeNodes: Ref<TreeNode[]>
  loading: LoadingState
}

export function useNoteAutosave(options: UseNoteAutosaveOptions) {
  const autoSavingNoteContent = ref(false)
  const autoSavingNoteTitle = ref(false)
  const autoSavingNoteTags = ref(false)
  const autoSaveError = ref('')

  const NOTE_TITLE_AUTOSAVE_DELAY_MS = 900
  const NOTE_CONTENT_AUTOSAVE_DELAY_MS = 1200
  const NOTE_TAGS_AUTOSAVE_DELAY_MS = 400

  let noteTitleAutoSaveTimer: number | null = null
  let noteTitleAutoSaveRequestId = 0
  let noteContentAutoSaveTimer: number | null = null
  let noteContentAutoSaveRequestId = 0
  let noteTagsAutoSaveTimer: number | null = null
  let noteTagsAutoSaveRequestId = 0
  let noteContentAutoSaveErrorShown = false
  let pendingNoteTitleAutoSave: { noteId: Id; title: string } | null = null
  let pendingNoteContentAutoSave: { noteId: Id; markdownContent: string } | null = null
  let pendingNoteTagsAutoSave: { noteId: Id; tagIds: Id[] } | null = null

  const noteSaved = computed(() => {
    if (!options.currentNote.value) {
      return false
    }

    return (
      options.noteTitle.value.trim() === options.currentNote.value.title &&
      options.noteContent.value === (options.currentNote.value.markdownContent || '') &&
      areSameTagSelection(options.selectedNoteTagIds.value, extractTagIds(options.currentNote.value.tags))
    )
  })

  const autoSavingNote = computed(() => {
    return autoSavingNoteContent.value || autoSavingNoteTitle.value || autoSavingNoteTags.value
  })

  function clearNoteTitleAutoSaveTimer() {
    if (noteTitleAutoSaveTimer !== null) {
      window.clearTimeout(noteTitleAutoSaveTimer)
      noteTitleAutoSaveTimer = null
    }

    pendingNoteTitleAutoSave = null
  }

  function clearNoteContentAutoSaveTimer() {
    if (noteContentAutoSaveTimer !== null) {
      window.clearTimeout(noteContentAutoSaveTimer)
      noteContentAutoSaveTimer = null
    }

    pendingNoteContentAutoSave = null
  }

  function clearNoteTagsAutoSaveTimer() {
    if (noteTagsAutoSaveTimer !== null) {
      window.clearTimeout(noteTagsAutoSaveTimer)
      noteTagsAutoSaveTimer = null
    }

    pendingNoteTagsAutoSave = null
  }

  function flushPendingNoteTitleAutoSave() {
    if (noteTitleAutoSaveTimer === null || !pendingNoteTitleAutoSave) {
      return
    }

    const pendingSave = pendingNoteTitleAutoSave
    clearNoteTitleAutoSaveTimer()
    void autoSaveNoteTitle(pendingSave.noteId, pendingSave.title)
  }

  function flushPendingNoteContentAutoSave() {
    if (noteContentAutoSaveTimer === null || !pendingNoteContentAutoSave) {
      return
    }

    const pendingSave = pendingNoteContentAutoSave
    clearNoteContentAutoSaveTimer()
    void autoSaveNoteContent(pendingSave.noteId, pendingSave.markdownContent)
  }

  function flushPendingNoteTagsAutoSave() {
    if (noteTagsAutoSaveTimer === null || !pendingNoteTagsAutoSave) {
      return
    }

    const pendingSave = pendingNoteTagsAutoSave
    clearNoteTagsAutoSaveTimer()
    void autoSaveNoteTags(pendingSave.noteId, pendingSave.tagIds)
  }

  function resetNoteAutoSaveState(config: { clearTimers?: boolean } = {}) {
    if (config.clearTimers ?? true) {
      clearNoteTitleAutoSaveTimer()
      clearNoteContentAutoSaveTimer()
      clearNoteTagsAutoSaveTimer()
    }
    autoSavingNoteTitle.value = false
    autoSavingNoteContent.value = false
    autoSavingNoteTags.value = false
    autoSaveError.value = ''
    noteContentAutoSaveErrorShown = false
  }

  function scheduleNoteTitleAutoSave(noteId: Id, title: string) {
    clearNoteTitleAutoSaveTimer()
    pendingNoteTitleAutoSave = { noteId, title }

    noteTitleAutoSaveTimer = window.setTimeout(() => {
      const pendingSave = pendingNoteTitleAutoSave
      noteTitleAutoSaveTimer = null
      pendingNoteTitleAutoSave = null
      if (pendingSave) {
        void autoSaveNoteTitle(pendingSave.noteId, pendingSave.title)
      }
    }, NOTE_TITLE_AUTOSAVE_DELAY_MS)
  }

  function scheduleNoteContentAutoSave(noteId: Id, markdownContent: string) {
    clearNoteContentAutoSaveTimer()
    pendingNoteContentAutoSave = { noteId, markdownContent }

    noteContentAutoSaveTimer = window.setTimeout(() => {
      const pendingSave = pendingNoteContentAutoSave
      noteContentAutoSaveTimer = null
      pendingNoteContentAutoSave = null
      if (pendingSave) {
        void autoSaveNoteContent(pendingSave.noteId, pendingSave.markdownContent)
      }
    }, NOTE_CONTENT_AUTOSAVE_DELAY_MS)
  }

  function scheduleNoteTagsAutoSave(noteId: Id, tagIds: Id[]) {
    clearNoteTagsAutoSaveTimer()
    pendingNoteTagsAutoSave = {
      noteId,
      tagIds: [...tagIds],
    }

    noteTagsAutoSaveTimer = window.setTimeout(() => {
      const pendingSave = pendingNoteTagsAutoSave
      noteTagsAutoSaveTimer = null
      pendingNoteTagsAutoSave = null
      if (pendingSave) {
        void autoSaveNoteTags(pendingSave.noteId, pendingSave.tagIds)
      }
    }, NOTE_TAGS_AUTOSAVE_DELAY_MS)
  }

  function markAutoSaveFailure(message: string, noticeText: string) {
    autoSaveError.value = message
    if (!noteContentAutoSaveErrorShown) {
      options.showNotice(noticeText, 'error')
      noteContentAutoSaveErrorShown = true
    }
  }

  function getCurrentNoteTagIds() {
    return extractTagIds(options.currentNote.value?.tags)
  }

  function syncCurrentNoteSnapshot(patch: Partial<NoteDetail>) {
    if (!options.currentNote.value) {
      return
    }

    options.currentNote.value = {
      ...options.currentNote.value,
      ...patch,
    }
  }

  function syncCurrentNoteTreeNode(patch: Partial<TreeNode>, config: { sort?: boolean } = {}) {
    if (!options.selectedNoteId.value) {
      return
    }

    const patchTreeNodes = (nodes: TreeNode[]): TreeNode[] => {
      return nodes.map((node) => {
        if (node.type === 'note' && node.id === options.selectedNoteId.value) {
          return {
            ...node,
            ...patch,
          }
        }

        if (!node.children?.length) {
          return node
        }

        return {
          ...node,
          children: patchTreeNodes(node.children),
        }
      })
    }

    const patchedNodes = patchTreeNodes(options.treeNodes.value)
    options.treeNodes.value = config.sort ? sortTreeNodes(patchedNodes) : patchedNodes
  }

  async function autoSaveNoteTitle(
    noteId: Id,
    title: string,
    config: { force?: boolean; showFailureNotice?: boolean } = {},
  ) {
    const trimmedTitle = title.trim()
    const force = config.force ?? false
    const showFailureNotice = config.showFailureNotice ?? true

    if (!noteId || (options.loading.save && !force)) {
      if (options.currentNote.value?.id === noteId && trimmedTitle && trimmedTitle !== options.currentNote.value.title) {
        scheduleNoteTitleAutoSave(noteId, options.noteTitle.value)
      }
      return false
    }

    const requestId = ++noteTitleAutoSaveRequestId
    autoSavingNoteTitle.value = true
    autoSaveError.value = ''

    try {
      await options.request<void>(`/notes/${noteId}/title/autosave`, {
        method: 'PUT',
        body: JSON.stringify({
          title: trimmedTitle,
        }),
      })

      if (options.currentNote.value?.id === noteId && options.noteTitle.value === title) {
        const updatedTime = createLocalUpdatedTime()
        options.noteTitle.value = trimmedTitle
        syncCurrentNoteSnapshot({
          title: trimmedTitle,
          updatedTime,
        })
        syncCurrentNoteTreeNode(
          {
            name: trimmedTitle,
            updatedTime,
          },
          { sort: true },
        )
      }

      noteContentAutoSaveErrorShown = false
      return true
    } catch (error) {
      if (options.currentNote.value?.id === noteId && options.noteTitle.value === title) {
        autoSaveError.value = (error as Error).message
        if (showFailureNotice) {
          markAutoSaveFailure((error as Error).message, '笔记标题自动保存失败，请手动保存。')
        }
      }
      return false
    } finally {
      if (noteTitleAutoSaveRequestId === requestId) {
        autoSavingNoteTitle.value = false
      }

      if (options.currentNote.value?.id === noteId) {
        const latestTitle = options.noteTitle.value.trim()
        if (latestTitle && latestTitle !== options.currentNote.value.title) {
          scheduleNoteTitleAutoSave(noteId, options.noteTitle.value)
        }
      }
    }
  }

  async function autoSaveNoteContent(
    noteId: Id,
    markdownContent: string,
    config: { force?: boolean; showFailureNotice?: boolean } = {},
  ) {
    const force = config.force ?? false
    const showFailureNotice = config.showFailureNotice ?? true

    if (!noteId || (options.loading.save && !force)) {
      if (
        options.currentNote.value?.id === noteId &&
        options.noteContent.value !== (options.currentNote.value.markdownContent || '')
      ) {
        scheduleNoteContentAutoSave(noteId, options.noteContent.value)
      }
      return false
    }

    const requestId = ++noteContentAutoSaveRequestId
    autoSavingNoteContent.value = true
    autoSaveError.value = ''

    try {
      await options.request<void>(`/notes/${noteId}/content/autosave`, {
        method: 'PUT',
        body: JSON.stringify({
          markdownContent,
        }),
      })

      if (options.currentNote.value?.id === noteId && options.noteContent.value === markdownContent) {
        const updatedTime = createLocalUpdatedTime()
        syncCurrentNoteSnapshot({
          markdownContent,
          updatedTime,
        })
        syncCurrentNoteTreeNode({
          updatedTime,
        })
      }

      noteContentAutoSaveErrorShown = false
      return true
    } catch (error) {
      if (options.currentNote.value?.id === noteId && options.noteContent.value === markdownContent) {
        autoSaveError.value = (error as Error).message
        if (showFailureNotice) {
          markAutoSaveFailure((error as Error).message, '笔记正文自动保存失败，请手动保存。')
        }
      }
      return false
    } finally {
      if (noteContentAutoSaveRequestId === requestId) {
        autoSavingNoteContent.value = false
      }

      if (
        options.currentNote.value?.id === noteId &&
        options.noteContent.value !== (options.currentNote.value.markdownContent || '')
      ) {
        scheduleNoteContentAutoSave(noteId, options.noteContent.value)
      }
    }
  }

  async function autoSaveNoteTags(
    noteId: Id,
    tagIds: Id[],
    config: { force?: boolean; showFailureNotice?: boolean } = {},
  ) {
    const force = config.force ?? false
    const showFailureNotice = config.showFailureNotice ?? true

    if (!noteId || (options.loading.save && !force)) {
      if (
        options.currentNote.value?.id === noteId &&
        !areSameTagSelection(options.selectedNoteTagIds.value, getCurrentNoteTagIds())
      ) {
        scheduleNoteTagsAutoSave(noteId, options.selectedNoteTagIds.value)
      }
      return false
    }

    const requestId = ++noteTagsAutoSaveRequestId
    autoSavingNoteTags.value = true
    autoSaveError.value = ''

    try {
      const updatedTags = sortTags(
        await options.request<TagItem[]>(`/notes/${noteId}/tags`, {
          method: 'PUT',
          body: JSON.stringify({
            tagIds,
          }),
        }),
      )

      if (options.currentNote.value?.id === noteId && areSameTagSelection(options.selectedNoteTagIds.value, tagIds)) {
        const updatedTime = createLocalUpdatedTime()
        const updatedTagIds = extractTagIds(updatedTags)
        syncCurrentNoteSnapshot({
          tags: updatedTags,
          updatedTime,
        })
        options.selectedNoteTagIds.value = updatedTagIds
        syncCurrentNoteTreeNode({
          tags: updatedTags,
          updatedTime,
        })
      }

      noteContentAutoSaveErrorShown = false
      return true
    } catch (error) {
      if (options.currentNote.value?.id === noteId && areSameTagSelection(options.selectedNoteTagIds.value, tagIds)) {
        autoSaveError.value = (error as Error).message
        if (showFailureNotice) {
          markAutoSaveFailure((error as Error).message, '笔记标签自动保存失败，请手动保存。')
        }
      }
      return false
    } finally {
      if (noteTagsAutoSaveRequestId === requestId) {
        autoSavingNoteTags.value = false
      }

      if (
        options.currentNote.value?.id === noteId &&
        !areSameTagSelection(options.selectedNoteTagIds.value, getCurrentNoteTagIds())
      ) {
        scheduleNoteTagsAutoSave(noteId, options.selectedNoteTagIds.value)
      }
    }
  }

  async function saveNote() {
    if (!options.currentNote.value) {
      return
    }

    try {
      if (!options.noteTitle.value.trim()) {
        throw new Error('笔记标题不能为空。')
      }

      const noteId = options.currentNote.value.id
      const titleChanged = options.noteTitle.value.trim() !== options.currentNote.value.title
      const contentChanged = options.noteContent.value !== (options.currentNote.value.markdownContent || '')
      const tagsChanged = !areSameTagSelection(options.selectedNoteTagIds.value, getCurrentNoteTagIds())

      clearNoteTitleAutoSaveTimer()
      clearNoteContentAutoSaveTimer()
      clearNoteTagsAutoSaveTimer()
      options.loading.save = true

      const saveSteps: Array<Promise<boolean>> = []

      if (titleChanged) {
        saveSteps.push(
          autoSaveNoteTitle(noteId, options.noteTitle.value, {
            force: true,
            showFailureNotice: false,
          }),
        )
      }

      if (contentChanged) {
        saveSteps.push(
          autoSaveNoteContent(noteId, options.noteContent.value, {
            force: true,
            showFailureNotice: false,
          }),
        )
      }

      if (tagsChanged) {
        saveSteps.push(
          autoSaveNoteTags(noteId, options.selectedNoteTagIds.value, {
            force: true,
            showFailureNotice: false,
          }),
        )
      }

      const saveResults = await Promise.all(saveSteps)
      if (saveResults.some((result) => !result)) {
        throw new Error(autoSaveError.value || '保存笔记失败。')
      }

      if (saveSteps.length) {
        await options.refreshKnowledgeBasesSnapshot()
      }

      autoSaveError.value = ''
      noteContentAutoSaveErrorShown = false

      options.showNotice('笔记已保存。')
    } catch (error) {
      options.showNotice((error as Error).message, 'error')
    } finally {
      options.loading.save = false
      if (options.currentNote.value?.id) {
        const latestTitle = options.noteTitle.value.trim()
        if (latestTitle && latestTitle !== options.currentNote.value.title) {
          scheduleNoteTitleAutoSave(options.currentNote.value.id, options.noteTitle.value)
        }
      }
      if (
        options.currentNote.value?.id &&
        options.noteContent.value !== (options.currentNote.value.markdownContent || '')
      ) {
        scheduleNoteContentAutoSave(options.currentNote.value.id, options.noteContent.value)
      }
      if (
        options.currentNote.value?.id &&
        !areSameTagSelection(options.selectedNoteTagIds.value, getCurrentNoteTagIds())
      ) {
        scheduleNoteTagsAutoSave(options.currentNote.value.id, options.selectedNoteTagIds.value)
      }
    }
  }

  function clearCurrentNote() {
    flushPendingNoteTitleAutoSave()
    flushPendingNoteContentAutoSave()
    flushPendingNoteTagsAutoSave()
    resetNoteAutoSaveState({ clearTimers: false })
    options.currentNote.value = null
    options.selectedNoteId.value = null
    options.selectedNoteTagIds.value = []
    options.noteTitle.value = ''
    options.noteContent.value = ''
  }

  watch(
    () => options.currentNote.value?.id ?? null,
    () => {
      flushPendingNoteTitleAutoSave()
      flushPendingNoteContentAutoSave()
      flushPendingNoteTagsAutoSave()
      resetNoteAutoSaveState({ clearTimers: false })
    },
  )

  watch(options.noteTitle, (value) => {
    if (!options.currentNote.value || options.loading.note) {
      return
    }

    const normalizedTitle = value.trim()

    if (!normalizedTitle) {
      clearNoteTitleAutoSaveTimer()
      autoSaveError.value = ''
      return
    }

    if (normalizedTitle === options.currentNote.value.title) {
      clearNoteTitleAutoSaveTimer()
      autoSaveError.value = ''
      noteContentAutoSaveErrorShown = false

      if (value !== options.currentNote.value.title) {
        options.noteTitle.value = options.currentNote.value.title
      }
      return
    }

    autoSaveError.value = ''
    scheduleNoteTitleAutoSave(options.currentNote.value.id, value)
  })

  watch(options.noteContent, (value) => {
    if (!options.currentNote.value || options.loading.note) {
      return
    }

    if (value === (options.currentNote.value.markdownContent || '')) {
      clearNoteContentAutoSaveTimer()
      autoSaveError.value = ''
      noteContentAutoSaveErrorShown = false
      return
    }

    autoSaveError.value = ''
    scheduleNoteContentAutoSave(options.currentNote.value.id, value)
  })

  watch(
    options.selectedNoteTagIds,
    (value) => {
      if (!options.currentNote.value || options.loading.note) {
        return
      }

      if (areSameTagSelection(value, getCurrentNoteTagIds())) {
        clearNoteTagsAutoSaveTimer()
        autoSaveError.value = ''
        noteContentAutoSaveErrorShown = false
        return
      }

      autoSaveError.value = ''
      scheduleNoteTagsAutoSave(options.currentNote.value.id, value)
    },
    { deep: true },
  )

  onBeforeUnmount(() => {
    resetNoteAutoSaveState({ clearTimers: true })
  })

  return {
    autoSaveError,
    autoSavingNote,
    clearCurrentNote,
    noteSaved,
    saveNote,
  }
}

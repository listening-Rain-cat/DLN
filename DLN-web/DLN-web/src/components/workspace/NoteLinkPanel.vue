<script setup lang="ts">
import { computed } from 'vue'
import type { Id, NoteDetail, TreeNode } from '../../app/shared'

interface ExistingLinkItem {
  id: string
  noteId: Id | null
  title: string
  isBroken: boolean
}

interface PotentialLinkItem {
  noteId: Id
  title: string
  occurrences: number
  excerptPrefix: string
  excerptSuffix: string
}

const props = defineProps<{
  currentNote: NoteDetail | null
  noteContent: string
  treeNodes: TreeNode[]
}>()

const emit = defineEmits<{
  (e: 'open-note', noteId: Id): void
}>()

function normalizeTitleKey(value?: string | null) {
  return (value || '').trim().toLocaleLowerCase('zh-CN')
}

function flattenNoteNodes(nodes: TreeNode[]) {
  const queue = [...nodes]
  const notes: TreeNode[] = []

  while (queue.length) {
    const node = queue.shift()
    if (!node) {
      continue
    }

    if (node.type === 'note') {
      notes.push(node)
    }

    if (node.children?.length) {
      queue.push(...node.children)
    }
  }

  return notes
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function extractWikiLinkRanges(content: string) {
  const ranges: Array<{ start: number; end: number; title: string }> = []
  const pattern = /\[\[([^\]|]+)(?:\|[^\]]+)?\]\]/g

  for (const match of content.matchAll(pattern)) {
    const start = match.index ?? -1
    if (start < 0) {
      continue
    }

    ranges.push({
      start,
      end: start + match[0].length,
      title: (match[1] || '').trim(),
    })
  }

  return ranges
}

function isRangeOverlapping(start: number, end: number, ranges: Array<{ start: number; end: number }>) {
  return ranges.some((range) => start < range.end && end > range.start)
}

function isAsciiWord(value: string) {
  return /^[a-z0-9_-]+$/i.test(value)
}

function isWordBoundaryChar(value: string) {
  return !value || /[\s.,!?;:()[\]{}"'`~<>/\\|+-]/.test(value)
}

function hasValidBoundary(source: string, start: number, end: number, title: string) {
  if (!isAsciiWord(title)) {
    return true
  }

  const before = start > 0 ? source[start - 1] : ''
  const after = end < source.length ? source[end] : ''
  return isWordBoundaryChar(before) && isWordBoundaryChar(after)
}

function cleanExcerpt(value: string) {
  return value
    .replace(/\r?\n+/g, ' ')
    .replace(/[*_`>#-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function buildExcerpt(source: string, index: number, title: string) {
  const prefixStart = Math.max(0, index - 32)
  const suffixEnd = Math.min(source.length, index + title.length + 64)

  let excerptPrefix = cleanExcerpt(source.slice(prefixStart, index))
  let excerptSuffix = cleanExcerpt(source.slice(index + title.length, suffixEnd))

  if (prefixStart > 0 && excerptPrefix) {
    excerptPrefix = `...${excerptPrefix}`
  }

  if (suffixEnd < source.length && excerptSuffix) {
    excerptSuffix = `${excerptSuffix}...`
  }

  return {
    excerptPrefix,
    excerptSuffix,
  }
}

const flattenedNotes = computed(() => flattenNoteNodes(props.treeNodes))

const noteCandidates = computed(() => {
  const deduplicated = new Map<string, TreeNode>()

  flattenedNotes.value.forEach((note) => {
    const key = normalizeTitleKey(note.name)
    if (!key || deduplicated.has(key)) {
      return
    }
    deduplicated.set(key, note)
  })

  return Array.from(deduplicated.values())
})

const existingLinks = computed<ExistingLinkItem[]>(() => {
  const links = props.currentNote?.outgoingLinks ?? []
  const noteById = new Map(flattenedNotes.value.map((node) => [node.id, node]))
  const deduplicated = new Set<string>()

  return links
    .map((link, index) => {
      const treeNote = link.targetNoteId ? noteById.get(link.targetNoteId) : null
      const title = (link.targetNoteName || treeNote?.name || '').trim()
      const noteId = link.targetNoteId ?? null
      const uniqueKey = `${noteId || 'broken'}::${title || link.id || index}`

      if (!title || deduplicated.has(uniqueKey)) {
        return null
      }

      deduplicated.add(uniqueKey)
      return {
        id: `${link.id || index}`,
        noteId,
        title,
        isBroken: !noteId,
      }
    })
    .filter(Boolean) as ExistingLinkItem[]
})

const potentialLinks = computed<PotentialLinkItem[]>(() => {
  if (!props.currentNote) {
    return []
  }

  const source = props.noteContent || props.currentNote.markdownContent || ''
  if (!source.trim()) {
    return []
  }

  const wikiLinkRanges = extractWikiLinkRanges(source)
  const linkedTitleKeys = new Set<string>([
    ...wikiLinkRanges.map((item) => normalizeTitleKey(item.title)),
    ...existingLinks.value.map((item) => normalizeTitleKey(item.title)),
  ])

  const currentNoteKey = normalizeTitleKey(props.currentNote.title)

  return noteCandidates.value
    .map((note) => {
      const title = note.name.trim()
      const titleKey = normalizeTitleKey(title)

      if (!title || title.length < 2 || titleKey === currentNoteKey || linkedTitleKeys.has(titleKey)) {
        return null
      }

      const pattern = new RegExp(escapeRegExp(title), 'g')
      let firstMatchIndex = -1
      let occurrences = 0

      for (const match of source.matchAll(pattern)) {
        const index = match.index ?? -1
        if (index < 0) {
          continue
        }

        const end = index + title.length
        if (isRangeOverlapping(index, end, wikiLinkRanges) || !hasValidBoundary(source, index, end, title)) {
          continue
        }

        occurrences += 1
        if (firstMatchIndex < 0) {
          firstMatchIndex = index
        }
      }

      if (firstMatchIndex < 0) {
        return null
      }

      return {
        noteId: note.id,
        title,
        occurrences,
        ...buildExcerpt(source, firstMatchIndex, title),
      }
    })
    .filter((item): item is PotentialLinkItem => Boolean(item))
    .sort((left, right) => {
      if (right.occurrences !== left.occurrences) {
        return right.occurrences - left.occurrences
      }

      return left.title.localeCompare(right.title, 'zh-CN')
    }) as PotentialLinkItem[]
})
</script>

<template>
  <div class="note-link-panel">
    <section class="note-link-section">
      <header class="note-link-section-header">
        <h4>当前笔记中的链接</h4>
        <span>{{ existingLinks.length }}</span>
      </header>

      <div v-if="existingLinks.length" class="note-link-existing-list">
        <button
          v-for="link in existingLinks"
          :key="link.id"
          type="button"
          class="note-link-existing-item"
          :class="{ broken: link.isBroken }"
          :disabled="!link.noteId"
          @click="link.noteId && emit('open-note', link.noteId)"
        >
          <span class="note-link-existing-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path
                d="M7 4.75h7.5L18.25 8.5V18A1.25 1.25 0 0 1 17 19.25H7A1.25 1.25 0 0 1 5.75 18V6A1.25 1.25 0 0 1 7 4.75Z"
                fill="none"
                stroke="currentColor"
                stroke-linejoin="round"
                stroke-width="1.6"
              />
              <path d="M14.5 4.75V8.5h3.75" fill="none" stroke="currentColor" stroke-linejoin="round" stroke-width="1.6" />
            </svg>
          </span>
          <span class="note-link-existing-title">{{ link.title }}</span>
        </button>
      </div>
      <div v-else class="note-link-empty-state">当前笔记里还没有写入双链。</div>
    </section>

    <section class="note-link-section">
      <header class="note-link-section-header">
        <h4>当前笔记潜在的链接</h4>
        <span>{{ potentialLinks.length }}</span>
      </header>

      <div v-if="potentialLinks.length" class="note-link-potential-list">
        <article v-for="item in potentialLinks" :key="item.noteId" class="note-link-potential-card">
          <p class="note-link-potential-excerpt">
            <span v-if="item.excerptPrefix">{{ item.excerptPrefix }} </span>
            <mark>{{ item.title }}</mark>
            <span v-if="item.excerptSuffix"> {{ item.excerptSuffix }}</span>
          </p>

          <div class="note-link-potential-footer">
            <button type="button" class="note-link-chip" @click="emit('open-note', item.noteId)">
              <span class="note-link-chip-icon" aria-hidden="true">⛓</span>
              <span>{{ item.title }}</span>
            </button>
            <span class="note-link-potential-count">出现 {{ item.occurrences }} 次</span>
          </div>
        </article>
      </div>
      <div v-else class="note-link-empty-state">正文里暂时没有发现可自动关联的现有笔记标题。</div>
    </section>
  </div>
</template>

<style scoped>
.note-link-panel {
  display: grid;
  gap: 1rem;
  padding-bottom: 0.4rem;
}

.note-link-section {
  display: grid;
  gap: 0.75rem;
}

.note-link-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  color: #173637;
}

.note-link-section-header h4 {
  margin: 0;
  font-size: 0.97rem;
  font-weight: 700;
}

.note-link-section-header span {
  color: rgba(24, 54, 56, 0.48);
  font-size: 0.82rem;
}

.note-link-existing-list,
.note-link-potential-list {
  display: grid;
  gap: 0.55rem;
}

.note-link-existing-item,
.note-link-potential-card,
.note-link-empty-state {
  border: 1px solid rgba(32, 67, 63, 0.08);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.62);
}

.note-link-existing-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  width: 100%;
  padding: 0.68rem 0.78rem;
  border-radius: 0.7rem;
  color: #214243;
  text-align: left;
  cursor: pointer;
  transition:
    background 160ms ease,
    border-color 160ms ease,
    transform 160ms ease;
}

.note-link-existing-item:hover:not(:disabled) {
  background: rgba(246, 239, 230, 0.96);
  border-color: rgba(32, 67, 63, 0.12);
  transform: translateY(-1px);
}

.note-link-existing-item:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.note-link-existing-item.broken {
  color: rgba(143, 66, 56, 0.82);
}

.note-link-existing-icon {
  flex: 0 0 auto;
  width: 1rem;
  height: 1rem;
  color: rgba(29, 68, 69, 0.66);
}

.note-link-existing-icon svg {
  width: 100%;
  height: 100%;
  display: block;
}

.note-link-existing-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-link-potential-card {
  display: grid;
  gap: 0.7rem;
  padding: 0.85rem 0.9rem 0.82rem;
  border-radius: 0.88rem;
}

.note-link-potential-excerpt {
  margin: 0;
  color: rgba(24, 54, 56, 0.82);
  font-size: 0.84rem;
  line-height: 1.6;
  word-break: break-word;
}

.note-link-potential-excerpt mark {
  padding: 0 0.16rem;
  color: #1f1812;
  background: #f0c563;
  border-radius: 0.22rem;
}

.note-link-potential-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.note-link-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.38rem;
  padding: 0.38rem 0.64rem;
  border: 1px solid rgba(32, 67, 63, 0.08);
  border-radius: 999px;
  background: rgba(248, 243, 236, 0.88);
  color: #255754;
  cursor: pointer;
  transition:
    background 160ms ease,
    border-color 160ms ease,
    transform 160ms ease;
}

.note-link-chip:hover {
  background: rgba(243, 236, 227, 0.96);
  border-color: rgba(32, 67, 63, 0.12);
  transform: translateY(-1px);
}

.note-link-chip-icon {
  font-size: 0.74rem;
  opacity: 0.7;
}

.note-link-potential-count {
  color: rgba(24, 54, 56, 0.5);
  font-size: 0.76rem;
}

.note-link-empty-state {
  padding: 0.9rem 0.95rem;
  border-radius: 0.8rem;
  color: rgba(24, 54, 56, 0.68);
  font-size: 0.82rem;
  line-height: 1.55;
}
</style>

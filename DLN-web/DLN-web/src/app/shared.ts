export type Id = string
export type ViewMode = 'home' | 'templates' | 'settings' | 'graph-d3'
export type NoticeType = 'success' | 'error'
export type AuthMode = 'login' | 'register'
export type KnowledgeBaseModalMode = 'create' | 'edit'
export type TemplateModalMode = 'create' | 'edit'
export type CreateItemType = 'folder' | 'note'
export type DeleteKind = 'knowledge-base' | 'folder' | 'note' | 'template' | 'tag' | 'history'
export type SearchScope = 'all' | 'title' | 'content' | 'tag'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface LoginResponse {
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

export interface UserInfo {
  id: Id
  username: string
  email: string
  nickname: string
  avatarUrl?: string | null
  status?: number
  createdTime?: string
  updatedTime?: string
}

export interface UserSettings {
  id: Id
  userId: Id
  codeTheme?: string | null
  contentTheme?: string | null
  createdTime?: string
  updatedTime?: string
}

export interface UserThemeOptions {
  contentThemes?: string[] | null
  codeThemes?: string[] | null
  defaultContentTheme?: string | null
  defaultCodeTheme?: string | null
}

export interface KnowledgeBase {
  id: Id
  name: string
  description?: string | null
  status?: number
  createdTime?: string
  updatedTime?: string
}

export interface NoteTemplate {
  id: Id
  name: string
  description?: string | null
  templateContent: string
  createdTime?: string
  updatedTime?: string
}

export interface TreeNode {
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

export interface TagItem {
  id: Id
  knowledgeBaseId?: Id
  name: string
  createdTime?: string
}

export interface KnowledgeGraphNode {
  noteId: Id
  folderId?: Id | null
  title: string
  incomingCount: number
  outgoingCount: number
}

export interface KnowledgeGraphEdge {
  id: Id
  sourceNoteId: Id
  targetNoteId: Id
  targetNoteName?: string
  isBroken: number
}

export interface KnowledgeGraph {
  knowledgeBaseId: Id
  nodes: KnowledgeGraphNode[]
  edges: KnowledgeGraphEdge[]
}

export interface AttachmentItem {
  id: Id
  fileName: string
  fileUrl: string
}

export interface LinkItem {
  id: Id
  sourceNoteId?: Id
  targetNoteId?: Id | null
  targetNoteName?: string
}

export interface NoteLinkCandidate {
  noteId: Id
  folderId?: Id | null
  title: string
}

export interface NoteLinkPreview {
  noteId?: Id | null
  title: string
  markdownContent: string
  isBroken: number
}

export type NoteLinkOpenHandler = (noteId: Id) => Promise<void>

export interface NoteDetail {
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

export interface NoteHistoryVersion {
  id: Id
  noteId: Id
  versionNo: number
  title: string
  createdBy?: Id
  createdTime?: string
}

export interface NoteHistoryDetail extends NoteHistoryVersion {
  markdownContent: string
}

export interface KnowledgeBaseSearchFilters {
  keyword?: string
  scope?: SearchScope
  folderId?: Id | null
  tagIds?: Id[]
}

export interface NoteSearchResult {
  noteId: Id
  knowledgeBaseId: Id
  folderId?: Id | null
  title: string
  folderPath: string
  snippet: string
  matchedByTitle: boolean
  matchedByContent: boolean
  matchedByTag: boolean
  incomingCount: number
  outgoingCount: number
  brokenLinkCount: number
  createdTime?: string
  updatedTime?: string
  tags: TagItem[]
}

export const API_BASE = ((import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080')
  .trim()
  .replace(/\/+$/, '')

export const TOKEN_KEY = 'dln-token'
export const LEGACY_TOKEN_KEY = 'token'
export const USER_KEY = 'dln-user'
export const DEFAULT_CONTENT_THEME = 'light'
export const DEFAULT_CODE_THEME = 'github'
export const DEFAULT_CONTENT_THEME_OPTIONS = ['ant-design', 'dark', 'light', 'wechat']
export const GRAPH_VIEW_WIDTH = 1200
export const GRAPH_VIEW_HEIGHT = 720
export const GRAPH_CENTER_X = GRAPH_VIEW_WIDTH / 2
export const GRAPH_CENTER_Y = GRAPH_VIEW_HEIGHT / 2
export const GRAPH_INNER_RADIUS = 220
export const GRAPH_RING_GAP = 96

export function loadStoredUser(): UserInfo | null {
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

export function setAuthCookie(tokenValue: string) {
  const normalized = tokenValue.trim()
  if (!normalized) {
    return
  }

  const encoded = encodeURIComponent(normalized)
  document.cookie = `${TOKEN_KEY}=${encoded}; path=/; SameSite=Lax`
  document.cookie = `${LEGACY_TOKEN_KEY}=${encoded}; path=/; SameSite=Lax`
}

export function clearAuthCookie() {
  document.cookie = `${TOKEN_KEY}=; path=/; Max-Age=0; SameSite=Lax`
  document.cookie = `${LEGACY_TOKEN_KEY}=; path=/; Max-Age=0; SameSite=Lax`
}

export function normalizeOptionalText(value: string) {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

export function normalizeThemeName(value: string | undefined | null, fallback: string) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : fallback
}

export function normalizeThemeOptions(
  values: Array<string | null | undefined> | undefined | null,
  fallbackValues: string[],
) {
  const result: string[] = []
  const seen = new Set<string>()

  for (const value of [...fallbackValues, ...(values ?? [])]) {
    const trimmed = value?.trim()

    if (!trimmed || seen.has(trimmed)) {
      continue
    }

    seen.add(trimmed)
    result.push(trimmed)
  }

  return result
}

export function resolveAssetUrl(value?: string | null) {
  const trimmed = value?.trim()
  if (!trimmed) {
    return ''
  }

  if (/^(https?:)?\/\//i.test(trimmed) || trimmed.startsWith('data:') || trimmed.startsWith('blob:')) {
    return trimmed
  }

  return `${API_BASE}${trimmed.startsWith('/') ? trimmed : `/${trimmed}`}`
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function stripLeadingTitleHeading(title: string, markdownContent?: string | null) {
  const markdown = markdownContent || ''
  const normalizedTitle = title.trim()

  if (!normalizedTitle) {
    return markdown
  }

  const pattern = new RegExp(`^\\s*#\\s+${escapeRegExp(normalizedTitle)}\\s*(?:(?:\\r?\\n){1,2}|$)`, 'i')
  return markdown.replace(pattern, '')
}

export function normalizeNote(note: NoteDetail): NoteDetail {
  return {
    ...note,
    markdownContent: stripLeadingTitleHeading(note.title, note.markdownContent),
    tags: note.tags ?? [],
    attachments: note.attachments ?? [],
    outgoingLinks: note.outgoingLinks ?? [],
    incomingLinks: note.incomingLinks ?? [],
  }
}

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

export function formatDateTime(value?: string | null) {
  if (!value) {
    return '未记录'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return dateTimeFormatter.format(date)
}

export function previewTemplateContent(content: string) {
  const normalized = content.replace(/\s+/g, ' ').trim()

  if (!normalized) {
    return '模板正文为空。'
  }

  return normalized.length > 140 ? `${normalized.slice(0, 140)}...` : normalized
}

export function sortTags(tags: TagItem[]) {
  return [...tags].sort((left, right) => left.name.localeCompare(right.name, 'zh-CN', { sensitivity: 'base' }))
}

export function normalizeTagIds(tagIds: Id[]) {
  return [...new Set(tagIds.filter(Boolean))]
}

export function extractTagIds(tags?: TagItem[]) {
  return normalizeTagIds((tags ?? []).map((tag) => tag.id))
}

export function areSameTagSelection(left: Id[], right: Id[]) {
  const normalizedLeft = [...normalizeTagIds(left)].sort()
  const normalizedRight = [...normalizeTagIds(right)].sort()

  if (normalizedLeft.length !== normalizedRight.length) {
    return false
  }

  return normalizedLeft.every((value, index) => value === normalizedRight[index])
}

export interface PositionedKnowledgeGraphNode extends KnowledgeGraphNode {
  x: number
  y: number
  radius: number
  degree: number
}

export interface PositionedKnowledgeGraphEdge extends KnowledgeGraphEdge {
  sourceX: number
  sourceY: number
  targetX: number
  targetY: number
}

export interface GraphLayout {
  nodes: PositionedKnowledgeGraphNode[]
  edges: PositionedKnowledgeGraphEdge[]
}

export function buildGraphLayout(graph?: KnowledgeGraph | null): GraphLayout {
  if (!graph?.nodes.length) {
    return {
      nodes: [],
      edges: [],
    }
  }

  const degreeMap = new Map<Id, number>()
  graph.nodes.forEach((node) => {
    degreeMap.set(node.noteId, (node.incomingCount || 0) + (node.outgoingCount || 0))
  })

  const sortedNodes = [...graph.nodes].sort((left, right) => {
    const degreeDiff = (degreeMap.get(right.noteId) ?? 0) - (degreeMap.get(left.noteId) ?? 0)
    if (degreeDiff !== 0) {
      return degreeDiff
    }

    return left.title.localeCompare(right.title, 'zh-CN', { sensitivity: 'base' })
  })

  const capacities: number[] = []
  let remaining = sortedNodes.length
  let ringIndex = 0
  while (remaining > 0) {
    const ringCapacity = ringIndex === 0 ? 8 : 8 + ringIndex * 6
    capacities.push(ringCapacity)
    remaining -= ringCapacity
    ringIndex += 1
  }

  const positionedNodes = sortedNodes.map((node, index) => {
    let consumed = 0
    let currentRing = 0

    while (currentRing < capacities.length && index >= consumed + capacities[currentRing]) {
      consumed += capacities[currentRing]
      currentRing += 1
    }

    const slotIndex = index - consumed
    const ringCapacity = capacities[currentRing] || 1
    const angle = (Math.PI * 2 * slotIndex) / ringCapacity - Math.PI / 2 + currentRing * 0.16
    const radius = GRAPH_INNER_RADIUS + currentRing * GRAPH_RING_GAP + (slotIndex % 2) * 10
    const x = GRAPH_CENTER_X + Math.cos(angle) * radius
    const y = GRAPH_CENTER_Y + Math.sin(angle) * radius
    const degree = degreeMap.get(node.noteId) ?? 0

    return {
      ...node,
      x,
      y,
      degree,
      radius: 20 + Math.min(12, degree * 2),
    }
  })

  const nodePositionMap = new Map(positionedNodes.map((node) => [node.noteId, node]))
  const positionedEdges = graph.edges
    .map((edge) => {
      const source = nodePositionMap.get(edge.sourceNoteId)
      const target = nodePositionMap.get(edge.targetNoteId)
      if (!source || !target) {
        return null
      }

      return {
        ...edge,
        sourceX: source.x,
        sourceY: source.y,
        targetX: target.x,
        targetY: target.y,
      }
    })
    .filter(Boolean) as PositionedKnowledgeGraphEdge[]

  return {
    nodes: positionedNodes,
    edges: positionedEdges,
  }
}

export function createLocalUpdatedTime() {
  return new Date().toISOString()
}

export function findTreeNodeById(nodes: TreeNode[], targetId: Id): TreeNode | null {
  for (const node of nodes) {
    if (node.id === targetId) {
      return node
    }

    if (node.children?.length) {
      const childMatch = findTreeNodeById(node.children, targetId)
      if (childMatch) {
        return childMatch
      }
    }
  }

  return null
}

export function collectSubtreeIds(node: TreeNode, folderIds: Set<Id>, noteIds: Set<Id>) {
  if (node.type === 'folder') {
    folderIds.add(node.id)
  } else {
    noteIds.add(node.id)
  }

  if (!node.children?.length) {
    return
  }

  for (const child of node.children) {
    collectSubtreeIds(child, folderIds, noteIds)
  }
}

export function sortTreeNodes(nodes: TreeNode[]): TreeNode[] {
  return [...nodes]
    .map((node) => {
      if (!node.children?.length) {
        return node
      }

      return {
        ...node,
        children: sortTreeNodes(node.children),
      }
    })
    .sort((left, right) => {
      const typeCompare = right.type.localeCompare(left.type)
      if (typeCompare !== 0) {
        return typeCompare
      }

      return left.name.localeCompare(right.name, 'zh-CN', { sensitivity: 'base' })
    })
}

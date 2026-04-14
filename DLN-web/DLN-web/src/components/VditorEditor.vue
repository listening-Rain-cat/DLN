<script setup lang="ts">
import { inject, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

interface OutlineItem {
  id: string
  text: string
  level: number
}

interface PreviewThemeSettings {
  contentTheme: string
  codeTheme: string
}

interface ApiResult<T> {
  code?: number
  message?: string
  data?: T
}

interface VditorUploadPayload {
  code?: number
  msg?: string
  data?: {
    errFiles?: string[]
    succMap?: Record<string, string>
  }
}

interface NoteLinkCandidate {
  noteId: string
  folderId?: string | null
  title: string
}

interface NoteLinkPreview {
  noteId?: string | null
  title: string
  markdownContent: string
  isBroken: number
}

interface LinkHintRangeInfo {
  node: Text
  startOffset: number
  endOffset: number
}

interface TextPosition {
  node: Text
  offset: number
}

interface WikiLinkMatch {
  title: string
  start: number
  end: number
}

interface HighlightBox {
  id: string
  left: number
  top: number
  width: number
  height: number
}

type NoteLinkCandidatesFetcher = (keyword: string) => Promise<NoteLinkCandidate[]>
type NoteLinkPreviewFetcher = (title: string) => Promise<NoteLinkPreview | null>
type NoteLinkOpenHandler = (noteId: string) => Promise<void>
type CurrentNoteIdGetter = () => string | null

const DEFAULT_CONTENT_THEME = 'light'
const DEFAULT_CODE_THEME = 'github'
const COMPACT_EDITOR_MAX_WIDTH = 2400
const WIKI_LINK_QUERY_PATTERN = /\[\[([^\[\]\r\n]*)$/
const WIKI_LINK_PATTERN = /\[\[([^\[\]\r\n]+)]]/g
const STANDALONE_URL_PATTERN = /^<?(https?:\/\/[^\s<>]+)>?$/i
const STANDALONE_MARKDOWN_LINK_PATTERN = /^\[[^\]]+]\((https?:\/\/[^\s)]+)\)$/i
const DIRECT_VIDEO_URL_PATTERN = /^https?:\/\/\S+\.(?:mp4|m4v|ogg|ogv|webm)(?:[?#]\S*)?$/i
const DIRECT_AUDIO_URL_PATTERN = /^https?:\/\/\S+\.(?:mp3|wav|flac)(?:[?#]\S*)?$/i
const API_BASE = ((import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080')
  .trim()
  .replace(/\/+$/, '')
const TOKEN_KEY = 'dln-token'
const LEGACY_TOKEN_KEY = 'token'
const props = defineProps<{
  modelValue: string
  contentTheme?: string
  codeTheme?: string
  noteId?: string | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'outline-change', outline: OutlineItem[]): void
  (e: 'fullscreen-change', value: boolean): void
  (e: 'theme-change', value: PreviewThemeSettings): void
}>()

const fetchLinkCandidates = inject<NoteLinkCandidatesFetcher | undefined>(
  'noteLinkCandidatesFetcher',
  undefined,
)
const fetchLinkPreview = inject<NoteLinkPreviewFetcher | undefined>(
  'noteLinkPreviewFetcher',
  undefined,
)
const openLinkedNote = inject<NoteLinkOpenHandler | undefined>(
  'noteLinkOpenHandler',
  undefined,
)
const getCurrentNoteId = inject<CurrentNoteIdGetter | undefined>('currentNoteIdGetter', undefined)

const host = ref<HTMLDivElement | null>(null)
const editorMount = ref<HTMLDivElement | null>(null)
const linkPreviewBody = ref<HTMLDivElement | null>(null)
let editor: Vditor | null = null
let syncingFromProps = false
let applyingMediaTransform = false
let outlineTimer: number | null = null
let pendingThemeSyncTimer: number | null = null
let fullscreenObserver: MutationObserver | null = null
let lastThemeSettings: PreviewThemeSettings | null = null
let linkHintRequestId = 0
let linkPreviewRequestId = 0
let linkPreviewHideTimer: number | null = null
let wikiLinkHighlightTimer: number | null = null
let hoverLinkRange: Range | null = null
let selectionLinkRange: Range | null = null
const wikiLinkBoxes = ref<HighlightBox[]>([])
const activeWikiLinkBoxes = ref<HighlightBox[]>([])

const linkPreviewCache = new Map<string, NoteLinkPreview | null>()

const linkHint = reactive({
  visible: false,
  loading: false,
  query: '',
  left: 0,
  top: 0,
  selectedIndex: 0,
  items: [] as NoteLinkCandidate[],
  rangeInfo: null as LinkHintRangeInfo | null,
})

const linkPreviewState = reactive({
  visible: false,
  loading: false,
  title: '',
  left: 0,
  top: 0,
  data: null as NoteLinkPreview | null,
})

const editorToolbar = [
  'emoji',
  'headings',
  'bold',
  'italic',
  'strike',
  'link',
  '|',
  'list',
  'ordered-list',
  'check',
  'outdent',
  'indent',
  '|',
  'quote',
  'line',
  'code',
  'inline-code',
  'insert-before',
  'insert-after',
  '|',
  'export',
  'upload',
  'table',
  'undo',
  'redo',
  '|',
  'fullscreen',
  'edit-mode',
]

function normalizeThemeName(value: string | undefined | null, fallback: string) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : fallback
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

function buildNoteImageUploadUrl(noteId?: string | null) {
  const normalizedNoteId = noteId?.trim()
  return normalizedNoteId ? `${API_BASE}/notes/${encodeURIComponent(normalizedNoteId)}/attachments/vditor` : ''
}

function resolveEditorNoteId() {
  const propNoteId = props.noteId?.trim()
  if (propNoteId) {
    return propNoteId
  }

  return getCurrentNoteId?.() || null
}

function buildUploadHeaders() {
  const headers: Record<string, string> = {}
  const rawToken = (localStorage.getItem(TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY) || '').trim()
  if (!rawToken) {
    return headers
  }

  headers.Authorization = rawToken.startsWith('Bearer ') ? rawToken : `Bearer ${rawToken}`
  return headers
}

function extractUploadErrorMessage(responseText: string) {
  if (!responseText.trim()) {
    return '图片上传失败，请稍后重试。'
  }

  try {
    const payload = JSON.parse(responseText) as ApiResult<unknown> & { msg?: string }
    return payload.message || payload.msg || '图片上传失败，请稍后重试。'
  } catch {
    return responseText
  }
}

function syncUploadOptions() {
  if (!editor) {
    return
  }

  const uploadOptions = editor.vditor.options.upload
  if (!uploadOptions) {
    return
  }

  uploadOptions.url = buildNoteImageUploadUrl(resolveEditorNoteId())
}

function escapeMarkdownText(value: string) {
  return value.replace(/\\/g, '\\\\').replace(/\[/g, '\\[').replace(/]/g, '\\]')
}

function stripFileExtension(fileName: string) {
  const lastDotIndex = fileName.lastIndexOf('.')
  if (lastDotIndex <= 0) {
    return fileName
  }
  return fileName.slice(0, lastDotIndex)
}

function escapeHtmlAttribute(value: string) {
  return value.replace(/&/g, '&amp;').replace(/"/g, '&quot;')
}

function extractStandaloneMediaUrl(line: string) {
  const trimmed = line.trim()
  if (!trimmed || trimmed !== line) {
    return null
  }

  if (trimmed.startsWith('<video') || trimmed.startsWith('<audio') || trimmed.startsWith('<iframe')) {
    return null
  }

  const markdownLinkMatch = trimmed.match(STANDALONE_MARKDOWN_LINK_PATTERN)
  if (markdownLinkMatch?.[1]) {
    return markdownLinkMatch[1]
  }

  const plainUrlMatch = trimmed.match(STANDALONE_URL_PATTERN)
  if (plainUrlMatch?.[1]) {
    return plainUrlMatch[1]
  }

  return null
}

function buildEmbeddedMediaHtml(url: string) {
  const normalizedUrl = url.trim()
  if (!normalizedUrl) {
    return null
  }

  if (DIRECT_VIDEO_URL_PATTERN.test(normalizedUrl)) {
    return `<video controls="controls" src="${escapeHtmlAttribute(normalizedUrl)}"></video>`
  }

  if (DIRECT_AUDIO_URL_PATTERN.test(normalizedUrl)) {
    return `<audio controls="controls" src="${escapeHtmlAttribute(normalizedUrl)}"></audio>`
  }

  const qqMatch = normalizedUrl.match(/\/\/v\.qq\.com\/x\/cover\/.*\/([^/?#]+)\.html(?:\?.*)?$/i)
  if (qqMatch?.[1]) {
    return `<iframe class="iframe__video" src="https://v.qq.com/txp/iframe/player.html?vid=${escapeHtmlAttribute(qqMatch[1])}" allowfullscreen="true"></iframe>`
  }

  if (/player\.bilibili\.com\/player\.html/i.test(normalizedUrl)) {
    return `<iframe class="iframe__video" src="${escapeHtmlAttribute(normalizedUrl)}" allowfullscreen="true"></iframe>`
  }

  if (/bilibili\.com/i.test(normalizedUrl)) {
    try {
      const parsedUrl = new URL(normalizedUrl)
      const pathBvidMatch = parsedUrl.pathname.match(/\/video\/([^/?#]+)/i)
      const bvid = parsedUrl.searchParams.get('bvid') || pathBvidMatch?.[1]

      if (bvid) {
        const params = new URLSearchParams({
          bvid,
          page: parsedUrl.searchParams.get('page') || '1',
          high_quality: '1',
          as_wide: '1',
          allowfullscreen: 'true',
          autoplay: '0',
        })

        return `<iframe class="iframe__video" src="${escapeHtmlAttribute(`https://player.bilibili.com/player.html?${params.toString()}`)}" allowfullscreen="true"></iframe>`
      }
    } catch {
      return null
    }
  }

  return null
}

function transformStandaloneMediaLinks(markdown: string) {
  if (!markdown.trim()) {
    return markdown
  }

  const lineBreak = markdown.includes('\r\n') ? '\r\n' : '\n'
  const lines = markdown.split(/\r?\n/)
  let insideCodeFence = false
  let changed = false

  const transformedLines = lines.map((line) => {
    const trimmed = line.trim()

    if (/^```/.test(trimmed)) {
      insideCodeFence = !insideCodeFence
      return line
    }

    if (insideCodeFence) {
      return line
    }

    const mediaUrl = extractStandaloneMediaUrl(line)
    if (!mediaUrl) {
      return line
    }

    const embeddedHtml = buildEmbeddedMediaHtml(mediaUrl)
    if (!embeddedHtml) {
      return line
    }

    changed = true
    return embeddedHtml
  })

  return changed ? transformedLines.join(lineBreak) : markdown
}

function buildImageMarkdown(fileName: string, url: string) {
  const altText = escapeMarkdownText(stripFileExtension(fileName).trim() || 'image')
  return `![${altText}](${url})`
}

function handleUploadSuccess(responseText: string) {
  if (!editor) {
    return
  }

  let payload: VditorUploadPayload | null = null

  try {
    payload = JSON.parse(responseText) as VditorUploadPayload
  } catch {
    editor.tip('图片上传成功，但返回结果无法解析。')
    return
  }

  const successMap = payload.data?.succMap ?? {}
  const uploadedEntries = Object.entries(successMap)
  const errorFiles = payload.data?.errFiles ?? []

  if (uploadedEntries.length) {
    const markdown = uploadedEntries
      .map(([fileName, url]) => buildImageMarkdown(fileName, url))
      .join('\n')

    editor.focus()
    editor.insertValue(`${markdown}\n`)
  }

  if (errorFiles.length || payload.code === 1) {
    const errorMessage = payload.msg || extractUploadErrorMessage(responseText)
    editor.tip(errorFiles.length ? `${errorMessage}：${errorFiles.join('、')}` : errorMessage)
  }
}

function getPreviewThemeSettings(): PreviewThemeSettings {
  return {
    contentTheme: normalizeThemeName(
      editor?.vditor.options.preview?.theme?.current,
      props.contentTheme ?? DEFAULT_CONTENT_THEME,
    ),
    codeTheme: normalizeThemeName(
      editor?.vditor.options.preview?.hljs?.style,
      props.codeTheme ?? DEFAULT_CODE_THEME,
    ),
  }
}

function syncThemeSettings(emitChange = false) {
  const nextSettings = getPreviewThemeSettings()
  const changed =
    !lastThemeSettings ||
    lastThemeSettings.contentTheme !== nextSettings.contentTheme ||
    lastThemeSettings.codeTheme !== nextSettings.codeTheme

  lastThemeSettings = nextSettings

  if (emitChange && changed) {
    emit('theme-change', nextSettings)
  }
}

function scheduleThemeSettingsSync() {
  if (pendingThemeSyncTimer) {
    window.clearTimeout(pendingThemeSyncTimer)
  }

  pendingThemeSyncTimer = window.setTimeout(() => {
    syncThemeSettings(true)
    pendingThemeSyncTimer = null
  }, 0)
}

function handlePotentialThemeChange() {
  scheduleThemeSettingsSync()
}

function getCurrentEditorElement() {
  if (!host.value || !editor) {
    return null
  }

  const currentMode = editor.getCurrentMode()

  if (currentMode === 'wysiwyg') {
    return host.value.querySelector('.vditor-wysiwyg') as HTMLElement | null
  }

  if (currentMode === 'ir') {
    return host.value.querySelector('.vditor-ir') as HTMLElement | null
  }

  return host.value.querySelector('.vditor-sv') as HTMLElement | null
}

function getPrimaryContentElement() {
  if (!host.value || !editor) {
    return null
  }

  const currentMode = editor.getCurrentMode()

  if (currentMode === 'wysiwyg') {
    return host.value.querySelector('.vditor-wysiwyg .vditor-reset') as HTMLElement | null
  }

  if (currentMode === 'ir') {
    return host.value.querySelector('.vditor-ir .vditor-reset') as HTMLElement | null
  }

  return host.value.querySelector('.vditor-sv .vditor-reset') as HTMLElement | null
}

function shouldSkipHighlightTextNode(node: Text) {
  const parentElement = node.parentElement
  if (!parentElement) {
    return true
  }

  return Boolean(
    parentElement.closest(
      '.vditor-wysiwyg__preview, .vditor-ir__preview, .vditor-preview, .vditor-hint, .note-link-preview',
    ),
  )
}

function collectWikiLinkRanges() {
  const editorElement = getCurrentEditorElement()
  if (!editorElement) {
    return []
  }

  const ranges: Range[] = []
  const walker = document.createTreeWalker(
    editorElement,
    NodeFilter.SHOW_TEXT,
    {
      acceptNode(node) {
        if (!(node instanceof Text)) {
          return NodeFilter.FILTER_SKIP
        }

        const text = node.textContent ?? ''
        if (!text.includes('[[') || !text.includes(']]') || shouldSkipHighlightTextNode(node)) {
          return NodeFilter.FILTER_SKIP
        }

        return NodeFilter.FILTER_ACCEPT
      },
    },
  )

  let currentNode = walker.nextNode()
  while (currentNode) {
    const textNode = currentNode as Text
    const text = textNode.textContent ?? ''

    WIKI_LINK_PATTERN.lastIndex = 0
    let match: RegExpExecArray | null
    while ((match = WIKI_LINK_PATTERN.exec(text)) !== null) {
      if (!match[1].trim()) {
        continue
      }

      const range = document.createRange()
      range.setStart(textNode, match.index)
      range.setEnd(textNode, match.index + match[0].length)
      ranges.push(range)
    }

    currentNode = walker.nextNode()
  }

  return ranges
}

function buildHighlightBoxes(ranges: Range[], prefix: string) {
  if (!host.value) {
    return []
  }

  const hostRect = host.value.getBoundingClientRect()
  const result: HighlightBox[] = []

  ranges.forEach((range, rangeIndex) => {
    Array.from(range.getClientRects()).forEach((rect, rectIndex) => {
      if (rect.width <= 0 || rect.height <= 0) {
        return
      }

      result.push({
        id: `${prefix}-${rangeIndex}-${rectIndex}`,
        left: rect.left - hostRect.left,
        top: rect.top - hostRect.top,
        width: rect.width,
        height: rect.height,
      })
    })
  })

  return result
}

function refreshWikiLinkHighlights() {
  wikiLinkBoxes.value = buildHighlightBoxes(collectWikiLinkRanges(), 'wiki-link')
  const activeRange = hoverLinkRange ?? selectionLinkRange
  activeWikiLinkBoxes.value = activeRange
    ? buildHighlightBoxes([activeRange.cloneRange()], 'wiki-link-active')
    : []
}

function scheduleWikiLinkHighlightRefresh() {
  if (wikiLinkHighlightTimer) {
    window.clearTimeout(wikiLinkHighlightTimer)
  }

  wikiLinkHighlightTimer = window.setTimeout(() => {
    refreshWikiLinkHighlights()
    wikiLinkHighlightTimer = null
  }, 0)
}

function normalizeOutlineHeadingText(value?: string | null) {
  const rawText = value?.replace(/\u200b/g, '').trim() || ''
  const normalizedText = rawText.replace(/^#{1,6}(?:\s|$)+/, '').trim()
  return normalizedText || rawText || '未命名标题'
}

function buildOutline(contentElement: HTMLElement): OutlineItem[] {
  const headings = Array.from(contentElement.querySelectorAll<HTMLElement>('h1, h2, h3, h4, h5, h6'))

  return headings.map((heading, index) => {
    const id = `outline-heading-${index}`
    heading.id = id

    return {
      id,
      text: normalizeOutlineHeadingText(heading.textContent),
      level: Number(heading.tagName.replace('H', '')) || 1,
    }
  })
}

function syncOutline() {
  if (!host.value) {
    emit('outline-change', [])
    return
  }

  const primaryContent = getPrimaryContentElement()
  if (!primaryContent) {
    emit('outline-change', [])
    return
  }

  let outline = buildOutline(primaryContent)

  if (!outline.length) {
    const preview = host.value.querySelector('.vditor-preview') as HTMLElement | null
    if (preview && preview !== primaryContent) {
      outline = buildOutline(preview)
    }
  }

  emit('outline-change', outline)
}

function scheduleOutlineSync() {
  if (outlineTimer) {
    window.clearTimeout(outlineTimer)
  }

  outlineTimer = window.setTimeout(() => {
    syncOutline()
    outlineTimer = null
  }, 120)
}

function syncToolbarTitles() {
  if (!host.value) {
    return
  }

  host.value.querySelectorAll<HTMLElement>('.vditor-toolbar [aria-label]').forEach((element) => {
    const label = element.getAttribute('aria-label')?.trim()
    if (label) {
      element.setAttribute('title', label)
    }
  })
}

function syncFullscreenState() {
  emit('fullscreen-change', Boolean(editorMount.value?.classList.contains('vditor--fullscreen')))
}

function scrollToHeading(id: string) {
  if (!host.value) {
    return
  }

  const target =
    getPrimaryContentElement()?.querySelector<HTMLElement>(`#${id}`) ??
    host.value.querySelector<HTMLElement>(`.vditor-preview #${id}`)

  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function getSelectionRange() {
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0) {
    return null
  }

  return selection.getRangeAt(0)
}

function isNodeInsideEditor(node: Node | null) {
  const editorElement = getCurrentEditorElement()
  return Boolean(editorElement && node && editorElement.contains(node))
}

function getEdgeTextNode(node: Node | null, backward: boolean): Text | null {
  if (!node) {
    return null
  }

  if (node.nodeType === Node.TEXT_NODE) {
    return node as Text
  }

  const children = node.childNodes
  if (!children.length) {
    return null
  }

  const ordered = backward
    ? Array.from(children).reverse()
    : Array.from(children)

  for (const child of ordered) {
    const textNode = getEdgeTextNode(child, backward)
    if (textNode) {
      return textNode
    }
  }

  return null
}

function resolveTextPositionFromRange(range: Range): TextPosition | null {
  if (range.startContainer.nodeType === Node.TEXT_NODE) {
    const textNode = range.startContainer as Text
    return {
      node: textNode,
      offset: Math.min(range.startOffset, textNode.textContent?.length ?? 0),
    }
  }

  const container = range.startContainer
  const previousNode =
    range.startOffset > 0 ? container.childNodes[range.startOffset - 1] ?? null : null
  const nextNode = container.childNodes[range.startOffset] ?? null
  const previousTextNode = getEdgeTextNode(previousNode, true)
  if (previousTextNode) {
    return {
      node: previousTextNode,
      offset: previousTextNode.textContent?.length ?? 0,
    }
  }

  const nextTextNode = getEdgeTextNode(nextNode, false)
  if (nextTextNode) {
    return {
      node: nextTextNode,
      offset: 0,
    }
  }

  return null
}

function resolveTextPositionFromPoint(clientX: number, clientY: number): TextPosition | null {
  const caretDocument = document as Document & {
    caretRangeFromPoint?: (x: number, y: number) => Range | null
    caretPositionFromPoint?: (x: number, y: number) => { offsetNode: Node; offset: number } | null
  }

  if (caretDocument.caretPositionFromPoint) {
    const caretPosition = caretDocument.caretPositionFromPoint(clientX, clientY)
    if (!caretPosition) {
      return null
    }

    if (caretPosition.offsetNode.nodeType === Node.TEXT_NODE) {
      const textNode = caretPosition.offsetNode as Text
      return {
        node: textNode,
        offset: Math.min(caretPosition.offset, textNode.textContent?.length ?? 0),
      }
    }

    const range = document.createRange()
    range.setStart(caretPosition.offsetNode, caretPosition.offset)
    range.collapse(true)
    return resolveTextPositionFromRange(range)
  }

  if (caretDocument.caretRangeFromPoint) {
    const range = caretDocument.caretRangeFromPoint(clientX, clientY)
    return range ? resolveTextPositionFromRange(range) : null
  }

  return null
}

function resolveWikiLinkRangeFromPoint(clientX: number, clientY: number) {
  const textPosition = resolveTextPositionFromPoint(clientX, clientY)
  if (!textPosition || !isNodeInsideEditor(textPosition.node)) {
    return null
  }

  const match = findWikiLinkMatch(textPosition.node.textContent ?? '', textPosition.offset)
  if (!match) {
    return null
  }

  const range = document.createRange()
  range.setStart(textPosition.node, match.start)
  range.setEnd(textPosition.node, match.end)

  return {
    title: match.title,
    range,
  }
}

function findWikiLinkMatch(text: string, offset: number): WikiLinkMatch | null {
  WIKI_LINK_PATTERN.lastIndex = 0

  let match: RegExpExecArray | null
  while ((match = WIKI_LINK_PATTERN.exec(text)) !== null) {
    const start = match.index
    const end = start + match[0].length
    if (offset >= start && offset <= end) {
      return {
        title: match[1].trim(),
        start,
        end,
      }
    }
  }

  return null
}

function isInsideClosedWikiLink(text: string, offset: number) {
  const start = text.lastIndexOf('[[', offset)
  if (start === -1) {
    return false
  }

  const end = text.indexOf(']]', start + 2)
  if (end === -1) {
    return false
  }

  if (offset > end + 2) {
    return false
  }

  const title = text.slice(start + 2, end).trim()
  return Boolean(title)
}

function getOverlayOffset() {
  const editorElement = getCurrentEditorElement()
  if (!host.value || !editorElement?.parentElement) {
    return { left: 0, top: 0 }
  }

  const hostRect = host.value.getBoundingClientRect()
  const parentRect = editorElement.parentElement.getBoundingClientRect()
  return {
    left: parentRect.left - hostRect.left,
    top: parentRect.top - hostRect.top,
  }
}

function hideLinkHint() {
  linkHint.visible = false
  linkHint.loading = false
  linkHint.query = ''
  linkHint.selectedIndex = 0
  linkHint.items = []
  linkHint.rangeInfo = null
}

function clearLinkPreviewHideTimer() {
  if (linkPreviewHideTimer) {
    window.clearTimeout(linkPreviewHideTimer)
    linkPreviewHideTimer = null
  }
}

function hideLinkPreview() {
  clearLinkPreviewHideTimer()
  linkPreviewState.visible = false
  linkPreviewState.loading = false
  linkPreviewState.title = ''
  linkPreviewState.data = null
}

function scheduleLinkPreviewHide() {
  clearLinkPreviewHideTimer()
  linkPreviewHideTimer = window.setTimeout(() => {
    hideLinkPreview()
  }, 120)
}

async function updateLinkHintFromSelection() {
  if (!editor || !fetchLinkCandidates) {
    hideLinkHint()
    return
  }

  const range = getSelectionRange()
  if (!range || !range.collapsed || !isNodeInsideEditor(range.startContainer)) {
    hideLinkHint()
    return
  }

  const textPosition = resolveTextPositionFromRange(range)
  if (!textPosition) {
    hideLinkHint()
    return
  }

  const currentText = textPosition.node.textContent ?? ''
  const existingWikiLink = findWikiLinkMatch(currentText, textPosition.offset)
  if (existingWikiLink || isInsideClosedWikiLink(currentText, textPosition.offset)) {
    hideLinkHint()
    return
  }

  const textBeforeCaret = currentText.slice(0, textPosition.offset)
  const triggerMatch = textBeforeCaret.match(WIKI_LINK_QUERY_PATTERN)
  if (!triggerMatch) {
    hideLinkHint()
    return
  }

  hideLinkPreview()

  const currentEditorElement = getCurrentEditorElement()
  if (!currentEditorElement) {
    hideLinkHint()
    return
  }

  const cursorPosition = editor.getCursorPosition()
  const overlayOffset = getOverlayOffset()
  const lineHeight =
    parseInt(window.getComputedStyle(currentEditorElement).lineHeight, 10) || 24
  const query = triggerMatch[1]

  linkHint.visible = true
  linkHint.loading = true
  linkHint.query = query
  linkHint.left = overlayOffset.left + cursorPosition.left
  linkHint.top = overlayOffset.top + cursorPosition.top + lineHeight
  linkHint.selectedIndex = 0
  linkHint.rangeInfo = {
    node: textPosition.node,
    startOffset: textPosition.offset - triggerMatch[0].length,
    endOffset: textPosition.offset,
  }

  const requestId = ++linkHintRequestId

  try {
    const items = await fetchLinkCandidates(query)
    if (requestId !== linkHintRequestId || linkHint.query !== query) {
      return
    }

    linkHint.items = items
  } catch {
    if (requestId !== linkHintRequestId) {
      return
    }

    linkHint.items = []
  } finally {
    if (requestId === linkHintRequestId) {
      linkHint.loading = false
    }
  }
}

function moveLinkHintSelection(step: number) {
  if (!linkHint.items.length) {
    return
  }

  const nextIndex = linkHint.selectedIndex + step
  if (nextIndex < 0) {
    linkHint.selectedIndex = linkHint.items.length - 1
    return
  }

  if (nextIndex >= linkHint.items.length) {
    linkHint.selectedIndex = 0
    return
  }

  linkHint.selectedIndex = nextIndex
}

function dispatchEditorInputEvent(data: string) {
  const editorElement = getCurrentEditorElement()
  if (!editorElement) {
    return
  }

  try {
    editorElement.dispatchEvent(
      new InputEvent('input', {
        bubbles: true,
        data,
        inputType: 'insertText',
      }),
    )
  } catch {
    editorElement.dispatchEvent(new Event('input', { bubbles: true }))
  }
}

function replaceCurrentLinkQuery(replacement: string) {
  const rangeInfo = linkHint.rangeInfo
  if (!rangeInfo || !rangeInfo.node.isConnected) {
    return
  }

  const selection = window.getSelection()
  if (!selection) {
    return
  }

  const range = document.createRange()
  range.setStart(rangeInfo.node, rangeInfo.startOffset)
  range.setEnd(rangeInfo.node, rangeInfo.endOffset)
  selection.removeAllRanges()
  selection.addRange(range)

  editor?.focus()

  const inserted = typeof document.execCommand === 'function'
    ? document.execCommand('insertText', false, replacement)
    : false

  if (!inserted) {
    const content = rangeInfo.node.textContent ?? ''
    rangeInfo.node.textContent =
      content.slice(0, rangeInfo.startOffset) +
      replacement +
      content.slice(rangeInfo.endOffset)

    const nextRange = document.createRange()
    nextRange.setStart(rangeInfo.node, rangeInfo.startOffset + replacement.length)
    nextRange.collapse(true)
    selection.removeAllRanges()
    selection.addRange(nextRange)
    dispatchEditorInputEvent(replacement)
  }
}

function applyLinkCandidate(candidate: NoteLinkCandidate) {
  replaceCurrentLinkQuery(`[[${candidate.title}]]`)
  hideLinkHint()

  window.setTimeout(() => {
    const latestValue = editor?.getValue()
    if (typeof latestValue === 'string') {
      emit('update:modelValue', latestValue)
    }
    scheduleOutlineSync()
  }, 0)
}

function positionLinkPreview(rect: DOMRect) {
  if (!host.value) {
    return
  }

  const hostRect = host.value.getBoundingClientRect()
  const maxWidth = 380
  const horizontalPadding = 16
  const left = rect.left - hostRect.left
  const top = rect.bottom - hostRect.top + 12
  const clampedLeft = Math.min(
    Math.max(horizontalPadding, left),
    Math.max(horizontalPadding, host.value.clientWidth - maxWidth - horizontalPadding),
  )

  linkPreviewState.left = clampedLeft
  linkPreviewState.top = top
}

async function renderLinkPreviewContent() {
  const previewElement = linkPreviewBody.value
  const previewData = linkPreviewState.data

  if (!previewElement || !previewData || previewData.isBroken) {
    return
  }

  const markdown = stripLeadingTitleHeading(
    previewData.title,
    previewData.markdownContent || '',
  ).trim()

  previewElement.innerHTML = ''

  await Vditor.preview(previewElement, markdown || '该笔记暂无正文。', {
    mode: normalizeThemeName(props.contentTheme, DEFAULT_CONTENT_THEME) === 'dark' ? 'dark' : 'light',
    hljs: {
      style: normalizeThemeName(props.codeTheme, DEFAULT_CODE_THEME),
    },
    markdown: {
      toc: false,
      mark: true,
    },
    theme: {
      current: normalizeThemeName(props.contentTheme, DEFAULT_CONTENT_THEME),
    },
  })
}

async function openLinkPreview(title: string, rect: DOMRect) {
  if (!fetchLinkPreview) {
    return
  }

  const normalizedTitle = title.trim()
  if (!normalizedTitle) {
    scheduleLinkPreviewHide()
    return
  }

  clearLinkPreviewHideTimer()
  positionLinkPreview(rect)

  if (linkPreviewState.visible && linkPreviewState.title === normalizedTitle && linkPreviewState.data) {
    return
  }

  linkPreviewState.visible = true
  linkPreviewState.loading = true
  linkPreviewState.title = normalizedTitle
  linkPreviewState.data = null

  const cached = linkPreviewCache.get(normalizedTitle)
  if (cached !== undefined) {
    linkPreviewState.data = cached
    linkPreviewState.loading = false
    if (cached && !cached.isBroken) {
      await nextTick()
      await renderLinkPreviewContent()
    }
    return
  }

  const requestId = ++linkPreviewRequestId

  try {
    const previewData = await fetchLinkPreview(normalizedTitle)
    if (requestId !== linkPreviewRequestId || linkPreviewState.title !== normalizedTitle) {
      return
    }

    linkPreviewCache.set(normalizedTitle, previewData)
    linkPreviewState.data = previewData
    if (previewData && !previewData.isBroken) {
      await nextTick()
      await renderLinkPreviewContent()
    }
  } catch {
    if (requestId !== linkPreviewRequestId) {
      return
    }

    linkPreviewState.data = null
  } finally {
    if (requestId === linkPreviewRequestId) {
      linkPreviewState.loading = false
    }
  }
}

async function navigateToLinkedNote(title: string) {
  if (!fetchLinkPreview || !openLinkedNote) {
    return
  }

  const normalizedTitle = title.trim()
  if (!normalizedTitle) {
    return
  }

  let previewData = linkPreviewCache.get(normalizedTitle)

  if (previewData === undefined) {
    try {
      previewData = await fetchLinkPreview(normalizedTitle)
      linkPreviewCache.set(normalizedTitle, previewData)
    } catch {
      return
    }
  }

  if (!previewData?.noteId || previewData.isBroken) {
    return
  }

  await openLinkedNote(previewData.noteId)
}

function handleEditorKeydown(event: KeyboardEvent) {
  if (!linkHint.visible) {
    return
  }

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    moveLinkHintSelection(1)
    return
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    moveLinkHintSelection(-1)
    return
  }

  if ((event.key === 'Enter' || event.key === 'Tab') && linkHint.items.length) {
    event.preventDefault()
    applyLinkCandidate(linkHint.items[linkHint.selectedIndex] ?? linkHint.items[0])
    return
  }

  if (event.key === 'Escape') {
    event.preventDefault()
    hideLinkHint()
  }
}

function handleHostMouseMove(event: MouseEvent) {
  if (!host.value || linkHint.visible) {
    return
  }

  const target = event.target as HTMLElement | null
  if (target?.closest('.note-link-preview')) {
    clearLinkPreviewHideTimer()
    return
  }

  const currentEditorElement = getCurrentEditorElement()
  const linkAtPoint = resolveWikiLinkRangeFromPoint(event.clientX, event.clientY)
  if (!linkAtPoint) {
    hoverLinkRange = null
    scheduleWikiLinkHighlightRefresh()
    if (currentEditorElement) {
      currentEditorElement.style.cursor = ''
    }
    scheduleLinkPreviewHide()
    return
  }

  hoverLinkRange = linkAtPoint.range.cloneRange()
  scheduleWikiLinkHighlightRefresh()
  if (currentEditorElement) {
    currentEditorElement.style.cursor = 'pointer'
  }
  void openLinkPreview(linkAtPoint.title, linkAtPoint.range.getBoundingClientRect())
}

function handleHostClick(event: MouseEvent) {
  if (linkHint.visible) {
    return
  }

  const target = event.target as HTMLElement | null
  if (target?.closest('.note-link-preview, .note-link-hint')) {
    return
  }

  const linkAtPoint = resolveWikiLinkRangeFromPoint(event.clientX, event.clientY)
  if (!linkAtPoint) {
    return
  }

  event.preventDefault()
  event.stopPropagation()
  void navigateToLinkedNote(linkAtPoint.title)
}

function handleSelectionChange() {
  void updateLinkHintFromSelection()

  if (linkHint.visible) {
    return
  }

  const range = getSelectionRange()
  if (!range || !range.collapsed || !isNodeInsideEditor(range.startContainer)) {
    selectionLinkRange = null
    scheduleWikiLinkHighlightRefresh()
    scheduleLinkPreviewHide()
    return
  }

  const textPosition = resolveTextPositionFromRange(range)
  if (!textPosition) {
    selectionLinkRange = null
    scheduleWikiLinkHighlightRefresh()
    scheduleLinkPreviewHide()
    return
  }

  const match = findWikiLinkMatch(textPosition.node.textContent ?? '', textPosition.offset)
  if (!match) {
    selectionLinkRange = null
    scheduleWikiLinkHighlightRefresh()
    scheduleLinkPreviewHide()
    return
  }

  const previewRange = document.createRange()
  previewRange.setStart(textPosition.node, match.start)
  previewRange.setEnd(textPosition.node, match.end)
  selectionLinkRange = previewRange.cloneRange()
  scheduleWikiLinkHighlightRefresh()
  void openLinkPreview(match.title, previewRange.getBoundingClientRect())
}

function handleHostMouseLeave() {
  hoverLinkRange = null
  scheduleWikiLinkHighlightRefresh()
  const currentEditorElement = getCurrentEditorElement()
  if (currentEditorElement) {
    currentEditorElement.style.cursor = ''
  }
  scheduleLinkPreviewHide()
}

function handleWikiLinkViewportChange() {
  scheduleWikiLinkHighlightRefresh()
}

defineExpose({
  scrollToHeading,
  getThemeSettings: getPreviewThemeSettings,
})

onMounted(() => {
  if (!host.value || !editorMount.value) {
    return
  }

  editor = new Vditor(editorMount.value, {
    height: '100%',
    lang: 'zh_CN',
    mode: 'wysiwyg',
    toolbar: editorToolbar,
    toolbarConfig: {
      pin: true,
    },
    theme: 'classic',
    preview: {
      maxWidth: COMPACT_EDITOR_MAX_WIDTH,
      theme: {
        current: normalizeThemeName(props.contentTheme, DEFAULT_CONTENT_THEME),
      },
      markdown: {
        toc: true,
        mark: true,
      },
      hljs: {
        lineNumber: true,
        style: normalizeThemeName(props.codeTheme, DEFAULT_CODE_THEME),
      },
    },
    customWysiwygToolbar() {},
    cache: {
      enable: false,
    },
    upload: {
      accept: 'image/*',
      fieldName: 'file[]',
      filename(name: string) {
        return name
      },
      max: 10 * 1024 * 1024,
      multiple: true,
      url: buildNoteImageUploadUrl(resolveEditorNoteId()),
      extraData: {
        fileType: 'image',
      },
      setHeaders() {
        return buildUploadHeaders()
      },
      validate(files: File[]) {
        if (!resolveEditorNoteId()) {
          return '请先打开一篇笔记后再插入图片。'
        }

        if (!files.length) {
          return '请选择要上传的图片。'
        }

        const invalidFile = files.find((file) => !(file.type || '').startsWith('image/'))
        if (invalidFile) {
          return `仅支持插入图片文件：${invalidFile.name}`
        }

        return true
      },
      error(responseText: string) {
        editor?.tip(extractUploadErrorMessage(responseText))
      },
      success(_editorElement: HTMLPreElement, responseText: string) {
        handleUploadSuccess(responseText)
      },
    },
    placeholder: '在这里开始书写内容，使用 #、##、### 创建右侧目录，也可以输入 [[ 建立双链。',
    after() {
      if (!editor) {
        return
      }

      const initialValue = transformStandaloneMediaLinks(props.modelValue || '')
      editor.setValue(initialValue)
      if (initialValue !== (props.modelValue || '')) {
        emit('update:modelValue', initialValue)
      }
      syncToolbarTitles()
      syncFullscreenState()
      syncThemeSettings()
      syncUploadOptions()
      scheduleOutlineSync()
      scheduleWikiLinkHighlightRefresh()
    },
    input(value: string) {
      if (syncingFromProps || applyingMediaTransform) {
        return
      }

      const transformedValue = transformStandaloneMediaLinks(value)

      if (transformedValue !== value) {
        applyingMediaTransform = true
        editor?.setValue(transformedValue)
        emit('update:modelValue', transformedValue)
        nextTick(() => {
          scheduleOutlineSync()
          scheduleWikiLinkHighlightRefresh()
          void updateLinkHintFromSelection()
          applyingMediaTransform = false
        })
        return
      }

      emit('update:modelValue', transformedValue)
      nextTick(() => {
        scheduleOutlineSync()
        scheduleWikiLinkHighlightRefresh()
        void updateLinkHintFromSelection()
      })
    },
  })

  host.value.addEventListener('click', handlePotentialThemeChange)
  host.value.addEventListener('click', handleHostClick, true)
  host.value.addEventListener('keydown', handleEditorKeydown, true)
  host.value.addEventListener('mousemove', handleHostMouseMove)
  host.value.addEventListener('mouseleave', handleHostMouseLeave)
  host.value.addEventListener('scroll', handleWikiLinkViewportChange, true)
  window.addEventListener('resize', handleWikiLinkViewportChange)
  document.addEventListener('selectionchange', handleSelectionChange)

  fullscreenObserver = new MutationObserver(() => {
    syncFullscreenState()
  })

  fullscreenObserver.observe(editorMount.value, {
    attributes: true,
    attributeFilter: ['class'],
  })
})

watch(
  () => props.modelValue,
  (value) => {
    if (!editor) {
      return
    }

    const normalizedValue = transformStandaloneMediaLinks(value || '')

    if (normalizedValue === editor.getValue()) {
      if (normalizedValue !== (value || '')) {
        emit('update:modelValue', normalizedValue)
      }
      return
    }

    syncingFromProps = true
    editor.setValue(normalizedValue)
    scheduleOutlineSync()
    scheduleWikiLinkHighlightRefresh()
    if (normalizedValue !== (value || '')) {
      emit('update:modelValue', normalizedValue)
    }
    window.setTimeout(() => {
      syncingFromProps = false
    }, 120)
  },
)

watch(
  () => resolveEditorNoteId(),
  () => {
    syncUploadOptions()
  },
)

watch(
  () => [props.contentTheme, props.codeTheme] as const,
  ([contentTheme, codeTheme]) => {
    if (!editor) {
      return
    }

    const nextContentTheme = normalizeThemeName(contentTheme, DEFAULT_CONTENT_THEME)
    const nextCodeTheme = normalizeThemeName(codeTheme, DEFAULT_CODE_THEME)
    const currentSettings = getPreviewThemeSettings()

    if (
      currentSettings.contentTheme === nextContentTheme &&
      currentSettings.codeTheme === nextCodeTheme
    ) {
      return
    }

    const editorTheme = editor.vditor.options.theme === 'dark' ? 'dark' : 'classic'
    editor.setTheme(editorTheme, nextContentTheme, nextCodeTheme)

    nextTick(() => {
      syncToolbarTitles()
      syncThemeSettings()
      scheduleOutlineSync()
      scheduleWikiLinkHighlightRefresh()
      if (linkPreviewState.visible && linkPreviewState.data && !linkPreviewState.data.isBroken) {
        void renderLinkPreviewContent()
      }
    })
  },
)

watch(
  () => linkPreviewState.data,
  (value) => {
    if (linkPreviewState.visible && value && !value.isBroken) {
      nextTick(() => {
        void renderLinkPreviewContent()
      })
    }
  },
)

onBeforeUnmount(() => {
  if (outlineTimer) {
    window.clearTimeout(outlineTimer)
  }

  if (pendingThemeSyncTimer) {
    window.clearTimeout(pendingThemeSyncTimer)
  }

  if (wikiLinkHighlightTimer) {
    window.clearTimeout(wikiLinkHighlightTimer)
  }

  clearLinkPreviewHideTimer()
  wikiLinkBoxes.value = []
  activeWikiLinkBoxes.value = []
  document.removeEventListener('selectionchange', handleSelectionChange)
  host.value?.removeEventListener('click', handlePotentialThemeChange)
  host.value?.removeEventListener('click', handleHostClick, true)
  host.value?.removeEventListener('keydown', handleEditorKeydown, true)
  host.value?.removeEventListener('mousemove', handleHostMouseMove)
  host.value?.removeEventListener('mouseleave', handleHostMouseLeave)
  host.value?.removeEventListener('scroll', handleWikiLinkViewportChange, true)
  window.removeEventListener('resize', handleWikiLinkViewportChange)
  fullscreenObserver?.disconnect()
  fullscreenObserver = null
  emit('fullscreen-change', false)
  editor?.destroy()
  editor = null
})
</script>

<template>
  <div ref="host" class="editor-shell">
    <div class="wiki-link-highlight-layer" aria-hidden="true">
      <span
        v-for="box in wikiLinkBoxes"
        :key="box.id"
        class="wiki-link-highlight-box"
        :style="{
          left: `${box.left}px`,
          top: `${box.top}px`,
          width: `${box.width}px`,
          height: `${box.height}px`,
        }"
      ></span>
      <span
        v-for="box in activeWikiLinkBoxes"
        :key="box.id"
        class="wiki-link-highlight-box active"
        :style="{
          left: `${box.left}px`,
          top: `${box.top}px`,
          width: `${box.width}px`,
          height: `${box.height}px`,
        }"
      ></span>
    </div>

    <div ref="editorMount" class="editor-host"></div>

    <div
      v-if="linkHint.visible"
      class="note-link-hint"
      :style="{
        left: `${linkHint.left}px`,
        top: `${linkHint.top}px`,
      }"
    >
      <div v-if="linkHint.loading" class="note-link-hint__state">正在检索当前知识库的笔记...</div>
      <template v-else-if="linkHint.items.length">
        <button
          v-for="(item, index) in linkHint.items"
          :key="item.noteId"
          type="button"
          class="note-link-hint__item"
          :class="{ active: index === linkHint.selectedIndex }"
          @mousedown.prevent="applyLinkCandidate(item)"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.noteId }}</span>
        </button>
      </template>
      <div v-else class="note-link-hint__state">当前知识库下没有匹配的笔记。</div>
    </div>

    <div
      v-if="linkPreviewState.visible"
      class="note-link-preview"
      :style="{
        left: `${linkPreviewState.left}px`,
        top: `${linkPreviewState.top}px`,
      }"
      @mouseenter="clearLinkPreviewHideTimer"
      @mouseleave="scheduleLinkPreviewHide"
    >
      <div class="note-link-preview__header">
        <strong>{{ linkPreviewState.data?.title || linkPreviewState.title }}</strong>
        <span
          class="note-link-preview__badge"
          :class="{ broken: Boolean(linkPreviewState.data?.isBroken) }"
        >
          {{ linkPreviewState.data?.isBroken ? '失效双链' : '笔记预览' }}
        </span>
      </div>

      <div v-if="linkPreviewState.loading" class="note-link-preview__state">正在加载预览...</div>
      <div v-else-if="linkPreviewState.data?.isBroken" class="note-link-preview__state">
        未找到同标题笔记，当前双链为失效状态。
      </div>
      <div
        v-else-if="linkPreviewState.data"
        ref="linkPreviewBody"
        class="note-link-preview__body"
      ></div>
      <div v-else class="note-link-preview__state">暂时无法获取预览内容。</div>
    </div>
  </div>
</template>

<style scoped>
.editor-shell {
  position: relative;
  min-height: 420px;
  height: 100%;
  border-radius: 1.6rem;
  overflow: visible;
}

.editor-host {
  position: relative;
  z-index: 2;
  min-height: 420px;
  height: 100%;
  border-radius: 1.6rem;
  overflow: visible;
}

.wiki-link-highlight-layer {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
  overflow: hidden;
  border-radius: 1.6rem;
}

.wiki-link-highlight-box {
  position: absolute;
  border-radius: 0.45rem;
  background: rgba(15, 118, 110, 0.12);
  box-shadow: inset 0 -2px 0 rgba(15, 118, 110, 0.42);
}

.wiki-link-highlight-box.active {
  background: rgba(15, 118, 110, 0.22);
  box-shadow: inset 0 -2px 0 rgba(13, 148, 136, 0.9), 0 0 0 1px rgba(15, 118, 110, 0.14);
}

.editor-host :deep(.vditor-toolbar) {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  padding-left: 0.75rem !important;
  padding-right: 0.75rem !important;
}

.editor-host :deep(.vditor-toolbar__br) {
  flex-basis: 100%;
}

.editor-host :deep(.vditor-reset video),
.editor-host :deep(.vditor-reset audio),
.editor-host :deep(.vditor-reset iframe),
.editor-host :deep(.vditor-wysiwyg__preview video),
.editor-host :deep(.vditor-wysiwyg__preview audio),
.editor-host :deep(.vditor-wysiwyg__preview iframe) {
  display: block;
  width: min(100%, 720px);
  max-width: 720px;
  margin: 0.75rem auto;
  border: 0;
  border-radius: 1rem;
  background: #0f172a;
}

.editor-host :deep(.iframe__video) {
  aspect-ratio: 16 / 9;
  min-height: auto;
}

.editor-host :deep(.vditor-reset video),
.editor-host :deep(.vditor-reset iframe),
.editor-host :deep(.vditor-wysiwyg__preview video),
.editor-host :deep(.vditor-wysiwyg__preview iframe) {
  aspect-ratio: 16 / 9;
  height: auto;
}

.note-link-hint,
.note-link-preview {
  position: absolute;
  z-index: 30;
  width: min(380px, calc(100% - 2rem));
  border: 1px solid rgba(23, 41, 74, 0.12);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(14px);
}

.note-link-hint {
  padding: 0.45rem;
}

.note-link-hint__item {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  border: 0;
  border-radius: 0.8rem;
  background: transparent;
  padding: 0.7rem 0.8rem;
  color: #0f172a;
  text-align: left;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.note-link-hint__item strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.95rem;
}

.note-link-hint__item span {
  flex-shrink: 0;
  color: #64748b;
  font-size: 0.72rem;
}

.note-link-hint__item.active,
.note-link-hint__item:hover {
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
}

.note-link-hint__state,
.note-link-preview__state {
  padding: 0.95rem 1rem;
  color: #475569;
  font-size: 0.9rem;
}

.note-link-preview {
  padding: 1rem;
}

.note-link-preview__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.85rem;
}

.note-link-preview__header strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0f172a;
  font-size: 0.98rem;
}

.note-link-preview__badge {
  flex-shrink: 0;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.12);
  padding: 0.28rem 0.62rem;
  color: #0f766e;
  font-size: 0.72rem;
  font-weight: 600;
}

.note-link-preview__badge.broken {
  background: rgba(185, 28, 28, 0.12);
  color: #b91c1c;
}

.note-link-preview__body {
  max-height: 320px;
  overflow: auto;
  color: #0f172a;
}

.note-link-preview__body :deep(.vditor-reset) {
  padding: 0 !important;
}

.note-link-preview__body :deep(pre) {
  max-width: 100%;
  overflow: auto;
}
</style>

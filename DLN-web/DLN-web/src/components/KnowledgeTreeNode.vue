<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'

defineOptions({ name: 'KnowledgeTreeNode' })

interface TagItem {
  id: string
  name: string
  createdTime?: string
}

interface TreeNode {
  id: string
  parentId: string | null
  name: string
  type: 'folder' | 'note'
  knowledgeBaseId: string
  createdTime?: string
  updatedTime?: string
  tags?: TagItem[]
  children?: TreeNode[]
}

const props = defineProps<{
  node: TreeNode
  selectedFolderId: string | null
  selectedNoteId: string | null
  showNoteMeta?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', node: TreeNode): void
  (e: 'create-folder', parentId: string | null): void
  (e: 'create-note', parentId: string | null): void
  (e: 'edit-folder', node: TreeNode): void
  (e: 'delete-folder', node: TreeNode): void
  (e: 'delete-note', node: TreeNode): void
}>()

const rootRef = ref<HTMLElement | null>(null)
const expanded = ref(true)
const menuOpen = ref(false)

const isFolder = computed(() => props.node.type === 'folder')
const hasChildren = computed(() => Boolean(props.node.children?.length))
const isActive = computed(() =>
  isFolder.value ? props.selectedFolderId === props.node.id : props.selectedNoteId === props.node.id,
)
const nodeKindLabel = computed(() => (isFolder.value ? '夹' : '记'))

const noteTags = computed(() => props.node.tags ?? [])
const noteMetaEnabled = computed(() => Boolean(props.showNoteMeta))
const showNoteMeta = computed(() => noteMetaEnabled.value && !isFolder.value)

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

function formatDateTime(value?: string) {
  if (!value) {
    return '未记录'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return dateTimeFormatter.format(date)
}

function closeMenu() {
  menuOpen.value = false
}

function toggleExpand() {
  if (isFolder.value && hasChildren.value) {
    expanded.value = !expanded.value
  }

  closeMenu()
}

function selectNode() {
  closeMenu()
  emit('select', props.node)
}

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

function createFolder() {
  emit('create-folder', props.node.id)
  closeMenu()
}

function createNote() {
  emit('create-note', props.node.id)
  closeMenu()
}

function editFolder() {
  emit('edit-folder', props.node)
  closeMenu()
}

function deleteFolder() {
  emit('delete-folder', props.node)
  closeMenu()
}

function deleteNote() {
  emit('delete-note', props.node)
  closeMenu()
}

function handleDocumentPointerDown(event: PointerEvent) {
  const target = event.target

  if (!(target instanceof Node)) {
    return
  }

  if (!rootRef.value?.contains(target)) {
    closeMenu()
  }
}

watch(menuOpen, (open) => {
  if (typeof document === 'undefined') {
    return
  }

  if (open) {
    document.addEventListener('pointerdown', handleDocumentPointerDown)
    return
  }

  document.removeEventListener('pointerdown', handleDocumentPointerDown)
})

onBeforeUnmount(() => {
  if (typeof document !== 'undefined') {
    document.removeEventListener('pointerdown', handleDocumentPointerDown)
  }
})
</script>

<template>
  <div ref="rootRef" class="tree-node">
    <div class="tree-row" :class="{ active: isActive, 'menu-open': menuOpen }">
      <button type="button" class="tree-main" @click="selectNode">
        <span class="caret" :class="{ ghost: !isFolder || !hasChildren }" @click.stop="toggleExpand">
          {{ isFolder ? (expanded ? '▾' : '▸') : '•' }}
        </span>
        <span class="kind-badge" :class="node.type">{{ nodeKindLabel }}</span>
        <span class="tree-copy" :class="{ 'with-meta': showNoteMeta }">
          <span class="label">{{ node.name }}</span>

          <div v-if="showNoteMeta" class="tree-tag-list tree-note-tags">
            <span v-for="tag in noteTags" :key="tag.id" class="tree-tag"># {{ tag.name }}</span>
            <span v-if="!noteTags.length" class="tree-tag empty">暂无标签</span>
          </div>

          <div v-if="showNoteMeta" class="tree-time-list tree-note-times">
            <span>创建 {{ formatDateTime(node.createdTime) }}</span>
            <span>修改 {{ formatDateTime(node.updatedTime || node.createdTime) }}</span>
          </div>
        </span>
      </button>

      <div class="actions">
        <button
          type="button"
          class="action-menu-trigger"
          aria-haspopup="menu"
          :aria-expanded="menuOpen"
          :title="isFolder ? '更多文件夹操作' : '更多笔记操作'"
          @click.stop="toggleMenu"
        >
          ...
        </button>

        <div v-if="menuOpen" class="action-menu" role="menu" @click.stop>
          <template v-if="isFolder">
            <button type="button" class="action-menu-item" role="menuitem" @click="createFolder">
              新建文件夹
            </button>
            <button type="button" class="action-menu-item" role="menuitem" @click="createNote">
              新建笔记
            </button>
            <button type="button" class="action-menu-item" role="menuitem" @click="editFolder">
              重命名
            </button>
            <button type="button" class="action-menu-item danger" role="menuitem" @click="deleteFolder">
              删除
            </button>
          </template>

          <template v-else>
            <button type="button" class="action-menu-item danger" role="menuitem" @click="deleteNote">
              删除
            </button>
          </template>
        </div>
      </div>
    </div>

    <div v-if="isFolder && expanded && hasChildren" class="children">
      <KnowledgeTreeNode
        v-for="child in node.children"
        :key="`${child.type}-${child.id}`"
        :node="child"
        :selected-folder-id="selectedFolderId"
        :selected-note-id="selectedNoteId"
        :show-note-meta="noteMetaEnabled"
        @select="emit('select', $event)"
        @create-folder="emit('create-folder', $event)"
        @create-note="emit('create-note', $event)"
        @edit-folder="emit('edit-folder', $event)"
        @delete-folder="emit('delete-folder', $event)"
        @delete-note="emit('delete-note', $event)"
      />
    </div>
  </div>
</template>

<style scoped>
.tree-node {
  display: grid;
  gap: 0.35rem;
}

.tree-row {
  position: relative;
  display: block;
  padding: 0.18rem 0;
  border-radius: 1rem;
  z-index: 0;
}

.tree-row.menu-open {
  z-index: 6;
}

.tree-row.active {
  background:
    linear-gradient(135deg, rgba(24, 91, 84, 0.16), rgba(200, 141, 61, 0.16)),
    rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 0 0 1px rgba(31, 79, 76, 0.1);
}

.tree-main {
  width: 100%;
  display: grid;
  grid-template-columns: 1rem 1.7rem minmax(0, 1fr);
  align-items: start;
  gap: 0.5rem;
  border: 0;
  background: transparent;
  color: inherit;
  padding: 0.6rem 3rem 0.6rem 0.72rem;
  text-align: left;
  border-radius: 0.95rem;
}

.tree-main:hover {
  background: rgba(34, 77, 74, 0.06);
}

.caret {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(34, 69, 70, 0.82);
  font-size: 0.9rem;
}

.caret.ghost {
  opacity: 0.34;
}

.kind-badge {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 0.55rem;
  display: grid;
  place-items: center;
  font-size: 0.66rem;
  font-weight: 700;
}

.kind-badge.folder {
  background: rgba(214, 147, 53, 0.16);
  color: #976425;
}

.kind-badge.note {
  background: rgba(36, 89, 85, 0.14);
  color: #285855;
}

.label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.87rem;
}

.tree-copy {
  min-width: 0;
  display: grid;
  gap: 0.42rem;
}

.tree-copy.with-meta {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.75rem;
}

.tree-tag-list,
.tree-time-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.36rem;
  min-width: 0;
}

.tree-note-tags {
  justify-content: center;
  justify-self: center;
}

.tree-note-times {
  justify-content: flex-end;
  justify-self: end;
}

.tree-tag,
.tree-time-list span {
  border-radius: 999px;
  padding: 0.18rem 0.52rem;
  font-size: 0.72rem;
  line-height: 1.3;
}

.tree-tag {
  background: rgba(37, 87, 84, 0.12);
  color: #255754;
  box-shadow: inset 0 0 0 1px rgba(37, 87, 84, 0.08);
}

.tree-tag.empty {
  background: rgba(127, 151, 149, 0.12);
  color: rgba(24, 54, 56, 0.58);
}

.tree-time-list span {
  background: rgba(255, 255, 255, 0.76);
  color: rgba(24, 54, 56, 0.66);
  box-shadow: inset 0 0 0 1px rgba(32, 67, 63, 0.06);
}

.actions {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
}

.action-menu-trigger {
  width: 1.9rem;
  height: 1.9rem;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #214546;
  font-size: 0.84rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  box-shadow: 0 10px 22px rgba(17, 38, 38, 0.08);
  opacity: 0;
  transform: scale(0.92);
  transition:
    opacity 140ms ease,
    transform 140ms ease,
    background 140ms ease,
    color 140ms ease;
}

.tree-row:hover .action-menu-trigger,
.tree-row.active .action-menu-trigger,
.tree-row.menu-open .action-menu-trigger {
  opacity: 1;
  transform: scale(1);
}

.action-menu-trigger:hover {
  background: rgba(255, 255, 255, 1);
  color: #163738;
}

.action-menu {
  position: absolute;
  top: calc(100% + 0.35rem);
  right: 0;
  min-width: 8.8rem;
  display: grid;
  gap: 0.2rem;
  padding: 0.35rem;
  border-radius: 0.95rem;
  background: rgba(255, 252, 247, 0.98);
  border: 1px solid rgba(35, 77, 72, 0.12);
  box-shadow: 0 18px 40px rgba(18, 42, 41, 0.16);
  backdrop-filter: blur(14px);
}

.action-menu-item {
  width: 100%;
  border: 0;
  background: transparent;
  color: #214546;
  border-radius: 0.72rem;
  padding: 0.5rem 0.72rem;
  font-size: 0.74rem;
  font-weight: 700;
  text-align: left;
}

.action-menu-item:hover {
  background: rgba(34, 77, 74, 0.08);
}

.action-menu-item.danger {
  color: #8f3d33;
}

.children {
  margin-left: 0.95rem;
  border-left: 1px dashed rgba(44, 87, 83, 0.18);
  padding-left: 0.7rem;
  display: grid;
  gap: 0.22rem;
}
</style>

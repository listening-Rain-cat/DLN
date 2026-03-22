<script setup lang="ts">
import { computed, ref } from 'vue'

defineOptions({ name: 'KnowledgeTreeNode' })

interface TreeNode {
  id: string
  parentId: string | null
  name: string
  type: 'folder' | 'note'
  knowledgeBaseId: string
  updatedTime?: string
  children?: TreeNode[]
}

const props = defineProps<{
  node: TreeNode
  selectedFolderId: string | null
  selectedNoteId: string | null
}>()

const emit = defineEmits<{
  (e: 'select', node: TreeNode): void
  (e: 'create-folder', parentId: string | null): void
  (e: 'create-note', parentId: string | null): void
  (e: 'delete-folder', node: TreeNode): void
  (e: 'delete-note', node: TreeNode): void
}>()

const expanded = ref(true)
const isFolder = computed(() => props.node.type === 'folder')
const isActive = computed(() =>
  isFolder.value ? props.selectedFolderId === props.node.id : props.selectedNoteId === props.node.id,
)

function toggleExpand() {
  if (isFolder.value) {
    expanded.value = !expanded.value
  }
}

function selectNode() {
  emit('select', props.node)
}
</script>

<template>
  <div class="tree-node">
    <div class="tree-row" :class="{ active: isActive }">
      <button type="button" class="tree-main" @click="selectNode">
        <span class="caret" :class="{ ghost: !isFolder }" @click.stop="toggleExpand">
          {{ isFolder ? (expanded ? '-' : '+') : '·' }}
        </span>
        <span class="icon">{{ isFolder ? '文件夹' : '笔记' }}</span>
        <span class="label">{{ node.name }}</span>
      </button>

      <div class="actions">
        <template v-if="isFolder">
          <button type="button" class="action-button" title="新建文件夹" @click="emit('create-folder', node.id)">
            新建夹
          </button>
          <button type="button" class="action-button" title="新建笔记" @click="emit('create-note', node.id)">
            新建记
          </button>
          <button type="button" class="action-button danger" title="删除文件夹" @click="emit('delete-folder', node)">
            删除
          </button>
        </template>

        <template v-else>
          <button type="button" class="action-button danger" title="删除笔记" @click="emit('delete-note', node)">
            删除
          </button>
        </template>
      </div>
    </div>

    <div v-if="isFolder && expanded && node.children?.length" class="children">
      <KnowledgeTreeNode
        v-for="child in node.children"
        :key="`${child.type}-${child.id}`"
        :node="child"
        :selected-folder-id="selectedFolderId"
        :selected-note-id="selectedNoteId"
        @select="emit('select', $event)"
        @create-folder="emit('create-folder', $event)"
        @create-note="emit('create-note', $event)"
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
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.45rem;
  padding: 0.2rem 0;
  border-radius: 0.95rem;
}

.tree-row.active {
  background:
    linear-gradient(135deg, rgba(31, 107, 100, 0.16), rgba(235, 165, 70, 0.14)),
    rgba(255, 255, 255, 0.82);
  box-shadow: inset 0 0 0 1px rgba(39, 83, 81, 0.14);
}

.tree-main {
  width: 100%;
  display: grid;
  grid-template-columns: 1.1rem 2.8rem minmax(0, 1fr);
  align-items: center;
  gap: 0.35rem;
  border: 0;
  background: transparent;
  color: inherit;
  padding: 0.55rem 0.65rem;
  text-align: left;
  border-radius: 0.9rem;
}

.tree-main:hover {
  background: rgba(39, 83, 81, 0.08);
}

.caret {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(36, 62, 66, 0.8);
  font-size: 0.9rem;
}

.caret.ghost {
  opacity: 0.4;
}

.icon {
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: rgba(24, 54, 56, 0.62);
}

.label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.95rem;
}

.actions {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding-right: 0.45rem;
  opacity: 0;
  transition: opacity 140ms ease;
}

.tree-row:hover .actions,
.tree-row.active .actions {
  opacity: 1;
}

.action-button {
  border: 0;
  background: rgba(255, 255, 255, 0.82);
  color: #214546;
  border-radius: 999px;
  padding: 0.22rem 0.45rem;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  box-shadow: 0 10px 22px rgba(17, 38, 38, 0.08);
}

.action-button.danger {
  color: #8f3d33;
}

.children {
  margin-left: 1.05rem;
  border-left: 1px dashed rgba(44, 87, 83, 0.18);
  padding-left: 0.7rem;
  display: grid;
  gap: 0.25rem;
}
</style>

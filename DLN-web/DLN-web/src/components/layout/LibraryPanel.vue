<script setup lang="ts">
import { computed } from 'vue'
import KnowledgeTreeNode from '../KnowledgeTreeNode.vue'

type Id = string

interface TagItem {
  id: Id
  name: string
  createdTime?: string
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

const props = defineProps<{
  userName: string
  selectedKnowledgeBaseId: Id | null
  selectedKnowledgeBaseName: string
  selectedKnowledgeBaseDescription: string
  treeNodes: TreeNode[]
  selectedFolderId: Id | null
  selectedNoteId: Id | null
  loadingTree: boolean
  hasCurrentNote: boolean
}>()

defineEmits<{
  (e: 'create-folder', parentId: Id | null): void
  (e: 'create-note', parentId: Id | null): void
  (e: 'edit-folder', node: TreeNode): void
  (e: 'select-tree-node', node: TreeNode): void
  (e: 'delete-folder', payload: { id: Id; name: string }): void
  (e: 'delete-note', payload: { id: Id; name: string }): void
  (e: 'exit-knowledge-base'): void
  (e: 'open-knowledge-base-directory'): void
  (e: 'toggle-collapse'): void
}>()

function countNodes(nodes: TreeNode[], type: TreeNode['type']): number {
  return nodes.reduce((total, node) => {
    const selfCount = node.type === type ? 1 : 0
    const childCount = node.children?.length ? countNodes(node.children, type) : 0
    return total + selfCount + childCount
  }, 0)
}

const folderCount = computed(() => countNodes(props.treeNodes, 'folder'))
const noteCount = computed(() => countNodes(props.treeNodes, 'note'))
const showResourceDirectory = computed(() => Boolean(props.selectedKnowledgeBaseId && props.hasCurrentNote))

const treeDescription = computed(() => {
  if (!props.selectedKnowledgeBaseId) {
    return '请先在主页中选择一个知识库。'
  }

  if (!props.hasCurrentNote) {
    return props.selectedKnowledgeBaseDescription || '当前位于知识库页面，资源目录已隐藏，打开笔记后会在左侧显示。'
  }

  return props.selectedKnowledgeBaseDescription || '当前面板显示所选知识库的资源目录，可继续浏览与维护内容。'
})
</script>

<template>
  <aside class="library-panel">
    <section class="tree-shell library-tree-shell" :class="{ 'no-resource-directory': !showResourceDirectory }">
      <div class="library-header-section">
        <div class="library-toolbar">
          <span class="library-toolbar-label">{{ userName || '当前用户' }}</span>
          <div class="library-toolbar-group">
            <button
              type="button"
              class="mini-button toolbar-button toolbar-icon-button"
              :disabled="!selectedKnowledgeBaseId"
              title="在根目录新建文件夹"
              @click="$emit('create-folder', null)"
            >
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
            <button
              type="button"
              class="mini-button toolbar-button toolbar-icon-button accent"
              :disabled="!selectedKnowledgeBaseId"
              title="在根目录新建笔记"
              @click="$emit('create-note', null)"
            >
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
            <button
              v-if="showResourceDirectory"
              type="button"
              class="mini-button toolbar-button toolbar-icon-button"
              title="收起左侧面板"
              @click="$emit('toggle-collapse')"
            >
              <svg class="toolbar-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M5.75 5.75h3.5v12.5h-3.5zM14.75 8.25 11.5 12l3.25 3.75"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.8"
                />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <template v-if="showResourceDirectory">
        <div v-if="loadingTree" class="auth-empty">目录加载中...</div>
        <div v-else class="tree-shell-body">
          <div class="tree-shell-surface">
            <div class="tree-shell-heading">
              <span>资源目录</span>
              <strong>{{ treeNodes.length ? '展开文件夹继续浏览' : '从这里开始创建内容' }}</strong>
            </div>

            <div v-if="treeNodes.length" class="tree-list">
              <KnowledgeTreeNode
                v-for="node in treeNodes"
                :key="`${node.type}-${node.id}`"
                :node="node"
                :selected-folder-id="selectedFolderId"
                :selected-note-id="selectedNoteId"
                @select="$emit('select-tree-node', $event)"
                @create-folder="$emit('create-folder', $event)"
                @create-note="$emit('create-note', $event)"
                @edit-folder="$emit('edit-folder', $event)"
                @delete-folder="$emit('delete-folder', $event)"
                @delete-note="$emit('delete-note', $event)"
              />
            </div>
            <div v-else class="empty-card compact-card">当前知识库还是空的，先创建一个文件夹或笔记吧。</div>
          </div>
        </div>

        <div class="library-footer">
          <span>DLN Workspace</span>
          <button
            type="button"
            class="library-footer-button"
            :disabled="!selectedKnowledgeBaseId"
            @click="$emit('open-knowledge-base-directory')"
          >
            当前知识库目录
          </button>
        </div>
      </template>

      <div v-else class="library-tree-spacer" aria-hidden="true"></div>

      <div class="library-hero" :class="{ empty: !selectedKnowledgeBaseId }">
        <button
          v-if="selectedKnowledgeBaseId"
          type="button"
          class="mini-button library-hero-exit"
          title="退出当前知识库"
          @click="$emit('exit-knowledge-base')"
        >
          <svg class="toolbar-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M14 4.75H8A1.25 1.25 0 0 0 6.75 6v12c0 .69.56 1.25 1.25 1.25h6"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
            />
            <path
              d="M10.5 12h8.75M16.25 8.25 20 12l-3.75 3.75"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
            />
          </svg>
          <span>退出</span>
        </button>
        <div class="library-hero-copy">
          <p class="eyebrow">已打开知识库</p>
          <h2>{{ selectedKnowledgeBaseName || '请选择知识库' }}</h2>
          <p class="library-hero-description">{{ treeDescription }}</p>
        </div>
        <span class="library-state-pill">
          {{ selectedKnowledgeBaseId ? '已打开知识库' : '等待选择' }}
        </span>
        <div v-if="selectedKnowledgeBaseId" class="library-hero-stats">
          <span v-if="hasCurrentNote"><strong>{{ folderCount }}</strong> 文件夹</span>
          <span><strong>{{ noteCount }}</strong> 笔记</span>
        </div>
      </div>
    </section>
  </aside>
</template>

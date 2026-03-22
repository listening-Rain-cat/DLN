<script setup lang="ts">
import KnowledgeTreeNode from '../KnowledgeTreeNode.vue'

type Id = string

interface KnowledgeBase {
  id: Id
  name: string
  description?: string | null
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

defineProps<{
  knowledgeBases: KnowledgeBase[]
  selectedKnowledgeBaseId: Id | null
  selectedKnowledgeBaseName: string
  treeNodes: TreeNode[]
  selectedFolderId: Id | null
  selectedNoteId: Id | null
  loadingTree: boolean
  loadingKnowledgeBases: boolean
}>()

defineEmits<{
  (e: 'create-knowledge-base'): void
  (e: 'edit-knowledge-base', item: KnowledgeBase): void
  (e: 'delete-knowledge-base', payload: { id: Id; name: string }): void
  (e: 'select-knowledge-base', knowledgeBaseId: Id): void
  (e: 'create-folder', parentId: Id | null): void
  (e: 'create-note', parentId: Id | null): void
  (e: 'select-tree-node', node: TreeNode): void
  (e: 'delete-folder', payload: { id: Id; name: string }): void
  (e: 'delete-note', payload: { id: Id; name: string }): void
}>()
</script>

<template>
  <aside class="library-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">知识库</p>
        <h2>我的知识库</h2>
      </div>
      <button type="button" class="soft-button" @click="$emit('create-knowledge-base')">新建知识库</button>
    </div>

    <div class="knowledge-base-list">
      <article
        v-for="knowledgeBase in knowledgeBases"
        :key="knowledgeBase.id"
        class="knowledge-base-card"
        :class="{ active: selectedKnowledgeBaseId === knowledgeBase.id }"
      >
        <button type="button" class="knowledge-base-main" @click="$emit('select-knowledge-base', knowledgeBase.id)">
          <strong>{{ knowledgeBase.name }}</strong>
          <span>{{ knowledgeBase.description || '暂无描述' }}</span>
        </button>

        <div class="inline-actions">
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

      <div v-if="!knowledgeBases.length && !loadingKnowledgeBases" class="empty-card compact-card">
        还没有知识库，先创建一个开始整理你的笔记吧。
      </div>
    </div>

    <div class="tree-shell">
      <div class="panel-heading compact">
        <div>
          <p class="eyebrow">目录树</p>
          <h3>{{ selectedKnowledgeBaseName || '请选择知识库' }}</h3>
        </div>
        <div class="inline-actions">
          <button
            type="button"
            class="soft-button"
            :disabled="!selectedKnowledgeBaseId"
            @click="$emit('create-folder', null)"
          >
            新建文件夹
          </button>
          <button
            type="button"
            class="soft-button accent"
            :disabled="!selectedKnowledgeBaseId"
            @click="$emit('create-note', null)"
          >
            新建笔记
          </button>
        </div>
      </div>

      <div v-if="loadingTree" class="auth-empty">目录加载中...</div>
      <div v-else-if="selectedKnowledgeBaseId">
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
            @delete-folder="$emit('delete-folder', $event)"
            @delete-note="$emit('delete-note', $event)"
          />
        </div>
        <div v-else class="empty-card compact-card">当前知识库还是空的，先创建一个文件夹或笔记吧。</div>
      </div>
      <div v-else class="empty-card compact-card">请先从上方选择一个知识库。</div>
    </div>
  </aside>
</template>

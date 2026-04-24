<script setup lang="ts">
import MarkdownPreview from '../MarkdownPreview.vue'

interface NoteTemplate {
  id: string
  name: string
  description?: string | null
  templateContent: string
  createdTime?: string
  updatedTime?: string
}

defineProps<{
  loadingTemplates: boolean
  noteTemplates: NoteTemplate[]
  formatDateTime: (value?: string | null) => string
}>()

defineEmits<{
  (e: 'create-template'): void
  (e: 'edit-template', template: NoteTemplate): void
  (e: 'delete-template', payload: { id: string; name: string }): void
}>()
</script>

<template>
  <article class="main-panel template-section settings-template-panel">
    <div class="panel-heading compact">
      <div>
        <p class="eyebrow">通用模板</p>
        <h3>跨知识库笔记模板</h3>
        <p class="panel-copy">统一管理用户的通用模板，新建笔记时可直接选择套用。</p>
      </div>
      <button type="button" class="soft-button accent" @click="$emit('create-template')">新建模板</button>
    </div>

    <div v-if="loadingTemplates" class="empty-card compact-card">模板加载中...</div>

    <div v-else-if="noteTemplates.length" class="template-list">
      <article v-for="template in noteTemplates" :key="template.id" class="template-card">
        <div class="template-card-main">
          <div class="template-card-header">
            <strong>{{ template.name }}</strong>
            <span class="knowledge-base-entry-state">通用模板</span>
          </div>
          <p class="template-card-description">
            {{ template.description || '新建笔记时可直接套用这份模板结构。' }}
          </p>
          <div class="template-card-preview">
            <MarkdownPreview :markdown="template.templateContent" empty-text="模板正文为空。" />
          </div>
          <div class="template-card-meta">
            <span>创建于 {{ formatDateTime(template.createdTime) }}</span>
            <span>更新于 {{ formatDateTime(template.updatedTime || template.createdTime) }}</span>
          </div>
        </div>

        <div class="template-card-actions">
          <button type="button" class="mini-button" @click="$emit('edit-template', template)">编辑</button>
          <button
            type="button"
            class="mini-button danger"
            @click="$emit('delete-template', { id: template.id, name: template.name })"
          >
            删除
          </button>
        </div>
      </article>
    </div>

    <div v-else class="empty-card compact-card template-empty-state">
      <p>你还没有通用模板。</p>
      <p>建议先建立几份常用模板，例如会议纪要、阅读笔记、周报或项目复盘。</p>
      <button type="button" class="soft-button accent" @click="$emit('create-template')">创建第一个模板</button>
    </div>
  </article>
</template>

<style scoped>
.template-card-preview {
  max-height: 120px;
  overflow: auto;
}

.template-card-preview :deep(.vditor-reset) {
  font-size: 0.84rem;
  line-height: 1.6;
  color: rgba(24, 54, 56, 0.78);
}

.template-card-preview :deep(h1),
.template-card-preview :deep(h2),
.template-card-preview :deep(h3),
.template-card-preview :deep(h4),
.template-card-preview :deep(h5),
.template-card-preview :deep(h6) {
  margin-bottom: 0.45rem;
  color: #173637;
}

.template-card-preview :deep(p),
.template-card-preview :deep(ul),
.template-card-preview :deep(ol),
.template-card-preview :deep(blockquote) {
  margin: 0.35rem 0;
}

.template-card-preview :deep(pre) {
  margin: 0.45rem 0;
  padding: 0.65rem 0.72rem;
  border-radius: 0.72rem;
  background: rgba(30, 41, 59, 0.92);
}
</style>

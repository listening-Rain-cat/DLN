<script setup lang="ts">
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
  previewTemplateContent: (content: string) => string
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
        <p class="panel-copy">这里统一管理当前用户的通用模板，新建笔记时可直接选择套用。</p>
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
          <p class="template-card-preview">{{ previewTemplateContent(template.templateContent) }}</p>
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

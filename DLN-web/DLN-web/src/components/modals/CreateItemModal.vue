<script setup lang="ts">
interface NoteTemplateOption {
  id: string
  name: string
  description?: string | null
}

type CreateItemType = 'folder' | 'note'

defineProps<{
  modal: {
    type: CreateItemType
    name: string
    templateId: string | null
  }
  loadingTemplates: boolean
  noteTemplates: NoteTemplateOption[]
  selectedCreateTemplate: NoteTemplateOption | null
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'submit'): void
}>()
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-card">
      <div class="modal-heading">
        <p class="eyebrow">{{ modal.type === 'folder' ? '文件夹' : '笔记' }}</p>
        <h3>{{ modal.type === 'folder' ? '新建文件夹' : '新建笔记' }}</h3>
      </div>

      <label class="field">
        <span>{{ modal.type === 'folder' ? '文件夹名称' : '笔记标题' }}</span>
        <input
          v-model="modal.name"
          type="text"
          :placeholder="modal.type === 'folder' ? '例如：阅读清单' : '例如：Redis 基础'"
        />
      </label>

      <label v-if="modal.type === 'note'" class="field">
        <span>模板</span>
        <select v-model="modal.templateId">
          <option :value="null">不使用模板</option>
          <option v-for="template in noteTemplates" :key="template.id" :value="template.id">
            {{ template.name }}
          </option>
        </select>
      </label>

      <div v-if="modal.type === 'note'" class="template-selection-summary">
        <p class="template-selection-title">
          {{ loadingTemplates ? '模板加载中...' : selectedCreateTemplate ? '已选择模板' : '未选择模板' }}
        </p>
        <p class="template-selection-copy">
          {{
            selectedCreateTemplate?.description ||
            (selectedCreateTemplate
              ? '新建笔记后将自动写入该模板内容。'
              : noteTemplates.length
                ? '你也可以直接创建空白笔记。'
                : '你还没有通用模板，可在左侧模板页里补充。')
          }}
        </p>
      </div>

      <div class="modal-actions">
        <button type="button" class="soft-button" @click="$emit('close')">取消</button>
        <button type="button" class="soft-button accent" @click="$emit('submit')">
          {{ modal.type === 'folder' ? '创建文件夹' : '创建笔记' }}
        </button>
      </div>
    </div>
  </div>
</template>

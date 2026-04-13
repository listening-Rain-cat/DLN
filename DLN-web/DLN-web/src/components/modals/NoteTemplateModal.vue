<script setup lang="ts">
type TemplateModalMode = 'create' | 'edit'

defineProps<{
  modal: {
    mode: TemplateModalMode
    name: string
    description: string
    templateContent: string
  }
  submitting: boolean
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'submit'): void
}>()
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-card template-modal">
      <div class="modal-heading">
        <p class="eyebrow">模板</p>
        <h3>{{ modal.mode === 'create' ? '新建笔记模板' : '编辑笔记模板' }}</h3>
      </div>

      <label class="field">
        <span>模板名称</span>
        <input v-model="modal.name" type="text" placeholder="例如：周报模板" />
      </label>

      <label class="field">
        <span>模板描述</span>
        <textarea v-model="modal.description" rows="3" placeholder="说明这个模板适合什么场景，可选填写"></textarea>
      </label>

      <label class="field">
        <span>模板内容</span>
        <textarea
          v-model="modal.templateContent"
          rows="12"
          placeholder="# 标题&#10;&#10;## 今日进展&#10;- &#10;&#10;## 下一步计划&#10;- "
        ></textarea>
      </label>

      <div class="modal-actions">
        <button type="button" class="soft-button" :disabled="submitting" @click="$emit('close')">取消</button>
        <button type="button" class="soft-button accent" :disabled="submitting" @click="$emit('submit')">
          {{ submitting ? '保存中...' : modal.mode === 'create' ? '创建模板' : '保存模板' }}
        </button>
      </div>
    </div>
  </div>
</template>

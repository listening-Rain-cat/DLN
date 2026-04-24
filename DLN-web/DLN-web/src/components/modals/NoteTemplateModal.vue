<script setup lang="ts">
import { ref } from 'vue'
import MarkdownPreview from '../MarkdownPreview.vue'

type TemplateModalMode = 'create' | 'edit'

const props = defineProps<{
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

const contentView = ref<'preview' | 'source'>(props.modal.templateContent.trim() ? 'preview' : 'source')
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
        <div class="template-content-switcher" role="tablist" aria-label="模板内容显示模式">
          <button
            type="button"
            class="template-content-switcher-button"
            :class="{ active: contentView === 'preview' }"
            :aria-selected="contentView === 'preview'"
            @click="contentView = 'preview'"
          >
            渲染预览
          </button>
          <button
            type="button"
            class="template-content-switcher-button"
            :class="{ active: contentView === 'source' }"
            :aria-selected="contentView === 'source'"
            @click="contentView = 'source'"
          >
            编辑源码
          </button>
        </div>

        <div v-if="contentView === 'preview'" class="template-content-preview">
          <MarkdownPreview
            :markdown="modal.templateContent"
            empty-text="模板内容为空，切换到“编辑源码”后输入内容。"
          />
        </div>

        <textarea
          v-else
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

<style scoped>
.template-content-switcher {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  width: fit-content;
  padding: 0.22rem;
  border-radius: 999px;
  background: rgba(37, 87, 84, 0.08);
  border: 1px solid rgba(37, 87, 84, 0.12);
}

.template-content-switcher-button {
  border: 0;
  border-radius: 999px;
  padding: 0.42rem 0.82rem;
  background: transparent;
  color: rgba(24, 54, 56, 0.72);
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 1;
  transition:
    background 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease;
}

.template-content-switcher-button.active {
  background: linear-gradient(135deg, #255754, #3d7f73);
  color: #fff8f1;
  box-shadow: 0 10px 24px rgba(37, 87, 84, 0.16);
}

.template-content-preview {
  min-height: 320px;
  max-height: 420px;
  overflow: auto;
  padding: 1rem;
  border: 1px solid rgba(33, 73, 70, 0.12);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.86);
}

.template-content-preview :deep(.vditor-reset) {
  color: #173637;
  line-height: 1.7;
}

.template-content-preview :deep(pre) {
  padding: 0.9rem 1rem;
  border-radius: 0.8rem;
  background: rgba(30, 41, 59, 0.94);
}
</style>

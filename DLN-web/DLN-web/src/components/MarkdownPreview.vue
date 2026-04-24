<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

const props = withDefaults(
  defineProps<{
    markdown?: string | null
    emptyText?: string
    contentTheme?: string
    codeTheme?: string
  }>(),
  {
    markdown: '',
    emptyText: '暂无内容。',
    contentTheme: 'light',
    codeTheme: 'github',
  },
)

const previewHost = ref<HTMLDivElement | null>(null)
let renderTaskId = 0

function normalizeThemeMode(theme: string) {
  return theme === 'dark' ? 'dark' : 'light'
}

async function renderPreview() {
  const host = previewHost.value
  if (!host) {
    return
  }

  const currentTaskId = ++renderTaskId
  host.innerHTML = ''

  await nextTick()

  const markdown = (props.markdown || '').trim()

  try {
    await Vditor.preview(host, markdown || props.emptyText, {
      mode: normalizeThemeMode(props.contentTheme),
      hljs: {
        style: props.codeTheme,
      },
      markdown: {
        toc: false,
        mark: true,
      },
      theme: {
        current: props.contentTheme,
      },
    })
  } catch {
    if (currentTaskId !== renderTaskId) {
      return
    }

    host.textContent = markdown || props.emptyText
    return
  }

  if (currentTaskId !== renderTaskId) {
    host.innerHTML = ''
  }
}

watch(
  () => [props.markdown, props.contentTheme, props.codeTheme] as const,
  () => {
    void renderPreview()
  },
  { immediate: true },
)

onMounted(() => {
  void renderPreview()
})

onBeforeUnmount(() => {
  renderTaskId += 1

  if (previewHost.value) {
    previewHost.value.innerHTML = ''
  }
})
</script>

<template>
  <div class="markdown-preview">
    <div ref="previewHost" class="markdown-preview-body"></div>
  </div>
</template>

<style scoped>
.markdown-preview {
  min-width: 0;
  min-height: 0;
}

.markdown-preview-body {
  min-width: 0;
  min-height: 0;
}

.markdown-preview-body :deep(.vditor-reset) {
  padding: 0 !important;
  color: inherit;
}

.markdown-preview-body :deep(.vditor-reset > :first-child) {
  margin-top: 0;
}

.markdown-preview-body :deep(.vditor-reset > :last-child) {
  margin-bottom: 0;
}

.markdown-preview-body :deep(img),
.markdown-preview-body :deep(video),
.markdown-preview-body :deep(audio),
.markdown-preview-body :deep(iframe) {
  max-width: 100%;
}

.markdown-preview-body :deep(pre) {
  max-width: 100%;
  overflow: auto;
}

.markdown-preview-body :deep(table) {
  width: 100%;
  display: block;
  overflow-x: auto;
}
</style>

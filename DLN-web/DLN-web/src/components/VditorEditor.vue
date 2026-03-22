<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

interface OutlineItem {
  id: string
  text: string
  level: number
}

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'outline-change', outline: OutlineItem[]): void
}>()

const host = ref<HTMLDivElement | null>(null)
let editor: Vditor | null = null
let syncingFromProps = false

function syncOutline() {
  if (!host.value) {
    emit('outline-change', [])
    return
  }

  const preview = host.value.querySelector('.vditor-preview') as HTMLElement | null
  if (!preview) {
    emit('outline-change', [])
    return
  }

  const headings = Array.from(preview.querySelectorAll('h1, h2, h3, h4, h5, h6')) as HTMLElement[]
  const outline = headings.map((heading, index) => {
    const id = `outline-heading-${index}`
    heading.id = id
    return {
      id,
      text: heading.textContent?.trim() || '未命名标题',
      level: Number(heading.tagName.replace('H', '')),
    }
  })

  emit('outline-change', outline)
}

function scheduleOutlineSync() {
  window.setTimeout(() => {
    syncOutline()
  }, 80)
}

function scrollToHeading(id: string) {
  if (!host.value) {
    return
  }

  const preview = host.value.querySelector('.vditor-preview') as HTMLElement | null
  const target = preview?.querySelector<HTMLElement>(`#${id}`)
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

defineExpose({
  scrollToHeading,
})

onMounted(() => {
  if (!host.value) {
    return
  }

  editor = new Vditor(host.value, {
    height: '100%',
    mode: 'sv',
    cache: {
      enable: false,
    },
    placeholder: '在这里开始书写内容，使用 #、##、### 创建右侧目录。',
    after() {
      if (!editor) {
        return
      }
      editor.setValue(props.modelValue || '')
      scheduleOutlineSync()
    },
    input(value: string) {
      if (syncingFromProps) {
        return
      }
      emit('update:modelValue', value)
      nextTick(() => {
        scheduleOutlineSync()
      })
    },
  })
})

watch(
  () => props.modelValue,
  (value) => {
    if (!editor) {
      return
    }

    if (value === editor.getValue()) {
      return
    }

    syncingFromProps = true
    editor.setValue(value || '')
    scheduleOutlineSync()
    window.setTimeout(() => {
      syncingFromProps = false
    }, 120)
  },
)

onBeforeUnmount(() => {
  editor?.destroy()
  editor = null
})
</script>

<template>
  <div ref="host" class="editor-host"></div>
</template>

<style scoped>
.editor-host {
  min-height: 420px;
  height: 100%;
  border-radius: 1.6rem;
  overflow: hidden;
}
</style>

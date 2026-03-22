<script setup lang="ts">
import { computed, ref } from 'vue'
import VditorEditor from '../VditorEditor.vue'

interface LinkItem {
  id: string
}

interface TagItem {
  id: string
}

interface AttachmentItem {
  id: string
}

interface NoteDetail {
  id: string
  title: string
  markdownContent: string
  outgoingLinks?: LinkItem[]
  incomingLinks?: LinkItem[]
  tags?: TagItem[]
  attachments?: AttachmentItem[]
}

interface OutlineItem {
  id: string
  text: string
  level: number
}

interface EditorExpose {
  scrollToHeading: (id: string) => void
}

const props = defineProps<{
  loadingNote: boolean
  currentNote: NoteDetail | null
  noteTitle: string
  noteContent: string
  selectedKnowledgeBaseName: string
  knowledgeBaseCount: number
  folderCount: number
  noteCount: number
}>()

const emit = defineEmits<{
  (e: 'update:noteTitle', value: string): void
  (e: 'update:noteContent', value: string): void
}>()

const editorRef = ref<EditorExpose | null>(null)
const outlineItems = ref<OutlineItem[]>([])

const noteTitleModel = computed({
  get: () => props.noteTitle,
  set: (value: string) => emit('update:noteTitle', value),
})

const noteContentModel = computed({
  get: () => props.noteContent,
  set: (value: string) => emit('update:noteContent', value),
})

function jumpToOutline(id: string) {
  editorRef.value?.scrollToHeading(id)
}
</script>

<template>
  <div class="workspace-body">
    <section class="main-panel">
      <div v-if="loadingNote" class="auth-empty">笔记加载中...</div>

      <div v-else-if="currentNote" class="editor-area">
        <label class="field">
          <span>笔记标题</span>
          <input v-model="noteTitleModel" type="text" placeholder="请输入笔记标题" />
        </label>

        <div class="note-stats">
          <span>知识库：{{ selectedKnowledgeBaseName }}</span>
          <span>出链：{{ currentNote.outgoingLinks?.length || 0 }}</span>
          <span>入链：{{ currentNote.incomingLinks?.length || 0 }}</span>
          <span>标签：{{ currentNote.tags?.length || 0 }}</span>
          <span>附件：{{ currentNote.attachments?.length || 0 }}</span>
        </div>

        <div class="editor-frame">
          <VditorEditor ref="editorRef" v-model="noteContentModel" @outline-change="outlineItems = $event" />
        </div>
      </div>

      <div v-else class="dashboard">
        <article class="metric-card feature">
          <p class="eyebrow">开始使用</p>
          <h3>先选择知识库，再打开或创建一篇笔记。</h3>
          <p>左侧负责管理知识库和目录树，打开笔记后，中间编辑器和右侧目录会保持同步。</p>
        </article>

        <article class="metric-card">
          <strong>{{ knowledgeBaseCount }}</strong>
          <span>知识库</span>
        </article>

        <article class="metric-card">
          <strong>{{ folderCount }}</strong>
          <span>文件夹</span>
        </article>

        <article class="metric-card">
          <strong>{{ noteCount }}</strong>
          <span>笔记</span>
        </article>
      </div>
    </section>

    <aside class="outline-panel">
      <div class="panel-heading compact">
        <div>
          <p class="eyebrow">目录</p>
          <h3>标题导航</h3>
        </div>
      </div>

      <div v-if="!outlineItems.length" class="empty-card compact-card">
        在正文里使用 <code>#</code>、<code>##</code>、<code>###</code> 等标题语法，这里会自动生成目录。
      </div>

      <button
        v-for="item in outlineItems"
        :key="item.id"
        type="button"
        class="outline-item"
        :style="{ paddingLeft: `${(item.level - 1) * 16 + 14}px` }"
        @click="jumpToOutline(item.id)"
      >
        <span class="outline-level">H{{ item.level }}</span>
        <span>{{ item.text }}</span>
      </button>
    </aside>
  </div>
</template>

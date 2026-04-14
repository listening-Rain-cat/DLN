<script setup lang="ts">
import { formatDateTime } from '../../app/shared'
import type { Id, NoteHistoryDetail, NoteHistoryVersion } from '../../app/shared'

defineProps<{
  modal: {
    loadingList: boolean
    loadingDetail: boolean
    restoring: boolean
    versions: NoteHistoryVersion[]
    selectedVersionId: Id | null
    selectedDetail: NoteHistoryDetail | null
  }
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'select-version', versionId: Id): void
  (e: 'restore'): void
}>()
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-card note-history-modal">
      <div class="modal-heading">
        <p class="eyebrow">版本管理</p>
        <h3>笔记历史版本</h3>
      </div>

      <div class="note-history-layout">
        <aside class="note-history-list-panel">
          <div class="note-history-list-head">
            <strong>历史列表</strong>
            <span>{{ modal.versions.length }} 个版本</span>
          </div>

          <div v-if="modal.loadingList" class="empty-card compact-card note-history-empty">历史版本加载中...</div>

          <div v-else-if="modal.versions.length" class="note-history-list">
            <button
              v-for="version in modal.versions"
              :key="version.id"
              type="button"
              class="note-history-item"
              :class="{ active: modal.selectedVersionId === version.id }"
              @click="$emit('select-version', version.id)"
            >
              <div class="note-history-item-copy">
                <strong>v{{ version.versionNo }}</strong>
                <span>{{ version.title }}</span>
              </div>
              <time>{{ formatDateTime(version.createdTime) }}</time>
            </button>
          </div>

          <div v-else class="empty-card compact-card note-history-empty">当前笔记还没有历史版本。</div>
        </aside>

        <section class="note-history-detail-panel">
          <div class="note-history-detail-head">
            <div>
              <p class="eyebrow">版本详情</p>
              <h4 v-if="modal.selectedDetail">v{{ modal.selectedDetail.versionNo }} · {{ modal.selectedDetail.title }}</h4>
              <h4 v-else>请选择一个历史版本</h4>
            </div>
            <button
              type="button"
              class="soft-button accent"
              :disabled="modal.loadingDetail || modal.restoring || !modal.selectedDetail"
              @click="$emit('restore')"
            >
              {{ modal.restoring ? '恢复中...' : '恢复为当前版本' }}
            </button>
          </div>

          <div v-if="modal.loadingDetail" class="empty-card compact-card note-history-empty">版本详情加载中...</div>

          <template v-else-if="modal.selectedDetail">
            <div class="note-history-meta">
              <span>版本号 v{{ modal.selectedDetail.versionNo }}</span>
              <span>时间 {{ formatDateTime(modal.selectedDetail.createdTime) }}</span>
            </div>
            <pre class="note-history-markdown">{{ modal.selectedDetail.markdownContent || '' }}</pre>
          </template>

          <div v-else class="empty-card compact-card note-history-empty">
            从左侧选择一个版本后，这里会显示当时保存的标题和 Markdown 正文。
          </div>
        </section>
      </div>

      <div class="modal-actions">
        <button type="button" class="soft-button" :disabled="modal.restoring" @click="$emit('close')">关闭</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.note-history-modal {
  width: min(1120px, calc(100vw - 2rem));
  max-width: 1120px;
  display: grid;
  gap: 1rem;
}

.note-history-layout {
  min-height: min(70vh, 680px);
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  gap: 1rem;
}

.note-history-list-panel,
.note-history-detail-panel {
  min-height: 0;
  border-radius: 1.2rem;
  background: rgba(255, 251, 244, 0.92);
  box-shadow: inset 0 0 0 1px rgba(205, 178, 137, 0.12);
  display: grid;
  align-content: start;
  gap: 0.9rem;
  padding: 1rem;
}

.note-history-list-panel {
  grid-template-rows: auto minmax(0, 1fr);
}

.note-history-list-head,
.note-history-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.note-history-list-head strong {
  font-size: 1rem;
  color: #1f4745;
}

.note-history-list-head span {
  color: rgba(31, 71, 69, 0.64);
  font-size: 0.84rem;
}

.note-history-list {
  min-height: 0;
  overflow: auto;
  display: grid;
  align-content: start;
  grid-auto-rows: max-content;
  gap: 0.6rem;
}

.note-history-item {
  width: 100%;
  border: 0;
  border-radius: 1rem;
  padding: 0.88rem;
  background: rgba(249, 244, 236, 0.88);
  display: grid;
  align-self: start;
  gap: 0.42rem;
  text-align: left;
  transition:
    transform 160ms ease,
    background 160ms ease,
    box-shadow 160ms ease;
}

.note-history-item:hover {
  transform: translateY(-1px);
  background: rgba(244, 237, 227, 0.96);
  box-shadow: 0 12px 24px rgba(31, 63, 61, 0.06);
}

.note-history-item.active {
  background: linear-gradient(135deg, rgba(36, 89, 85, 0.94), rgba(196, 138, 54, 0.9));
  color: #fffaf2;
  box-shadow: 0 14px 28px rgba(37, 87, 84, 0.14);
}

.note-history-item-copy {
  min-width: 0;
  display: grid;
  gap: 0.28rem;
}

.note-history-item-copy strong {
  font-size: 0.88rem;
}

.note-history-item-copy span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.95rem;
  font-weight: 600;
}

.note-history-item time {
  font-size: 0.78rem;
  color: rgba(31, 71, 69, 0.62);
}

.note-history-item.active time {
  color: rgba(255, 250, 242, 0.82);
}

.note-history-detail-head h4 {
  margin: 0.25rem 0 0;
  color: #1f4745;
}

.note-history-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.note-history-meta span {
  padding: 0.34rem 0.68rem;
  border-radius: 999px;
  background: rgba(244, 237, 227, 0.92);
  color: rgba(31, 71, 69, 0.74);
  font-size: 0.82rem;
  font-weight: 600;
}

.note-history-markdown {
  min-height: 0;
  height: 100%;
  margin: 0;
  padding: 1rem;
  border-radius: 1rem;
  background: #211f22;
  color: #f6efe4;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.9rem;
  line-height: 1.65;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.note-history-empty {
  padding: 1rem;
  line-height: 1.6;
}

@media (max-width: 980px) {
  .note-history-modal {
    width: min(100vw - 1rem, 1000px);
  }

  .note-history-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .note-history-list {
    max-height: 240px;
  }
}
</style>

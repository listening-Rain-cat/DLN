<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Id, KnowledgeBaseSearchFilters, NoteSearchResult, SearchScope, TagItem, TreeNode } from '../../app/shared'

interface FolderOption {
  id: Id
  label: string
}

const props = defineProps<{
  selectedKnowledgeBaseId: Id | null
  selectedKnowledgeBaseName: string
  treeNodes: TreeNode[]
  knowledgeBaseTags: TagItem[]
  workbenchPanelMode?: 'directory' | 'search'
  onSearchKnowledgeBaseNotes?: (filters: KnowledgeBaseSearchFilters) => Promise<NoteSearchResult[]>
}>()

const emit = defineEmits<{
  (e: 'open-note', noteId: Id): void
  (e: 'switch-panel', mode: 'directory' | 'search'): void
}>()

const keyword = ref('')
const scope = ref<SearchScope>('all')
const selectedFolderId = ref('')
const selectedTagIds = ref<Id[]>([])
const loading = ref(false)
const searched = ref(false)
const errorMessage = ref('')
const results = ref<NoteSearchResult[]>([])

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

const scopeOptions: Array<{ value: SearchScope; label: string }> = [
  { value: 'all', label: '综合' },
  { value: 'title', label: '标题' },
  { value: 'tag', label: '标签' },
]

const folderOptions = computed<FolderOption[]>(() => {
  const options: FolderOption[] = []

  const walk = (nodes: TreeNode[], parents: string[] = []) => {
    for (const node of nodes) {
      if (node.type === 'folder') {
        const currentPath = [...parents, node.name]
        options.push({
          id: node.id,
          label: currentPath.join(' / '),
        })

        if (node.children?.length) {
          walk(node.children, currentPath)
        }
        continue
      }

      if (node.children?.length) {
        walk(node.children, parents)
      }
    }
  }

  walk(props.treeNodes)
  return options
})

const activeFilterCount = computed(() => {
  let count = 0
  if (keyword.value.trim()) {
    count += 1
  }
  if (selectedFolderId.value) {
    count += 1
  }
  if (selectedTagIds.value.length) {
    count += 1
  }
  if (scope.value !== 'all') {
    count += 1
  }
  return count
})

const resultSummary = computed(() => {
  if (loading.value) {
    return '正在检索知识库中的笔记...'
  }

  if (searched.value) {
    return `找到 ${results.value.length} 条结果`
  }

  return '支持标题、正文、标签的综合检索，并可继续筛选目录和标签。'
})

function toggleTag(tagId: Id) {
  selectedTagIds.value = selectedTagIds.value.includes(tagId)
    ? selectedTagIds.value.filter((currentTagId) => currentTagId !== tagId)
    : [...selectedTagIds.value, tagId]
}

function formatDateTime(value?: string) {
  if (!value) {
    return '未知时间'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return dateTimeFormatter.format(date)
}

async function runSearch() {
  if (!props.selectedKnowledgeBaseId || !props.onSearchKnowledgeBaseNotes) {
    return
  }

  loading.value = true
  searched.value = true
  errorMessage.value = ''

  try {
    results.value = await props.onSearchKnowledgeBaseNotes({
      keyword: keyword.value.trim(),
      scope: scope.value,
      folderId: selectedFolderId.value || null,
      tagIds: [...selectedTagIds.value],
    })
  } catch (error) {
    results.value = []
    errorMessage.value = (error as Error).message
  } finally {
    loading.value = false
  }
}

async function resetSearch() {
  keyword.value = ''
  scope.value = 'all'
  selectedFolderId.value = ''
  selectedTagIds.value = []
  await runSearch()
}

watch(
  () => props.selectedKnowledgeBaseId,
  () => {
    keyword.value = ''
    scope.value = 'all'
    selectedFolderId.value = ''
    selectedTagIds.value = []
    results.value = []
    errorMessage.value = ''
    searched.value = false

    if (props.selectedKnowledgeBaseId) {
      void runSearch()
    }
  },
  { immediate: true },
)
</script>

<template>
  <section class="search-workbench-section">
    <div class="panel-heading compact">
      <div>
        <div class="workbench-panel-switcher panel-inline-switcher" role="tablist" aria-label="工作台卡片切换">
          <button
            type="button"
            class="workbench-panel-switcher-button"
            :class="{ active: workbenchPanelMode === 'directory' }"
            :aria-selected="workbenchPanelMode === 'directory'"
            @click="emit('switch-panel', 'directory')"
          >
            资源目录
          </button>
          <button
            type="button"
            class="workbench-panel-switcher-button"
            :class="{ active: workbenchPanelMode === 'search' }"
            :aria-selected="workbenchPanelMode === 'search'"
            @click="emit('switch-panel', 'search')"
          >
            检索与导航
          </button>
        </div>
        <h3>{{ selectedKnowledgeBaseName || '当前知识库检索' }}</h3>
        <p class="panel-copy">{{ resultSummary }}</p>
      </div>
      <div class="search-workbench-actions">
        <span class="knowledge-base-entry-state">{{ activeFilterCount }} 个筛选</span>
        <button type="button" class="mini-button" :disabled="loading" @click="resetSearch">重置</button>
      </div>
    </div>

    <div class="search-workbench-toolbar">
      <label class="search-field search-field-keyword">
        <span>关键词</span>
        <input
          v-model="keyword"
          type="text"
          placeholder="输入标题、正文或标签关键词"
          @keydown.enter.prevent="runSearch"
        />
      </label>

      <label class="search-field">
        <span>目录筛选</span>
        <select v-model="selectedFolderId">
          <option value="">全部目录</option>
          <option v-for="folder in folderOptions" :key="folder.id" :value="folder.id">{{ folder.label }}</option>
        </select>
      </label>

      <button type="button" class="soft-button accent search-submit-button" :disabled="loading" @click="runSearch">
        {{ loading ? '检索中...' : '开始检索' }}
      </button>
    </div>

    <div class="search-scope-row">
      <div class="search-scope-switcher" role="tablist" aria-label="检索范围切换">
        <button
          v-for="item in scopeOptions"
          :key="item.value"
          type="button"
          class="search-scope-button"
          :class="{ active: scope === item.value }"
          :aria-selected="scope === item.value"
          @click="scope = item.value"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div class="search-tag-filter">
      <span class="search-tag-filter-label">标签筛选</span>
      <div v-if="knowledgeBaseTags.length" class="search-tag-filter-list">
        <button
          v-for="tag in knowledgeBaseTags"
          :key="tag.id"
          type="button"
          class="tag-chip"
          :class="{ active: selectedTagIds.includes(tag.id) }"
          @click="toggleTag(tag.id)"
        >
          # {{ tag.name }}
        </button>
      </div>
      <div v-else class="panel-copy">当前知识库还没有标签，可先创建后再用作筛选。</div>
    </div>

    <div v-if="loading" class="empty-card compact-card">正在检索当前知识库...</div>
    <div v-else-if="errorMessage" class="empty-card compact-card">{{ errorMessage }}</div>
    <div v-else-if="results.length" class="search-results-grid">
      <article v-for="result in results" :key="result.noteId" class="search-result-card">
        <div class="search-result-head">
          <div class="search-result-copy">
            <strong>{{ result.title }}</strong>
            <span>{{ result.folderPath }}</span>
          </div>
          <button type="button" class="mini-button accent" @click="emit('open-note', result.noteId)">打开</button>
        </div>

        <div class="search-result-badges">
          <span v-if="result.matchedByTitle" class="search-result-badge">标题命中</span>
          <span v-if="result.matchedByContent" class="search-result-badge">正文命中</span>
          <span v-if="result.matchedByTag" class="search-result-badge">标签命中</span>
          <span class="search-result-badge muted">出链 {{ result.outgoingCount }}</span>
          <span class="search-result-badge muted">入链 {{ result.incomingCount }}</span>
          <span v-if="result.brokenLinkCount" class="search-result-badge warning">失效链 {{ result.brokenLinkCount }}</span>
        </div>

        <p class="search-result-snippet">{{ result.snippet }}</p>

        <div v-if="result.tags.length" class="search-result-tags">
          <span v-for="tag in result.tags" :key="tag.id" class="tag-chip"># {{ tag.name }}</span>
        </div>

        <div class="search-result-meta">
          <span>更新于 {{ formatDateTime(result.updatedTime || result.createdTime) }}</span>
        </div>
      </article>
    </div>
    <div v-else class="empty-card compact-card">
      {{ searched ? '没有找到匹配结果，试试缩短关键词或切换检索范围。' : '输入关键词后开始搜索，也可以直接浏览当前知识库全部笔记。' }}
    </div>
  </section>
</template>

<style scoped>
.search-workbench-section {
  display: grid;
  gap: 0.95rem;
  border-radius: 1rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(32, 67, 63, 0.08);
}

.search-workbench-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.workbench-panel-switcher {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.3rem;
  padding: 0.24rem;
  border-radius: 999px;
  background: rgba(37, 87, 84, 0.08);
  border: 1px solid rgba(37, 87, 84, 0.12);
}

.workbench-panel-switcher-button {
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

.workbench-panel-switcher-button.active {
  background: linear-gradient(135deg, #255754, #3d7f73);
  color: #fff8f1;
  box-shadow: 0 10px 24px rgba(37, 87, 84, 0.16);
}

.panel-inline-switcher {
  margin-bottom: 0.38rem;
}

.search-workbench-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(220px, 0.8fr) auto;
  gap: 0.8rem;
  align-items: end;
}

.search-field {
  display: grid;
  gap: 0.45rem;
}

.search-field span,
.search-tag-filter-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: rgba(24, 54, 56, 0.72);
}

.search-field input,
.search-field select {
  width: 100%;
  border: 1px solid rgba(37, 87, 84, 0.12);
  border-radius: 0.88rem;
  padding: 0.8rem 0.95rem;
  background: rgba(255, 252, 247, 0.92);
  color: #173637;
}

.search-submit-button {
  min-width: 120px;
}

.search-scope-row {
  display: flex;
  justify-content: flex-start;
}

.search-scope-switcher {
  display: inline-flex;
  align-items: center;
  padding: 0.24rem;
  border-radius: 999px;
  background: rgba(37, 87, 84, 0.08);
  border: 1px solid rgba(37, 87, 84, 0.12);
}

.search-scope-button {
  border: 0;
  border-radius: 999px;
  padding: 0.48rem 0.9rem;
  background: transparent;
  color: rgba(24, 54, 56, 0.72);
  font-weight: 700;
}

.search-scope-button.active {
  background: linear-gradient(135deg, #255754, #3d7f73);
  color: #fff8f1;
}

.search-tag-filter {
  display: grid;
  gap: 0.6rem;
}

.search-tag-filter-list,
.search-result-tags,
.search-result-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.search-results-grid {
  display: grid;
  gap: 0.8rem;
}

.search-result-card {
  display: grid;
  gap: 0.7rem;
  padding: 1rem;
  border-radius: 1rem;
  background: rgba(249, 244, 236, 0.88);
  box-shadow: inset 0 0 0 1px rgba(205, 178, 137, 0.12);
}

.search-result-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.8rem;
}

.search-result-copy {
  display: grid;
  gap: 0.25rem;
}

.search-result-copy strong {
  color: #173637;
  font-size: 1rem;
}

.search-result-copy span,
.search-result-meta span {
  color: rgba(24, 54, 56, 0.62);
  font-size: 0.82rem;
}

.search-result-badge {
  padding: 0.28rem 0.58rem;
  border-radius: 999px;
  background: rgba(37, 87, 84, 0.1);
  color: #255754;
  font-size: 0.76rem;
  font-weight: 700;
}

.search-result-badge.muted {
  background: rgba(255, 255, 255, 0.72);
  color: rgba(24, 54, 56, 0.62);
}

.search-result-badge.warning {
  background: rgba(198, 124, 45, 0.14);
  color: #a35d17;
}

.search-result-snippet {
  margin: 0;
  color: rgba(24, 54, 56, 0.78);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.search-result-meta {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 980px) {
  .workbench-panel-switcher {
    width: 100%;
  }

  .search-workbench-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .search-result-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

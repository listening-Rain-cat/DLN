<script setup lang="ts">
import { computed, ref } from 'vue'

defineOptions({ name: 'OutlineTreeNode' })

interface OutlineTreeItem {
  id: string
  text: string
  level: number
  children?: OutlineTreeItem[]
}

const props = defineProps<{
  item: OutlineTreeItem
  activeId: string | null
}>()

const emit = defineEmits<{
  (e: 'select', id: string): void
}>()

const hasChildren = computed(() => Boolean(props.item.children?.length))
const isActive = computed(() => props.item.id === props.activeId)
const collapsed = ref(false)

function selectNode() {
  emit('select', props.item.id)
}

function toggleChildren() {
  if (!hasChildren.value) {
    return
  }

  collapsed.value = !collapsed.value
}
</script>

<template>
  <div class="outline-tree-node">
    <div class="outline-tree-row" :class="{ active: isActive }">
      <button
        v-if="hasChildren"
        type="button"
        class="outline-tree-toggle"
        :class="{ collapsed }"
        :aria-label="collapsed ? '展开子目录' : '收起子目录'"
        :aria-expanded="!collapsed"
        @click="toggleChildren"
      >
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <path d="M5.5 3.5 10.5 8l-5 4.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" />
        </svg>
      </button>

      <span v-else class="outline-tree-toggle outline-tree-toggle-placeholder" aria-hidden="true"></span>

      <button type="button" class="outline-tree-main" @click="selectNode">
        <span class="outline-tree-badge">H{{ item.level }}</span>
        <span class="outline-tree-label">{{ item.text }}</span>
      </button>
    </div>

    <div v-if="hasChildren && !collapsed" class="outline-tree-children">
      <OutlineTreeNode
        v-for="child in item.children"
        :key="child.id"
        :item="child"
        :active-id="activeId"
        @select="emit('select', $event)"
      />
    </div>
  </div>
</template>

<style scoped>
.outline-tree-node {
  min-width: 0;
  display: grid;
  gap: 0.28rem;
}

.outline-tree-row {
  min-width: 0;
  border-radius: 0.95rem;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 0.32rem;
}

.outline-tree-row.active {
  background:
    linear-gradient(135deg, rgba(24, 91, 84, 0.16), rgba(200, 141, 61, 0.16)),
    rgba(255, 255, 255, 0.88);
  box-shadow: inset 0 0 0 1px rgba(31, 79, 76, 0.1);
}

.outline-tree-toggle {
  width: 1.7rem;
  height: 1.7rem;
  border: 0;
  border-radius: 0.6rem;
  background: transparent;
  color: rgba(34, 69, 70, 0.82);
  display: grid;
  place-items: center;
  transition:
    transform 160ms ease,
    background 160ms ease,
    color 160ms ease;
}

.outline-tree-toggle:hover {
  background: rgba(37, 87, 84, 0.08);
}

.outline-tree-toggle svg {
  width: 0.92rem;
  height: 0.92rem;
  transition: transform 160ms ease;
}

.outline-tree-toggle.collapsed svg {
  transform: rotate(0deg);
}

.outline-tree-toggle:not(.collapsed) svg {
  transform: rotate(90deg);
}

.outline-tree-toggle-placeholder {
  opacity: 0.3;
  pointer-events: none;
}

.outline-tree-main {
  min-width: 0;
  width: 100%;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  border-radius: 0.9rem;
  padding: 0.56rem 0.7rem 0.56rem 0.2rem;
  display: grid;
  grid-template-columns: 1.9rem minmax(0, 1fr);
  align-items: center;
  gap: 0.45rem;
}

.outline-tree-main:hover {
  background: rgba(37, 87, 84, 0.08);
}

.outline-tree-badge {
  width: 1.65rem;
  height: 1.65rem;
  border-radius: 0.58rem;
  display: grid;
  place-items: center;
  background: rgba(36, 89, 85, 0.12);
  color: #285855;
  font-size: 0.62rem;
  font-weight: 700;
}

.outline-tree-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.84rem;
}

.outline-tree-children {
  min-width: 0;
  position: relative;
  margin-left: 0.95rem;
  padding-left: 0.82rem;
  display: grid;
  gap: 0.22rem;
}

.outline-tree-children::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.12rem;
  bottom: 0.12rem;
  width: 2px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(37, 87, 84, 0.34), rgba(216, 146, 51, 0.26));
}
</style>

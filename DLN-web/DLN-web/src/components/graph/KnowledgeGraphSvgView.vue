<script setup lang="ts">
type Id = string

interface GraphLayoutEdge {
  id: Id
  sourceX: number
  sourceY: number
  targetX: number
  targetY: number
  isBroken?: number | boolean
}

interface GraphLayoutNode {
  noteId: Id
  title: string
  incomingCount: number
  outgoingCount: number
  x: number
  y: number
  radius: number
}

interface GraphLayout {
  edges: GraphLayoutEdge[]
  nodes: GraphLayoutNode[]
}

defineProps<{
  loading: boolean
  selectedKnowledgeBaseId: Id | null
  knowledgeBaseName: string
  graphNodeCount: number
  graphEdgeCount: number
  graphViewBox: string
  graphLayout: GraphLayout
  graphNodeStyle: (node: GraphLayoutNode) => Record<string, string>
}>()

defineEmits<{
  (e: 'open-note', noteId: Id): void
}>()
</script>

<template>
  <div class="main-panel large-card graph-page-panel">
    <div class="graph-stage">
      <div v-if="loading" class="graph-empty-state">知识图谱加载中...</div>
      <div v-else-if="!selectedKnowledgeBaseId" class="graph-empty-state">
        请选择一个知识库后再查看知识图谱。
      </div>
      <div v-else-if="!graphNodeCount" class="graph-empty-state">当前知识库还没有笔记，暂时无法生成知识图谱。</div>

      <svg
        v-else
        class="graph-canvas"
        :viewBox="graphViewBox"
        preserveAspectRatio="xMidYMid meet"
        aria-label="知识图谱关系图"
      >
        <line
          v-for="edge in graphLayout.edges"
          :key="edge.id"
          class="graph-edge"
          :class="{ broken: Boolean(edge.isBroken) }"
          :x1="edge.sourceX"
          :y1="edge.sourceY"
          :x2="edge.targetX"
          :y2="edge.targetY"
        />
      </svg>

      <div class="graph-center">
        <p class="eyebrow">知识图谱</p>
        <h3>{{ knowledgeBaseName || '请选择知识库' }}</h3>
        <p>
          {{
            selectedKnowledgeBaseId
              ? `当前共 ${graphNodeCount} 个节点，${graphEdgeCount} 条双链边。`
              : '先选择知识库，再从双链关系生成图谱。'
          }}
        </p>
      </div>

      <button
        v-for="node in graphLayout.nodes"
        :key="node.noteId"
        type="button"
        class="graph-node"
        :style="graphNodeStyle(node)"
        :title="`${node.title} · 出链 ${node.outgoingCount} / 入链 ${node.incomingCount}`"
        @click="$emit('open-note', node.noteId)"
      >
        <strong>{{ node.title }}</strong>
        <span>{{ node.outgoingCount }} out · {{ node.incomingCount }} in</span>
      </button>
    </div>
  </div>
</template>

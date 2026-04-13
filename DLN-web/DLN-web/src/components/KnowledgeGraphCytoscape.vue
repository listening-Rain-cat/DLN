<script setup lang="ts">
import cytoscape, { type Core, type ElementDefinition } from 'cytoscape'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

interface GraphNode {
  noteId: string
  title: string
  incomingCount: number
  outgoingCount: number
}

interface GraphEdge {
  id: string
  sourceNoteId: string
  targetNoteId: string
  isBroken: number
}

interface KnowledgeGraph {
  knowledgeBaseId: string
  nodes: GraphNode[]
  edges: GraphEdge[]
}

const props = defineProps<{
  graph: KnowledgeGraph | null
  loading: boolean
  selectedKnowledgeBaseId: string | null
  knowledgeBaseName: string
}>()

const emit = defineEmits<{
  (event: 'open-note', noteId: string): void
}>()

const containerRef = ref<HTMLDivElement | null>(null)
let cy: Core | null = null
let resizeObserver: ResizeObserver | null = null
let relayoutTimer: number | null = null

const nodeCount = computed(() => props.graph?.nodes.length ?? 0)
const edgeCount = computed(() => props.graph?.edges.length ?? 0)
const hasSelectedKnowledgeBase = computed(() => Boolean(props.selectedKnowledgeBaseId))
const hasRenderableGraph = computed(() => hasSelectedKnowledgeBase.value && nodeCount.value > 0)

function createElements(graph: KnowledgeGraph): ElementDefinition[] {
  const nodes = graph.nodes.map((node) => ({
    data: {
      id: node.noteId,
      label: node.title,
      incomingCount: node.incomingCount,
      outgoingCount: node.outgoingCount,
      weight: Math.max(1, node.incomingCount + node.outgoingCount),
    },
  }))

  const edges = graph.edges.map((edge) => ({
    data: {
      id: edge.id,
      source: edge.sourceNoteId,
      target: edge.targetNoteId,
      broken: edge.isBroken,
    },
  }))

  return [...nodes, ...edges]
}

function runLayout(instance: Core) {
  const container = instance.container()
  const width = Math.max(container?.clientWidth ?? 0, 960)
  const height = Math.max(container?.clientHeight ?? 0, 640)

  instance.layout({
    name: 'concentric',
    animate: false,
    fit: true,
    padding: 108,
    avoidOverlap: true,
    nodeDimensionsIncludeLabels: true,
    spacingFactor: 1.36,
    minNodeSpacing: 68,
    startAngle: -Math.PI / 2,
    sweep: Math.PI * 2 - 0.001,
    clockwise: true,
    equidistant: false,
    width,
    height,
    concentric: (node) => Number(node.data('weight') ?? 1),
    levelWidth: () => 1,
  }).run()
}

function scheduleRelayout(delay = 120) {
  if (relayoutTimer !== null) {
    window.clearTimeout(relayoutTimer)
  }

  relayoutTimer = window.setTimeout(() => {
    relayoutTimer = null

    if (!cy || !cy.elements().length) {
      return
    }

    cy.resize()
    runLayout(cy)
  }, delay)
}

function ensureGraph() {
  if (cy || !containerRef.value) {
    return
  }

  cy = cytoscape({
    container: containerRef.value,
    elements: [],
    minZoom: 0.28,
    maxZoom: 2.4,
    wheelSensitivity: 0.18,
    boxSelectionEnabled: false,
    style: [
      {
        selector: 'node',
        style: {
          label: 'data(label)',
          color: '#fff9f1',
          'font-size': '15px',
          'font-weight': 'bold',
          'text-wrap': 'wrap',
          'text-max-width': '176px',
          'text-valign': 'center',
          'text-halign': 'center',
          'text-outline-width': 0,
          shape: 'cut-rectangle',
          'background-color': '#255754',
          width: 'mapData(weight, 1, 12, 156, 236)',
          height: 'mapData(weight, 1, 12, 70, 98)',
          'border-width': '2.5px',
          'border-color': 'rgba(255, 255, 255, 0.74)',
          'overlay-padding': '8px',
          'overlay-opacity': 0,
        },
      },
      {
        selector: 'edge',
        style: {
          width: '2.2px',
          opacity: 0.92,
          'curve-style': 'bezier',
          'line-color': 'rgba(36, 84, 82, 0.34)',
          'target-arrow-color': 'rgba(36, 84, 82, 0.46)',
          'target-arrow-shape': 'triangle',
          'arrow-scale': 0.86,
        },
      },
      {
        selector: 'edge[broken = 1]',
        style: {
          'line-color': 'rgba(143, 61, 51, 0.28)',
          'target-arrow-color': 'rgba(143, 61, 51, 0.36)',
          'line-style': 'dashed',
        },
      },
      {
        selector: 'node:selected',
        style: {
          'background-color': '#d89233',
          'border-color': '#fff8ee',
          'border-width': '3px',
        },
      },
      {
        selector: '.faded',
        style: {
          opacity: 0.18,
          'text-opacity': 0.18,
        },
      },
    ],
  })

  cy.on('tap', 'node', (event) => {
    emit('open-note', String(event.target.id()))
  })

  cy.on('mouseover', 'node', (event) => {
    if (!cy) {
      return
    }

    const focused = event.target.closedNeighborhood()
    cy.elements().addClass('faded')
    focused.removeClass('faded')
  })

  cy.on('mouseout', 'node', () => {
    cy?.elements().removeClass('faded')
  })

  resizeObserver = new ResizeObserver(() => {
    if (!cy) {
      return
    }

    cy.resize()
    cy.fit(cy.elements(), 48)
    scheduleRelayout(160)
  })

  resizeObserver.observe(containerRef.value)
}

async function renderGraph() {
  await nextTick()
  ensureGraph()

  if (!cy) {
    return
  }

  if (!props.graph || !hasRenderableGraph.value || props.loading) {
    cy.elements().remove()
    return
  }

  const elements = createElements(props.graph)

  cy.batch(() => {
    cy?.elements().remove()
    cy?.add(elements)
  })

  cy.resize()
  runLayout(cy)
  scheduleRelayout()
}

onMounted(() => {
  void renderGraph()
})

watch(
  () => [props.graph, props.loading, props.selectedKnowledgeBaseId] as const,
  () => {
    void renderGraph()
  },
  { deep: true },
)

onBeforeUnmount(() => {
  if (relayoutTimer !== null) {
    window.clearTimeout(relayoutTimer)
    relayoutTimer = null
  }
  resizeObserver?.disconnect()
  resizeObserver = null
  cy?.destroy()
  cy = null
})
</script>

<template>
  <div class="graph-stage graph-stage-cytoscape">
    <div v-if="loading" class="graph-empty-state">知识图谱加载中...</div>
    <div v-else-if="!hasSelectedKnowledgeBase" class="graph-empty-state">请选择一个知识库后再查看知识图谱。</div>
    <div v-else-if="!hasRenderableGraph" class="graph-empty-state">当前知识库还没有笔记，暂时无法生成知识图谱。</div>
    <div v-else ref="containerRef" class="graph-cytoscape-canvas" aria-label="Cytoscape 知识图谱"></div>

    <div class="graph-center graph-center-cytoscape">
      <p class="eyebrow">Cytoscape.js 测试</p>
      <h3>{{ knowledgeBaseName || '请选择知识库' }}</h3>
      <p>
        {{
          selectedKnowledgeBaseId
            ? `当前共 ${nodeCount} 个节点，${edgeCount} 条双链边。拖动画布、滚轮缩放、点击节点可打开笔记。`
            : '先选择知识库，再从双链关系生成图谱。'
        }}
      </p>
    </div>
  </div>
</template>

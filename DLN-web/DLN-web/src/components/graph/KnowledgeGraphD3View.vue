<script setup lang="ts">
import * as d3 from 'd3'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { Id, KnowledgeGraph } from '../../app/shared'

interface GraphNodeDatum extends d3.SimulationNodeDatum {
  id: Id
  title: string
  incomingCount: number
  outgoingCount: number
  weight: number
  radius: number
}

interface GraphLinkDatum extends d3.SimulationLinkDatum<GraphNodeDatum> {
  id: Id
  source: Id | GraphNodeDatum
  target: Id | GraphNodeDatum
  isBroken: boolean
}

const props = defineProps<{
  graph: KnowledgeGraph | null
  loading: boolean
  selectedKnowledgeBaseId: Id | null
  knowledgeBaseName: string
}>()

const emit = defineEmits<{
  (event: 'open-note', noteId: Id): void
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const svgRef = ref<SVGSVGElement | null>(null)

let simulation: d3.Simulation<GraphNodeDatum, GraphLinkDatum> | null = null
let resizeObserver: ResizeObserver | null = null
let resizeTimer: number | null = null
let zoomBehavior: d3.ZoomBehavior<SVGSVGElement, unknown> | null = null
let renderRevision = 0

const nodeCount = computed(() => props.graph?.nodes.length ?? 0)
const edgeCount = computed(() => props.graph?.edges.length ?? 0)
const hasSelectedKnowledgeBase = computed(() => Boolean(props.selectedKnowledgeBaseId))
const hasRenderableGraph = computed(() => hasSelectedKnowledgeBase.value && nodeCount.value > 0)

function truncateText(value: string, maxLength: number) {
  if (value.length <= maxLength) {
    return value
  }
  return `${value.slice(0, Math.max(0, maxLength - 3))}...`
}

function stopSimulation() {
  simulation?.stop()
  simulation = null
}

function clearGraph() {
  stopSimulation()
  const svgElement = svgRef.value
  if (!svgElement) {
    return
  }

  const svgSelection = d3.select(svgElement)
  svgSelection.selectAll('*').remove()
  if (zoomBehavior) {
    svgSelection.on('.zoom', null)
  }
  zoomBehavior = null
}

function resolveStageSize() {
  const container = containerRef.value
  if (!container) {
    return { width: 1280, height: 760 }
  }

  const rect = container.getBoundingClientRect()
  return {
    width: Math.max(960, Math.floor(rect.width || 1280)),
    height: Math.max(620, Math.floor(rect.height || 760)),
  }
}

function scheduleRender(delay = 120) {
  if (resizeTimer !== null) {
    window.clearTimeout(resizeTimer)
  }

  resizeTimer = window.setTimeout(() => {
    resizeTimer = null
    void renderGraph()
  }, delay)
}

function toNodeId(nodeRef: Id | GraphNodeDatum): Id {
  return typeof nodeRef === 'string' ? nodeRef : nodeRef.id
}

function resolveNode(nodeRef: Id | GraphNodeDatum, nodeById: Map<Id, GraphNodeDatum>) {
  return typeof nodeRef === 'string' ? nodeById.get(nodeRef) : nodeRef
}

async function renderGraph() {
  const renderToken = ++renderRevision
  await nextTick()
  if (renderToken !== renderRevision) {
    return
  }

  const svgElement = svgRef.value
  if (!svgElement) {
    return
  }

  clearGraph()

  if (!props.graph || props.loading || !hasRenderableGraph.value) {
    return
  }

  const { width, height } = resolveStageSize()
  const svgSelection = d3
    .select(svgElement)
    .attr('viewBox', `0 0 ${width} ${height}`)
    .attr('preserveAspectRatio', 'xMidYMid meet')

  const root = svgSelection.append('g').attr('class', 'graph-d3-root')
  const linksGroup = root.append('g').attr('class', 'graph-d3-links')
  const nodesGroup = root.append('g').attr('class', 'graph-d3-nodes')

  const nodeData: GraphNodeDatum[] = props.graph.nodes.map((node) => {
    const weight = Math.max(1, node.incomingCount + node.outgoingCount)
    return {
      id: node.noteId,
      title: node.title || '未命名笔记',
      incomingCount: node.incomingCount,
      outgoingCount: node.outgoingCount,
      weight,
      radius: Math.min(20, 10 + Math.sqrt(weight) * 2.8),
      x: width / 2 + (Math.random() - 0.5) * 160,
      y: height / 2 + (Math.random() - 0.5) * 140,
    }
  })

  const nodeById = new Map<Id, GraphNodeDatum>(nodeData.map((node) => [node.id, node]))
  const nodeIdSet = new Set(nodeData.map((node) => node.id))

  const linkData: GraphLinkDatum[] = props.graph.edges
    .filter((edge) => nodeIdSet.has(edge.sourceNoteId) && nodeIdSet.has(edge.targetNoteId))
    .map((edge) => ({
      id: edge.id,
      source: edge.sourceNoteId,
      target: edge.targetNoteId,
      isBroken: Boolean(edge.isBroken),
    }))

  const adjacency = new Set<string>()
  linkData.forEach((link) => {
    const sourceId = toNodeId(link.source)
    const targetId = toNodeId(link.target)
    adjacency.add(`${sourceId}::${targetId}`)
    adjacency.add(`${targetId}::${sourceId}`)
  })

  const linkSelection = linksGroup
    .selectAll<SVGLineElement, GraphLinkDatum>('line')
    .data(linkData)
    .enter()
    .append('line')
    .attr('class', (link: GraphLinkDatum) => `graph-d3-link${link.isBroken ? ' broken' : ''}`)

  const nodeSelection = nodesGroup
    .selectAll<SVGGElement, GraphNodeDatum>('g')
    .data(nodeData)
    .enter()
    .append('g')
    .attr('class', 'graph-d3-node')

  nodeSelection.append('circle').attr('class', 'graph-d3-node-core').attr('r', (node: GraphNodeDatum) => node.radius)

  nodeSelection.append('line').attr('class', 'graph-d3-node-connector')

  nodeSelection
    .append('text')
    .attr('class', 'graph-d3-node-title')
    .text((node: GraphNodeDatum) => truncateText(node.title, 24))

  nodeSelection
    .append('text')
    .attr('class', 'graph-d3-node-meta')
    .text((node: GraphNodeDatum) => `${node.outgoingCount} out / ${node.incomingCount} in`)

  const dragBehavior = d3
    .drag<SVGGElement, GraphNodeDatum>()
    .on(
      'start',
      (event: d3.D3DragEvent<SVGGElement, GraphNodeDatum, GraphNodeDatum>, node: GraphNodeDatum) => {
        if (!event.active && simulation) {
          simulation.alphaTarget(0.2).restart()
        }
        node.fx = node.x
        node.fy = node.y
      },
    )
    .on(
      'drag',
      (event: d3.D3DragEvent<SVGGElement, GraphNodeDatum, GraphNodeDatum>, node: GraphNodeDatum) => {
        node.fx = event.x
        node.fy = event.y
      },
    )
    .on(
      'end',
      (event: d3.D3DragEvent<SVGGElement, GraphNodeDatum, GraphNodeDatum>, node: GraphNodeDatum) => {
        if (!event.active && simulation) {
          simulation.alphaTarget(0)
        }
        node.fx = null
        node.fy = null
      },
    )

  nodeSelection.call(dragBehavior)

  nodeSelection.on('click', (_event: MouseEvent, node: GraphNodeDatum) => {
    emit('open-note', node.id)
  })

  nodeSelection
    .on('mouseenter', (_event: MouseEvent, focusedNode: GraphNodeDatum) => {
      nodeSelection.classed(
        'muted',
        (node: GraphNodeDatum) => node.id !== focusedNode.id && !adjacency.has(`${focusedNode.id}::${node.id}`),
      )

      linkSelection.classed('muted', (link: GraphLinkDatum) => {
        const sourceId = toNodeId(link.source)
        const targetId = toNodeId(link.target)
        return sourceId !== focusedNode.id && targetId !== focusedNode.id
      })
    })
    .on('mouseleave', () => {
      nodeSelection.classed('muted', false)
      linkSelection.classed('muted', false)
    })

  simulation = d3
    .forceSimulation(nodeData)
    .force(
      'link',
      d3
        .forceLink<GraphNodeDatum, GraphLinkDatum>(linkData)
        .id((node: GraphNodeDatum) => node.id)
        .distance((link: GraphLinkDatum) => (link.isBroken ? 230 : 145))
        .strength((link: GraphLinkDatum) => (link.isBroken ? 0.22 : 0.42)),
    )
    .force('charge', d3.forceManyBody<GraphNodeDatum>().strength((node: GraphNodeDatum) => -280 - node.weight * 16))
    .force('center', d3.forceCenter(width / 2, height / 2))
    .force('x', d3.forceX<GraphNodeDatum>(width / 2).strength(0.03))
    .force('y', d3.forceY<GraphNodeDatum>(height / 2).strength(0.03))
    .force(
      'collide',
      d3
        .forceCollide<GraphNodeDatum>()
        .radius((node: GraphNodeDatum) => node.radius + 18)
        .iterations(2),
    )
    .alpha(0.9)
    .on('tick', () => {
      linkSelection
        .attr('x1', (link: GraphLinkDatum) => resolveNode(link.source, nodeById)?.x ?? 0)
        .attr('y1', (link: GraphLinkDatum) => resolveNode(link.source, nodeById)?.y ?? 0)
        .attr('x2', (link: GraphLinkDatum) => resolveNode(link.target, nodeById)?.x ?? 0)
        .attr('y2', (link: GraphLinkDatum) => resolveNode(link.target, nodeById)?.y ?? 0)

      nodeSelection
        .attr('transform', (node: GraphNodeDatum) => `translate(${node.x ?? 0},${node.y ?? 0})`)
        .each(function (node: GraphNodeDatum) {
          const group = d3.select<SVGGElement, GraphNodeDatum>(this)
          const labelOnLeft = (node.x ?? 0) > width / 2
          const direction = labelOnLeft ? -1 : 1
          const anchor = labelOnLeft ? 'end' : 'start'
          const connectorStartX = direction * (node.radius + 2)
          const connectorEndX = direction * (node.radius + 12)
          const labelX = direction * (node.radius + 16)

          group
            .select<SVGLineElement>('line.graph-d3-node-connector')
            .attr('x1', connectorStartX)
            .attr('y1', 0)
            .attr('x2', connectorEndX)
            .attr('y2', 0)

          group
            .select<SVGTextElement>('text.graph-d3-node-title')
            .attr('text-anchor', anchor)
            .attr('x', labelX)
            .attr('y', -3)

          group
            .select<SVGTextElement>('text.graph-d3-node-meta')
            .attr('text-anchor', anchor)
            .attr('x', labelX)
            .attr('y', 14)
        })
    })

  zoomBehavior = d3
    .zoom<SVGSVGElement, unknown>()
    .scaleExtent([0.35, 2.5])
    .on('zoom', (event: d3.D3ZoomEvent<SVGSVGElement, unknown>) => {
      root.attr('transform', event.transform.toString())
    })

  svgSelection.call(zoomBehavior)
  svgSelection.call(
    zoomBehavior.transform,
    d3.zoomIdentity.translate(width * 0.08, height * 0.04).scale(0.96),
  )
}

onMounted(() => {
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      scheduleRender()
    })
    resizeObserver.observe(containerRef.value)
  }
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
  if (resizeTimer !== null) {
    window.clearTimeout(resizeTimer)
    resizeTimer = null
  }
  resizeObserver?.disconnect()
  resizeObserver = null
  clearGraph()
})
</script>

<template>
  <div class="graph-stage graph-stage-d3">
    <div v-if="loading" class="graph-empty-state">知识图谱加载中...</div>
    <div v-else-if="!hasSelectedKnowledgeBase" class="graph-empty-state">请选择一个知识库后再查看知识图谱。</div>
    <div v-else-if="!hasRenderableGraph" class="graph-empty-state">当前知识库还没有笔记，暂时无法生成知识图谱。</div>
    <div v-else ref="containerRef" class="graph-d3-shell" aria-label="D3 知识图谱">
      <svg ref="svgRef" class="graph-d3-canvas"></svg>
    </div>

    <div class="graph-center graph-center-d3">
      <p class="eyebrow">知识图谱</p>
      <h3>{{ knowledgeBaseName || '请选择知识库' }}</h3>
      <p>
        {{
          selectedKnowledgeBaseId
            ? `当前共 ${nodeCount} 个节点，${edgeCount} 条双链边。可拖拽节点、滚轮缩放、点击节点打开笔记。`
            : '先选择知识库，再根据双链关系生成图谱。'
        }}
      </p>
    </div>
  </div>
</template>

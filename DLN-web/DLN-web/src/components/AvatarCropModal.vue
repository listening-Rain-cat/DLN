<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'

const props = defineProps<{
  imageUrl: string
  fileName: string
  submitting: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'confirm', file: File): void
}>()

const cropViewportRef = ref<HTMLDivElement | null>(null)
const imageRef = ref<HTMLImageElement | null>(null)

const imageMeta = reactive({
  naturalWidth: 0,
  naturalHeight: 0,
})

const viewport = reactive({
  width: 0,
  height: 0,
})

const zoom = ref(1)
const baseScale = ref(1)
const offset = reactive({
  x: 0,
  y: 0,
})

const dragState = reactive({
  active: false,
  startX: 0,
  startY: 0,
  originX: 0,
  originY: 0,
})

const imageStyle = computed(() => {
  const scale = baseScale.value * zoom.value
  return {
    width: `${imageMeta.naturalWidth * scale}px`,
    height: `${imageMeta.naturalHeight * scale}px`,
    transform: `translate(calc(-50% + ${offset.x}px), calc(-50% + ${offset.y}px))`,
  }
})

function measureViewport() {
  const rect = cropViewportRef.value?.getBoundingClientRect()
  viewport.width = rect?.width || 0
  viewport.height = rect?.height || 0
}

function clampOffset() {
  const scale = baseScale.value * zoom.value
  if (!scale || !viewport.width || !viewport.height) {
    offset.x = 0
    offset.y = 0
    return
  }

  const renderedWidth = imageMeta.naturalWidth * scale
  const renderedHeight = imageMeta.naturalHeight * scale
  const maxOffsetX = Math.max(0, (renderedWidth - viewport.width) / 2)
  const maxOffsetY = Math.max(0, (renderedHeight - viewport.height) / 2)

  offset.x = Math.min(maxOffsetX, Math.max(-maxOffsetX, offset.x))
  offset.y = Math.min(maxOffsetY, Math.max(-maxOffsetY, offset.y))
}

function resetTransform() {
  measureViewport()
  if (!imageMeta.naturalWidth || !imageMeta.naturalHeight || !viewport.width || !viewport.height) {
    return
  }

  baseScale.value = Math.max(
    viewport.width / imageMeta.naturalWidth,
    viewport.height / imageMeta.naturalHeight,
  )
  zoom.value = 1
  offset.x = 0
  offset.y = 0
}

function handleImageLoad() {
  if (!imageRef.value) {
    return
  }

  imageMeta.naturalWidth = imageRef.value.naturalWidth
  imageMeta.naturalHeight = imageRef.value.naturalHeight
  resetTransform()
}

function handlePointerDown(event: PointerEvent) {
  if (props.submitting) {
    return
  }

  dragState.active = true
  dragState.startX = event.clientX
  dragState.startY = event.clientY
  dragState.originX = offset.x
  dragState.originY = offset.y
}

function handlePointerMove(event: PointerEvent) {
  if (!dragState.active) {
    return
  }

  offset.x = dragState.originX + (event.clientX - dragState.startX)
  offset.y = dragState.originY + (event.clientY - dragState.startY)
  clampOffset()
}

function stopDragging() {
  dragState.active = false
}

watch(zoom, () => {
  clampOffset()
})

watch(
  () => props.imageUrl,
  async () => {
    imageMeta.naturalWidth = 0
    imageMeta.naturalHeight = 0
    await nextTick()
    measureViewport()
  },
  { immediate: true },
)

async function exportCroppedFile() {
  if (!imageRef.value || !viewport.width || !viewport.height) {
    return
  }

  const scale = baseScale.value * zoom.value
  const renderedWidth = imageMeta.naturalWidth * scale
  const renderedHeight = imageMeta.naturalHeight * scale
  const renderedLeft = (viewport.width - renderedWidth) / 2 + offset.x
  const renderedTop = (viewport.height - renderedHeight) / 2 + offset.y

  const sx = Math.max(0, (0 - renderedLeft) / scale)
  const sy = Math.max(0, (0 - renderedTop) / scale)
  const sw = Math.min(imageMeta.naturalWidth - sx, viewport.width / scale)
  const sh = Math.min(imageMeta.naturalHeight - sy, viewport.height / scale)

  const canvas = document.createElement('canvas')
  canvas.width = 512
  canvas.height = 512

  const context = canvas.getContext('2d')
  if (!context) {
    return
  }

  context.drawImage(imageRef.value, sx, sy, sw, sh, 0, 0, canvas.width, canvas.height)

  const blob = await new Promise<Blob | null>((resolve) => {
    canvas.toBlob((value) => resolve(value), 'image/png', 0.95)
  })

  if (!blob) {
    return
  }

  const safeName = props.fileName.replace(/\.[^.]+$/, '') || 'avatar'
  emit('confirm', new File([blob], `${safeName}.png`, { type: 'image/png' }))
}

window.addEventListener('pointermove', handlePointerMove)
window.addEventListener('pointerup', stopDragging)
window.addEventListener('pointercancel', stopDragging)
window.addEventListener('resize', resetTransform)

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', stopDragging)
  window.removeEventListener('pointercancel', stopDragging)
  window.removeEventListener('resize', resetTransform)
})
</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-card avatar-crop-modal">
      <div class="modal-heading">
        <p class="eyebrow">头像裁剪</p>
        <h3>调整头像显示区域</h3>
      </div>

      <p class="avatar-crop-copy">拖动图片调整位置，配合缩放让头像显示得更自然。</p>

      <div
        ref="cropViewportRef"
        class="avatar-crop-viewport"
        :class="{ dragging: dragState.active }"
        @pointerdown="handlePointerDown"
      >
        <img
          ref="imageRef"
          :src="imageUrl"
          alt="头像裁剪预览"
          class="avatar-crop-image"
          :style="imageStyle"
          draggable="false"
          @load="handleImageLoad"
        />
        <div class="avatar-crop-mask"></div>
      </div>

      <label class="field">
        <span>缩放</span>
        <input v-model="zoom" type="range" min="1" max="3" step="0.01" />
      </label>

      <div class="modal-actions">
        <button type="button" class="soft-button" :disabled="submitting" @click="emit('close')">取消</button>
        <button type="button" class="soft-button accent" :disabled="submitting" @click="exportCroppedFile">
          {{ submitting ? '上传中...' : '裁剪并上传' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.avatar-crop-modal {
  width: min(620px, 100%);
}

.avatar-crop-copy {
  margin: 0;
  color: rgba(24, 54, 56, 0.74);
}

.avatar-crop-viewport {
  position: relative;
  width: min(100%, 360px);
  aspect-ratio: 1;
  margin: 0 auto;
  border-radius: 1.35rem;
  overflow: hidden;
  background:
    linear-gradient(145deg, rgba(244, 237, 227, 0.96), rgba(251, 246, 239, 0.94)),
    rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 0 0 1px rgba(32, 67, 63, 0.07);
  touch-action: none;
  cursor: grab;
}

.avatar-crop-viewport.dragging {
  cursor: grabbing;
}

.avatar-crop-image {
  position: absolute;
  top: 50%;
  left: 50%;
  user-select: none;
  pointer-events: none;
}

.avatar-crop-mask {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  box-shadow:
    inset 0 0 0 2px rgba(255, 255, 255, 0.85),
    inset 0 0 0 999px rgba(23, 45, 46, 0.18);
}
</style>

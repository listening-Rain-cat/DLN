<script setup lang="ts">
type ViewMode = 'home' | 'templates' | 'settings' | 'graph' | 'graph-test'

defineProps<{
  viewMode: ViewMode
  displayName: string
  displayEmail: string
  currentAvatarUrl: string
  userInitial: string
}>()

defineEmits<{
  (e: 'open-home'): void
  (e: 'open-templates'): void
  (e: 'open-settings'): void
  (e: 'open-graph'): void
  (e: 'open-graph-test'): void
  (e: 'logout'): void
}>()
</script>

<template>
  <aside class="rail">
    <div class="rail-top rail-profile" :title="`${displayName} · ${displayEmail}`">
      <div class="rail-avatar-shell">
        <img v-if="currentAvatarUrl" :src="currentAvatarUrl" :alt="`${displayName} 的头像`" class="rail-avatar" />
        <div v-else class="rail-avatar-fallback">{{ userInitial }}</div>
      </div>
      <strong class="rail-profile-name">{{ displayName }}</strong>
      <span class="rail-profile-email">{{ displayEmail }}</span>
    </div>

    <nav class="rail-nav" aria-label="应用导航">
      <button type="button" class="rail-button" :class="{ active: viewMode === 'home' }" @click="$emit('open-home')">
        <svg class="rail-button-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M4.5 10.5 12 4l7.5 6.5v8a1 1 0 0 1-1 1h-4.5v-5h-4v5H5.5a1 1 0 0 1-1-1z"
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.8"
          />
        </svg>
        <span class="rail-button-label">主页</span>
      </button>

      <button
        type="button"
        class="rail-button"
        :class="{ active: viewMode === 'templates' }"
        @click="$emit('open-templates')"
      >
        <svg class="rail-button-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M7 4.75h7.5L18.25 8.5V18A1.25 1.25 0 0 1 17 19.25H7A1.25 1.25 0 0 1 5.75 18V6A1.25 1.25 0 0 1 7 4.75Z"
            fill="none"
            stroke="currentColor"
            stroke-linejoin="round"
            stroke-width="1.7"
          />
          <path d="M14.5 4.75V8.5h3.75" fill="none" stroke="currentColor" stroke-linejoin="round" stroke-width="1.7" />
          <path d="M8.75 12h6.5M8.75 15.25h6.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.7" />
        </svg>
        <span class="rail-button-label">模板</span>
      </button>

      <button
        type="button"
        class="rail-button"
        :class="{ active: viewMode === 'settings' }"
        @click="$emit('open-settings')"
      >
        <svg class="rail-button-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M12 8.8a3.2 3.2 0 1 1 0 6.4 3.2 3.2 0 0 1 0-6.4Z"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
          />
          <path
            d="m3.8 13.3 1.7.3a6.8 6.8 0 0 0 .7 1.8l-1 1.4 1.8 1.8 1.4-1a6.8 6.8 0 0 0 1.8.7l.3 1.7h2.5l.3-1.7a6.8 6.8 0 0 0 1.8-.7l1.4 1 1.8-1.8-1-1.4a6.8 6.8 0 0 0 .7-1.8l1.7-.3v-2.5l-1.7-.3a6.8 6.8 0 0 0-.7-1.8l1-1.4-1.8-1.8-1.4 1a6.8 6.8 0 0 0-1.8-.7l-.3-1.7h-2.5l-.3 1.7a6.8 6.8 0 0 0-1.8.7l-1.4-1-1.8 1.8 1 1.4a6.8 6.8 0 0 0-.7 1.8l-1.7.3z"
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.4"
          />
        </svg>
        <span class="rail-button-label">设置</span>
      </button>
      <button type="button" class="rail-button" :class="{ active: viewMode === 'graph' }" @click="$emit('open-graph')">
        <svg class="rail-button-icon" viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="6" cy="7" r="2.2" fill="none" stroke="currentColor" stroke-width="1.8" />
          <circle cx="18" cy="6" r="2.2" fill="none" stroke="currentColor" stroke-width="1.8" />
          <circle cx="12" cy="18" r="2.2" fill="none" stroke="currentColor" stroke-width="1.8" />
          <path
            d="M7.9 8.2 10.3 15M16.2 7.7 13.7 15M8 7.2h7.8"
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-width="1.8"
          />
        </svg>
        <span class="rail-button-label">图谱</span>
      </button>

      <button
        type="button"
        class="rail-button"
        :class="{ active: viewMode === 'graph-test' }"
        @click="$emit('open-graph-test')"
      >
        <svg class="rail-button-icon" viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="6" cy="7" r="1.9" fill="currentColor" />
          <circle cx="18" cy="6.5" r="1.9" fill="currentColor" />
          <circle cx="12" cy="17.5" r="1.9" fill="currentColor" />
          <path
            d="M7.7 7.5h8.6M7.2 8.7l3.5 6.3M16.8 8.2l-3.5 6.7"
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-width="1.8"
          />
          <path
            d="M4.5 18.5h4.2M15.3 18.5h4.2"
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-width="1.8"
          />
        </svg>
        <span class="rail-button-label">图测</span>
      </button>

      <button type="button" class="rail-button" @click="$emit('logout')">
        <svg class="rail-button-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M14 4.75H8A1.25 1.25 0 0 0 6.75 6v12c0 .69.56 1.25 1.25 1.25h6"
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.8"
          />
          <path
            d="M10.5 12h8.75M16.25 8.25 20 12l-3.75 3.75"
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.8"
          />
        </svg>
        <span class="rail-button-label">退出</span>
      </button>
    </nav>

    <div class="rail-bottom rail-brand">
      <div class="logo-mark">DL</div>
      <span class="rail-tag">notes</span>
    </div>
  </aside>
</template>

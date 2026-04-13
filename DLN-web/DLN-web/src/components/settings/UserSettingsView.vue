<script setup lang="ts">
import { computed, ref } from 'vue'

interface ProfileForm {
  nickname: string
  email: string
  avatarUrl?: string | null
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

const props = defineProps<{
  loadingProfile: boolean
  loadingAvatarUpload: boolean
  loadingUserSettings: boolean
  loadingThemeOptions: boolean
  currentUsername: string
  profileForm: ProfileForm
  displayName: string
  displayEmail: string
  userInitial: string
  profileAvatarUrl: string
  contentThemeOptions: string[]
  codeThemeOptions: string[]
  contentTheme: string
  codeTheme: string
}>()

const emit = defineEmits<{
  (e: 'save-profile'): void
  (e: 'avatar-change', event: Event): void
  (e: 'theme-selection-change'): void
  (e: 'update:contentTheme', value: string): void
  (e: 'update:codeTheme', value: string): void
}>()

const avatarInputRef = ref<HTMLInputElement | null>(null)

const contentThemeModel = computed({
  get: () => props.contentTheme,
  set: (value: string) => emit('update:contentTheme', value),
})

const codeThemeModel = computed({
  get: () => props.codeTheme,
  set: (value: string) => emit('update:codeTheme', value),
})

function openAvatarPicker() {
  avatarInputRef.value?.click()
}
</script>

<template>
  <div class="settings-grid">
    <article class="main-panel">
      <div class="panel-heading compact">
        <div>
          <p class="eyebrow">个人资料</p>
          <h3>公开显示信息</h3>
        </div>
        <button type="button" class="soft-button accent" :disabled="loadingProfile" @click="$emit('save-profile')">
          {{ loadingProfile ? '保存中...' : '保存资料' }}
        </button>
      </div>

      <div class="profile-summary">
        <div class="avatar-chip">
          <img v-if="profileAvatarUrl" :src="profileAvatarUrl" :alt="`${displayName} 的头像`" class="avatar-chip-image" />
          <span v-else>{{ userInitial }}</span>
        </div>
        <div>
          <strong>{{ displayName }}</strong>
          <p>{{ displayEmail }}</p>
        </div>
      </div>

      <div class="avatar-upload-panel">
        <div class="avatar-upload-preview">
          <img
            v-if="profileAvatarUrl"
            :src="profileAvatarUrl"
            :alt="`${displayName} 的头像预览`"
            class="avatar-upload-image"
          />
          <span v-else>{{ userInitial }}</span>
        </div>
        <div class="avatar-upload-copy">
          <strong>头像上传</strong>
          <p>支持 JPG、PNG、GIF、WebP，文件大小不超过 5MB。</p>
        </div>
        <div class="avatar-upload-actions">
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/png,image/jpeg,image/gif,image/webp"
            class="visually-hidden-input"
            @change="$emit('avatar-change', $event)"
          />
          <button type="button" class="soft-button" :disabled="loadingAvatarUpload" @click="openAvatarPicker">
            {{ loadingAvatarUpload ? '上传中...' : profileAvatarUrl ? '更换头像' : '上传头像' }}
          </button>
        </div>
      </div>

      <label class="field">
        <span>昵称</span>
        <input v-model="profileForm.nickname" type="text" />
      </label>

      <label class="field">
        <span>邮箱</span>
        <input v-model="profileForm.email" type="email" />
      </label>
    </article>

    <article class="main-panel">
      <div class="panel-heading compact">
        <div>
          <p class="eyebrow">安全</p>
          <h3>密码与会话</h3>
        </div>
      </div>

      <label class="field">
        <span>用户名</span>
        <input :value="currentUsername" type="text" readonly />
      </label>

      <label class="field">
        <span>当前密码</span>
        <input v-model="profileForm.oldPassword" type="password" />
      </label>

      <label class="field">
        <span>新密码</span>
        <input v-model="profileForm.newPassword" type="password" />
      </label>

      <label class="field">
        <span>确认新密码</span>
        <input v-model="profileForm.confirmPassword" type="password" />
      </label>
    </article>

    <article class="main-panel">
      <div class="panel-heading compact">
        <div>
          <p class="eyebrow">编辑器</p>
          <h3>内容与代码主题</h3>
        </div>
      </div>

      <label class="field">
        <span>内容主题</span>
        <select
          v-model="contentThemeModel"
          :disabled="loadingUserSettings || loadingThemeOptions"
          @change="$emit('theme-selection-change')"
        >
          <option v-for="theme in contentThemeOptions" :key="theme" :value="theme">
            {{ theme }}
          </option>
        </select>
      </label>

      <label class="field">
        <span>代码主题</span>
        <select
          v-model="codeThemeModel"
          :disabled="loadingUserSettings || loadingThemeOptions"
          @change="$emit('theme-selection-change')"
        >
          <option v-for="theme in codeThemeOptions" :key="theme" :value="theme">
            {{ theme }}
          </option>
        </select>
      </label>
    </article>
  </div>
</template>

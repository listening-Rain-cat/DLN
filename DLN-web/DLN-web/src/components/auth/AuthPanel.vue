<script setup lang="ts">
import heroImg from '../../assets/hero.png'

type NoticeType = 'success' | 'error'
type AuthMode = 'login' | 'register'

interface AuthForm {
  mode: AuthMode
  username: string
  password: string
  confirmPassword: string
  nickname: string
  email: string
}

defineProps<{
  authForm: AuthForm
  authTitle: string
  authSubmitText: string
  loadingAuth: boolean
  noticeText: string
  noticeType: NoticeType
}>()

defineEmits<{
  (e: 'submit'): void
}>()
</script>

<template>
  <div class="auth-shell">
    <section class="auth-layout">
      <article class="auth-hero-panel">
        <div class="auth-hero-visual">
          <img :src="heroImg" alt="双链笔记系统插图" class="auth-hero-image" />
        </div>

        <div class="auth-hero-content">
          <p class="eyebrow">双链笔记系统</p>
          <h1>搭建属于你的个人知识网络</h1>
          <p class="auth-copy">你可以管理多个知识库，用 Vditor 编写 Markdown。</p>

          <div class="hero-points">
            <div class="hero-point">
              <strong>多个知识库</strong>
              <span>把学习、工作和项目内容分开整理。</span>
            </div>
            <div class="hero-point">
              <strong>双链笔记</strong>
              <span>让相关笔记彼此关联，构建清晰的知识网络。</span>
            </div>
            <div class="hero-point">
              <strong>Markdown 编辑</strong>
              <span>支持高效写作，输入简洁，阅读舒适。</span>
            </div>
          </div>
        </div>
      </article>

      <form class="auth-card" @submit.prevent="$emit('submit')">
        <div class="auth-tabs">
          <button
            type="button"
            class="auth-tab"
            :class="{ active: authForm.mode === 'login' }"
            @click="authForm.mode = 'login'"
          >
            登录
          </button>
          <button
            type="button"
            class="auth-tab"
            :class="{ active: authForm.mode === 'register' }"
            @click="authForm.mode = 'register'"
          >
            注册
          </button>
        </div>

        <div class="modal-heading">
          <p class="modal-copy">{{ authForm.mode === 'login' ? '登录' : '创建账号' }}</p>
          <h3>{{ authTitle }}</h3>
        </div>

        <label class="field">
          <span>用户名</span>
          <input v-model="authForm.username" type="text" autocomplete="username" />
        </label>

        <label v-if="authForm.mode === 'register'" class="field">
          <span>昵称</span>
          <input v-model="authForm.nickname" type="text" autocomplete="nickname" />
        </label>

        <label v-if="authForm.mode === 'register'" class="field">
          <span>邮箱</span>
          <input v-model="authForm.email" type="email" autocomplete="email" />
        </label>

        <label class="field">
          <span>密码</span>
          <input
            v-model="authForm.password"
            type="password"
            :autocomplete="authForm.mode === 'login' ? 'current-password' : 'new-password'"
          />
        </label>

        <label v-if="authForm.mode === 'register'" class="field">
          <span>确认密码</span>
          <input v-model="authForm.confirmPassword" type="password" autocomplete="new-password" />
        </label>

        <div v-if="authForm.mode === 'login'" class="auth-card-fill">
          <div class="auth-card-intro">
            <p class="auth-card-kicker">快速开始</p>
            <strong>登录后继续你的知识整理</strong>
            <span>回到知识库和编辑区，延续上一次的写作与关联。</span>
          </div>

          <div class="auth-card-highlights">
            <div class="auth-card-highlight">
              <strong>知识库分区</strong>
              <span>把学习、工作和项目内容拆开管理，结构更清晰。</span>
            </div>
            <div class="auth-card-highlight">
              <strong>双链关联</strong>
              <span>笔记之间可以互相引用，帮助你快速串联上下文。</span>
            </div>
          </div>
        </div>

        <div v-if="noticeText" class="notice" :class="noticeType">
          {{ noticeText }}
        </div>

        <button type="submit" class="soft-button accent auth-submit" :disabled="loadingAuth">
          {{ authSubmitText }}
        </button>
      </form>
    </section>
  </div>
</template>

import type { Ref } from 'vue'
import {
  API_BASE,
  DEFAULT_CODE_THEME,
  DEFAULT_CONTENT_THEME,
  DEFAULT_CONTENT_THEME_OPTIONS,
  LEGACY_TOKEN_KEY,
  normalizeOptionalText,
  normalizeThemeName,
  normalizeThemeOptions,
  TOKEN_KEY,
  USER_KEY,
} from './shared'
import type { ApiResult, AuthMode, LoginResponse, NoticeType, UserInfo, UserSettings, UserThemeOptions, ViewMode } from './shared'

interface AuthFormState {
  mode: AuthMode
  username: string
  password: string
  confirmPassword: string
  nickname: string
  email: string
}

interface ProfileFormState {
  nickname: string
  email: string
  avatarUrl: string
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

interface LoadingState {
  auth: boolean
  profile: boolean
  userSettings: boolean
  themeOptions: boolean
}

interface UseSessionManagerOptions {
  token: Ref<string>
  currentUser: Ref<UserInfo | null>
  authForm: AuthFormState
  profileForm: ProfileFormState
  loading: LoadingState
  userContentTheme: Ref<string>
  userCodeTheme: Ref<string>
  contentThemeOptions: Ref<string[]>
  codeThemeOptions: Ref<string[]>
  viewMode: Ref<ViewMode>
  showNotice: (text: string, type?: NoticeType) => void
  onUnauthorized: () => void
  onAfterAuthSuccess: () => Promise<void>
}

export function useSessionManager(options: UseSessionManagerOptions) {
  let userSettingsRequestId = 0

  function syncProfileForm() {
    options.profileForm.nickname = options.currentUser.value?.nickname ?? ''
    options.profileForm.email = options.currentUser.value?.email ?? ''
    options.profileForm.avatarUrl = options.currentUser.value?.avatarUrl ?? ''
    options.profileForm.oldPassword = ''
    options.profileForm.newPassword = ''
    options.profileForm.confirmPassword = ''
  }

  function persistUser(user: UserInfo | null) {
    options.currentUser.value = user

    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(USER_KEY)
    }

    syncProfileForm()
  }

  function applyLoginUser(user: LoginResponse) {
    persistUser({
      id: user.id,
      username: user.username,
      email: user.email,
      nickname: user.nickname,
      avatarUrl: user.avatarUrl,
      status: user.status,
      createdTime: user.createdTime,
      updatedTime: user.updatedTime,
    })
  }

  async function request<T>(path: string, init: RequestInit = {}, withAuth = true): Promise<T> {
    const headers = new Headers(init.headers ?? {})

    if (!(init.body instanceof FormData) && init.body != null && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }

    if (withAuth && options.token.value) {
      headers.set(
        'Authorization',
        options.token.value.startsWith('Bearer ') ? options.token.value : `Bearer ${options.token.value}`,
      )
    }

    let response: Response

    try {
      response = await fetch(`${API_BASE}${path}`, {
        ...init,
        headers,
      })
    } catch {
      throw new Error('无法连接后端服务，请确认后端已启动并允许当前页面访问。')
    }

    const rawText = await response.text()
    let payload: ApiResult<T> | null = null

    if (rawText) {
      try {
        payload = JSON.parse(rawText) as ApiResult<T>
      } catch {
        payload = null
      }
    }

    if (response.status === 401) {
      options.onUnauthorized()
      options.showNotice('登录状态已过期，请重新登录。', 'error')
      throw new Error(payload?.message || '登录状态已过期。')
    }

    if (!response.ok || !payload || payload.code !== 200) {
      throw new Error(payload?.message || `请求失败，状态码 ${response.status}。`)
    }

    return payload.data
  }

  async function fetchUserInfo() {
    const user = await request<UserInfo>('/userInfo')
    persistUser(user)
  }

  function applyUserSettings(settings?: Partial<UserSettings> | null) {
    options.userContentTheme.value = normalizeThemeName(settings?.contentTheme, DEFAULT_CONTENT_THEME)
    options.userCodeTheme.value = normalizeThemeName(settings?.codeTheme, DEFAULT_CODE_THEME)
    options.contentThemeOptions.value = normalizeThemeOptions(options.contentThemeOptions.value, [
      options.userContentTheme.value,
    ])
    options.codeThemeOptions.value = normalizeThemeOptions(options.codeThemeOptions.value, [options.userCodeTheme.value])
  }

  function applyUserThemeOptions(optionsPayload?: Partial<UserThemeOptions> | null) {
    const defaultContentTheme = normalizeThemeName(optionsPayload?.defaultContentTheme, DEFAULT_CONTENT_THEME)
    const defaultCodeTheme = normalizeThemeName(optionsPayload?.defaultCodeTheme, DEFAULT_CODE_THEME)

    options.contentThemeOptions.value = normalizeThemeOptions(optionsPayload?.contentThemes, [
      ...DEFAULT_CONTENT_THEME_OPTIONS,
      defaultContentTheme,
    ])
    options.codeThemeOptions.value = normalizeThemeOptions(optionsPayload?.codeThemes, [defaultCodeTheme])
    options.contentThemeOptions.value = normalizeThemeOptions(options.contentThemeOptions.value, [
      options.userContentTheme.value,
    ])
    options.codeThemeOptions.value = normalizeThemeOptions(options.codeThemeOptions.value, [options.userCodeTheme.value])
  }

  async function fetchUserSettings() {
    options.loading.userSettings = true

    try {
      const settings = await request<UserSettings>('/user/settings')
      applyUserSettings(settings)
    } finally {
      options.loading.userSettings = false
    }
  }

  async function fetchUserThemeOptions() {
    options.loading.themeOptions = true

    try {
      const optionsPayload = await request<UserThemeOptions>('/user/settings/theme-options')
      applyUserThemeOptions(optionsPayload)
    } catch {
      applyUserThemeOptions()
    } finally {
      options.loading.themeOptions = false
    }
  }

  async function submitAuth() {
    const mode = options.authForm.mode

    try {
      if (!options.authForm.username.trim() || !options.authForm.password.trim()) {
        throw new Error('用户名和密码不能为空。')
      }

      options.loading.auth = true

      if (mode === 'register') {
        if (!options.authForm.nickname.trim() || !options.authForm.email.trim()) {
          throw new Error('注册时请填写昵称和邮箱。')
        }

        if (options.authForm.password !== options.authForm.confirmPassword) {
          throw new Error('两次输入的密码不一致。')
        }

        await request(
          '/register',
          {
            method: 'POST',
            body: JSON.stringify({
              username: options.authForm.username.trim(),
              password: options.authForm.password,
              confirmPassword: options.authForm.confirmPassword,
              email: options.authForm.email.trim(),
              nickname: options.authForm.nickname.trim(),
            }),
          },
          false,
        )
      }

      const loginData = await request<LoginResponse>(
        '/login',
        {
          method: 'POST',
          body: JSON.stringify({
            username: options.authForm.username.trim(),
            password: options.authForm.password,
          }),
        },
        false,
      )

      options.token.value = loginData.token
      localStorage.setItem(TOKEN_KEY, loginData.token)
      localStorage.setItem(LEGACY_TOKEN_KEY, loginData.token)
      applyLoginUser(loginData)
      options.authForm.mode = 'login'
      options.authForm.password = ''
      options.authForm.confirmPassword = ''
      options.viewMode.value = 'home'
      await options.onAfterAuthSuccess()
      options.showNotice(mode === 'login' ? '登录成功。' : '注册成功。')
    } catch (error) {
      options.showNotice((error as Error).message, 'error')
    } finally {
      options.loading.auth = false
    }
  }

  async function saveUserSettings(settings: { contentTheme: string; codeTheme: string }, silent = false) {
    const requestId = ++userSettingsRequestId

    try {
      options.loading.userSettings = true

      const savedSettings = await request<UserSettings>('/user/settings', {
        method: 'PUT',
        body: JSON.stringify({
          contentTheme: normalizeThemeName(settings.contentTheme, DEFAULT_CONTENT_THEME),
          codeTheme: normalizeThemeName(settings.codeTheme, DEFAULT_CODE_THEME),
        }),
      })

      if (requestId !== userSettingsRequestId) {
        return
      }

      applyUserSettings(savedSettings)
      if (!silent) {
        options.showNotice('编辑器主题设置已更新。')
      }
    } catch (error) {
      if (requestId !== userSettingsRequestId) {
        return
      }

      options.showNotice((error as Error).message, 'error')
    } finally {
      if (requestId === userSettingsRequestId) {
        options.loading.userSettings = false
      }
    }
  }

  function handleEditorThemeChange(settings: { contentTheme: string; codeTheme: string }) {
    const nextSettings = {
      contentTheme: normalizeThemeName(settings.contentTheme, DEFAULT_CONTENT_THEME),
      codeTheme: normalizeThemeName(settings.codeTheme, DEFAULT_CODE_THEME),
    }

    if (
      options.userContentTheme.value === nextSettings.contentTheme &&
      options.userCodeTheme.value === nextSettings.codeTheme
    ) {
      return
    }

    applyUserSettings(nextSettings)
    void saveUserSettings(nextSettings, true)
  }

  function handleSettingsThemeSelectionChange() {
    const nextSettings = {
      contentTheme: normalizeThemeName(options.userContentTheme.value, DEFAULT_CONTENT_THEME),
      codeTheme: normalizeThemeName(options.userCodeTheme.value, DEFAULT_CODE_THEME),
    }

    applyUserSettings(nextSettings)
    void saveUserSettings(nextSettings)
  }

  async function saveProfile() {
    try {
      if (!options.profileForm.nickname.trim() || !options.profileForm.email.trim()) {
        throw new Error('昵称和邮箱不能为空。')
      }

      if (options.profileForm.oldPassword || options.profileForm.newPassword || options.profileForm.confirmPassword) {
        if (!options.profileForm.oldPassword) {
          throw new Error('修改密码前请先输入当前密码。')
        }

        if (!options.profileForm.newPassword) {
          throw new Error('请输入新密码。')
        }

        if (options.profileForm.newPassword !== options.profileForm.confirmPassword) {
          throw new Error('两次输入的新密码不一致。')
        }
      }

      options.loading.profile = true

      await request('/user', {
        method: 'PUT',
        body: JSON.stringify({
          nickname: options.profileForm.nickname.trim(),
          email: options.profileForm.email.trim(),
          avatarUrl: normalizeOptionalText(options.profileForm.avatarUrl),
          oldPassword: normalizeOptionalText(options.profileForm.oldPassword),
          newPassword: normalizeOptionalText(options.profileForm.newPassword),
        }),
      })

      await fetchUserInfo()
      options.profileForm.oldPassword = ''
      options.profileForm.newPassword = ''
      options.profileForm.confirmPassword = ''
      options.showNotice('个人资料已更新。')
    } catch (error) {
      options.showNotice((error as Error).message, 'error')
    } finally {
      options.loading.profile = false
    }
  }

  return {
    applyLoginUser,
    applyUserSettings,
    applyUserThemeOptions,
    fetchUserInfo,
    fetchUserSettings,
    fetchUserThemeOptions,
    handleEditorThemeChange,
    handleSettingsThemeSelectionChange,
    persistUser,
    request,
    saveProfile,
    saveUserSettings,
    submitAuth,
    syncProfileForm,
  }
}

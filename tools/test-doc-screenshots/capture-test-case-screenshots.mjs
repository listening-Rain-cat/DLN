import { mkdir } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright-core'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const ROOT = path.resolve(__dirname, '..', '..')
const OUTPUT_DIR = path.join(ROOT, 'docs', 'screenshots', 'test-cases')
const EDGE_PATH = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const APP_BASE = 'http://127.0.0.1:5173'
const API_BASE = 'http://127.0.0.1:8080'

const suffix = new Date().toISOString().replace(/\D/g, '').slice(-10)

const state = {
  baseUser: {
    username: `base${suffix}`,
    nickname: `Base${suffix.slice(-4)}`,
    email: `base${suffix}@example.com`,
    password: `Base${suffix.slice(-6)}`,
  },
  workUser: {
    username: `case${suffix}`,
    nickname: `Case${suffix.slice(-4)}`,
    email: `case${suffix}@example.com`,
    password: `Case${suffix.slice(-6)}`,
  },
  finalPassword: `Done${suffix.slice(-6)}`,
  knowledgeBaseName: `kb-${suffix}`,
  updatedKnowledgeBaseName: `kb-${suffix}-v2`,
  updatedKnowledgeBaseDescription: `updated-description-${suffix}`,
  folderName: `folder-${suffix}`,
  noteA: `overview-${suffix}`,
  noteB: `algorithm-${suffix}`,
  templateName: `template-${suffix}`,
}

let currentPassword = state.workUser.password

function log(message) {
  console.log(`[capture] ${message}`)
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function apiRequestFull(pathname, { method = 'GET', token, body } = {}) {
  const headers = {}
  if (body != null) {
    headers['Content-Type'] = 'application/json'
  }
  if (token) {
    headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE}${pathname}`, {
    method,
    headers,
    body: body == null ? undefined : JSON.stringify(body),
  })

  let payload = null
  try {
    payload = await response.json()
  } catch {
    payload = null
  }

  if (!response.ok || !payload || payload.code !== 200) {
    throw new Error(payload?.message || `API ${method} ${pathname} failed with ${response.status}`)
  }

  return payload
}

async function apiRequest(pathname, { method = 'GET', token, body } = {}) {
  const payload = await apiRequestFull(pathname, { method, token, body })
  return payload.data
}

async function registerUser(user) {
  return apiRequest('/register', {
    method: 'POST',
    body: {
      username: user.username,
      password: user.password,
      confirmPassword: user.password,
      email: user.email,
      nickname: user.nickname,
    },
  })
}

async function loginApi(username, password) {
  return apiRequest('/login', {
    method: 'POST',
    body: {
      username,
      password,
    },
  })
}

async function waitForNotice(page) {
  const notice = page.locator('.notice').last()
  await notice.waitFor({ state: 'visible', timeout: 8000 })
  await sleep(300)
  return notice
}

async function saveShot(page, fileName) {
  await page.evaluate(() => window.scrollTo(0, 0))
  await sleep(250)
  await page.screenshot({
    path: path.join(OUTPUT_DIR, fileName),
    fullPage: false,
  })
}

async function saveApiShot(context, fileName, title, endpoint, requestBody, responseBody) {
  const reportPage = await context.newPage()
  const requestText = JSON.stringify(requestBody, null, 2)
  const responseText = JSON.stringify(responseBody, null, 2)
  const html = `<!doctype html>
  <html lang="zh-CN">
    <head>
      <meta charset="utf-8" />
      <title>${title}</title>
      <style>
        body { margin: 0; font-family: "Microsoft YaHei", sans-serif; background: linear-gradient(180deg, #f7f3ea, #efe6d9); color: #19383a; }
        .shell { width: 1400px; margin: 0 auto; padding: 28px; }
        .card { background: rgba(255,255,255,0.9); border-radius: 20px; padding: 24px; box-shadow: 0 18px 40px rgba(21,47,48,0.08); margin-bottom: 18px; }
        h1 { margin: 0 0 8px; font-size: 28px; }
        h2 { margin: 0 0 14px; font-size: 22px; }
        .meta { color: rgba(25,56,58,0.68); font-size: 14px; margin-bottom: 10px; }
        .tag { display: inline-block; padding: 8px 14px; border-radius: 999px; background: linear-gradient(135deg, #255754, #c48a36); color: #fff; font-size: 14px; margin-top: 6px; }
        pre { margin: 0; padding: 18px; border-radius: 14px; overflow: auto; background: #132829; color: #f5f5f2; font-size: 15px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
      </style>
    </head>
    <body>
      <div class="shell">
        <section class="card">
          <h1>${title}</h1>
          <div class="meta">接口运行结果截图</div>
          <div><strong>接口地址：</strong>${endpoint}</div>
          <div class="tag">实际运行成功</div>
        </section>
        <section class="card">
          <h2>请求参数</h2>
          <pre>${requestText}</pre>
        </section>
        <section class="card">
          <h2>返回结果</h2>
          <pre>${responseText}</pre>
        </section>
      </div>
    </body>
  </html>`

  await reportPage.setViewportSize({ width: 1600, height: 1100 })
  await reportPage.setContent(html, { waitUntil: 'load' })
  await saveShot(reportPage, fileName)
  await reportPage.close()
}

async function selectAuthMode(page, mode) {
  const index = mode === 'login' ? 0 : 1
  await page.locator('.auth-tab').nth(index).click()
  await sleep(300)
}

async function fillLogin(page, { username, password }) {
  const inputs = page.locator('.auth-card .field input')
  await inputs.nth(0).fill(username)
  await inputs.nth(1).fill(password)
}

async function fillRegister(page, { username, nickname, email, password, confirmPassword }) {
  const inputs = page.locator('.auth-card .field input')
  await inputs.nth(0).fill(username)
  await inputs.nth(1).fill(nickname)
  await inputs.nth(2).fill(email)
  await inputs.nth(3).fill(password)
  await inputs.nth(4).fill(confirmPassword)
}

async function submitAuth(page) {
  await page.locator('.auth-submit').click()
}

async function logout(page) {
  await page.locator('.rail-nav .rail-button').nth(4).click()
  await page.locator('.auth-shell').waitFor({ state: 'visible', timeout: 8000 })
}

async function openHome(page) {
  await page.locator('.rail-nav .rail-button').nth(0).click()
  await sleep(400)
}

async function openKnowledgeBaseManager(page) {
  await openHome(page)
  await page.locator('.knowledge-base-switcher').waitFor({ state: 'visible', timeout: 8000 })
  await sleep(400)
}

async function openTemplates(page) {
  await page.locator('.rail-nav .rail-button').nth(1).click()
  await page.locator('.settings-template-panel').waitFor({ state: 'visible', timeout: 8000 })
}

async function openSettings(page) {
  await page.locator('.rail-nav .rail-button').nth(2).click()
  await page.locator('.settings-grid').waitFor({ state: 'visible', timeout: 8000 })
}

async function openGraph(page) {
  await page.locator('.rail-nav .rail-button').nth(3).click()
  await page.locator('.graph-stage-d3').waitFor({ state: 'visible', timeout: 8000 })
}

async function createKnowledgeBase(page, name, description) {
  await page.locator('.knowledge-base-switcher .soft-button.accent').click()
  const modal = page.locator('.modal-card').last()
  await modal.locator('input').first().fill(name)
  await modal.locator('textarea').first().fill(description)
  await modal.locator('.soft-button.accent').click()
}

async function closeOpenModal(page) {
  const modal = page.locator('.modal-card').last()
  if (await modal.count()) {
    await modal.locator('.soft-button').first().click()
    await sleep(300)
  }
}

async function findKnowledgeBaseEntry(page, kbName) {
  return page.locator('.knowledge-base-entry').filter({ hasText: kbName }).first()
}

async function selectKnowledgeBase(page, kbName) {
  const entry = await findKnowledgeBaseEntry(page, kbName)
  await entry.locator('.knowledge-base-entry-main').click()
  await page.locator('.resource-directory-tree-shell').waitFor({ state: 'visible', timeout: 8000 })
  await sleep(500)
}

async function createRootFolder(page, folderName, rootSelector = '.editor-panel') {
  await page.locator(`${rootSelector} .toolbar-icon-button`).first().click()
  const modal = page.locator('.modal-card').last()
  await modal.locator('input').first().fill(folderName)
  await modal.locator('.soft-button.accent').click()
}

async function createRootNote(page, noteTitle, rootSelector) {
  await page.locator(`${rootSelector} .toolbar-icon-button.accent`).first().click()
  const modal = page.locator('.modal-card').last()
  await modal.locator('input').first().fill(noteTitle)
  await modal.locator('.soft-button.accent').click()
  await page.locator('.note-title-input').waitFor({ state: 'visible', timeout: 8000 })
  await sleep(700)
}

function flattenTree(nodes) {
  const result = []
  for (const node of nodes) {
    result.push(node)
    if (node.children?.length) {
      result.push(...flattenTree(node.children))
    }
  }
  return result
}

async function getWorkToken() {
  const loginData = await loginApi(state.workUser.username, currentPassword)
  return loginData.token
}

async function locateKnowledgeBaseAndNotes() {
  const token = await getWorkToken()
  const knowledgeBases = await apiRequest('/knowledgeBases', { token })
  const kb = knowledgeBases.find((item) => item.name === state.updatedKnowledgeBaseName)
  if (!kb) {
    throw new Error('Unable to find target knowledge base')
  }

  const tree = await apiRequest(`/knowledgeBases/${kb.id}/tree`, { token })
  const flat = flattenTree(tree)
  const folder = flat.find((item) => item.type === 'folder' && item.name === state.folderName)
  const noteA = flat.find((item) => item.type === 'note' && item.name === state.noteA)
  const noteB = flat.find((item) => item.type === 'note' && item.name === state.noteB)

  if (!folder || !noteA || !noteB) {
    throw new Error('Unable to locate folder or notes from tree data')
  }

  return {
    token,
    kb,
    folder,
    noteA,
    noteB,
  }
}

async function configureLinkedNotes() {
  const { token, noteA, noteB } = await locateKnowledgeBaseAndNotes()

  await apiRequest(`/notes/${noteA.id}/content/autosave`, {
    method: 'PUT',
    token,
    body: {
      markdownContent: `## Overview\n\nLink to [[${state.noteB}]].\n\nAutosave baseline ${suffix}.`,
    },
  })

  await apiRequest(`/notes/${noteB.id}/content/autosave`, {
    method: 'PUT',
    token,
    body: {
      markdownContent: `## Algorithm\n\nBacklink to [[${state.noteA}]].`,
    },
  })
}

async function openNoteFromTree(page, title) {
  const treeRow = page.locator('.tree-main').filter({ hasText: title }).first()
  await treeRow.click()
  await page.locator('.note-title-input').waitFor({ state: 'visible', timeout: 8000 })
  await expectInputValue(page, title)
  await sleep(600)
}

async function expectInputValue(page, value) {
  await page.waitForFunction(
    (expected) => {
      const input = document.querySelector('.note-title-input')
      return input && input.value === expected
    },
    value,
    { timeout: 8000 },
  )
}

async function returnToKnowledgeBaseDirectory(page) {
  const button = page.locator('.library-footer-button').first()
  await button.click()
  await page.locator('.resource-directory-tree-shell').waitFor({ state: 'visible', timeout: 8000 })
  await sleep(500)
}

async function run() {
  await mkdir(OUTPUT_DIR, { recursive: true })
  log(`output: ${OUTPUT_DIR}`)

  log('registering base test user via API')
  await registerUser(state.baseUser)

  const browser = await chromium.launch({
    headless: false,
    executablePath: EDGE_PATH,
    args: ['--disable-gpu', '--no-sandbox'],
  })

  const context = await browser.newContext({
    viewport: { width: 1600, height: 1100 },
    deviceScaleFactor: 1,
  })

  const page = await context.newPage()
  page.on('pageerror', (error) => {
    console.error('[pageerror]', error)
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      console.error('[console.error]', message.text())
    }
  })

  try {
    log('opening app')
    await page.goto(APP_BASE, { waitUntil: 'networkidle' })
    await page.locator('.auth-shell').waitFor({ state: 'visible', timeout: 10000 })

    log('capturing login test cases')
    await selectAuthMode(page, 'login')
    await fillLogin(page, { username: '', password: '' })
    await submitAuth(page)
    await waitForNotice(page)
    await saveShot(page, 'tc-6-2-1-login-empty.png')

    await fillLogin(page, { username: state.baseUser.username, password: 'wrong123' })
    await submitAuth(page)
    await waitForNotice(page)
    await saveShot(page, 'tc-6-2-2-login-wrong-password.png')

    await fillLogin(page, { username: state.baseUser.username, password: state.baseUser.password })
    await submitAuth(page)
    await page.locator('.shell').waitFor({ state: 'visible', timeout: 10000 })
    await waitForNotice(page)
    await saveShot(page, 'tc-6-2-3-login-success.png')
    await logout(page)

    log('capturing register test cases')
    await selectAuthMode(page, 'register')
    await fillRegister(page, {
      username: `u${suffix}`,
      nickname: '',
      email: '',
      password: 'abc123',
      confirmPassword: 'abc123',
    })
    await submitAuth(page)
    await waitForNotice(page)
    await saveShot(page, 'tc-6-3-1-register-missing-fields.png')

    await fillRegister(page, {
      username: `u${suffix}`,
      nickname: `Nick${suffix.slice(-3)}`,
      email: `tmp${suffix}@example.com`,
      password: 'abc123',
      confirmPassword: 'abc124',
    })
    await submitAuth(page)
    await waitForNotice(page)
    await saveShot(page, 'tc-6-3-2-register-password-mismatch.png')

    await fillRegister(page, {
      username: state.baseUser.username,
      nickname: `Nick${suffix.slice(-3)}`,
      email: `dup${suffix}@example.com`,
      password: 'abc123',
      confirmPassword: 'abc123',
    })
    await submitAuth(page)
    await waitForNotice(page)
    await saveShot(page, 'tc-6-3-3-register-duplicate-username.png')

    await fillRegister(page, {
      username: state.workUser.username,
      nickname: state.workUser.nickname,
      email: state.workUser.email,
      password: state.workUser.password,
      confirmPassword: state.workUser.password,
    })
    await submitAuth(page)
    await page.locator('.shell').waitFor({ state: 'visible', timeout: 10000 })
    await waitForNotice(page)
    await saveShot(page, 'tc-6-3-4-register-success.png')

    log('capturing knowledge base test cases')
    await openKnowledgeBaseManager(page)
    await createKnowledgeBase(page, state.knowledgeBaseName, `description-${suffix}`)
    await waitForNotice(page)
    await saveShot(page, 'tc-6-4-1-create-knowledge-base.png')

    await openKnowledgeBaseManager(page)
    await createKnowledgeBase(page, state.knowledgeBaseName, `duplicate-${suffix}`)
    await waitForNotice(page)
    await saveShot(page, 'tc-6-4-2-create-duplicate-knowledge-base.png')
    await closeOpenModal(page)

    await openKnowledgeBaseManager(page)
    const kbEntry = await findKnowledgeBaseEntry(page, state.knowledgeBaseName)
    await kbEntry.locator('.mini-button').first().click()
    const editModal = page.locator('.modal-card').last()
    await editModal.locator('input').first().fill(state.updatedKnowledgeBaseName)
    await editModal.locator('textarea').first().fill(state.updatedKnowledgeBaseDescription)
    await editModal.locator('.soft-button.accent').click()
    await waitForNotice(page)
    await saveShot(page, 'tc-6-4-3-update-knowledge-base.png')

    await createRootFolder(page, state.folderName)
    await waitForNotice(page)
    await saveShot(page, 'tc-6-4-4-create-folder.png')

    log('capturing note and history test cases')
    await createRootNote(page, state.noteA, '.editor-panel')
    await waitForNotice(page)
    await saveShot(page, 'tc-6-5-1-create-note-a.png')

    await createRootNote(page, state.noteB, '.library-panel')
    await waitForNotice(page)
    await saveShot(page, 'tc-6-5-2-create-note-b.png')

    const { token, noteA, noteB } = await locateKnowledgeBaseAndNotes()
    const autoSavePayload = {
      markdownContent: `## Overview\n\nLink to [[${state.noteB}]].\n\nAutosave baseline ${suffix}.`,
    }
    const autoSaveResponse = await apiRequestFull(`/notes/${noteA.id}/content/autosave`, {
      method: 'PUT',
      token,
      body: autoSavePayload,
    })
    await apiRequestFull(`/notes/${noteB.id}/content/autosave`, {
      method: 'PUT',
      token,
      body: {
        markdownContent: `## Algorithm\n\nBacklink to [[${state.noteA}]].`,
      },
    })
    await saveApiShot(
      context,
      'tc-6-5-3-note-content-autosave.png',
      '笔记正文自动保存接口运行截图',
      `PUT /notes/${noteA.id}/content/autosave`,
      autoSavePayload,
      autoSaveResponse,
    )
    await openNoteFromTree(page, state.noteA)
    await saveShot(page, 'tc-6-5-4-bidirectional-link-created.png')

    await page.locator('.outline-panel-switcher-button').nth(1).click()
    await sleep(500)
    await saveShot(page, 'tc-6-5-5-query-note-detail.png')

    await page.locator('.editor-frame-toolbar .note-save-button.accent').click()
    await waitForNotice(page)
    await saveShot(page, 'tc-6-5-6-create-history-version.png')

    await page.locator('.editor-frame-toolbar .note-save-button').first().click()
    await page.locator('.note-history-modal').waitFor({ state: 'visible', timeout: 8000 })
    await sleep(500)
    await saveShot(page, 'tc-6-5-7-query-history-list.png')
    await page.locator('.note-history-modal .modal-actions .soft-button').click()
    await page.locator('.note-history-modal').waitFor({ state: 'hidden', timeout: 8000 })
    await sleep(400)

    log('capturing search and graph test cases')
    await returnToKnowledgeBaseDirectory(page)
    await page.locator('.resource-directory-section .workbench-panel-switcher-button').nth(1).click()
    await page.locator('.search-workbench-section').waitFor({ state: 'visible', timeout: 8000 })
    await page.locator('.search-field-keyword input').fill(suffix)
    await page.locator('.search-submit-button').click()
    await page.locator('.search-result-card').first().waitFor({ state: 'visible', timeout: 8000 })
    await sleep(500)
    await saveShot(page, 'tc-6-6-1-search-notes.png')

    await openGraph(page)
    await page.locator('.graph-d3-shell, .graph-empty-state').first().waitFor({ state: 'visible', timeout: 8000 })
    await sleep(1800)
    await saveShot(page, 'tc-6-6-2-knowledge-graph.png')

    log('capturing template test cases')
    await openTemplates(page)
    await page.locator('.settings-template-panel .soft-button.accent').first().click()
    const templateModal = page.locator('.template-modal')
    await templateModal.locator('input').first().fill(state.templateName)
    await templateModal.locator('textarea').first().fill(`template-description-${suffix}`)
    await templateModal.locator('.template-content-switcher-button').nth(1).click()
    await templateModal.locator('textarea').nth(1).fill(`# ${state.templateName}\n\n- item 1\n- item 2`)
    await templateModal.locator('.soft-button.accent').click()
    await waitForNotice(page)
    await saveShot(page, 'tc-6-7-1-create-template.png')

    await sleep(500)
    await saveShot(page, 'tc-6-7-2-query-template-list.png')

    log('capturing user settings and password test cases')
    await openSettings(page)
    const profileFields = page.locator('.settings-grid .main-panel').nth(0).locator('.field input')
    await profileFields.nth(0).fill(state.workUser.nickname)
    await profileFields.nth(1).fill(state.workUser.email)
    const passwordFields = page.locator('.settings-grid .main-panel').nth(1).locator('.field input')

    await passwordFields.nth(1).fill(currentPassword)
    await passwordFields.nth(2).fill(`New${suffix.slice(-4)}`)
    await passwordFields.nth(3).fill(`Diff${suffix.slice(-4)}`)
    await page.locator('.settings-grid .main-panel').first().locator('.soft-button.accent').click()
    await waitForNotice(page)
    await saveShot(page, 'tc-6-8-1-password-confirm-mismatch.png')

    await passwordFields.nth(1).fill('wrong-old')
    await passwordFields.nth(2).fill(state.finalPassword)
    await passwordFields.nth(3).fill(state.finalPassword)
    await page.locator('.settings-grid .main-panel').first().locator('.soft-button.accent').click()
    await waitForNotice(page)
    await saveShot(page, 'tc-6-8-2-password-old-wrong.png')

    await passwordFields.nth(1).fill(currentPassword)
    await passwordFields.nth(2).fill(state.finalPassword)
    await passwordFields.nth(3).fill(state.finalPassword)
    await page.locator('.settings-grid .main-panel').first().locator('.soft-button.accent').click()
    await waitForNotice(page)
    currentPassword = state.finalPassword
    await saveShot(page, 'tc-6-8-3-password-update-success.png')

    await logout(page)
    await selectAuthMode(page, 'login')
    await fillLogin(page, { username: state.workUser.username, password: state.finalPassword })
    await submitAuth(page)
    await page.locator('.shell').waitFor({ state: 'visible', timeout: 10000 })
    await waitForNotice(page)
    await saveShot(page, 'tc-6-8-4-login-with-new-password.png')

    log('all screenshots captured successfully')
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})

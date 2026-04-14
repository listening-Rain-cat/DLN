import { computed } from 'vue'
import type { ComputedRef, Ref } from 'vue'
import type { Id, KnowledgeBase, NoteDetail, NoteTemplate, TreeNode, ViewMode } from './shared'

interface UseWorkspaceChromeOptions {
  viewMode: Ref<ViewMode>
  selectedKnowledgeBaseId: Ref<Id | null>
  currentNote: Ref<NoteDetail | null>
  currentFolder: ComputedRef<TreeNode | null>
  selectedKnowledgeBase: ComputedRef<KnowledgeBase | null>
  noteTemplates: Ref<NoteTemplate[]>
  knowledgeBases: Ref<KnowledgeBase[]>
  loading: {
    templates: boolean
    graph: boolean
  }
  graphNodeCount: ComputedRef<number>
  graphEdgeCount: ComputedRef<number>
  autoSavingNote: ComputedRef<boolean>
  autoSaveError: Ref<string>
  noteSaved: ComputedRef<boolean>
  noteCount: ComputedRef<number>
  folderCount: ComputedRef<number>
  displayName: ComputedRef<string>
}

export function useWorkspaceChrome(options: UseWorkspaceChromeOptions) {
  const showKnowledgeBaseSidebar = computed(() => {
    return (
      options.viewMode.value === 'home' &&
      Boolean(options.selectedKnowledgeBaseId.value) &&
      Boolean(options.currentNote.value)
    )
  })

  const resourceTreeStatus = computed(() => {
    if (!showKnowledgeBaseSidebar.value) {
      return '进入知识库后显示'
    }

    return options.selectedKnowledgeBase.value?.name || '未选择知识库'
  })

  const isHomeKnowledgeBaseHub = computed(() => {
    return (
      options.viewMode.value === 'home' &&
      !options.selectedKnowledgeBaseId.value &&
      !options.currentNote.value
    )
  })

  const isKnowledgeBaseWorkspace = computed(() => {
    return (
      options.viewMode.value === 'home' &&
      Boolean(options.selectedKnowledgeBaseId.value) &&
      !options.currentNote.value
    )
  })

  const workspaceTitle = computed(() => {
    if (options.viewMode.value === 'templates') {
      return '通用模板'
    }

    if (options.viewMode.value === 'settings') {
      return '账号设置'
    }

    if (options.viewMode.value === 'graph-d3') {
      return options.selectedKnowledgeBase.value?.name || '图谱'
    }

    return (
      options.currentNote.value?.title ||
      options.currentFolder.value?.name ||
      options.selectedKnowledgeBase.value?.name ||
      '准备开始记录'
    )
  })

  const workspaceStatus = computed(() => {
    if (options.viewMode.value === 'templates') {
      return options.loading.templates ? '模板加载中' : `${options.noteTemplates.value.length} 个模板可用`
    }

    if (options.viewMode.value === 'settings') {
      return '资料与安全设置'
    }

    if (options.viewMode.value === 'graph-d3') {
      if (!options.selectedKnowledgeBase.value) {
        return '等待选择知识库'
      }

      if (options.loading.graph) {
        return 'D3 图谱加载中'
      }

      return `${options.graphNodeCount.value} 个节点 / ${options.graphEdgeCount.value} 条边`
    }

    if (options.currentNote.value) {
      if (options.autoSavingNote.value) {
        return '笔记自动保存中'
      }

      if (options.autoSaveError.value) {
        return '笔记自动保存失败，请手动保存'
      }

      return options.noteSaved.value ? '所有修改已保存' : '有未保存更改'
    }

    if (options.selectedKnowledgeBase.value) {
      return `当前知识库：${options.selectedKnowledgeBase.value.name}`
    }

    return '请选择知识库'
  })

  const breadcrumbItems = computed(() => {
    const items = ['DLN']

    if (options.viewMode.value === 'templates') {
      items.push('模板', '通用模板')
      return items
    }

    if (options.viewMode.value === 'settings') {
      items.push('设置', '账号设置')
      return items
    }

    if (options.viewMode.value === 'graph-d3') {
      items.push('图谱')
      if (options.selectedKnowledgeBase.value?.name) {
        items.push(options.selectedKnowledgeBase.value.name)
      }
      return items
    }

    items.push('主页')

    if (options.selectedKnowledgeBase.value?.name) {
      items.push(options.selectedKnowledgeBase.value.name)
    }

    if (options.currentFolder.value?.name) {
      items.push(options.currentFolder.value.name)
    }

    if (options.currentNote.value?.title) {
      items.push(options.currentNote.value.title)
    }

    return items
  })

  const statusbarLeftItems = computed(() => {
    if (options.viewMode.value === 'templates') {
      return ['模板中心：通用模板管理', '适用范围：跨知识库复用', '下一步：新建、编辑或删除模板']
    }

    if (isHomeKnowledgeBaseHub.value) {
      return ['首页：知识库管理', '当前内容：知识库列表', '下一步：打开或新建知识库']
    }

    if (isKnowledgeBaseWorkspace.value) {
      return [
        '工作台：知识库概览与目录',
        `当前知识库：${options.selectedKnowledgeBase.value?.name || '未命名知识库'}`,
      ]
    }

    return [
      `资源树：${resourceTreeStatus.value}`,
      `主任务区：${workspaceTitle.value}`,
      `局部导航：${options.viewMode.value === 'home' ? '文档大纲' : '辅助面板'}`,
      `路径：${breadcrumbItems.value.join(' / ')}`,
    ]
  })

  const statusbarRightItems = computed(() => {
    if (options.viewMode.value === 'templates') {
      return [
        options.loading.templates ? '模板加载中...' : `模板：${options.noteTemplates.value.length}`,
        `知识库：${options.knowledgeBases.value.length}`,
        `已登录：${options.displayName.value}`,
      ]
    }

    if (isHomeKnowledgeBaseHub.value) {
      return [
        `已登录：${options.displayName.value}`,
        `知识库：${options.knowledgeBases.value.length}`,
        `模板：${options.noteTemplates.value.length}`,
      ]
    }

    if (isKnowledgeBaseWorkspace.value) {
      return [`笔记：${options.noteCount.value}`, `模板：${options.noteTemplates.value.length}`]
    }

    if (options.currentNote.value) {
      return []
    }

    return [
      `知识库 ${options.knowledgeBases.value.length}`,
      `文件夹 ${options.folderCount.value}`,
      `笔记 ${options.noteCount.value}`,
      workspaceStatus.value,
    ]
  })

  return {
    showKnowledgeBaseSidebar,
    resourceTreeStatus,
    isHomeKnowledgeBaseHub,
    isKnowledgeBaseWorkspace,
    workspaceTitle,
    workspaceStatus,
    breadcrumbItems,
    statusbarLeftItems,
    statusbarRightItems,
  }
}

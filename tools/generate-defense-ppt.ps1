param(
    [string]$OutputPath
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutputPath) {
    $OutputPath = Join-Path $repoRoot "docs\双链笔记系统-答辩PPT.pptx"
}

$docsRoot = Join-Path $repoRoot "docs"
$screenshotRoot = Join-Path $docsRoot "screenshots"
$systemImplRoot = Join-Path $screenshotRoot "system-implementation"
$testCaseRoot = Join-Path $screenshotRoot "test-cases"

function New-RgbValue {
    param(
        [int]$Red,
        [int]$Green,
        [int]$Blue
    )

    return $Red + ($Green -shl 8) + ($Blue -shl 16)
}

$script:msoFalse = 0
$script:msoTrue = -1
$script:msoTextOrientationHorizontal = 1
$script:msoShapeRectangle = 1
$script:msoShapeRoundedRectangle = 5
$script:ppLayoutBlank = 12
$script:ppSaveAsOpenXMLPresentation = 24
$script:ppAlignLeft = 1
$script:ppAlignCenter = 2
$script:ppAlignRight = 3

$script:colors = @{
    Navy = New-RgbValue 22 54 96
    Blue = New-RgbValue 59 110 167
    BlueSoft = New-RgbValue 225 236 248
    Accent = New-RgbValue 219 124 71
    AccentSoft = New-RgbValue 253 237 227
    Ink = New-RgbValue 39 49 66
    Muted = New-RgbValue 111 123 146
    Border = New-RgbValue 207 218 232
    Card = New-RgbValue 246 249 253
    White = New-RgbValue 255 255 255
    Light = New-RgbValue 242 246 251
    Success = New-RgbValue 89 163 117
}

function Add-Rectangle {
    param(
        [object]$Slide,
        [double]$Left,
        [double]$Top,
        [double]$Width,
        [double]$Height,
        [int]$FillColor,
        [int]$LineColor = 0,
        [double]$LineWeight = 1,
        [int]$ShapeType = $script:msoShapeRoundedRectangle
    )

    $shape = $Slide.Shapes.AddShape($ShapeType, $Left, $Top, $Width, $Height)
    $shape.Fill.ForeColor.RGB = $FillColor
    $shape.Line.ForeColor.RGB = $LineColor
    $shape.Line.Weight = $LineWeight
    return $shape
}

function Set-TextRangeStyle {
    param(
        [object]$TextRange,
        [string]$FontName,
        [double]$FontSize,
        [int]$Color,
        [bool]$Bold = $false
    )

    $TextRange.Font.Name = $FontName
    $TextRange.Font.Size = $FontSize
    $TextRange.Font.Bold = if ($Bold) { $script:msoTrue } else { $script:msoFalse }
    $TextRange.Font.Color.RGB = $Color
}

function Add-TextBlock {
    param(
        [object]$Slide,
        [double]$Left,
        [double]$Top,
        [double]$Width,
        [double]$Height,
        [string]$Text,
        [double]$FontSize,
        [int]$Color,
        [bool]$Bold = $false,
        [int]$Align = $script:ppAlignLeft,
        [string]$FontName = "Microsoft YaHei"
    )

    $shape = $Slide.Shapes.AddTextbox($script:msoTextOrientationHorizontal, $Left, $Top, $Width, $Height)
    $shape.Line.Visible = $script:msoFalse
    $shape.Fill.Visible = $script:msoFalse
    $shape.TextFrame.MarginLeft = 0
    $shape.TextFrame.MarginRight = 0
    $shape.TextFrame.MarginTop = 0
    $shape.TextFrame.MarginBottom = 0
    $shape.TextFrame.WordWrap = $script:msoTrue
    $shape.TextFrame.TextRange.Text = $Text
    $shape.TextFrame.TextRange.ParagraphFormat.Alignment = $Align
    $shape.TextFrame.TextRange.ParagraphFormat.SpaceAfter = 8
    Set-TextRangeStyle -TextRange $shape.TextFrame.TextRange -FontName $FontName -FontSize $FontSize -Color $Color -Bold:$Bold
    return $shape
}

function Add-BulletList {
    param(
        [object]$Slide,
        [double]$Left,
        [double]$Top,
        [double]$Width,
        [double]$Height,
        [string[]]$Items,
        [double]$FontSize = 18,
        [int]$Color = $script:colors.Ink
    )

    $text = ($Items | ForEach-Object { "• $_" }) -join "`r`n"
    $shape = Add-TextBlock -Slide $Slide -Left $Left -Top $Top -Width $Width -Height $Height -Text $text -FontSize $FontSize -Color $Color
    $shape.TextFrame.TextRange.ParagraphFormat.SpaceAfter = 12
    return $shape
}

function Add-Card {
    param(
        [object]$Slide,
        [double]$Left,
        [double]$Top,
        [double]$Width,
        [double]$Height,
        [string]$Title,
        [string]$Body,
        [int]$FillColor,
        [int]$TitleColor = $script:colors.Navy,
        [int]$BodyColor = $script:colors.Ink
    )

    Add-Rectangle -Slide $Slide -Left $Left -Top $Top -Width $Width -Height $Height -FillColor $FillColor -LineColor $script:colors.Border -LineWeight 1 | Out-Null
    Add-TextBlock -Slide $Slide -Left ($Left + 18) -Top ($Top + 14) -Width ($Width - 36) -Height 28 -Text $Title -FontSize 18 -Color $TitleColor -Bold:$true | Out-Null
    Add-TextBlock -Slide $Slide -Left ($Left + 18) -Top ($Top + 50) -Width ($Width - 36) -Height ($Height - 62) -Text $Body -FontSize 12.5 -Color $BodyColor | Out-Null
}

function Add-MetricCard {
    param(
        [object]$Slide,
        [double]$Left,
        [double]$Top,
        [double]$Width,
        [double]$Height,
        [string]$Value,
        [string]$Label,
        [int]$FillColor
    )

    Add-Rectangle -Slide $Slide -Left $Left -Top $Top -Width $Width -Height $Height -FillColor $FillColor -LineColor $script:colors.Border -LineWeight 1 | Out-Null
    Add-TextBlock -Slide $Slide -Left ($Left + 18) -Top ($Top + 16) -Width ($Width - 36) -Height 34 -Text $Value -FontSize 26 -Color $script:colors.Navy -Bold:$true | Out-Null
    Add-TextBlock -Slide $Slide -Left ($Left + 18) -Top ($Top + 54) -Width ($Width - 36) -Height 26 -Text $Label -FontSize 12 -Color $script:colors.Muted | Out-Null
}

function Add-ImageFit {
    param(
        [object]$Slide,
        [string]$Path,
        [double]$Left,
        [double]$Top,
        [double]$Width,
        [double]$Height,
        [bool]$WithFrame = $true
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    if ($WithFrame) {
        Add-Rectangle -Slide $Slide -Left $Left -Top $Top -Width $Width -Height $Height -FillColor $script:colors.White -LineColor $script:colors.Border -LineWeight 1 -ShapeType $script:msoShapeRectangle | Out-Null
    }

    $picture = $Slide.Shapes.AddPicture($Path, $script:msoFalse, $script:msoTrue, $Left, $Top, -1, -1)
    $originalWidth = [double]$picture.Width
    $originalHeight = [double]$picture.Height
    $scale = [Math]::Min($Width / $originalWidth, $Height / $originalHeight)
    $picture.LockAspectRatio = $script:msoFalse
    $picture.Width = $originalWidth * $scale
    $picture.Height = $originalHeight * $scale
    $picture.Left = $Left + (($Width - $picture.Width) / 2)
    $picture.Top = $Top + (($Height - $picture.Height) / 2)
    return $picture
}

function Add-SectionHeader {
    param(
        [object]$Slide,
        [string]$Title,
        [int]$Index
    )

    Add-Rectangle -Slide $Slide -Left 0 -Top 0 -Width $script:slideWidth -Height $script:slideHeight -FillColor $script:colors.Light -LineColor $script:colors.Light -ShapeType $script:msoShapeRectangle | Out-Null
    Add-Rectangle -Slide $Slide -Left 0 -Top 0 -Width $script:slideWidth -Height 54 -FillColor $script:colors.Navy -LineColor $script:colors.Navy -ShapeType $script:msoShapeRectangle | Out-Null
    Add-Rectangle -Slide $Slide -Left 34 -Top 50 -Width 82 -Height 4 -FillColor $script:colors.Accent -LineColor $script:colors.Accent -ShapeType $script:msoShapeRectangle | Out-Null
    Add-TextBlock -Slide $Slide -Left 34 -Top 12 -Width 700 -Height 28 -Text $Title -FontSize 24 -Color $script:colors.White -Bold:$true | Out-Null
    Add-TextBlock -Slide $Slide -Left 870 -Top 14 -Width 60 -Height 24 -Text ([string]$Index) -FontSize 16 -Color $script:colors.White -Bold:$true -Align $script:ppAlignRight | Out-Null
}

function Add-Caption {
    param(
        [object]$Slide,
        [double]$Left,
        [double]$Top,
        [double]$Width,
        [string]$Text
    )

    Add-TextBlock -Slide $Slide -Left $Left -Top $Top -Width $Width -Height 18 -Text $Text -FontSize 10.5 -Color $script:colors.Muted -Align $script:ppAlignCenter | Out-Null
}

$powerPoint = $null
$presentation = $null

try {
    $powerPoint = New-Object -ComObject PowerPoint.Application
    $powerPoint.Visible = $script:msoTrue

    $presentation = $powerPoint.Presentations.Add()
    try {
        $presentation.PageSetup.SlideSize = 16
    } catch {
    }

    $script:slideWidth = [double]$presentation.PageSetup.SlideWidth
    $script:slideHeight = [double]$presentation.PageSetup.SlideHeight

    $coverImage = Join-Path $screenshotRoot "home-dashboard.png"
    $workspaceImage = Join-Path $systemImplRoot "figure-5-3-knowledge-base-workspace.png"
    $editorImage = Join-Path $systemImplRoot "figure-5-4-note-editor-and-links.png"
    $searchImage = Join-Path $screenshotRoot "search-panel.png"
    $graphImage = Join-Path $screenshotRoot "knowledge-graph.png"
    $templateImage = Join-Path $screenshotRoot "template-center.png"
    $settingsImage = Join-Path $screenshotRoot "user-settings.png"
    $testLinkImage = Join-Path $testCaseRoot "tc-6-5-4-bidirectional-link-created.png"
    $testGraphImage = Join-Path $testCaseRoot "tc-6-6-2-knowledge-graph.png"

    $slide = $presentation.Slides.Add(1, $script:ppLayoutBlank)
    Add-Rectangle -Slide $slide -Left 0 -Top 0 -Width $script:slideWidth -Height $script:slideHeight -FillColor $script:colors.Light -LineColor $script:colors.Light -ShapeType $script:msoShapeRectangle | Out-Null
    Add-Rectangle -Slide $slide -Left 0 -Top 0 -Width 360 -Height $script:slideHeight -FillColor $script:colors.Navy -LineColor $script:colors.Navy -ShapeType $script:msoShapeRectangle | Out-Null
    Add-Rectangle -Slide $slide -Left 34 -Top 46 -Width 104 -Height 24 -FillColor $script:colors.Accent -LineColor $script:colors.Accent -ShapeType $script:msoShapeRoundedRectangle | Out-Null
    Add-TextBlock -Slide $slide -Left 48 -Top 50 -Width 86 -Height 16 -Text "毕业设计答辩" -FontSize 10.5 -Color $script:colors.White -Bold:$true -Align $script:ppAlignCenter | Out-Null
    Add-TextBlock -Slide $slide -Left 36 -Top 108 -Width 284 -Height 150 -Text "基于 Spring Boot + Vue 的`r`n个人双链笔记系统" -FontSize 28 -Color $script:colors.White -Bold:$true | Out-Null
    Add-TextBlock -Slide $slide -Left 36 -Top 278 -Width 272 -Height 64 -Text "围绕个人知识管理场景，构建记录、关联、检索与可视化的一体化闭环。" -FontSize 14 -Color $script:colors.BlueSoft | Out-Null
    Add-Rectangle -Slide $slide -Left 36 -Top 360 -Width 288 -Height 2 -FillColor $script:colors.AccentSoft -LineColor $script:colors.AccentSoft -ShapeType $script:msoShapeRectangle | Out-Null
    Add-TextBlock -Slide $slide -Left 36 -Top 382 -Width 260 -Height 86 -Text "学生：XXX`r`n学号：XXXXXXXX`r`n指导教师：XXX`r`n答辩日期：2026 年 4 月" -FontSize 13 -Color $script:colors.White | Out-Null
    Add-TextBlock -Slide $slide -Left 384 -Top 58 -Width 532 -Height 30 -Text "项目首页运行截图" -FontSize 12 -Color $script:colors.Muted -Bold:$true | Out-Null
    Add-ImageFit -Slide $slide -Path $coverImage -Left 384 -Top 92 -Width 532 -Height 368 | Out-Null
    Add-TextBlock -Slide $slide -Left 384 -Top 476 -Width 532 -Height 20 -Text "关键词：双链笔记 / Markdown 编辑 / 检索导航 / 知识图谱" -FontSize 11 -Color $script:colors.Muted -Align $script:ppAlignCenter | Out-Null

    $slide = $presentation.Slides.Add(2, $script:ppLayoutBlank)
    Add-SectionHeader -Slide $slide -Title "选题背景与研究意义" -Index 2
    Add-BulletList -Slide $slide -Left 44 -Top 92 -Width 420 -Height 250 -Items @(
        "学习和项目过程中会产生大量碎片化笔记，后续整理成本高",
        "传统笔记系统更偏向目录存储，不擅长表达知识之间的关联",
        "当笔记规模扩大后，仅依靠标题和目录查找效率明显下降",
        "需要把记录、关联、检索与可视化能力整合到同一系统中"
    ) -FontSize 18 | Out-Null
    Add-Card -Slide $slide -Left 500 -Top 104 -Width 390 -Height 130 -Title "现有问题" -Body "内容积累快于知识整理速度，笔记之间缺乏上下文连接，复习和回顾时常常只能靠人工记忆跳转。" -FillColor $script:colors.White
    Add-Card -Slide $slide -Left 500 -Top 254 -Width 390 -Height 130 -Title "设计目标" -Body "围绕个人知识管理场景，构建以双链为核心的知识组织方式，提升知识的可定位性、可理解性和可复用性。" -FillColor $script:colors.AccentSoft
    Add-Card -Slide $slide -Left 500 -Top 404 -Width 390 -Height 82 -Title "项目定位" -Body "不是单纯做记录工具，而是面向知识网络构建的个人笔记系统。" -FillColor $script:colors.BlueSoft

    $slide = $presentation.Slides.Add(3, $script:ppLayoutBlank)
    Add-SectionHeader -Slide $slide -Title "系统目标与功能设计" -Index 3
    Add-Card -Slide $slide -Left 40 -Top 96 -Width 270 -Height 120 -Title "用户与基础设置" -Body "注册、登录、资料维护、密码修改、头像上传、主题配置。" -FillColor $script:colors.White
    Add-Card -Slide $slide -Left 340 -Top 96 -Width 270 -Height 120 -Title "知识库与目录管理" -Body "知识库创建、目录树组织、文件夹管理、知识内容分层归档。" -FillColor $script:colors.White
    Add-Card -Slide $slide -Left 640 -Top 96 -Width 270 -Height 120 -Title "Markdown 编辑" -Body "基于 Vditor 的编辑器能力，支持正文编辑、预览和导出。" -FillColor $script:colors.White
    Add-Card -Slide $slide -Left 40 -Top 248 -Width 270 -Height 120 -Title "双链与历史版本" -Body "支持 [[笔记标题]] 双链引用、入链出链展示和版本快照管理。" -FillColor $script:colors.AccentSoft
    Add-Card -Slide $slide -Left 340 -Top 248 -Width 270 -Height 120 -Title "检索与模板" -Body "支持标题、正文、标签检索，以及模板创建、读取和复用。" -FillColor $script:colors.AccentSoft
    Add-Card -Slide $slide -Left 640 -Top 248 -Width 270 -Height 120 -Title "知识图谱可视化" -Body "基于双链关系生成知识图谱，支持从整体上理解知识结构。" -FillColor $script:colors.AccentSoft
    Add-TextBlock -Slide $slide -Left 46 -Top 406 -Width 856 -Height 54 -Text "系统围绕知识录入、知识组织、知识关联、知识定位和知识可视化形成完整闭环。" -FontSize 20 -Color $script:colors.Navy -Bold:$true -Align $script:ppAlignCenter | Out-Null

    $slide = $presentation.Slides.Add(4, $script:ppLayoutBlank)
    Add-SectionHeader -Slide $slide -Title "系统架构与技术选型" -Index 4
    Add-Card -Slide $slide -Left 52 -Top 100 -Width 320 -Height 88 -Title "前端表示层" -Body "Vue 3 + TypeScript + Vite + Vditor + D3" -FillColor $script:colors.White
    Add-TextBlock -Slide $slide -Left 190 -Top 188 -Width 48 -Height 30 -Text "↓" -FontSize 26 -Color $script:colors.Accent -Bold:$true -Align $script:ppAlignCenter | Out-Null
    Add-Card -Slide $slide -Left 52 -Top 216 -Width 320 -Height 88 -Title "后端业务层" -Body "Spring Boot 3.5.11 + Spring Security + JWT + MyBatis-Plus" -FillColor $script:colors.White
    Add-TextBlock -Slide $slide -Left 190 -Top 304 -Width 48 -Height 30 -Text "↓" -FontSize 26 -Color $script:colors.Accent -Bold:$true -Align $script:ppAlignCenter | Out-Null
    Add-Card -Slide $slide -Left 52 -Top 332 -Width 320 -Height 88 -Title "数据持久层" -Body "MySQL 8.0.35，围绕用户、知识库、笔记、双链和标签建模" -FillColor $script:colors.White
    Add-Card -Slide $slide -Left 430 -Top 96 -Width 454 -Height 136 -Title "运行环境" -Body "Java 17`r`nMaven 3.9.12`r`nNode.js 22.14.0`r`nnpm 10.9.2" -FillColor $script:colors.BlueSoft
    Add-Card -Slide $slide -Left 430 -Top 252 -Width 454 -Height 136 -Title "架构特点" -Body "采用前后端分离模式，前端负责交互与展示，后端负责认证、业务逻辑、数据处理与接口输出。" -FillColor $script:colors.AccentSoft
    Add-Card -Slide $slide -Left 430 -Top 408 -Width 454 -Height 76 -Title "适配场景" -Body "适合个人学习笔记、项目知识沉淀和长期知识积累。" -FillColor $script:colors.White

    $slide = $presentation.Slides.Add(5, $script:ppLayoutBlank)
    Add-SectionHeader -Slide $slide -Title "数据库设计与核心数据模型" -Index 5
    Add-BulletList -Slide $slide -Left 46 -Top 100 -Width 414 -Height 236 -Items @(
        "笔记标题与正文分表存储，便于自动保存和后续扩展",
        "双链关系独立建表，方便做入链、出链和图谱统计",
        "知识库、文件夹和笔记均预留软删除字段",
        "标签、模板和历史版本表共同支撑内容复用与追踪"
    ) -FontSize 17 | Out-Null
    Add-Card -Slide $slide -Left 494 -Top 96 -Width 396 -Height 170 -Title "核心业务表" -Body "t_user / t_user_settings`r`nt_knowledge_base / t_folder`r`nt_note / t_note_content`r`nt_note_link / t_note_history`r`nt_note_template / t_tag / t_note_tag" -FillColor $script:colors.White
    Add-Card -Slide $slide -Left 494 -Top 288 -Width 396 -Height 72 -Title "设计思路" -Body "围绕用户、知识库、文件夹、笔记和双链关系进行建模。" -FillColor $script:colors.AccentSoft
    Add-MetricCard -Slide $slide -Left 48 -Top 372 -Width 192 -Height 92 -Value "19" -Label "2026-04-15 运行快照中的笔记数" -FillColor $script:colors.White
    Add-MetricCard -Slide $slide -Left 260 -Top 372 -Width 192 -Height 92 -Value "16" -Label "历史版本记录数" -FillColor $script:colors.White
    Add-MetricCard -Slide $slide -Left 494 -Top 372 -Width 192 -Height 92 -Value "10" -Label "双链关系记录数" -FillColor $script:colors.White
    Add-MetricCard -Slide $slide -Left 706 -Top 372 -Width 184 -Height 92 -Value "6" -Label "标签记录数" -FillColor $script:colors.White

    $slide = $presentation.Slides.Add(6, $script:ppLayoutBlank)
    Add-SectionHeader -Slide $slide -Title "关键实现：Markdown 编辑、自动保存与双链解析" -Index 6
    Add-BulletList -Slide $slide -Left 42 -Top 96 -Width 352 -Height 260 -Items @(
        "前端基于 Vditor 提供 Markdown 编辑、预览和导出能力",
        "标题自动保存与正文自动保存接口分离，降低冗余写入",
        "正文保存时解析 [[笔记标题]] 并刷新当前笔记的出链关系",
        "目标笔记不存在时标记失效链，后续可在补全目标后自动恢复",
        "笔记详情返回入链、出链、候选项和预览内容"
    ) -FontSize 16.5 | Out-Null
    Add-Card -Slide $slide -Left 42 -Top 382 -Width 352 -Height 92 -Title "实现流程" -Body "编辑正文 -> 自动保存接口 -> 更新正文表 -> 解析双链 -> 维护 note_link -> 返回详情与统计信息" -FillColor $script:colors.AccentSoft
    Add-ImageFit -Slide $slide -Path $editorImage -Left 424 -Top 96 -Width 476 -Height 344 | Out-Null
    Add-Caption -Slide $slide -Left 426 -Top 448 -Width 472 -Text "笔记编辑界面：正文编辑、链接面板与目录导航联动展示"

    $slide = $presentation.Slides.Add(7, $script:ppLayoutBlank)
    Add-SectionHeader -Slide $slide -Title "系统运行效果展示" -Index 7
    Add-ImageFit -Slide $slide -Path $workspaceImage -Left 42 -Top 96 -Width 418 -Height 166 | Out-Null
    Add-Caption -Slide $slide -Left 42 -Top 266 -Width 418 -Text "知识库工作台"
    Add-ImageFit -Slide $slide -Path $searchImage -Left 498 -Top 96 -Width 418 -Height 166 | Out-Null
    Add-Caption -Slide $slide -Left 498 -Top 266 -Width 418 -Text "搜索面板"
    Add-ImageFit -Slide $slide -Path $graphImage -Left 42 -Top 302 -Width 418 -Height 166 | Out-Null
    Add-Caption -Slide $slide -Left 42 -Top 472 -Width 418 -Text "知识图谱"
    Add-ImageFit -Slide $slide -Path $templateImage -Left 498 -Top 302 -Width 418 -Height 166 | Out-Null
    Add-Caption -Slide $slide -Left 498 -Top 472 -Width 418 -Text "模板中心"

    $slide = $presentation.Slides.Add(8, $script:ppLayoutBlank)
    Add-SectionHeader -Slide $slide -Title "系统测试与结果分析" -Index 8
    Add-MetricCard -Slide $slide -Left 42 -Top 94 -Width 180 -Height 92 -Value "26" -Label "核心测试用例截图" -FillColor $script:colors.White
    Add-MetricCard -Slide $slide -Left 240 -Top 94 -Width 180 -Height 92 -Value "9+" -Label "覆盖的核心模块" -FillColor $script:colors.White
    Add-MetricCard -Slide $slide -Left 438 -Top 94 -Width 180 -Height 92 -Value "100%" -Label "当前文档中的通过率" -FillColor $script:colors.White
    Add-MetricCard -Slide $slide -Left 636 -Top 94 -Width 260 -Height 92 -Value "前后端联调" -Label "页面验证 + 接口验证" -FillColor $script:colors.AccentSoft
    Add-BulletList -Slide $slide -Left 42 -Top 212 -Width 394 -Height 232 -Items @(
        "登录、注册与重复账号校验结果均符合预期",
        "知识库、文件夹、笔记创建和自动保存流程正常",
        "双链建立、历史版本创建与查询功能正常",
        "检索、图谱、模板和密码修改等功能均验证通过"
    ) -FontSize 16 | Out-Null
    Add-ImageFit -Slide $slide -Path $testLinkImage -Left 472 -Top 212 -Width 198 -Height 210 | Out-Null
    Add-ImageFit -Slide $slide -Path $testGraphImage -Left 692 -Top 212 -Width 198 -Height 210 | Out-Null
    Add-Caption -Slide $slide -Left 472 -Top 426 -Width 198 -Text "双链建立测试截图"
    Add-Caption -Slide $slide -Left 692 -Top 426 -Width 198 -Text "图谱测试截图"

    $slide = $presentation.Slides.Add(9, $script:ppLayoutBlank)
    Add-SectionHeader -Slide $slide -Title "项目特色与亮点" -Index 9
    Add-Card -Slide $slide -Left 46 -Top 104 -Width 402 -Height 122 -Title "双链驱动的知识组织方式" -Body "通过 [[...]] 语法建立笔记之间的引用关系，让知识不再是孤立页面，而是可联通、可追踪的网络结构。" -FillColor $script:colors.White
    Add-Card -Slide $slide -Left 488 -Top 104 -Width 402 -Height 122 -Title "检索与图谱联动" -Body "既支持关键词、标题、正文和标签检索，也支持从整体图谱理解知识结构，兼顾局部定位与全局认知。" -FillColor $script:colors.White
    Add-Card -Slide $slide -Left 46 -Top 262 -Width 402 -Height 122 -Title "自动保存与历史版本" -Body "降低内容丢失风险，并能够通过历史快照实现版本追踪和恢复，增强日常使用可靠性。" -FillColor $script:colors.AccentSoft
    Add-Card -Slide $slide -Left 488 -Top 262 -Width 402 -Height 122 -Title "工程实用性较强" -Body "已实现模板复用、主题切换、PDF/HTML/Markdown 导出等功能，兼顾演示效果与实际可用性。" -FillColor $script:colors.AccentSoft
    Add-TextBlock -Slide $slide -Left 52 -Top 424 -Width 840 -Height 44 -Text "项目不是只做存储，而是更关注知识之间的连接、复用与理解。" -FontSize 22 -Color $script:colors.Navy -Bold:$true -Align $script:ppAlignCenter | Out-Null

    $slide = $presentation.Slides.Add(10, $script:ppLayoutBlank)
    Add-SectionHeader -Slide $slide -Title "不足与后续改进方向" -Index 10
    Add-Card -Slide $slide -Left 42 -Top 96 -Width 404 -Height 280 -Title "当前不足" -Body "1. 回收站闭环尚未完成，目前主要依赖软删除字段。`r`n2. 知识图谱以全局图为主，局部关系分析仍可细化。`r`n3. 备份与跨端同步尚未实现。`r`n4. 自动化单元测试仍需继续补强。" -FillColor $script:colors.White
    Add-Card -Slide $slide -Left 492 -Top 96 -Width 404 -Height 280 -Title "后续优化" -Body "1. 完成回收站、恢复与彻底删除闭环。`r`n2. 增加局部关系图、知识路径探索和版本差异对比。`r`n3. 支持备份同步和更多个性化编辑器配置。`r`n4. 增强单元测试和回归验证能力。" -FillColor $script:colors.AccentSoft
    Add-TextBlock -Slide $slide -Left 66 -Top 408 -Width 810 -Height 44 -Text "当前版本已具备核心业务能力，后续工作重点放在工程完善和知识分析能力增强上。" -FontSize 20 -Color $script:colors.Navy -Bold:$true -Align $script:ppAlignCenter | Out-Null

    $slide = $presentation.Slides.Add(11, $script:ppLayoutBlank)
    Add-Rectangle -Slide $slide -Left 0 -Top 0 -Width $script:slideWidth -Height $script:slideHeight -FillColor $script:colors.Navy -LineColor $script:colors.Navy -ShapeType $script:msoShapeRectangle | Out-Null
    Add-ImageFit -Slide $slide -Path $settingsImage -Left 526 -Top 84 -Width 360 -Height 268 | Out-Null
    Add-TextBlock -Slide $slide -Left 56 -Top 104 -Width 370 -Height 52 -Text "汇报总结" -FontSize 30 -Color $script:colors.White -Bold:$true | Out-Null
    Add-TextBlock -Slide $slide -Left 56 -Top 174 -Width 380 -Height 140 -Text "本项目完成了个人双链笔记系统的设计与实现，已形成知识录入、组织、关联、检索和可视化的完整闭环，并通过核心功能测试验证了可用性。" -FontSize 17 -Color $script:colors.BlueSoft | Out-Null
    Add-Rectangle -Slide $slide -Left 56 -Top 334 -Width 164 -Height 3 -FillColor $script:colors.Accent -LineColor $script:colors.Accent -ShapeType $script:msoShapeRectangle | Out-Null
    Add-TextBlock -Slide $slide -Left 56 -Top 364 -Width 384 -Height 70 -Text "谢谢各位老师`r`n恳请批评指正" -FontSize 26 -Color $script:colors.White -Bold:$true | Out-Null
    Add-TextBlock -Slide $slide -Left 528 -Top 370 -Width 356 -Height 34 -Text "Q&A" -FontSize 26 -Color $script:colors.White -Bold:$true -Align $script:ppAlignCenter | Out-Null
    Add-TextBlock -Slide $slide -Left 528 -Top 410 -Width 356 -Height 20 -Text "基于 Spring Boot + Vue 的个人双链笔记系统" -FontSize 12 -Color $script:colors.BlueSoft -Align $script:ppAlignCenter | Out-Null

    $outputDir = Split-Path -Parent $OutputPath
    if (-not (Test-Path -LiteralPath $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir | Out-Null
    }

    if (Test-Path -LiteralPath $OutputPath) {
        Remove-Item -LiteralPath $OutputPath -Force
    }

    $presentation.SaveAs($OutputPath, $script:ppSaveAsOpenXMLPresentation)
    Write-Output "PPT generated: $OutputPath"
}
finally {
    if ($presentation) {
        $presentation.Close()
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($presentation)
    }
    if ($powerPoint) {
        $powerPoint.Quit()
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($powerPoint)
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}

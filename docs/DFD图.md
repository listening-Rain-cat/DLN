## 顶层

```dot
digraph "顶层数据流图" {
  e1 [shape=box, label="E1用户"];
  p0 [shape=circle, label="P0\n个人双链笔记系统"];

  e1 -> p0 [label="身份与个人设置请求"];
  e1 -> p0 [label="知识空间管理请求"];
  e1 -> p0 [label="笔记与历史版本请求"];
  e1 -> p0 [label="双链交互请求"];
  e1 -> p0 [label="标签与模板请求"];
  e1 -> p0 [label="附件与导出请求"];
  e1 -> p0 [label="检索与知识图谱请求"];

  p0 -> e1 [label="身份认证结果与个人设置信息"];
  p0 -> e1 [label="知识空间数据与操作结果"];
  p0 -> e1 [label="笔记详情与历史版本结果"];
  p0 -> e1 [label="双链关联结果"];
  p0 -> e1 [label="标签模板数据与操作结果"];
  p0 -> e1 [label="附件下载内容与导出结果"];
  p0 -> e1 [label="检索结果与知识图谱数据"];
}
```

## 零层

### 总零层图

```dot
digraph "零层数据流图" {
  e1 [shape=box, label="E1用户"];

  p1 [shape=circle, label="P1.0\n账户与设置管理"];
  p2 [shape=circle, label="P2.0\n知识空间管理"];
  p3 [shape=circle, label="P3.0\n笔记与历史版本管理"];
  p4 [shape=circle, label="P4.0\n双链关系管理"];
  p5 [shape=circle, label="P5.0\n标签与模板管理"];
  p6 [shape=circle, label="P6.0\n附件与导出管理"];
  p7 [shape=circle, label="P7.0\n检索与知识图谱管理"];

  d1 [shape=cylinder, label="D1用户表"];
  d2 [shape=cylinder, label="D2用户设置表"];
  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d7 [shape=cylinder, label="D7笔记历史表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];
  d11 [shape=cylinder, label="D11笔记模板表"];
  d12 [shape=cylinder, label="D12笔记附件表"];

  e1 -> p1 [label="身份与个人设置请求"];
  p1 -> e1 [label="身份认证结果与个人设置信息"];

  e1 -> p2 [label="知识空间管理请求"];
  p2 -> e1 [label="知识空间数据与操作结果"];

  e1 -> p3 [label="笔记与历史版本请求"];
  p3 -> e1 [label="笔记详情与历史版本结果"];

  e1 -> p4 [label="双链交互请求"];
  p4 -> e1 [label="双链关联结果"];

  e1 -> p5 [label="标签与模板请求"];
  p5 -> e1 [label="标签模板数据与操作结果"];

  e1 -> p6 [label="附件与导出请求"];
  p6 -> e1 [label="附件下载内容与导出结果"];

  e1 -> p7 [label="检索与知识图谱请求"];
  p7 -> e1 [label="检索结果与知识图谱数据"];

  p1 -> d1 [label="注册信息、登录校验信息、个人资料更新信息、头像地址更新信息"];
  d1 -> p1 [label="用户资料、认证数据"];
  p1 -> d2 [label="用户设置更新信息"];
  d2 -> p1 [label="个性化设置数据"];

  p2 -> d3 [label="知识库新增修改删除信息"];
  d3 -> p2 [label="知识库列表与知识库校验数据"];
  p2 -> d4 [label="文件夹新增修改删除信息"];
  d4 -> p2 [label="文件夹数据与目录结构数据"];
  d5 -> p2 [label="目录树中的笔记基础数据"];
  d9 -> p2 [label="目录树中的标签数据"];
  d10 -> p2 [label="目录树中的笔记标签关联数据"];
  p2 -> d8 [label="知识空间删除触发的双链清理信息"];
  p2 -> d9 [label="知识空间删除触发的标签删除信息"];
  p2 -> d10 [label="知识空间删除触发的笔记标签清理信息"];

  d3 -> p3 [label="知识库校验数据"];
  d4 -> p3 [label="目录定位与文件夹校验数据"];
  d5 -> p3 [label="笔记基础数据"];
  p3 -> d5 [label="笔记新增修改移动删除信息"];
  d6 -> p3 [label="笔记正文数据"];
  p3 -> d6 [label="笔记正文新增修改恢复信息"];
  d7 -> p3 [label="历史版本数据"];
  p3 -> d7 [label="历史快照新增删除信息"];
  d8 -> p3 [label="入链出链数据"];
  p3 -> d8 [label="双链刷新与删除信息"];
  d9 -> p3 [label="标签数据"];
  d10 -> p3 [label="笔记标签关联数据"];
  p3 -> d10 [label="笔记删除触发的标签关联清理信息"];
  d11 -> p3 [label="模板内容数据"];
  d12 -> p3 [label="笔记附件数据"];

  d5 -> p4 [label="候选笔记与目标笔记定位数据"];
  d6 -> p4 [label="目标笔记预览内容"];
  d8 -> p4 [label="双链关系与入链出链数据"];
  p4 -> d8 [label="双链生成更新与失效维护信息"];

  d3 -> p5 [label="知识库校验数据"];
  d5 -> p5 [label="笔记校验数据"];
  d9 -> p5 [label="标签数据"];
  p5 -> d9 [label="标签新增删除信息"];
  d10 -> p5 [label="笔记标签关联数据"];
  p5 -> d10 [label="笔记标签设置与清理信息"];
  d11 -> p5 [label="模板列表与模板内容"];
  p5 -> d11 [label="模板新增修改删除信息"];

  d2 -> p6 [label="导出主题设置数据"];
  d5 -> p6 [label="笔记定位与笔记标题数据"];
  d6 -> p6 [label="导出正文数据"];
  d12 -> p6 [label="附件元数据与附件列表数据"];
  p6 -> d12 [label="附件上传与删除信息"];

  d3 -> p7 [label="知识库校验数据"];
  d4 -> p7 [label="目录路径与文件夹筛选数据"];
  d5 -> p7 [label="检索定位与图谱节点数据"];
  d6 -> p7 [label="正文检索数据"];
  d8 -> p7 [label="链路统计与图谱边数据"];
  d9 -> p7 [label="标签筛选数据"];
  d10 -> p7 [label="笔记标签关联筛选数据"];
}
```

### 分图

**零层子图 1：账户与设置管理**

```dot
digraph "零层子图1_账户与设置管理" {
  e1 [shape=box, label="E1用户"];
  p1 [shape=circle, label="P1.0\n账户与设置管理"];
  d1 [shape=cylinder, label="D1用户表"];
  d2 [shape=cylinder, label="D2用户设置表"];

  e1 -> p1 [label="身份与个人设置请求"];
  p1 -> e1 [label="身份认证结果与个人设置信息"];

  p1 -> d1 [label="注册信息、登录校验信息、个人资料更新信息、头像地址更新信息"];
  d1 -> p1 [label="用户资料、认证数据"];

  p1 -> d2 [label="用户设置更新信息"];
  d2 -> p1 [label="个性化设置数据"];
}
```

**零层子图 2：知识空间管理**

```dot
digraph "零层子图2_知识空间管理" {
  e1 [shape=box, label="E1用户"];
  p2 [shape=circle, label="P2.0\n知识空间管理"];

  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];

  e1 -> p2 [label="知识空间管理请求"];
  p2 -> e1 [label="知识空间数据与操作结果"];

  p2 -> d3 [label="知识库新增修改删除信息"];
  d3 -> p2 [label="知识库列表与知识库校验数据"];

  p2 -> d4 [label="文件夹新增修改删除信息"];
  d4 -> p2 [label="文件夹数据与目录结构数据"];

  d5 -> p2 [label="目录树中的笔记基础数据"];
  d9 -> p2 [label="目录树中的标签数据"];
  d10 -> p2 [label="目录树中的笔记标签关联数据"];

  p2 -> d8 [label="知识空间删除触发的双链清理信息"];
  p2 -> d9 [label="知识空间删除触发的标签删除信息"];
  p2 -> d10 [label="知识空间删除触发的笔记标签清理信息"];
}
```

**零层子图 3：笔记与历史版本管理**

```dot
digraph "零层子图3_笔记与历史版本管理" {
  e1 [shape=box, label="E1用户"];
  p3 [shape=circle, label="P3.0\n笔记与历史版本管理"];

  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d7 [shape=cylinder, label="D7笔记历史表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];
  d11 [shape=cylinder, label="D11笔记模板表"];
  d12 [shape=cylinder, label="D12笔记附件表"];

  e1 -> p3 [label="笔记与历史版本请求"];
  p3 -> e1 [label="笔记详情与历史版本结果"];

  d3 -> p3 [label="知识库校验数据"];
  d4 -> p3 [label="目录定位与文件夹校验数据"];

  d5 -> p3 [label="笔记基础数据"];
  p3 -> d5 [label="笔记新增修改移动删除信息"];

  d6 -> p3 [label="笔记正文数据"];
  p3 -> d6 [label="笔记正文新增修改恢复信息"];

  d7 -> p3 [label="历史版本数据"];
  p3 -> d7 [label="历史快照新增删除信息"];

  d8 -> p3 [label="入链出链数据"];
  p3 -> d8 [label="双链刷新与删除信息"];

  d9 -> p3 [label="标签数据"];
  d10 -> p3 [label="笔记标签关联数据"];
  p3 -> d10 [label="笔记删除触发的标签关联清理信息"];

  d11 -> p3 [label="模板内容数据"];
  d12 -> p3 [label="笔记附件数据"];
}
```

**零层子图 4：双链关系、检索与知识图谱管理**

```dot
digraph "零层子图4_双链关系_检索与知识图谱管理" {
  e1 [shape=box, label="E1用户"];
  p4 [shape=circle, label="P4.0\n双链关系管理"];
  p7 [shape=circle, label="P7.0\n检索与知识图谱管理"];

  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];

  e1 -> p4 [label="双链交互请求"];
  p4 -> e1 [label="双链关联结果"];

  d5 -> p4 [label="候选笔记与目标笔记定位数据"];
  d6 -> p4 [label="目标笔记预览内容"];
  d8 -> p4 [label="双链关系与入链出链数据"];
  p4 -> d8 [label="双链生成更新与失效维护信息"];

  e1 -> p7 [label="检索与知识图谱请求"];
  p7 -> e1 [label="检索结果与知识图谱数据"];

  d3 -> p7 [label="知识库校验数据"];
  d4 -> p7 [label="目录路径与文件夹筛选数据"];
  d5 -> p7 [label="检索定位与图谱节点数据"];
  d6 -> p7 [label="正文检索数据"];
  d8 -> p7 [label="链路统计与图谱边数据"];
  d9 -> p7 [label="标签筛选数据"];
  d10 -> p7 [label="笔记标签关联筛选数据"];
}
```

**零层子图 5：标签模板、附件与导出管理**

```dot
digraph "零层子图5_标签模板_附件与导出管理" {
  e1 [shape=box, label="E1用户"];
  p5 [shape=circle, label="P5.0\n标签与模板管理"];
  p6 [shape=circle, label="P6.0\n附件与导出管理"];

  d2 [shape=cylinder, label="D2用户设置表"];
  d3 [shape=cylinder, label="D3知识库表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];
  d11 [shape=cylinder, label="D11笔记模板表"];
  d12 [shape=cylinder, label="D12笔记附件表"];

  e1 -> p5 [label="标签与模板请求"];
  p5 -> e1 [label="标签模板数据与操作结果"];

  d3 -> p5 [label="知识库校验数据"];
  d5 -> p5 [label="笔记校验数据"];
  d9 -> p5 [label="标签数据"];
  p5 -> d9 [label="标签新增删除信息"];
  d10 -> p5 [label="笔记标签关联数据"];
  p5 -> d10 [label="笔记标签设置与清理信息"];
  d11 -> p5 [label="模板列表与模板内容"];
  p5 -> d11 [label="模板新增修改删除信息"];

  e1 -> p6 [label="附件与导出请求"];
  p6 -> e1 [label="附件下载内容与导出结果"];

  d2 -> p6 [label="导出主题设置数据"];
  d5 -> p6 [label="笔记定位与笔记标题数据"];
  d6 -> p6 [label="导出正文数据"];
  d12 -> p6 [label="附件元数据与附件列表数据"];
  p6 -> d12 [label="附件上传与删除信息"];
}
```

## 一层

### **一层数据流图：P1.0账户与设置管理**

```dot
digraph "一层数据流图_P1_0" {
  e1 [shape=box, label="E1用户"];

  p11 [shape=circle, label="P1.1\n用户注册"];
  p12 [shape=circle, label="P1.2\n用户登录"];
  p13 [shape=circle, label="P1.3\n查询个人信息"];
  p14 [shape=circle, label="P1.4\n维护个人信息"];
  p15 [shape=circle, label="P1.5\n上传头像"];
  p16 [shape=circle, label="P1.6\n查询个性化设置"];
  p17 [shape=circle, label="P1.7\n修改个性化设置"];
  p18 [shape=circle, label="P1.8\n获取主题选项"];

  d1 [shape=cylinder, label="D1用户表"];
  d2 [shape=cylinder, label="D2用户设置表"];

  e1 -> p11 [label="注册信息"];
  d1 -> p11 [label="用户名与邮箱校验数据"];
  p11 -> d1 [label="新用户数据"];
  p11 -> d2 [label="默认主题设置数据"];
  p11 -> e1 [label="注册结果与用户信息"];

  e1 -> p12 [label="登录凭证"];
  d1 -> p12 [label="认证数据"];
  p12 -> e1 [label="登录结果与身份令牌"];

  e1 -> p13 [label="个人信息查询请求"];
  d1 -> p13 [label="用户资料"];
  d2 -> p13 [label="用户设置数据"];
  p13 -> e1 [label="个人信息与设置信息"];

  e1 -> p14 [label="个人资料修改信息"];
  d1 -> p14 [label="当前用户资料"];
  p14 -> d1 [label="用户资料更新信息"];
  p14 -> e1 [label="个人资料更新结果"];

  e1 -> p15 [label="头像上传请求"];
  d1 -> p15 [label="当前用户资料"];
  p15 -> d1 [label="头像地址更新信息"];
  p15 -> e1 [label="头像上传结果"];

  e1 -> p16 [label="个性化设置查询请求"];
  d2 -> p16 [label="用户设置数据"];
  p16 -> e1 [label="个性化设置信息"];

  e1 -> p17 [label="个性化设置修改信息"];
  d2 -> p17 [label="当前设置数据"];
  p17 -> d2 [label="用户设置更新信息"];
  p17 -> e1 [label="设置修改结果"];

  e1 -> p18 [label="主题选项查询请求"];
  p18 -> e1 [label="主题选项数据"];
}
```

### **一层数据流图：P2.0知识空间管理**

```dot
digraph "一层数据流图_P2_0" {
  e1 [shape=box, label="E1用户"];

  p21 [shape=circle, label="P2.1\n创建知识库"];
  p22 [shape=circle, label="P2.2\n查询知识库列表"];
  p23 [shape=circle, label="P2.3\n修改知识库"];
  p24 [shape=circle, label="P2.4\n删除知识库"];
  p25 [shape=circle, label="P2.5\n创建文件夹"];
  p26 [shape=circle, label="P2.6\n修改文件夹"];
  p27 [shape=circle, label="P2.7\n删除文件夹"];
  p28 [shape=circle, label="P2.8\n生成目录树"];

  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];

  e1 -> p21 [label="知识库创建信息"];
  d3 -> p21 [label="知识库名称校验数据"];
  p21 -> d3 [label="知识库新增数据"];
  p21 -> e1 [label="知识库创建结果"];

  e1 -> p22 [label="知识库列表查询请求"];
  d3 -> p22 [label="知识库列表数据"];
  p22 -> e1 [label="知识库列表"];

  e1 -> p23 [label="知识库修改信息"];
  d3 -> p23 [label="当前知识库数据与名称校验数据"];
  p23 -> d3 [label="知识库更新数据"];
  p23 -> e1 [label="知识库修改结果"];

  e1 -> p24 [label="知识库删除请求"];
  d3 -> p24 [label="待删除知识库数据"];
  d4 -> p24 [label="待删除文件夹数据"];
  d5 -> p24 [label="待删除笔记数据"];
  p24 -> d3 [label="知识库删除标记信息"];
  p24 -> d4 [label="文件夹删除标记信息"];
  p24 -> d5 [label="笔记删除标记信息"];
  p24 -> d8 [label="知识库删除触发的双链清理信息"];
  p24 -> d9 [label="知识库删除触发的标签删除信息"];
  p24 -> d10 [label="知识库删除触发的笔记标签清理信息"];
  p24 -> e1 [label="知识库删除结果"];

  e1 -> p25 [label="文件夹创建信息"];
  d3 -> p25 [label="知识库校验数据"];
  d4 -> p25 [label="父目录与同级名称校验数据"];
  p25 -> d4 [label="文件夹新增数据"];
  p25 -> e1 [label="文件夹创建结果"];

  e1 -> p26 [label="文件夹修改信息"];
  d3 -> p26 [label="知识库校验数据"];
  d4 -> p26 [label="当前文件夹数据与父目录校验数据"];
  p26 -> d4 [label="文件夹更新数据"];
  p26 -> e1 [label="文件夹修改结果"];

  e1 -> p27 [label="文件夹删除请求"];
  d4 -> p27 [label="待删除文件夹数据"];
  d5 -> p27 [label="目录下笔记数据"];
  p27 -> d4 [label="文件夹删除标记信息"];
  p27 -> d5 [label="目录下笔记删除标记信息"];
  p27 -> d8 [label="目录删除触发的双链清理信息"];
  p27 -> d10 [label="目录删除触发的笔记标签清理信息"];
  p27 -> e1 [label="文件夹删除结果"];

  e1 -> p28 [label="目录树查询请求"];
  d3 -> p28 [label="知识库校验数据"];
  d4 -> p28 [label="文件夹层级数据"];
  d5 -> p28 [label="笔记基础数据"];
  d9 -> p28 [label="标签数据"];
  d10 -> p28 [label="笔记标签关联数据"];
  p28 -> e1 [label="目录树数据"];
}
```

#### 分图版

分图 1：知识库管理

```
digraph "分图1_知识库管理" {
  e1 [shape=box, label="E1用户"];

  p21 [shape=circle, label="P2.1\n创建知识库"];
  p22 [shape=circle, label="P2.2\n查询知识库列表"];
  p23 [shape=circle, label="P2.3\n修改知识库"];
  p24 [shape=circle, label="P2.4\n删除知识库"];

  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];

  e1 -> p21 [label="知识库创建信息"];
  d3 -> p21 [label="知识库名称校验数据"];
  p21 -> d3 [label="知识库新增数据"];
  p21 -> e1 [label="知识库创建结果"];

  e1 -> p22 [label="知识库列表查询请求"];
  d3 -> p22 [label="知识库列表数据"];
  p22 -> e1 [label="知识库列表"];

  e1 -> p23 [label="知识库修改信息"];
  d3 -> p23 [label="当前知识库数据与名称校验数据"];
  p23 -> d3 [label="知识库更新数据"];
  p23 -> e1 [label="知识库修改结果"];

  e1 -> p24 [label="知识库删除请求"];
  d3 -> p24 [label="待删除知识库数据"];
  d4 -> p24 [label="待删除文件夹数据"];
  d5 -> p24 [label="待删除笔记数据"];
  p24 -> d3 [label="知识库删除标记信息"];
  p24 -> d4 [label="文件夹删除标记信息"];
  p24 -> d5 [label="笔记删除标记信息"];
  p24 -> d8 [label="知识库删除触发的双链清理信息"];
  p24 -> d9 [label="知识库删除触发的标签删除信息"];
  p24 -> d10 [label="知识库删除触发的笔记标签清理信息"];
  p24 -> e1 [label="知识库删除结果"];
}

```

分图 2：文件夹管理

```
digraph "分图2_文件夹管理" {
  e1 [shape=box, label="E1用户"];

  p25 [shape=circle, label="P2.5\n创建文件夹"];
  p26 [shape=circle, label="P2.6\n修改文件夹"];
  p27 [shape=circle, label="P2.7\n删除文件夹"];

  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];

  e1 -> p25 [label="文件夹创建信息"];
  d3 -> p25 [label="知识库校验数据"];
  d4 -> p25 [label="父目录与同级名称校验数据"];
  p25 -> d4 [label="文件夹新增数据"];
  p25 -> e1 [label="文件夹创建结果"];

  e1 -> p26 [label="文件夹修改信息"];
  d3 -> p26 [label="知识库校验数据"];
  d4 -> p26 [label="当前文件夹数据与父目录校验数据"];
  p26 -> d4 [label="文件夹更新数据"];
  p26 -> e1 [label="文件夹修改结果"];

  e1 -> p27 [label="文件夹删除请求"];
  d4 -> p27 [label="待删除文件夹数据"];
  d5 -> p27 [label="目录下笔记数据"];
  p27 -> d4 [label="文件夹删除标记信息"];
  p27 -> d5 [label="目录下笔记删除标记信息"];
  p27 -> d8 [label="目录删除触发的双链清理信息"];
  p27 -> d10 [label="目录删除触发的笔记标签清理信息"];
  p27 -> e1 [label="文件夹删除结果"];
}

```

分图 3：目录树生成

```
digraph "分图3_目录树生成" {
  e1 [shape=box, label="E1用户"];

  p28 [shape=circle, label="P2.8\n生成目录树"];

  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];

  e1 -> p28 [label="目录树查询请求"];
  d3 -> p28 [label="知识库校验数据"];
  d4 -> p28 [label="文件夹层级数据"];
  d5 -> p28 [label="笔记基础数据"];
  d9 -> p28 [label="标签数据"];
  d10 -> p28 [label="笔记标签关联数据"];
  p28 -> e1 [label="目录树数据"];
}

```

### **一层数据流图：P3.0笔记与历史版本管理**

```dot
digraph "一层数据流图_P3_0" {
  e1 [shape=box, label="E1用户"];

  p31 [shape=circle, label="P3.1\n创建笔记"];
  p32 [shape=circle, label="P3.2\n查看笔记详情"];
  p33 [shape=circle, label="P3.3\n编辑或移动笔记"];
  p34 [shape=circle, label="P3.4\n标题自动保存"];
  p35 [shape=circle, label="P3.5\n正文自动保存"];
  p36 [shape=circle, label="P3.6\n删除笔记"];
  p37 [shape=circle, label="P3.7\n创建历史快照"];
  p38 [shape=circle, label="P3.8\n查询历史版本"];
  p39 [shape=circle, label="P3.9\n恢复历史版本"];
  p310 [shape=circle, label="P3.10\n删除历史版本"];

  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d7 [shape=cylinder, label="D7笔记历史表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];
  d11 [shape=cylinder, label="D11笔记模板表"];
  d12 [shape=cylinder, label="D12笔记附件表"];

  e1 -> p31 [label="笔记创建信息"];
  d3 -> p31 [label="知识库校验数据"];
  d4 -> p31 [label="文件夹校验数据"];
  d11 -> p31 [label="模板内容数据"];
  p31 -> d5 [label="新笔记基础数据"];
  p31 -> d6 [label="初始正文数据"];
  p31 -> d8 [label="初始双链刷新信息"];
  p31 -> e1 [label="笔记创建结果"];

  e1 -> p32 [label="笔记详情查询请求"];
  d5 -> p32 [label="笔记基础数据"];
  d6 -> p32 [label="笔记正文数据"];
  d8 -> p32 [label="入链出链数据"];
  d9 -> p32 [label="标签数据"];
  d10 -> p32 [label="笔记标签关联数据"];
  d12 -> p32 [label="附件列表数据"];
  p32 -> e1 [label="笔记详情"];

  e1 -> p33 [label="笔记编辑与移动信息"];
  d4 -> p33 [label="文件夹校验数据"];
  d5 -> p33 [label="当前笔记基础数据"];
  p33 -> d5 [label="笔记基础更新数据与移动数据"];
  p33 -> d6 [label="正文更新数据"];
  p33 -> d8 [label="双链刷新信息"];
  p33 -> e1 [label="笔记编辑结果"];

  e1 -> p34 [label="标题自动保存信息"];
  d5 -> p34 [label="当前标题数据"];
  p34 -> d5 [label="标题更新数据"];
  p34 -> d8 [label="失效链接刷新信息"];
  p34 -> e1 [label="标题保存结果"];

  e1 -> p35 [label="正文自动保存信息"];
  d6 -> p35 [label="当前正文数据"];
  p35 -> d6 [label="正文更新数据"];
  p35 -> d8 [label="双链刷新信息"];
  p35 -> e1 [label="正文保存结果"];

  e1 -> p36 [label="笔记删除请求"];
  d5 -> p36 [label="待删除笔记数据"];
  p36 -> d5 [label="笔记删除标记信息"];
  p36 -> d8 [label="笔记删除触发的双链清理信息"];
  p36 -> d10 [label="笔记删除触发的标签关联清理信息"];
  p36 -> e1 [label="笔记删除结果"];

  e1 -> p37 [label="历史快照创建请求"];
  d5 -> p37 [label="当前笔记标题数据"];
  d6 -> p37 [label="当前笔记正文数据"];
  p37 -> d7 [label="历史快照新增数据"];
  p37 -> e1 [label="历史快照创建结果"];

  e1 -> p38 [label="历史版本查询请求"];
  d7 -> p38 [label="历史版本列表与详情数据"];
  p38 -> e1 [label="历史版本数据"];

  e1 -> p39 [label="历史版本恢复请求"];
  d7 -> p39 [label="历史版本标题与正文数据"];
  p39 -> d5 [label="恢复后的笔记基础数据"];
  p39 -> d6 [label="恢复后的正文数据"];
  p39 -> d8 [label="恢复后的双链刷新信息"];
  p39 -> e1 [label="历史版本恢复结果与笔记详情"];

  e1 -> p310 [label="历史版本删除请求"];
  p310 -> d7 [label="历史版本删除信息"];
  p310 -> e1 [label="历史版本删除结果"];
}
```

#### 分图版

分图 1：笔记创建与查看

```
digraph "分图1_笔记创建与查看" {
  e1 [shape=box, label="E1用户"];

  p31 [shape=circle, label="P3.1\n创建笔记"];
  p32 [shape=circle, label="P3.2\n查看笔记详情"];

  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];
  d11 [shape=cylinder, label="D11笔记模板表"];
  d12 [shape=cylinder, label="D12笔记附件表"];

  e1 -> p31 [label="笔记创建信息"];
  d3 -> p31 [label="知识库校验数据"];
  d4 -> p31 [label="文件夹校验数据"];
  d11 -> p31 [label="模板内容数据"];
  p31 -> d5 [label="新笔记基础数据"];
  p31 -> d6 [label="初始正文数据"];
  p31 -> d8 [label="初始双链刷新信息"];
  p31 -> e1 [label="笔记创建结果"];

  e1 -> p32 [label="笔记详情查询请求"];
  d5 -> p32 [label="笔记基础数据"];
  d6 -> p32 [label="笔记正文数据"];
  d8 -> p32 [label="入链出链数据"];
  d9 -> p32 [label="标签数据"];
  d10 -> p32 [label="笔记标签关联数据"];
  d12 -> p32 [label="附件列表数据"];
  p32 -> e1 [label="笔记详情"];
}

```

分图 2：笔记编辑、自动保存与删除

```
digraph "分图2_笔记编辑自动保存与删除" {
  e1 [shape=box, label="E1用户"];

  p33 [shape=circle, label="P3.3\n编辑或移动笔记"];
  p34 [shape=circle, label="P3.4\n标题自动保存"];
  p35 [shape=circle, label="P3.5\n正文自动保存"];
  p36 [shape=circle, label="P3.6\n删除笔记"];

  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];

  e1 -> p33 [label="笔记编辑与移动信息"];
  d4 -> p33 [label="文件夹校验数据"];
  d5 -> p33 [label="当前笔记基础数据"];
  p33 -> d5 [label="笔记基础更新数据与移动数据"];
  p33 -> d6 [label="正文更新数据"];
  p33 -> d8 [label="双链刷新信息"];
  p33 -> e1 [label="笔记编辑结果"];

  e1 -> p34 [label="标题自动保存信息"];
  d5 -> p34 [label="当前标题数据"];
  p34 -> d5 [label="标题更新数据"];
  p34 -> d8 [label="失效链接刷新信息"];
  p34 -> e1 [label="标题保存结果"];

  e1 -> p35 [label="正文自动保存信息"];
  d6 -> p35 [label="当前正文数据"];
  p35 -> d6 [label="正文更新数据"];
  p35 -> d8 [label="双链刷新信息"];
  p35 -> e1 [label="正文保存结果"];

  e1 -> p36 [label="笔记删除请求"];
  d5 -> p36 [label="待删除笔记数据"];
  p36 -> d5 [label="笔记删除标记信息"];
  p36 -> d8 [label="笔记删除触发的双链清理信息"];
  p36 -> d10 [label="笔记删除触发的标签关联清理信息"];
  p36 -> e1 [label="笔记删除结果"];
}

```

分图 3：历史版本管理

```
digraph "分图3_历史版本管理" {
  e1 [shape=box, label="E1用户"];

  p37 [shape=circle, label="P3.7\n创建历史快照"];
  p38 [shape=circle, label="P3.8\n查询历史版本"];
  p39 [shape=circle, label="P3.9\n恢复历史版本"];
  p310 [shape=circle, label="P3.10\n删除历史版本"];

  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d7 [shape=cylinder, label="D7笔记历史表"];
  d8 [shape=cylinder, label="D8双链关系表"];

  e1 -> p37 [label="历史快照创建请求"];
  d5 -> p37 [label="当前笔记标题数据"];
  d6 -> p37 [label="当前笔记正文数据"];
  p37 -> d7 [label="历史快照新增数据"];
  p37 -> e1 [label="历史快照创建结果"];

  e1 -> p38 [label="历史版本查询请求"];
  d7 -> p38 [label="历史版本列表与详情数据"];
  p38 -> e1 [label="历史版本数据"];

  e1 -> p39 [label="历史版本恢复请求"];
  d7 -> p39 [label="历史版本标题与正文数据"];
  p39 -> d5 [label="恢复后的笔记基础数据"];
  p39 -> d6 [label="恢复后的正文数据"];
  p39 -> d8 [label="恢复后的双链刷新信息"];
  p39 -> e1 [label="历史版本恢复结果与笔记详情"];

  e1 -> p310 [label="历史版本删除请求"];
  p310 -> d7 [label="历史版本删除信息"];
  p310 -> e1 [label="历史版本删除结果"];
}

```

### **一层数据流图：P4.0双链关系管理**

```dot
digraph "一层数据流图_P4_0" {
  e1 [shape=box, label="E1用户"];

  p41 [shape=circle, label="P4.1\n解析双链标记"];
  p42 [shape=circle, label="P4.2\n生成或更新双链关系"];
  p43 [shape=circle, label="P4.3\n维护失效链接"];
  p44 [shape=circle, label="P4.4\n查询双链候选项"];
  p45 [shape=circle, label="P4.5\n预览双链目标"];
  p46 [shape=circle, label="P4.6\n提供入链与出链数据"];

  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d8 [shape=cylinder, label="D8双链关系表"];

  e1 -> p41 [label="双链解析请求与当前笔记内容"];
  p41 -> p42 [label="解析出的双链目标数据"];

  d5 -> p42 [label="目标笔记定位数据"];
  p42 -> d8 [label="双链新增或更新数据"];
  p42 -> e1 [label="双链生成结果"];

  e1 -> p43 [label="失效链接维护请求"];
  d5 -> p43 [label="当前笔记标题数据"];
  d8 -> p43 [label="现有双链数据"];
  p43 -> d8 [label="失效双链状态更新数据"];
  p43 -> e1 [label="失效链接维护结果"];

  e1 -> p44 [label="双链候选查询条件"];
  d5 -> p44 [label="候选笔记数据"];
  p44 -> e1 [label="双链候选项"];

  e1 -> p45 [label="双链预览请求"];
  d5 -> p45 [label="目标笔记定位数据"];
  d6 -> p45 [label="目标笔记正文数据"];
  p45 -> e1 [label="双链预览数据"];

  e1 -> p46 [label="关联查看请求"];
  d8 -> p46 [label="入链出链数据"];
  p46 -> e1 [label="入链与出链信息"];
}
```

### **一层数据流图：P5.0标签与模板管理**

```dot
digraph "一层数据流图_P5_0" {
  e1 [shape=box, label="E1用户"];

  p51 [shape=circle, label="P5.1\n创建标签"];
  p52 [shape=circle, label="P5.2\n查询知识库标签"];
  p53 [shape=circle, label="P5.3\n查询笔记标签"];
  p54 [shape=circle, label="P5.4\n设置笔记标签"];
  p55 [shape=circle, label="P5.5\n删除标签"];
  p56 [shape=circle, label="P5.6\n查询模板列表"];
  p57 [shape=circle, label="P5.7\n创建模板"];
  p58 [shape=circle, label="P5.8\n修改模板"];
  p59 [shape=circle, label="P5.9\n删除模板"];

  d3 [shape=cylinder, label="D3知识库表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];
  d11 [shape=cylinder, label="D11笔记模板表"];

  e1 -> p51 [label="标签创建信息"];
  d3 -> p51 [label="知识库校验数据"];
  d9 -> p51 [label="标签名称校验数据"];
  p51 -> d9 [label="标签新增数据"];
  p51 -> e1 [label="标签创建结果"];

  e1 -> p52 [label="知识库标签查询请求"];
  d3 -> p52 [label="知识库校验数据"];
  d9 -> p52 [label="知识库标签数据"];
  p52 -> e1 [label="知识库标签列表"];

  e1 -> p53 [label="笔记标签查询请求"];
  d5 -> p53 [label="笔记校验数据"];
  d10 -> p53 [label="笔记标签关联数据"];
  d9 -> p53 [label="标签数据"];
  p53 -> e1 [label="笔记标签数据"];

  e1 -> p54 [label="笔记标签设置请求"];
  d5 -> p54 [label="笔记校验数据"];
  d9 -> p54 [label="标签校验数据"];
  d10 -> p54 [label="当前笔记标签关联数据"];
  p54 -> d10 [label="笔记标签设置数据"];
  p54 -> e1 [label="标签设置结果"];

  e1 -> p55 [label="标签删除请求"];
  d9 -> p55 [label="标签数据"];
  d10 -> p55 [label="标签关联数据"];
  p55 -> d9 [label="标签删除信息"];
  p55 -> d10 [label="标签关联清理信息"];
  p55 -> e1 [label="标签删除结果"];

  e1 -> p56 [label="模板列表查询请求"];
  d11 -> p56 [label="模板列表与模板内容"];
  p56 -> e1 [label="模板列表"];

  e1 -> p57 [label="模板创建信息"];
  d11 -> p57 [label="模板名称校验数据"];
  p57 -> d11 [label="模板新增数据"];
  p57 -> e1 [label="模板创建结果"];

  e1 -> p58 [label="模板修改信息"];
  d11 -> p58 [label="当前模板数据与名称校验数据"];
  p58 -> d11 [label="模板更新数据"];
  p58 -> e1 [label="模板修改结果"];

  e1 -> p59 [label="模板删除请求"];
  p59 -> d11 [label="模板删除信息"];
  p59 -> e1 [label="模板删除结果"];
}
```

### **一层数据流图：P6.0附件与导出管理**

```dot
digraph "一层数据流图_P6_0" {
  e1 [shape=box, label="E1用户"];

  p61 [shape=circle, label="P6.1\n上传附件"];
  p62 [shape=circle, label="P6.2\n上传编辑器图片"];
  p63 [shape=circle, label="P6.3\n查询附件列表"];
  p64 [shape=circle, label="P6.4\n下载附件"];
  p65 [shape=circle, label="P6.5\n删除附件"];
  p66 [shape=circle, label="P6.6\n导出Markdown"];
  p67 [shape=circle, label="P6.7\n导出HTML"];
  p68 [shape=circle, label="P6.8\n导出PDF"];

  d2 [shape=cylinder, label="D2用户设置表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d12 [shape=cylinder, label="D12笔记附件表"];

  e1 -> p61 [label="附件文件与上传信息"];
  d5 -> p61 [label="所属笔记校验数据"];
  p61 -> d12 [label="附件新增信息"];
  p61 -> e1 [label="附件上传结果"];

  e1 -> p62 [label="编辑器图片文件"];
  d5 -> p62 [label="所属笔记校验数据"];
  p62 -> d12 [label="图片附件新增信息"];
  p62 -> e1 [label="图片上传结果与访问地址"];

  e1 -> p63 [label="附件列表查询请求"];
  d5 -> p63 [label="所属笔记校验数据"];
  d12 -> p63 [label="附件列表数据"];
  p63 -> e1 [label="附件列表"];

  e1 -> p64 [label="附件下载请求"];
  d12 -> p64 [label="附件元数据与附件数据"];
  p64 -> e1 [label="附件下载内容"];

  e1 -> p65 [label="附件删除请求"];
  d12 -> p65 [label="附件元数据"];
  d5 -> p65 [label="所属笔记校验数据"];
  p65 -> d12 [label="附件删除信息"];
  p65 -> e1 [label="附件删除结果"];

  e1 -> p66 [label="Markdown导出请求"];
  d5 -> p66 [label="笔记标题数据"];
  d6 -> p66 [label="Markdown正文数据"];
  p66 -> e1 [label="Markdown导出文件"];

  e1 -> p67 [label="HTML导出请求"];
  d2 -> p67 [label="用户主题设置数据"];
  d5 -> p67 [label="笔记标题数据"];
  d6 -> p67 [label="Markdown正文数据"];
  p67 -> e1 [label="HTML导出文件"];

  e1 -> p68 [label="PDF导出请求"];
  d2 -> p68 [label="用户主题设置数据"];
  d5 -> p68 [label="笔记标题数据"];
  d6 -> p68 [label="Markdown正文数据"];
  p68 -> e1 [label="PDF导出文件"];
}
```

### **一层数据流图：P7.0检索与知识图谱管理**

```dot
digraph "一层数据流图_P7_0" {
  e1 [shape=box, label="E1用户"];

  p71 [shape=circle, label="P7.1\n执行笔记检索"];
  p72 [shape=circle, label="P7.2\n筛选与排序检索结果"];
  p73 [shape=circle, label="P7.3\n输出检索导航数据"];
  p74 [shape=circle, label="P7.4\n构建全局知识图谱"];
  p75 [shape=circle, label="P7.5\n输出知识图谱数据"];

  d3 [shape=cylinder, label="D3知识库表"];
  d4 [shape=cylinder, label="D4文件夹表"];
  d5 [shape=cylinder, label="D5笔记信息表"];
  d6 [shape=cylinder, label="D6笔记内容表"];
  d8 [shape=cylinder, label="D8双链关系表"];
  d9 [shape=cylinder, label="D9标签信息表"];
  d10 [shape=cylinder, label="D10笔记标签关联表"];

  e1 -> p71 [label="检索条件"];
  d3 -> p71 [label="知识库校验数据"];
  d4 -> p71 [label="文件夹筛选数据"];
  d5 -> p71 [label="笔记基础检索数据"];
  d6 -> p71 [label="正文检索数据"];
  d9 -> p71 [label="标签检索数据"];
  d10 -> p71 [label="笔记标签关联筛选数据"];
  p71 -> p72 [label="原始命中数据"];

  d4 -> p72 [label="目录路径数据"];
  d8 -> p72 [label="入链出链统计数据与失效链接统计数据"];
  p72 -> p73 [label="排序后的检索结果数据"];

  p73 -> e1 [label="检索结果与导航数据"];

  e1 -> p74 [label="知识图谱查询请求"];
  d3 -> p74 [label="知识库校验数据"];
  d5 -> p74 [label="图谱节点数据"];
  d8 -> p74 [label="图谱边数据"];
  p74 -> p75 [label="图谱节点与边结果数据"];

  p75 -> e1 [label="知识图谱数据"];
}
```

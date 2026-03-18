# Space 管理员统计前端接入说明

## 1. 文档目的

`OpenAPI` 是接口字段和契约的唯一真相源。  
这份文档补的是“前端如何用这些接口”的一层说明，主要回答下面几个问题：

- 管理员统计页建议怎么拆页面结构
- 首屏该优先请求哪些接口
- 哪些筛选条件应该全局共享
- 每个接口适合展示什么内容
- 新旧接口如何迁移
- 导出按钮应该接哪个接口

如果这份文档和 OpenAPI spec 有冲突，以 `design/API/NT-API.yml` 为准。

## 2. 管理员页推荐结构

管理员视角建议按“平台运营 / 空间治理”来组织，而不是按老师视角组织。

推荐页面结构：

- `总览 Overview`
  - 给领导和学院管理员看规模与成果
  - 展示 KPI 卡片、趋势图、分类分布、完成情况
- `告警 Alerts`
  - 展示当前需要处理的治理事项
  - 例如待审核题目、待审核报名、待评审提交
- `老师分析 Publishers`
  - 展示老师发题情况和效果对比
  - 用于看哪些老师活跃、哪些老师效果好
- `题目分析 Tasks`
  - 展示具体 task 的转化和治理状态
  - 用于下钻查看问题题目
- `参与者分析 Participants`
  - 展示参与者状态、实名情况、年级专业分布
  - 用于看参与人群画像和完成情况

## 3. 首屏推荐请求方式

首屏不建议一次性把所有资源都打满。

推荐请求顺序：

1. `GET /spaces/{spaceId}/analytics/overview`
2. `GET /spaces/{spaceId}/analytics/alerts`
3. 页面滚动到老师分析区域时，再请求 `GET /spaces/{spaceId}/analytics/publishers`
4. 页面滚动到题目分析区域时，再请求 `GET /spaces/{spaceId}/analytics/tasks`
5. 页面滚动到参与者分析区域时，再请求 `GET /spaces/{spaceId}/analytics/participants`

这样做的好处：

- 领导首屏更快
- 表格型资源延迟加载
- 页面不需要一进来就做所有聚合请求

## 4. 共享筛选条件

管理员统计页建议维护一个统一的筛选状态，并尽量复用到各个 analytics 资源。

### 4.1 全局共享筛选

这些参数建议在页面顶部统一维护：

- `from`
- `to`
- `categoryId`
- `taskApproved`

### 4.2 部分资源共享筛选

- `publisherId`
  - 适用于 `overview`
  - 适用于 `tasks`
  - 适用于 `participants`
- `groupBy`
  - 适用于 `overview`
  - 适用于 `participants`
- `sortBy` / `sortOrder`
  - 适用于 `publishers`
  - 适用于 `tasks`

### 4.3 资源专属筛选

- `hasPendingReview`
  - 只适用于 `tasks`
- `hasPendingApproval`
  - 只适用于 `tasks`
- `participationApproved`
  - 只适用于 `participants`
- `completionStatus`
  - 只适用于 `participants`
- `realName`
  - 只适用于 `participants`

### 4.4 推荐默认值

- 时间范围默认最近 `30` 天，或者当前学期
- `groupBy`
  - 短时间窗口用 `day`
  - 长时间窗口用 `week`
- 面向领导展示时，如果不希望待审核题目稀释成果数据，可以默认 `taskApproved=APPROVED`

## 5. 各接口怎么用

### 5.1 总览

接口：

- `GET /spaces/{spaceId}/analytics/overview`

适合展示：

- KPI 卡片
- 分类分布
- 审批状态分布
- 完成状态分布
- 趋势图

推荐卡片映射：

- `entityMetrics.taskCount`
  - 题目总数
- `entityMetrics.publisherCount`
  - 发题老师数
- `entityMetrics.participantCount`
  - 报名主体数
- `entityMetrics.approvedParticipantCount`
  - 审核通过报名数
- `entityMetrics.submittedParticipantCount`
  - 至少提交过一次的报名主体数
- `entityMetrics.successfulParticipantCount`
  - 成功主体数
- `studentMetrics.studentCount`
  - 真实学生人数
- `studentMetrics.approvedStudentCount`
  - 审核通过学生人数
- `studentMetrics.successfulStudentCount`
  - 成功学生人数
- `entityMetrics.participationConversionRate`
  - 报名审批转化率
- `entityMetrics.submissionConversionRate`
  - 提交转化率
- `entityMetrics.successRate`
  - 成功率

重要语义：

- `participantCount` 按报名主体算
  - 个人报名算 `1`
  - 团队报名也算 `1`
- `studentMetrics` 按真实学生人数算
  - 团队会展开为团队成员快照人数
- `successfulParticipantCount` 的口径是 `TaskMembership.completionStatus == SUCCESS`

推荐图表：

- `taskDistributions.byCategory`
  - 题目分类分布
- `taskDistributions.byApprovalStatus`
  - 题目审批状态分布
- `taskDistributions.byCompletionStatus`
  - 参与完成状态分布
- `trends.tasksCreated`
  - 发题趋势
- `trends.participantsJoined`
  - 报名趋势
- `trends.submissionsCreated`
  - 提交趋势
- `trends.successesAchieved`
  - 成功趋势

### 5.2 告警

接口：

- `GET /spaces/{spaceId}/analytics/alerts`

适合展示：

- 页面顶部治理卡片
- 待办提示
- 异常项提示

推荐映射：

- `pendingTaskApprovalCount`
  - 待审核题目数
- `pendingParticipantApprovalCount`
  - 待审核报名数
- `pendingSubmissionReviewCount`
  - 待评审提交数
- `stalledTaskCount`
  - 已有人报名但长时间无提交的题目数
- `overdueUnreviewedSubmissionCount`
  - 超时未评审提交数
- `inactivePublisherCount`
  - 长时间未发题老师数

当前第一版阈值由后端固定：

- `stalledTaskCount` 阈值：`14` 天
- `overdueUnreviewedSubmissionCount` 阈值：`7` 天
- `inactivePublisherCount` 阈值：`30` 天

前端暂时只把这些阈值作为说明文案展示，不做前端配置。

### 5.3 老师分析

接口：

- `GET /spaces/{spaceId}/analytics/publishers`

适合展示：

- 老师对比表
- 活跃老师排行
- 发题效果分析

推荐默认排序：

- `sortBy=taskCount&sortOrder=desc`

常用排序：

- `sortBy=successRate&sortOrder=desc`
- `sortBy=participantCount&sortOrder=desc`
- `sortBy=lastTaskCreatedAt&sortOrder=desc`

推荐列：

- `publisherName`
- `taskCount`
- `participantCount`
- `approvedParticipantCount`
- `submittedParticipantCount`
- `successfulParticipantCount`
- `avgParticipantsPerTask`
- `submissionConversionRate`
- `successRate`
- `lastTaskCreatedAt`

### 5.4 题目分析

接口：

- `GET /spaces/{spaceId}/analytics/tasks`

适合展示：

- task 级别下钻表格
- 治理问题题目排查
- 某个老师名下题目的效果分析

推荐默认排序：

- `sortBy=createdAt&sortOrder=desc`

推荐列：

- `taskName`
- `publisher.name`
- `category.name`
- `approved`
- `createdAt`
- `deadline`
- `participantCount`
- `pendingParticipantApprovalCount`
- `approvedParticipantCount`
- `rejectedParticipantCount`
- `submittedParticipantCount`
- `pendingReviewCount`
- `resubmittableCount`
- `successfulParticipantCount`
- `failedParticipantCount`
- `submissionConversionRate`
- `successRate`

推荐快捷筛选：

- `hasPendingApproval=true`
- `hasPendingReview=true`
- `publisherId=<teacherId>`
- `categoryId=<categoryId>`

### 5.5 参与者分析

接口：

- `GET /spaces/{spaceId}/analytics/participants`

适合展示：

- 参与者状态总览
- 实名情况
- 学生年级 / 专业 / 班级分布
- 成功 / 未成功参与者画像

推荐卡片：

- `entityMetrics.participantCount`
- `entityMetrics.approvedParticipantCount`
- `entityMetrics.pendingParticipantCount`
- `entityMetrics.disapprovedParticipantCount`
- `entityMetrics.submittedParticipantCount`
- `entityMetrics.successfulParticipantCount`
- `studentMetrics.studentCount`
- `studentMetrics.studentsWithRealNameCount`

推荐图表：

- `distributions.byApprovalStatus`
- `distributions.byCompletionStatus`
- `distributions.byGrade`
- `distributions.byMajor`
- `distributions.byClassName`
- `distributions.byRealNameStatus`

推荐趋势图：

- `trends.participantsJoined`
- `trends.submissionsCreated`
- `trends.successesAchieved`

参数语义：

- `participationApproved`
  - 按报名审批状态过滤
- `completionStatus`
  - 按完成状态过滤
- `realName`
  - `all`
  - `with`
  - `without`

这个接口可以承接以前旧报表里“成功学生画像 / 未成功学生画像”的展示需求，前端不需要再按旧接口结构做两套页面。

## 6. 导出接口怎么接

新导出接口已经按 analytics 资源风格拆开，建议前端“当前筛选条件导出”直接复用当前页面筛选状态。

### 6.1 导出参与者

接口：

- `GET /spaces/{spaceId}/analytics/participants/export`

适用场景：

- 导出报名明细
- 导出实名信息明细
- 导出参与和完成状态明细

建议和 `analytics/participants` 共用以下筛选：

- `from`
- `to`
- `categoryId`
- `publisherId`
- `taskApproved`
- `participationApproved`
- `completionStatus`
- `realName`

### 6.2 导出题目

接口：

- `GET /spaces/{spaceId}/analytics/tasks/export`

适用场景：

- 导出 task 级别运营表
- 导出题目治理问题清单

建议和 `analytics/tasks` 共用以下筛选：

- `from`
- `to`
- `categoryId`
- `publisherId`
- `taskApproved`
- `hasPendingReview`
- `hasPendingApproval`

### 6.3 导出老师

接口：

- `GET /spaces/{spaceId}/analytics/publishers/export`

适用场景：

- 导出老师发题效果表
- 导出老师维度汇报材料

建议和 `analytics/publishers` 共用以下筛选：

- `from`
- `to`
- `categoryId`
- `taskApproved`

### 6.4 前端接入建议

- 导出按钮应直接复用当前页面筛选状态
- 导出时不需要额外请求 analytics JSON 接口后前端拼 CSV
- 直接下载后端返回的 `text/csv`
- 页面上应明确提示“导出内容口径与当前筛选一致”

## 7. 新旧接口迁移说明

### 7.1 应该优先使用的新接口

- `GET /spaces/{spaceId}/analytics/overview`
- `GET /spaces/{spaceId}/analytics/alerts`
- `GET /spaces/{spaceId}/analytics/publishers`
- `GET /spaces/{spaceId}/analytics/tasks`
- `GET /spaces/{spaceId}/analytics/participants`
- `GET /spaces/{spaceId}/analytics/participants/export`
- `GET /spaces/{spaceId}/analytics/tasks/export`
- `GET /spaces/{spaceId}/analytics/publishers/export`

### 7.2 已不建议继续新增使用的旧接口

- `GET /spaces/{spaceId}/publishers/participation`
  - 已 deprecated
  - 老版老师聚合统计
- `GET /spaces/{spaceId}/participants/export`
  - 已 deprecated
  - 旧版导出入口

### 7.3 需要特别注意的 breaking change

- `GET /spaces/{spaceId}/analytics/tasks`
  - 现在返回的是 task metrics 列表
  - 不再是旧的“大报表结构”

前端如果还在按旧 `analytics/tasks` 的字段渲染，必须切到新的字段结构。

## 8. 前端实现建议

- 整个管理员统计页维护一个统一 filter store
- 百分比字段按 `0.0 - 1.0` 处理，前端再渲染成 `%`
- 不要把 `participantCount` 直接标成“学生数”
- 页面上要明确区分：
  - `报名主体数`
  - `真实学生人数`
- `publisherId` 是题目 / 参与者下钻筛选，不要误以为 `publishers` 自身也支持这个参数
- 导出按钮默认放在对应分区右上角，不建议再做一个“万能导出”下拉覆盖所有资源

## 9. 推荐首屏组合

首屏建议这样拼：

1. 请求 `overview`
2. 请求 `alerts`
3. 渲染 KPI 卡片和趋势图
4. 老师分析区域懒加载 `publishers`
5. 题目分析区域懒加载 `tasks`
6. 参与者分析区域懒加载 `participants`

这样既能满足“领导看成果”，也能保留后续下钻分析能力。

## 10. 真相源

接口契约和字段定义：

- `design/API/NT-API.yml`

如果这份文档和 spec 不一致，以 spec 为准。

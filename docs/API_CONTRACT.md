# 海洋生物知识库 API Contract

## 1. 文档目的

本文档定义前后端接口契约，指导后端 API 实现和前端接口调用。所有接口基于 RESTful 风格，JSON 格式传输。

## 2. API 原则

| 原则 | 说明 |
|------|------|
| API 风格 | RESTful，GET 查询、POST 新增/保存、DELETE 删除 |
| 认证方式 | JWT Token，Header `token: <value>` |
| 租户 scope | 无，单实例 |
| RBAC | 管理员/普通用户两档，通过拦截器校验 |
| DTO 优先 | 不直接返回数据库模型，使用 CommonResp 包装 |
| 敏感字段禁返 | password 字段永远不返回 |
| 分页 | 标准化分页参数 page + size |

## 3. Auth Context

后端从 JWT Token 中解析用户信息：

```json
{
  "userId": 123456789,
  "loginName": "admin",
  "name": "管理员"
}
```

Token 存储于 Redis，Key：`token:{userId}`，TTL 24h。

## 4. 通用响应结构

### 成功响应
```json
{
  "success": true,
  "message": "操作成功",
  "content": { ... }
}
```

### 列表响应
```json
{
  "success": true,
  "message": "查询成功",
  "content": {
    "total": 100,
    "list": [ ... ]
  }
}
```

### 错误响应
```json
{
  "success": false,
  "message": "参数校验失败：名称不能为空",
  "content": null
}
```

## 5. 错误码

| HTTP 状态码 | 场景 | message 示例 |
|------------|------|-------------|
| 200 | 成功 | "操作成功" / "查询成功" |
| 400 | 参数校验失败 | "参数校验失败：名称不能为空" |
| 401 | 未认证 / Token 无效 | "登录已过期，请重新登录" |
| 403 | 无权限 | "无权限访问" |
| 404 | 资源不存在 | "电子书不存在" |
| 500 | 系统异常 | "系统异常，请稍后重试" |

## 6. 分页、排序、过滤

### 分页参数（Query）
```
?page=1&size=10
```

### 过滤参数（Query）
```
?name=海洋        // 模糊搜索
?category1Id=1    // 分类过滤
?ebookId=123      // 电子书过滤
```

### 排序
默认按 sort 字段升序，无需额外参数。

## 7. RBAC 和 Scope 规则

| API 类别 | 公开访问 | 登录用户 | 管理员 |
|---------|---------|---------|--------|
| 电子书列表/详情 | ✅ | ✅ | ✅ |
| 电子书 CRUD | ❌ | ❌ | ✅ |
| 分类查询 | ✅ | ✅ | ✅ |
| 分类 CRUD | ❌ | ❌ | ✅ |
| 文档查询/阅读 | ✅ | ✅ | ✅ |
| 文档 CRUD | ❌ | ❌ | ✅ |
| 文档点赞 | ❌ | ✅ | ✅ |
| 用户管理 | ❌ | ❌ | ✅ |
| 统计查询 | ✅ | ✅ | ✅ |
| 登录/退出 | ✅ | ✅ | ✅ |

## 8. API 模块概览

| 模块 | Endpoint 数量 | Phase |
|------|--------------|-------|
| 电子书管理 | 5 | V1a |
| 分类管理 | 4 | V1a |
| 文档管理 | 5 | V1a |
| 用户管理 | 6 | V1a |
| 统计 | 1 | V1b |
| 文件上传 | 1 | V1a |
| Swagger 文档 | 1 | V1a |

### Swagger API 文档

- **访问地址**：`http://localhost:8080/doc.html`（knife4j 增强 UI）
- **配置要求**：
  - 所有 Controller 类添加 `@Api(tags = "模块名称")`
  - 所有接口方法添加 `@ApiOperation("功能描述")`
  - 请求参数使用 `@ApiParam` 注解
  - 响应对象使用 `@ApiModel` / `@ApiModelProperty` 注解
- **在线测试**：支持直接在页面上调试接口，自动携带 Token

## 9. Endpoint 详情

### 9.1 电子书管理

#### GET /api/ebook/list
- **Purpose**：分页查询电子书列表
- **Roles**：公开
- **Query**：page, size, name?, category1Id?, category2Id?
- **Response**：
```json
{
  "success": true,
  "message": "查询成功",
  "content": {
    "total": 50,
    "list": [
      {
        "id": 123456789,
        "name": "海洋鱼类图鉴",
        "category1Id": 1,
        "category1Name": "鱼类",
        "category2Id": 11,
        "category2Name": "深海鱼类",
        "description": "介绍深海鱼类的分类和特征",
        "cover": "/files/cover/fish.jpg",
        "docCount": 25,
        "viewCount": 1500,
        "voteCount": 89,
        "createTime": "2026-01-15 10:30:00",
        "updateTime": "2026-06-10 14:20:00"
      }
    ]
  }
}
```

#### POST /api/ebook/save
- **Purpose**：新增或编辑电子书（id 为空则新增，有值则编辑）
- **Roles**：管理员
- **Request**：
```json
{
  "id": null,
  "name": "海洋鱼类图鉴",
  "category1Id": 1,
  "category2Id": 11,
  "description": "介绍深海鱼类的分类和特征",
  "cover": "/files/cover/fish.jpg"
}
```
- **校验**：name 非空（最大 50）、category1Id 非空、category2Id 非空
- **Response**：`{ "success": true, "message": "保存成功", "content": null }`

#### DELETE /api/ebook/delete/{id}
- **Purpose**：删除电子书
- **Roles**：管理员
- **Path**：id（电子书 ID）
- **Response**：`{ "success": true, "message": "删除成功", "content": null }`

---

### 9.2 分类管理

#### GET /api/category/list
- **Purpose**：查询分类树形列表
- **Roles**：公开
- **Response**：
```json
{
  "success": true,
  "message": "查询成功",
  "content": [
    {
      "id": 1,
      "parent": 0,
      "name": "鱼类",
      "sort": 1,
      "children": [
        {
          "id": 11,
          "parent": 1,
          "name": "深海鱼类",
          "sort": 1,
          "children": []
        }
      ]
    }
  ]
}
```

#### POST /api/category/save
- **Purpose**：新增或编辑分类
- **Roles**：管理员
- **Request**：
```json
{
  "id": null,
  "parent": 1,
  "name": "深海鱼类",
  "sort": 1
}
```
- **校验**：name 非空（最大 20）、parent 非空、不能将自己设为父分类
- **Response**：`{ "success": true, "message": "保存成功", "content": null }`

#### DELETE /api/category/delete/{id}
- **Purpose**：删除分类
- **Roles**：管理员
- **校验**：存在子分类时禁止删除
- **Response**：`{ "success": true, "message": "删除成功", "content": null }`

---

### 9.3 文档管理

#### GET /api/doc/list
- **Purpose**：查询某电子书下的文档树形列表
- **Roles**：公开
- **Query**：ebookId（必填）
- **Response**：
```json
{
  "success": true,
  "message": "查询成功",
  "content": [
    {
      "id": 101,
      "ebookId": 123,
      "parent": 0,
      "name": "第一章 深海生态",
      "sort": 1,
      "viewCount": 200,
      "voteCount": 15,
      "children": [
        {
          "id": 102,
          "ebookId": 123,
          "parent": 101,
          "name": "1.1 深海环境概述",
          "sort": 1,
          "viewCount": 150,
          "voteCount": 10,
          "children": []
        }
      ]
    }
  ]
}
```

#### GET /api/doc/{id}
- **Purpose**：获取文档详情（含正文内容）
- **Roles**：公开
- **Path**：id（文档 ID）
- **Response**：
```json
{
  "success": true,
  "message": "查询成功",
  "content": {
    "id": 101,
    "ebookId": 123,
    "parent": 0,
    "name": "第一章 深海生态",
    "sort": 1,
    "viewCount": 201,
    "voteCount": 15,
    "content": "<p>深海是指海洋中深度超过200米的区域...</p>"
  }
}
```
- **副作用**：view_count +1

#### POST /api/doc/save
- **Purpose**：新增或编辑文档（含内容）
- **Roles**：管理员
- **Request**：
```json
{
  "id": null,
  "ebookId": 123,
  "parent": 0,
  "name": "第一章 深海生态",
  "sort": 1,
  "content": "<p>深海是指海洋中深度超过200米的区域...</p>"
}
```
- **校验**：name 非空（最大 50）、ebookId 非空、content 经 XSS 过滤
- **Response**：`{ "success": true, "message": "保存成功", "content": null }`
- **注意**：保存时 doc 表和 content 表同步更新

#### DELETE /api/doc/delete/{id}
- **Purpose**：删除文档（含递归子文档和内容）
- **Roles**：管理员
- **Path**：id（文档 ID）
- **Response**：`{ "success": true, "message": "删除成功", "content": null }`

#### POST /api/doc/vote/{id}
- **Purpose**：文档点赞
- **Roles**：登录用户
- **Path**：id（文档 ID）
- **校验**：Redis 防重（同 IP 同文档每天一次）
- **Response**：`{ "success": true, "message": "点赞成功", "content": null }`
- **副作用**：vote_count +1、发送 MQ 消息触发通知

---

### 9.4 用户管理

#### POST /api/user/login
- **Purpose**：用户登录
- **Roles**：公开
- **Request**：
```json
{
  "loginName": "admin",
  "password": "admin123"
}
```
- **Response**：
```json
{
  "success": true,
  "message": "登录成功",
  "content": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userId": 123456789,
    "loginName": "admin",
    "name": "管理员"
  }
}
```

#### GET /api/user/logout
- **Purpose**：用户退出
- **Roles**：登录用户
- **Response**：`{ "success": true, "message": "退出成功", "content": null }`
- **副作用**：清除 Redis 中的 Token

#### GET /api/user/list
- **Purpose**：分页查询用户列表
- **Roles**：管理员
- **Query**：page, size, loginName?
- **Response**：用户列表（不含 password 字段）

#### POST /api/user/save
- **Purpose**：新增或编辑用户
- **Roles**：管理员
- **Request**：
```json
{
  "id": null,
  "loginName": "user01",
  "name": "张三",
  "password": "abc123"
}
```
- **校验**：loginName 非空且唯一、name 非空、password（新增时必填，编辑时可空）
- **Response**：`{ "success": true, "message": "保存成功", "content": null }`
- **敏感处理**：password MD5+盐值加密后存储

#### DELETE /api/user/delete/{id}
- **Purpose**：删除用户
- **Roles**：管理员
- **Response**：`{ "success": true, "message": "删除成功", "content": null }`

#### POST /api/user/reset-password
- **Purpose**：重置用户密码
- **Roles**：管理员
- **Request**：
```json
{
  "userId": 123456789,
  "password": "newPass123"
}
```
- **校验**：password 6-32 位，必须包含数字和字母
- **Response**：`{ "success": true, "message": "密码重置成功", "content": null }`

---

### 9.5 统计

#### GET /api/snapshot/get-statistic
- **Purpose**：获取首页统计数据
- **Roles**：公开
- **Response**：
```json
{
  "success": true,
  "message": "查询成功",
  "content": {
    "totalViewCount": 50000,
    "totalVoteCount": 3200,
    "voteRate": 6.4,
    "todayViewCount": 350,
    "todayVoteCount": 22,
    "expectedTodayViewCount": 420,
    "viewIncreaseRate": 12.5,
    "voteIncreaseRate": 8.3,
    "trendList": [
      { "date": "2026-05-16", "viewIncrease": 280, "voteIncrease": 18 },
      { "date": "2026-05-17", "viewIncrease": 310, "voteIncrease": 22 }
    ]
  }
}
```

---

### 9.6 文件上传

#### POST /api/file/upload
- **Purpose**：上传封面图片
- **Roles**：管理员
- **Content-Type**：multipart/form-data
- **Request**：file（图片文件）
- **Response**：
```json
{
  "success": true,
  "message": "上传成功",
  "content": "/files/cover/abc123.jpg"
}
```
- **校验**：仅允许 jpg/png/gif，最大 2MB

## 10. 非目标和禁止 API

以下 API 在 V1 中不实现：

- 用户自主注册接口（POST /api/user/register）
- 修改密码接口（用户自己改密码）
- 评论相关接口
- 收藏相关接口
- 阅读历史接口
- 数据导出接口
- 批量操作接口

## 11. V1 必须 API 列表

| 序号 | Method | Path | 阶段 |
|------|--------|------|------|
| 1 | GET | /api/ebook/list | V1a |
| 2 | POST | /api/ebook/save | V1a |
| 3 | DELETE | /api/ebook/delete/{id} | V1a |
| 4 | GET | /api/category/list | V1a |
| 5 | POST | /api/category/save | V1a |
| 6 | DELETE | /api/category/delete/{id} | V1a |
| 7 | GET | /api/doc/list | V1a |
| 8 | GET | /api/doc/{id} | V1a |
| 9 | POST | /api/doc/save | V1a |
| 10 | DELETE | /api/doc/delete/{id} | V1a |
| 11 | POST | /api/doc/vote/{id} | V1b |
| 12 | POST | /api/user/login | V1a |
| 13 | GET | /api/user/logout | V1a |
| 14 | GET | /api/user/list | V1a |
| 15 | POST | /api/user/save | V1a |
| 16 | DELETE | /api/user/delete/{id} | V1a |
| 17 | POST | /api/user/reset-password | V1a |
| 18 | GET | /api/snapshot/get-statistic | V1b |
| 19 | POST | /api/file/upload | V1a |

**总计**：19 个业务 API + Swagger 文档（/doc.html）

## 12. 开放问题

| 问题 | 影响 | 建议 |
|------|------|------|
| 电子书列表是否需要返回分类名称 | 前端展示 | 返回 category1Name、category2Name（JOIN 查询或冗余） |
| 点赞接口是否需要返回最新点赞数 | 前端刷新 | 返回更新后的 voteCount |
| 文件上传路径规则 | 存储管理 | /files/cover/{uuid}.{ext} |

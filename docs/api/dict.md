# 课安 Phase 2 学校 / 校区 / 课程

统一响应：`{ "code": 0, "message": "ok", "data": {} }`。`code = 0` 成功。

以下接口均可匿名访问。若携带有效 Access Token，则按登录用户所属学校过滤；未登录默认 `schoolId = 1`。

---

## GET /api/schools

启用中的学校列表（注册选校）。

响应 `data`：

```json
[{ "id": 1, "name": "演示大学" }]
```

---

## GET /api/campuses

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| schoolId | number | 否 | 学校 ID |

响应 `data`：

```json
[{ "id": 1, "schoolId": 1, "name": "主校区" }]
```

---

## GET /api/courses

课程必须选自管理员目录，用户不可自填课程名。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| schoolId | number | 否 | 学校 ID |

响应 `data`：

```json
[
  { "id": 1, "schoolId": 1, "courseCode": "CS101", "courseName": "大学计算机基础" }
]
```

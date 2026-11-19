# 鉴权实现说明

本文记录本项目基于 JWT 的注册登录与数据隔离实现，包括设计取舍和已知限制，方便后续维护与面试复盘。

## 一、整体流程

```
注册/登录 ──► AuthService 校验密码（BCrypt）
                 │
                 └─► JwtUtil.generate(userId) 生成 token 返回前端
                         前端存 localStorage，之后每个请求带 Authorization: Bearer <token>

业务请求 ──► AuthInterceptor.preHandle 验签，取出 userId
                 │
                 ├─► UserContext.set(userId)   （写入 ThreadLocal）
                 │
                 ├─► Controller 通过 UserContext.get() 获取当前用户
                 │
                 └─► afterCompletion 中 UserContext.clear()
```

## 二、JWT 的生成与校验

### 生成

```java
Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expireHours * 3600_000L))
        .signWith(key())
        .compact();
```

token 由三段组成，用 `.` 分隔：

```
header . payload . signature
```

- header / payload 只是 Base64 编码，**不是加密**，任何人解码都能看到 `userId` 和过期时间。因此 token 中不放密码、手机号等敏感信息。
- signature 使用 `jwt.secret` 对前两段做 HMAC-SHA256。payload 被篡改后签名无法对应，验签直接失败。

核心特性：**内容公开，但不可篡改**。

### 校验

```java
Jwts.parser().verifyWith(key()).build().parseSignedClaims(token)
```

该方法同时完成验签、过期检查、payload 解析，任一环节失败都会抛异常，`parseUserId` 捕获后返回 `null`，由拦截器按未登录处理。

## 三、密码存储

注册时使用 BCrypt 加密：

```java
user.setPassword(passwordEncoder.encode(password));
```

BCrypt 的两个关键特点：

1. **自带随机盐**：同一密码每次加密结果不同，数据库泄漏也无法用彩虹表反查。
2. **计算故意慢**（默认强度 10）：显著提高暴力破解成本。

登录时使用 `matches` 比对：

```java
passwordEncoder.matches(password, user.getPassword())
```

BCrypt 会从数据库的哈希中取出盐重新计算再比对。**不能使用 `encode(输入).equals(数据库值)`**，因为盐不同，结果永远不相等。

> 注册流程是"先查重名再插入"，并发下仍可能重复。生产环境需要为 `username` 建唯一索引兜底。

## 四、拦截器

```java
if (auth != null && auth.startsWith("Bearer ")) {
    Long userId = jwtUtil.parseUserId(auth.substring(7));
    if (userId != null) {
        UserContext.set(userId);
        return true;
    }
}
response.setStatus(401);
return false;
```

细节：

- 放行 `OPTIONS` 请求：浏览器跨域预检不带 token，不放行会导致跨域失败。
- `return false` 表示拦截，不再进入 Controller。
- `afterCompletion` 中必须清理 ThreadLocal。

拦截范围在 `WebConfig` 中配置：

```java
registry.addInterceptor(authInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/auth/**");
```

注册、登录接口必须排除，否则会出现"登录需要先登录"的死锁。

## 五、当前用户上下文与 ThreadLocal

Tomcat 为每个请求分配一个线程，`ThreadLocal` 提供线程私有的存储：

```
请求 A（线程 T1）：拦截器写入 userId=1 → 业务层读取到 1
请求 B（线程 T2）：拦截器写入 userId=2 → 业务层读取到 2
```

**必须调用 `clear()` 的原因**：Tomcat 复用线程。如果不清除，T1 处理完请求 A 后接着处理请求 C 时，C 会读到 A 遗留的 userId，导致数据串号。`afterCompletion` 无论请求成功或异常都会执行，是清理的可靠位置。

## 六、数据隔离

仅校验"已登录"不足以保护数据，还必须保证用户只能访问自己的记录：

```java
// 查询：所有 SQL 附带 user_id 条件
.eq(SleepRecord::getUserId, userId)

// 修改 / 删除：先查出记录并校验归属
if (existing == null || !userId.equals(existing.getUserId())) {
    return null;
}
```

这属于对**水平越权（IDOR）**的防护。若缺少校验，用户把 URL 中的 `/api/records/5` 改成 `/api/records/6` 即可操作他人数据。

## 七、已知限制

| 限制 | 说明 | 改进方向 |
| --- | --- | --- |
| token 无法主动失效 | 修改密码后旧 token 在 72 小时内仍有效 | Redis 黑名单，或用户表增加 tokenVersion 并在验签时比对 |
| token 存于 localStorage | 存在 XSS 读取风险 | 改为 HttpOnly Cookie，并配合 CSRF 防护 |
| 单机内存缓存 | AI 建议缓存重启即失效 | 多实例部署时改用 Redis |

## 八、常见面试问题

**JWT 与 Session 的区别？**
Session 在服务端保存状态，通过 cookie 中的 sessionId 查询；JWT 无状态，服务端不存储任何会话数据，依赖签名自证。JWT 便于水平扩展，代价是无法主动作废。

**用户修改密码后，旧 token 还能用吗？**
当前实现可以继续使用，直到过期。需要通过黑名单或 tokenVersion 机制解决。

**为什么用 ThreadLocal 传递用户信息？**
避免在每个方法签名中层层传递 userId，同时保证线程隔离。使用后必须清理，防止线程复用造成数据串通。

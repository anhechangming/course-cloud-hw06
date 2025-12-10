# 校园选课系统 - 微服务版

**项目名称**: course-cloud
**版本**: v1.0.0
**基于**: course v1.1.0 (单体应用)

## 项目简介

本项目是将单体选课系统拆分为微服务架构的实践项目。通过服务拆分、独立数据库、HTTP通信等技术，实现了课程管理、学生管理和选课管理的解耦。

## 架构图

```
客户端
  ↓
Nacos (8848) - 服务注册/发现/负载均衡
  ↓
  ├─→ user-service (8081) → user_db (3306)
  │   └── 学生/用户管理
  │   └── 注册到Nacos
  │
  ├─→ catalog-service (8082) → catalog_db (3307)
  │   └── 课程管理
  │   └── 注册到Nacos
  │
  └─→ enrollment-service (8083) → enrollment_db (3308)
      ├── 选课管理
      ├── 注册到Nacos
      ├── HTTP调用 → user-service（验证学生，负载均衡）
      └── HTTP调用 → catalog-service（验证课程，负载均衡）
```

## 技术栈

- **Spring Boot**: 3.3.4
- **Java**: 21
- **MySQL**: 8.4
- **Docker & Docker Compose**: 容器化部署
- **RestTemplate**: 服务间通信
- **Nacos**: 2.2.3（服务注册发现、健康检查、故障转移）
- **Spring Cloud Alibaba Nacos Discovery**: 微服务注册发现适配

## 服务说明

### user-service (用户服务)

- **端口**: 8081
- **数据库**: user_db (3306)
- **功能**: 学生/用户管理
- **API端点**:
  - `GET /api/students` - 获取所有学生
  - `GET /api/students/{id}` - 获取单个学生
  - `GET /api/students/studentId/{studentId}` - 按学号查询
  - `POST /api/students` - 创建学生
  - `PUT /api/students/{id}` - 更新学生
  - `DELETE /api/students/{id}` - 删除学生

### catalog-service (课程目录服务)

- **端口**: 8082
- **数据库**: catalog_db (3307)
- **功能**: 课程管理
- **API端点**:
  - `GET /api/courses` - 获取所有课程
  - `GET /api/courses/{id}` - 获取单个课程
  - `GET /api/courses/code/{code}` - 按课程代码查询
  - `POST /api/courses` - 创建课程
  - `PUT /api/courses/{id}` - 更新课程
  - `DELETE /api/courses/{id}` - 删除课程

### enrollment-service (选课服务)

- **端口**: 8083
- **数据库**: enrollment_db (3308)
- **功能**: 选课管理，通过RestTemplate调用user-service和catalog-service
- **API端点**:
  - `GET /api/enrollments` - 获取所有选课记录
  - `GET /api/enrollments/course/{courseId}` - 按课程查询选课
  - `GET /api/enrollments/student/{studentId}` - 按学生查询选课
  - `POST /api/enrollments` - 学生选课
  - `DELETE /api/enrollments/{id}` - 学生退课

## 环境要求

- JDK 25+
- Maven 3.8+
- Docker 20.10+
- Docker Compose 2.0+

## 构建和运行步骤

### 快速启动（推荐）

使用 `run.sh` 脚本一键构建并启动所有服务：

```bash
# 赋予执行权限（首次运行需要）
chmod +x run.sh

# 构建并启动所有服务
./run.sh
```

脚本会自动完成以下操作：
1. 编译所有服务的 JAR 文件
2. 构建 Docker 镜像并启动容器
3. 等待服务启动完成
4. 显示服务状态和访问地址

### 手动构建

#### 1. 构建所有服务

```bash
# 构建 user-service
cd user-service
mvn clean package -DskipTests
cd ..

# 构建 catalog-service
cd catalog-service
mvn clean package -DskipTests
cd ..

# 构建 enrollment-service
cd enrollment-service
mvn clean package -DskipTests
cd ..
```

#### 2. 使用 Docker Compose 部署

```bash
# 启动所有服务
docker-compose up -d --build

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f

# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

#### 3. 验证服务

```bash
# 检查 user-service
curl http://localhost:8081/api/students

# 检查 catalog-service
curl http://localhost:8082/api/courses

# 检查 enrollment-service
curl http://localhost:8083/api/enrollments
```

## 测试说明

运行测试脚本：

```bash
chmod +x test-services.sh
./test-services.sh
```

测试脚本会执行以下操作：

1. 创建学生（user-service）
2. 获取所有学生
3. 创建课程（catalog-service）
4. 获取所有课程
5. 学生选课（验证服务间通信）
6. 查询选课记录
7. 测试学生不存在的错误处理
8. 测试课程不存在的错误处理

## 服务间通信示例

enrollment-service 通过 RestTemplate 调用其他服务：

```java
// 验证学生是否存在
String userUrl = userServiceUrl + "/api/students/studentId/" + studentId;
Map<String, Object> studentResponse = restTemplate.getForObject(userUrl, Map.class);

// 验证课程是否存在
String courseUrl = catalogServiceUrl + "/api/courses/" + courseId;
Map<String, Object> courseResponse = restTemplate.getForObject(courseUrl, Map.class);
```

## 数据库配置

| 服务 | 数据库 | 端口 | 用户名 | 密码 |
|------|--------|------|--------|------|
| user-service | user_db | 3306 | user_user | user_pass |
| catalog-service | catalog_db | 3307 | catalog_user | catalog_pass |
| enrollment-service | enrollment_db | 3308 | enrollment_user | enrollment_pass |

## 常见问题

### Q: 服务启动失败？

A: 检查端口是否被占用，确保 8081/8082/8083 和 3306/3307/3308 端口可用。

### Q: 服务间调用失败？

A: 确保所有服务都已启动，检查 docker logs 查看具体错误。

### Q: 数据库连接失败？

A: 等待数据库健康检查完成，通常需要 10-15 秒。

## 项目结构

```
course-cloud/
├── README.md
├── docker-compose.yml
├── run.sh                # 一键启动脚本
├── test-services.sh
├── user-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── catalog-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
└── enrollment-service/
    ├── src/
    ├── Dockerfile
    └── pom.xml
```

## 作业完成情况

- ✅ 服务拆分：三个独立微服务
- ✅ 独立数据库：每个服务有独立的数据库
- ✅ 服务间通信：使用 RestTemplate 实现 HTTP 调用
- ✅ Docker 容器化：每个服务都有 Dockerfile
- ✅ Docker Compose：一键部署所有服务
- ✅ 测试脚本：完整的功能测试

## 07Nacos服务注册与发现

- **控制台访问**: http://localhost:8848/nacos（默认账号 / 密码：nacos/nacos）

### user-service (用户服务)

- **端口**: 8081（容器内），宿主机动态映射（8084+/8081）

- **数据库**: user_db (3306)

- **功能**: 学生 / 用户管理，注册到 Nacos 支持多实例负载均衡

- 核心配置

  ```yaml
  spring:
    cloud:
      nacos:
        discovery:
          server-addr: nacos:8848
          namespace: dev
          group: DEFAULT_GROUP
          instance-id: ${HOSTNAME} # 保证实例ID唯一
  management:
    endpoints:
      web:
        exposure:
          include: health # 暴露健康检查接口
  ```

- API 端点

  - `GET /api/students/test` - 负载均衡测试接口（返回实例 IP / 端口）

### catalog-service (课程目录服务)

- **端口**: 8082（容器内）
- **数据库**: catalog_db (3307)
- **功能**: 课程管理，注册到 Nacos 支持多实例部署
- **核心配置**: 同 user-service（Nacos 注册 + 健康检查）
- API 端点
  - `GET /api/courses/test` - 负载均衡测试接口

### enrollment-service (选课服务)

- **端口**: 8083（宿主机 / 容器内）

- **数据库**: enrollment_db (3308)

- **功能**: 选课管理，通过 RestTemplate 调用其他服务（支持负载均衡）

- 核心配置

  ```java
  @Bean
  @LoadBalanced // 开启RestTemplate负载均衡
  public RestTemplate restTemplate() {
      return new RestTemplate();
  }
  ```

- API 端点

  - `GET /api/enrollments/test` - 负载均衡 / 故障转移测试接口
  - `GET /actuator/health` - 健康检查接口

## 构建和运行步骤

### 手动构建

#### 1. 构建所有服务

```bash
# 构建 user-service
cd user-service
mvn clean package -DskipTests
cd ..

# 构建 catalog-service
cd catalog-service
mvn clean package -DskipTests
cd ..

# 构建 enrollment-service
cd enrollment-service
mvn clean package -DskipTests
cd ..
```

#### 2. 使用 Docker Compose 部署

```bash
# 启动所有服务（含Nacos）
docker compose up -d --build

# 扩容user-service为3实例（测试负载均衡）
docker compose up -d --scale user-service=3

# 查看服务状态
docker compose ps

# 查看Nacos日志
docker compose logs -f nacos

# 查看微服务日志（以user-service为例）
docker compose logs -f user-service

# 停止所有服务
docker compose down

# 停止并删除数据卷（清理数据库/Nacos数据）
docker compose down -v
```

#### 3. 验证服务

```bash
# 检查Nacos控制台
curl -I http://localhost:8848/nacos/

# 检查user-service注册状态（dev命名空间）
curl -X GET "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=user-service&namespaceId=dev"

# 检查user-service API
curl http://localhost:8084/api/students

# 检查负载均衡效果
for i in {1..10}; do
  curl -s http://localhost:8083/api/enrollments/test | jq '.["user-service"]'
done
```

## Nacos 核心配置说明

### docker-compose.yml 中 Nacos 配置

```yaml
nacos:
  image: nacos/nacos-server:v2.2.3
  container_name: nacos
  environment:
    - MODE=standalone # 单机模式
    - JVM_XMS=256m    # JVM初始内存
    - JVM_XMX=256m    # JVM最大内存
    - JVM_XMN=128m    # 新生代内存
  ports:
    - "8848:8848"  # 核心服务端口
    - "8080:8080"  # 控制台端口
    - "9848:9848"  # 客户端通信端口
  volumes:
    - nacos-data:/home/nacos/data
    - nacos-logs:/home/nacos/logs
  networks:
    - course-network
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8848/nacos/"]
    interval: 20s
    timeout: 15s
    retries: 20
    start_period: 90s # 启动初期不检测（预留初始化时间）
  restart: unless-stopped
```

### 微服务 Nacos 注册配置（application.yml）

```yaml
spring:
  application:
    name: user-service # 服务名（Nacos注册标识）
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848 # Nacos地址（容器内域名）
        namespace: dev          # 命名空间（需提前在Nacos创建）
        group: DEFAULT_GROUP    # 分组
        instance-id: ${HOSTNAME} # 实例ID（容器名，保证唯一）
        heart-beat-interval: 5000    # 心跳间隔5秒
        heart-beat-timeout: 15000    # 心跳超时15秒
        ip-delete-timeout: 30000     # 实例剔除超时30秒
management:
  endpoints:
    web:
      exposure:
        include: health # 暴露健康检查接口
  endpoint:
    health:
      show-details: always # 显示健康检查详细信息
```

## 测试说明

### 1. 基础功能测试

运行基础测试脚本 `test-services.sh`：

```bash
chmod +x test-services.sh
./test-services.sh
```

脚本执行：

1. 创建学生（user-service）
2. 获取所有学生
3. 创建课程（catalog-service）
4. 获取所有课程
5. 学生选课（验证服务间通信）
6. 查询选课记录
7. 异常场景测试（学生 / 课程不存在）

### 2. 负载均衡测试

运行 Nacos 专属测试脚本 `nacos-test.sh`：

```bash
chmod +x nacos-test.sh
./nacos-test.sh
```

脚本执行：

1. 验证 Nacos 服务可用性
2. 检查微服务注册状态（dev 命名空间）
3. 10 次循环调用测试负载均衡（轮询多实例）
4. 模拟实例故障，验证故障转移

### 3. 故障转移测试

```bash
# 1. 查看user-service实例
docker ps | grep user-service

# 2. 停止其中一个实例（模拟故障）
docker stop course-cloud-hw07-user-service-1

# 3. 等待15秒（Nacos剔除不健康实例）
sleep 15

# 4. 验证请求自动避开故障实例
for i in {1..10}; do
  curl -s http://localhost:8083/api/enrollments/test | jq '.["user-service"]'
done

# 5. 恢复故障实例
docker start course-cloud-hw07-user-service-1
```

## 数据库配置

| 服务               | 数据库                   | 宿主机端口 | 容器内端口 | 用户名          | 密码            |
| ------------------ | ------------------------ | ---------- | ---------- | --------------- | --------------- |
| user-service       | user_db                  | 3306       | 3306       | user_user       | user_pass       |
| catalog-service    | catalog_db               | 3307       | 3306       | catalog_user    | catalog_pass    |
| enrollment-service | enrollment_db            | 3308       | 3306       | enrollment_user | enrollment_pass |
| Nacos              | 嵌入式数据库（单机模式） | -          | -          | nacos           | nacos           |

## 08openFeign负载均衡与熔断降级测试

## 一、OpenFeign 配置说明

### 1. 核心依赖（pom.xml）

```xml
<!-- Spring Cloud OpenFeign 核心 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<!-- Resilience4j 熔断器（Feign 熔断依赖） -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### 2. 核心配置（application.yml）

| 配置项                                                       | 说明                 | 取值                           |
| ------------------------------------------------------------ | -------------------- | ------------------------------ |
| `spring.cloud.openfeign.circuitbreaker.enabled`              | 开启 Feign 熔断开关  | `true`                         |
| `feign.client.config.default.connectTimeout`                 | 全局连接超时         | `3000ms`                       |
| `feign.client.config.default.readTimeout`                    | 全局读取超时         | `5000ms`                       |
| `feign.client.config.user-service.loggerLevel`               | 微服务级日志级别     | `FULL`（打印完整请求 / 响应）  |
| `resilience4j.circuitbreaker.instances.user-service.failure-rate-threshold` | 熔断失败率阈值       | `50%`（失败率超 50% 触发熔断） |
| `resilience4j.circuitbreaker.instances.user-service.wait-duration-in-open-state` | 熔断打开状态持续时间 | `10s`（10 秒后进入半开状态）   |

### 3. Feign 客户端配置

#### （1）UserClient 配置

```java
@FeignClient(
    name = "user-service", // 匹配 Nacos 注册的服务名
    fallbackFactory = UserClientFallbackFactory.class // 降级工厂（带异常信息）
)
public interface UserClient {
    // 按学号查询学生（适配微服务接口路径）
    @GetMapping("/api/users/students/studentId/{studentId}")
    Map<String, Object> getStudentByStudentId(@PathVariable("studentId") String studentId);
}
```

#### （2）CatalogClient 配置

```java
@FeignClient(
    name = "catalog-service", // 匹配 Nacos 注册的服务名
    fallbackFactory = CatalogClientFallbackFactory.class // 降级工厂
)
public interface CatalogClient {
    // 查询课程信息（适配微服务接口路径）
    @GetMapping("/api/courses/{id}")
    Map<String, Object> getCourse(@PathVariable("id") String id);
}
```

### 4. 降级工厂核心逻辑

- 实现 `FallbackFactory` 接口，捕获服务调用异常并返回标准化降级响应；
- 降级响应包含 `status: ERROR` 标识，便于业务层识别降级触发；
- 补充默认 `data` 字段（如课程容量），避免业务代码空指针。

## 二、负载均衡测试结果

### 1. 测试环境

- User Service 实例数：3 个（端口 8081，Nacos 注册健康实例数 3）；
- Catalog Service 实例数：3 个（端口 8082，Nacos 注册健康实例数 3）；
- 测试工具：curl 批量发送选课请求。

### 2. 测试步骤

```bash
# 循环发送10次选课请求（学生ID 20249811~20249820）
for i in {11..20}; do
  curl -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d "{
    \"courseId\": \"3d37d0bdd2a811f094f4aa5b30e31250\",
    \"studentId\": \"202498$i\"
  }"
done
```

### 3. 测试结果

| 微服务          | 实例路由分布                                                 | 负载均衡策略                       | 结果说明                              |
| --------------- | ------------------------------------------------------------ | ---------------------------------- | ------------------------------------- |
| User Service    | e8267d9025ce:8081（4 次）、62b2181845b3:8081（3 次）、5f056cae2dc5:8081（3 次） | Spring Cloud LoadBalancer 轮询策略 | 请求均匀分发到 3 个实例，负载均衡生效 |
| Catalog Service | 9d78f2e14c89:8082（3 次）、8e67d1c03b78:8082（4 次）、7c56b0a92d67:8082（3 次） | Spring Cloud LoadBalancer 轮询策略 | 请求均匀分发到 3 个实例，负载均衡生效 |

### 4. 关键日志示例

```plaintext
2025-12-08T15:24:17.673Z INFO 1 --- [enrollment-service] [nio-8083-exec-6] c.z.c.e.service.EnrollmentService       : 选课请求 | studentId: 20249811 | 路由到User Service实例: e8267d9025ce:8081
2025-12-08T15:24:27.987Z INFO 1 --- [enrollment-service] [nio-8083-exec-7] c.z.c.e.service.EnrollmentService       : 选课请求 | studentId: 20249812 | 路由到User Service实例: 62b2181845b3:8081
```

## 三、熔断降级测试结果

### 1. 测试场景 1：停止所有 User Service 实例

#### 测试步骤

```bash
# 停止所有 User Service 实例
docker stop user-service-1 user-service-2 user-service-3
# 发送选课请求
curl -X POST http://localhost:8083/api/enrollments \
-H "Content-Type: application/json" \
-d '{
  "courseId": "3d37d0bdd2a811f094f4aa5b30e31250",
  "studentId": "20249811"
}'
```

#### 测试结果

- 响应状态码：503 Service Unavailable；

- 响应内容：

  ```json
  {
    "path":"/api/enrollments",
    "error":"Service Unavailable",
    "message":"用户服务暂时不可用: 用户服务暂时不可用: Load balancer does not contain an instance for the service user-service",
    "timestamp":"2025-12-08T14:18:16.444802260",
    "status":503
  }
  ```

- 日志验证：

  ```plaintext
  2025-12-08T14:18:16.441Z ERROR 1 --- [enrollment-service] [nio-8083-exec-6] c.z.c.e.client.UserClientFallbackFactory : 🔥 UserClientFallbackFactory 被调用！
  2025-12-08T14:18:16.442Z ERROR 1 --- [enrollment-service] [nio-8083-exec-6] c.z.c.e.client.UserClientFallbackFactory : ✅✅✅ UserClient Fallback 触发！studentId: 20249811
  2025-12-08T14:18:16.443Z ERROR 1 --- [enrollment-service] [nio-8083-exec-6] c.z.c.e.service.EnrollmentService       : ✅ 用户服务降级触发: 用户服务暂时不可用: Load balancer does not contain an instance for the service user-service
  ```

- 结论：熔断降级触发，返回标准化 503 响应，降级逻辑生效。

### 2. 测试场景 2：重启 User Service 实例（学生 ID 不存在）

#### 测试步骤

```bash
# 重启所有 User Service 实例
docker start user-service-1 user-service-2 user-service-3
# 发送选课请求（学生ID不存在）
curl -X POST http://localhost:8083/api/enrollments \
-H "Content-Type: application/json" \
-d '{
  "courseId": "3d37d0bdd2a811f094f4aa5b30e31250",
  "studentId": "20249812"
}'
```

#### 测试结果

- 响应状态码：503 Service Unavailable；

- 响应内容：

  ```json
  {"id":"1e299b67-efb0-42eb-b48c-1a296de66064",
   "courseId":"3d37d0bdd2a811f094f4aa5b30e31250",
   "studentId":"20249812",
   "enrolledAt":"2025-12-08T16:05:52.319718016"}
  ```

  

- 结论：User Service 正常运行

## 四、OpenFeign vs RestTemplate 对比分析

| 维度        | OpenFeign                                                    | RestTemplate                                                 |
| ----------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 开发效率    | 声明式 API，通过注解定义接口，无需手动拼接 URL / 参数，代码简洁 | 编程式调用，需手动拼接 URL、设置请求头 / 参数，代码冗余      |
| 可读性      | 接口与微服务 API 一一对应，语义清晰，易维护                  | URL 硬编码在代码中，参数拼接复杂，可读性差                   |
| 负载均衡    | 自动集成 Spring Cloud LoadBalancer，无需额外配置             | 需手动结合 `@LoadBalanced` 注解，通过服务名调用              |
| 熔断降级    | 支持 `fallbackFactory` 优雅降级，可捕获异常信息              | 需手动结合 Resilience4j 注解，代码侵入性高                   |
| 日志 / 监控 | 内置日志级别配置（NONE/BASIC/HEADERS/FULL），便于调试        | 需手动打印日志，监控成本高                                   |
| 扩展性      | 支持拦截器、编码器 / 解码器自定义，扩展性强                  | 自定义需封装工具类，扩展性弱                                 |
| 学习成本    | 低（基于注解，符合 RESTful 风格）                            | 中（需熟悉 HTTP 调用细节）                                   |
| 适用场景    | 微服务间常规调用，追求开发效率和可维护性                     | 特殊场景（如复杂 HTTP 请求构造），或低版本 Spring Cloud 项目 |

### 核心结论

OpenFeign 更适合微服务架构下的服务调用场景，大幅降低开发成本，提升代码可维护性；RestTemplate 适合需要精细化控制 HTTP 请求的特殊场景，或对框架侵入性要求低的场景。

## 五、构建与运行步骤

### 1. 环境准备

- 虚拟机环境：Ubuntu 20.04 + Docker + Docker Compose；
- 依赖服务：Nacos（服务注册）、MySQL（各微服务数据库）。

### 2. 代码构建

```bash
# 进入项目目录
cd ~/桌面/course-cloud-hw08

mvn clean package -DskipTests
# 构建 Docker 镜像
docker-compose build --no-cache enrollment-service
```

### 3. 服务启动

```bash
# 启动所有依赖服务（Nacos/MySQL/用户/课程服务）
docker compose up -d 

# 查看服务状态
docker-compose ps
```

### 4. 验证服务

```bash
# 检查 Nacos 服务注册
curl http://localhost:8848/nacos/v1/ns/catalog/instances?serviceName=user-service&groupName=COURSEHUB_GROUP&namespaceId=dev
# 发送选课请求
curl -X POST http://localhost:8083/api/enrollments \
-H "Content-Type: application/json" \
-d '{
  "courseId": "3d37d0bdd2a811f094f4aa5b30e31250",
  "studentId": "20249811"
}'
```

## 六、熔断机制实现方式

### 1. 核心依赖

通过 `spring-cloud-starter-circuitbreaker-resilience4j` 集成 Resilience4j 熔断器，替代传统 Hystrix。

### 2. 核心配置

```yaml
# 开启 Feign 熔断
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
# Resilience4j 熔断器规则
resilience4j:
  circuitbreaker:
    instances:
      user-service:
        failure-rate-threshold: 50 # 失败率超50%触发熔断
        wait-duration-in-open-state: 10s # 熔断打开10秒后尝试恢复
        minimum-number-of-calls: 5 # 最少5次调用才计算失败率
        record-exceptions: # 触发熔断的异常类型
          - feign.FeignException
          - java.lang.Exception
```

### 3. 代码实现

#### （1）降级工厂（FallbackFactory）

- 实现 `FallbackFactory` 接口，捕获服务调用异常；
- 构建标准化降级响应（包含 `status: ERROR` 标识）；
- 记录降级日志，便于问题排查。

#### （2）业务层识别降级

```java
// 调用 Feign 客户端
Map<String, Object> studentResponse = userClient.getStudentByStudentId(studentId);
// 识别降级响应
if ("ERROR".equals(studentResponse.get("status"))) {
    String errorMsg = (String) studentResponse.get("message");
    throw new ServiceUnavailableException("用户服务暂时不可用: " + errorMsg);
}
```

#### （3）全局异常处理

```java
@ExceptionHandler(ServiceUnavailableException.class)
public ResponseEntity<Map<String, Object>> handleServiceUnavailableException(ServiceUnavailableException e) {
    Map<String, Object> response = buildErrorResponse(
        HttpStatus.SERVICE_UNAVAILABLE,
        e.getMessage(),
        request.getDescription(false)
    );
    return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
}
```

### 4. 熔断状态流转

- **关闭状态（CLOSED）**：服务正常，请求正常转发，统计失败率；
- **打开状态（OPEN）**：失败率超阈值，触发熔断，直接调用降级方法；
- **半开状态（HALF_OPEN）**：熔断打开 10 秒后，允许少量请求尝试调用服务，若成功则恢复关闭状态，否则继续打开。



## 09 API 网关与统一认证

### 模块概述

**版本**: 09（重大架构升级）
**核心功能**:
- 基于 Spring Cloud Gateway 实现统一入口，集中管理路由与认证
- JWT 认证机制：用户登录获取 Token，Gateway 验证 Token 并注入用户信息到请求头
- 后端服务无需关心认证逻辑，仅读取 Gateway 传递的用户信息（`X-User-Id`、`X-Username`、`X-User-Role`）

**架构变更**:
```
客户端
  ↓
Gateway (9000:8090) - JWT 认证、路由转发
  ↓
  ├─→ user-service (8081)    - 登录、注册、用户管理
  ├─→ catalog-service (8082) - 课程管理
  └─→ enrollment-service (8083) - 选课管理
```

---

### Gateway 路由配置说明

#### 1. 核心配置（application.yml）

```yaml
server:
  port: 8090  # Gateway 内部端口，Docker 映射为 9000:8090

spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848
        namespace: dev
        group: COURSEHUB_GROUP
    gateway:
      discovery:
        locator:
          enabled: true  # 启用自动路��发现
          lower-case-service-id: true
      routes:
        # 用户服务路由
        - id: user-service
          uri: lb://user-service  # lb = Load Balancer
          predicates:
            - Path=/api/users/**,/api/auth/**
          filters:
            - StripPrefix=1  # 去掉 /api 前缀

        # 课程服务路由
        - id: catalog-service
          uri: lb://catalog-service
          predicates:
            - Path=/api/courses/**
          filters:
            - StripPrefix=1

        # 选课服务路由
        - id: enrollment-service
          uri: lb://enrollment-service
          predicates:
            - Path=/api/enrollments/**
          filters:
            - StripPrefix=1

      # CORS 跨域配置
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: [GET, POST, PUT, DELETE, PATCH]
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600

# JWT 配置（需与 user-service 保持一致）
jwt:
  secret: "MySecretKeyForJWT2024CourseCloudSystemVeryLongAndSecure1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"  # ≥512 bits（HS512 要求）
  expiration: 86400000  # 24 小时
```

#### 2. 路由转发示例

| 客户端请求                         | Gateway 转发路径         | 目标服务          |
| ---------------------------------- | ------------------------ | ----------------- |
| `POST /api/auth/login`             | `POST /auth/login`       | user-service:8081 |
| `POST /api/auth/register/student`  | `POST /auth/register/student` | user-service:8081 |
| `POST /api/courses`                | `POST /courses`          | catalog-service:8082 |
| `POST /api/enrollments`            | `POST /enrollments`      | enrollment-service:8083 |

**StripPrefix=1** 说明：Gateway 去掉路径中的第一个前缀（`/api`），转发给后端服务。例如 `/api/courses` → `/courses`。

---

### JWT 认证流程说明

#### 1. 认证流程图

```
1. 用户注册/登录
   客户端 → Gateway → user-service
   ↓
   user-service 生成 JWT Token（包含 userId、username、role）
   ↓
   返回 Token 给客户端

2. 访问受保护接口
   客户端携带 Token → Gateway JWT 过滤器
   ↓
   验证 Token 有效性（签名、过期时间）
   ↓
   解析 Token，提取用户信息
   ↓
   注入请求头：X-User-Id、X-Username、X-User-Role
   ↓
   转发到后端服务（user-service/catalog-service/enrollment-service）
   ↓
   后端服务读取请求头，无需验证 Token
```

#### 2. JWT 过滤器核心逻辑（Gateway）

**文件**: `gateway-service/src/main/java/.../filter/JwtAuthenticationFilter.java`

```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    // 白名单：不需要认证的路径
    private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register/student",
        "/api/auth/register/teacher"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 1. 白名单路径直接放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取 Authorization 请求头
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange.getResponse());  // 返回 401
        }

        // 3. 验证 Token
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return unauthorized(exchange.getResponse());
        }

        // 4. 解析 Token，提取用户信息
        Claims claims = jwtUtil.parseToken(token);
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        // 5. 注入请求头
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
            .header("X-User-Id", userId)
            .header("X-Username", username)
            .header("X-User-Role", role)
            .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;  // 优先级最高
    }
}
```

#### 3. JWT 工具类（JwtUtil）

**文件**: `gateway-service/src/main/java/.../util/JwtUtil.java`

```java
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    // 生成 Token（user-service 调用）
    public String generateToken(String userId, String username, String role) {
        return Jwts.builder()
            .setSubject(userId)  // 用户 ID
            .claim("username", username)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS512)
            .compact();
    }

    // 验证 Token（Gateway 调用）
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 解析 Token（Gateway 调用）
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
```

#### 4. 用户登录接口（user-service）

**文件**: `user-service/src/main/java/.../controller/AuthController.java`

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        // 1. 查询用户
        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "用户名或密码错误"));
        }

        // 2. 验证密码（BCrypt）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "用户名或密码错误"));
        }

        // 3. 生成 JWT Token
        String token = jwtUtil.generateToken(
            user.getId(),
            user.getUsername(),
            user.getUserType().name()  // STUDENT / TEACHER
        );

        // 4. 返回 Token 和用户信息
        LoginResponse loginResponse = new LoginResponse(token, user);
        return ResponseEntity.ok(ApiResponse.success(200, "登录成功", loginResponse));
    }
}
```

---

### 测试结果展示

#### 1. 学生注册

```bash
curl -X POST http://localhost:9000/api/auth/register/student \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newtest",
    "password": "pass123",
    "email": "new@test.com",
    "studentId": "9999",
    "name": "测试学生",
    "major": "计算机科学",
    "grade": 2024
  }'
```

**响应** (201 Created):
```json
{
  "code": 201,
  "message": "创建成功",
  "data": {
    "id": "84e5fcb7-7a60-4453-8ca7-4b7e7f18d6fb",
    "username": "newtest",
    "email": "new@test.com",
    "studentId": "9999",
    "name": "测试学生",
    "major": "计算机科学",
    "grade": 2024,
    "createdAt": "2025-12-10T14:17:44.793833399"
  }
}
```

#### 2. 用户登录（获取 Token）

```bash
curl -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newtest",
    "password": "pass123"
  }'
```

**响应** (200 OK):
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI4NGU1ZmNiNy03YTYwLTQ0NTMtOGNhNy00YjdlN2YxOGQ2ZmIiLCJ1c2VybmFtZSI6Im5ld3Rlc3QiLCJyb2xlIjoiU1RVREVOVCIsImlhdCI6MTc2NTM3NzQ4MCwiZXhwIjoxNzY1NDYzODgwfQ.Dqf_cbBWTEPzMs5sCtGo3WlYMVoWGsP294eQMdX1W--j8lVDSHgp_BDTTVdp6nc2UjO0j-rfQ-JGIYHf6cA_lg",
    "userInfo": {
      "id": "84e5fcb7-7a60-4453-8ca7-4b7e7f18d6fb",
      "username": "newtest",
      "email": "new@test.com",
      "role": "STUDENT"
    }
  }
}
```

**Token 内容**（Base64 解码后）:
```json
{
  "sub": "84e5fcb7-7a60-4453-8ca7-4b7e7f18d6fb",  // userId
  "username": "newtest",
  "role": "STUDENT",
  "iat": 1765377480,  // 签发时间
  "exp": 1765463880   // 过期时间（24小时后）
}
```

#### 3. Token 验证（访问受保护接口）

```bash
curl -X GET http://localhost:9000/api/auth/verify \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI4NGU1ZmNiNy03YTYwLTQ0NTMtOGNhNy00YjdlN2YxOGQ2ZmIiLCJ1c2VybmFtZSI6Im5ld3Rlc3QiLCJyb2xlIjoiU1RVREVOVCIsImlhdCI6MTc2NTM3NzQ4MCwiZXhwIjoxNzY1NDYzODgwfQ.Dqf_cbBWTEPzMs5sCtGo3WlYMVoWGsP294eQMdX1W--j8lVDSHgp_BDTTVdp6nc2UjO0j-rfQ-JGIYHf6cA_lg"
```

**响应** (200 OK):
```json
{
  "code": 200,
  "message": "验证成功",
  "data": "Token有效！用户ID: 84e5fcb7-7a60-4453-8ca7-4b7e7f18d6fb, 用户名: newtest, 角色: STUDENT"
}
```

**后端日志验证**（user-service）:
```
2025-12-10T14:22:10.150Z INFO [user-service] Token验证请求：userId=84e5fcb7-7a60-4453-8ca7-4b7e7f18d6fb, username=newtest, role=STUDENT
```

#### 4. 未认证访问（401 测试）

```bash
curl -i -X POST http://localhost:9000/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"courseId":"test-id","studentId":"9999"}'
```

**响应** (401 Unauthorized):
```
HTTP/1.1 401 Unauthorized
content-length: 0
```

**说明**: Gateway JWT 过滤器拦截未携带 Token 的请求，直接返回 401，不转发到后端服务。

#### 5. 完整选课流程测试

**步骤 1: 创建课程**
```bash
curl -X POST http://localhost:9000/api/courses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "code": "CSC101",
    "title": "计算机科学导论",
    "instructorId": "T001",
    "instructorName": "张教授",
    "instructorEmail": "zhang@example.com",
    "dayOfWeek": "MONDAY",
    "start": "09:00:00",
    "end": "10:30:00",
    "expectedAttendance": 50,
    "capacity": 60
  }'
```

**响应** (201 Created):
```json
{
  "hostname": "1a9a8f0cf332",
  "data": {
    "id": "4e107813-de29-444a-84a5-bc550e76b328",
    "code": "CSC101",
    "title": "计算机科学导论",
    "instructorName": "张教授",
    "instructorEmail": "zhang@example.com",
    "dayOfWeek": "MONDAY",
    "start": "09:00",
    "end": "10:30",
    "capacity": 60,
    "expectedAttendance": 50
  },
  "port": "8082",
  "status": "SUCCESS"
}
```

**步骤 2: 学生选课**
```bash
curl -i -X POST http://localhost:9000/api/enrollments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "courseId": "4e107813-de29-444a-84a5-bc550e76b328",
    "studentId": "9999"
  }'
```

**响应** (201 Created):
```
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "3f2a288c-15ce-4af0-bc81-2ea55feb7467",
  "courseId": "4e107813-de29-444a-84a5-bc550e76b328",
  "studentId": "9999",
  "enrolledAt": "2025-12-10T16:26:26.882057090"
}
```

**后端日志验证**（enrollment-service）:
```
2025-12-10T16:26:26.757Z INFO [enrollment-service] 学生选课请求成功（网关版）: userId=84e5fcb7-7a60-4453-8ca7-4b7e7f18d6fb, username=newtest, courseId=4e107813-de29-444a-84a5-bc550e76b328, studentId=9999
```

**✅ 验证成功**: 后端服务成功接收到 Gateway 注入的用户信息请求头（`X-User-Id`、`X-Username`、`X-User-Role`）。

---

### 部署与测试

#### 1. 构建与启动

```bash
# 1. 构建所有服务
cd user-service && mvn clean package -DskipTests && cd ..
cd catalog-service && mvn clean package -DskipTests && cd ..
cd enrollment-service && mvn clean package -DskipTests && cd ..
cd gateway-service && mvn clean package -DskipTests && cd ..

# 2. 启动所有服务（含 Gateway）
docker compose up -d --build

# 3. 验证服务状态
docker compose ps
```

#### 2. 关键配置检查点

| 检查项                     | 说明                                                         |
| -------------------------- | ------------------------------------------------------------ |
| Gateway 端口映射           | `9000:8090`（宿主机 9000 → 容器 8090）                       |
| JWT secret 一致性          | Gateway 和 user-service 的 `jwt.secret` **必须完全一致**     |
| Controller 路径配置        | 后端 Controller `@RequestMapping` 应为 `/auth`、`/courses`、`/enrollments`（不含 `/api`） |
| Feign Client 路径配置      | 服务间调用路径应为 `/courses/{id}`（不含 `/api`）            |
| Nacos 服务注册             | 所有服务（含 Gateway）注册到 dev 命名空间，COURSEHUB_GROUP 分组 |

#### 3. 常见问题排查

| 问题                          | 原因                                      | 解决方案                                                     |
| ----------------------------- | ----------------------------------------- | ------------------------------------------------------------ |
| 登录返回 500（WeakKeyException） | JWT secret 长度不足 512 bits（64 字节）   | 修改 `jwt.secret` 为至少 64 字节的字符串                     |
| 登录成功但验证返回 401        | Gateway 和 user-service 的 secret 不一致  | 确保两个服务的 `jwt.secret` 完全相同                         |
| 选课返回 404                  | Controller 路径配置错误（含 `/api` 前缀） | 修改 Controller `@RequestMapping` 为 `/enrollments`（去掉 `/api`） |
| Feign 调用返回 404            | Feign Client 路径与 Controller 不匹配    | 修改 Feign `@GetMapping` 为 `/courses/{id}`（去掉 `/api`）  |
| Gateway 无响应                | Gateway 容器未启动或端口冲突              | 检查 `docker compose ps`，确保 Gateway 容器 healthy          |

---

### 核心技术栈

- **Spring Cloud Gateway**: 3.1.x（基于 Reactor 的异步网关）
- **JWT**: jjwt 0.11.5（HS512 签名算法）
- **Spring Security**: BCryptPasswordEncoder（密码加密）
- **Nacos**: 2.2.3（服务注册与发现）

---

## 许可证

MIT License

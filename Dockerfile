# 构建阶段：使用 Gradle 8.14+ 构建 JAR（Spring Boot 4.0.1 需要）
FROM gradle:8.14-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

# 运行阶段：使用轻量 JRE 镜像
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 复制构建产物
COPY --from=builder /app/build/libs/*.jar app.jar

# 复制字体文件（如果有的话）
COPY --from=builder /app/src/main/resources/fonts /app/fonts 2>/dev/null || true

# 设置时区为上海
ENV TZ=Asia/Shanghai

# 暴露端口（Render 会通过 PORT 环境变量指定）
EXPOSE 8081

# 启动命令
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]

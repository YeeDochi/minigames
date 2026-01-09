# 빌드 시점에 전달받을 변수 선언
ARG MODULE_PATH
ARG MODULE_NAME

# 1단계: 빌더 (전체 프로젝트를 복사하여 지정된 모듈을 빌드)
FROM gradle:8.5-jdk17-alpine AS builder

# 이 단계에서 사용할 변수들을 다시 선언
ARG MODULE_PATH
ARG MODULE_NAME

WORKDIR /app

# 빌드에 필요한 파일들만 먼저 복사하여 Docker 캐시를 활용
COPY settings.gradle.kts gradlew ./
COPY gradle gradle
# 전체 소스 코드를 복사
COPY . .

# 해당 모듈 디렉터리로 이동하여, 루트의 gradlew로 bootJar 태스크를 실행
RUN cd ${MODULE_PATH} && /app/gradlew bootJar --no-daemon

# 2단계: 런타임 (빌드된 JAR 파일만 복사하여 가벼운 최종 이미지 생성)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 빌더 스테이지에서 빌드된 지정된 모듈의 JAR 파일을 복사
# 예: COPY --from=builder /app/member/build/libs/*.jar app.jar
COPY --from=builder /app/${MODULE_PATH}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

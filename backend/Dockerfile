# The build context is the repository root, because the build reads contracts/openapi.yaml.
FROM eclipse-temurin:21.0.13_9-jdk-alpine AS build
WORKDIR /workspace/backend
COPY contracts/openapi.yaml ../contracts/openapi.yaml
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
COPY backend/src src
# CI runs the tests. Run through sh, so it works when the context comes from Windows without +x.
RUN --mount=type=cache,target=/root/.m2 sh mvnw -B -ntp package -Dmaven.test.skip=true

FROM eclipse-temurin:21.0.13_9-jre-alpine
RUN addgroup -S wolfandfox && adduser -S -G wolfandfox wolfandfox
WORKDIR /app
COPY --from=build /workspace/backend/target/wolfandfox-backend-*.jar app.jar
USER wolfandfox
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

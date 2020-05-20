FROM tmaretdotio/openjdk-15-loom:7-141

ARG projectVersion

ENV PROJECT_VERSION projectVersion

COPY "target/fibers-server-${projectVersion}-jar-with-dependencies.jar" /app/fibers-server.jar

WORKDIR /app
ENTRYPOINT ["java","-jar","fibers-server.jar"]

EXPOSE 8080
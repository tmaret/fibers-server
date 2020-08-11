FROM ubuntu:latest

ARG projectVersion
ENV PROJECT_VERSION projectVersion

ENV JAVA_VERSION 16-loom+4
ENV JAVA_URL https://download.java.net/java/early_access/loom/4/openjdk-16-loom+4-56_linux-x64_bin.tar.gz
ENV JAVA_HOME /usr/java/$JAVA_VERSION
ENV PATH $JAVA_HOME/bin:$PATH

RUN apt update -y
RUN apt install -y curl tar

WORKDIR $JAVA_HOME
RUN curl -sL $JAVA_URL | tar -xzf - --strip-components=1

COPY "target/fibers-server-${projectVersion}-jar-with-dependencies.jar" /app/fibers-server.jar

WORKDIR /app
ENTRYPOINT ["java","-jar","fibers-server.jar"]

EXPOSE 8080
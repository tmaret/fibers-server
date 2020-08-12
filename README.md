# fibers-server

`fibers-server` is an evaluation server based on [Jetty](https://www.eclipse.org/jetty) for lightweight threads (fibers) provided by [Project Loom](https://wiki.openjdk.java.net/display/loom/Main)
Early-Access [builds](https://jdk.java.net/loom/). The server provides a sync and an async servlet handlers that can be configured to serve requests including more or less CPU, IO and sleep.
For comparison, the server can be started with a thread factory backed by kernel or lightweight threads (fibers).


# Setup

Setup Project Loom early-access build by following the [instructions](https://jdk.java.net/loom/).

```
$ java -version
openjdk version "16-loom" 2021-03-16
OpenJDK Runtime Environment (build 16-loom+4-56)
OpenJDK 64-Bit Server VM (build 16-loom+4-56, mixed mode, sharing)
```

# Build

```
mvn clean:install
```

# Run

Print help usage via

```
docker run -p 8080:8080 -i tmaretdotio/fibers-server:0.0.2 -h
```

Run a server using kernel threads and unbounded pool

```
docker run -p 8080:8080 -i tmaretdotio/fibers-server:0.0.2 -t kernel
```

Run the server using lightweight threads (fibers) and unbounded pool

```
docker run -p 8080:8080 -i tmaretdotio/fibers-server:0.0.2 -t fibers
```

Run the server with bounded pool (400) and lightweight threads

```
docker run -p 8080:8080 -i tmaretdotio/fibers-server:0.0.2 -t fibers -c 400
```

# Test

Request served by a sync servlet

```
curl http://localhost:8080/sync
```

Request served by an async servlet

```
curl http://localhost:8080/async
```

Specify the amount of work performed by the servlet before returning the response.

```
curl 'http://localhost:8080/sync?cpuIterations=10000&idleDelay=0&fileLength=100000'
```

| Request Parameter | Definition |
| :---------------: | ---------- |
| `cpuIterations`   | The number of SHA256 iterations on a 256 byte array. Default is 10'000 iterations and corresponds to ~ 3ms on a 2.3 GHz Intel Core i9. |
| `idleDelay`       | The time to sleep in ms. Default is 0ms. |
| `fileLength`      | The length in bytes of the file returned. The file is auto-generated and served from disk. Default length is 100KB.|

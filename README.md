# fibers-server

`fibers-server` aims at evaluating a threaded server implementation 
backed by user-mode threads (fibers) provided by [Project Loom](https://wiki.openjdk.java.net/display/loom/Main) 
Early-Access [builds](https://jdk.java.net/loom/).

# Setup

Setup Project Loom early-access build by following the [instructions](https://jdk.java.net/loom/).

```
$ java -version
openjdk version "15-loom" 2020-09-15
OpenJDK Runtime Environment (build 15-loom+4-55)
OpenJDK 64-Bit Server VM (build 15-loom+4-55, mixed mode, sharing)
```

# Build

```
mvn clean:install
```

# Run

Print help usage via

```
docker run -p 8080:8080 -i tmaretdotio/fibers-server:0.0.1-SNAPSHOT -h
```

Run a server using kernel threads with

```
docker run -p 8080:8080 -i tmaretdotio/fibers-server:0.0.1-SNAPSHOT -k kernel
```

Run the server using fibers with

```
docker run -p 8080:8080 -i tmaretdotio/fibers-server:0.0.1-SNAPSHOT -k fibers
```

Send request to be served by a sync servlet

```
curl http://localhost:8080/sync
``` 

Send request to be served by an async servlet

```
curl http://localhost:8080/async
```

Send a request with the specific `cpuIterations`, `idleDelay` and `fileLength`

```
curl 'http://localhost:8080/sync?cpuIterations=10000&idleDelay=0&fileLength=100000'
```

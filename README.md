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
java -jar target/server-0.0.1-SNAPSHOT-jar-with-dependencies.jar -h
```

Run a server using kernel threads with

```
java -jar target/server-0.0.1-SNAPSHOT-jar-with-dependencies.jar -k kernel
```

Run the server using fibers with

```
java -jar target/server-0.0.1-SNAPSHOT-jar-with-dependencies.jar -k fibers
```


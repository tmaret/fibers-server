/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.tmaret.server;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tmaret.server.servlets.AsyncServlet;
import io.tmaret.server.servlets.SyncServlet;
import io.tmaret.server.work.Cpu;
import io.tmaret.server.work.Idle;
import io.tmaret.server.work.Io;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.builder;
import static java.util.concurrent.Executors.defaultThreadFactory;
import static java.util.concurrent.Executors.newThreadExecutor;
import static java.util.concurrent.TimeUnit.HOURS;

public class ServerStarter implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(ServerStarter.class);

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @Option(names = {"-p", "--port"}, defaultValue = "${env:SERVER_PORT:-8080}", description = "The server port to listen for connections (default: ${DEFAULT-VALUE})")
    private int port = 8080;

    @Option(names = {"-t", "--thread-factory"}, defaultValue = "${env:SERVER_THREAD_FACTORY:-kernel}", description = "Thread factory implementation in: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    private ThreadSupport threadSupport;

    @Option(names = {"-c", "--thread-pool-capacity"}, defaultValue = "${env:SERVER_THREAD_FACTORY:--1}", description = "The thread pool max capacity or -1 for unbounded capacity (default: ${DEFAULT-VALUE})")
    private int poolCapacity = -1;

    private enum ThreadSupport {
        kernel,
        fibers
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ServerStarter()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        Io io = new Io();
        Cpu cpu = new Cpu();
        Idle idle = new Idle();

        ThreadFactory threadFactory = (threadSupport == ThreadSupport.fibers) ? builder().virtual().factory() : defaultThreadFactory();
        ExecutorService executor = newThreadExecutor(threadFactory);
        ThreadPool threadPool = (poolCapacity < 0) ? unboundedThreadPool(executor) : queuedThreadPool(poolCapacity, threadFactory);

        Server server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        ServletHolder asyncHolder = new ServletHolder(new AsyncServlet(cpu, io, idle));
        asyncHolder.setAsyncSupported(true);
        handler.addServletWithMapping(asyncHolder, "/async");

        ServletHolder syncHolder = new ServletHolder(new SyncServlet(cpu, io, idle));
        handler.addServletWithMapping(syncHolder, "/sync");

        getRuntime().addShutdownHook(new Thread(() -> stop(server)));

        LOG.info("Start server on port {} with thread support: {}, pool capacity: {}" ,
                port, threadSupport, poolCapacity);

        server.start();
        server.join();

        return 0;
    }

    private ThreadPool queuedThreadPool(int capacity, ThreadFactory threadFactory) {
        return new QueuedThreadPool(capacity, 0, 60000, -1, null, null, threadFactory);
    }

    private ThreadPool unboundedThreadPool(ExecutorService executor) {

        return new ThreadPool() {
            @Override
            public void join() throws InterruptedException {
                executor.awaitTermination(Long.MAX_VALUE, HOURS);
            }

            @Override
            public int getThreads() {
                return MAX_VALUE;
            }

            @Override
            public int getIdleThreads() {
                return MAX_VALUE;
            }

            @Override
            public boolean isLowOnThreads() {
                return false;
            }

            @Override
            public void execute(Runnable command) {
                executor.execute(command);
            }
        };
    }

    private static void stop(Server server) {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

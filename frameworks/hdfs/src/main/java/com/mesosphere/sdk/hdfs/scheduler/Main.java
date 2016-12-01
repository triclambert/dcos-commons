package com.mesosphere.sdk.hdfs.scheduler;

import org.apache.mesos.specification.*;

import java.io.File;

/**
 * Hello World Service.
 */
public class Main {
    private static final Integer COUNT = Integer.valueOf(System.getenv("COUNT"));
    private static final Double CPUS = Double.valueOf(System.getenv("CPUS"));

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            new DefaultService(new File(args[0]));
        } else {
            new DefaultService(DefaultServiceSpec.newBuilder()
                    .name("hdfs")
                    .principal("hdfs-principal")
                    .zookeeperConnection("master.mesos:2181")
                    .apiPort(8080)
                    .addPod(DefaultPodSpec.newBuilder()
                            .count(COUNT)
                            .addTask(DefaultTaskSpec.newBuilder()
                                    .name("hdfs")
                                    .goalState(TaskSpec.GoalState.RUNNING)
                                    .commandSpec(DefaultCommandSpec.newBuilder()
                                            .value("echo hdfs >> hdfs-container-path/output && sleep 1000")
                                            .build())
                                    .resourceSet(DefaultResourceSet
                                            .newBuilder("hdfs-role", "hdfs-principal")
                                            .id("hdfs-resources")
                                            .cpus(CPUS)
                                            .memory(256.0)
                                            .addVolume("ROOT", 5000.0, "hdfs-container-path")
                                            .build()).build()).build()).build());
        }
    }
}

package com.mesosphere.sdk.hdfs.scheduler;

import org.apache.mesos.specification.*;

import java.io.File;

/**
 * Hello World Service.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            new DefaultService(new File(args[0]));
        }
    }
}

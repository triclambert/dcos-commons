package org.apache.mesos.scheduler.plan;

import org.apache.mesos.specification.PodInstance;

import java.util.Collection;

/**
 * Created by gabriel on 12/2/16.
 */
public class PodInstanceRequirement {
    private final PodInstance podInstance;
    private final Collection<String> tasksToLaunch;

    public PodInstanceRequirement(PodInstance podInstance, Collection<String> tasksToLaunch) {
        this.podInstance = podInstance;
        this.tasksToLaunch = tasksToLaunch;
    }

    public PodInstance getPodInstance() {
        return podInstance;
    }

    public Collection<String> getTasksToLaunch() {
        return tasksToLaunch;
    }
}

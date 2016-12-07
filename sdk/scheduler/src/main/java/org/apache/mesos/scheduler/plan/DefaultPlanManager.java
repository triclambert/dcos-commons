package org.apache.mesos.scheduler.plan;

import org.apache.mesos.Protos;
import org.apache.mesos.scheduler.ChainedObserver;

import java.util.*;

/**
 * Provides the default implementation of a {@link PlanManager}.
 * Encapsulates the plan and a strategy for executing that plan.
 */
public class DefaultPlanManager extends ChainedObserver implements PlanManager {
    private final Plan plan;

    public DefaultPlanManager(final Plan plan) {
        this.plan = plan;
        this.plan.subscribe(this);
    }

    @Override
    public Plan getPlan() {
        return plan;
    }

    @Override
    public Collection<? extends Step> getCandidates(Collection<String> dirtyAssets) {
        return PlanUtils.getCandidates(plan, dirtyAssets);
    }

    @Override
    public void update(Protos.TaskStatus status) {
        plan.update(status);
    }

    @Override
    public Set<String> getDirtyAssets() {
        Set<String> dirtyAssets = new HashSet<>();
        final List<? extends Phase> phases = plan.getChildren();
        for (Phase phase : phases) {
            final List<? extends Step> steps = phase.getChildren();
            for (Step step : steps) {
                if (step.isPrepared() || step.isStarting()) {
                    Optional<String> dirtyAsset = step.getDirtyAsset();
                    if (dirtyAsset.isPresent()) {
                        dirtyAssets.add(dirtyAsset.get());
                    }
                }
            }
        }
        return dirtyAssets;
    }
}

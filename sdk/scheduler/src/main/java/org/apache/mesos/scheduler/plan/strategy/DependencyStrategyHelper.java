package org.apache.mesos.scheduler.plan.strategy;

import org.apache.mesos.scheduler.plan.Element;
import org.apache.mesos.scheduler.plan.Step;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The {@link DependencyStrategyHelper} exists to aid in the construction of {@link Strategy} objects which describe a
 * set of dependencies between constituent elements.
 *
 * @param <C> is the type of {@link Element}s to which the dependencies captured here apply.
 */
public class DependencyStrategyHelper<C extends Element> {
    private final Map<C, Set<C>> dependencies;

    public DependencyStrategyHelper() {
        this(Collections.emptyList());
    }

    public DependencyStrategyHelper(Collection<C> elements) {
        this.dependencies = new HashMap<>();
        elements.forEach(child -> dependencies.put(child, new HashSet<>()));
    }

    public DependencyStrategyHelper(Element<C> parentElement) {
        this(parentElement.getChildren());
    }

    public void addElement(C element) throws InvalidDependencyException {
        if (dependencies.get(element) != null) {
            throw new InvalidDependencyException("Attempted to overwrite previously added element: " + element);
        }

        dependencies.put(element, new HashSet<>());
    }

    public void addDependency(C child, C parent) {
        Set<C> deps = dependencies.get(child);
        if (deps == null) {
            deps = new HashSet<>();
        }

        if (dependencies.get(parent) == null) {
            dependencies.put(parent, new HashSet<>());
        }

        deps.add(parent);
        dependencies.put(child, deps);
    }

    public Collection<C> getCandidates(Collection<String> dirtyAssets) {
        if (dependencies.size() == 0) {
            return Collections.emptyList();
        }

        Collection<C> candidates = dependencies.entrySet().stream()
                .filter(entry -> !entry.getKey().getStrategy().isInterrupted())
                .filter(entry -> !entry.getKey().isComplete())
                .filter(entry -> !entry.getKey().hasErrors())
                .filter(entry -> dependenciesFulfilled(entry.getValue()))
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());

        if (candidates.size() == 0) {
            return Collections.emptyList();
        }

        if (candidates.stream().findFirst().get() instanceof Step) {
            List<C> cands = new ArrayList<C>();
            for (C candidate : candidates) {
                Step step = (Step) candidate;
                Optional<String> asset = step.getAsset();
                if (!asset.isPresent()) {
                    cands.add(candidate);
                } else if (!dirtyAssets.contains(asset.get())) {
                    cands.add(candidate);
                }
            }

            candidates = cands;
        }

        return candidates;
    }

    public Map<C, Set<C>> getDependencies() {
        return dependencies;
    }

    private boolean dependenciesFulfilled(Set<C> deps) {
        if (deps.isEmpty()) {
            return true;
        } else {
            return deps.stream().allMatch(c -> c.isComplete());
        }
    }

    /**
     * An {@link InvalidDependencyException} is thrown when an attempt generate an invalid dependency occurs.
     */
    public static class InvalidDependencyException extends Exception {
        public InvalidDependencyException(String message) {
            super(message);
        }
    }
}

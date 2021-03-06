package com.mesosphere.sdk.offer;

import com.google.protobuf.TextFormat;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Resource.DiskInfo;
import org.apache.mesos.Protos.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A representation of the pool of resources available in a single {@link Offer}. Tracks the
 * consumption of the {@link Offer}'s resources, as they are matched with
 * {@link ResourceRequirement}s.
 */
public class MesosResourcePool {
    private static final Logger logger = LoggerFactory.getLogger(MesosResourcePool.class);

    private final Offer offer;
    private final Map<String, List<MesosResource>> unreservedAtomicPool;
    private final Map<String, Value> unreservedMergedPool;
    private final Map<String, MesosResource> reservedPool;

    /**
     * Creates a new pool of resources based on what's available in the provided {@link Offer}.
     */
    public MesosResourcePool(Offer offer) {
        this.offer = offer;
        final Collection<MesosResource> mesosResources = getMesosResources(offer);
        this.unreservedAtomicPool = getUnreservedAtomicPool(mesosResources);
        this.unreservedMergedPool = getUnreservedMergedPool(mesosResources);
        this.reservedPool = getReservedPool(mesosResources);
    }

    /**
     * Returns the underlying offer which this resource pool represents.
     */
    public Offer getOffer() {
        return offer;
    }

    /**
     * Returns the unreserved resources which cannot be partially consumed from an Offer. For
     * example, a MOUNT volume cannot be partially consumed, it's all-or-nothing.
     */
    public Map<String, List<MesosResource>> getUnreservedAtomicPool() {
        return unreservedAtomicPool;
    }

    /**
     * Returns the unreserved resources of which a subset can be consumed from an Offer. For
     * example, an offer may contain 4.0 CPUs and 2.4 of those CPUs can be reserved.
     */
    public Map<String, Value> getUnreservedMergedPool() {
        return unreservedMergedPool;
    }

    /**
     * Returns the resources which are reserved.
     */
    public Map<String, MesosResource> getReservedPool() {
        return reservedPool;
    }

    /**
     * Consumes and returns a {@link MesosResource} which meets the provided
     * {@link ResourceRequirement}, or does nothing and returns an empty {@link Optional} if no
     * available resources meet the requirement.
     */
    public Optional<MesosResource> consume(ResourceRequirement resourceRequirement) {
        if (resourceRequirement.expectsResource()) {
            logger.info("Retrieving reserved resource");
            return consumeReserved(resourceRequirement);
        } else if (resourceRequirement.isAtomic()) {
            logger.info("Retrieving atomic resource");
            return consumeAtomic(resourceRequirement);
        } else if (resourceRequirement.reservesResource()) {
            logger.info("Retrieving resource for reservation");
            return consumeUnreservedMerged(resourceRequirement);
        } else if (resourceRequirement.consumesUnreservedResource()) {
            logger.info("Retrieving resource for unreserved resource requirement.");
            return consumeUnreservedMerged(resourceRequirement);
        }

        logger.error("The following resource requirement did not meet any consumption criteria: {}",
                TextFormat.shortDebugString(resourceRequirement.getResource()));
        return Optional.empty();
    }

    /**
     * Consumes and returns a {@link MesosResource} which meets the provided
     * {@link DynamicPortRequirement}, or does nothing and returns an empty {@link Optional} if no
     * available resources meet the requirement.
     */
    public Optional<MesosResource> consume(DynamicPortRequirement dynamicPortRequirement) {
        Value availableValue = unreservedMergedPool.get(dynamicPortRequirement.getName());

        if (availableValue == null) {
            return Optional.empty();
        }

        // Choose first available port
        if (availableValue.getRanges().getRangeCount() > 0) {
            Value.Range range = availableValue.getRanges().getRange(0);
            Resource resource = ResourceUtils.getUnreservedResource(
                    dynamicPortRequirement.getName(),
                    Value.newBuilder()
                        .setType(Value.Type.RANGES)
                        .setRanges(Value.Ranges.newBuilder()
                                .addRange(Value.Range.newBuilder()
                                        .setBegin(range.getBegin())
                                        // Use getBegin again, since we just want the one port.
                                        .setEnd(range.getBegin())))
                        .build());

            return consumeUnreservedMerged(new ResourceRequirement(resource));
        }

        return Optional.empty();
    }

    /**
     * Marks the provided resource as available for consumption.
     */
    public void release(MesosResource mesosResource) {
        if (mesosResource.isAtomic()) {
            releaseAtomicResource(mesosResource);
            return;
        } else {
            releaseMergedResource(mesosResource);
            return;
        }
    }

    private void releaseMergedResource(MesosResource mesosResource) {
        Value currValue = unreservedMergedPool.get(mesosResource.getName());

        if (currValue == null) {
            currValue = ValueUtils.getZero(mesosResource.getType());
        }

        Value updatedValue = ValueUtils.add(currValue, mesosResource.getValue());
        unreservedMergedPool.put(mesosResource.getName(), updatedValue);
    }

    private void releaseAtomicResource(MesosResource mesosResource) {
        Resource.Builder resBuilder = Resource.newBuilder(mesosResource.getResource());
        resBuilder.clearReservation();
        resBuilder.setRole("*");

        if (resBuilder.hasDisk()) {
            DiskInfo.Builder diskBuilder = DiskInfo.newBuilder(resBuilder.getDisk());
            diskBuilder.clearPersistence();
            diskBuilder.clearVolume();
            resBuilder.setDisk(diskBuilder.build());
        }

        Resource releasedResource = resBuilder.build();

        List<MesosResource> resList = unreservedAtomicPool.get(mesosResource.getName());
        if (resList == null) {
            resList = new ArrayList<MesosResource>();
        }

        resList.add(new MesosResource(releasedResource));
        unreservedAtomicPool.put(mesosResource.getName(), resList);
    }

    private Optional<MesosResource> consumeReserved(ResourceRequirement resourceRequirement) {
        MesosResource mesosResource = reservedPool.get(resourceRequirement.getResourceId());

        if (mesosResource != null) {
            if (mesosResource.isAtomic()) {
                if (sufficientValue(resourceRequirement.getValue(), mesosResource.getValue())) {
                    reservedPool.remove(resourceRequirement.getResourceId());
                } else {
                    return Optional.empty();
                }
            } else {
                reservedPool.remove(resourceRequirement.getResourceId());
            }
        } else {
           logger.warn("Failed to find reserved resource: {}, in available resources: {}",
                   resourceRequirement.getResourceId(), reservedPool.keySet());
        }

        return Optional.ofNullable(mesosResource);
    }

    private Optional<MesosResource> consumeAtomic(ResourceRequirement resourceRequirement) {
        Value desiredValue = resourceRequirement.getValue();
        List<MesosResource> atomicResources = unreservedAtomicPool.get(resourceRequirement.getName());
        List<MesosResource> filteredResources = new ArrayList<>();
        Optional<MesosResource> sufficientResource = Optional.empty();

        if (atomicResources != null) {
            for (MesosResource atomicResource : atomicResources) {
                if (sufficientValue(desiredValue, atomicResource.getValue())) {
                    sufficientResource = Optional.of(atomicResource);
                    // do NOT break: ensure filteredResources is fully populated
                } else {
                    filteredResources.add(atomicResource);
                }
            }
        }

        if (filteredResources.size() == 0) {
            unreservedAtomicPool.remove(resourceRequirement.getName());
        } else {
            unreservedAtomicPool.put(resourceRequirement.getName(), filteredResources);
        }

        if (!sufficientResource.isPresent()) {
            logger.warn("No sufficient atomic resources found for resource requirement: {}",
                    TextFormat.shortDebugString(resourceRequirement.getResource()));
        }

        return sufficientResource;
    }

    private Optional<MesosResource> consumeUnreservedMerged(ResourceRequirement resourceRequirement) {
        Value desiredValue = resourceRequirement.getValue();
        Value availableValue = unreservedMergedPool.get(resourceRequirement.getName());

        if (sufficientValue(desiredValue, availableValue)) {
            unreservedMergedPool.put(resourceRequirement.getName(), ValueUtils.subtract(availableValue, desiredValue));
            Resource resource = ResourceUtils.getUnreservedResource(resourceRequirement.getName(), desiredValue);
            return Optional.of(new MesosResource(resource));
        } else {
            return Optional.empty();
        }
    }

    private static boolean sufficientValue(Value desired, Value available) {
        if (desired == null) {
            return true;
        } else if (available == null) {
            return false;
        }

        Value difference = ValueUtils.subtract(desired, available);
        return ValueUtils.compare(difference, ValueUtils.getZero(desired.getType())) <= 0;
    }

    private static Collection<MesosResource> getMesosResources(Offer offer) {
        Collection<MesosResource> mesosResources = new ArrayList<MesosResource>();

        for (Resource resource : offer.getResourcesList()) {
            mesosResources.add(new MesosResource(resource));
        }

        return mesosResources;
    }

    private static Map<String, MesosResource> getReservedPool(
            Collection<MesosResource> mesosResources) {
        Map<String, MesosResource> reservedPool = new HashMap<String, MesosResource>();

        for (MesosResource mesResource : mesosResources) {
            if (mesResource.hasResourceId()) {
                reservedPool.put(mesResource.getResourceId(), mesResource);
            }
        }

        return reservedPool;
    }

    private static Map<String, List<MesosResource>> getUnreservedAtomicPool(
            Collection<MesosResource> mesosResources) {
        Map<String, List<MesosResource>> pool = new HashMap<String, List<MesosResource>>();

        for (MesosResource mesosResource : getUnreservedAtomicResources(mesosResources)) {
            String name = mesosResource.getName();
            List<MesosResource> resList = pool.get(name);

            if (resList == null) {
                resList = new ArrayList<MesosResource>();
            }

            resList.add(mesosResource);
            pool.put(name, resList);
        }

        return pool;
    }

    private static Map<String, Value> getUnreservedMergedPool(
            Collection<MesosResource> mesosResources) {
        Map<String, Value> pool = new HashMap<String, Value>();

        for (MesosResource mesosResource : getUnreservedMergedResources(mesosResources)) {
            String name = mesosResource.getName();
            Value currValue = pool.get(name);

            if (currValue == null) {
                currValue = ValueUtils.getZero(mesosResource.getType());
            }

            pool.put(name, ValueUtils.add(currValue, mesosResource.getValue()));
        }

        return pool;
    }

    private static Collection<MesosResource> getUnreservedAtomicResources(
            Collection<MesosResource> mesosResources) {
        return getUnreservedResources(getAtomicResources(mesosResources));
    }

    private static Collection<MesosResource> getUnreservedMergedResources(
            Collection<MesosResource> mesosResources) {
        return getUnreservedResources(getMergedResources(mesosResources));
    }

    private static Collection<MesosResource> getUnreservedResources(
            Collection<MesosResource> mesosResources) {
        Collection<MesosResource> unreservedResources = new ArrayList<MesosResource>();

        for (MesosResource mesosResource : mesosResources) {
            if (!mesosResource.hasResourceId()) {
                unreservedResources.add(mesosResource);
            }
        }

        return unreservedResources;
    }

    private static Collection<MesosResource> getAtomicResources(
            Collection<MesosResource> mesosResources) {
        Collection<MesosResource> atomicResources = new ArrayList<>();

        for (MesosResource mesosResource : mesosResources) {
            if (mesosResource.isAtomic()) {
                atomicResources.add(mesosResource);
            }
        }

        return atomicResources;
    }

    private static Collection<MesosResource> getMergedResources(
            Collection<MesosResource> mesosResources) {
        Collection<MesosResource> mergedResources = new ArrayList<>();

        for (MesosResource mesosResource : mesosResources) {
            if (!mesosResource.isAtomic()) {
                mergedResources.add(mesosResource);
            }
        }

        return mergedResources;
    }
}

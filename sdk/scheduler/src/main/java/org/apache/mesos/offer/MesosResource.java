package org.apache.mesos.offer;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Label;
import org.apache.mesos.Protos.Labels;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Resource.DiskInfo.Source;
import org.apache.mesos.Protos.Value;

/**
 * Wrapper around a Mesos {@link Resource}, combined with a resource ID string.
 **/
public class MesosResource {
    public static final String RESOURCE_ID_KEY = "resource_id";
    public static final String RESOURCE_SET_NAME_KEY = "resource_set";
    public static final String DYNAMIC_PORT_KEY = "dynamic_port";
    public static final String VIP_LABEL_NAME_KEY = "vip_key";
    public static final String VIP_LABEL_VALUE_KEY = "vip_value";

    private final Resource resource;
    private final String resourceId;
    private final String resourceSetName;
    private final String podName;
    private final int podIndex;

    public MesosResource(Resource resource) {
        this.resource = resource;
        this.resourceId = ResourceUtils.getResourceId(resource);
        this.resourceSetName = ResourceUtils.getResourceSetName(resource);
        this.podName = ResourceUtils.getPodName(resource);
        this.podIndex = ResourceUtils.getPodIndex(resource);
        //validate();
    }

    public static class MesosResourceException extends Exception {
        public MesosResourceException(String msg) {
            super(msg);
        }
    }

    private void validate() throws MesosResourceException {
        if (hasResourceId() && !hasResourceSet()) {
            throw new MesosResourceException("Has resource ID but is not a member of a resource set");
        }

        if (!hasResourceId() && hasResourceSet()) {
            throw new MesosResourceException("Has resource set but does not have a resource ID.");
        }
    }

    public Resource getResource() {
        return resource;
    }

    public boolean isAtomic() {
        return resource.hasDisk()
            && resource.getDisk().hasSource()
            && resource.getDisk().getSource().getType().equals(Source.Type.MOUNT);
    }

    public String getName() {
        return resource.getName();
    }

    public Value.Type getType() {
        return resource.getType();
    }

    public boolean hasResourceId() {
        return resourceId != null;
    }

    public String getResourceId() {
        return resourceId;
    }

    public boolean hasResourceSet() {
        return resourceSetName != null && podName != null;
    }

    public String getResourceSetName() {
        return resourceSetName;
    }

    public String getPodName() {
        return podName;
    }

    public int getPodIndex() {
        return podIndex;
    }

    public boolean hasReservation() {
        return resource.hasReservation();
    }

    public Value getValue() {
        return ValueUtils.getValue(resource);
    }

    public String getRole() {
        return resource.getRole();
    }

    public String getPrincipal() {
        if (hasReservation() &&
                resource.getReservation().hasPrincipal()) {
            return resource.getReservation().getPrincipal();
        }

        return null;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


}

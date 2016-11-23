package org.apache.mesos.specification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.mesos.specification.util.RLimit;

import java.util.Collection;

/**
 * Specification for rlimits set within a container instance.
 */
@JsonDeserialize(as = DefaultRLimitSpec.class)
public interface RLimitSpec {

    @JsonProperty("rlimits")
    Collection<RLimit> getRLimits();
}

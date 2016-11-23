package org.apache.mesos.specification;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.mesos.specification.util.RLimit;

import javax.validation.constraints.Size;
import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * Default implementation of an {@link RLimitSpec}.
 */
public class DefaultRLimitSpec implements RLimitSpec {
    @NotNull
    @Size(min = 1)
    private Collection<RLimit> rlimits;

    public DefaultRLimitSpec(@JsonProperty("rlimits") Collection<RLimit> rlimits) {
        this.rlimits = rlimits;
    }

    @Override
    public Collection<RLimit> getRLimits() {
        return rlimits;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}

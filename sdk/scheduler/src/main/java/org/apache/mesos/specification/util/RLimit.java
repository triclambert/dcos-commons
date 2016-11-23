package org.apache.mesos.specification.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Representation of an individual rlimit, consisting of a name and optional soft/hard limits.
 *
 * A valid instance of this class has either both limits set or neither, with the further constraint that the soft limit
 * must be less than or equal to the hard limit.
 */
public class RLimit {
    public static final String UNKNOWN = "RLIMIT_UNKNOWN";
    public static final String AS = "RLIMIT_AS";
    public static final String CORE = "RLIMIT_CORE";
    public static final String CPU = "RLIMIT_CPU";
    public static final String DATA = "RLIMIT_DATA";
    public static final String FSIZE = "RLIMIT_FSIZE";
    public static final String LOCKS = "RLIMIT_LOCKS";
    public static final String MEMLOCK = "RLIMIT_MEMLOCK";
    public static final String MSGQUEUE = "RLIMIT_MSGQUEUE";
    public static final String NICE = "RLIMIT_NICE";
    public static final String NOFILE = "RLIMIT_NOFILE";
    public static final String NPROC = "RLIMIT_NPROC";
    public static final String RSS = "RLIMIT_RSS";
    public static final String RTPRIO = "RLIMIT_RTPRIO";
    public static final String RTTIME = "RLIMIT_RTTIME";
    public static final String SIGPENDING = "RLIMIT_SIGPENDING";
    public static final String STACK = "RLIMIT_STACK";

    private static final Set<String> RLIMITS = new HashSet<>();

    static {
        RLIMITS.add(UNKNOWN);
        RLIMITS.add(AS);
        RLIMITS.add(CORE);
        RLIMITS.add(CPU);
        RLIMITS.add(DATA);
        RLIMITS.add(FSIZE);
        RLIMITS.add(LOCKS);
        RLIMITS.add(MEMLOCK);
        RLIMITS.add(MSGQUEUE);
        RLIMITS.add(NICE);
        RLIMITS.add(NOFILE);
        RLIMITS.add(NPROC);
        RLIMITS.add(RSS);
        RLIMITS.add(RTPRIO);
        RLIMITS.add(RTTIME);
        RLIMITS.add(SIGPENDING);
        RLIMITS.add(STACK);
    }

    private final String name;
    private final Long soft;
    private final Long hard;

    public RLimit(
            @JsonProperty("name") String name,
            @JsonProperty("soft") Long soft,
            @JsonProperty("hard") Long hard) throws InvalidRLimitException {
        this.name = name;
        this.soft = soft;
        this.hard = hard;

        validate();
    }

    public String getName() {
        return name;
    }

    public Long getSoft() {
        return soft;
    }

    public Long getHard() {
        return hard;
    }

    private void validate() throws InvalidRLimitException {
        if (!RLIMITS.contains(name)) {
            throw new InvalidRLimitException(name + " is not a valid rlimit");
        }

        if (!(soft == -1 && hard == -1) && !(soft != -1 && hard != -1)) {
            throw new InvalidRLimitException("soft and hard rlimits must be either both set or both unset");
        }

        if (soft > hard || soft < hard && soft == -1) {
            throw new InvalidRLimitException("soft rlimit must be less than or equal to the hard rlimit");
        }
    }

    /**
     * An exception for errors pertaining to {@link RLimit}.
     */
    public static class InvalidRLimitException extends Exception {
        public InvalidRLimitException(String s) {
            super(s);
        }
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

package org.apache.mesos.specification.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw YAML individual rlimit specification.
 */
public class RawRLimit {
    private Long soft;
    private Long hard;

    public Long getSoft() {
        return soft;
    }

    @JsonProperty("soft")
    public void setSoft(Long soft) {
        this.soft = soft;
    }

    public Long getHard() {
        return hard;
    }

    @JsonProperty("hard")
    public void setHard(Long hard) {
        this.hard = hard;
    }
}

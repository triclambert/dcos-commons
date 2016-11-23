package org.apache.mesos.specification.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.mesos.util.WriteOnceLinkedHashMap;

import java.util.LinkedHashMap;

/**
 * Raw YAML container.
 */
public class RawContainer {
    private String imageName;
    private WriteOnceLinkedHashMap<String, RawRLimit> rlimits;

    public String getImageName() {
        return imageName;
    }

    @JsonProperty("image-name")
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public LinkedHashMap<String, RawRLimit> getRLimits() {
        return rlimits;
    }

    @JsonProperty("rlimits")
    public void setRLimits(WriteOnceLinkedHashMap<String, RawRLimit> rlimits) {
        this.rlimits = rlimits;
    }
}

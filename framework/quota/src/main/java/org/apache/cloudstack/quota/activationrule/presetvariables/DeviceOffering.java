package org.apache.cloudstack.quota.activationrule.presetvariables;

import java.util.List;

public class DeviceOffering extends GenericPresetVariable {
    @PresetVariableDefinition(description = "List of tags of the device offering (i.e.: [\"tag1\", \"tag2\"]).")
    private List<String> tags;
    @PresetVariableDefinition(description = "The ID of the domain to which the device offering belongs.")
    private Long domainId;
    @PresetVariableDefinition(description = "The ID of the zone to which the device offering belongs.")
    private Long zoneId;

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }
}

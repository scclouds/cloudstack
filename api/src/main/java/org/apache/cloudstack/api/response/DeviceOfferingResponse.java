package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.hostdevices.DeviceOffering;

import java.util.Date;

@EntityReference(value = DeviceOffering.class)
public class DeviceOfferingResponse extends BaseResponse {
    private String name;
    private String description;
    private String state;
    private String id;
    private Long domainId;
    private Long zoneID;
    private Date created;
    private Date removed;
    private Boolean isPublic;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public void setZoneID(Long zoneID) {
        this.zoneID = zoneID;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }
}

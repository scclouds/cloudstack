package com.cloud.hostdevices;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "device_offering_device_tags")
public class DeviceOfferingDeviceTagVO {
    @Column(name = "device_offering_id")
    private Long deviceOfferingId;
    @Column(name = "device_tag")
    private String deviceTag;

    public DeviceOfferingDeviceTagVO() {
    }

    public DeviceOfferingDeviceTagVO(Long deviceOfferingId, String deviceTag) {
        this.deviceOfferingId = deviceOfferingId;
        this.deviceTag = deviceTag;
    }

    public Long getDeviceOfferingId() {
        return deviceOfferingId;
    }

    public String getDeviceTag() {
        return deviceTag;
    }
}

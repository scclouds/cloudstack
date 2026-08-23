package com.cloud.hostdevices;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "vm_instance_device_offerings")
public class VMInstanceDeviceOfferingsVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "vm_instance_id")
    private Long virtualMachineId;
    @Column(name = "device_offering_id")
    private Long deviceOfferingId;

    public VMInstanceDeviceOfferingsVO() {
    }

    public VMInstanceDeviceOfferingsVO(Long virtualMachineId, Long deviceOfferingId) {
        this.virtualMachineId = virtualMachineId;
        this.deviceOfferingId = deviceOfferingId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public Long getDeviceOfferingId() {
        return deviceOfferingId;
    }

    public void setDeviceOfferingId(Long deviceOfferingId) {
        this.deviceOfferingId = deviceOfferingId;
    }
}

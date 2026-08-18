package org.apache.cloudstack.hostdevices.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "device_offerings")
public class DeviceOfferingVO implements DeviceOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "created")
    @Temporal(value = TemporalType.DATE)
    private Date created;

    @Column(name = "removed")
    @Temporal(value = TemporalType.DATE)
    private Date removed;

    @Column(name = "public")
    private Boolean isPublic;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "zone_id")
    private Long zoneId;

    public DeviceOfferingVO() {}

    public DeviceOfferingVO(String name, String description, Long domainId, Long zoneId) {
        this.name = name;
        this.description = description;
        this.domainId = domainId;
        this.zoneId = zoneId;
        this.uuid = UUID.randomUUID().toString();
        this.state = State.Enabled;
        this.isPublic = domainId != null || zoneId != null;
    }

    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public State getState() {
        return state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getZoneId() {
        return zoneId;
    }
}

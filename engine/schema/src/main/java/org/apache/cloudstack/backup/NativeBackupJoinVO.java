package org.apache.cloudstack.backup;

import org.apache.commons.lang3.BooleanUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "native_backup_view")
public class NativeBackupJoinVO {

    @Id
    @Column(name="id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "backup_offering_id")
    private long backupOfferingId;

    @Column(name = "image_store_id")
    private long imageStoreId;

    @Column(name = "parent_id")
    private long parentId;

    @Column(name = "type")
    private String type;

    @Column(name = "date")
    @Temporal(value = TemporalType.DATE)
    private Date date;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    private Backup.Status status;

    @Column(name = "end_of_chain")
    private Boolean endOfChain;

    @Column(name = "current")
    private Boolean current;

    public NativeBackupJoinVO() {
    }

    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getVmId() {
        return vmId;
    }

    public long getBackupOfferingId() {
        return backupOfferingId;
    }

    public long getImageStoreId() {
        return imageStoreId;
    }

    public long getParentId() {
        return parentId;
    }

    public String getType() {
        return type;
    }

    public Date getDate() {
        return date;
    }

    public Backup.Status getStatus() {
        return status;
    }

    public Boolean getEndOfChain() {
        return BooleanUtils.isTrue(endOfChain);
    }

    public Boolean getCurrent() {
        return BooleanUtils.isTrue(current);
    }
}

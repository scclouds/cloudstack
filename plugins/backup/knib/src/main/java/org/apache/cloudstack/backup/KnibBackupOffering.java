package org.apache.cloudstack.backup;

import java.util.Date;

public class KnibBackupOffering implements BackupOffering {

    private String uuid;

    private String name;

    private String description;

    public KnibBackupOffering(String name, String uuid, String description) {
        this.name = name;
        this.uuid = uuid;
        this.description = description;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return -1;
    }

    @Override
    public String getExternalId() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public long getZoneId() {
        return -1;
    }

    @Override
    public boolean isUserDrivenBackupAllowed() {
        return true;
    }

    @Override
    public String getProvider() {
        return "knib";
    }

    @Override
    public Date getCreated() {
        return null;
    }
}

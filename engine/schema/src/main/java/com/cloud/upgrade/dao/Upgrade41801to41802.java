package com.cloud.upgrade.dao;

import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressDaoImpl;
import com.cloud.network.dao.PublicIpQuarantineDao;
import com.cloud.network.dao.PublicIpQuarantineDaoImpl;
import com.cloud.network.vo.PublicIpQuarantineVO;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

public class Upgrade41801to41802 implements DbUpgrade {
    private PublicIpQuarantineDao publicIpQuarantineDao = new PublicIpQuarantineDaoImpl();

    private IPAddressDao ipAddressDao = new IPAddressDaoImpl();

    final static Logger LOG = Logger.getLogger(Upgrade41801to41802.class);

    private static final String IP_QUARANTINE_DUPLICATE_MSG = "Setting the `removed` column the same as `created` column due" +
            " to a bug causing duplication of registry when a public IP of Guest network was deallocated.";

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.18.0.1", "4.18.0.2"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.18.0.2";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41801to41802.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    public void normalizeDuplicateQuarantinedIps() {
        List<PublicIpQuarantineVO> ipsInQuarantine = publicIpQuarantineDao.listAllIncludingRemoved();
        Map<Date, List<PublicIpQuarantineVO>> ipsToBeUpdated = new HashMap<>();
        LOG.debug(String.format("Retrieved the following public IP(s) in quarantine %s.", ipsInQuarantine));

        List<Long> uniqueIdsOfPublicIpsInQuarantine = ipsInQuarantine.stream()
                .map(PublicIpQuarantineVO::getPublicIpAddressId)
                .distinct()
                .collect(Collectors.toList());

        for (long publicIpId : uniqueIdsOfPublicIpsInQuarantine) {
            LOG.debug(String.format("Iterating over the quarantine entries for public IP with ID [%s].", publicIpId));
            List<PublicIpQuarantineVO> samePublicIpIdQuarantines = ipsInQuarantine.stream()
                    .filter(publicIp -> publicIp.getPublicIpAddressId() == publicIpId)
                    .collect(Collectors.toList());
            findAndAddDuplicatedQuarantineEntries(samePublicIpIdQuarantines, ipsToBeUpdated);
        }
        updatePublicIps(ipsToBeUpdated);
        LOG.debug("Finished normalizing duplicate entries for public IPs in quarantine.");
    }

    public void findAndAddDuplicatedQuarantineEntries(List<PublicIpQuarantineVO> samePublicIpIdQuarantines, Map<Date, List<PublicIpQuarantineVO>> ipsToBeUpdated) {
        ListIterator<PublicIpQuarantineVO> iterator = samePublicIpIdQuarantines.listIterator();
        while (iterator.hasNext()) {
            PublicIpQuarantineVO currentIpQuarantine = iterator.next();
            LOG.debug(String.format("Iterating over the quarantine entries for public IP address [%s].", currentIpQuarantine));

            if (ipsToBeUpdated.values().stream().anyMatch(listOfIpsToBeUpdated -> listOfIpsToBeUpdated.contains(currentIpQuarantine))) {
                String address = ipAddressDao.findById(currentIpQuarantine.getPublicIpAddressId()).getAddress().toString();
                LOG.debug(String.format("Skipping IP address [%s] as it is already in the list of public IPs address to be updated.", address));
                continue;
            }

            Date created = currentIpQuarantine.getCreated();

            List<PublicIpQuarantineVO> collectedDuplicateQuarantineEntries = samePublicIpIdQuarantines.stream()
                    .filter(possibleDuplicateEntry -> Math.abs(possibleDuplicateEntry.getCreated().getTime() - created.getTime()) < 5000 &&
                            possibleDuplicateEntry.getRemoved() == null &&
                            possibleDuplicateEntry.getId() != currentIpQuarantine.getId())
                    .collect(Collectors.toList());

            if (collectedDuplicateQuarantineEntries.isEmpty()) {
                LOG.debug(String.format("Did not find any IP that are in a 5 second interval of the quarantine entry %s and do not have the `removed` column " +
                        "set.", currentIpQuarantine));
                continue;
            }

            LOG.debug(String.format("Found the IP(s) %s that are in a 5 second interval of the quarantine entry [%s] and do not have the `removed` column set.",
                    collectedDuplicateQuarantineEntries, currentIpQuarantine));
            if (ipsToBeUpdated.containsKey(created)) {
                LOG.debug(String.format("The key [%s] is already on the HashMap, therefore, we will append the IPs [%s] to this key. ", created, collectedDuplicateQuarantineEntries));
                ipsToBeUpdated.get(created).addAll(collectedDuplicateQuarantineEntries);
            } else {
                LOG.debug(String.format("The key [%s] is not in the HashMap, therefore, we will put the IPs [%s] in this new key. ", created, collectedDuplicateQuarantineEntries));
                ipsToBeUpdated.put(created, collectedDuplicateQuarantineEntries);
            }

            LOG.debug(String.format("The following IPs %s will have the removed date updated to [%s] and the removal reason updated to [%s].",
                    ipsToBeUpdated, created, IP_QUARANTINE_DUPLICATE_MSG));
        }
    }

    public void updatePublicIps(Map<Date, List<PublicIpQuarantineVO>> ipsToBeUpdated) {
        for (Date newDateValue : ipsToBeUpdated.keySet()) {
            for (PublicIpQuarantineVO publicIpQuarantineToBeUpdated : ipsToBeUpdated.get(newDateValue)) {
                LOG.debug(String.format("Updating the quarantine entry for the public IPs [%s].", publicIpQuarantineToBeUpdated));
                publicIpQuarantineToBeUpdated.setRemoved(newDateValue);
                publicIpQuarantineToBeUpdated.setRemovalReason(IP_QUARANTINE_DUPLICATE_MSG);
                publicIpQuarantineDao.update(publicIpQuarantineToBeUpdated.getId(), publicIpQuarantineToBeUpdated);
            }
        }
    }

    @Override
    public void performDataMigration(Connection conn) {
        normalizeDuplicateQuarantinedIps();
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return new InputStream[0];
    }
}
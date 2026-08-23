package org.apache.cloudstack.hostdevices;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

public interface HostDevice extends Identity, InternalIdentity {
    enum State {
        Attached, Disabled, Failure, Free, HostInMaintenance, Missing;

        public static State getFromString(String state) {
            for (State s : State.values()) {
                if (s.name().equalsIgnoreCase(state)) {
                    return s;
                }
            }
            return null;
        }
    }

    enum Type {
        Display, Network, Storage, USB, Generic;

        public static Type getFromString(String type) {
            for (Type t : Type.values()) {
                if (t.name().equalsIgnoreCase(type)) {
                    return t;
                }
            }
            return null;
        }

        public static Type getFromClassCode(String classCode) {
            classCode = removePrefix(classCode);
            int classInt = getDeviceClassAsInt(classCode);
            switch (classInt) {
                case 1:
                    return Storage;
                case 2:
                    return Network;
                case 3:
                    return Display;
                case 12:
                    return USB;
                default:
                    return Generic;
            }
        }

        private static int getDeviceClassAsInt(String classCode) {
            return Integer.parseInt(classCode.substring(0, 2), 16);
        }

        private static String removePrefix(String classCode) {
            return classCode.substring(2);
        }
    }

    String getDisplayName();

    String getPciName();

    String getPciDomain();

    String getPciClass();

    String getPciSlot();

    String getPciBus();

    String getPciFunction();

    String getPciVendorId();

    String getPciDeviceId();

    Date getCreated();

    Date getRemoved();

    State getState();

    Type getType();

    Long getInstanceId();

    String getDeviceTag();

    Long getAccountId();

    Long getDomainId();

    Long getHostId();
}

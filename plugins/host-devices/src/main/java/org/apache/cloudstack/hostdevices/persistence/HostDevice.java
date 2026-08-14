package org.apache.cloudstack.hostdevices.persistence;

public interface HostDevice {
    enum State {
        Attached, Disabled, Failure, Free, HostInMaintenance, Missing
    }

    enum Type {
        Display, Network, Storage, USB, Generic;

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
}

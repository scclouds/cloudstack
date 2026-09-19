// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.utils.libvirt.model;

public class PciDevice implements LibvirtDevice {
    private String name;
    private String classCode;
    private String domain;
    private String bus;
    private String slot;
    private String function;
    private String productId;
    private String productName;
    private String vendorId;
    private String vendorName;
    private String deviceType;

    public PciDevice() {
        this.deviceType = "pci";
    }

    public PciDevice(String name, String classCode, String domain, String bus, String slot, String function, String productId, String vendorId, String productName, String vendorName) {
        this();
        this.name = name;
        this.classCode = classCode;
        this.domain = domain;
        this.bus = bus;
        this.slot = slot;
        this.function = function;
        this.productId = productId;
        this.vendorId = vendorId;
        this.productName = productName;
        this.vendorName = vendorName;
    }

    public PciDevice(String pciDomain, String pciBus, String pciSlot, String pciFunction) {
        this();
        this.domain = pciDomain;
        this.bus = pciBus;
        this.slot = pciSlot;
        this.function = pciFunction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getBus() {
        return bus;
    }

    public void setBus(String bus) {
        this.bus = bus;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    @Override
    public String getDeviceType() {
        return this.deviceType;
    }
}

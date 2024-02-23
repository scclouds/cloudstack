/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloud.hypervisor.kvm.resource;

import com.cloud.agent.properties.AgentPropertiesFileHandler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.libvirt.TypedParameter;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Set;

@RunWith(PowerMockRunner.class)
public class MigrateKVMAsyncTest {
    @Mock
    private AgentPropertiesFileHandler agentPropertiesFileHandler;
    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private Connect connect;
    @Mock
    private Domain domain;

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestPropertyChangedReturnIt() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                false, false, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(1000L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertEquals(1000L, flags);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestVirMigrateLive() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                false, false, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertEquals(1L, flags & 1L);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestVirMigrateCompressed() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                false, false, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        Mockito.when(connect.getLibVirVersion()).thenReturn(1000003L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertEquals(2048L, flags & 2048L);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestVirMigrateCompressedVersionNotSupported() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                false, false, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        Mockito.when(connect.getLibVirVersion()).thenReturn(1000000L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertNotEquals(2048L, flags & 2048L);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestVirMigratePersistDestAndVirMigrateNonSharedIncNotEqualReturnFail() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                true, true, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertNotEquals(128L, flags & 136L);
        Assert.assertNotEquals(8L, flags & 136L);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestVirMigratePersistDestAndVirMigrateNonSharedIncWithVirMigrateNonSharedDiskReturnFail() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                true, true, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertNotEquals(192L, flags & 200L);
        Assert.assertNotEquals(72L, flags & 200L);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestVirMigrateNonSharedDiskWithVirMigratePersistDestOrVirMigrateNonSharedIncReturnFail() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                true, false, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertNotEquals(192L, flags & 200L);
        Assert.assertNotEquals(72L, flags & 200L);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestVirMigrateNonSharedDisk() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                true, false, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertEquals(64L, flags & 64L);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestVirMigrateAutoConvergeVersionSupportedReturn() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                false, false, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        Mockito.when(connect.getLibVirVersion()).thenReturn(1002003L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertEquals(2049, flags & 2049);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestVirMigrateAutoConvergeVersionNotSupportedNotReturn() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                false, false, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        Mockito.when(connect.getLibVirVersion()).thenReturn(1002000L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertNotEquals(8192L, flags & 8192L);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestNotVirMigrateAutoConvergeVersionSupportedNotReturn() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                false, false, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        Mockito.when(connect.getLibVirVersion()).thenReturn(1002003L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertNotEquals(8192L, flags & 8192L);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getFlagsTestNotVirMigrateAutoConvergeVersionNotSupportedNotReturn() throws LibvirtException{
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "",
                false, false, "", "", null);

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(-1L);

        Mockito.when(connect.getLibVirVersion()).thenReturn(1002000L);

        long flags = migrateKVMAsync.getFlags();
        Assert.assertNotEquals(8192L, flags & 8192L);
    }

    @Test
    public void createTypedParameterListTestNoMigrateDiskLabels() {
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "testxml",
                false, false, "tst", "1.1.1.1", null);

        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();

        TypedParameter[] result = migrateKVMAsync.createTypedParameterList();

        Assert.assertEquals(4, result.length);

        Assert.assertEquals("tst", result[0].getValueAsString());
        Assert.assertEquals("testxml", result[1].getValueAsString());
        Assert.assertEquals("tcp:1.1.1.1", result[2].getValueAsString());
        Assert.assertEquals("10", result[3].getValueAsString());

    }

    @Test
    public void createTypedParameterListTestWithMigrateDiskLabels() {
        Set<String> labels = Set.of("vda", "vdb");
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "testxml",
                false, false, "tst", "1.1.1.1", labels);

        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();

        TypedParameter[] result = migrateKVMAsync.createTypedParameterList();

        Assert.assertEquals(6, result.length);

        Assert.assertEquals("tst", result[0].getValueAsString());
        Assert.assertEquals("testxml", result[1].getValueAsString());
        Assert.assertEquals("tcp:1.1.1.1", result[2].getValueAsString());
        Assert.assertEquals("10", result[3].getValueAsString());

        Assert.assertEquals(labels, Set.of(result[4].getValueAsString(), result[5].getValueAsString()));
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.resource;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.EncryptionUtil;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.test.TestAppender;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.xml.*", "org.xml.*"})
@PrepareForTest({EncryptionUtil.class})
public class NfsSecondaryStorageResourceTest {
    @Spy
    private NfsSecondaryStorageResource nfsSecondaryStorageResourceSpy = new NfsSecondaryStorageResource();

    private static final String HOSTNAME = "hostname";

    private static final String UUID = "uuid";

    private static final String METADATA = "metadata";

    private static final String TIMEOUT = "timeout";

    private static final String PSK = "6HyGMx9Vat7rZw1pMZrM4OlD4FFwLUPznTsFqVFSOIvk0mAWMRCVZ6UCq42gZvhp";

    private static final String PROTOCOL = "http";

    private static final String EXPECTED_SIGNATURE = "expectedSignature";

    private static final String COMPUTED_SIGNATURE = "computedSignature";

    private NfsSecondaryStorageResource resource;

    @Before
    public void setUp() {
        resource = new NfsSecondaryStorageResource();
    }

    @Test
    @PrepareForTest(NfsSecondaryStorageResource.class)
    public void testSwiftWriteMetadataFile() throws Exception {
        String filename = "testfile";
        try {
            String expected = "uniquename=test\nfilename=" + filename + "\nsize=100\nvirtualsize=1000";

            StringWriter stringWriter = new StringWriter();
            BufferedWriter bufferWriter = new BufferedWriter(stringWriter);
            PowerMockito.whenNew(BufferedWriter.class).withArguments(any(FileWriter.class)).thenReturn(bufferWriter);

            resource.swiftWriteMetadataFile(filename, "test", filename, 100, 1000);

            Assert.assertEquals(expected, stringWriter.toString());
        } finally {
            File remnance = new File(filename);
            remnance.delete();
        }
    }

    @Test
    public void testCleanupStagingNfs() throws Exception{

        NfsSecondaryStorageResource spyResource = spy(resource);
        RuntimeException exception = new RuntimeException();
        doThrow(exception).when(spyResource).execute(any(DeleteCommand.class));
        TemplateObjectTO mockTemplate = Mockito.mock(TemplateObjectTO.class);

        TestAppender.TestAppenderBuilder appenderBuilder = new TestAppender.TestAppenderBuilder();
        appenderBuilder.addExpectedPattern(Level.DEBUG, "Failed to clean up staging area:");
        TestAppender testLogAppender = appenderBuilder.build();
        TestAppender.safeAddAppender(NfsSecondaryStorageResource.s_logger, testLogAppender);

        spyResource.cleanupStagingNfs(mockTemplate);

        testLogAppender.assertMessagesLogged();

    }

    private void prepareForValidatePostUploadRequestSignatureTests() {
        Mockito.doReturn(PROTOCOL).when(nfsSecondaryStorageResourceSpy).getUploadProtocol();
        Mockito.doReturn(PSK).when(nfsSecondaryStorageResourceSpy).getPostUploadPSK();
        PowerMockito.mockStatic(EncryptionUtil.class);
        PowerMockito.when(EncryptionUtil.generateSignature(Mockito.anyString(), Mockito.anyString())).thenReturn(COMPUTED_SIGNATURE);
        String fullUrl = String.format("%s://%s/upload/%s", PROTOCOL, HOSTNAME, UUID);
        String data = String.format("%s%s%s", METADATA, fullUrl, TIMEOUT);
        PowerMockito.when(EncryptionUtil.generateSignature(data, PSK)).thenReturn(EXPECTED_SIGNATURE);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenProtocolDiffers() {
        prepareForValidatePostUploadRequestSignatureTests();
        Mockito.doReturn("https").when(nfsSecondaryStorageResourceSpy).getUploadProtocol();

        nfsSecondaryStorageResourceSpy.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, HOSTNAME, UUID, METADATA, TIMEOUT);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenHostnameDiffers() {
        prepareForValidatePostUploadRequestSignatureTests();

        nfsSecondaryStorageResourceSpy.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, "test", UUID, METADATA, TIMEOUT);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenUuidDiffers() {
        prepareForValidatePostUploadRequestSignatureTests();

        nfsSecondaryStorageResourceSpy.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, HOSTNAME, "test", METADATA, TIMEOUT);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenMetadataDiffers() {
        prepareForValidatePostUploadRequestSignatureTests();

        nfsSecondaryStorageResourceSpy.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, HOSTNAME, UUID, "test", TIMEOUT);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenTimeoutDiffers() {
        prepareForValidatePostUploadRequestSignatureTests();

        nfsSecondaryStorageResourceSpy.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, HOSTNAME, UUID, METADATA, "test");
    }

    @Test
    public void validatePostUploadRequestSignatureTestSuccessWhenDataIsTheSame() {
        prepareForValidatePostUploadRequestSignatureTests();

        nfsSecondaryStorageResourceSpy.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, HOSTNAME, UUID, METADATA, TIMEOUT);
    }

    @Test
    public void getUploadProtocolTestReturnHttpsWhenUseHttpsToUploadIsTrue() {
        Mockito.doReturn(true).when(nfsSecondaryStorageResourceSpy).useHttpsToUpload();

        String result = nfsSecondaryStorageResourceSpy.getUploadProtocol();

        Assert.assertEquals("https", result);
    }

    @Test
    public void getUploadProtocolTestReturnHttpWhenUseHttpsToUploadIsFalse() {
        Mockito.doReturn(false).when(nfsSecondaryStorageResourceSpy).useHttpsToUpload();

        String result = nfsSecondaryStorageResourceSpy.getUploadProtocol();

        Assert.assertEquals("http", result);
    }
}

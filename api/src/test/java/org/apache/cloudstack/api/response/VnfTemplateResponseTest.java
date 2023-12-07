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
package org.apache.cloudstack.api.response;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class VnfTemplateResponseTest {

    @Test
    public void testAddVnfNicToResponse() {
        final VnfTemplateResponse response = new VnfTemplateResponse();

        response.addVnfNic(new VnfNicResponse());
        response.addVnfNic(new VnfNicResponse());

        Assert.assertEquals(2, response.getVnfNics().size());
    }

    @Test
    public void testAddVnfDetailToResponse() {
        final VnfTemplateResponse response = new VnfTemplateResponse();

        response.addVnfDetail("key1", "value1");
        response.addVnfDetail("key2", "value2");
        response.addVnfDetail("key3", "value3");

        Assert.assertEquals(3, response.getVnfDetails().size());
    }
}

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

package org.apache.cloudstack.backup.veeam.api;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This class maps the XML definition of the sub-body parameters that is sent to Veeam to restore the VM.
 * For more information, please read these documentations:
 * https://helpcenter.veeam.com/docs/backup/em_rest/post_vmrestorepoints_id_actionrestore.html?ver=120
 * https://helpcenter.veeam.com/docs/backup/em_rest/em_web_api_specifications.html?ver=120
 */
@JacksonXmlRootElement(localName = "VmRestoreSpec")
public class VmRestoreSpec {
    @JacksonXmlProperty(localName = "QuickRollback")
    private Boolean quickRollback;

    public Boolean isQuickRollback() {
        return quickRollback;
    }

    public void setQuickRollback(Boolean quickRollback) {
        this.quickRollback = quickRollback;
    }
}

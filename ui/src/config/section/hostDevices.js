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

import store from '@/store'

export default {
  name: 'hostdevices',
  title: 'label.hostdevices',
  icon: 'link-outlined',
  permission: ['listHostDevices'],
  columns () {
    const columns = ['displayname', 'devicetag', 'state', 'type', 'virtualmachinename', 'account', 'domain']

    if (store.getters.userInfo.roletype === 'Admin') {
      columns.push('hostname')
    }

    return columns
  },
  actions: [
    {
      api: 'updateHostDevice',
      icon: 'edit-outlined',
      label: 'label.action.edit.hostdevice',
      dataView: true,
      popup: true,
      show: (record) => { return ['Disabled', 'Free'].includes(record.state) },
      args: ['id', 'displayname', 'devicetag', 'type', 'enabled'],
      mapping: {
        id: {
          value: (record) => { return record.id }
        }
      }
    }
  ]
}

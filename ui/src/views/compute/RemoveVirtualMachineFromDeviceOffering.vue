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

<template>
  <div class="container">
    <a-alert class="alert-message" type="warning">
      <template #message>
        <span>{{ $t('messagew.action.remove.device.offering') }}</span>
      </template>
    </a-alert>
    <a-table
      size="small"
      :columns="columns"
      :dataSource="deviceOfferings"
      :rowKey="record => record.id"
      :pagination="false"
      :loading="loading">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <label>{{ record.name }}</label>
        </template>
        <template v-if="column.key === 'deviceTags'">
          <label>{{ parseDeviceTags(record.devicetags) }}</label>
        </template>
        <template v-if="column.key === 'actions'">
          <div class="remove-button-container">
            <tooltip-button
              tooltipPlacement="right"
              :tooltip="$t('label.delete')"
              type="primary"
              :danger="true"
              icon="close-outlined"
              size="small"
              :disabled="loading || removeLoading"
              :loading="removeLoading"
              @onClick="removeDeviceOffering(record)"/>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>

import TooltipButton from '@/components/widgets/TooltipButton.vue'
import { postAPI } from '@/api'
import { parseDeviceTags } from '@/utils/util'

export default {
  components: {
    TooltipButton
  },
  name: 'RemoveVirtualMachineFromDeviceOffering',
  emits: ['refresh-data'],
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      columns: [],
      deviceOfferings: [],
      hostDevices: [],
      removeLoading: false
    }
  },
  watch: {
    'resource.deviceofferings': {
      immediate: true,
      handler (offerings) {
        this.deviceOfferings = offerings || []
      }
    }
  },
  created () {
    this.deviceOfferings = this.resource.deviceofferings
    this.columns = [
      {
        key: 'name',
        title: this.$t('label.name'),
        dataIndex: 'name',
        width: 200
      },
      {
        key: 'deviceTags',
        title: this.$t('label.device.tags'),
        dataIndex: 'deviceTags'
      },
      {
        key: 'actions',
        title: this.$t('label.actions'),
        dataIndex: 'actions',
        width: 25
      }
    ]
  },
  methods: {
    parseDeviceTags,
    removeDeviceOffering (record) {
      this.removeLoading = true
      postAPI('removeVirtualMachineFromDeviceOffering', {
        virtualmachineid: this.resource.id,
        deviceofferingid: record.id
      }).then(() => {
        const index = this.deviceOfferings.findIndex(
          offering => String(offering.id) === String(record.id)
        )

        if (index !== -1) {
          this.deviceOfferings.splice(index, 1)
        }
        this.$message.success(this.$t('message.remove.device.offering.success'))
        this.$emit('refresh-data')
      }).catch((error) => {
        this.$message.error(error.message || this.$t('message.remove.device.offering.failed'))
      }).finally(() => {
        this.removeLoading = false
      })
    }
  }
}

</script>

<style scoped lang="less">

.container {
  min-width: 480px;
  max-width: 480px;
}

.alert-message {
  margin-bottom: 15px;
}

.remove-button-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

</style>

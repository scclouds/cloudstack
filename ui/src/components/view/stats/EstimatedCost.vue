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
  <a-card :bordered="true" :title="$t('label.estimatedcost')" v-if="'quotaResourceQuoting' in $store.getters.apis">
    <a-spin :spinning="loading">
      <a-card-grid class="estimatedcost-card-grid" :hoverable="false">
        <div class="resource-detail-item">
          <div class="resource-detail-item__label">{{ $t('label.virtual.machine') }}</div>
          <div class="resource-detail-item__details">
            <desktop-outlined />
            {{formatValue(virtualmachine)}}
          </div>
        </div>
        <div class="resource-detail-item">
          <div class="resource-detail-item__label">{{ $t('label.disk') }}</div>
          <div class="resource-detail-item__details">
            <hdd-outlined />
            {{formatValue(rootvolume)}} | {{formatValue(datavolume)}}
          </div>
        </div>
        <div class="resource-detail-item">
          <div class="resource-detail-item__label">{{ $t('label.total') }}</div>
          <div class="resource-detail-item__details">
            {{currencysymbol}}
            {{formatValue(total)}}
          </div>
        </div>
      </a-card-grid>
    </a-spin>
    <a-card-grid class="estimatedcost-card-grid" :hoverable="false" style="text-align: center">
      <a-slider
        v-model:value="timevalue"
        :max="maxtimevalue"
      />
      <a-input-number
        class="estimatedcost-input"
        size="small"
        :min="0"
        :max="maxtimevalue"
        v-model:value="timevalue"
      />
      <a-button type="primary" @click="updateEstimatedCost">
        {{ $t('label.calculate') }}
      </a-button>
      <br>
      <a-radio-group v-model:value="timeunit" button-style="solid" size="small" style="margin-top: 12px">
        <a-radio-button value="hour">{{ $t('label.hours') }}</a-radio-button>
        <a-radio-button value="day">{{ $t('label.days') }}</a-radio-button>
        <a-radio-button value="month">{{ $t('label.months') }}</a-radio-button>
      </a-radio-group>
    </a-card-grid>
  </a-card>
</template>

<script>
import ResourceIcon from '@/components/view/ResourceIcon.vue'
import { api } from '@/api'

export default {
  name: 'EstimatedCost',
  components: { ResourceIcon },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      currencysymbol: '$',
      currencylocale: 'en-US',
      timevalue: 0,
      maxtimevalue: 24,
      timeunit: 'hour',
      virtualmachine: 0,
      rootvolume: 0,
      datavolume: 0,
      total: 0
    }
  },
  watch: {
    timeunit: function (val) {
      if (val === 'hour') {
        this.maxtimevalue = 24
      } else if (val === 'day') {
        this.maxtimevalue = this.daysInMonth
      } else if (val === 'month') {
        this.maxtimevalue = 12
      }
    }
  },
  created () {
    this.updateEstimatedCost()
  },
  computed: {
    daysInMonth: function () {
      const today = new Date()
      today.setMonth(today.getMonth() + 1)
      today.setDate(0)
      return today.getDate()
    }
  },
  methods: {
    formatValue (num) {
      return Intl.NumberFormat(this.currencylocale, { minimumFractionDigits: 2, maximumFractionDigits: 2, roundingMode: 'halfEven' }).format(num)
    },

    getFormatFromHypervisor (hypervisor) {
      switch (hypervisor) {
        case 'XenServer':
          return 'VHD'
        case 'KVM':
          return 'QCOW2'
        case 'VMware':
          return 'OVA'
        case 'Ovm':
        case 'Ovm3':
          return 'RAW'
        default:
          return ''
      }
    },

    runningVMQuoting (time, obj) {
      obj.value.host = {
        id: this.resource.hostid || '',
        name: this.resource.hostname || '',
        tags: this.resource.tagsofhost || [],
        isTagARule: this.resource.hostistagarule || null
      }
      obj.value.id = ''
      obj.value.name = this.resource.name || ''
      obj.value.osName = this.resource.ostypename || ''
      obj.value.hypervisorType = this.resource.hypervisor || ''
      obj.value.computeOffering = {
        customized: this.resource.serviceofferingcustomized,
        id: this.resource.serviceofferingid || '',
        name: this.resource.serviceofferingname || ''
      }
      obj.value.computingResources = {
        cpuNumber: this.resource.cpunumber,
        cpuSpeed: this.resource.cpuspeed,
        memory: this.resource.memory
      }
      obj.value.tags = {}
      obj.value.template = {
        id: this.resource.templateid || '',
        name: this.resource.templatename || ''
      }
      return `{"usageType":"RUNNING_VM","volumeToQuote": ${time},"metadata": ${JSON.stringify(obj)}}`
    },

    allocatedVMQuoting (time, obj) {
      obj.value.id = ''
      obj.value.name = this.resource.name || ''
      obj.value.osName = this.resource.ostypename || ''
      obj.value.hypervisorType = this.resource.hypervisor || ''
      obj.value.computeOffering = {
        customized: this.resource.serviceofferingcustomized,
        id: this.resource.serviceofferingid || '',
        name: this.resource.serviceofferingname || ''
      }
      obj.value.tags = {}
      obj.value.template = {
        id: this.resource.templateid || '',
        name: this.resource.templatename || ''
      }
      return `{"usageType":"ALLOCATED_VM","volumeToQuote": ${time},"metadata": ${JSON.stringify(obj)}}`
    },

    rootVolumeQuoting (time, obj) {
      obj.value.diskOffering = {
        id: '',
        name: ''
      }
      obj.value.id = ''
      obj.value.name = ''
      obj.value.provisioningType = ''
      obj.value.volumeFormat = this.getFormatFromHypervisor(this.resource.hypervisor)
      obj.value.storage = {
        id: '',
        name: '',
        scope: null,
        tags: [],
        isTagARule: null
      }
      obj.value.tags = {}
      obj.value.size = 0

      if (this.resource.isoid) {
        obj.value.size = this.resource.diskofferingsize || 0
        obj.value.diskOffering.id = this.resource.diskofferingid || ''
        obj.value.diskOffering.name = this.resource.diskofferingname || ''
        obj.value.provisioningType = this.resource.diskofferingprovisioningtype?.toUpperCase()
      } else {
        if (this.resource.rootdisksizeitem) {
          obj.value.size = this.resource.rootdisksize || 0
          obj.value.diskOffering.id = this.resource.serviceofferingdiskid || ''
          obj.value.diskOffering.name = this.resource.serviceofferingdiskname || ''
          obj.value.provisioningType = this.resource.serviceofferingprovisioningtype?.toUpperCase()
        } else if (this.resource.overridediskofferingid) {
          obj.value.size = this.resource.rootdisksize || 0
          obj.value.diskOffering.id = this.resource.overridediskofferingid
          obj.value.diskOffering.name = this.resource.overridediskofferingname
          obj.value.provisioningType = this.resource.overridediskofferingprovisioningtype?.toUpperCase()
        } else if (this.resource.serviceofferingdisksize) {
          obj.value.size = this.resource.serviceofferingdisksize || 0
          obj.value.diskOffering.id = this.resource.serviceofferingdiskid || ''
          obj.value.diskOffering.name = this.resource.serviceofferingdiskname || ''
          obj.value.provisioningType = this.resource.serviceofferingprovisioningtype?.toUpperCase()
        } else {
          obj.value.size = this.resource.templatesize / 1024 / 1024 / 1024 || 0
          obj.value.diskOffering.id = this.resource.serviceofferingdiskid || ''
          obj.value.diskOffering.name = this.resource.serviceofferingdiskname || ''
          obj.value.provisioningType = this.resource.serviceofferingprovisioningtype?.toUpperCase()
        }
      }

      return `{"usageType":"VOLUME","volumeToQuote": ${obj.value.size * time},"metadata": ${JSON.stringify(obj)}}`
    },

    dataVolumeQuoting (time, obj) {
      if (this.resource.isoid || !this.resource.diskofferingid) {
        return
      }
      obj.value.diskOffering = {
        id: this.resource.diskofferingid || '',
        name: this.resource.diskofferingname || ''
      }
      obj.value.id = ''
      obj.value.name = ''
      obj.value.provisioningType = this.resource.diskofferingprovisioningtype?.toUpperCase()
      obj.value.volumeFormat = this.getFormatFromHypervisor(this.resource.hypervisor)
      obj.value.storage = {
        id: '',
        name: '',
        scope: null,
        tags: [],
        isTagARule: null
      }
      obj.value.tags = {}
      obj.value.size = this.resource.diskofferingsize || this.resource.size

      return `{"usageType":"VOLUME","volumeToQuote": ${obj.value.size ? obj.value.size * time : 0},"metadata": ${JSON.stringify(obj)}}`
    },

    sanitizeDomainPath (path) {
      if (path.charAt(0) === '/') {
        return path
      }
      if (path === 'ROOT') {
        return '/'
      }
      return path.substr(4, path.length - 1) + '/'
    },

    createCommonVariables () {
      const common = {
        account: {
          id: this.resource.accountid || '',
          name: this.resource.accountname || ''
        },
        domain: {
          id: this.resource.domainid || '',
          name: this.resource.domainname || '',
          path: this.sanitizeDomainPath(this.resource.domainpath) || ''
        },
        zone: {
          id: this.resource.zoneid || '',
          name: this.resource.zonename || ''
        },
        resourceType: '',
        value: {
          accountResources: []
        }
      }

      if (this.resource.projectid) {
        common.project = {
          id: this.resource.projectid || '',
          name: this.resource.projectname || ''
        }
      } else {
        common.account.role = {
          id: this.resource.accountroleid || '',
          name: this.resource.accountrolename || '',
          type: this.resource.accountroletype || null
        }
      }
      return common
    },

    buildResourcesToQuote (time) {
      if (this.timeunit === 'day') {
        time *= 24
      } else if (this.timeunit === 'month') {
        time *= this.daysInMonth * 24
      }
      const runningvm = this.runningVMQuoting(time, this.createCommonVariables())
      const allocatedvm = this.allocatedVMQuoting(time, this.createCommonVariables())
      const rootvolume = this.rootVolumeQuoting(time, this.createCommonVariables())
      const datavolume = this.dataVolumeQuoting(time, this.createCommonVariables())
      if (datavolume) {
        return `[${runningvm}, ${allocatedvm}, ${rootvolume}, ${datavolume}]`
      }
      return `[${runningvm}, ${allocatedvm}, ${rootvolume}]`
    },

    async updateEstimatedCost () {
      this.loading = true

      api('quotaResourceQuoting', {
        resourcestoquote: this.buildResourcesToQuote(this.timevalue, 'hour')
      }).then(json => {
        const resp = json?.quotaResourceQuotingresponse?.quoting?.details || []
        const total = json?.quotaResourceQuotingresponse?.quoting?.totalquote || 0
        this.currencysymbol = json?.quotaResourceQuotingresponse?.quoting?.currencysymbol || '$'
        this.currencylocale = json?.quotaResourceQuotingresponse?.quoting?.currencylocale || 'en-US'

        const that = this
        this.virtualmachine = 0
        resp.forEach(function (resourceQuote) {
          if (resourceQuote.quoteid === '0') {
            that.virtualmachine += resourceQuote.quote
          }
          if (resourceQuote.quoteid === '1') {
            that.virtualmachine += resourceQuote.quote
          }
          if (resourceQuote.quoteid === '2') {
            that.rootvolume = resourceQuote.quote
          }
          if (resourceQuote.quoteid === '3') {
            that.datavolume = resourceQuote.quote
          }
        })

        if (resp.length === 3) {
          this.datavolume = 0
        }
        this.total = total
        this.loading = false
      })
    }
  }
}
</script>

<style scoped lang="less">
.estimatedcost-card-grid {
  width: 100%;
  box-shadow: none;
}
.estimatedcost-input {
  width: 50px;
  margin-right: 10px;
}
</style>

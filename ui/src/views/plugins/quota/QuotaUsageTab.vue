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
  <div>
    <filter-quota-data-by-period-view @fetchData="fetchData"/>

    <div v-if="dataSource.length > 0">
      <hr class="m-20-0" />
      <div style="font-size: 18px">
        <strong> {{ $t('label.quota.usage.types.summary') }} </strong>
      </div>
      <export-to-csv-button :action="exportDataToCsv" />
      <bar-chart :chart-options="getUsageTypeChartOptions()" :chart-data="getUsageTypeChartData()"/>
      <a-table
        size="small"
        :loading="loading"
        :columns="columns"
        :dataSource="dataSource.filter(row => row.quota != 0)"
        :rowKey="record => record.name"
        :pagination="false"
        :scroll="{ y: '55vh' }">
        <template #nameRedirect="props">
          <a @click="handleSelectedTypeChange(`${props.record.type}-${props.record.name}`)">{{ $t(props.text) }}</a>
        </template>
        <template #unit="{ text }">
          {{ $t(text) }}
        </template>
        <template #quota="{ text }">
          {{ parseFloat(text).toFixed(2) }}
        </template>
        <template #footer >
          <div style="text-align: right;">
            {{ $t('label.currency') }}: <b>{{ currency }}</b><br/>
            {{ $t('label.quota.total.consumption') }}: <b>{{ totalQuota }}</b>
          </div>
        </template>
      </a-table>

      <hr class="m-20-0" id="resource-by-type" />
      <strong>
        <tooltip-label style="font-size: 18px" :title="$t('label.quota.usage.resources.by.type')" :tooltip="$t('message.quota.usage.resource.warn')"/>
      </strong>
      <a-select
        v-model:value="selectedType"
        class="w-100"
        style="margin: 5px 0 10px 0px"
        show-search
        v-model="selectedType"
        @change="handleSelectedTypeChange">
        <a-select-option
          v-for="quotaType of getQuotaTypesFiltered()"
          :value="`${quotaType.id}-${quotaType.type}`"
          :key="quotaType.id">
          {{ $t(quotaType.type) }}
        </a-select-option>
      </a-select>
      <export-to-csv-button v-if="dataSourceResource.length > 0" :action="exportResourcesToCsv" :label="`label.export.resources.csv`" />
      <a-table
        size="small"
        :loading="loadingResources"
        :columns="resourceColumns"
        :dataSource="dataSourceResource"
        :rowKey="(record) => record.displayname"
        :pagination="false"
        :scroll="{ y: '55vh' }">
        <template #title v-if="dataSourceResource.length > 0">
          <div>{{ $t('label.currency') }}: <b>{{ currency }}</b></div>
        </template>
        <template #displayName="props">
          <span v-if="!props.text">
            -
          </span>
          <span v-if="!props.text === '<untraceable>' || !props.record.resourceid">
            {{ props.text }}
          </span>
          <a v-else @click="handleSelectedResourceChange(props.record.resourceid)">
            {{ props.text }}
          </a>
        </template>
        <template #quota="{ text }">
          {{ parseFloat(text).toFixed(2) }}
        </template>
      </a-table>

      <hr class="m-20-0" id="details-by-resource" />
      <strong>
        <tooltip-label style="font-size: 18px" :title="$t('label.quota.usage.details.by.resource')"/>
      </strong>
      <a-select
        v-model:value="selectedResource"
        class="w-100"
        style="margin: 5px 0 10px 0px"
        show-search
        v-model="selectedResource"
        @change="handleSelectedResourceChange"
        :disabled="getResources().length == 0">
        <a-select-option
          v-for="item of getResources()"
          :value="item.id"
          :key="item.id">
          {{ $t(item.name) }}
        </a-select-option>
      </a-select>
      <export-to-csv-button v-if="dataSourceResourceDetails.length > 0" :action="exportResourceDetailsToCsv" :label="`label.export.details.csv`" />
      <a-table
        size="small"
        :loading="loadingResourceDetails"
        :columns="resourceDetailsColumns"
        :dataSource="dataSourceResourceDetails"
        :rowKey="record => record.tariffname + '-' + record.startDate"
        :pagination="false"
        :scroll="{ y: '55vh' }">
        <template #title v-if="dataSourceResourceDetails.length > 0">
          <div>{{ $t('label.currency') }}: <b>{{ currency }}</b></div>
        </template>
        <template #tariffName="props">
          <a v-if="'quotaTariffList' in $store.getters.apis" :href="`#/quotatariff/${props.record.tariffid}`" target="_blank">
            {{ props.text }}
          </a>
          <span v-else>
            {{ props.text }}
          </span>
        </template>
        <template #endDate="{ text }">
          {{ $toLocaleDate(text) }}
        </template>
        <template #startDate="{ text }">
          {{ $toLocaleDate(text) }}
        </template>
        <template #quota="{ text }">
          {{ parseFloat(text).toFixed(2) }}
        </template>
      </a-table>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import FilterQuotaDataByPeriodView from './FilterQuotaDataByPeriodView.vue'
import BarChart from '@/components/view/charts/BarChart.vue'
import ExportToCsvButton from '@/components/view/buttons/ExportToCsvButton.vue'
import { getChartColorObject } from '@/utils/chart'
import { getByQuotaTypeByType, getQuotaTypes } from '@/utils/quota'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import * as exportUtils from '@/utils/export'
import * as dateUtils from '@/utils/date'

export default {
  name: 'QuotaUsageTab',
  components: {
    FilterQuotaDataByPeriodView,
    BarChart,
    ExportToCsvButton,
    TooltipLabel
  },
  data () {
    return {
      dataSource: [],
      selectedType: '',
      loadingResources: false,
      dataSourceResource: [],
      selectedResource: '',
      loadingResourceDetails: false,
      dataSourceResourceDetails: [],
      startDate: undefined,
      endDate: undefined
    }
  },
  computed: {
    columns () {
      return [
        {
          title: this.$t('label.quota.type.name'),
          dataIndex: 'name',
          width: 'calc(100% / 3)',
          slots: { customRender: 'nameRedirect' },
          sorter: (a, b) => a.name.localeCompare(b.name)
        },
        {
          title: this.$t('label.quota.type.unit'),
          dataIndex: 'unit',
          width: 'calc(100% / 3)',
          slots: { customRender: 'unit' },
          sorter: (a, b) => a.unit.localeCompare(b.unit)
        },
        {
          title: this.$t('label.quota.usage'),
          dataIndex: 'quota',
          width: 'calc(100% / 3)',
          slots: { customRender: 'quota' },
          sorter: (a, b) => a.quota - b.quota,
          defaultSortOrder: 'descend'
        }
      ]
    },
    resourceColumns () {
      return [
        {
          title: this.$t('label.resource'),
          dataIndex: 'displayname',
          width: '50%',
          slots: { customRender: 'displayName' },
          sorter: (a, b) => a.displayname.localeCompare(b.displayname),
          defaultSortOrder: 'ascend'
        },
        {
          title: this.$t('label.quota.usage.quota.consumed'),
          dataIndex: 'quotaconsumed',
          width: '50%',
          slots: { customRender: 'quota' },
          sorter: (a, b) => a.quotaconsumed - b.quotaconsumed
        }
      ]
    },
    resourceDetailsColumns () {
      return [
        {
          title: this.$t('label.quota.tariff'),
          dataIndex: 'tariffname',
          slots: { customRender: 'tariffName' },
          sorter: (a, b) => a.tariffname.localeCompare(b.tariffname)
        },
        {
          title: this.$t('label.start.date'),
          dataIndex: 'startDate',
          slots: { customRender: 'startDate' },
          sorter: (a, b) => a.startDate.localeCompare(b.startDate),
          defaultSortOrder: 'descend'
        },
        {
          title: this.$t('label.end.date'),
          dataIndex: 'endDate',
          slots: { customRender: 'endDate' },
          sorter: (a, b) => a.endDate.localeCompare(b.endDate)
        },
        {
          title: this.$t('label.quota.usage.quota.consumed'),
          dataIndex: 'quotaused',
          slots: { customRender: 'quota' },
          sorter: (a, b) => a.quotaused - b.quotaused
        }
      ]
    }
  },
  methods: {
    async fetchData (startDate, endDate) {
      if (this.loading) return

      this.startDate = dateUtils.getMomentFormattedAndNormalized({ value: startDate })
      this.endDate = dateUtils.getMomentFormattedAndNormalized({ value: endDate })
      this.loading = true
      this.dataSource = []
      this.dataSourceResource = []
      this.dataSourceResourceDetails = []
      this.selectedResource = ''
      this.selectedType = ''

      try {
        const quotaStatement = await this.getQuotaStatement({
          startDate: this.startDate,
          endDate: this.endDate
        })

        if (!quotaStatement) {
          return
        }

        this.dataSource = quotaStatement.quotausage.filter(row => row.quota !== 0)
        this.currency = quotaStatement.currency
        this.totalQuota = quotaStatement.totalquota.toFixed(2)
      } finally {
        this.loading = false
      }
    },
    async fetchResourceData () {
      if (this.selectedType === '' || this.loadingResources) return

      this.dataSourceResource = []
      this.loadingResources = true

      try {
        const quotaStatement = await this.getQuotaStatement({
          startDate: this.startDate,
          endDate: this.endDate,
          showResources: true,
          type: this.selectedType.split('-')[0]
        })

        this.dataSourceResource = quotaStatement.quotausage[0].resources
        this.dataSourceResource = this.dataSourceResource.filter(row => row.quotaconsumed !== 0)
      } finally {
        this.loadingResources = false
      }
    },
    async fetchResourceDetailsData () {
      if (this.selectedResource === '' || this.loadingResourceDetails) return

      this.dataSourceResourceDetails = []
      this.loadingResourceDetails = true

      try {
        this.dataSourceResourceDetails = await api('quotaStatementDetails', {
          startDate: this.startDate,
          endDate: this.endDate,
          usageType: this.selectedType.split('-')[0],
          id: this.selectedResource
        }).then(json => json.quotastatementdetailsresponse?.quotausagedetails?.items || [])
          .catch(error => { error && this.$notification.info({ message: this.$t('message.request.no.data') }) })

        this.dataSourceResourceDetails = this.dataSourceResourceDetails.map(detail => ({
          ...detail,
          startDate: dateUtils.getMomentFormattedAndNormalized({ value: detail.startdate }),
          endDate: dateUtils.getMomentFormattedAndNormalized({ value: detail.enddate })
        })).filter(row => row.quotaused !== 0)
      } finally {
        this.loadingResourceDetails = false
      }
    },
    async getQuotaStatement (apiParams) {
      const params = {
        domainid: this.$route.query?.domainid,
        account: this.$route.query?.account,
        ...apiParams
      }

      return await api('quotaStatement', params)
        .then(json => json.quotastatementresponse.statement || {})
        .catch(error => {
          if (error) {
            this.$notification.info({ message: this.$t('message.request.no.data') })
          }
        })
    },
    getUsageTypeChartOptions () {
      return { responsive: true }
    },
    getUsageTypeChartData () {
      const datasets = []
      for (const row of this.dataSource) {
        datasets.push({
          label: this.$t(row.name),
          data: [row.quota],
          ...getChartColorObject(getByQuotaTypeByType(row.name).chartColor)
        })
      }

      return { labels: [this.$t('label.quota.type.name')], datasets }
    },
    exportDataToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSource,
        keys: ['type', 'name', 'unit', 'quota'],
        fileName: `quota-usage-of-user-${this.$route.params.id}-between-${this.startDate}-and-${this.endDate}`
      })
    },
    exportResourcesToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSourceResource.map(row => ({ ...row, name: row.displayname })),
        keys: ['resourceid', 'name', 'quotaconsumed'],
        fileName: `quota-usage-of-resources-of-type-${this.selectedType}-of-user-${this.$route.params.id}-between-${this.startDate}-and-${this.endDate}`
      })
    },
    exportResourceDetailsToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSourceResourceDetails.map(row => ({
          ...row,
          startdate: dateUtils.getMomentFormattedAndNormalized({ value: row.startdate, keepMoment: false }),
          enddate: dateUtils.getMomentFormattedAndNormalized({ value: row.enddate, keepMoment: false })
        })),
        keys: ['tariffid', 'tariffname', 'startdate', 'enddate', 'quotaused'],
        fileName: `detailed-quota-usage-of-resource-${this.selectedResource}-of-type-${this.selectedType}-of-user-${this.$route.params.id}-between-${this.startDate}-and-${this.endDate}`
      })
    },
    getQuotaTypesFiltered () {
      const quotaTypesRetrieved = this.dataSource.map(item => item.name)
      return getQuotaTypes().filter((item) => quotaTypesRetrieved.includes(item.type))
    },
    getResources () {
      return this.dataSourceResource.filter(item => item.resourceid).map(item => ({ id: item.resourceid, name: item.displayname }))
    },
    async handleSelectedTypeChange (value) {
      this.selectedType = value
      this.selectedResource = ''
      this.dataSourceResourceDetails = []
      document.getElementById('resource-by-type').scrollIntoView({ behavior: 'smooth' })
      await this.fetchResourceData()
    },
    async handleSelectedResourceChange (value) {
      if (!value) return

      this.selectedResource = value
      document.getElementById('details-by-resource').scrollIntoView({ behavior: 'smooth' })
      await this.fetchResourceDetailsData()
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/common/common.scss';
</style>

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
      <export-to-csv-button :action="exportDataToCsv" />
      <bar-chart :chart-options="getCreditsChartOptions()" :chart-data="getCreditsChartData()"/>

      <a-table
        size="small"
        :loading="loading"
        :columns="columns"
        :dataSource="dataSource"
        :rowKey="record => record.creditedon"
        :pagination="false"
        :scroll="{ y: '55vh' }">
        <template #title>
          {{ $t('label.currency') }}: <b>{{ currency }}</b>
        </template>
        <template #creditedOn="{ text }">
          {{ $toLocaleDate(text) }}
        </template>
        <template #credit="{ text }">
          {{ parseFloat(text).toFixed(2) }}
        </template>
      </a-table>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import BarChart from '@/components/view/charts/BarChart.vue'
import * as dateUtils from '@/utils/date'
import * as exportUtils from '@/utils/export'
import FilterQuotaDataByPeriodView from './FilterQuotaDataByPeriodView.vue'
import ExportToCsvButton from '@/components/view/buttons/ExportToCsvButton.vue'
import * as chartUtils from '@/utils/chart'

export default {
  name: 'QuotaCreditTab',
  components: {
    FilterQuotaDataByPeriodView,
    BarChart,
    ExportToCsvButton
  },
  data () {
    return {
      loading: false,
      currency: '',
      dataSource: [],
      startDate: undefined,
      endDate: undefined
    }
  },
  computed: {
    columns () {
      return [
        {
          title: this.$t('label.date'),
          dataIndex: 'creditedon',
          width: 'calc(100% / 2)',
          sorter: (a, b) => a.creditedon.localeCompare(b.creditedon),
          defaultSortOrder: 'descend',
          slots: { customRender: 'creditedOn' }
        },
        {
          title: this.$t('label.credit'),
          dataIndex: 'credit',
          width: 'calc(100% / 2)',
          sorter: (a, b) => a.credit - b.credit,
          slots: { customRender: 'credit' }
        }
      ]
    }
  },
  methods: {
    async fetchData (startDate, endDate) {
      if (this.loading) return

      this.startDate = dateUtils.getMomentFormattedAndNormalized({ value: startDate })
      this.endDate = dateUtils.getMomentFormattedAndNormalized({ value: endDate })
      this.dataSource = []
      this.loading = true

      try {
        const data = await this.getQuotaCreditsList()
        if (!data) {
          return
        }
        this.currency = data[0]?.currency
        this.dataSource = data.map(row => ({
          ...row,
          date: dateUtils.getMomentFormattedAndNormalized({ value: row.creditedon, keepMoment: false })
        }))
      } finally {
        this.loading = false
      }
    },
    async getQuotaCreditsList () {
      const params = {
        domainid: this.$route.query?.domainid,
        accountid: this.$route.query?.accountid,
        startdate: this.startDate,
        enddate: this.endDate
      }

      return await api('quotaCreditsList', params)
        .then(json => json.quotacreditslistresponse.credit || {})
        .catch(error => { error && this.$notification.info({ message: this.$t('message.request.no.data') }) })
    },
    exportDataToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSource,
        keys: ['creditorname', 'date', 'credit'],
        fileName: `credits-of-user-${this.$route.params.id}-between-${this.startDate}-and-${this.endDate}`
      })
    },
    getCreditsChartData () {
      const datasets = []

      const data = []
      const res = {}
      this.dataSource.map(value => {
        const date = this.$toLocalDate(value.creditedon).split('T')[0]

        if (!res[date]) {
          res[date] = { date, credit: 0 }
          data.push(res[date])
        }

        res[date].credit += value.credit
        return res
      })

      datasets.push({
        label: this.$t('label.credit'),
        data: data.map(row => row.credit),
        ...chartUtils.getChartColorObject()
      })

      return {
        labels: data.map(row => row.date),
        datasets
      }
    },
    getCreditsChartOptions () {
      return {
        scales: {
          xAxis: {
            type: 'time',
            time: {
              unit: chartUtils.getUnitToTimeCartesianAxis('day', this.dataSource.length),
              displayFormats: chartUtils.defaultDisplayFormats
            }
          }
        },
        plugins: {
          tooltip: {
            callbacks: {
              title: (tooltipItem) => dateUtils.moment(tooltipItem[0].label).format(chartUtils.defaultDisplayFormats.day),
              label: (tooltipItem) => parseFloat(tooltipItem.raw).toFixed(2)
            }
          }
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/common/common.scss';
</style>

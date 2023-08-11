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

import { shallowRef, defineAsyncComponent } from 'vue'
import { i18n } from '@/locales'

export default {
  name: 'quota',
  title: 'label.quota',
  icon: 'pie-chart-outlined',
  docHelp: 'plugins/quota.html',
  permission: ['quotaSummary'],
  children: [
    {
      name: 'quotasummary',
      title: 'label.quota.summary',
      icon: 'bars-outlined',
      permission: ['quotaSummary'],
      filters: ['all', 'activeaccounts', 'removedaccounts'],
      customParamHandler: (params, query) => {
        switch (query.filter) {
          case 'all':
            params.accountstatetoshow = 'ALL'
            break
          case 'removedaccounts':
            params.accountstatetoshow = 'REMOVED'
            break
          default:
            params.accountstatetoshow = 'ACTIVE'
        }

        return params
      },
      tabs: [
        {
          name: 'quota.statement.quota',
          component: shallowRef(defineAsyncComponent(() => import('@/views/plugins/quota/QuotaUsageTab.vue')))
        },
        {
          name: 'quota.statement.balance',
          component: shallowRef(defineAsyncComponent(() => import('@/views/plugins/quota/QuotaBalanceTab.vue')))
        },
        {
          name: 'quota.credits',
          component: shallowRef(defineAsyncComponent(() => import('@/views/plugins/quota/QuotaCreditTab.vue')))
        }
      ],
      columns: [
        'account',
        {
          field: 'state',
          customTitle: 'accountState',
          state: (record) => record.accountremoved || (record.projectid && record.projectremoved) ? 'disabled' : 'enabled'
        },
        {
          field: 'quotastate',
          customTitle: 'quotaState',
          quotastate: (record) => !record.quotaenabled || record.accountremoved || (record.projectid && record.projectremoved) ? 'disabled' : 'enabled'
        },
        'domain',
        'currency',
        {
          field: 'balance',
          customTitle: 'quota.current.balance'
        }
      ],
      actions: [
        {
          api: 'quotaCredits',
          icon: 'plus-outlined',
          docHelp: 'plugins/quota.html#quota-credits',
          label: 'label.quota.add.credits',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/plugins/quota/AddQuotaCredit.vue')))
        }
      ]
    },
    {
      name: 'quotatariff',
      title: 'label.quota.tariff',
      icon: 'credit-card-outlined',
      docHelp: 'plugins/quota.html#quota-tariff',
      permission: ['quotaTariffList'],
      customParamHandler: (params, query) => {
        params.listall = false
        if (['all', 'removed'].includes(query.filter) || params.uuid) {
          params.listall = true
        }

        if (['removed'].includes(query.filter)) {
          params.listonlyremoved = true
        }

        return params
      },
      columns: [
        'name',
        {
          field: 'usageName',
          customTitle: 'usageType',
          usageName: (record) => i18n.global.t(record.usageName)
        },
        {
          field: 'usageUnit',
          customTitle: 'usageUnit',
          usageUnit: (record) => i18n.global.t(record.usageUnit)
        },
        {
          field: 'tariffValue',
          customTitle: 'quota.tariff.value'
        },
        {
          field: 'processingPeriod',
          customTitle: 'quota.tariff.processingperiod',
          processingPeriod: (record) => i18n.global.t(record.processingPeriod)
        },
        {
          field: 'hasActivationRule',
          customTitle: 'quota.tariff.hasactivationrule',
          hasActivationRule: (record) => record.activationRule ? i18n.global.t('label.yes') : i18n.global.t('label.no')
        },
        {
          field: 'executionPosition',
          customTitle: 'quota.tariff.position',
          executionPosition: (record) => record.position
        },
        {
          field: 'effectiveDate',
          customTitle: 'start.date'
        },
        {
          field: 'endDate',
          customTitle: 'end.date'
        },
        'removed'
      ],
      details: [
        'uuid',
        'name',
        'description',
        {
          field: 'usageName',
          customTitle: 'usageType'
        },
        'usageUnit',
        {
          field: 'tariffValue',
          customTitle: 'quota.tariff.value'
        },
        {
          field: 'processingPeriod',
          customTitle: 'quota.tariff.processingperiod'
        },
        {
          field: 'executeOn',
          customTitle: 'quota.tariff.executeon'
        },
        {
          field: 'effectiveDate',
          customTitle: 'start.date'
        },
        {
          field: 'endDate',
          customTitle: 'end.date'
        },
        'removed',
        {
          field: 'activationRule',
          customTitle: 'quota.tariff.activationrule'
        }
      ],
      filters: ['all', 'active', 'removed'],
      searchFilters: ['usagetype'],
      actions: [
        {
          api: 'quotaTariffCreate',
          icon: 'plus-outlined',
          label: 'label.action.quota.tariff.create',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/plugins/quota/CreateQuotaTariff.vue')))
        },
        {
          api: 'quotaTariffUpdate',
          icon: 'edit-outlined',
          label: 'label.action.quota.tariff.edit',
          dataView: true,
          popup: true,
          show: (record) => !record.removed,
          component: shallowRef(defineAsyncComponent(() => import('@/views/plugins/quota/EditQuotaTariff.vue')))
        },
        {
          api: 'quotaTariffDelete',
          icon: 'delete-outlined',
          label: 'label.action.quota.tariff.remove',
          message: 'message.action.quota.tariff.remove',
          dataView: true,
          show: (record) => !record.removed
        }
      ]
    },
    {
      name: 'quotaemailtemplate',
      title: 'label.emailtemplate',
      icon: 'mail-outlined',
      permission: ['quotaEmailTemplateList'],
      columns: ['templatetype', 'templatesubject', 'templatebody'],
      details: ['templatetype', 'templatesubject', 'templatebody'],
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/views/plugins/quota/EmailTemplateDetails.vue')))
      }]
    }
  ]
}

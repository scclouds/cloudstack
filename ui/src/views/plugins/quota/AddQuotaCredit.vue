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
  <a-spin :spinning="loading">
    <a-form
      class="form"
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit">
      <a-form-item v-if="'listDomains' in $store.getters.apis" ref="domainid" name="domainid">
        <template #label>
          <tooltip-label :title="$t('label.domain')" :tooltip="apiParams.domainid.description"/>
        </template>
        <a-select
          showSearch
          v-model:value="form.domainid"
          :loading="domainLoading"
          :placeholder="this.$t('label.domainid')"
          @change="val => { this.handleDomainChange(val) }">
          <a-select-option v-for="domain in this.domainList" :value="`${domain.id}|${domain.path}`" :key="domain.id">
            {{ domain.path }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item v-if="'listDomains' in $store.getters.apis" ref="account" name="account">
        <template #label>
          <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account.description"/>
        </template>
        <a-select
          showSearch
          v-model:value="form.account"
          :placeholder="this.$t('label.account')">
          <a-select-option v-for="account in accountList" :value="account.name" :key="account.id">
            {{ account.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="value" name="value">
        <template #label>
          <tooltip-label :title="$t('label.value')" :tooltip="apiParams.value.description"/>
        </template>
        <a-input-number
          v-model:value="form.value"
          :placeholder="$t('placeholder.quota.credit.add.value')" />
      </a-form-item>
      <a-form-item ref="min_balance" name="min_balance">
        <template #label>
          <tooltip-label :title="$t('label.min_balance')" :tooltip="apiParams.min_balance.description"/>
        </template>
        <a-input-number
          v-model:value="form.min_balance"
          :placeholder="$t('placeholder.quota.credit.add.min_balance')" />
      </a-form-item>
      <a-form-item ref="quota_enforce" name="quota_enforce">
        <template #label>
          <tooltip-label :title="$t('label.quota.enforce')" :tooltip="apiParams.quota_enforce.description"/>
        </template>
        <a-switch
          v-model:checked="form.quota_enforce" />
      </a-form-item>
      <a-form-item ref="postingDate" name="postingDate">
        <template #label>
          <tooltip-label :title="$t('label.posting.date')" :tooltip="apiParams.postingdate.description"/>
        </template>
        <a-date-picker
          v-model:value="form.postingDate"
          :disabled-date="disabledPostingDate"
          :placeholder="$t('placeholder.quota.credit.posting.date')"
          show-time />
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { ref, reactive, toRaw } from 'vue'
import { mixinForm } from '@/utils/mixin'
import { moment, getMomentFormattedAndNormalized } from '@/utils/date'

export default {
  name: 'AddQuotaCredit',
  mixins: [mixinForm],
  components: {
    TooltipLabel
  },
  data () {
    return {
      loading: false,
      domainList: [],
      accountList: [],
      domainId: undefined,
      domainLoading: false,
      domainError: false
    }
  },
  inject: ['parentFetchData'],
  beforeCreate () {
    this.apiParams = this.$getApiParams('quotaCredits')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        domainid: [{ required: true, message: this.$t('message.action.quota.credit.add.error.domainidrequired') }],
        account: [{ required: true, message: this.$t('message.action.quota.credit.add.error.accountrequired') }],
        value: [{ required: true, message: this.$t('message.action.quota.credit.add.error.valuerequired') }]
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return

      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        values.domainid = this.domainId

        if (values.postingDate) {
          values.postingDate = getMomentFormattedAndNormalized({ value: values.postingDate })
        }

        this.loading = true
        api('quotaCredits', values).then(response => {
          this.$message.success(this.$t('message.action.quota.credit.add.success', { credit: values.value, account: values.account }))
          this.parentFetchData()
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleDomainChange (domain) {
      this.domainId = domain?.split('|')[0]
      if ('listAccounts' in this.$store.getters.apis) {
        this.fetchAccounts()
      }
    },
    fetchData () {
      if ('listDomains' in this.$store.getters.apis) {
        this.fetchDomains()
      }
    },
    fetchDomains () {
      this.domainLoading = true
      api('listDomains', {
        listAll: true,
        details: 'min'
      }).then(response => {
        this.domainList = response.listdomainsresponse.domain

        if (this.domainList[0]) {
          this.handleDomainChange(null)
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.domainLoading = false
      })
    },
    fetchAccounts () {
      api('listAccounts', {
        domainid: this.domainId
      }).then(response => {
        this.accountList = response.listaccountsresponse.account || []
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    disabledPostingDate (current) {
      return current > moment().endOf('day')
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/objects/form.scss';
</style>

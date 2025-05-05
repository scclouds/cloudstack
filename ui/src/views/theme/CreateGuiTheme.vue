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
      <a-form-item ref="name" name="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-model:value="form.name"
          v-focus="true"
          :placeholder="$t('label.name')"
          :max-length="2048"/>
      </a-form-item>
      <a-form-item ref="description" name="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        </template>
        <a-textarea
          v-model:value="form.description"
          :placeholder="$t('label.description')"
          :max-length="4096" />
      </a-form-item>
      <a-form-item
        :label="$t('label.accounts')"
        name="accountids"
        ref="accountids">
        <a-select
          v-model:value="form.accountids"
          :loading="accounts.loading"
          mode="multiple"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :placeholder="$t('placeholder.gui.theme.accounts')"
          :dropdownMatchSelectWidth="true"
          class="selector"
          @change="form.ispublic = isPublic">
          <a-select-option v-for="account in accounts.options" :key="account.id" :label="account.name || account.description">
            <span>
              <team-outlined style="margin-right: 5px" />
              {{ account.name || account.email }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        :label="$t('label.domains')"
        name="domainids"
        ref="domainids">
        <a-select
          v-model:value="form.domainids"
          :loading="domains.loading"
          mode="multiple"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :placeholder="$t('placeholder.gui.theme.domains')"
          class="selector"
          @change="form.ispublic = isPublic">
          <a-select-option v-for="domain in domains.options" :key="domain.id" :label="domain.name || domain.description">
            <span>
              <block-outlined style="margin-right: 5px" />
              {{ domain.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="css" name="css">
        <template #label>
          <tooltip-label :title="$t('label.css')" :tooltip="apiParams.css.description"/>
        </template>
        <a-textarea
          v-model:value="form.css"
          :placeholder="apiParams.css.description"
          :max-length="65535" />
      </a-form-item>
      <a-form-item ref="jsonconfiguration" name="jsonconfiguration">
        <template #label>
          <tooltip-label :title="$t('label.jsonconfiguration')" :tooltip="apiParams.jsonconfiguration.description"/>
        </template>
        <a-textarea
          v-model:value="form.jsonconfiguration"
          :placeholder="apiParams.jsonconfiguration.description"
          :max-length="65535" />
      </a-form-item>
      <a-form-item ref="commonnames" name="commonnames">
        <template #label>
          <tooltip-label :title="$t('label.commonnames')" :tooltip="apiParams.commonnames.description"/>
        </template>
        <a-textarea
          v-model:value="form.commonnames"
          :placeholder="apiParams.commonnames.description"
          :max-length="65535" />
      </a-form-item>
      <a-form-item ref="customlabelspath" name="customlabelspath">
        <template #label>
          <tooltip-label :title="$t('label.customlabelspath')" :tooltip="apiParams.customlabelspath.description"/>
        </template>
        <a-textarea
          v-model:value="form.customlabelspath"
          :placeholder="apiParams.customlabelspath.description"
          :max-length="65535" />
      </a-form-item>
      <a-form-item name="recursivedomains" ref="recursivedomains">
        <template #label>
          <tooltip-label :title="$t('label.recursivedomains')" :tooltip="apiParams.recursivedomains.description"/>
        </template>
        <a-switch v-model:checked="form.recursivedomains" />
      </a-form-item>
      <a-form-item name="public" ref="public">
        <template #label>
          <tooltip-label :title="$t('label.ispublic')" :tooltip="apiParams.ispublic.description"/>
        </template>
        <a-switch v-model:checked="form.ispublic" @click="handleChangeIsPublic" />
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
import { ref, reactive, toRaw } from 'vue'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { mixinForm } from '@/utils/mixin'
import { applyCustomGuiTheme } from '@/utils/guiTheme'

export default {
  name: 'CreateGuiTheme',
  mixins: [mixinForm],
  components: {
    TooltipLabel
  },
  data () {
    return {
      loading: false,
      accounts: {},
      domains: {},
      selectedAccounts: [],
      selectedDomains: []
    }
  },
  computed: {
    isPublic () {
      return this.form.accountids.length === 0 && this.form.domainids.length === 0
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createGuiTheme')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  inject: ['parentFetchData'],
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        ispublic: true,
        accountids: [],
        domainids: []
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    fetchData () {
      this.fetchAccounts()
      this.fetchDomains()
    },
    fetchAccounts () {
      this.accounts.loading = true
      this.accounts.options = []
      const params = {
        listall: true
      }

      api('listAccounts', params).then(response => {
        this.accounts.options = response.listaccountsresponse.account
      }).finally(() => {
        this.accounts.loading = false
      })
    },
    fetchDomains () {
      this.domains.loading = true
      this.domains.options = []
      const params = {
        listall: true
      }

      api('listDomains', params).then(response => {
        this.domains.options = response.listdomainsresponse.domain
      }).finally(() => {
        this.domains.loading = false
      })
    },
    handleChangeIsPublic () {
      if (this.form.ispublic) {
        this.selectedAccounts = this.form.accountids
        this.selectedDomains = this.form.domainids
        this.form.accountids = []
        this.form.domainids = []
      } else {
        this.form.accountids = this.selectedAccounts
        this.form.domainids = this.selectedDomains
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return

      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        const params = {}
        this.loading = true

        for (const [key, value] of Object.entries(values)) {
          if (value) {
            params[key] = value
          }
        }

        api('createGuiTheme', {}, 'POST', params).then(() => {
          this.$message.success(this.$t('message.success.create.gui.theme', { guiTheme: params.name }))
          this.parentFetchData()
          this.closeModal()
          applyCustomGuiTheme(this.$store.getters.userInfo.accountid, this.$store.getters.userInfo.domainid)
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
    }
  }
}
</script>

<style lang="scss" scoped>
.form {
  width: 80vw;

  @media (min-width: 500px) {
    width: 35vw;
  }
}
</style>

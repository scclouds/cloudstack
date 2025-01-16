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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-modal
      v-if="showAddKeyPair"
      :visible="showAddKeyPair"
      :closable="true"
      :maskClosable="false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      style="top: 20px;"
      width="50vw"
      @cancel="closeModal"
      @ok="handleSubmit"
      :ok-button-props="{props: { type: 'default' } }"
      :cancel-button-props="{props: { type: 'primary' } }"
      centered>
    <template #title>
      {{ $t('label.action.create.api.key') }}
    </template>
      <a-spin :spinning="loading">
        <a-form
          :ref="formRef"
          :model="form"
          layout="vertical"
          @finish="handleSubmit">
          <a-form-item name="name" ref="name">
            <template #label>
              <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
            </template>
            <a-input
              v-focus="true"
              v-model:value="form.name" />
          </a-form-item>
          <a-form-item name="description" ref="description">
            <template #label>
              <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
            </template>
            <a-input
              v-model:value="form.description" />
          </a-form-item>
          <a-row>
            <a-form-item ref="startDate" name="startDate">
              <template #label>
                <tooltip-label :title="$t('label.start.date')" :tooltip="apiParams.startdate.description"/>
              </template>
              <a-date-picker
                v-model:value="form.startDate"
                :disabled-date="disabledStartDate"
                show-time
              />
            </a-form-item>
            <a-form-item ref="endDate" name="endDate" style="margin: 0 8px">
              <template #label>
                <tooltip-label :title="$t('label.end.date')" :tooltip="apiParams.enddate.description"/>
              </template>
              <a-date-picker
                :disabled-date="disabledEndDate"
                v-model:value="form.endDate"
                show-time />
            </a-form-item>
          </a-row>
          <a-form-item>
            <template #label>
              <tooltip-label :title="$t('label.rules')" :tooltip="apiParams.rules.description"/>
            </template>
            <key-permission-table
              :resource="resource"
              @update-rules="updateRules"/>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import KeyPermissionTable from '@/views/iam/KeyPermissionTable.vue'
import { dayjs, parseDayJsObject } from '@/utils/date'

export default {
  name: 'GenerateApiKeyPair',
  components: {
    TooltipLabel,
    KeyPermissionTable
  },
  props: {
    showAddKeyPair: {
      type: Boolean,
      default: false
    },
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      rules: [],
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('registerUserKeys')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    buildRequest () {
      const values = toRaw(this.form)
      this.loading = true
      const params = {
        name: values.name,
        id: this.resource.id,
        description: values.description ? values.description : null,
        startdate: values.startDate ? parseDayJsObject({ value: values.startDate }) : null,
        endDate: values.endDate ? parseDayJsObject({ value: values.endDate }) : null
      }
      var data = {}
      for (var r in this.rules) {
        var rule = this.rules[r]
        data['rules[' + r + '].rule'] = rule.rule ? rule.rule : ''
        data['rules[' + r + '].permission'] = rule.permission ? rule.permission : 'deny'
        data['rules[' + r + '].description'] = rule.description ? rule.description : ''
      }
      return [params, data]
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        var request = this.buildRequest()
        var params = request[0]
        var data = request[1]
        this.loading = true
        api('registerUserKeys', params, 'POST', data).then(response => {
          this.$pollJob({
            jobId: response.registeruserkeysresponse.jobid,
            successMessage: `${this.$t('message.success.register.user.keypair')} ${this.$t('label.for')} user ${this.resource.id}`,
            successMethod: () => {
              this.fetchData()
            },
            errorMessage: this.$t('message.register.keypair.failed'),
            errorMethod: () => {
              this.fetchData()
            },
            loadingMessage: `${this.$t('label.registering.keypair')} ${this.$t('label.for')} user ${this.resource.id} ${this.$t('label.is.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
            duration: 0
          })
        }).finally(() => {
          this.loading = false
          this.closeModal()
        })
      })
    },
    closeModal () {
      this.$emit('close-modal')
    },
    handleCancel () {
      this.$emit('handle-cancel')
    },
    fetchData () {
      this.$emit('fetch-data')
    },
    updateRules (rules) {
      this.rules = rules
    },
    disabledStartDate (current) {
      return current < dayjs().startOf('day')
    },
    disabledEndDate (current) {
      return current < (this.form.startDate || dayjs().startOf('day'))
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 600px) {
      width: 450px;
    }
  }
</style>

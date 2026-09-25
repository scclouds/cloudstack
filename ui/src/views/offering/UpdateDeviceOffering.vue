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
  <div class="form-layout" @keyup.ctrl.enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="this.rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-focus="true"
            v-model:value="form.name"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item name="description" ref="description">
          <template #label>
            <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
          </template>
          <a-input
            v-model:value="form.description"
            :placeholder="apiParams.description.description"/>
        </a-form-item>
        <a-form-item name="devicetags" ref="devicetags">
          <template #label>
            <tooltip-label :title="$t('label.device.tags')" :tooltip="apiParams.devicetags.description"/>
          </template>
          <a-input
            v-model:value="form.devicetags"
            :placeholder="apiParams.devicetags.description"/>
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :disabled="loading" :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import TooltipLabel from '@/components/widgets/TooltipLabel.vue'
import { postAPI } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon.vue'
import { mixinForm } from '@/utils/mixin'
import { parseDeviceTags } from '@/utils/util'

export default {
  name: 'UpdateDeviceOffering',
  emits: ['close-action', 'refresh-data'],
  mixins: [mixinForm],
  components: { ResourceIcon, TooltipLabel },
  data () {
    return {
      loading: false
    }
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateDeviceOffering')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        name: this.resource.name || '',
        description: this.resource.description || '',
        devicetags: parseDeviceTags(this.resource.devicetags, ':')
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.name') }],
        description: [{ required: true, message: this.$t('message.error.description') }],
        devicetags: [{ required: true, message: this.$t('message.error.device.tags'), validator: this.deviceTagsValidator, trigger: 'blur' }]
      })
    },
    async deviceTagsValidator () {
      const pattern = /^[A-Za-z0-9]+:[0-9]+$/i
      const tags = this.form.devicetags.split(',')

      if (tags.length === 0) {
        return Promise.reject(this.$t('message.error.device.tags'))
      }

      for (const tag of tags) {
        if (!tag.trim()) {
          return Promise.reject(this.$t('message.error.device.tags'))
        }

        if (!pattern.test(tag.trim())) {
          return Promise.reject(this.$t('message.error.device.tags'))
        }
      }

      return Promise.resolve()
    },
    async handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const cleanForm = this.handleRemoveFields(formRaw)
        cleanForm.id = this.resource.id
        postAPI('updateDeviceOffering', cleanForm).then(json => {
          this.$message.success(this.$t('message.update.device.offering'))
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        for (const field of error.errorFields) {
          this.$message.error(field.errors[0])
        }
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}

</script>

<style scoped lang="less">

</style>

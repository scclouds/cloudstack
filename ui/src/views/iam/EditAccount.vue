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
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :loading="loading"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item name="newname" ref="newname">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.newname.description"/>
          </template>
          <a-input
            v-model:value="form.newname"
            :placeholder="apiParams.newname.description" />
        </a-form-item>
        <a-form-item name="networkdomain" ref="networkdomain">
          <template #label>
            <tooltip-label :title="$t('label.networkdomain')" :tooltip="apiParams.networkdomain.description"/>
          </template>
          <a-input
            v-model:value="form.networkdomain"
            :placeholder="apiParams.networkdomain.description" />
        </a-form-item>
        <a-form-item ref="roleid" name="roleid">
          <template #label>
            <tooltip-label :title="$t('label.role')" :tooltip="apiParams.roleid.description"/>
          </template>
          <a-select
            v-model:value="form.roleid"
            :loading="roleLoading"
            :placeholder="apiParams.roleid.description"
            v-focus="true"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }">
            <a-select-option v-for="role in roles" :key="role.id" :value="role.id">{{ role.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          name="defaultprojectid"
          ref="defaultprojectid"
          v-show="projects.length > 0">
          <template #label>
            <tooltip-label :title="$t('label.default.project')" :tooltip="apiParams.defaultprojectid.description"/>
          </template>
          <a-select
            v-model:value="form.defaultprojectid"
            :loading="projectsLoading"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="project in projects"
              :key="project.id"
              :label="project.displaytext || project.name">
              <span>
                <resource-icon v-if="project.icon && project.icon.base64image" :image="project.icon.base64image" size="1x" style="margin-right: 5px"/>
                <project-outlined v-else style="margin-right: 5px" />
                {{ project.displaytext || project.name }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import ResourceIcon from '@/components/view/ResourceIcon.vue'

export default {
  name: 'EditAccount',
  components: {
    ResourceIcon,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      projects: [],
      projectsLoading: false,
      loading: false,
      accountId: null
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateAccount')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  computed: {
    listingSelf () {
      return this.$store.getters.userInfo.id === this.resource.id
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
    },
    fetchData () {
      this.account = this.resource.name
      this.accountId = this.$route.params.id || null
      if ('listProjects' in this.$store.getters.apis) {
        this.fetchProjects()
      }
      this.fillEditFormFieldValues()
      this.fetchRoles()
    },
    fetchRoles () {
      this.roleLoading = true
      const params = {}
      params.state = 'enabled'
      api('listRoles', params).then(response => {
        this.roles = response.listrolesresponse.role || []
        this.form.roleid = this.resource.roleid
      }).finally(() => {
        this.roleLoading = false
      })
    },
    fillEditFormFieldValues () {
      const form = this.form
      this.loading = true
      if (this.resource.networkdomain) {
        form.networkdomain = this.resource.networkdomain
      }
      form.newname = this.resource.name
      this.loading = false
    },
    fetchProjects () {
      this.projects = []
      var params = {
        page: 1,
        pageSize: 500,
        details: 'min',
        showIcon: true
      }
      if (this.listingSelf) {
        params.listAll = true
      } else {
        params.domainid = this.resource.domainid
        if (!this.isDomainAdmin(this.resource.roletype)) {
          params.account = this.resource.name
        }
      }
      var page = 1
      const getNextPage = () => {
        this.projectsLoading = true
        api('listProjects', { listAll: true, page: page, pageSize: 500, details: 'min', showIcon: true }).then(json => {
          const projectBatch = json?.listprojectsresponse?.project
          if (projectBatch) {
            this.projects.push(...projectBatch)
          }
          if (this.projects.length < json.listprojectsresponse.count) {
            params.page++
            getNextPage()
          }
        }).finally(() => {
          this.projects.unshift({ name: this.$t('label.account.view') })
          this.projectsLoading = false
        })
      }
      getNextPage()
      this.form.defaultprojectid = this.resource.defaultprojectid
    },
    handleSubmit (e) {
      console.log(this.resource)
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          id: this.$props.resource.id,
          roleid: values.roleid,
          account: this.account,
          defaultprojectid: (values.defaultprojectid) ? values.defaultprojectid : ''
        }
        if (values.newname) {
          if (values.newname !== this.resource.name) {
            params.newname = values.newname
          }
        }
        if (values.networkdomain || values.networkdomain === '') {
          if (values.networkdomain !== this.resource.networkdomain) {
            params.networkdomain = values.networkdomain
          }
        }

        if (this.defaultprojectid !== undefined) {
          params.defaultprojectid = this.defaultprojectid
        }

        api('updateAccount', params).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.edit.account'),
            description: `${this.$t('message.success.update.account')} ${params.account}`
          })
          this.closeAction()
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
            duration: 0
          })
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    isDomainAdmin (roletype) {
      return ['DomainAdmin'].includes(roletype)
    },
    closeAction () {
      this.$emit('close-action')
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

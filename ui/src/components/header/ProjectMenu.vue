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
  <span class="header-notice-opener">
    <a-select
      v-if="!isDisabled()"
      class="project-select"
      :loading="loading"
      v-model:value="projectSelected"
      :filterOption="filterProject"
      @change="indexToProject"
      @focus="fetchData"
      showSearch>

      <a-select-option
        v-for="(project, index) in projects"
        :key="index"
        :label="project.displaytext || project.name">
        <span>
          <resource-icon v-if="project.icon && project.icon.base64image" :image="project.icon.base64image" size="1x" style="margin-right: 5px"/>
          <project-outlined v-else style="margin-right: 5px" />
          {{ project.displaytext || project.name }}
        </span>
      </a-select-option>
    </a-select>
  </span>
</template>

<script>
import store from '@/store'
import { api } from '@/api'
import _ from 'lodash'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'ProjectMenu',
  components: {
    ResourceIcon
  },
  data () {
    return {
      defaultProject: null,
      projects: [],
      loading: false
    }
  },
  created () {
    this.fetchData().then(response => {
      if (this.defaultProject && !this.$store.getters?.project.name) {
        this.changeProject(this.defaultProject)
      }
    })
  },
  computed: {
    projectSelected () {
      let projectIndex = 0
      if (this.$store.getters?.project?.id) {
        projectIndex = this.projects.findIndex(project => project.id === this.$store.getters.project.id)
        this.$store.dispatch('ToggleTheme', projectIndex === undefined ? 'light' : 'dark')
      }

      return projectIndex
    }
  },
  methods: {
    fetchData () {
      if (this.isDisabled()) {
        return
      }
      var page = 1
      const projects = []
      const defaultProjectId = (this.$store.getters?.defaultView !== {}) ? this.$store.getters.defaultView : undefined
      const getNextPage = () => {
        this.loading = true
        return api('listProjects', { listAll: true, page: page, pageSize: 500, details: 'min', showIcon: true }).then(json => {
          const projectBatch = json?.listprojectsresponse?.project
          if (projectBatch) {
            projects.push(...projectBatch)
            if (!this.defaultProject) {
              this.defaultProject = projectBatch.find((project) => project.id === defaultProjectId)
            }
          }
          if (projects.length < json.listprojectsresponse.count) {
            page++
            getNextPage()
          }
        }).finally(() => {
          this.loading = false
          this.$store.commit('RELOAD_ALL_PROJECTS', projects)
        })
      }
      return getNextPage()
    },
    isDisabled () {
      return !Object.prototype.hasOwnProperty.call(store.getters.apis, 'listProjects')
    },
    changeProject (project) {
      this.$store.dispatch('ProjectView', project.id)
      this.$store.dispatch('SetProject', project)
      this.$store.dispatch('ToggleTheme', project.id === undefined ? 'light' : 'dark')
    },
    indexToProject (index) {
      const project = this.projects[index]
      this.changeProject(project)
      this.$message.success(`${this.$t('message.switch.to')} "${project.displaytext || project.name}"`)
      if (this.$route.name !== 'dashboard') {
        this.$router.push({ name: 'dashboard' })
      }
    },
    filterProject (input, option) {
      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
    }
  },
  mounted () {
    this.$store.watch(
      (state, getters) => getters.allProjects,
      (newValue, oldValue) => {
        if (oldValue !== newValue && newValue !== undefined) {
          this.projects = _.orderBy(newValue, ['displaytext'], ['asc'])
          this.projects.unshift({ name: this.$t('label.account.view') })
        }
      }
    )
  }
}
</script>

<style lang="less" scoped>
.project {
  &-select {
    width: 27vw;
  }

  &-icon {
    font-size: 20px;
    line-height: 1;
    padding-top: 5px;
    padding-right: 5px;
  }
}

.custom-suffix-icon {
  font-size: 20px;
  position: absolute;
  top: 0;
  right: 1px;
  margin-top: -5px;
}
</style>

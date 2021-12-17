<template>
  <div>
    <el-cascader
      filterable
      clearable
      placeholder="试试搜索：手机"
      v-model="paths"
      :options="categorys"
      :props="setting"
    ></el-cascader>
  </div>
</template>

<script>
import PubSub from 'pubsub-js'
export default {
  components: {},
  props: {
    catalogPath: {
      type: Array,
      default () {
        return []
      }
    }
  },
  data () {
    return {
      setting: {
        value: 'catId',
        label: 'name',
        children: 'children'
      },
      categorys: [],
      paths: this.catalogPath
    }
  },
  watch: {
    catalogPath (v) {
      this.paths = this.catalogPath
    },
    paths (v) {
      this.$emit('update:catalogPath', v)
      // 还可以使用pubsub-js进行传值
      PubSub.publish('catPath', v)
    }
  },
  methods: {
    getCategorys () {
      this.$http({
        url: this.$http.adornUrl('/product/category/list/tree'),
        method: 'get'
      }).then(({ data }) => {
        this.categorys = data.data
      })
    }
  },
  created () {
    this.getCategorys()
  }
}
</script>
<style scoped>
</style>

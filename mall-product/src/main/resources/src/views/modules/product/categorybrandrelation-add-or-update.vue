<template>
  <el-dialog
    :title="!dataForm.id ? '新增' : '修改'"
    :close-on-click-modal="false"
    :visible.sync="visible">
    <el-form :model="dataForm" :rules="dataRule" ref="dataForm" @keyup.enter.native="dataFormSubmit()" label-width="80px">
    <el-form-item label="品牌id" prop="brandId">
      <el-input v-model="dataForm.brandId" placeholder="品牌id"></el-input>
    </el-form-item>
    <el-form-item label="分类id" prop="catalogId">
      <el-input v-model="dataForm.catalogId" placeholder="分类id"></el-input>
    </el-form-item>
    <el-form-item label="" prop="brandName">
      <el-input v-model="dataForm.brandName" placeholder=""></el-input>
    </el-form-item>
    <el-form-item label="" prop="catalogName">
      <el-input v-model="dataForm.catalogName" placeholder=""></el-input>
    </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
    </span>
  </el-dialog>
</template>

<script>
  export default {
    data () {
      return {
        visible: false,
        dataForm: {
          id: 0,
          brandId: '',
          catalogId: '',
          brandName: '',
          catalogName: ''
        },
        dataRule: {
          brandId: [
            { required: true, message: '品牌id不能为空', trigger: 'blur' }
          ],
          catalogId: [
            { required: true, message: '分类id不能为空', trigger: 'blur' }
          ],
          brandName: [
            { required: true, message: '不能为空', trigger: 'blur' }
          ],
          catalogName: [
            { required: true, message: '不能为空', trigger: 'blur' }
          ]
        }
      }
    },
    methods: {
      init (id) {
        this.dataForm.id = id || 0
        this.visible = true
        this.$nextTick(() => {
          this.$refs['dataForm'].resetFields()
          if (this.dataForm.id) {
            this.$http({
              url: this.$http.adornUrl(`/product/categorybrandrelation/info/${this.dataForm.id}`),
              method: 'get',
              params: this.$http.adornParams()
            }).then(({data}) => {
              if (data && data.code === 0) {
                this.dataForm.brandId = data.categoryBrandRelation.brandId
                this.dataForm.catalogId = data.categoryBrandRelation.catalogId
                this.dataForm.brandName = data.categoryBrandRelation.brandName
                this.dataForm.catalogName = data.categoryBrandRelation.catalogName
              }
            })
          }
        })
      },
      // 表单提交
      dataFormSubmit () {
        this.$refs['dataForm'].validate((valid) => {
          if (valid) {
            this.$http({
              url: this.$http.adornUrl(`/product/categorybrandrelation/${!this.dataForm.id ? 'save' : 'update'}`),
              method: 'post',
              data: this.$http.adornData({
                'id': this.dataForm.id || undefined,
                'brandId': this.dataForm.brandId,
                'catalogId': this.dataForm.catalogId,
                'brandName': this.dataForm.brandName,
                'catalogName': this.dataForm.catalogName
              })
            }).then(({data}) => {
              if (data && data.code === 0) {
                this.$message({
                  message: '操作成功',
                  type: 'success',
                  duration: 1500,
                  onClose: () => {
                    this.visible = false
                    this.$emit('refreshDataList')
                  }
                })
              } else {
                this.$message.error(data.msg)
              }
            })
          }
        })
      }
    }
  }
</script>

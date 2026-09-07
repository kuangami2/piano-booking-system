<template>
  <el-card style="margin: 24px">
    <template #header>
      <span>{{ route.meta.title }}</span>
    </template>
    <el-empty :description="`${route.meta.title}页面待开发`" />
  </el-card>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

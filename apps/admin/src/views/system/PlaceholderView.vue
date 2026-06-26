<template>
  <a-card>
    <a-result :title="String(route.meta.title)" :sub-title="String(route.meta.description || '模块入口已预留，后续按业务优先级逐步实现。')">
      <template #extra>
        <a-tag color="blue">架构已预留</a-tag>
      </template>
    </a-result>
  </a-card>
</template>

<script setup lang="ts">
import { useRoute } from "vue-router";

const route = useRoute();
</script>


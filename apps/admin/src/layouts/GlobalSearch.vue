<template>
  <a-select
    v-model:value="selectedValue"
    show-search
    :filter-option="false"
    :options="searchOptions"
    placeholder="搜索客户、合同、商机、报价..."
    style="min-width: 280px; max-width: 360px"
    :loading="searching"
    dropdown-match-select-width
    @search="handleSearch"
    @focus="handleFocus"
    @select="handleSelect"
    @clear="searchOptions = []"
    allow-clear
  >
    <template #suffixIcon><SearchOutlined /></template>
    <template #option="{ item }">
      <div style="display: flex; align-items: center; gap: 8px; padding: 2px 0">
        <a-tag :color="item._color" size="small" style="margin: 0; flex-shrink: 0">{{ item._module }}</a-tag>
        <div style="flex: 1; min-width: 0">
          <div style="font-size: 13px; line-height: 1.3">{{ item._title }}</div>
          <div style="font-size: 11px; color: #8c8c8c; line-height: 1.3">{{ item._subtitle }}</div>
        </div>
      </div>
    </template>
  </a-select>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { searchGlobal, type SearchResult } from "@/api/system";

const router = useRouter();
const selectedValue = ref("");
const searchOptions = ref<{ label: string; value: string; options: { label: string; value: string }[] }[]>([]);
const searching = ref(false);
let searchTimer: ReturnType<typeof setTimeout> | null = null;

async function handleSearch(value: string) {
  if (searchTimer) clearTimeout(searchTimer);
  if (!value || value.trim().length < 2) { searchOptions.value = []; return; }
  searching.value = true;
  searchTimer = setTimeout(async () => {
    try {
      const results = await searchGlobal(value.trim());
      const groups: Record<string, { label: string; value: string }[]> = {};
      const typeLabels: Record<string, string> = { customer: "客户", contract: "合同", project: "项目", part: "物料", employee: "员工" };
      for (const r of results) {
        const label = typeLabels[r.type] || r.type;
        if (!groups[label]) groups[label] = [];
        groups[label].push({ label: r.title + (r.subtitle ? " (" + r.subtitle + ")" : ""), value: r.url });
      }
      searchOptions.value = Object.entries(groups).map(([label, options]) => ({ label, value: label, options }));
    } catch { searchOptions.value = []; }
    finally { searching.value = false; }
  }, 300);
}

function handleSelect(value: string) {
  selectedValue.value = "";
  searchOptions.value = [];
  router.push(value);
}
function handleFocus() { if (selectedValue.value) handleSearch(selectedValue.value); }
</script>
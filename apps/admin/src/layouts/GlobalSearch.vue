<template>
  <a-select
    v-model:value="selectedValue"
    show-search
    :filter-option="false"
    :options="searchOptions"
    placeholder="搜索客户、合同、项目、物料、员工"
    class="global-search"
    :loading="searching"
    dropdown-match-select-width
    @search="handleSearch"
    @focus="handleFocus"
    @select="handleSelect"
    @clear="searchOptions = []"
    allow-clear
  >
    <template #suffixIcon><SearchOutlined /></template>
    <template #option="option">
      <div class="search-result">
        <a-tag
          :color="option.color"
          size="small"
          class="search-result-module"
          >{{ option.module }}</a-tag
        >
        <div class="search-result-copy">
          <div class="search-result-title">{{ option.title }}</div>
          <div v-if="option.subtitle" class="search-result-subtitle">
            {{ option.subtitle }}
          </div>
        </div>
      </div>
    </template>
  </a-select>
</template>

<script setup lang="ts">
import { ref } from "vue";
import SearchOutlined from "@ant-design/icons-vue/SearchOutlined";
import { useRouter } from "vue-router";
import { searchGlobal } from "@/api/system";

const router = useRouter();
const selectedValue = ref("");
type SearchOption = {
  label: string;
  value: string;
  module: string;
  color: string;
  title: string;
  subtitle: string;
};
const searchOptions = ref<SearchOption[]>([]);
const searching = ref(false);
let searchTimer: ReturnType<typeof setTimeout> | null = null;
let searchSequence = 0;

async function handleSearch(value: string) {
  if (searchTimer) clearTimeout(searchTimer);
  const sequence = ++searchSequence;
  if (!value || value.trim().length < 2) {
    searchOptions.value = [];
    searching.value = false;
    return;
  }
  searching.value = true;
  searchTimer = setTimeout(async () => {
    try {
      const results = await searchGlobal(value.trim());
      const typeLabels: Record<string, string> = {
        customer: "客户",
        contract: "合同",
        project: "项目",
        part: "物料",
        employee: "员工",
      };
      const typeColors: Record<string, string> = {
        customer: "blue",
        contract: "cyan",
        project: "green",
        part: "gold",
        employee: "purple",
      };
      if (sequence !== searchSequence) return;
      searchOptions.value = results.map((result) => ({
        label: `${typeLabels[result.type] || result.type} ${result.title} ${result.subtitle}`,
        value: result.url,
        module: typeLabels[result.type] || result.type,
        color: typeColors[result.type] || "default",
        title: result.title,
        subtitle: result.subtitle,
      }));
    } catch {
      if (sequence === searchSequence) searchOptions.value = [];
    } finally {
      if (sequence === searchSequence) searching.value = false;
    }
  }, 300);
}

function handleSelect(value: string) {
  selectedValue.value = "";
  searchOptions.value = [];
  router.push(value);
}
function handleFocus() {
  if (selectedValue.value) handleSearch(selectedValue.value);
}
</script>

<style scoped>
.global-search {
  width: 100%;
  min-width: 280px;
  max-width: 360px;
}

.search-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}

.search-result-module {
  flex-shrink: 0;
  margin: 0;
}

.search-result-copy {
  min-width: 0;
  flex: 1;
}

.search-result-title,
.search-result-subtitle {
  overflow: hidden;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-result-title {
  font-size: 13px;
}

.search-result-subtitle {
  color: #75808b;
  font-size: 11px;
}

@media (max-width: 768px) {
  .global-search {
    min-width: 0;
    max-width: none;
  }
}
</style>

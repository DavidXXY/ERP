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
import SearchOutlined from "@ant-design/icons-vue/SearchOutlined";
import { listCustomers, listOpportunities, listQuotes, listContracts } from "@/api/crm";

const router = useRouter();
const selectedValue = ref<string>("");
const searchOptions = ref<any[]>([]);
const searching = ref(false);
let debounceTimer: ReturnType<typeof setTimeout>;

function handleSearch(value: string) {
  clearTimeout(debounceTimer);
  if (!value.trim()) { searchOptions.value = []; return; }
  searching.value = true;
  debounceTimer = setTimeout(async () => {
    try {
      const [customers, opps, quotes, contracts] = await Promise.all([
        listCustomers(), listOpportunities(), listQuotes(), listContracts(),
      ]);
      const term = value.toLowerCase();
      const results: any[] = [];
      customers.filter((c: any) => (c.name + (c.code || "")).toLowerCase().includes(term)).slice(0, 5).forEach((c: any) => {
        results.push({ value: "/crm/customers?customer=" + c.id, _module: "客户", _title: c.name, _subtitle: c.code + " · " + c.industry, _color: "blue" });
      });
      opps.filter((o: any) => (o.code + o.customerName + (o.needSummary || "")).toLowerCase().includes(term)).slice(0, 5).forEach((o: any) => {
        results.push({ value: "/crm/opportunities/" + o.id, _module: "商机", _title: o.code, _subtitle: o.customerName + " · " + (o.needSummary || "").slice(0, 30), _color: "cyan" });
      });
      quotes.filter((q: any) => (q.code + q.customerName + (q.serviceScope || "")).toLowerCase().includes(term)).slice(0, 5).forEach((q: any) => {
        results.push({ value: "/crm/quotes/" + q.id, _module: "报价", _title: q.code, _subtitle: q.customerName + " · " + (q.serviceScope || "").slice(0, 30), _color: "purple" });
      });
      contracts.filter((c: any) => (c.code + c.projectName + c.customerName).toLowerCase().includes(term)).slice(0, 5).forEach((c: any) => {
        results.push({ value: "/crm/contracts/" + c.id, _module: "合同", _title: c.code, _subtitle: c.projectName + " · " + c.customerName, _color: "green" });
      });
      searchOptions.value = results.slice(0, 12);
    } catch { searchOptions.value = []; }
    finally { searching.value = false; }
  }, 300);
}

function handleFocus() {
  if (selectedValue.value && typeof selectedValue.value === "string") handleSearch(selectedValue.value);
}

function handleSelect(value: string) {
  searchOptions.value = [];
  router.push(value);
}
</script>

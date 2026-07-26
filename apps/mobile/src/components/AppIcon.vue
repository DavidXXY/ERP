<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(defineProps<{ type?: string; size?: number | string; color?: string }>(), {
  type: "circle",
  size: 16,
  color: "#333333",
});

const glyphs: Record<string, string> = {
  search: "⌕",
  location: "⌖",
  "location-filled": "⌖",
  person: "●",
  checkbox: "✓",
  checkmarkempty: "✓",
  gear: "⚙",
  compose: "＋",
  notification: "!",
  "camera-filled": "▣",
  refresh: "↻",
  right: "›",
  down: "⌄",
  locked: "→",
  info: "!",
  weixin: "微",
  wallet: "¥",
  calendar: "日",
  map: "⌖",
  list: "≡",
  "cloud-upload": "↑",
  trash: "×",
  "hand-up": "✓",
  undo: "↩",
  "folder-add": "＋",
};
const glyph = computed(() => glyphs[props.type] || "•");
const iconStyle = computed(() => ({ color: props.color, fontSize: `${Number(props.size)}px` }));
</script>

<template><text class="app-icon" :style="iconStyle"><slot>{{ glyph }}</slot></text></template>

<style scoped>
.app-icon { display: inline-flex; align-items: center; justify-content: center; width: 1.15em; height: 1.15em; line-height: 1; font-family: Arial, sans-serif; font-weight: 750; font-style: normal; }
</style>

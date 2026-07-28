<script setup lang="ts">
import { onLaunch, onShow } from "@dcloudio/uni-app";
import { useAuthStore } from "@/stores/auth";
import { flushQueue } from "@/utils/offline";

onLaunch(() => useAuthStore().restore());
onShow(() => {
  if (useAuthStore().token) void flushQueue();
});
</script>

<style lang="scss">
page {
  min-height: 100%;
  background: #f3f5f7;
  color: #17202a;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 28rpx;
}

view, text, button, input, textarea, scroll-view {
  box-sizing: border-box;
}

button::after { border: 0; }

/* #ifdef H5 */
html,
body {
  background: #dfe5e3;
}

uni-app,
uni-page,
uni-page-wrapper {
  width: 100%;
  max-width: 480px;
  margin-right: auto;
  margin-left: auto;
}

uni-app {
  min-height: 100vh;
  background: #f3f5f7;
  box-shadow: 0 0 32px rgba(25, 45, 40, 0.12);
}

uni-page-refresh {
  right: 0 !important;
  left: 0 !important;
  width: auto !important;
  max-width: 100%;
}

uni-tabbar.uni-tabbar-bottom {
  right: 0;
  left: 0;
  width: 100%;
  max-width: 480px;
  margin-right: auto;
  margin-left: auto;
}

uni-tabbar.uni-tabbar-bottom .uni-tabbar {
  right: 0;
  left: 0;
  width: 100%;
  max-width: 480px;
  margin-right: auto;
  margin-left: auto;
}
/* #endif */
</style>

<template>
  <div class="page-stack">
    <a-card>
      <template #title>消息中心 <a-tag v-if="unreadCount>0" color="red">{{ unreadCount }} 条未读</a-tag></template>
      <template #extra><a-space><a-button @click="goBack">返回办公室</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button><a-button v-if="unreadCount>0" @click="handleMarkAllRead">全部标记已读</a-button></a-space></template>
      <a-space class="table-toolbar"><a-button @click="handleRefreshNotifications"><template #icon><ReloadOutlined /></template>扫描业务预警</a-button></a-space>
      <a-list bordered :data-source="notifications" :loading="loading">
        <template #renderItem="{ item }">
          <a-list-item :class="{'notification-unread':!item.read}" @click="handleNavigate(item)">
            <a-list-item-meta>
              <template #title>
                <span :class="{'notification-title-unread':!item.read}">{{ item.title }}</span>
                <a-tag v-if="!item.read" color="blue" size="small" style="margin-left:8px">未读</a-tag>
              </template>
              <template #description>
                <p style="margin:0">{{ item.content }}</p>
                <span style="font-size:12px;color:#8c8c8c">{{ formatDateTime(item.createdAt) }}</span>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button v-if="!item.read" type="link" size="small" @click.stop="handleRead(item)">标记已读</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listNotifications, readNotification, refreshNotifications, type NotificationRecord } from "@/api/office";
const router=useRouter(); const loading=ref(false); const notifications=ref<NotificationRecord[]>([]);
const unreadCount=computed(()=>notifications.value.filter(n=>!n.read).length);
onMounted(loadData);
async function loadData(){loading.value=true;try{notifications.value=await listNotifications();}catch(error){message.error(error instanceof Error?error.message:'数据加载失败');}finally{loading.value=false;}}
function goBack(){router.push('/office');}
async function handleRead(item:NotificationRecord){try{await readNotification(item.id);await loadData();}catch(error){message.error(error instanceof Error?error.message:'消息处理失败');}}
async function handleMarkAllRead(){const unread=notifications.value.filter(n=>!n.read);if(!unread.length)return;for(const n of unread){try{await readNotification(n.id);}catch{}}message.success('已标记 '+unread.length+' 条为已读');await loadData();}
async function handleRefreshNotifications(){try{const count=await refreshNotifications();message.success(count?'新增 '+count+' 条业务预警':'当前没有新的业务预警');await loadData();}catch(error){message.error(error instanceof Error?error.message:'预警扫描失败');}}
function handleNavigate(item:NotificationRecord){if(!item.read)handleRead(item);if(item.title?.includes('提醒')||item.title?.includes('预警'))return;}

function formatDateTime(v?:string){return v?new Date(v).toLocaleString('zh-CN',{hour12:false}):'-';}
</script>
<style scoped>
.notification-unread{background:#e6f7ff;cursor:pointer}
.notification-unread:hover{background:#bae7ff}
.notification-title-unread{font-weight:600}
</style>

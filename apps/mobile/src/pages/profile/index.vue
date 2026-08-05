<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import AppHeader from "@/components/AppHeader.vue";
import StateView from "@/components/StateView.vue";
import { getPersonalProfile } from "@/api/personal";
import { bindCurrentWechatApi } from "@/api/auth";
import { useAuthStore } from "@/stores/auth";
import type { PersonalOverview } from "@/types/domain";
import { getQueue } from "@/utils/offline";

const auth=useAuthStore();const profile=ref<PersonalOverview|null>(null);const loading=ref(true);const error=ref("");const offlineCount=ref(0);
const initials=computed(()=>profile.value?.account.displayName?.slice(-2)||auth.user?.displayName?.slice(-2)||"ERP");
async function load(){loading.value=!profile.value;error.value="";offlineCount.value=getQueue().length;try{profile.value=await getPersonalProfile();}catch(e){error.value=(e as Error).message;}finally{loading.value=false;}}
async function bindWechat(){try{const code=await new Promise<string>((resolve,reject)=>uni.login({provider:"weixin",success:r=>r.code?resolve(r.code):reject(new Error("未获取到微信凭证")),fail:reject}));await bindCurrentWechatApi(code);uni.showToast({title:"微信绑定成功",icon:"success"});}catch(e){uni.showToast({title:(e as Error).message,icon:"none"});}}
async function logout(){const result=await uni.showModal({title:"退出登录",content:"确认退出当前 ERP 账号？"});if(result.confirm)auth.logout();}
onShow(load);
</script>

<template>
  <view>
    <AppHeader title="个人中心" subtitle="账号、档案与移动设置" />
    <view class="page-shell profile-page">
      <StateView :loading="loading" :error="error" :empty="!profile" @retry="load">
        <template v-if="profile">
          <view class="identity surface">
            <view class="avatar">{{ initials }}</view>
            <view class="identity-main">
              <text>{{ profile.account.displayName }}</text>
              <text>{{ profile.employee?.position || profile.account.username }} · {{ profile.account.organizationName || profile.employee?.organizationName || "组织未设置" }}</text>
            </view>
          </view>
          <view class="surface menu">
            <view class="menu-row" @click="uni.navigateTo({ url: '/pages/applications/history' })">
              <view class="menu-icon"><uni-icons type="list" size="21" color="#176b5b" /></view><text>我的申请</text><uni-icons type="right" size="17" color="#9aa2a9" />
            </view>
            <view v-if="auth.can('inventory:view')" class="menu-row" @click="uni.navigateTo({ url: '/pages/spares/index' })">
              <view class="menu-icon"><uni-icons type="gear" size="21" color="#176b5b" /></view><text>备件库存</text><uni-icons type="right" size="17" color="#9aa2a9" />
            </view>
            <view v-if="auth.can('office:notification:view')" class="menu-row" @click="uni.navigateTo({ url: '/pages/notifications/index' })">
              <view class="menu-icon"><uni-icons type="notification" size="21" color="#176b5b" /></view><text>消息通知</text><uni-icons type="right" size="17" color="#9aa2a9" />
            </view>
            <view v-if="auth.can('maintenance:view') || auth.can('maintenance:order:manage')" class="menu-row" @click="uni.navigateTo({ url: '/pages/offline/index' })">
              <view class="menu-icon"><uni-icons type="cloud-upload" size="21" color="#176b5b" /></view><text>离线任务</text><text v-if="offlineCount" class="menu-badge">{{ offlineCount }}</text><uni-icons type="right" size="17" color="#9aa2a9" />
            </view>
            <!-- #ifdef MP-WEIXIN -->
            <view class="menu-row" @click="bindWechat"><view class="menu-icon"><uni-icons type="weixin" size="21" color="#176b5b" /></view><text>绑定当前微信</text><uni-icons type="right" size="17" color="#9aa2a9" /></view>
            <!-- #endif -->
          </view>
          <view class="surface account-block">
            <text class="section-title">账号信息</text>
            <view class="account-row"><text>用户名</text><text>{{ profile.account.username }}</text></view>
            <view class="account-row"><text>手机</text><text>{{ profile.account.phone || "未设置" }}</text></view>
            <view class="account-row"><text>邮箱</text><text>{{ profile.account.email || "未设置" }}</text></view>
            <view class="account-row"><text>入职日期</text><text>{{ profile.employee?.entryDate || "-" }}</text></view>
          </view>
          <button class="logout-btn" @click="logout"><uni-icons type="undo" size="19" color="#b53232" />退出登录</button>
          <text class="version">工程运维 ERP 移动端 v0.1.0</text>
        </template>
      </StateView>
    </view>
  </view>
</template>

<style scoped>.profile-page{padding-top:24rpx}.identity{padding:30rpx;display:flex;align-items:center;gap:22rpx}.avatar{width:100rpx;height:100rpx;display:flex;align-items:center;justify-content:center;border-radius:12rpx;background:#176b5b;color:#fff;font-size:31rpx;font-weight:800}.identity-main{flex:1;min-width:0}.identity-main text{display:block}.identity-main text:first-child{font-size:34rpx;font-weight:760}.identity-main text:last-child{margin-top:10rpx;color:#69737e;font-size:23rpx;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.menu{margin-top:20rpx;padding:0 24rpx}.menu-row{min-height:94rpx;display:grid;grid-template-columns:55rpx 1fr auto 30rpx;align-items:center;gap:10rpx;border-bottom:1rpx solid #edf0f1}.menu-row:last-child{border-bottom:0}.menu-icon{width:44rpx;height:44rpx;display:flex;align-items:center;justify-content:center;border-radius:8rpx;background:#e8f2ef}.menu-badge{min-width:34rpx;height:34rpx;padding:0 7rpx;display:flex;align-items:center;justify-content:center;border-radius:17rpx;color:#fff;background:#b96f15;font-size:19rpx}.account-block{margin-top:20rpx;padding:28rpx}.account-row{display:flex;justify-content:space-between;gap:20rpx;padding:21rpx 0;border-bottom:1rpx solid #edf0f1;color:#69737e;font-size:24rpx}.account-row text:last-child{color:#303b44;text-align:right}.logout-btn{margin-top:30rpx;min-height:82rpx;display:flex;align-items:center;justify-content:center;gap:10rpx;border:1rpx solid #e4bcbc;border-radius:11rpx;color:#b53232;background:#fff;font-size:27rpx}.version{display:block;margin-top:25rpx;text-align:center;color:#9aa2a9;font-size:21rpx}</style>

<script setup lang="ts">
import { fetchMe } from "@/api/auth";
import { listMyApplied, listMyPublished } from "@/api/task";
import { useUserStore } from "@/store/user";
import { genderLabel } from "@/utils/format";
import { onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";

const userStore = useUserStore();
const isLoggedIn = userStore.isLoggedIn;
const user = computed(() => userStore.state.user);
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20;
const publishedCount = ref(0);
const appliedCount = ref(0);

const avatarText = computed(() => {
  const name = user.value?.nickname || user.value?.username || "我";
  return name.slice(0, 1);
});

const schoolLine = computed(() => {
  const school = user.value?.schoolName || "未设置学校";
  const campus = user.value?.campusName;
  return campus ? `${school} · ${campus}` : school;
});

const genderText = computed(() => genderLabel(user.value?.gender));

function goLogin() {
  uni.navigateTo({ url: "/pages/auth/login" });
}

function go(url: string) {
  uni.navigateTo({ url });
}

async function refreshUser() {
  if (!isLoggedIn.value) {
    publishedCount.value = 0;
    appliedCount.value = 0;
    return;
  }
  try {
    const latest = await fetchMe();
    if (userStore.state.token) {
      userStore.setLogin(userStore.state.token, latest);
    }
  } catch {
    // 保持本地缓存
  }
  try {
    const [published, applied] = await Promise.all([listMyPublished(1, 1), listMyApplied(1, 1)]);
    publishedCount.value = published.total || 0;
    appliedCount.value = applied.total || 0;
  } catch {
    publishedCount.value = 0;
    appliedCount.value = 0;
  }
}

onShow(() => {
  refreshUser();
});
</script>

<template>
  <view class="page">
    <view v-if="!isLoggedIn" class="guest">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="nav-title">我的</view>
      <view class="guest-body">
        <view class="lock-avatar">
          <text class="lock-person">👤</text>
          <view class="lock-badge">🔒</view>
        </view>
        <view class="guest-name">未登录</view>
        <view class="guest-sub">登录后即可使用全部功能</view>
        <view class="login-wrap">
          <wd-button type="primary" round block @click="goLogin">登录 / 注册</wd-button>
        </view>
      </view>
    </view>

    <view v-else class="logged">
      <view class="hero">
        <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
        <view class="nav-title light">我的</view>
        <view class="profile-row">
          <image v-if="user?.avatarUrl" class="avatar" :src="user.avatarUrl" mode="aspectFill" />
          <view v-else class="avatar avatar-text">{{ avatarText }}</view>
          <view class="profile-main">
            <view class="name-line">
              <text class="nickname">{{ user?.nickname || user?.username }}</text>
              <text class="gender-tag">{{ genderText }}</text>
            </view>
            <view class="school-line">{{ schoolLine }}</view>
            <view class="rate-line">
              <text class="stars">☆ 暂无评分</text>
              <text class="done">已完成 {{ user?.completedCount || 0 }} 次</text>
            </view>
          </view>
        </view>
      </view>
      <view class="stats">
        <view class="stat">
          <view class="num">{{ publishedCount }}</view>
          <view class="label">我发布</view>
        </view>
        <view class="stat">
          <view class="num">{{ appliedCount }}</view>
          <view class="label">我申请</view>
        </view>
        <view class="stat">
          <view class="num">{{ user?.completedCount || 0 }}</view>
          <view class="label">已完成</view>
        </view>
      </view>
      <view class="section">
        <view class="section-title">我的代课</view>
        <wd-cell-group border>
          <wd-cell title="我发布的" is-link @click="go('/pages/mine/published')">
            <template #icon><text class="cell-icon">📄</text></template>
          </wd-cell>
          <wd-cell title="我申请的" is-link @click="go('/pages/mine/applied')">
            <template #icon><text class="cell-icon">📝</text></template>
          </wd-cell>
          <wd-cell title="我的收藏" is-link @click="go('/pages/mine/favorites')">
            <template #icon><text class="cell-icon">⭐</text></template>
          </wd-cell>
        </wd-cell-group>
      </view>
      <view class="section">
        <view class="section-title">其他</view>
        <wd-cell-group border>
          <wd-cell title="个人资料" is-link @click="go('/pages/mine/profile')">
            <template #icon><text class="cell-icon">👤</text></template>
          </wd-cell>
          <wd-cell title="我的评价" is-link @click="go('/pages/mine/reviews')">
            <template #icon><text class="cell-icon">💬</text></template>
          </wd-cell>
          <wd-cell title="黑名单" is-link @click="go('/pages/mine/blacklist')">
            <template #icon><text class="cell-icon">🚫</text></template>
          </wd-cell>
          <wd-cell title="举报与反馈" is-link @click="go('/pages/mine/feedback')">
            <template #icon><text class="cell-icon">⚑</text></template>
          </wd-cell>
          <wd-cell title="设置" is-link @click="go('/pages/mine/settings')">
            <template #icon><text class="cell-icon">⚙</text></template>
          </wd-cell>
        </wd-cell-group>
      </view>
    </view>
    <wd-toast />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f4f6fb;
}
.guest {
  min-height: 100vh;
  background: linear-gradient(180deg, #edf3ff 0%, #f7f9fc 42%, #f7f9fc 100%);
}
.status-bar {
  width: 100%;
}
.nav-title {
  text-align: center;
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
  padding: 10px 0 8px;
}
.nav-title.light {
  color: #fff;
}
.guest-body {
  padding-top: 72px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.lock-avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: #dbe7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.lock-person {
  font-size: 42px;
}
.lock-badge {
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  box-shadow: 0 2px 6px rgba(77, 128, 240, 0.2);
}
.guest-name {
  margin-top: 18px;
  font-size: 20px;
  font-weight: 600;
  color: #1d2129;
}
.guest-sub {
  margin-top: 8px;
  color: #86909c;
  font-size: 13px;
}
.login-wrap {
  margin-top: 28px;
  width: 220px;
}
.hero {
  background: linear-gradient(180deg, #5b8ff9 0%, #4d80f0 100%);
  padding-bottom: 48px;
}
.profile-row {
  display: flex;
  padding: 8px 20px 0;
  align-items: center;
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #dbe7ff;
  margin-right: 14px;
  flex-shrink: 0;
}
.avatar-text {
  color: #4d80f0;
  font-size: 24px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.profile-main {
  flex: 1;
  min-width: 0;
}
.name-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nickname {
  color: #fff;
  font-size: 20px;
  font-weight: 600;
}
.gender-tag {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}
.school-line {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
}
.rate-line {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
}
.stats {
  margin: -28px 16px 0;
  background: #fff;
  border-radius: 16px;
  display: flex;
  padding: 16px 0;
  box-shadow: 0 8px 20px rgba(77, 128, 240, 0.08);
  position: relative;
  z-index: 1;
}
.stat {
  flex: 1;
  text-align: center;
}
.num {
  font-size: 22px;
  font-weight: 700;
  color: #1d2129;
}
.label {
  margin-top: 4px;
  font-size: 12px;
  color: #86909c;
}
.section {
  margin-top: 16px;
  padding: 0 16px;
}
.section-title {
  font-size: 13px;
  color: #86909c;
  margin: 0 4px 8px;
}
.cell-icon {
  margin-right: 8px;
  font-size: 16px;
}
</style>

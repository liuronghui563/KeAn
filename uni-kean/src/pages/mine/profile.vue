<script setup lang="ts">
import { useUserStore } from "@/store/user";
import { genderLabel } from "@/utils/format";
import { useToast } from "wot-design-uni";
import { computed } from "vue";

const toast = useToast();
const userStore = useUserStore();
const user = computed(() => userStore.state.user);

function comingSoon() {
  toast.info("功能暂未开放");
}
</script>

<template>
  <view class="page">
    <view class="head">
      <image v-if="user?.avatarUrl" class="avatar" :src="user.avatarUrl" mode="aspectFill" />
      <view v-else class="avatar avatar-text">{{ (user?.nickname || "我").slice(0, 1) }}</view>
      <view class="name">{{ user?.nickname || user?.username || "未登录" }}</view>
    </view>
    <wd-cell-group border>
      <wd-cell title="性别" :value="genderLabel(user?.gender)" />
      <wd-cell title="学校" :value="user?.schoolName || '-'" />
      <wd-cell title="校区" :value="user?.campusName || '-'" />
      <wd-cell title="修改密码" is-link @click="comingSoon" />
      <wd-cell title="账号与安全" is-link @click="comingSoon" />
    </wd-cell-group>
    <wd-toast />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f8;
}
.head {
  background: #fff;
  padding: 28px 16px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 12px;
}
.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #dbe7ff;
}
.avatar-text {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4d80f0;
  font-size: 26px;
  font-weight: 600;
}
.name {
  margin-top: 12px;
  font-size: 18px;
  font-weight: 600;
}
</style>

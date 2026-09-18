<script setup lang="ts">
import TaskForm from "@/components/TaskForm.vue";
import { useUserStore } from "@/store/user";
import { onShow } from "@dcloudio/uni-app";
import { ref } from "vue";

const userStore = useUserStore();
const loggedIn = ref(userStore.isLoggedIn.value);

onShow(() => {
  loggedIn.value = userStore.isLoggedIn.value;
});

function goLogin() {
  uni.navigateTo({ url: "/pages/auth/login?redirect=publish" });
}

function onSuccess(id: number) {
  uni.navigateTo({ url: `/pages/task/detail?id=${id}` });
}
</script>

<template>
  <view class="page">
    <view v-if="!loggedIn" class="guest">
      <wd-status-tip image="content" tip="登录后才能发布代课" />
      <wd-button type="primary" @click="goLogin">去登录</wd-button>
    </view>
    <TaskForm v-else @success="onSuccess" />
    <wd-toast />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f8;
}
.guest {
  padding-top: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
</style>

<script setup lang="ts">
import { useUserStore } from "@/store/user";
import { computed } from "vue";

const userStore = useUserStore();
const completedCount = computed(() => userStore.state.user?.completedCount || 0);
</script>

<template>
  <view class="page">
    <view class="score-card">
      <view class="score">暂无</view>
      <view class="stars">☆☆☆☆☆</view>
      <view class="from">完成 {{ completedCount }} 次代课后可获得评价</view>
    </view>
    <view class="list-title">评价列表</view>
    <wd-status-tip image="content" tip="暂无评价" />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f8;
}
.score-card {
  background: #fff;
  margin: 12px 16px;
  border-radius: 16px;
  padding: 24px 16px;
  text-align: center;
}
.score {
  font-size: 36px;
  font-weight: 700;
  color: #1d2129;
}
.stars {
  margin-top: 8px;
  color: #c9cdd4;
  letter-spacing: 4px;
}
.from {
  margin-top: 8px;
  color: #86909c;
  font-size: 12px;
}
.list-title {
  padding: 8px 20px;
  color: #86909c;
  font-size: 13px;
}
</style>

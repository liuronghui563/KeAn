<script setup lang="ts">
import { login } from "@/api/auth";
import { useUserStore } from "@/store/user";
import { onLoad } from "@dcloudio/uni-app";
import { useToast } from "wot-design-uni";
import { reactive, ref } from "vue";

const toast = useToast();
const userStore = useUserStore();
const loading = ref(false);
const formRef = ref();
const redirect = ref("");
const model = reactive({
  username: "",
  password: ""
});

onLoad((query) => {
  redirect.value = String(query?.redirect || "");
});

function afterLogin() {
  if (redirect.value === "publish") {
    uni.switchTab({ url: "/pages/publish/index" });
    return;
  }
  uni.switchTab({ url: "/pages/mine/index" });
}

function onBack() {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
    return;
  }
  uni.switchTab({ url: "/pages/mine/index" });
}

function goRegister() {
  uni.navigateTo({ url: "/pages/auth/register" });
}

function handleLogin() {
  formRef.value
    .validate()
    .then(async ({ valid }: { valid: boolean }) => {
      if (!valid) {
        return;
      }
      loading.value = true;
      try {
        const data = await login({
          username: model.username,
          password: model.password
        });
        userStore.setLogin(data.token, data.user);
        toast.success("登录成功");
        setTimeout(() => {
          afterLogin();
        }, 400);
      } catch (error) {
        toast.error((error as Error).message || "登录失败");
      } finally {
        loading.value = false;
      }
    })
    .catch(() => undefined);
}
</script>

<template>
  <view class="page">
    <wd-navbar title="登录" left-arrow safe-area-inset-top @click-left="onBack" />
    <view class="hero">
      <view class="title">课安</view>
      <view class="sub">校园临时代课互助</view>
    </view>
    <wd-form ref="formRef" :model="model" error-type="toast">
      <wd-cell-group border>
        <wd-input
          v-model="model.username"
          label="用户名"
          label-width="80px"
          prop="username"
          clearable
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请填写用户名' }]"
        />
        <wd-input
          v-model="model.password"
          label="密码"
          label-width="80px"
          prop="password"
          show-password
          clearable
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请填写密码' }]"
        />
      </wd-cell-group>
      <view class="footer">
        <wd-button type="primary" size="large" block :loading="loading" @click="handleLogin">
          登录
        </wd-button>
        <wd-button type="text" custom-class="link" @click="goRegister">没有账号？去注册</wd-button>
      </view>
    </wd-form>
    <wd-toast />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f8;
}
.hero {
  padding: 48px 24px 24px;
}
.title {
  font-size: 28px;
  font-weight: 600;
  color: #1d2129;
}
.sub {
  margin-top: 8px;
  color: #86909c;
  font-size: 14px;
}
.footer {
  padding: 24px 16px;
}
</style>

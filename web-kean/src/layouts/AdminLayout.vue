<script setup lang="ts">
import { useUserStore } from "@/stores/user";
import { logout } from "@/api/auth";
import { useRouter } from "vue-router";
import { Odometer } from "@element-plus/icons-vue";

const router = useRouter();
const userStore = useUserStore();

async function handleLogout() {
  try {
    await logout();
  } catch {
    // ignore
  }
  userStore.logoutLocal();
  await router.push("/login");
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">课安管理端</div>
      <el-menu router :default-active="$route.path" background-color="#1d2129" text-color="#c9cdd4" active-text-color="#4d80f0">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>Dashboard</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ ($route.meta.title as string) || "课安" }}</span>
        <div class="right">
          <span class="user">{{ userStore.user?.nickname || userStore.user?.username }}</span>
          <el-button type="primary" link @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #1d2129;
  color: #fff;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  letter-spacing: 1px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e6eb;
  background: #fff;
}
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user {
  color: #4e5969;
}
.main {
  background: #f2f3f5;
}
</style>

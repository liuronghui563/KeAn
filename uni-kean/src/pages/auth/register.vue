<script setup lang="ts">
import { register } from "@/api/auth";
import { listCampuses, listSchools } from "@/api/catalog";
import { useToast } from "wot-design-uni";
import { onMounted, reactive, ref, watch } from "vue";

const toast = useToast();
const loading = ref(false);
const formRef = ref();
const model = reactive({
  username: "",
  password: "",
  nickname: "",
  phone: "",
  gender: "" as string,
  schoolId: 1 as number | string,
  campusId: 1 as number | string
});

const schoolColumns = ref([{ label: "演示大学", value: 1 }]);
const campusColumns = ref([{ label: "主校区", value: 1 }]);
const genderColumns = [
  { label: "男", value: "MALE" },
  { label: "女", value: "FEMALE" }
];

async function loadSchools() {
  const schools = await listSchools();
  schoolColumns.value = schools.map((item) => ({ label: item.name, value: item.id }));
  if (!schools.some((item) => item.id === Number(model.schoolId)) && schools.length) {
    model.schoolId = schools[0].id;
  }
}

async function loadCampuses() {
  const campuses = await listCampuses(Number(model.schoolId) || 1);
  campusColumns.value = campuses.map((item) => ({ label: item.name, value: item.id }));
  if (!campuses.some((item) => item.id === Number(model.campusId)) && campuses.length) {
    model.campusId = campuses[0].id;
  }
}

watch(
  () => model.schoolId,
  () => {
    loadCampuses().catch(() => undefined);
  }
);

onMounted(() => {
  loadSchools()
    .then(() => loadCampuses())
    .catch(() => undefined);
});

function handleRegister() {
  formRef.value
    .validate()
    .then(async ({ valid }: { valid: boolean }) => {
      if (!valid) {
        return;
      }
      loading.value = true;
      try {
        await register({
          username: model.username,
          password: model.password,
          nickname: model.nickname,
          gender: String(model.gender),
          schoolId: Number(model.schoolId),
          campusId: Number(model.campusId),
          phone: model.phone || undefined
        });
        toast.success("注册成功，请登录");
        setTimeout(() => {
          uni.redirectTo({ url: "/pages/auth/login" });
        }, 400);
      } catch (error) {
        toast.error((error as Error).message || "注册失败");
      } finally {
        loading.value = false;
      }
    })
    .catch(() => undefined);
}
</script>

<template>
  <view class="page">
    <wd-navbar title="注册" left-arrow safe-area-inset-top @click-left="uni.navigateBack()" />
    <view class="hero">
      <view class="title">创建账号</view>
      <view class="sub">仅支持同校学生注册</view>
    </view>
    <wd-form ref="formRef" :model="model" error-type="toast">
      <wd-cell-group border>
        <wd-input
          v-model="model.username"
          label="用户名"
          label-width="80px"
          prop="username"
          clearable
          placeholder="4-32 位字母数字下划线"
          :rules="[{ required: true, message: '请填写用户名' }]"
        />
        <wd-input
          v-model="model.password"
          label="密码"
          label-width="80px"
          prop="password"
          show-password
          clearable
          placeholder="8-32 位密码"
          :rules="[{ required: true, message: '请填写密码' }]"
        />
        <wd-input
          v-model="model.nickname"
          label="昵称"
          label-width="80px"
          prop="nickname"
          clearable
          placeholder="请输入昵称"
          :rules="[{ required: true, message: '请填写昵称' }]"
        />
        <wd-picker
          v-model="model.gender"
          label="性别"
          label-width="80px"
          prop="gender"
          :columns="genderColumns"
          :rules="[{ required: true, message: '请选择性别' }]"
        />
        <wd-input
          v-model="model.phone"
          label="手机号"
          label-width="80px"
          prop="phone"
          clearable
          placeholder="选填"
        />
        <wd-picker
          v-model="model.schoolId"
          label="学校"
          label-width="80px"
          prop="schoolId"
          :columns="schoolColumns"
          :rules="[{ required: true, message: '请选择学校' }]"
        />
        <wd-picker
          v-model="model.campusId"
          label="校区"
          label-width="80px"
          prop="campusId"
          :columns="campusColumns"
          :rules="[{ required: true, message: '请选择校区' }]"
        />
      </wd-cell-group>
      <view class="footer">
        <wd-button type="primary" size="large" block :loading="loading" @click="handleRegister">
          注册
        </wd-button>
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
  padding: 32px 24px 16px;
}
.title {
  font-size: 24px;
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

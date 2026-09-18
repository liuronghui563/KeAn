<script setup lang="ts">
import {
  acceptApplication,
  applyTask,
  listApplications,
  rejectApplication,
  withdrawApplication,
  APPLICATION_STATUS_TEXT,
  type ApplicationItem
} from "@/api/application";
import {
  cancelTask,
  completeTask,
  confirmTask,
  deleteTask,
  getTask,
  TASK_STATUS_TEXT,
  type TaskItem
} from "@/api/task";
import { useUserStore } from "@/store/user";
import { formatReward, genderRequirementLabel, parseDateTime } from "@/utils/format";
import { onLoad, onShow } from "@dcloudio/uni-app";
import { useToast } from "wot-design-uni";
import { computed, ref } from "vue";

const toast = useToast();
const userStore = useUserStore();
const task = ref<TaskItem | null>(null);
const applications = ref<ApplicationItem[]>([]);
const loading = ref(true);
const submitting = ref(false);
const applyMessage = ref("");
const id = ref(0);

const canEdit = computed(() => {
  return Boolean(task.value?.mine && task.value.status === "WAITING" && (task.value.applyCount || 0) === 0);
});

const canApply = computed(() => {
  if (!task.value || task.value.mine || !userStore.isLoggedIn.value) {
    return false;
  }
  const open = task.value.status === "WAITING" || task.value.status === "APPLYING";
  const status = task.value.myApplicationStatus;
  return open && (!status || status === "CANCELLED");
});

const needLoginToApply = computed(() => {
  if (!task.value || task.value.mine || userStore.isLoggedIn.value) {
    return false;
  }
  return task.value.status === "WAITING" || task.value.status === "APPLYING";
});

const canWithdraw = computed(() => task.value?.myApplicationStatus === "PENDING");

const canConfirm = computed(() => {
  if (!task.value || task.value.status !== "MATCHED") {
    return false;
  }
  if (task.value.mine) {
    return task.value.publisherConfirmed !== 1;
  }
  if (task.value.matchedApplicant) {
    return task.value.applicantConfirmed !== 1;
  }
  return false;
});

const canComplete = computed(() => {
  if (!task.value || task.value.status !== "IN_PROGRESS") {
    return false;
  }
  if (task.value.mine) {
    return task.value.publisherCompleted !== 1;
  }
  if (task.value.matchedApplicant) {
    return task.value.applicantCompleted !== 1;
  }
  return false;
});

const canCancel = computed(() => {
  if (!task.value) {
    return false;
  }
  const status = task.value.status;
  if (status === "WAITING" || status === "APPLYING") {
    return Boolean(task.value.mine);
  }
  if (status === "MATCHED" || status === "CONFIRMED") {
    return Boolean(task.value.mine || task.value.matchedApplicant);
  }
  return false;
});

async function load() {
  if (!id.value) {
    return;
  }
  loading.value = true;
  try {
    task.value = await getTask(id.value);
    if (task.value.mine && (task.value.status === "WAITING" || task.value.status === "APPLYING" || task.value.status === "MATCHED")) {
      applications.value = await listApplications(id.value);
    } else {
      applications.value = [];
    }
  } catch (error) {
    toast.error((error as Error).message || "加载失败");
  } finally {
    loading.value = false;
  }
}

function goLogin() {
  uni.navigateTo({ url: "/pages/auth/login" });
}

function goEdit() {
  uni.navigateTo({ url: `/pages/task/edit?id=${id.value}` });
}

async function handleWithdraw() {
  if (!task.value?.myApplicationId) {
    toast.error("没有可撤回的申请");
    return;
  }
  submitting.value = true;
  try {
    task.value = await withdrawApplication(task.value.myApplicationId);
    toast.success("已撤回申请");
  } catch (error) {
    toast.error((error as Error).message || "撤回失败");
  } finally {
    submitting.value = false;
  }
}

async function handleApply() {
  if (!userStore.isLoggedIn.value) {
    goLogin();
    return;
  }
  submitting.value = true;
  try {
    task.value = await applyTask(id.value, applyMessage.value.trim() || undefined);
    toast.success("已申请");
    applyMessage.value = "";
  } catch (error) {
    toast.error((error as Error).message || "申请失败");
  } finally {
    submitting.value = false;
  }
}

async function handleAccept(appId: number) {
  submitting.value = true;
  try {
    task.value = await acceptApplication(appId);
    toast.success("已选择代课人");
    await load();
  } catch (error) {
    toast.error((error as Error).message || "操作失败");
  } finally {
    submitting.value = false;
  }
}

async function handleReject(appId: number) {
  submitting.value = true;
  try {
    task.value = await rejectApplication(appId);
    toast.success("已拒绝");
    await load();
  } catch (error) {
    toast.error((error as Error).message || "操作失败");
  } finally {
    submitting.value = false;
  }
}

async function handleConfirm() {
  submitting.value = true;
  try {
    task.value = await confirmTask(id.value);
    toast.success("已确认履约");
  } catch (error) {
    toast.error((error as Error).message || "确认失败");
  } finally {
    submitting.value = false;
  }
}

async function handleComplete() {
  submitting.value = true;
  try {
    task.value = await completeTask(id.value);
    toast.success("已确认完成");
  } catch (error) {
    toast.error((error as Error).message || "操作失败");
  } finally {
    submitting.value = false;
  }
}

function handleCancel() {
  uni.showModal({
    title: "取消代课",
    content: "确认取消该代课任务？",
    success: async (res) => {
      if (!res.confirm) {
        return;
      }
      submitting.value = true;
      try {
        task.value = await cancelTask(id.value);
        toast.success("已取消");
      } catch (error) {
        toast.error((error as Error).message || "取消失败");
      } finally {
        submitting.value = false;
      }
    }
  });
}

function handleDelete() {
  uni.showModal({
    title: "删除代课",
    content: "删除后不可恢复，确认删除？",
    success: async (res) => {
      if (!res.confirm) {
        return;
      }
      try {
        await deleteTask(id.value);
        toast.success("已删除");
        setTimeout(() => {
          uni.navigateBack();
        }, 400);
      } catch (error) {
        toast.error((error as Error).message || "删除失败");
      }
    }
  });
}

onLoad((query) => {
  id.value = Number(query?.id || 0);
  if (!id.value) {
    toast.error("任务不存在");
  }
});

onShow(() => {
  load();
});
</script>

<template>
  <view class="page">
    <view v-if="task" class="content">
      <view class="hero">
        <view class="title">{{ task.courseName }}</view>
        <view class="status">{{ TASK_STATUS_TEXT[task.status] || task.status }}</view>
      </view>
      <wd-cell-group border title="上课信息">
        <wd-cell title="日期" :value="task.taskDate" />
        <wd-cell title="时间" :value="`${task.startTime} - ${task.endTime}`" />
        <wd-cell title="校区" :value="task.campusName || '-'" />
        <wd-cell title="地点" :value="`${task.building} ${task.classroom}`" />
        <wd-cell title="是否上机" :value="task.computerLab === 1 ? '是' : '否'" />
        <wd-cell title="性别要求" :value="genderRequirementLabel(task.genderRequirement)" />
        <wd-cell title="酬谢" :value="formatReward(task.reward)" />
        <wd-cell title="申请人数" :value="`${task.applyCount} 人`" />
        <wd-cell title="发布时间" :value="parseDateTime(task.createdAt)" />
      </wd-cell-group>
      <wd-cell-group border title="发布者">
        <wd-cell title="昵称" :value="task.publisher?.nickname || '-'" />
        <wd-cell title="完成次数" :value="String(task.publisher?.completedCount ?? 0)" />
      </wd-cell-group>
      <wd-cell-group border title="说明">
        <wd-cell title="原因" :value="task.reason || '未填写'" />
        <wd-cell title="要求" :value="task.requirement || '未填写'" />
        <wd-cell title="备注" :value="task.remark || '未填写'" />
      </wd-cell-group>
      <wd-cell-group v-if="task.myApplicationStatus" border title="我的申请">
        <wd-cell title="状态" :value="APPLICATION_STATUS_TEXT[task.myApplicationStatus] || task.myApplicationStatus" />
      </wd-cell-group>
      <wd-cell-group v-if="task.status === 'MATCHED' || task.status === 'CONFIRMED'" border title="履约确认">
        <wd-cell title="发布者" :value="task.publisherConfirmed === 1 ? '已确认' : '未确认'" />
        <wd-cell title="代课者" :value="task.applicantConfirmed === 1 ? '已确认' : '未确认'" />
      </wd-cell-group>
      <wd-cell-group v-if="task.status === 'IN_PROGRESS' || task.status === 'COMPLETED'" border title="完成确认">
        <wd-cell title="发布者" :value="task.publisherCompleted === 1 ? '已完成' : '未确认'" />
        <wd-cell title="代课者" :value="task.applicantCompleted === 1 ? '已完成' : '未确认'" />
      </wd-cell-group>
      <view v-if="task.mine && applications.length" class="apps">
        <view class="apps-title">申请人</view>
        <view v-for="item in applications" :key="item.id" class="app-card">
          <view class="app-top">
            <text class="name">{{ item.nickname || "同学" }}</text>
            <text class="app-status">{{ APPLICATION_STATUS_TEXT[item.status] || item.status }}</text>
          </view>
          <view class="msg">{{ item.message || "无留言" }}</view>
          <view v-if="item.status === 'PENDING' && task.status === 'APPLYING'" class="app-actions">
            <wd-button size="small" type="primary" :disabled="submitting" @click="handleAccept(item.id)">接受</wd-button>
            <wd-button size="small" plain :disabled="submitting" @click="handleReject(item.id)">拒绝</wd-button>
          </view>
        </view>
      </view>
      <view v-if="canApply" class="apply-box">
        <wd-textarea v-model="applyMessage" placeholder="申请留言，选填" :maxlength="500" />
        <wd-button type="primary" block :loading="submitting" @click="handleApply">申请代课</wd-button>
      </view>
      <view class="footer">
        <wd-button v-if="canEdit" type="primary" block @click="goEdit">编辑</wd-button>
        <wd-button v-if="canEdit" type="error" plain block @click="handleDelete">删除</wd-button>
        <wd-button v-if="canWithdraw" plain block :loading="submitting" @click="handleWithdraw">撤回申请</wd-button>
        <wd-button v-if="canConfirm" type="primary" block :loading="submitting" @click="handleConfirm">确认履约</wd-button>
        <wd-button v-if="canComplete" type="primary" block :loading="submitting" @click="handleComplete">确认完成</wd-button>
        <wd-button v-if="canCancel" type="warning" plain block :loading="submitting" @click="handleCancel">取消任务</wd-button>
        <wd-button v-if="needLoginToApply" type="primary" block @click="goLogin">登录后申请</wd-button>
      </view>
    </view>
    <wd-status-tip v-else-if="!loading" image="content" tip="任务不存在" />
    <wd-toast />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 24px;
}
.hero {
  padding: 20px 16px 8px;
}
.title {
  font-size: 22px;
  font-weight: 600;
  color: #1d2129;
}
.status {
  margin-top: 6px;
  color: #4d80f0;
  font-size: 13px;
}
.apps,
.apply-box {
  margin: 12px 16px 0;
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
}
.apps-title {
  font-size: 14px;
  color: #86909c;
  margin-bottom: 8px;
}
.app-card {
  padding: 10px 0;
  border-bottom: 1px solid #f2f3f5;
}
.app-card:last-child {
  border-bottom: none;
}
.app-top {
  display: flex;
  justify-content: space-between;
}
.name {
  font-weight: 600;
}
.app-status {
  color: #4d80f0;
  font-size: 12px;
}
.msg {
  margin-top: 6px;
  color: #4e5969;
  font-size: 13px;
}
.app-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}
.footer {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>

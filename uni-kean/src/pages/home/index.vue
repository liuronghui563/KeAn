<script setup lang="ts">
import { listCampuses } from "@/api/catalog";
import { listTasks, TASK_STATUS_TEXT, type TaskItem } from "@/api/task";
import { formatDate, formatReward } from "@/utils/format";
import { onPullDownRefresh, onReachBottom, onShow } from "@dcloudio/uni-app";
import { useToast } from "wot-design-uni";
import { onMounted, reactive, ref } from "vue";

const toast = useToast();
const loading = ref(false);
const finished = ref(false);
const list = ref<TaskItem[]>([]);
const keyword = ref("");
const filter = reactive({
  campusId: 0 as number,
  status: "ALL"
});
const selectedDate = ref("");
const datePickerValue = ref<number | string>("");
const page = ref(1);
const total = ref(0);

const campusOptions = ref([{ label: "全部校区", value: 0 }]);
const statusOptions = [
  { label: "可申请", value: "ALL" },
  { label: "待申请", value: "WAITING" },
  { label: "申请中", value: "APPLYING" }
];

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/task/detail?id=${id}` });
}

async function loadCatalog() {
  const campuses = await listCampuses();
  campusOptions.value = [{ label: "全部校区", value: 0 }, ...campuses.map((item) => ({ label: item.name, value: item.id }))];
}

async function loadList(reset = false) {
  if (loading.value) {
    return;
  }
  if (reset) {
    page.value = 1;
    finished.value = false;
  }
  loading.value = true;
  try {
    const data = await listTasks({
      keyword: keyword.value.trim() || undefined,
      campusId: Number(filter.campusId) || undefined,
      status: filter.status === "ALL" ? undefined : filter.status,
      taskDate: selectedDate.value || undefined,
      page: page.value,
      size: 10
    });
    total.value = data.total;
    list.value = reset ? data.list : list.value.concat(data.list);
    finished.value = list.value.length >= data.total;
  } catch (error) {
    toast.error((error as Error).message || "加载失败");
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
}

function onSearch() {
  loadList(true);
}

function onFilterChange() {
  loadList(true);
}

function onDateConfirm(event: { value?: string | number }) {
  const value = event?.value ?? datePickerValue.value;
  if (value === "" || value === undefined || value === null) {
    selectedDate.value = "";
  } else {
    selectedDate.value = formatDate(Number(value));
  }
  loadList(true);
}

function onDateClear() {
  selectedDate.value = "";
  datePickerValue.value = "";
  loadList(true);
}

onShow(() => {
  loadList(true);
});

onMounted(() => {
  loadCatalog().catch(() => undefined);
});

onPullDownRefresh(() => {
  loadList(true);
});

onReachBottom(() => {
  if (finished.value || loading.value) {
    return;
  }
  page.value += 1;
  loadList(false);
});
</script>

<template>
  <view class="page">
    <view class="search">
      <wd-search v-model="keyword" placeholder="搜索课程 / 教学楼 / 教室" hide-cancel @search="onSearch" @clear="onSearch" />
    </view>
    <wd-drop-menu>
      <wd-drop-menu-item v-model="filter.campusId" :options="campusOptions" @change="onFilterChange" />
      <wd-drop-menu-item v-model="filter.status" :options="statusOptions" @change="onFilterChange" />
    </wd-drop-menu>
    <wd-datetime-picker
      v-model="datePickerValue"
      type="date"
      label="上课日期"
      placeholder="不限日期"
      clearable
      @confirm="onDateConfirm"
      @clear="onDateClear"
    />
    <view v-if="list.length" class="list">
      <view v-for="item in list" :key="item.id" class="card" @click="goDetail(item.id)">
        <view class="card-top">
          <text class="course">{{ item.courseName }}</text>
          <text class="status">{{ TASK_STATUS_TEXT[item.status] || item.status }}</text>
        </view>
        <view class="meta">{{ item.taskDate }} {{ item.startTime }}-{{ item.endTime }}</view>
        <view class="meta">{{ item.campusName }} · {{ item.building }} {{ item.classroom }}</view>
        <view class="card-bottom">
          <text class="reward">{{ formatReward(item.reward) }}</text>
          <text class="count">{{ item.applyCount }} 人申请</text>
        </view>
      </view>
      <view class="end">{{ finished ? "没有更多了" : "上拉加载更多" }}</view>
    </view>
    <wd-status-tip v-else-if="!loading" image="content" tip="暂无代课任务" />
    <wd-toast />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f8;
}
.search {
  background: #fff;
}
.list {
  padding: 12px 16px 24px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.course {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}
.status {
  font-size: 12px;
  color: #4d80f0;
}
.meta {
  margin-top: 8px;
  color: #4e5969;
  font-size: 13px;
}
.card-bottom {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  color: #86909c;
  font-size: 13px;
}
.reward {
  color: #f77234;
  font-weight: 600;
}
.end {
  text-align: center;
  color: #86909c;
  font-size: 12px;
  padding: 8px 0 16px;
}
</style>

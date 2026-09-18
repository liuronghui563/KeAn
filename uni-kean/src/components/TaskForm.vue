<script setup lang="ts">
import { listCampuses } from "@/api/catalog";
import { createTask, getTask, updateTask, type TaskPayload } from "@/api/task";
import { useUserStore } from "@/store/user";
import { formatDate, tomorrowAt } from "@/utils/format";
import { useToast } from "wot-design-uni";
import { onMounted, reactive, ref } from "vue";

const props = defineProps<{ taskId?: number }>();
const emit = defineEmits<{ success: [id: number] }>();

const toast = useToast();
const userStore = useUserStore();
const formRef = ref();
const loading = ref(false);

const model = reactive({
  courseName: "",
  campusId: "" as number | string,
  taskDate: tomorrowAt(8, 0),
  startTime: "08:00",
  endTime: "09:40",
  building: "",
  classroom: "",
  computerLab: "0",
  genderRequirement: "ANY",
  reward: "0",
  reason: "",
  requirement: "",
  remark: ""
});

const minDate = Date.now();
const campusColumns = ref<{ label: string; value: number }[]>([]);
const computerLabColumns = [
  { label: "否", value: "0" },
  { label: "是", value: "1" }
];
const genderRequirementColumns = [
  { label: "不限", value: "ANY" },
  { label: "仅限男生", value: "MALE" },
  { label: "仅限女生", value: "FEMALE" }
];

async function loadCatalog() {
  const schoolId = userStore.state.user?.schoolId || 1;
  const campusList = await listCampuses(schoolId);
  campusColumns.value = campusList.map((item) => ({ label: item.name, value: item.id }));
  if (!model.campusId && campusList.length) {
    model.campusId = campusList[0].id;
  }
}

async function loadDetail() {
  if (!props.taskId) {
    return;
  }
  const task = await getTask(props.taskId);
  model.courseName = task.courseName;
  model.campusId = task.campusId;
  model.taskDate = new Date(`${task.taskDate}T00:00:00`).getTime();
  model.startTime = task.startTime;
  model.endTime = task.endTime;
  model.building = task.building;
  model.classroom = task.classroom;
  model.computerLab = task.computerLab === 1 ? "1" : "0";
  model.genderRequirement = task.genderRequirement || "ANY";
  model.reward = String(task.reward ?? 0);
  model.reason = task.reason || "";
  model.requirement = task.requirement || "";
  model.remark = task.remark || "";
}

function buildPayload(): TaskPayload {
  return {
    courseName: model.courseName.trim(),
    campusId: Number(model.campusId),
    taskDate: formatDate(Number(model.taskDate)),
    startTime: String(model.startTime).slice(0, 5),
    endTime: String(model.endTime).slice(0, 5),
    building: model.building.trim(),
    classroom: model.classroom.trim(),
    computerLab: String(model.computerLab) === "1",
    genderRequirement: String(model.genderRequirement),
    reward: Number(model.reward || 0),
    reason: model.reason.trim() || undefined,
    requirement: model.requirement.trim() || undefined,
    remark: model.remark.trim() || undefined
  };
}

function handleSubmit() {
  formRef.value
    .validate()
    .then(async ({ valid }: { valid: boolean }) => {
      if (!valid) {
        return;
      }
      loading.value = true;
      try {
        const payload = buildPayload();
        const saved = props.taskId ? await updateTask(props.taskId, payload) : await createTask(payload);
        toast.success(props.taskId ? "已保存" : "发布成功");
        emit("success", saved.id);
      } catch (error) {
        toast.error((error as Error).message || "提交失败");
      } finally {
        loading.value = false;
      }
    })
    .catch(() => undefined);
}

onMounted(async () => {
  try {
    await loadCatalog();
    await loadDetail();
  } catch (error) {
    toast.error((error as Error).message || "加载失败");
  }
});
</script>

<template>
  <wd-form ref="formRef" :model="model" error-type="toast">
    <wd-cell-group border>
      <wd-input
        v-model="model.courseName"
        label="课程名"
        label-width="80px"
        prop="courseName"
        clearable
        placeholder="请输入课程名称"
        :rules="[{ required: true, message: '请填写课程名' }]"
      />
      <wd-datetime-picker
        v-model="model.taskDate"
        type="date"
        label="上课日期"
        label-width="80px"
        prop="taskDate"
        :min-date="minDate"
        :rules="[{ required: true, message: '请选择上课日期' }]"
      />
      <wd-datetime-picker
        v-model="model.startTime"
        type="time"
        label="开始时间"
        label-width="80px"
        prop="startTime"
        :rules="[{ required: true, message: '请选择开始时间' }]"
      />
      <wd-datetime-picker
        v-model="model.endTime"
        type="time"
        label="结束时间"
        label-width="80px"
        prop="endTime"
        :rules="[{ required: true, message: '请选择结束时间' }]"
      />
      <wd-picker
        v-model="model.campusId"
        label="校区"
        label-width="80px"
        prop="campusId"
        :columns="campusColumns"
        :rules="[{ required: true, message: '请选择校区' }]"
      />
      <wd-input
        v-model="model.building"
        label="教学楼"
        label-width="80px"
        prop="building"
        clearable
        placeholder="例如 教学楼A"
        :rules="[{ required: true, message: '请填写教学楼' }]"
      />
      <wd-input
        v-model="model.classroom"
        label="教室"
        label-width="80px"
        prop="classroom"
        clearable
        placeholder="例如 101"
        :rules="[{ required: true, message: '请填写教室' }]"
      />
      <wd-picker
        v-model="model.computerLab"
        label="是否上机"
        label-width="80px"
        prop="computerLab"
        :columns="computerLabColumns"
        :rules="[{ required: true, message: '请选择是否上机' }]"
      />
      <wd-picker
        v-model="model.genderRequirement"
        label="性别要求"
        label-width="80px"
        prop="genderRequirement"
        :columns="genderRequirementColumns"
        :rules="[{ required: true, message: '请选择性别要求' }]"
      />
      <wd-input
        v-model="model.reward"
        label="酬谢"
        label-width="80px"
        prop="reward"
        type="digit"
        placeholder="仅展示，允许 0"
        :rules="[{ required: true, message: '请填写酬谢金额' }]"
      />
      <wd-textarea v-model="model.reason" label="代课原因" label-width="80px" placeholder="选填" :maxlength="500" />
      <wd-textarea v-model="model.requirement" label="代课要求" label-width="80px" placeholder="选填" :maxlength="500" />
      <wd-textarea v-model="model.remark" label="备注" label-width="80px" placeholder="选填" :maxlength="500" />
    </wd-cell-group>
    <view class="footer">
      <wd-button type="primary" size="large" block :loading="loading" @click="handleSubmit">
        {{ taskId ? "保存修改" : "发布代课" }}
      </wd-button>
    </view>
  </wd-form>
</template>

<style scoped>
.footer {
  padding: 24px 16px 40px;
}
</style>

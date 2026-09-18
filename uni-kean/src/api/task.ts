import { request } from "@/utils/request";

export interface PublisherBrief {
  id: number;
  nickname: string;
  avatarUrl?: string | null;
  completedCount: number;
}

export interface TaskItem {
  id: number;
  publisherId: number;
  courseId?: number | null;
  courseName: string;
  taskDate: string;
  startTime: string;
  endTime: string;
  startAt?: string;
  endAt?: string;
  schoolId: number;
  campusId: number;
  campusName?: string | null;
  building: string;
  classroom: string;
  computerLab?: number | null;
  genderRequirement?: string | null;
  reward: number;
  reason?: string | null;
  requirement?: string | null;
  remark?: string | null;
  status: string;
  applyCount: number;
  createdAt: string;
  publisher?: PublisherBrief | null;
  mine?: boolean;
  publisherConfirmed?: number;
  applicantConfirmed?: number;
  publisherCompleted?: number;
  applicantCompleted?: number;
  acceptedApplicationId?: number | null;
  myApplicationStatus?: string | null;
  myApplicationId?: number | null;
  matchedApplicant?: boolean;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

export interface TaskQuery {
  keyword?: string;
  taskDate?: string;
  courseId?: number;
  campusId?: number;
  status?: string;
  page?: number;
  size?: number;
}

export interface TaskPayload {
  courseName: string;
  taskDate: string;
  startTime: string;
  endTime: string;
  campusId: number;
  building: string;
  classroom: string;
  computerLab: boolean;
  genderRequirement: string;
  reward: number;
  reason?: string;
  requirement?: string;
  remark?: string;
}

function compact(params: TaskQuery) {
  const data: Record<string, string | number> = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return;
    }
    data[key] = value as string | number;
  });
  return data;
}

export function listTasks(params: TaskQuery) {
  return request<PageResult<TaskItem>>({
    url: "/api/tasks",
    method: "GET",
    data: compact(params)
  });
}

export function getTask(id: number) {
  return request<TaskItem>({
    url: `/api/tasks/${id}`,
    method: "GET"
  });
}

export function createTask(payload: TaskPayload) {
  return request<TaskItem>({
    url: "/api/tasks",
    method: "POST",
    data: payload
  });
}

export function updateTask(id: number, payload: TaskPayload) {
  return request<TaskItem>({
    url: `/api/tasks/${id}`,
    method: "PUT",
    data: payload
  });
}

export function deleteTask(id: number) {
  return request<null>({
    url: `/api/tasks/${id}`,
    method: "DELETE"
  });
}

export function confirmTask(id: number) {
  return request<TaskItem>({
    url: `/api/tasks/${id}/confirm`,
    method: "POST"
  });
}

export function completeTask(id: number) {
  return request<TaskItem>({
    url: `/api/tasks/${id}/complete`,
    method: "POST"
  });
}

export function cancelTask(id: number, reason?: string) {
  return request<TaskItem>({
    url: `/api/tasks/${id}/cancel`,
    method: "POST",
    data: { reason }
  });
}

export function listMyPublished(page = 1, size = 20) {
  return request<PageResult<TaskItem>>({
    url: "/api/me/published",
    method: "GET",
    data: { page, size }
  });
}

export function listMyApplied(page = 1, size = 20) {
  return request<PageResult<TaskItem>>({
    url: "/api/me/applied",
    method: "GET",
    data: { page, size }
  });
}

export const TASK_STATUS_TEXT: Record<string, string> = {
  WAITING: "待申请",
  APPLYING: "申请中",
  MATCHED: "已匹配",
  CONFIRMED: "已确认",
  IN_PROGRESS: "进行中",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
  EXPIRED: "已过期"
};

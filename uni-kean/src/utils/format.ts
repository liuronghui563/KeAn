export function pad(value: number) {
  return String(value).padStart(2, "0");
}

export function formatDate(ts: number) {
  const date = new Date(ts);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function formatTime(value: number | string) {
  if (typeof value === "string") {
    return value.slice(0, 5);
  }
  const date = new Date(value);
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function parseDateTime(iso?: string | null) {
  if (!iso) {
    return "";
  }
  return iso.replace("T", " ").slice(0, 16);
}

export function tomorrowAt(hour: number, minute: number) {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  date.setHours(hour, minute, 0, 0);
  return date.getTime();
}

export function formatReward(value: number | string | null | undefined) {
  const amount = Number(value || 0);
  if (Number.isNaN(amount) || amount <= 0) {
    return "无偿";
  }
  return `¥${amount}`;
}

export function genderLabel(value?: string | null) {
  if (value === "MALE") {
    return "男";
  }
  if (value === "FEMALE") {
    return "女";
  }
  return "未设置";
}

export function genderRequirementLabel(value?: string | null) {
  if (value === "MALE") {
    return "仅限男生";
  }
  if (value === "FEMALE") {
    return "仅限女生";
  }
  return "不限";
}

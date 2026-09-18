import { clearAuth, getToken } from "./storage";

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

interface RequestOptions {
  url: string;
  method?: UniApp.RequestOptions["method"];
  data?: unknown;
}

/** App 真机必须用绝对地址。后端跑在主机上，数据库在虚拟机 192.168.198.139。 */
const APP_FALLBACK_BASE = "http://10.232.131.239:8080";
const REQUEST_TIMEOUT_MS = 15000;

function resolveBaseUrl(): string {
  const fromEnv = String(import.meta.env.VITE_API_BASE_URL || "").trim();
  // #ifdef APP-PLUS
  return (fromEnv || APP_FALLBACK_BASE).replace(/\/$/, "");
  // #endif
  // #ifdef H5
  if (typeof window !== "undefined" && /^(localhost|127\.0\.0\.1)$/i.test(window.location.hostname)) {
    return "";
  }
  return (fromEnv || APP_FALLBACK_BASE).replace(/\/$/, "");
  // #endif
  return (fromEnv || APP_FALLBACK_BASE).replace(/\/$/, "");
}

function buildUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  const normalized = path.startsWith("/") ? path : `/${path}`;
  const base = resolveBaseUrl();
  if (!base) {
    return normalized;
  }
  return `${base}${normalized}`;
}

function parseBody<T>(raw: unknown): ApiResult<T> | null {
  if (raw == null) {
    return null;
  }
  if (typeof raw === "string") {
    try {
      return JSON.parse(raw) as ApiResult<T>;
    } catch {
      return null;
    }
  }
  if (typeof raw === "object") {
    return raw as ApiResult<T>;
  }
  return null;
}

export function request<T>(options: RequestOptions): Promise<T> {
  const token = getToken();
  const method = options.method || "GET";
  const header: Record<string, string> = {};
  if (method !== "GET") {
    header["Content-Type"] = "application/json";
  }
  if (token) {
    header.Authorization = `Bearer ${token}`;
  }

  const url = buildUrl(options.url);
  // #ifdef APP-PLUS
  if (!/^https?:\/\//i.test(url)) {
    return Promise.reject(new Error("接口地址必须是 http/https，请配置 VITE_API_BASE_URL"));
  }
  // #endif

  const payload = options.data;
  const hasData = payload !== undefined && payload !== null
    && !(typeof payload === "object" && !Array.isArray(payload) && Object.keys(payload as object).length === 0);

  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method,
      data: hasData ? payload : undefined,
      header,
      timeout: REQUEST_TIMEOUT_MS,
      success: (res) => {
        const body = parseBody<T>(res.data);
        if (res.statusCode === 401 || body?.code === 40100) {
          clearAuth();
          reject(new Error(body?.message || "未登录或登录已失效"));
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body?.code === 0) {
          resolve(body.data);
          return;
        }
        reject(new Error(body?.message || `请求失败(${res.statusCode})`));
      },
      fail: (err) => {
        const msg = err.errMsg || "网络异常";
        if (/timeout/i.test(msg)) {
          reject(new Error("请求超时，请确认主机后端 10.232.131.239:8080 已启动"));
          return;
        }
        reject(new Error(`无法连接主机后端 10.232.131.239:8080（${msg}）`));
      }
    });
  });
}

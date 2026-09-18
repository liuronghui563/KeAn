import { request } from "@/utils/request";
import type { AdminUser } from "@/utils/storage";

export interface LoginResult {
  token: string;
  user: AdminUser;
}

export function login(username: string, password: string) {
  return request<LoginResult>("/api/auth/login", {
    method: "POST",
    data: { username, password }
  });
}

export function fetchMe() {
  return request<AdminUser>("/api/auth/me");
}

export function logout() {
  return request<null>("/api/auth/logout", { method: "POST" });
}

import { request } from "@/utils/request";
import type { AuthUser } from "@/utils/storage";

export interface LoginPayload {
  username: string;
  password: string;
}

export interface RegisterPayload {
  username: string;
  password: string;
  nickname: string;
  gender: string;
  schoolId: number;
  campusId: number;
  phone?: string;
}

export interface LoginResult {
  token: string;
  user: AuthUser;
}

export function login(payload: LoginPayload) {
  return request<LoginResult>({
    url: "/api/auth/login",
    method: "POST",
    data: payload
  });
}

export function register(payload: RegisterPayload) {
  return request<AuthUser>({
    url: "/api/auth/register",
    method: "POST",
    data: payload
  });
}

export function fetchMe() {
  return request<AuthUser>({
    url: "/api/auth/me",
    method: "GET"
  });
}

export function logout() {
  return request<null>({
    url: "/api/auth/logout",
    method: "POST"
  });
}

import { api } from "./api";

const TOKEN_KEY = "speakpro_token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
  // 同步写入 Cookie，供 Next.js middleware 服务端路由保护使用
  // SameSite=Strict 防止 CSRF；不设置 HttpOnly 因为 JS 需要读取
  document.cookie = `${TOKEN_KEY}=${token}; path=/; SameSite=Strict; max-age=${7 * 24 * 3600}`;
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY);
  // 同步清除 Cookie
  document.cookie = `${TOKEN_KEY}=; path=/; SameSite=Strict; max-age=0`;
}

export function isAuthenticated(): boolean {
  return !!getToken();
}

interface LoginParams {
  email: string;
  password: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    name: string;
    email: string;
    role: string;
  };
}

export async function login(params: LoginParams): Promise<AuthResponse> {
  const response = await api.post<AuthResponse>("/auth/login", params);
  setToken(response.accessToken);
  localStorage.setItem("speakpro_refresh_token", response.refreshToken);
  localStorage.setItem("speakpro_user", JSON.stringify(response.user));
  return response;
}

// ============ 教师端注册 + 邮件重置 ============

interface RegisterParams {
  name: string;
  email: string;
  password: string;
  /** 目前仅支持教师端自主注册；后续若开放 admin 可加字段 */
  role?: "teacher";
}

/**
 * 教师端注册：复用后端 /auth/register，role 固定 teacher。
 * 成功后自动登录。
 */
export async function register(params: RegisterParams): Promise<AuthResponse> {
  const response = await api.post<AuthResponse>("/auth/register", {
    name: params.name,
    email: params.email,
    password: params.password,
    role: params.role ?? "teacher",
  });
  setToken(response.accessToken);
  localStorage.setItem("speakpro_refresh_token", response.refreshToken);
  localStorage.setItem("speakpro_user", JSON.stringify(response.user));
  return response;
}

/**
 * 请求邮件重置：无论邮箱是否存在，后端都返回 ok，以防账号枚举。
 */
export async function requestEmailReset(email: string): Promise<void> {
  await api.post<{ ok: boolean }>("/auth/request-email-reset", { email });
}

/**
 * 用邮件 token 重置密码。
 */
export async function resetEmailPassword(
  token: string,
  newPassword: string,
): Promise<void> {
  await api.post<{ ok: boolean }>("/auth/reset-email-password", {
    token,
    newPassword,
  });
}

export function getUser(): AuthResponse["user"] | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem("speakpro_user");
  return raw ? JSON.parse(raw) : null;
}

export function logout(): void {
  removeToken();
  localStorage.removeItem("speakpro_refresh_token");
  localStorage.removeItem("speakpro_user");
  window.location.href = "/login";
}

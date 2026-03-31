import { api } from "./api";

const TOKEN_KEY = "speakpro_token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY);
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

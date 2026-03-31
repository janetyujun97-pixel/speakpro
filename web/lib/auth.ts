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

interface LoginResponse {
  token: string;
  user: {
    id: string;
    name: string;
    email: string;
    role: string;
  };
}

export async function login(params: LoginParams): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>("/auth/login", params);
  setToken(response.token);
  return response;
}

export function logout(): void {
  removeToken();
  window.location.href = "/login";
}

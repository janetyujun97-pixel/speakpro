const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost/api/v1";

function getAuthHeaders(): HeadersInit {
  if (typeof window === "undefined") return {};
  const token = localStorage.getItem("speakpro_token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// NestJS 统一响应格式
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${BASE_URL}${endpoint}`;

  const response = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...getAuthHeaders(),
      ...options.headers,
    },
  });

  const json: ApiResponse<T> = await response.json().catch(() => ({
    code: response.status,
    message: `请求失败: ${response.status}`,
    data: null as T,
  }));

  if (!response.ok || json.code !== 0) {
    throw new Error(json.message || `请求失败: ${response.status}`);
  }

  return json.data;
}

export const api = {
  get<T>(endpoint: string): Promise<T> {
    return request<T>(endpoint, { method: "GET" });
  },

  post<T>(endpoint: string, data?: unknown): Promise<T> {
    return request<T>(endpoint, {
      method: "POST",
      body: data ? JSON.stringify(data) : undefined,
    });
  },

  put<T>(endpoint: string, data?: unknown): Promise<T> {
    return request<T>(endpoint, {
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    });
  },

  delete<T>(endpoint: string): Promise<T> {
    return request<T>(endpoint, { method: "DELETE" });
  },
};

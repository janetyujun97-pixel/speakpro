"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";

/** React Query 全局配置 Provider */
export function QueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // 数据 5 分钟内不重新请求
            staleTime: 5 * 60 * 1000,
            // 缓存保留 10 分钟
            gcTime: 10 * 60 * 1000,
            // 失败重试 2 次
            retry: 2,
            // 窗口获取焦点时不自动刷新（教师后台通常长时间打开）
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

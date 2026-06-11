import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// 无需登录可访问的路径
const PUBLIC_PATHS = ["/login", "/register", "/forgot-password", "/reset-password"];

// 登录后不应再访问的路径（如已登录访问 /login 会跳转 Dashboard）
const AUTH_ONLY_PATHS = ["/login", "/register"];

// 仅管理员可访问的路径前缀（真正的鉴权在后端，这里只做 UX 跳转）
const ADMIN_PATHS = ["/admin", "/system"];

// 解码 JWT payload 读取 role（不校验签名，仅用于前端路由跳转）
function readRoleFromToken(token: string): string | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const json = atob(base64);
    const data = JSON.parse(json);
    return data?.role ?? null;
  } catch {
    return null;
  }
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // 静态资源 / Next.js 内部路径直接放行
  if (
    pathname.startsWith("/_next") ||
    pathname.startsWith("/api") ||
    pathname.startsWith("/favicon") ||
    pathname.includes(".")
  ) {
    return NextResponse.next();
  }

  // 从 Cookie 读取 token（SSR 环境无法访问 localStorage）
  // Web 端同时在 Cookie 中维护 token 以支持服务端路由保护
  const tokenCookie = request.cookies.get("speakpro_token");
  const isAuthenticated = !!tokenCookie?.value;

  const isPublicPath = PUBLIC_PATHS.some((p) => pathname === p || pathname.startsWith(p + "/"));
  const isAuthOnlyPath = AUTH_ONLY_PATHS.some((p) => pathname === p || pathname.startsWith(p + "/"));

  // 已登录用户访问登录/注册页 → 重定向到 Dashboard
  if (isAuthenticated && isAuthOnlyPath) {
    return NextResponse.redirect(new URL("/", request.url));
  }

  // 未登录用户访问受保护页面 → 重定向到登录页
  if (!isAuthenticated && !isPublicPath) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("from", pathname);
    return NextResponse.redirect(loginUrl);
  }

  // 管理员专属路径：非 admin 重定向回首页
  const isAdminPath = ADMIN_PATHS.some((p) => pathname === p || pathname.startsWith(p + "/"));
  if (isAuthenticated && isAdminPath) {
    const role = readRoleFromToken(tokenCookie!.value);
    if (role !== "admin") {
      return NextResponse.redirect(new URL("/", request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  // 匹配所有路径（除静态资源外，已在 middleware 函数内处理）
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};

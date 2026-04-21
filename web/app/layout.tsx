import type { Metadata } from "next";
import { Providers } from "./providers";
import "./globals.css";

// Editorial 字体组合 — 与 design/SpeakPro Admin 设计稿一致
// 使用 CDN <link> 加载以规避 next/font 在受限网络下的构建时抓取失败；
// CSS 变量名（--font-inter / --font-fraunces / --font-mono）与 tailwind.config.ts 保持一致

export const metadata: Metadata = {
  title: "SpeakPro 教师后台",
  description: "SpeakPro 教师管理后台 - 口语教学与评测平台",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="zh-CN">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Fraunces:ital,opsz,wght@0,9..144,400;0,9..144,500;0,9..144,600;1,9..144,400;1,9..144,500&family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap"
        />
        <style
          dangerouslySetInnerHTML={{
            __html: `
              :root {
                --font-inter: 'Inter', 'PingFang SC', -apple-system, system-ui, sans-serif;
                --font-fraunces: 'Fraunces', 'Source Serif Pro', Georgia, serif;
                --font-mono: 'JetBrains Mono', 'SF Mono', ui-monospace, monospace;
              }
            `,
          }}
        />
      </head>
      <body className="font-sans antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}

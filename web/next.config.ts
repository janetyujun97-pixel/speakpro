import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 启用压缩
  compress: true,

  // 图片优化
  images: {
    formats: ["image/avif", "image/webp"],
    remotePatterns: [
      {
        protocol: "https",
        hostname: "**.aliyuncs.com",
      },
    ],
  },

  // 实验性优化
  experimental: {
    optimizeCss: true,
  },

  // 安全头
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          { key: "X-Frame-Options", value: "DENY" },
          { key: "X-Content-Type-Options", value: "nosniff" },
        ],
      },
    ];
  },
};

export default nextConfig;

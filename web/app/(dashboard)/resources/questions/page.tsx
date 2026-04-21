"use client";

// 旧路由兼容层：重定向到统一的 /resources?tab=questions
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Mono } from "@/components/editorial/primitives";

export default function QuestionsRedirect() {
  const router = useRouter();
  useEffect(() => {
    router.replace("/resources?tab=questions");
  }, [router]);
  return (
    <div className="py-20 text-center">
      <Mono size={11}>— 正在跳转到题库 —</Mono>
    </div>
  );
}

"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";

interface DashboardStats {
  activeStudents: number;
  weeklyPractice: number;
  averageScore: number;
  completionRate: number;
  activeStudentsTrend: number;
  weeklyPracticeTrend: number;
  averageScoreTrend: number;
  completionRateTrend: number;
}

interface ScoreTrend {
  date: string;
  pronunciation: number;
  fluency: number;
  grammar: number;
}

interface StudentRank {
  id: string;
  name: string;
  class: string;
  avgScore: number;
  practiceCount: number;
}

interface DashboardData {
  stats: DashboardStats;
  scoreTrends: ScoreTrend[];
  studentRanks: StudentRank[];
}

export function useDashboardStats() {
  return useQuery<DashboardStats>({
    queryKey: ["dashboard", "stats"],
    queryFn: () => api.get<DashboardStats>("/dashboard/stats"),
  });
}

export function useDashboardScoreTrends() {
  return useQuery<ScoreTrend[]>({
    queryKey: ["dashboard", "score-trends"],
    queryFn: () => api.get<ScoreTrend[]>("/dashboard/score-trends"),
  });
}

export function useDashboardStudentRanks() {
  return useQuery<StudentRank[]>({
    queryKey: ["dashboard", "student-ranks"],
    queryFn: () => api.get<StudentRank[]>("/dashboard/student-ranks"),
  });
}

export function useDashboardData() {
  return useQuery<DashboardData>({
    queryKey: ["dashboard"],
    queryFn: () => api.get<DashboardData>("/dashboard"),
  });
}

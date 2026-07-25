import { apiFetch } from "./client";

export interface AdminUser {
  id: string;
  email: string;
  role: "ADMIN" | "USER";
  createdAt: string;
}

interface ActionResult {
  success: boolean;
  message: string;
}

export function fetchAdminUsers(): Promise<AdminUser[]> {
  return apiFetch<AdminUser[]>("/api/v1/admin/users");
}

export function updateUserRole(userId: string, role: "ADMIN" | "USER"): Promise<ActionResult> {
  return apiFetch<ActionResult>(`/api/v1/admin/users/${userId}/role`, {
    method: "PUT",
    body: JSON.stringify({ role }),
  });
}

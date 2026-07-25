import { apiFetch } from "./client";

interface ActionResult {
  success: boolean;
  message: string;
}

export function changePassword(currentPassword: string, newPassword: string): Promise<ActionResult> {
  return apiFetch<ActionResult>("/api/v1/users/me/password", {
    method: "POST",
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

export function changeEmail(newEmail: string, currentPassword: string): Promise<ActionResult> {
  return apiFetch<ActionResult>("/api/v1/users/me/email", {
    method: "POST",
    body: JSON.stringify({ newEmail, currentPassword }),
  });
}

import { useState } from "react";
import { ShieldCheck, UserCheck, Users, Search, RefreshCw, AlertCircle, CheckCircle2, X } from "lucide-react";
import { Dialog } from "../../shared/components/Dialog";
import { useAdminUsersQuery, useUpdateUserRoleMutation } from "../../shared/hooks/useAdminQueries";
import { useAuth } from "../auth/AuthContext";
import type { AdminUser } from "../../shared/api/adminApi";

interface AdminUserManagementModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function AdminUserManagementModal({ isOpen, onClose }: AdminUserManagementModalProps) {
  const { user: currentUser } = useAuth();
  const [searchQuery, setSearchQuery] = useState("");
  const [msg, setMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const { data: users = [], isLoading, isFetching, isError, refetch } = useAdminUsersQuery(isOpen);
  const updateRoleMutation = useUpdateUserRoleMutation();

  const handleRoleToggle = (targetUser: AdminUser) => {
    const newRole = targetUser.role === "ADMIN" ? "USER" : "ADMIN";
    setMsg(null);
    updateRoleMutation.mutate(
      { userId: targetUser.id, role: newRole },
      {
        onSuccess: () => {
          setMsg({ type: "success", text: `${targetUser.email} kullanıcısının rolü '${newRole}' olarak güncellendi.` });
        },
        onError: (err) => {
          setMsg({ type: "error", text: err instanceof Error ? err.message : "Rol değiştirme başarısız oldu." });
        },
      },
    );
  };

  const filteredUsers = users.filter((u) => u.email.toLowerCase().includes(searchQuery.toLowerCase()));
  const loading = isLoading || (isFetching && users.length === 0);

  return (
    <Dialog open={isOpen} onClose={onClose} title="Kullanıcı ve Yetki Yönetimi" maxWidthClassName="max-w-3xl">
      <div className="flex h-[85vh] flex-col">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-border p-6">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/10 text-amber-500">
              <Users className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-text-primary">Kullanıcı & Yetki Yönetimi</h3>
              <p className="text-xs text-text-muted">Sistemdeki tüm kullanıcıları listeleyin ve admin yetkisi tanımlayın</p>
            </div>
          </div>
          <button
            onClick={onClose}
            aria-label="Kapat"
            className="rounded-lg p-1.5 text-text-muted hover:bg-surface-subtle hover:text-text-primary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Toolbar & Filter */}
        <div className="flex items-center justify-between border-b border-border bg-surface-subtle px-6 py-3">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-text-muted" aria-hidden="true" />
            <input
              type="text"
              aria-label="E-posta ile ara"
              placeholder="E-posta ile ara..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-lg border border-border bg-surface py-1.5 pl-9 pr-3 text-xs text-text-primary focus:border-primary focus:outline-none"
            />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-text-muted">
              Toplam: <strong className="text-text-primary">{users.length}</strong> kullanıcı
            </span>
            <button
              onClick={() => refetch()}
              disabled={loading}
              className="rounded-lg border border-border bg-surface p-1.5 text-text-muted hover:text-text-primary"
              title="Yenile"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            </button>
          </div>
        </div>

        {/* Feedback Message */}
        {(msg || isError) && (
          <div
            className={`mx-6 mt-3 flex items-center gap-2 rounded-xl p-3 text-xs ${
              msg?.type === "success"
                ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                : "bg-rose-500/10 text-rose-600 dark:text-rose-400"
            }`}
          >
            {msg?.type === "success" ? (
              <CheckCircle2 className="h-4 w-4 shrink-0" />
            ) : (
              <AlertCircle className="h-4 w-4 shrink-0" />
            )}
            <span>{msg ? msg.text : "Kullanıcılar yüklenemedi."}</span>
          </div>
        )}

        {/* User Table Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="flex h-40 items-center justify-center text-xs text-text-muted">
              Kullanıcılar yükleniyor...
            </div>
          ) : filteredUsers.length === 0 ? (
            <div className="flex h-40 items-center justify-center text-xs text-text-muted">
              Kullanıcı bulunamadı.
            </div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-border">
              <table className="w-full min-w-[560px] text-left text-xs">
                <thead className="bg-surface-subtle text-text-muted">
                  <tr>
                    <th className="px-4 py-3 font-semibold">Kullanıcı E-posta</th>
                    <th className="px-4 py-3 font-semibold">Mevcut Rol</th>
                    <th className="px-4 py-3 font-semibold">Kayıt Tarihi</th>
                    <th className="px-4 py-3 font-semibold text-right">İşlem</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border bg-surface">
                  {filteredUsers.map((u) => {
                    const isSelf = u.id === currentUser?.userId;
                    return (
                      <tr key={u.id} className="hover:bg-surface-subtle">
                        <td className="px-4 py-3 font-medium text-text-primary">
                          {u.email}
                          {isSelf && (
                            <span className="ml-2 inline-flex items-center rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary">
                              Sen
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[10px] font-bold ${
                              u.role === "ADMIN"
                                ? "bg-amber-500/10 text-amber-600 dark:text-amber-400"
                                : "bg-blue-500/10 text-blue-600 dark:text-blue-400"
                            }`}
                          >
                            {u.role === "ADMIN" ? (
                              <ShieldCheck className="h-3 w-3" />
                            ) : (
                              <UserCheck className="h-3 w-3" />
                            )}
                            {u.role}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-text-muted">
                          {new Date(u.createdAt).toLocaleDateString("tr-TR")}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => handleRoleToggle(u)}
                            disabled={isSelf || (updateRoleMutation.isPending && updateRoleMutation.variables?.userId === u.id)}
                            title={isSelf ? "Kendi rolünüzü değiştiremezsiniz" : undefined}
                            className={`rounded-lg border px-3 py-1 text-[11px] font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 ${
                              u.role === "ADMIN"
                                ? "border-rose-500/30 text-rose-600 hover:bg-rose-500/10 dark:text-rose-400"
                                : "border-amber-500/30 text-amber-600 hover:bg-amber-500/10 dark:text-amber-400"
                            }`}
                          >
                            {updateRoleMutation.isPending && updateRoleMutation.variables?.userId === u.id
                              ? "Güncelleniyor..."
                              : u.role === "ADMIN"
                              ? "ADMIN Yetkisini Kaldır"
                              : "ADMIN Yetkisi Ver"}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="flex justify-end border-t border-border bg-surface-subtle px-6 py-4">
          <button
            onClick={onClose}
            className="rounded-lg border border-border bg-surface px-4 py-2 text-xs font-semibold text-text-primary hover:bg-surface-subtle"
          >
            Kapat
          </button>
        </div>
      </div>
    </Dialog>
  );
}

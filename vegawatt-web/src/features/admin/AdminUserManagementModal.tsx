import React, { useEffect, useState } from "react";
import { X, ShieldCheck, UserCheck, Users, Search, RefreshCw, AlertCircle, CheckCircle2 } from "lucide-react";
import { apiFetch } from "../../shared/api/client";

interface UserItem {
  id: string;
  email: string;
  role: "ADMIN" | "USER";
  createdAt: string;
}

interface AdminUserManagementModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AdminUserManagementModal: React.FC<AdminUserManagementModalProps> = ({ isOpen, onClose }) => {
  const [users, setUsers] = useState<UserItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [updatingId, setUpdatingId] = useState<string | null>(null);
  const [msg, setMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const data = await apiFetch<UserItem[]>("/api/v1/admin/users");
      setUsers(data);
    } catch (err: any) {
      setMsg({ type: "error", text: err.message || "Kullanıcılar yüklenemedi." });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchUsers();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleRoleToggle = async (user: UserItem) => {
    const newRole = user.role === "ADMIN" ? "USER" : "ADMIN";
    try {
      setUpdatingId(user.id);
      setMsg(null);
      await apiFetch<{ success: boolean; message: string }>(`/api/v1/admin/users/${user.id}/role`, {
        method: "PUT",
        body: JSON.stringify({ role: newRole }),
      });
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, role: newRole } : u))
      );
      setMsg({ type: "success", text: `${user.email} kullanıcısının rolü '${newRole}' olarak güncellendi.` });
    } catch (err: any) {
      setMsg({ type: "error", text: err.message || "Rol değiştirme başarısız oldu." });
    } finally {
      setUpdatingId(null);
    }
  };

  const filteredUsers = users.filter((u) =>
    u.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
      <div className="flex h-[85vh] w-full max-w-3xl flex-col rounded-2xl border border-border bg-card shadow-2xl transition-all">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-border p-6">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/10 text-amber-500">
              <Users className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-foreground">Kullanıcı & Yetki Yönetimi</h3>
              <p className="text-xs text-muted-foreground">Sistemdeki tüm kullanıcıları listeleyin ve admin yetkisi tanımlayın</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Toolbar & Filter */}
        <div className="flex items-center justify-between border-b border-border bg-muted/40 px-6 py-3">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <input
              type="text"
              placeholder="E-posta ile ara..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-lg border border-border bg-background py-1.5 pl-9 pr-3 text-xs text-foreground focus:border-primary focus:outline-none"
            />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-muted-foreground">
              Toplam: <strong className="text-foreground">{users.length}</strong> kullanıcı
            </span>
            <button
              onClick={fetchUsers}
              disabled={loading}
              className="rounded-lg border border-border bg-background p-1.5 text-muted-foreground hover:text-foreground"
              title="Yenile"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            </button>
          </div>
        </div>

        {/* Feedback Message */}
        {msg && (
          <div
            className={`mx-6 mt-3 flex items-center gap-2 rounded-xl p-3 text-xs ${
              msg.type === "success"
                ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                : "bg-rose-500/10 text-rose-600 dark:text-rose-400"
            }`}
          >
            {msg.type === "success" ? (
              <CheckCircle2 className="h-4 w-4 shrink-0" />
            ) : (
              <AlertCircle className="h-4 w-4 shrink-0" />
            )}
            <span>{msg.text}</span>
          </div>
        )}

        {/* User Table Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="flex h-40 items-center justify-center text-xs text-muted-foreground">
              Kullanıcılar yükleniyor...
            </div>
          ) : filteredUsers.length === 0 ? (
            <div className="flex h-40 items-center justify-center text-xs text-muted-foreground">
              Kullanıcı bulunamadı.
            </div>
          ) : (
            <div className="overflow-hidden rounded-xl border border-border">
              <table className="w-full text-left text-xs">
                <thead className="bg-muted text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 font-semibold">Kullanıcı E-posta</th>
                    <th className="px-4 py-3 font-semibold">Mevcut Rol</th>
                    <th className="px-4 py-3 font-semibold">Kayıt Tarihi</th>
                    <th className="px-4 py-3 font-semibold text-right">İşlem</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border bg-card">
                  {filteredUsers.map((u) => (
                    <tr key={u.id} className="hover:bg-muted/30">
                      <td className="px-4 py-3 font-medium text-foreground">{u.email}</td>
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
                      <td className="px-4 py-3 text-muted-foreground">
                        {new Date(u.createdAt).toLocaleDateString("tr-TR")}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={() => handleRoleToggle(u)}
                          disabled={updatingId === u.id}
                          className={`rounded-lg border px-3 py-1 text-[11px] font-semibold transition ${
                            u.role === "ADMIN"
                              ? "border-rose-500/30 text-rose-600 hover:bg-rose-500/10 dark:text-rose-400"
                              : "border-amber-500/30 text-amber-600 hover:bg-amber-500/10 dark:text-amber-400"
                          }`}
                        >
                          {updatingId === u.id
                            ? "Güncelleniyor..."
                            : u.role === "ADMIN"
                            ? "ADMIN Yetkisini Kaldır"
                            : "ADMIN Yetkisi Ver"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="flex justify-end border-t border-border bg-muted/20 px-6 py-4">
          <button
            onClick={onClose}
            className="rounded-lg bg-muted px-4 py-2 text-xs font-semibold text-foreground hover:bg-muted/80"
          >
            Kapat
          </button>
        </div>
      </div>
    </div>
  );
};

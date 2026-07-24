import React, { useState } from "react";
import { X, Lock, Mail, KeyRound, CheckCircle2, AlertCircle } from "lucide-react";
import { apiFetch } from "../../shared/api/client";
import { useAuth } from "../auth/AuthContext";

interface ProfileSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ProfileSettingsModal: React.FC<ProfileSettingsModalProps> = ({ isOpen, onClose }) => {
  const { user, updateCurrentUser } = useAuth();
  const [activeTab, setActiveTab] = useState<"password" | "email">("password");

  // Password state
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  // Email state
  const [newEmail, setNewEmail] = useState(user?.email || "");
  const [currentPasswordForEmail, setCurrentPasswordForEmail] = useState("");

  // Feedback state
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  if (!isOpen) return null;

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMsg("");
    setErrorMsg("");

    if (newPassword.length < 8) {
      setErrorMsg("Yeni şifre en az 8 karakter olmalıdır.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setErrorMsg("Yeni şifreler eşleşmiyor.");
      return;
    }

    try {
      setLoading(true);
      const res = await apiFetch<{ success: boolean; message: string }>("/api/v1/users/me/password", {
        method: "POST",
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      setSuccessMsg(res.message || "Şifreniz başarıyla güncellendi.");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err: any) {
      setErrorMsg(err.message || "Şifre değiştirme başarısız oldu.");
    } finally {
      setLoading(false);
    }
  };

  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMsg("");
    setErrorMsg("");

    if (!newEmail || !newEmail.includes("@")) {
      setErrorMsg("Geçerli bir e-posta adresi giriniz.");
      return;
    }
    if (!currentPasswordForEmail) {
      setErrorMsg("Mevcut şifrenizi girmelisiniz.");
      return;
    }

    try {
      setLoading(true);
      const res = await apiFetch<{ success: boolean; message: string }>("/api/v1/users/me/email", {
        method: "POST",
        body: JSON.stringify({ newEmail, currentPassword: currentPasswordForEmail }),
      });
      updateCurrentUser({ email: newEmail });
      setSuccessMsg(res.message || "E-posta adresiniz güncellendi.");
      setCurrentPasswordForEmail("");
    } catch (err: any) {
      setErrorMsg(err.message || "E-posta değiştirme başarısız oldu.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-2xl transition-all">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-border pb-4">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-foreground">Hesap Ayarları</h3>
              <p className="text-xs text-muted-foreground">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="mt-4 flex rounded-lg bg-muted p-1">
          <button
            onClick={() => {
              setActiveTab("password");
              setSuccessMsg("");
              setErrorMsg("");
            }}
            className={`flex-1 rounded-md py-1.5 text-xs font-semibold transition ${
              activeTab === "password"
                ? "bg-card text-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <Lock className="mr-1.5 inline-block h-3.5 w-3.5" />
            Şifre Değiştir
          </button>
          <button
            onClick={() => {
              setActiveTab("email");
              setSuccessMsg("");
              setErrorMsg("");
            }}
            className={`flex-1 rounded-md py-1.5 text-xs font-semibold transition ${
              activeTab === "email"
                ? "bg-card text-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <Mail className="mr-1.5 inline-block h-3.5 w-3.5" />
            E-posta Değiştir
          </button>
        </div>

        {/* Alert Messages */}
        {successMsg && (
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-emerald-500/10 p-3 text-xs text-emerald-600 dark:text-emerald-400">
            <CheckCircle2 className="h-4 w-4 shrink-0" />
            <span>{successMsg}</span>
          </div>
        )}

        {errorMsg && (
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-rose-500/10 p-3 text-xs text-rose-600 dark:text-rose-400">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Password Tab Form */}
        {activeTab === "password" && (
          <form onSubmit={handlePasswordSubmit} className="mt-4 space-y-3">
            <div>
              <label className="text-xs font-medium text-muted-foreground">Mevcut Şifre</label>
              <input
                type="password"
                required
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                placeholder="••••••••"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Yeni Şifre</label>
              <input
                type="password"
                required
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                placeholder="En az 8 karakter"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Yeni Şifre (Tekrar)</label>
              <input
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                placeholder="En az 8 karakter"
              />
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg border border-border px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-muted"
              >
                İptal
              </button>
              <button
                type="submit"
                disabled={loading}
                className="rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-white transition hover:bg-primary-hover disabled:opacity-50"
              >
                {loading ? "Güncelleniyor..." : "Şifreyi Güncelle"}
              </button>
            </div>
          </form>
        )}

        {/* Email Tab Form */}
        {activeTab === "email" && (
          <form onSubmit={handleEmailSubmit} className="mt-4 space-y-3">
            <div>
              <label className="text-xs font-medium text-muted-foreground">Yeni E-posta Adresi</label>
              <input
                type="email"
                required
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                placeholder="ornek@domain.com"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Mevcut Şifre</label>
              <input
                type="password"
                required
                value={currentPasswordForEmail}
                onChange={(e) => setCurrentPasswordForEmail(e.target.value)}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                placeholder="••••••••"
              />
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg border border-border px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-muted"
              >
                İptal
              </button>
              <button
                type="submit"
                disabled={loading}
                className="rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-white transition hover:bg-primary-hover disabled:opacity-50"
              >
                {loading ? "Güncelleniyor..." : "E-postayı Güncelle"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

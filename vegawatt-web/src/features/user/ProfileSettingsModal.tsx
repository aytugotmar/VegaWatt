import React, { useState } from "react";
import { X, Lock, Mail, KeyRound, CheckCircle2, AlertCircle } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../shared/api/client";
import { useAuth } from "../auth/AuthContext";

interface ProfileSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const SESSION_REVOKED_REDIRECT_DELAY_MS = 1500;

export const ProfileSettingsModal: React.FC<ProfileSettingsModalProps> = ({ isOpen, onClose }) => {
  const { user, updateCurrentUser, logout } = useAuth();
  const navigate = useNavigate();
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

  // Password/email changes revoke every session server-side, including the current one — the
  // access token stays technically valid until its own TTL expires, but the refresh token behind
  // it is already dead, so we force a clean logout instead of leaving the user in a half-signed-in
  // state until they hit a 401 on some unrelated action.
  const forceReLoginAfterSessionRevoke = () => {
    window.setTimeout(() => {
      onClose();
      void logout().then(() => navigate("/login"));
    }, SESSION_REVOKED_REDIRECT_DELAY_MS);
  };

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
      setSuccessMsg(res.message || "Şifreniz başarıyla güncellendi. Güvenliğiniz için çıkış yapılıyor...");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      forceReLoginAfterSessionRevoke();
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
      setSuccessMsg(res.message || "E-posta adresiniz güncellendi. Güvenliğiniz için çıkış yapılıyor...");
      setCurrentPasswordForEmail("");
      forceReLoginAfterSessionRevoke();
    } catch (err: any) {
      setErrorMsg(err.message || "E-posta değiştirme başarısız oldu.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm">
      <div className="dialog-glass w-full max-w-md rounded-2xl border border-border p-6 shadow-2xl transition-all">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-border pb-4">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-text-primary">Hesap Ayarları</h3>
              <p className="text-xs text-text-muted">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-text-muted hover:bg-surface-subtle hover:text-text-primary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="mt-4 flex rounded-lg bg-surface-subtle p-1">
          <button
            onClick={() => {
              setActiveTab("password");
              setSuccessMsg("");
              setErrorMsg("");
            }}
            className={`flex-1 rounded-md py-1.5 text-xs font-semibold transition ${
              activeTab === "password"
                ? "bg-surface-raised text-text-primary shadow-sm"
                : "text-text-muted hover:text-text-primary"
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
                ? "bg-surface-raised text-text-primary shadow-sm"
                : "text-text-muted hover:text-text-primary"
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
              <label className="text-xs font-medium text-text-muted">Mevcut Şifre</label>
              <input
                type="password"
                required
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-text-primary focus:border-primary focus:outline-none"
                placeholder="••••••••"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-text-muted">Yeni Şifre</label>
              <input
                type="password"
                required
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-text-primary focus:border-primary focus:outline-none"
                placeholder="En az 8 karakter"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-text-muted">Yeni Şifre (Tekrar)</label>
              <input
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-text-primary focus:border-primary focus:outline-none"
                placeholder="En az 8 karakter"
              />
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg border border-border px-4 py-2 text-xs font-semibold text-text-muted hover:bg-surface-subtle"
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
              <label className="text-xs font-medium text-text-muted">Yeni E-posta Adresi</label>
              <input
                type="email"
                required
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
                className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-text-primary focus:border-primary focus:outline-none"
                placeholder="ornek@domain.com"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-text-muted">Mevcut Şifre</label>
              <input
                type="password"
                required
                value={currentPasswordForEmail}
                onChange={(e) => setCurrentPasswordForEmail(e.target.value)}
                className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-text-primary focus:border-primary focus:outline-none"
                placeholder="••••••••"
              />
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg border border-border px-4 py-2 text-xs font-semibold text-text-muted hover:bg-surface-subtle"
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

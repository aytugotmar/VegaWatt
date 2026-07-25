import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { X, Lock, Mail, KeyRound, CheckCircle2, AlertCircle } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Dialog } from "../../shared/components/Dialog";
import { Input } from "../../shared/components/Input";
import { PasswordInput } from "../../shared/components/PasswordInput";
import { changeEmail, changePassword } from "../../shared/api/userApi";
import { useAuth } from "../auth/AuthContext";

interface ProfileSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const SESSION_REVOKED_REDIRECT_DELAY_MS = 1500;

export function ProfileSettingsModal({ isOpen, onClose }: ProfileSettingsModalProps) {
  const { user, updateCurrentUser, logout } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<"password" | "email">("password");

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordFormError, setPasswordFormError] = useState("");

  const [newEmail, setNewEmail] = useState(user?.email || "");
  const [currentPasswordForEmail, setCurrentPasswordForEmail] = useState("");
  const [emailFormError, setEmailFormError] = useState("");

  const [successMsg, setSuccessMsg] = useState("");

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

  const passwordMutation = useMutation({
    mutationFn: () => changePassword(currentPassword, newPassword),
    onSuccess: (res) => {
      setSuccessMsg(res.message || "Şifreniz başarıyla güncellendi. Güvenliğiniz için çıkış yapılıyor...");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      forceReLoginAfterSessionRevoke();
    },
  });

  const emailMutation = useMutation({
    mutationFn: () => changeEmail(newEmail, currentPasswordForEmail),
    onSuccess: (res) => {
      updateCurrentUser({ email: newEmail });
      setSuccessMsg(res.message || "E-posta adresiniz güncellendi. Güvenliğiniz için çıkış yapılıyor...");
      setCurrentPasswordForEmail("");
      forceReLoginAfterSessionRevoke();
    },
  });

  if (!isOpen) return null;

  const handlePasswordSubmit = (e: FormEvent) => {
    e.preventDefault();
    setSuccessMsg("");
    setPasswordFormError("");

    if (newPassword.length < 8) {
      setPasswordFormError("Yeni şifre en az 8 karakter olmalıdır.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordFormError("Yeni şifreler eşleşmiyor.");
      return;
    }
    passwordMutation.mutate();
  };

  const handleEmailSubmit = (e: FormEvent) => {
    e.preventDefault();
    setSuccessMsg("");
    setEmailFormError("");

    if (!newEmail || !newEmail.includes("@")) {
      setEmailFormError("Geçerli bir e-posta adresi giriniz.");
      return;
    }
    if (!currentPasswordForEmail) {
      setEmailFormError("Mevcut şifrenizi girmelisiniz.");
      return;
    }
    emailMutation.mutate();
  };

  const passwordErrorMsg =
    passwordFormError || (passwordMutation.isError ? passwordMutation.error.message : "");
  const emailErrorMsg = emailFormError || (emailMutation.isError ? emailMutation.error.message : "");

  return (
    <Dialog open={isOpen} onClose={onClose} title="Hesap Ayarları" maxWidthClassName="max-w-md">
      <div className="p-6">
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
            aria-label="Kapat"
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

        {activeTab === "password" && passwordErrorMsg && (
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-rose-500/10 p-3 text-xs text-rose-600 dark:text-rose-400">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{passwordErrorMsg}</span>
          </div>
        )}
        {activeTab === "email" && emailErrorMsg && (
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-rose-500/10 p-3 text-xs text-rose-600 dark:text-rose-400">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{emailErrorMsg}</span>
          </div>
        )}

        {/* Password Tab Form */}
        {activeTab === "password" && (
          <form onSubmit={handlePasswordSubmit} className="mt-4 space-y-3">
            <PasswordInput
              label="Mevcut Şifre"
              required
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              placeholder="••••••••"
            />
            <PasswordInput
              label="Yeni Şifre"
              required
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="En az 8 karakter"
            />
            <PasswordInput
              label="Yeni Şifre (Tekrar)"
              required
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="En az 8 karakter"
            />

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
                disabled={passwordMutation.isPending}
                className="rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-white transition hover:bg-primary-hover disabled:opacity-50"
              >
                {passwordMutation.isPending ? "Güncelleniyor..." : "Şifreyi Güncelle"}
              </button>
            </div>
          </form>
        )}

        {/* Email Tab Form */}
        {activeTab === "email" && (
          <form onSubmit={handleEmailSubmit} className="mt-4 space-y-3">
            <Input
              label="Yeni E-posta Adresi"
              type="email"
              required
              value={newEmail}
              onChange={(e) => setNewEmail(e.target.value)}
              placeholder="ornek@domain.com"
            />
            <PasswordInput
              label="Mevcut Şifre"
              required
              value={currentPasswordForEmail}
              onChange={(e) => setCurrentPasswordForEmail(e.target.value)}
              placeholder="••••••••"
            />

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
                disabled={emailMutation.isPending}
                className="rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-white transition hover:bg-primary-hover disabled:opacity-50"
              >
                {emailMutation.isPending ? "Güncelleniyor..." : "E-postayı Güncelle"}
              </button>
            </div>
          </form>
        )}
      </div>
    </Dialog>
  );
}

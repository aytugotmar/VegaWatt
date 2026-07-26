import { Check } from "lucide-react";
import { useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import { Button } from "../../shared/components/Button";
import { Input } from "../../shared/components/Input";
import { PasswordInput } from "../../shared/components/PasswordInput";
import { useLanguage } from "../../shared/i18n/LanguageContext";
import { useAuth } from "./AuthContext";

const MIN_PASSWORD_LENGTH = 8;

interface RegisterFormProps {
  onSuccess: () => void;
}

export function RegisterForm({ onSuccess }: RegisterFormProps) {
  const { t } = useLanguage();
  const { register } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const passwordsMatch = confirmPassword.length === 0 || confirmPassword === password;
  const hasMinLength = password.length >= MIN_PASSWORD_LENGTH;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setErrorMessage(null);
    if (password !== confirmPassword) {
      setErrorMessage(t("auth.passwordConfirm"));
      return;
    }
    setIsSubmitting(true);
    try {
      await register(email, password);
      onSuccess();
    } catch (error) {
      setErrorMessage(error instanceof ApiError ? error.message : t("auth.loginFailed"));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <Input
        label={t("auth.email")}
        type="email"
        autoComplete="email"
        required
        className="h-12"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
      />
      <PasswordInput
        label={t("auth.password")}
        autoComplete="new-password"
        minLength={MIN_PASSWORD_LENGTH}
        required
        value={password}
        onChange={(event) => setPassword(event.target.value)}
      />
      <PasswordInput
        label={t("auth.passwordConfirm")}
        autoComplete="new-password"
        required
        value={confirmPassword}
        error={!passwordsMatch ? t("auth.passwordConfirm") : undefined}
        onChange={(event) => setConfirmPassword(event.target.value)}
      />

      <ul className="flex flex-col gap-1 text-xs text-text-muted">
        <li className={`flex items-center gap-1.5 ${hasMinLength ? "text-success" : ""}`}>
          <Check className="h-3.5 w-3.5" aria-hidden="true" />
          At least {MIN_PASSWORD_LENGTH} characters
        </li>
        <li className="flex items-center gap-1.5">
          <Check className="h-3.5 w-3.5" aria-hidden="true" />
          Secure session (httpOnly refresh token)
        </li>
        <li className="flex items-center gap-1.5">
          <Check className="h-3.5 w-3.5" aria-hidden="true" />
          Isolated user-scoped home access
        </li>
      </ul>

      {errorMessage && <p className="text-sm font-medium text-danger">{errorMessage}</p>}
      <Button type="submit" loading={isSubmitting} className="h-12 justify-center">
        {t("auth.signUp")}
      </Button>
    </form>
  );
}

import { useState, type FormEvent } from "react";
import { ApiError } from "../../shared/api/client";
import { Button } from "../../shared/components/Button";
import { Input } from "../../shared/components/Input";
import { PasswordInput } from "../../shared/components/PasswordInput";
import { useLanguage } from "../../shared/i18n/LanguageContext";
import { useAuth } from "./AuthContext";

interface LoginFormProps {
  onSuccess: () => void;
}

export function LoginForm({ onSuccess }: LoginFormProps) {
  const { t } = useLanguage();
  const { login } = useAuth();
  const [email, setEmail] = useState("admin@vegawatt.com");
  const [password, setPassword] = useState("VegaWatt111!");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setErrorMessage(null);
    setIsSubmitting(true);
    try {
      await login(email, password);
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
        autoComplete="current-password"
        required
        value={password}
        onChange={(event) => setPassword(event.target.value)}
      />
      {errorMessage && <p className="text-sm font-medium text-danger">{errorMessage}</p>}
      <Button type="submit" loading={isSubmitting} className="h-12 justify-center">
        {t("auth.signIn")}
      </Button>
    </form>
  );
}

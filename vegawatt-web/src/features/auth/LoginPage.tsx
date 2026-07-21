import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { Button } from "../../shared/components/Button";
import { Input } from "../../shared/components/Input";
import { useAuth } from "./AuthContext";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setErrorMessage(null);
    setIsSubmitting(true);
    try {
      await login(email, password);
      navigate("/app/homes");
    } catch (error) {
      setErrorMessage(error instanceof ApiError ? error.message : "Giriş yapılamadı.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-sm flex-col justify-center px-6 py-12">
      <h1 className="mb-6 text-2xl font-semibold text-text-primary">Giriş yap</h1>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Input
          label="E-posta"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <Input
          label="Şifre"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        {errorMessage && <p className="text-sm font-medium text-danger">{errorMessage}</p>}
        <Button type="submit" loading={isSubmitting}>
          Giriş yap
        </Button>
      </form>
      <p className="mt-6 text-sm text-text-secondary">
        Hesabınız yok mu?{" "}
        <Link to="/register" className="font-medium text-primary hover:underline">
          Kayıt olun
        </Link>
      </p>
    </div>
  );
}

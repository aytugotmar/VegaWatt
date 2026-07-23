import { Link, useNavigate } from "react-router-dom";
import { AuthShell } from "./AuthShell";
import { LoginForm } from "./LoginForm";

export function LoginPage() {
  const navigate = useNavigate();

  return (
    <AuthShell
      title="Tekrar hoş geldiniz"
      subtitle="Enerji panelinize devam etmek için giriş yapın."
      footer={
        <>
          Hesabınız yok mu?{" "}
          <Link to="/register" className="font-medium text-primary hover:underline">
            Ücretsiz hesap oluşturun
          </Link>
        </>
      }
    >
      <LoginForm onSuccess={() => navigate("/app/overview")} />
    </AuthShell>
  );
}

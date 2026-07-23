import { Link, useNavigate } from "react-router-dom";
import { AuthShell } from "./AuthShell";
import { RegisterForm } from "./RegisterForm";

export function RegisterPage() {
  const navigate = useNavigate();

  return (
    <AuthShell
      title="VegaWatt hesabınızı oluşturun"
      subtitle="Evinizin enerji tüketimini izlemeye başlayın."
      footer={
        <>
          Zaten hesabınız var mı?{" "}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Giriş yapın
          </Link>
        </>
      }
    >
      <RegisterForm onSuccess={() => navigate("/app/overview")} />
    </AuthShell>
  );
}

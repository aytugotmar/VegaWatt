import { Link, useNavigate } from "react-router-dom";
import { useLanguage } from "../../shared/i18n/LanguageContext";
import { AuthShell } from "./AuthShell";
import { RegisterForm } from "./RegisterForm";

export function RegisterPage() {
  const { t } = useLanguage();
  const navigate = useNavigate();

  return (
    <AuthShell
      title={t("auth.signUp")}
      subtitle={t("auth.heroSubtitle")}
      footer={
        <>
          {t("auth.hasAccount")}{" "}
          <Link to="/login" className="font-medium text-primary hover:underline">
            {t("auth.signIn")}
          </Link>
        </>
      }
    >
      <RegisterForm onSuccess={() => navigate("/app/overview")} />
    </AuthShell>
  );
}

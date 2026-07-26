import { Link, useNavigate } from "react-router-dom";
import { useLanguage } from "../../shared/i18n/LanguageContext";
import { AuthShell } from "./AuthShell";
import { LoginForm } from "./LoginForm";

export function LoginPage() {
  const { t } = useLanguage();
  const navigate = useNavigate();

  return (
    <AuthShell
      title={t("auth.welcomeBack")}
      subtitle={t("auth.continueToPanel")}
      footer={
        <>
          {t("auth.noAccount")}{" "}
          <Link to="/register" className="font-medium text-primary hover:underline">
            {t("auth.createFreeAccount")}
          </Link>
        </>
      }
    >
      <LoginForm onSuccess={() => navigate("/app/overview")} />
    </AuthShell>
  );
}

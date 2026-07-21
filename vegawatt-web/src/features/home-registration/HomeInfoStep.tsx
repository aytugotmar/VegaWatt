import { Input } from "../../shared/components/Input";
import type { FieldErrors, HomeInfoValues } from "./registrationSchema";

interface HomeInfoStepProps {
  values: HomeInfoValues;
  errors: FieldErrors;
  onChange: (field: keyof HomeInfoValues, value: string) => void;
}

export function HomeInfoStep({ values, errors, onChange }: HomeInfoStepProps) {
  return (
    <div className="flex flex-col gap-4">
      <Input
        label="Ev adı"
        placeholder="Örn. Bahçelievler Dairesi"
        value={values.name}
        onChange={(event) => onChange("name", event.target.value)}
        error={errors.name}
      />
      <Input
        label="Bildirim e-postası"
        type="email"
        placeholder="ornek@eposta.com"
        hint="Kota ve anomali bildirimleri bu adrese gönderilir."
        value={values.contactEmail}
        onChange={(event) => onChange("contactEmail", event.target.value)}
        error={errors.contactEmail}
      />
    </div>
  );
}

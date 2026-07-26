const TEAM_MEMBERS = [
  { name: "Aytuğ Otmar", github: "https://github.com/aytugotmar" },
  { name: "Bekircan Küçükakın", github: "https://github.com/bekucukakin" },
  { name: "Kenan Özçakır", github: "https://github.com/KenanOzcakir" },
];

export function Footer() {
  return (
    <footer className="w-full border-t border-border bg-surface-subtle/50 py-6 px-4 sm:px-6">
      <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 text-center sm:flex-row sm:text-left">
        <div className="flex items-center gap-3">
          <div className="overflow-hidden rounded-md border border-border/80 bg-surface p-0.5 shadow-md">
            <img src="/assets/ui.gif" alt="VegaWatt UI Preview" className="h-14 sm:h-16 w-auto object-cover rounded" />
          </div>
          <div className="flex flex-col text-xs text-text-muted">
            <span className="font-semibold text-text-primary">© 2026 VegaWatt</span>
            <span>Akıllı Ev Enerji Yönetimi & Telemetri Platformu</span>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-center gap-2.5 text-xs text-text-secondary">
          <span className="font-medium text-text-primary">Geliştirici Ekip:</span>
          {TEAM_MEMBERS.map((member) => (
            <a
              key={member.name}
              href={member.github}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 rounded-full bg-surface px-2.5 py-1 font-medium text-text-secondary transition hover:bg-primary-soft hover:text-primary border border-border"
            >
              <svg className="h-3.5 w-3.5 fill-current" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
              </svg>
              {member.name}
            </a>
          ))}
        </div>
      </div>
    </footer>
  );
}

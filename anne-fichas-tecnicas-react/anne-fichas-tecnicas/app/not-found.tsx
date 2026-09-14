import Link from "next/link";

export default function NotFound() {
  return (
    <div className="app" style={{ textAlign: "center", paddingTop: 100 }}>
      <div
        style={{
          fontFamily: "'Cormorant Garamond', serif",
          fontSize: 30,
          color: "var(--green)",
          marginBottom: 12,
        }}
      >
        Página não encontrada
      </div>
      <p style={{ color: "var(--muted)", marginBottom: 24 }}>
        O que você procurava não existe ou foi removido.
      </p>
      <Link href="/" className="btn">
        Voltar ao catálogo
      </Link>
    </div>
  );
}

"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";

export function LoginView() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      // Passa por /api/auth/login (mesma origem do frontend) em vez de chamar
      // o backend Java direto — é essa rota que grava o cookie httpOnly
      // "accessToken" na origem do próprio Next.js. Ver lib/api/auth.ts.
      await authApi.login({ username, password });

      // Redireciona para o catálogo após o login bem-sucedido
      router.push("/");
      router.refresh();
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "Usuário ou senha incorretos.";
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="app">
      <header className="top no-print">
        <div className="brand">
          <div className="mark">ANNE</div>
          <div className="tagline">Fichas técnicas &amp; produção</div>
        </div>
      </header>

      <div className="login-wrap">
        <div className="login-card">
          <h2>Entrar</h2>
          <form onSubmit={handleSubmit}>
            <div className="field-block">
              <label htmlFor="l_username">Usuário</label>
              <input
                id="l_username"
                type="text"
                autoComplete="username"
                autoFocus
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
            </div>
            <div className="field-block">
              <label htmlFor="l_password">Senha</label>
              <input
                id="l_password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            {error && <div className="error-text">{error}</div>}

            <button type="submit" className="btn login-submit" disabled={loading}>
              {loading ? "Entrando..." : "Entrar"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
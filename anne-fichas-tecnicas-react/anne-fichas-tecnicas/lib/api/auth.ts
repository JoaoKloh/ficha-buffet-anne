import { apiClient, encerrarSessao } from "./client";

export interface LoginCredentials {
  username: string;
  password: string;
}

export const authApi = {
  // Nomes dos campos idênticos aos de LoginRequestDTO (username, password) no
  // backend. Vai para /api/auth/login (mesma origem do frontend) — não para o
  // backend Java diretamente — para que os cookies de sessão sejam gravados pela
  // origem do próprio Next.js. Ver app/api/auth/login/route.ts.
  login: (credentials: LoginCredentials) =>
    apiClient.post<void>("/api/auth/login", credentials),

  // Revoga a sessão deste navegador, apaga os cookies e vai para /login
  // (os outros dispositivos continuam logados). Ver app/api/auth/logout/route.ts.
  logout: () => encerrarSessao(),
};

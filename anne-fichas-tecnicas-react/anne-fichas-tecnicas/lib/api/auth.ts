import { apiClient } from "./client";

export interface LoginCredentials {
  username: string;
  password: string;
}

export const authApi = {
  // Nomes dos campos idênticos aos de LoginRequestDTO (username, password) no
  // backend. Vai para /api/auth/login (mesma origem do frontend) — não para o
  // backend Java diretamente — para que o cookie de sessão seja gravado pela
  // origem do próprio Next.js. Ver app/api/auth/login/route.ts.
  login: (credentials: LoginCredentials) =>
    apiClient.post<void>("/api/auth/login", credentials),
};

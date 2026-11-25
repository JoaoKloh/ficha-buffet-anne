import { NextResponse } from "next/server";
import { ZodError, type ZodTypeAny, type z } from "zod";
import { BackendError } from "./java";

/**
 * Tratamento comum das rotas do Anne Gestão, no padrão de app/api/lancamentos:
 * corpo no envelope { data } (desembrulhado pelo apiClient), erro em { message }
 * com o status certo. Validação Zod vira 400 com a primeira mensagem e `issues`.
 */
export async function responder<T>(fn: () => Promise<T>, status = 200, padrao = "Erro interno do servidor."): Promise<NextResponse> {
  try {
    const data = await fn();
    if (data === undefined) return new NextResponse(null, { status: 204 });
    return NextResponse.json({ data }, { status });
  } catch (error) {
    if (error instanceof ZodError) {
      return NextResponse.json({ message: error.issues[0]?.message ?? "Dados inválidos.", issues: error.issues }, { status: 400 });
    }
    if (error instanceof BackendError) {
      return NextResponse.json(
        error.code ? { message: error.message, code: error.code } : { message: error.message },
        { status: error.status }
      );
    }
    console.error("[gestao]", error);
    return NextResponse.json({ message: padrao }, { status: 500 });
  }
}

/** Lê e valida o corpo JSON. */
export async function corpo<S extends ZodTypeAny>(req: Request, schema: S): Promise<z.infer<S>> {
  const raw = await req.json().catch(() => {
    throw new BackendError("Corpo da requisição inválido.", 400);
  });
  return schema.parse(raw);
}

/** Id numérico de query string (?id=) ou de parâmetro de rota. */
export function idDe(valor: string | null | undefined): number {
  const id = Number(valor);
  if (!Number.isInteger(id) || id <= 0) throw new BackendError("ID obrigatório.", 400);
  return id;
}
export const idQuery = (req: Request, nome = "id"): number => idDe(new URL(req.url).searchParams.get(nome));

// app/api/prato/upload/route.ts
import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { handleUpload, type HandleUploadBody } from "@vercel/blob/client";
import { FOTO_MAX_BYTES, FOTO_TIPOS_ACEITOS } from "@/lib/models/prato";

/**
 * Gera o token temporário para o browser enviar a foto do prato direto ao
 * Vercel Blob (client upload). O arquivo não passa por aqui — só o pedido do
 * token — e o BLOB_READ_WRITE_TOKEN do .env nunca sai do servidor.
 *
 * Diferente das outras rotas de /api/prato, esta não chama o backend Java,
 * então não há 401 dele para repassar: a sessão é checada aqui mesmo, do
 * mesmo jeito que o middleware (presença do cookie accessToken).
 */
export async function POST(req: Request) {
  const cookieStore = await cookies();
  if (!cookieStore.has("accessToken")) {
    return NextResponse.json({ message: "Sessão expirada. Entre novamente." }, { status: 401 });
  }

  try {
    const body = (await req.json()) as HandleUploadBody;
    const result = await handleUpload({
      body,
      request: req,
      onBeforeGenerateToken: async () => ({
        allowedContentTypes: FOTO_TIPOS_ACEITOS,
        maximumSizeInBytes: FOTO_MAX_BYTES,
        addRandomSuffix: true,
      }),
    });
    return NextResponse.json(result);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Não foi possível enviar a imagem.";
    return NextResponse.json({ message }, { status: 400 });
  }
}

import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

/**
 * Leitura de nota fiscal por foto. Por decisão do projeto, o processamento
 * ainda não existe: a interface está pronta e esta rota só responde que o
 * recurso não está disponível. Quando houver o serviço, ele recebe as fotos
 * (multipart) e devolve um NotaFiscalImportadaDTO.
 */
export async function POST() {
  return NextResponse.json({ message: "A leitura de nota por foto ainda não está disponível. Importe o XML ou lance a nota à mão." }, { status: 501 });
}

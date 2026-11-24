import { NextResponse } from "next/server";
import { backupBackend } from "@/lib/server/gestao/backupBackend";
import { hoje } from "@/lib/utils/datas";

export const dynamic = "force-dynamic";

/** Planilha para consultar (.xlsx). Download direto, por isso sem o envelope { data }. */
export async function GET() {
  try {
    const xlsx = await backupBackend.planilha();
    return new NextResponse(new Uint8Array(xlsx), {
      headers: {
        "Content-Type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "Content-Disposition": `attachment; filename="anne-gestao-backup-${hoje()}.xlsx"`,
        "Cache-Control": "no-store",
      },
    });
  } catch (error) {
    console.error("[gestao]", error);
    return NextResponse.json({ message: "Não consegui gerar a planilha agora. Salve o backup .json." }, { status: 500 });
  }
}

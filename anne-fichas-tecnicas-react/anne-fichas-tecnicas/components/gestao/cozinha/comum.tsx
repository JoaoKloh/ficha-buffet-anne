"use client";

import { createContext, useContext } from "react";
import { msgErro } from "@/components/ui/Gestao";
import { useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import type { CozinhaResponseDTO, ExecucaoResponseDTO, SalvarExecucaoRequestDTO } from "@/lib/models/cozinha";
import { salvarExecucao } from "@/lib/api/gestao/cozinha";
import { CATEGORIAS_PREPARACAO, CONSERVACOES, PRACAS, rotulo } from "@/lib/models/dominio";
import { fmtQ } from "@/lib/utils/numero";

/** Dados e modo da tela da Cozinha, compartilhados pelas abas. */
export interface CozinhaCtx {
  dados: CozinhaResponseDTO;
  admin: boolean;
  praca: string;
  setPraca: (p: string) => void;
  abrirEvento: (id: number, aba?: string) => void;
}
export const Ctx = createContext<CozinhaCtx | null>(null);
export const useCozinha = (): CozinhaCtx => useContext(Ctx)!;

export const exDe = (dados: CozinhaResponseDTO, chave: string): Partial<ExecucaoResponseDTO> => dados.execucoes.find((x) => x.chave === chave) ?? {};
export const qU = (n: number, u: string) => `${fmtQ(n, u)} ${u || ""}`.trim();
export const catPrepLabel = (k: string) => rotulo(CATEGORIAS_PREPARACAO, k, "Outros");
export const consLabel = (k: string) => rotulo(CONSERVACOES, k);
export const ordemCat = (k: string) => {
  const i = CATEGORIAS_PREPARACAO.findIndex((c) => c[0] === k);
  return i < 0 ? 99 : i;
};
export const ordemPraca = (p: string) => PRACAS.indexOf(p as (typeof PRACAS)[number]);
export const tomFicha = (f: string) => (f === "Completa" ? "good" : f === "Incompleta" ? "bad" : "warn");

/** Grava a execução e recarrega; avisa quando o envio foi apagado ou a baixa mudou. */
export function useSalvarExec() {
  const { recarregar } = useSheet();
  const { showToast } = useToast();
  const { admin } = useCozinha();
  return async (dto: SalvarExecucaoRequestDTO) => {
    try {
      const r = await salvarExecucao(dto);
      if (r.envioApagado) showToast("Qtd enviada apagada: faltam assinatura ou temperatura para sair.", "error");
      else if (r.baixaAlterada != null && admin) showToast(r.baixaAlterada ? "Produção registrada · baixa no estoque" : "Produção removida · baixa desfeita");
      recarregar();
    } catch (e) {
      showToast(msgErro(e), "error");
    }
  };
}

/** Número em pt-BR digitado no checklist; vazio = null; inválido = undefined. */
export function parseNumBR(raw: string): number | null | undefined {
  const s = raw.trim();
  if (!s) return null;
  const n = s.includes(",") ? Number(s.replace(/\./g, "").replace(",", ".")) : Number(s);
  return isFinite(n) ? n : undefined;
}

export function PracaChips({ pracas }: { pracas: string[] }) {
  const { praca, setPraca } = useCozinha();
  const us = PRACAS.filter((p) => pracas.includes(p));
  return (
    <div className="toolbar" role="group" aria-label="Praça">
      <button type="button" className="chip" aria-pressed={!praca} onClick={() => setPraca("")}>
        Todas as praças
      </button>
      {us.map((p) => (
        <button key={p} type="button" className="chip" aria-pressed={praca === p} onClick={() => setPraca(p)}>
          {p}
        </button>
      ))}
    </div>
  );
}

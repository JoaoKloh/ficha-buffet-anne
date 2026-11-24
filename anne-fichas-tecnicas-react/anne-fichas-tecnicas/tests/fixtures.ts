import type { CozinhaProjecaoDTO, ExecucaoResponseDTO, PratoProjecaoDTO } from "@/lib/models/cozinha";

/** Fábricas de registros para os testes das funções puras das telas (o que o backend devolve). */

export const exec = (p: Partial<ExecucaoResponseDTO> & { chave: string }): ExecucaoResponseDTO => ({
  id: 1, kind: "item", eventoId: 1, cardapioId: null, insumoId: null, key: null, produzido: null, produzidoEm: null, executadoPor: "", enviadoQ: null, enviado: false, enviadoEm: null,
  separadoPor: "", retorno: null, conferidoPor: "", obs: "", tempSaida: null, tempChegada: null, saiu: null, voltou: null, avaria: null, feito: false, resp: "", dnSet: false, dn: null,
  itens: {}, chefPor: "", chefHora: "", cargaPor: "", cargaHora: "", recebPor: "", recebHora: "", retornoPor: "", retornoHora: "", veiculo: "", atualizadoEm: "", ...p,
});

export const pratoProjecao = (p: Partial<PratoProjecaoDTO> & { cid: number; nome: string }): PratoProjecaoDTO => ({
  cat: "volante", praca: "Salgados", resp: "", st: "sim", porcao: 1, fator: "1 un por convidado", total: 100, uu: "un", ulabel: "un", rend: 10, rendLabel: "10 un", lotes: 10,
  conserv: "refrigerado", ficha: "Completa", modo: "", obsEnvio: "", etapas: [], ins: [], loucas: [], comp: [], ...p,
});

export const projecao = (p: Partial<CozinhaProjecaoDTO> = {}): CozinhaProjecaoDTO => ({
  eventoId: 1, codigo: "26-001", nome: "Evento 1", data: "2026-12-12", hora: "", local: "", cidade: "", status: "contratado", N: 100, base: 100, margem: 0, fechado: false, obs: "", equipe: "",
  pratos: [], compras: [], loucas: [], tarefas: [], extras: [], ...p,
});

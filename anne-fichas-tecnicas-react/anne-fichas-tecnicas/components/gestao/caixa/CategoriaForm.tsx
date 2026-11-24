"use client";

import { useState } from "react";
import { FormSheet, Fld, Sel } from "@/components/ui/Gestao";
import { CUSTO_TIPOS, GRUPOS_DRE, type CustoTipo, type GrupoDre } from "@/lib/models/dominio";
import type { CategoriaResponseDTO } from "@/lib/models/caixa";
import { atualizarCategoria, criarCategoria, excluirCategoria } from "@/lib/api/gestao/caixa";

const CUSTO_EV_OPCOES = [["", "Automático pelo nome"], ...CUSTO_TIPOS] as const;

/** Linha da DRE. Categoria com lançamentos não pode ser excluída. */
export function CategoriaForm({ categoria, usada, proximaOrdem }: { categoria?: CategoriaResponseDTO; usada?: boolean; proximaOrdem: number }) {
  const [v, setV] = useState({
    nome: categoria?.nome ?? "",
    grupo: (categoria?.grupo ?? "custo_direto") as GrupoDre,
    ordem: String(categoria?.ordem ?? proximaOrdem),
    custoEvento: (categoria?.custoEvento ?? "") as CustoTipo | "",
  });
  const dto = () => ({ nome: v.nome.trim(), grupo: v.grupo, ordem: Math.max(0, Math.round(Number(v.ordem) || 0)), custoEvento: v.custoEvento || null });
  return (
    <FormSheet
      intro={usada ? "Esta categoria tem lançamentos, por isso não pode ser excluída." : undefined}
      validar={() => (v.nome.trim() ? null : "Informe o nome.")}
      onSalvar={() => (categoria ? atualizarCategoria({ ...dto(), id: categoria.id }) : criarCategoria(dto()))}
      onExcluir={categoria && !usada ? () => excluirCategoria(categoria.id) : null}
    >
      <Fld label="Nome" obrig full>
        <input value={v.nome} onChange={(e) => setV({ ...v, nome: e.target.value })} autoFocus />
      </Fld>
      <Fld label="Linha da DRE" full>
        <Sel value={v.grupo} onChange={(x) => setV({ ...v, grupo: x as GrupoDre })} opcoes={GRUPOS_DRE} />
      </Fld>
      <Fld label="Nos eventos, conta como" hint="Usado no previsto × realizado do evento. Automático: o sistema decide pelo nome da categoria.">
        <Sel value={v.custoEvento} onChange={(x) => setV({ ...v, custoEvento: x as CustoTipo | "" })} opcoes={CUSTO_EV_OPCOES} />
      </Fld>
      <Fld label="Ordem na lista">
        <input type="number" min={0} step={1} value={v.ordem} onChange={(e) => setV({ ...v, ordem: e.target.value })} />
      </Fld>
    </FormSheet>
  );
}

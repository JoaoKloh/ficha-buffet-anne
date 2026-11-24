"use client";

import { useState } from "react";
import { Carregando, FormSheet, Fld, Sel } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { CATEGORIAS_INSUMO, LOCAIS_ESTOQUE, TIPOS_ITEM, UNIDADES_INSUMO, reutilizavel, type CategoriaInsumo, type LocalEstoque, type TipoItem, type UnidadeInsumo } from "@/lib/models/dominio";
import type { InsumoResponseDTO } from "@/lib/models/insumo";
import { atualizarInsumo, criarInsumo, excluirInsumo, situacaoInsumo } from "@/lib/api/gestao/fichas";
import { dec, numIn } from "@/lib/utils/numero";
import { round2 } from "@/lib/utils/datas";

/** Local padrão pelo setor e tipo, como no protótipo. */
function localPadrao(categoria: string, tipo: TipoItem): LocalEstoque {
  const m: Record<string, LocalEstoque> = { "Proteínas e frutos do mar": "Refrigerado", "Queijos e laticínios": "Refrigerado", Hortifruti: "Refrigerado", Mercearia: "Seco", Bebidas: "Seco", "Produtos de limpeza": "Depósito limpeza" };
  return m[categoria] ?? (tipo === "limpeza" ? "Depósito limpeza" : reutilizavel(tipo) ? "Depósito de louças" : "Depósito");
}

/** Cadastro de item (ingrediente, embalagem, louça...). `onSalvo` recebe o item, para fichas e notas ligarem a linha a ele. */
export function InsumoForm({ insumo, preset, tipoInicial, onSalvo }: { insumo?: InsumoResponseDTO; preset?: { nome?: string; fornecedor?: string }; tipoInicial?: TipoItem; onSalvo?: (i: InsumoResponseDTO) => void }) {
  const { fechar } = useSheet();
  const { dados: sit } = useDados(() => (insumo ? situacaoInsumo(insumo.id) : Promise.resolve({ emFichas: 0, temMovimentos: false })), [insumo?.id]);
  const tipo0 = insumo?.tipo ?? tipoInicial ?? "ingrediente";
  const [v, setV] = useState({
    nome: insumo?.nome ?? preset?.nome ?? "",
    tipo: tipo0,
    categoria: (insumo?.categoria ?? "Mercearia") as CategoriaInsumo,
    unidade: (insumo?.unidade ?? (reutilizavel(tipo0) ? "un" : "kg")) as UnidadeInsumo,
    local: (insumo?.local ?? localPadrao("Mercearia", tipo0)) as LocalEstoque,
    unidadeCompra: insumo?.unidadeCompra ?? "",
    fatorCompra: dec(insumo?.fatorCompra ?? ""),
    estoqueMinimo: dec(insumo?.estoqueMinimo || ""),
    custo: dec(insumo?.custo || ""),
    fornecedor: insumo?.fornecedor ?? preset?.fornecedor ?? "",
  });
  if (insumo && !sit) return <Carregando />;
  const set = <K extends keyof typeof v>(k: K, x: (typeof v)[K]) => setV((s) => ({ ...s, [k]: x }));
  const dto = () => ({
    nome: v.nome.trim(), tipo: v.tipo, categoria: v.categoria, unidade: v.unidade, local: v.local, unidadeCompra: v.unidadeCompra.trim(),
    fatorCompra: numIn(v.fatorCompra) || null, estoqueMinimo: numIn(v.estoqueMinimo) || 0, custo: round2(numIn(v.custo)), fornecedor: v.fornecedor.trim(),
  });
  const usado = !!sit?.emFichas;
  const temMov = !!sit?.temMovimentos;

  return (
    <FormSheet
      intro={usado ? "Este item está em fichas técnicas, por isso não pode ser excluído." : temMov ? "Este item tem movimentos de estoque, por isso não pode ser excluído." : undefined}
      validar={() => (v.nome.trim() ? null : "Informe o nome.")}
      onSalvar={() => (insumo ? atualizarInsumo({ ...dto(), id: insumo.id }) : criarInsumo(dto()))}
      depois={
        onSalvo
          ? (r) => {
              onSalvo(r as InsumoResponseDTO);
              fechar();
            }
          : undefined
      }
      onExcluir={insumo && !usado && !temMov ? () => excluirInsumo(insumo.id) : null}
    >
      <Fld label="Nome" obrig full>
        <input value={v.nome} onChange={(e) => set("nome", e.target.value)} placeholder="Ex.: Camarão médio limpo" autoFocus />
      </Fld>
      <Fld label="Tipo">
        <Sel value={v.tipo} onChange={(x) => set("tipo", x as TipoItem)} opcoes={TIPOS_ITEM} />
      </Fld>
      <Fld label="Setor de compra">
        <Sel value={v.categoria} onChange={(x) => setV((s) => ({ ...s, categoria: x as CategoriaInsumo, local: insumo ? s.local : localPadrao(x, s.tipo) }))} opcoes={CATEGORIAS_INSUMO.map((c) => [c, c] as const)} />
      </Fld>
      <Fld label="Unidade de estoque" hint="A unidade das fichas e do saldo.">
        <Sel value={v.unidade} onChange={(x) => set("unidade", x as UnidadeInsumo)} opcoes={UNIDADES_INSUMO} />
      </Fld>
      <Fld label="Local">
        <Sel value={v.local} onChange={(x) => set("local", x as LocalEstoque)} opcoes={LOCAIS_ESTOQUE.map((l) => [l, l] as const)} />
      </Fld>
      <Fld label="Como vem na compra">
        <input value={v.unidadeCompra} onChange={(e) => set("unidadeCompra", e.target.value)} placeholder="Ex.: pote 0,2 kg, caixa 50 un" />
      </Fld>
      <Fld label="Cada embalagem de compra tem" hint="Em unidades de estoque. Ex.: 0,2 (kg) para um pote de 200 g.">
        <input inputMode="decimal" placeholder="0,00" value={v.fatorCompra} onChange={(e) => set("fatorCompra", e.target.value)} />
      </Fld>
      <Fld label="Estoque mínimo" hint="Abaixo disso o item entra em alerta.">
        <input inputMode="decimal" placeholder="0,00" value={v.estoqueMinimo} onChange={(e) => set("estoqueMinimo", e.target.value)} />
      </Fld>
      <Fld label="Preço por unidade de estoque (R$)" hint="Último preço pago. Ex.: R$ 120,41 o kg.">
        <input inputMode="decimal" placeholder="0,00" value={v.custo} onChange={(e) => set("custo", e.target.value)} />
      </Fld>
      <Fld label="Fornecedor" full>
        <input value={v.fornecedor} onChange={(e) => set("fornecedor", e.target.value)} />
      </Fld>
    </FormSheet>
  );
}

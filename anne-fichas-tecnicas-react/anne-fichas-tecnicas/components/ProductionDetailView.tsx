"use client";

import { useRouter } from "next/navigation";
import { Header } from "./Header";
import type { PratoDetalhadoResponseDTO } from "@/lib/models/prato";
import type { ProducaoResponseDTO } from "@/lib/models/producao";
import { buildKitchenRows, buildShoppingList } from "@/lib/utils/production";
import { fmtDate, fmtQuantidade } from "@/lib/utils/format";

interface Props {
  producao: ProducaoResponseDTO;
  pratos: PratoDetalhadoResponseDTO[];
}

export function ProductionDetailView({ producao, pratos }: Props) {
  const router = useRouter();
  const kitchenRows = buildKitchenRows(producao, pratos);
  const shoppingRows = buildShoppingList(producao, pratos);

  return (
    <div className="app">
      <div className="no-print">
        <Header />
      </div>

      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          flexWrap: "wrap",
          gap: 14,
          marginBottom: 24,
        }}
      >
        <div>
          <button
            type="button"
            className="btn ghost small no-print"
            style={{ marginBottom: 10 }}
            onClick={() => router.push("/producao")}
          >
            ← Voltar
          </button>
          <div
            style={{
              fontFamily: "Georgia, 'Iowan Old Style', 'Palatino Linotype', 'Book Antiqua', Palatino, serif",
              fontSize: 28,
              color: "var(--green)",
              fontWeight: 600,
            }}
          >
            {producao.nome}
          </div>
          <div style={{ fontSize: 13, color: "var(--muted)", marginTop: 2 }}>
            {fmtDate(producao.data)}
            {producao.quantidade ? ` · ${producao.quantidade} convidados` : ""} · Lista de
            produção e compras
          </div>
        </div>
        <button type="button" className="btn no-print" onClick={() => window.print()}>
          Imprimir lista completa
        </button>
      </div>

      <div className="section-h">Lista de produção — cozinha</div>
      <table className="prod-table">
        <thead>
          <tr>
            <th>Prato</th>
            <th>Categoria</th>
            <th>Quantidade a produzir</th>
            <th>Modo de preparo</th>
          </tr>
        </thead>
        <tbody>
          {kitchenRows.map((row) => (
            <tr key={row.dishId}>
              <td>{row.nome}</td>
              <td>{row.categoria}</td>
              <td className="qty-strong">
                {fmtQuantidade(row.quantidade)} {row.rendUnid}
              </td>
              <td>{row.receita || "Sem receita cadastrada."}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="section-h">Lista de compras consolidada</div>
      <table className="prod-table">
        <thead>
          <tr>
            <th>Ingrediente</th>
            <th>Quantidade total</th>
            <th>Unidade</th>
          </tr>
        </thead>
        <tbody>
          {shoppingRows.length === 0 ? (
            <tr>
              <td colSpan={3} style={{ color: "var(--muted)" }}>
                Nenhum ingrediente cadastrado nos pratos desta produção.
              </td>
            </tr>
          ) : (
            shoppingRows.map((row) => (
              <tr key={`${row.nome}-${row.unidade}`}>
                <td>{row.nome}</td>
                <td className="qty-strong">{fmtQuantidade(row.total)}</td>
                <td>{row.unidade}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

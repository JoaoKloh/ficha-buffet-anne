import type { Orcamento } from "@/lib/models/orcamento";
import { fmtCurrency, fmtDate, fmtNumero } from "@/lib/utils/format";
import { pratosSelecionados, type ResumoOrcamento } from "@/lib/utils/orcamento";

/**
 * Resumo do orçamento para o cliente. Fica oculto na tela e é a única coisa
 * impressa nesta página (ver .orc-relatorio em globals.css). O "Compartilhar"
 * de Meus orçamentos envia este mesmo relatório como texto (textoDoRelatorio).
 */
export function OrcamentoRelatorio({ orc, resumo }: { orc: Orcamento; resumo: ResumoOrcamento }) {
  const e = orc.evento;
  const cardapio = pratosSelecionados(orc);
  const linha = (rotulo: string, valor: string, forte = false) => (
    <tr>
      <td>{forte ? <b>{rotulo}</b> : rotulo}</td>
      <td className="num">{forte ? <b>{valor}</b> : valor}</td>
    </tr>
  );

  return (
    <div className="orc-relatorio" aria-hidden="true">
      <div className="orc-relatorio-head">
        <div>
          <h1 className="page-title">{e.nome || "Orçamento de evento"}</h1>
          <div className="fin-muted">
            {[e.categoria, e.local, e.data ? fmtDate(e.data) : ""].filter(Boolean).join(" · ")}
          </div>
        </div>
        <div className="orc-relatorio-marca">
          <div className="mark">ANNE</div>
          <div className="fin-muted">{resumo.pax} convidados</div>
        </div>
      </div>

      <table className="orc-relatorio-table">
        <tbody>
          {linha("Cliente", e.cliente || "—")}
          {linha("Contato", e.contato || "—")}
          {linha("Tipo de serviço", e.tipoServico || "—")}
          {linha("Duração", `${fmtNumero(e.duracaoHoras)} h`)}
        </tbody>
      </table>

      {cardapio.length > 0 && (
        <>
          <h2 className="section-h">Alimentos selecionados</h2>
          {cardapio.map((c) => (
            <div key={c.categoria} className="orc-relatorio-cardapio">
              <h3>{c.categoria}</h3>
              <ul>
                {c.pratos.map((nome, i) => (
                  <li key={`${nome}-${i}`}>{nome}</li>
                ))}
              </ul>
            </div>
          ))}
        </>
      )}

      <h2 className="section-h">Composição de custo</h2>
      <table className="orc-relatorio-table">
        <tbody>
          {linha("Alimentos", fmtCurrency(resumo.alimentos))}
          {linha("Equipe", fmtCurrency(resumo.equipe))}
          {linha("Louças, aluguel e materiais", fmtCurrency(resumo.materiais))}
          {linha("Extras e degustação", fmtCurrency(resumo.extras))}
          {linha("Custo total", fmtCurrency(resumo.custo), true)}
        </tbody>
      </table>

      <h2 className="section-h">Proposta de venda</h2>
      <table className="orc-relatorio-table">
        <tbody>
          {linha("Valor total do evento", fmtCurrency(resumo.venda), true)}
          {linha("Valor por pessoa", fmtCurrency(resumo.porPessoa), true)}
        </tbody>
      </table>

      {orc.notas && (
        <>
          <h2 className="section-h">Observações</h2>
          <p className="orc-relatorio-notas">{orc.notas}</p>
        </>
      )}
    </div>
  );
}

/**
 * Texto do relatório já renderizado, na mesma ordem e com os mesmos valores da
 * impressão. innerText não serve: o relatório fica em display:none na tela.
 */
export function textoDoRelatorio(el: HTMLElement): string {
  const texto = (n: Element) => n.textContent?.trim() ?? "";
  return Array.from(el.querySelectorAll(".orc-relatorio-head h1, .orc-relatorio-head .fin-muted, h2, h3, tr, li, p"))
    .map((n) => {
      if (n instanceof HTMLTableRowElement) return Array.from(n.cells, texto).join(": ");
      if (n.tagName === "LI") return `- ${texto(n)}`;
      if (n.tagName === "H2") return `\n${texto(n).toUpperCase()}`;
      return texto(n);
    })
    .filter(Boolean)
    .join("\n");
}

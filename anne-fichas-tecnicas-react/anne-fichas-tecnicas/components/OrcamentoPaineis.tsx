"use client";

import {
  CATEGORIAS_EVENTO,
  type CategoriaCardapio,
  type DadosEvento,
  type FuncaoEquipe,
  type GrupoItens,
  type ItemCardapio,
  type Orcamento,
  type ServicoAdicional,
} from "@/lib/models/orcamento";
import { fmtCurrency, fmtCurrencySigned, fmtNumero } from "@/lib/utils/format";
import {
  INSUMOS,
  type ResumoOrcamento,
  baseCategoria,
  qtdFuncao,
  removerEm,
  substituirEm,
  toNum,
  totalCategoria,
  totalFuncao,
  totalItemCardapio,
  totalItensQtd,
} from "@/lib/utils/orcamento";
import {
  Acordeao,
  BotaoAdicionar,
  BotaoRemover,
  Campo,
  NumInput,
  OrcCard,
  Stat,
  TabelaCustos,
  TabelaItens,
  ValorComSinal,
} from "./OrcamentoCampos";

export interface PainelProps {
  orc: Orcamento;
  resumo: ResumoOrcamento;
  atualizar: (patch: Partial<Orcamento>) => void;
}

/** Acordeões de grupos "qtd × valor", cada um com sua tabela editável. */
function GruposItens({
  grupos,
  onChange,
  rotuloNome,
}: {
  grupos: GrupoItens[];
  onChange: (grupos: GrupoItens[]) => void;
  rotuloNome?: string;
}) {
  return (
    <div className="orc-acc-list">
      {grupos.map((g, gi) => (
        <Acordeao key={g.nome} titulo={g.nome} total={totalItensQtd(g.itens)}>
          <TabelaItens
            itens={g.itens}
            rotuloNome={rotuloNome}
            onChange={(itens) => onChange(substituirEm(grupos, gi, { ...g, itens }))}
          />
        </Acordeao>
      ))}
    </div>
  );
}

// ---------- Evento ----------

export function EventoPainel({ orc, resumo, atualizar }: PainelProps) {
  const e = orc.evento;
  const set = (patch: Partial<DadosEvento>) => atualizar({ evento: { ...e, ...patch } });

  const texto = (label: string, campo: "nome" | "local" | "cliente" | "contato" | "tipoServico") => (
    <Campo label={label}>
      {(id) => <input id={id} type="text" value={e[campo]} onChange={(ev) => set({ [campo]: ev.target.value })} />}
    </Campo>
  );

  const totalVerba = INSUMOS.reduce((s, { chave }) => s + resumo.insumos[chave].verba, 0);
  const totalUtil = INSUMOS.reduce((s, { chave }) => s + resumo.insumos[chave].utilizado, 0);

  return (
    <>
      <OrcCard titulo="Dados do evento" dica="Essas informações identificam o orçamento">
        <div className="orc-grid cols-4">
          {texto("Nome do evento", "nome")}
          <Campo label="Data">
            {(id) => <input id={id} type="date" value={e.data} onChange={(ev) => set({ data: ev.target.value })} />}
          </Campo>
          {texto("Local", "local")}
          <Campo label="Categoria">
            {(id) => (
              <select id={id} value={e.categoria} onChange={(ev) => set({ categoria: ev.target.value })}>
                {CATEGORIAS_EVENTO.map((c) => (
                  <option key={c}>{c}</option>
                ))}
              </select>
            )}
          </Campo>
          {texto("Cliente", "cliente")}
          {texto("Contato", "contato")}
          {texto("Tipo de serviço", "tipoServico")}
          <Campo label="Horário">
            {(id) => (
              <input id={id} type="time" value={e.horario} onChange={(ev) => set({ horario: ev.target.value })} />
            )}
          </Campo>
          <Campo label="Número de convidados (pax)">
            {(id) => <NumInput id={id} min="1" step="1" value={e.pax} onChange={(pax) => set({ pax })} />}
          </Campo>
          <Campo label="Duração do serviço (horas)">
            {(id) => (
              <NumInput
                id={id}
                step="0.5"
                value={e.duracaoHoras}
                onChange={(duracaoHoras) => set({ duracaoHoras })}
              />
            )}
          </Campo>
        </div>
      </OrcCard>

      <OrcCard
        titulo="Orçamento de insumos"
        dica="Verba planejada × valor realmente usado (Alimentos, Bebida, Gelo, Descartáveis)"
      >
        <div className="orc-table-wrap">
          <table className="orc-table">
            <thead>
              <tr>
                <th>Categoria</th>
                <th className="num">Verba / pax (R$)</th>
                <th className="num">Verba total</th>
                <th className="num">Utilizado</th>
                <th className="num">Saldo</th>
              </tr>
            </thead>
            <tbody>
              {INSUMOS.map(({ chave, label }) => {
                const ins = resumo.insumos[chave];
                return (
                  <tr key={chave}>
                    <td>{label}</td>
                    <td className="num">
                      <NumInput
                        className="valor"
                        step="0.01"
                        value={orc.verbaInsumos[chave]}
                        aria-label={`Verba por convidado — ${label}`}
                        onChange={(v) => atualizar({ verbaInsumos: { ...orc.verbaInsumos, [chave]: v } })}
                      />
                    </td>
                    <td className="num">{fmtCurrency(ins.verba)}</td>
                    <td className="num">{fmtCurrency(ins.utilizado)}</td>
                    <td className="num total">
                      <ValorComSinal valor={ins.saldo} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
            <tfoot>
              <tr>
                <td>Total</td>
                <td />
                <td className="num">{fmtCurrency(totalVerba)}</td>
                <td className="num">{fmtCurrency(totalUtil)}</td>
                <td className="num">
                  <ValorComSinal valor={resumo.saldoInsumos} />
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </OrcCard>
    </>
  );
}

// ---------- Cardápio ----------

function TabelaCardapio({
  cat,
  base,
  onChange,
}: {
  cat: CategoriaCardapio;
  base: number;
  onChange: (cat: CategoriaCardapio) => void;
}) {
  const setItens = (itens: ItemCardapio[]) => onChange({ ...cat, itens });
  function atualizar(i: number, patch: Partial<ItemCardapio>) {
    const atual = cat.itens[i];
    if (atual) setItens(substituirEm(cat.itens, i, { ...atual, ...patch }));
  }
  const porQuem = cat.baseQtd === "staff" ? "membro" : "pessoa";

  return (
    <>
      <div className="orc-table-wrap">
        <table className="orc-table">
          <thead>
            <tr>
              <th aria-label="Incluir" />
              <th>Item</th>
              <th className="num">g/ml por porção</th>
              <th className="num">Valor unit.</th>
              <th className="num">Porções / {porQuem}</th>
              <th className="num">Total</th>
              <th aria-label="Ações" />
            </tr>
          </thead>
          <tbody>
            {cat.itens.map((it, i) => (
              <tr key={i} className={it.incluir ? "" : "off"}>
                <td>
                  <input
                    type="checkbox"
                    className="orc-chk"
                    checked={it.incluir}
                    aria-label={`Incluir ${it.nome || "item"}`}
                    onChange={(e) => atualizar(i, { incluir: e.target.checked })}
                  />
                </td>
                <td>
                  <input
                    type="text"
                    className="nome"
                    value={it.nome}
                    placeholder="Nome do item"
                    aria-label="Item"
                    onChange={(e) => atualizar(i, { nome: e.target.value })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="qtd"
                    step="1"
                    value={it.gramas}
                    aria-label={`Gramas por porção de ${it.nome || "item"}`}
                    onChange={(gramas) => atualizar(i, { gramas })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="valor"
                    step="0.01"
                    value={it.valorUnit}
                    aria-label={`Valor unitário de ${it.nome || "item"}`}
                    onChange={(valorUnit) => atualizar(i, { valorUnit })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="qtd"
                    step="0.1"
                    value={it.porcaoPax}
                    aria-label={`Porções por ${porQuem} de ${it.nome || "item"}`}
                    onChange={(porcaoPax) => atualizar(i, { porcaoPax })}
                  />
                </td>
                <td className="num total">{fmtCurrency(totalItemCardapio(it, base))}</td>
                <td>
                  <BotaoRemover
                    rotulo={`Remover ${it.nome || "item"}`}
                    onClick={() => setItens(removerEm(cat.itens, i))}
                  />
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={5}>Subtotal</td>
              <td className="num">
                {fmtCurrency(cat.itens.reduce((s, it) => s + totalItemCardapio(it, base), 0))}
              </td>
              <td />
            </tr>
          </tfoot>
        </table>
      </div>
      <BotaoAdicionar
        onClick={() => setItens([...cat.itens, { nome: "", gramas: 0, valorUnit: 0, porcaoPax: 0, incluir: true }])}
      >
        Adicionar item
      </BotaoAdicionar>
    </>
  );
}

export function CardapioPainel({ orc, resumo, atualizar }: PainelProps) {
  return (
    <OrcCard
      titulo="Cardápio"
      dica={`Total do cardápio: ${fmtCurrency(resumo.alimentos)} · ${fmtCurrency(
        resumo.alimentos / Math.max(1, resumo.pax)
      )} / pessoa · ${fmtNumero(resumo.gramas)} g por convidado`}
    >
      <div className="orc-acc-list">
        {orc.cardapio.map((cat, ci) => (
          <Acordeao
            key={cat.categoria}
            titulo={cat.baseQtd === "staff" ? `${cat.categoria} (por membro da equipe)` : cat.categoria}
            total={totalCategoria(cat, orc)}
          >
            <TabelaCardapio
              cat={cat}
              base={baseCategoria(cat, orc)}
              onChange={(nova) => atualizar({ cardapio: substituirEm(orc.cardapio, ci, nova) })}
            />
          </Acordeao>
        ))}
      </div>
    </OrcCard>
  );
}

// ---------- Bebidas & gelo / Descartáveis ----------

export function BebidasPainel({ orc, atualizar }: PainelProps) {
  return (
    <>
      <OrcCard titulo="Bebidas" dica="Marque os itens usados e ajuste quantidade/valor">
        <GruposItens grupos={orc.bebidas} rotuloNome="Bebida" onChange={(bebidas) => atualizar({ bebidas })} />
      </OrcCard>
      <OrcCard titulo="Gelo">
        <TabelaItens itens={orc.gelo} onChange={(gelo) => atualizar({ gelo })} />
      </OrcCard>
    </>
  );
}

export function DescartaveisPainel({ orc, atualizar }: PainelProps) {
  return (
    <OrcCard titulo="Descartáveis, higiene e limpeza">
      <GruposItens grupos={orc.descartaveis} onChange={(descartaveis) => atualizar({ descartaveis })} />
    </OrcCard>
  );
}

// ---------- Equipe ----------

export function EquipePainel({ orc, resumo, atualizar }: PainelProps) {
  const pax = resumo.pax;
  function set(i: number, patch: Partial<FuncaoEquipe>) {
    const atual = orc.equipe[i];
    if (atual) atualizar({ equipe: substituirEm(orc.equipe, i, { ...atual, ...patch }) });
  }

  return (
    <OrcCard titulo="Equipe do evento" dica={'"Auto" sugere a quantidade pelo nº de convidados por profissional'}>
      <div className="orc-table-wrap">
        <table className="orc-table">
          <thead>
            <tr>
              <th>Função</th>
              <th className="num">Pax / profissional</th>
              <th className="num">Auto</th>
              <th className="num">Qtd.</th>
              <th className="num">Diárias</th>
              <th className="num">Valor diária</th>
              <th className="num">Total</th>
              <th aria-label="Ações" />
            </tr>
          </thead>
          <tbody>
            {orc.equipe.map((f, i) => (
              <tr key={i}>
                <td>
                  <input
                    type="text"
                    className="nome"
                    value={f.funcao}
                    aria-label="Função"
                    onChange={(e) => set(i, { funcao: e.target.value })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="qtd"
                    step="1"
                    value={f.profissionalPorPax}
                    aria-label={`Convidados por ${f.funcao}`}
                    onChange={(profissionalPorPax) => set(i, { profissionalPorPax })}
                  />
                </td>
                <td className="num">
                  <input
                    type="checkbox"
                    className="orc-chk"
                    checked={f.auto}
                    aria-label={`Quantidade automática de ${f.funcao}`}
                    // Ao desligar o "auto", fixa a quantidade que estava sendo sugerida.
                    onChange={(e) => set(i, { auto: e.target.checked, qtd: qtdFuncao(f, pax) })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="qtd"
                    step="1"
                    value={qtdFuncao(f, pax)}
                    disabled={f.auto}
                    aria-label={`Quantidade de ${f.funcao}`}
                    onChange={(qtd) => set(i, { qtd })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="qtd"
                    step="1"
                    value={f.diarias}
                    aria-label={`Diárias de ${f.funcao}`}
                    onChange={(diarias) => set(i, { diarias })}
                  />
                </td>
                <td className="num">
                  <NumInput
                    className="valor"
                    step="0.01"
                    value={f.valorUnit}
                    aria-label={`Valor da diária de ${f.funcao}`}
                    onChange={(valorUnit) => set(i, { valorUnit })}
                  />
                </td>
                <td className="num total">{fmtCurrency(totalFuncao(f, pax))}</td>
                <td>
                  <BotaoRemover
                    rotulo={`Remover ${f.funcao}`}
                    onClick={() => atualizar({ equipe: removerEm(orc.equipe, i) })}
                  />
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={3}>Total</td>
              <td className="num">{resumo.equipeQtd}</td>
              <td colSpan={2} />
              <td className="num">{fmtCurrency(resumo.equipe)}</td>
              <td />
            </tr>
          </tfoot>
        </table>
      </div>
      <BotaoAdicionar
        onClick={() =>
          atualizar({
            equipe: [
              ...orc.equipe,
              { funcao: "Nova função", profissionalPorPax: 0, qtd: 1, diarias: 1, valorUnit: 0, auto: false },
            ],
          })
        }
      >
        Adicionar função
      </BotaoAdicionar>
    </OrcCard>
  );
}

// ---------- Materiais & extras ----------

export function MateriaisPainel({ orc, resumo, atualizar }: PainelProps) {
  const mat = orc.materiais;
  return (
    <>
      <OrcCard titulo="Frete & locação">
        <div className="orc-grid cols-3">
          <Campo label="Frete (R$)">
            {(id) => (
              <NumInput
                id={id}
                step="0.01"
                value={mat.frete}
                onChange={(frete) => atualizar({ materiais: { ...mat, frete } })}
              />
            )}
          </Campo>
          <Campo label="Verba de locação (R$)">
            {(id) => (
              <NumInput
                id={id}
                step="0.01"
                value={mat.verbaLocacao}
                onChange={(verbaLocacao) => atualizar({ materiais: { ...mat, verbaLocacao } })}
              />
            )}
          </Campo>
          <Stat rotulo="Louças & materiais / pax" valor={fmtCurrency(resumo.materiais / Math.max(1, resumo.pax))} />
        </div>
      </OrcCard>
      <OrcCard titulo="Louças, utensílios e estrutura" dica="Catálogo de aluguel — marque o que entra neste evento">
        <GruposItens grupos={mat.grupos} onChange={(grupos) => atualizar({ materiais: { ...mat, grupos } })} />
      </OrcCard>
      <OrcCard titulo="Verba extra">
        <TabelaCustos itens={orc.verbaExtra} onChange={(verbaExtra) => atualizar({ verbaExtra })} />
      </OrcCard>
      <OrcCard titulo="Custo de degustação">
        <TabelaCustos itens={orc.degustacao} onChange={(degustacao) => atualizar({ degustacao })} />
      </OrcCard>
    </>
  );
}

// ---------- Comercial ----------

const NOVO_SERVICO: ServicoAdicional = { nome: "", empresa: "", custo: 0, valorCobrado: 0 };

export function ComercialPainel({ orc, resumo, atualizar }: PainelProps) {
  const servicos = orc.servicosAdicionais;
  const pacote = orc.pacoteBebidas;
  function set(i: number, patch: Partial<ServicoAdicional>) {
    const atual = servicos[i];
    if (atual) atualizar({ servicosAdicionais: substituirEm(servicos, i, { ...atual, ...patch }) });
  }
  const adicionar = () => atualizar({ servicosAdicionais: [...servicos, { ...NOVO_SERVICO }] });

  return (
    <>
      <OrcCard titulo="Serviços adicionais (terceirizados)" dica="Fotografia, decoração, DJ etc. repassados ao cliente">
        {servicos.length === 0 ? (
          <p className="fin-muted orc-empty">
            Nenhum serviço adicional ainda. Ex.: fotografia, decoração, DJ, cerimonial.
          </p>
        ) : (
          <div className="orc-table-wrap">
            <table className="orc-table">
              <thead>
                <tr>
                  <th>Serviço</th>
                  <th>Empresa</th>
                  <th className="num">Custo (fornecedor)</th>
                  <th className="num">Valor cobrado</th>
                  <th className="num">Contribuição</th>
                  <th aria-label="Ações" />
                </tr>
              </thead>
              <tbody>
                {servicos.map((s, i) => (
                  <tr key={i}>
                    <td>
                      <input
                        type="text"
                        className="nome"
                        value={s.nome}
                        placeholder="Ex.: Fotografia"
                        aria-label="Serviço"
                        onChange={(e) => set(i, { nome: e.target.value })}
                      />
                    </td>
                    <td>
                      <input
                        type="text"
                        className="nome"
                        value={s.empresa}
                        placeholder="Fornecedor"
                        aria-label="Empresa"
                        onChange={(e) => set(i, { empresa: e.target.value })}
                      />
                    </td>
                    <td className="num">
                      <NumInput
                        className="valor"
                        step="0.01"
                        value={s.custo}
                        aria-label={`Custo de ${s.nome || "serviço"}`}
                        onChange={(custo) => set(i, { custo })}
                      />
                    </td>
                    <td className="num">
                      <NumInput
                        className="valor"
                        step="0.01"
                        value={s.valorCobrado}
                        aria-label={`Valor cobrado de ${s.nome || "serviço"}`}
                        onChange={(valorCobrado) => set(i, { valorCobrado })}
                      />
                    </td>
                    <td className="num total">
                      <ValorComSinal valor={toNum(s.valorCobrado) - toNum(s.custo)} />
                    </td>
                    <td>
                      <BotaoRemover
                        rotulo={`Remover ${s.nome || "serviço"}`}
                        onClick={() => atualizar({ servicosAdicionais: removerEm(servicos, i) })}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <BotaoAdicionar onClick={adicionar}>Adicionar serviço</BotaoAdicionar>
      </OrcCard>

      <OrcCard titulo="Pacote de bebidas premium (upsell)" dica="Bebidas vendidas à parte, fora da verba do evento">
        <TabelaItens
          itens={pacote.itens}
          rotuloNome="Bebida"
          onChange={(itens) => atualizar({ pacoteBebidas: { ...pacote, itens } })}
        />
        <div className="orc-grid cols-3 orc-mt">
          <Campo label="Valor de venda do pacote / pessoa (R$)">
            {(id) => (
              <NumInput
                id={id}
                step="0.5"
                value={pacote.valorVendaPorPessoa}
                onChange={(valorVendaPorPessoa) => atualizar({ pacoteBebidas: { ...pacote, valorVendaPorPessoa } })}
              />
            )}
          </Campo>
          <Stat rotulo="Custo do pacote" valor={fmtCurrency(resumo.pacoteCusto)} />
          <Stat
            rotulo="Contribuição do pacote"
            valor={fmtCurrencySigned(resumo.pacoteContrib)}
            sinal={resumo.pacoteContrib}
          />
        </div>
      </OrcCard>
    </>
  );
}

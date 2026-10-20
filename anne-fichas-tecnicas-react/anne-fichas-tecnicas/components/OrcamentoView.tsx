"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { flushSync } from "react-dom";
import { Header } from "./Header";
import { useToast } from "./ui/Toast";
import { MeusOrcamentosModal, SalvarOrcamentoModal } from "./OrcamentoModais";
import {
  BebidasPainel,
  CardapioPainel,
  ComercialPainel,
  DescartaveisPainel,
  EquipePainel,
  EventoPainel,
  MateriaisPainel,
  type PainelProps,
} from "./OrcamentoPaineis";
import { PrecificacaoPainel } from "./OrcamentoPrecificacao";
import { OrcamentoRelatorio, textoDoRelatorio } from "./OrcamentoRelatorio";
import {
  carregarRascunho,
  excluirOrcamento,
  listarOrcamentos,
  salvarOrcamento,
  salvarRascunho,
} from "@/lib/api/orcamentos";
import { CreateOrcamentoRequestSchema, type Orcamento, type OrcamentoSalvo } from "@/lib/models/orcamento";
import { fmtCurrency, fmtPercent } from "@/lib/utils/format";
import { calcularResumo, faixaMargem, novoOrcamento, paraCreateOrcamentoRequest } from "@/lib/utils/orcamento";

const ABAS = [
  { id: "evento", label: "Evento", Painel: EventoPainel },
  { id: "cardapio", label: "Cardápio", Painel: CardapioPainel },
  { id: "bebidas", label: "Bebidas & Gelo", Painel: BebidasPainel },
  { id: "descartaveis", label: "Descartáveis", Painel: DescartaveisPainel },
  { id: "equipe", label: "Equipe", Painel: EquipePainel },
  { id: "materiais", label: "Materiais & Extras", Painel: MateriaisPainel },
  { id: "comercial", label: "Comercial", Painel: ComercialPainel },
  { id: "precificacao", label: "Precificação", Painel: PrecificacaoPainel },
] as const satisfies readonly { id: string; label: string; Painel: React.ComponentType<PainelProps> }[];

type AbaId = (typeof ABAS)[number]["id"];

// Preferência de interface (não é dado do orçamento): lembra a última aba aberta.
const ABA_KEY = "anne:orcamento-aba";

function lerUltimaAba(): AbaId {
  try {
    const salva = window.localStorage.getItem(ABA_KEY);
    return ABAS.find((a) => a.id === salva)?.id ?? "evento";
  } catch {
    return "evento";
  }
}

const ROTULO_FAIXA = { boa: "boa", atencao: "atenção", critica: "crítica" } as const;

export function OrcamentoView() {
  const [orc, setOrc] = useState<Orcamento>(novoOrcamento);
  const [carregado, setCarregado] = useState(false);
  const [aba, setAba] = useState<AbaId>("evento");
  // Troca a cada orçamento novo/aberto para remontar os painéis (acordeões voltam ao estado inicial).
  const [versao, setVersao] = useState(0);
  const [modal, setModal] = useState<"salvar" | "meus" | null>(null);
  const [salvos, setSalvos] = useState<OrcamentoSalvo[]>([]);
  // Orçamento salvo exibido no relatório durante um "Compartilhar"; null = o orçamento em edição.
  const [orcRelatorio, setOrcRelatorio] = useState<Orcamento | null>(null);
  const relatorioRef = useRef<HTMLDivElement>(null);
  const { showToast } = useToast();

  // Rascunho e aba vivem no navegador (ver lib/api/orcamentos.ts): só depois da montagem.
  useEffect(() => {
    carregarRascunho().then((rascunho) => {
      if (rascunho) setOrc(rascunho);
      setAba(lerUltimaAba());
      setCarregado(true);
    });
  }, []);

  // Autosave do rascunho a cada alteração, como no artefato original.
  useEffect(() => {
    if (carregado) salvarRascunho(orc);
  }, [orc, carregado]);

  const resumo = useMemo(() => calcularResumo(orc), [orc]);
  const resumoRelatorio = useMemo(
    () => (orcRelatorio ? calcularResumo(orcRelatorio) : resumo),
    [orcRelatorio, resumo]
  );

  function atualizar(patch: Partial<Orcamento>) {
    setOrc((prev) => ({ ...prev, ...patch }));
  }

  function trocarOrcamento(novo: Orcamento) {
    setOrc(novo);
    setVersao((v) => v + 1);
  }

  function selecionarAba(id: AbaId) {
    setAba(id);
    try {
      window.localStorage.setItem(ABA_KEY, id);
    } catch {
      // sem armazenamento: só não lembra a aba
    }
  }

  function handleNovo() {
    if (
      !confirm(
        "Começar um novo orçamento em branco? As alterações não salvas deste orçamento serão substituídas."
      )
    )
      return;
    trocarOrcamento(novoOrcamento());
    selecionarAba("evento");
  }

  async function abrirMeus() {
    try {
      setSalvos(await listarOrcamentos());
      setModal("meus");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Não foi possível carregar os orçamentos.", "error");
    }
  }

  async function handleSalvar(nome: string) {
    const atualizado: Orcamento = { ...orc, evento: { ...orc.evento, nome } };
    const req = CreateOrcamentoRequestSchema.safeParse(paraCreateOrcamentoRequest(atualizado, resumo));
    if (!req.success) {
      showToast(req.error.issues[0]?.message ?? "Revise os dados do orçamento.", "error");
      return;
    }
    try {
      await salvarOrcamento(req.data, atualizado);
      setOrc(atualizado);
      setModal(null);
      showToast("Orçamento salvo.");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Não foi possível salvar o orçamento.", "error");
    }
  }

  function handleAbrir(o: OrcamentoSalvo) {
    if (!o.orcamento) return;
    trocarOrcamento(structuredClone(o.orcamento));
    setModal(null);
    showToast(`Orçamento "${o.orcamento.evento.nome || o.nomeCliente}" aberto.`);
  }

  async function handleExcluir(o: OrcamentoSalvo) {
    const nome = o.orcamento?.evento.nome || o.nomeCliente;
    if (!confirm(`Excluir o orçamento "${nome}"? Essa ação não pode ser desfeita.`)) return;
    try {
      await excluirOrcamento(o.id);
      setSalvos((prev) => prev.filter((x) => x.id !== o.id));
      showToast("Orçamento excluído.");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Não foi possível excluir o orçamento.", "error");
    }
  }

  /**
   * "Imprimir resumo" e "Compartilhar" geram o mesmo OrcamentoRelatorio. `alvo` troca o
   * orçamento exibido nele só durante a ação (flushSync: o relatório precisa estar no DOM
   * antes da leitura, e navigator.share tem de ser chamado ainda dentro do clique).
   */
  async function emitirResumo(modo: "imprimir" | "compartilhar", alvo?: Orcamento) {
    flushSync(() => setOrcRelatorio(alvo ?? null));
    try {
      if (modo === "imprimir") {
        window.print();
        return;
      }
      const el = relatorioRef.current;
      if (!el) return;
      const texto = textoDoRelatorio(el);
      if (typeof navigator.share === "function") {
        await navigator.share({ title: (alvo ?? orc).evento.nome || "Orçamento de evento", text: texto });
      } else {
        await navigator.clipboard.writeText(texto);
        showToast("Resumo copiado para a área de transferência.");
      }
    } catch (err) {
      // Usuário fechou a folha de compartilhamento: não é erro.
      if (err instanceof DOMException && err.name === "AbortError") return;
      showToast("Não foi possível compartilhar o resumo.", "error");
    } finally {
      setOrcRelatorio(null);
    }
  }

  const abaAtual = ABAS.find((a) => a.id === aba) ?? ABAS[0];
  const Painel = abaAtual.Painel;
  const faixa = faixaMargem(resumo.margemPct);

  return (
    <div className="app orc-app">
      <div className="no-print">
        <Header
          actions={
            <>
              <button type="button" className="btn ghost small" onClick={handleNovo}>
                + Novo
              </button>
              <button type="button" className="btn ghost small" onClick={abrirMeus}>
                Meus orçamentos
              </button>
              <button type="button" className="btn ghost small" onClick={() => emitirResumo("imprimir")}>
                Imprimir resumo
              </button>
              <button type="button" className="btn small" onClick={() => setModal("salvar")}>
                Salvar
              </button>
            </>
          }
        />

        {!carregado ? (
          <p className="empty-state">Carregando orçamento...</p>
        ) : (
          <>
            {orc.evento.nome && <p className="orc-evento-nome">{orc.evento.nome}</p>}

            <div className="chips orc-tabs" role="tablist" aria-label="Etapas do orçamento">
              {ABAS.map((a) => (
                <button
                  key={a.id}
                  type="button"
                  role="tab"
                  id={`orc-tab-${a.id}`}
                  aria-selected={aba === a.id}
                  aria-controls="orc-painel"
                  className={`chip ${aba === a.id ? "active" : ""}`}
                  onClick={() => selecionarAba(a.id)}
                >
                  {a.label}
                </button>
              ))}
            </div>

            <div
              key={versao}
              id="orc-painel"
              role="tabpanel"
              aria-labelledby={`orc-tab-${aba}`}
              className="fin-stack"
            >
              <Painel orc={orc} resumo={resumo} atualizar={atualizar} />
            </div>

            <footer className="orc-summary" aria-label="Resumo do orçamento">
              <div className="orc-summary-item">
                <span className="orc-stat-label">Custo total</span>
                <span className="orc-summary-valor">{fmtCurrency(resumo.custo)}</span>
              </div>
              <div className="orc-summary-item">
                <span className="orc-stat-label">Venda sugerida</span>
                <span className="orc-summary-valor">{fmtCurrency(resumo.venda)}</span>
              </div>
              <div className="orc-summary-item">
                <span className="orc-stat-label">Por pessoa</span>
                <span className="orc-summary-valor">{fmtCurrency(resumo.porPessoa)}</span>
              </div>
              <div className="orc-summary-item">
                <span className="orc-stat-label">Lucro estimado</span>
                <span className="orc-summary-valor">{fmtCurrency(resumo.lucro)}</span>
              </div>
              <span
                className={`orc-margem ${faixa}`}
                title={`Margem de contribuição ${ROTULO_FAIXA[faixa]} (boa ≥ 40%, atenção ≥ 20%)`}
              >
                Margem {fmtPercent(resumo.margemPct)}
              </span>
            </footer>
          </>
        )}
      </div>

      {carregado && (
        <div ref={relatorioRef}>
          <OrcamentoRelatorio orc={orcRelatorio ?? orc} resumo={resumoRelatorio} />
        </div>
      )}

      {modal === "salvar" && (
        <SalvarOrcamentoModal
          nomeInicial={orc.evento.nome}
          onSalvar={handleSalvar}
          onClose={() => setModal(null)}
        />
      )}
      {modal === "meus" && (
        <MeusOrcamentosModal
          orcamentos={salvos}
          onAbrir={handleAbrir}
          onCompartilhar={(o) => o.orcamento && emitirResumo("compartilhar", o.orcamento)}
          onExcluir={handleExcluir}
          onClose={() => setModal(null)}
        />
      )}
    </div>
  );
}

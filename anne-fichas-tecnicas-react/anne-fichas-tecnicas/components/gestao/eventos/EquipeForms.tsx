"use client";

import { useState } from "react";
import { Carregando, Confirma, FormSheet, Fld, Pill, Sel, Vazio, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { ICONES } from "@/components/ui/Icones";
import { FUNCOES_EQUIPE, type FuncaoEquipe } from "@/lib/models/dominio";
import type { AjustarEquipeRequestDTO, EquipeResponseDTO } from "@/lib/models/equipe";
import type { EventoResponseDTO, PessoaEquipeDTO, SalvarPessoaEquipeRequestDTO } from "@/lib/models/evento";
import { ajustarEquipe, listarEquipe } from "@/lib/api/gestao/equipe";
import { associarEquipe, atualizarPessoaEquipe, criarPessoaEquipe, detalheEvento, excluirPessoaEquipe } from "@/lib/api/gestao/eventos";
import { brl, dec, numIn } from "@/lib/utils/numero";
import { round2 } from "@/lib/utils/datas";
import { semAcento } from "@/lib/utils/format";

type PessoaFormProps =
  /** Escalar alguém no evento (pessoa nova ou já cadastrada, pelo nome) ou editar quem já está na escala. */
  | { evento: EventoResponseDTO; pessoa?: PessoaEquipeDTO; cadastro?: undefined }
  /** Editar o cadastro da pessoa (ícone de edição no checklist): mesmos campos, sem os horários de cada evento. */
  | { cadastro: EquipeResponseDTO; evento?: undefined; pessoa?: undefined };

/** Formulário da pessoa da equipe: escala do evento ou, com `cadastro`, a edição do cadastro dela. */
export function PessoaForm({ evento, pessoa, cadastro }: PessoaFormProps) {
  const p = pessoa;
  const c = cadastro;
  const [v, setV] = useState({
    nome: c?.nome ?? p?.nome ?? "",
    funcao: (c?.funcaoPadrao ?? p?.funcao ?? "Garçom") as FuncaoEquipe,
    chegada: p?.chegada ?? "",
    saida: p?.saida ?? "",
    valor: dec(c?.diariaPadrao ?? p?.valor ?? ""),
    contato: c?.contato ?? p?.contato ?? "",
    pix: c?.pix ?? p?.pix ?? "",
  });
  const set = (k: keyof typeof v) => (x: { target: { value: string } }) => setV({ ...v, [k]: x.target.value });
  const dto = (): SalvarPessoaEquipeRequestDTO => ({ ...v, nome: v.nome.trim(), valor: round2(numIn(v.valor)) });

  // Cadastro: só os campos que mudaram vão no PATCH; sem mudança, nada é enviado.
  const mudancas = (x: EquipeResponseDTO): AjustarEquipeRequestDTO => {
    const d: AjustarEquipeRequestDTO = {};
    if (v.nome.trim() !== x.nome) d.nome = v.nome.trim();
    if (v.funcao !== x.funcaoPadrao) d.funcaoPadrao = v.funcao;
    if (round2(numIn(v.valor)) !== round2(x.diariaPadrao)) d.diariaPadrao = round2(numIn(v.valor));
    if (v.contato.trim() !== x.contato) d.contato = v.contato.trim();
    if (v.pix.trim() !== x.pix) d.pix = v.pix.trim();
    return d;
  };
  const salvarCadastro = (x: EquipeResponseDTO) => {
    const d = mudancas(x);
    return Object.keys(d).length ? ajustarEquipe(x.id, d) : Promise.resolve(null);
  };

  return (
    <FormSheet
      intro={
        c
          ? "Nome, telefone e Pix valem em todas as escalas (contas a pagar de diária ainda não pagas acompanham o nome). Função e diária padrão só valem para as próximas escalas: as existentes mantêm o que foi combinado em cada evento."
          : p?.pago
            ? "A diária desta pessoa já foi paga: para mudar o valor, ajuste o pagamento em Caixa."
            : !p
              ? "Cadastra a pessoa na equipe e já escala neste evento. A função e a diária viram o padrão dela no cadastro, usado quando ela for marcada na lista de outros eventos."
              : "Nome, telefone e Pix mudam no cadastro da pessoa e valem para os outros eventos; função, horários e diária são só deste evento."
      }
      validar={() => (!v.nome.trim() ? "Informe o nome." : p?.pago && round2(numIn(v.valor)) !== round2(p.valor) ? "A diária já foi paga; o valor não pode mudar aqui." : null)}
      onSalvar={() => (c ? salvarCadastro(c) : p ? atualizarPessoaEquipe(evento.id, p.id, dto()) : criarPessoaEquipe(evento!.id, dto()))}
      okMsg={c ? "Cadastro salvo" : p ? "Salvo" : "Escalado"}
      onExcluir={evento && p && !p.pago ? () => excluirPessoaEquipe(evento.id, p.id) : null}
      excluirLabel="Tirar da escala"
      excluirConfirma={`Tirar ${p?.nome || "esta pessoa"} da escala?${p?.lancamentoId != null ? " A conta a pagar da diária também sai." : ""}`}
    >
      <Fld label="Nome" obrig>
        <input value={v.nome} onChange={set("nome")} autoFocus />
      </Fld>
      <Fld label={c ? "Função padrão" : "Função"}>
        <Sel value={v.funcao} onChange={(x) => setV({ ...v, funcao: x as FuncaoEquipe })} opcoes={FUNCOES_EQUIPE.map((f) => [f, f] as const)} />
      </Fld>
      {!c && (
        <>
          <Fld label="Chegada">
            <input type="time" value={v.chegada} onChange={set("chegada")} />
          </Fld>
          <Fld label="Saída">
            <input type="time" value={v.saida} onChange={set("saida")} />
          </Fld>
        </>
      )}
      <Fld label={c ? "Diária padrão (R$)" : "Diária / valor (R$)"}>
        <input inputMode="decimal" placeholder="0,00" value={v.valor} onChange={set("valor")} disabled={p?.pago} />
      </Fld>
      <Fld label="Telefone">
        <input type="tel" value={v.contato} onChange={set("contato")} />
      </Fld>
      <Fld label="Chave Pix" full>
        <input value={v.pix} onChange={set("pix")} />
      </Fld>
    </FormSheet>
  );
}

/**
 * Escalar equipe (checklist): marca quem trabalha no evento entre as pessoas do cadastro e salva a
 * escala inteira num pedido só. Quem entra usa a função e a diária padrão do cadastro; desmarcar
 * tira da escala (quem tem diária paga fica travado). "Novo integrante" cadastra e já escala.
 */
export function EquipeChecklistSheet({ evento }: { evento: EventoResponseDTO }) {
  const { dados: pessoas, erro } = useDados(() => listarEquipe());
  // A escala é lida de novo aqui (e não recebida por prop) para incluir quem entrou por "Novo integrante".
  const { dados: det, erro: erroDet } = useDados(() => detalheEvento(evento.id), [evento.id]);
  const { abrir, fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  // Só as mudanças em relação à escala gravada: sobrevivem às recargas da lista.
  const [entram, setEntram] = useState<Set<number>>(new Set());
  const [saem, setSaem] = useState<Set<number>>(new Set());
  const [busca, setBusca] = useState("");
  const [funcao, setFuncao] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  if (!pessoas || !det) return <Carregando erro={erro ?? erroDet} />;

  const novo = () => abrir("Novo integrante", <PessoaForm evento={evento} />);
  if (!pessoas.length) {
    return (
      <Vazio titulo="Ninguém no cadastro da equipe">
        Cadastre a primeira pessoa: ela já entra na escala deste evento e fica disponível para os próximos.
        <div className="toolbar" style={{ justifyContent: "center" }}>
          <button type="button" className="btn sm primary" onClick={novo}>
            Novo integrante
          </button>
        </div>
      </Vazio>
    );
  }

  const escala = new Map(det.equipe.map((x) => [x.equipeId, x]));
  const vaiEntrar = [...entram].filter((id) => !escala.has(id));
  const vaiSair = [...saem].filter((id) => escala.has(id));
  const marcado = (id: number) => (escala.has(id) ? !saem.has(id) : entram.has(id));
  const travado = (id: number) => !!escala.get(id)?.pago;
  const total = escala.size - vaiSair.length + vaiEntrar.length;
  const contasQueSaem = vaiSair.filter((id) => escala.get(id)?.lancamentoId != null).length;

  const marcar = (ids: number[], on: boolean) => {
    const livres = ids.filter((id) => !travado(id));
    const muda = (s: Set<number>, add: number[], del: number[]) => {
      const n = new Set(s);
      add.forEach((id) => n.add(id));
      del.forEach((id) => n.delete(id));
      return n;
    };
    const naEscala = livres.filter((id) => escala.has(id));
    const fora = livres.filter((id) => !escala.has(id));
    setSaem((s) => (on ? muda(s, [], naEscala) : muda(s, naEscala, [])));
    setEntram((s) => (on ? muda(s, fora, []) : muda(s, [], fora)));
    setConfirmando(false);
  };
  const limpar = () => {
    setEntram(new Set());
    setSaem(new Set());
    setConfirmando(false);
  };

  const q = semAcento(busca.trim());
  const visiveis = pessoas.filter((p) => (!funcao || p.funcaoPadrao === funcao) && (!q || semAcento(`${p.nome} ${p.contato} ${p.pix}`).includes(q)));
  const funcoes = FUNCOES_EQUIPE.filter((f) => pessoas.some((p) => p.funcaoPadrao === f));
  const grupos = funcoes.map((f) => ({ funcao: f, itens: visiveis.filter((p) => p.funcaoPadrao === f) })).filter((g) => g.itens.length);

  async function salvar() {
    setOcupado(true);
    try {
      const ids = pessoas!.map((p) => p.id).filter(marcado);
      const r = await associarEquipe(evento.id, { equipeIds: ids });
      showToast([r.adicionados && `${r.adicionados} escalado${r.adicionados === 1 ? "" : "s"}`, r.removidos && `${r.removidos} fora da escala`].filter(Boolean).join(" · ") || "Escala salva");
      recarregar();
      fechar();
    } catch (e) {
      showToast(msgErro(e), "error");
      setOcupado(false);
      setConfirmando(false);
    }
  }

  return (
    <>
      <p className="note">
        Marque quem trabalha em <b>{evento.nome}</b> e salve uma vez só. Quem entra usa a função e a diária do cadastro; ajuste cada pessoa depois tocando nela na escala.
        Desmarcar tira da escala.
      </p>
      <div className="toolbar">
        <input className="search" type="search" placeholder="Buscar por nome, telefone ou Pix…" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar pessoa" />
        <button type="button" className="btn sm" onClick={novo}>
          Novo integrante
        </button>
      </div>
      {funcoes.length > 1 && (
        <div className="toolbar" role="group" aria-label="Função">
          <button type="button" className="chip" aria-pressed={!funcao} onClick={() => setFuncao("")}>
            Todas
          </button>
          {funcoes.map((f) => (
            <button key={f} type="button" className="chip" aria-pressed={funcao === f} onClick={() => setFuncao(f)}>
              {f}
            </button>
          ))}
        </div>
      )}
      <div className="box">
        {!grupos.length && <Vazio titulo="Ninguém encontrado">Mude a busca ou a função, ou cadastre um novo integrante.</Vazio>}
        {grupos.map((g) => {
          const livres = g.itens.map((p) => p.id).filter((id) => !travado(id));
          const todos = livres.length > 0 && livres.every(marcado);
          return (
            <div key={g.funcao}>
              <div className="monthhead" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span>{g.funcao}</span>
                <label className="check" style={{ fontSize: 11 }}>
                  marcar todos <input type="checkbox" className="czck" checked={todos} disabled={!livres.length} onChange={(e) => marcar(livres, e.target.checked)} />
                </label>
              </div>
              {g.itens.map((p) => (
                <PessoaLinha
                  key={p.id}
                  p={p}
                  naEscala={escala.get(p.id)}
                  marcado={marcado(p.id)}
                  onMarcar={(on) => marcar([p.id], on)}
                  onEditar={() => abrir("Editar funcionário", <PessoaForm cadastro={p} />)}
                />
              ))}
            </div>
          );
        })}
      </div>
      <div className="actions">
        <span className="note left">
          <b>{total}</b> na escala{vaiEntrar.length > 0 && ` · ${vaiEntrar.length} entra${vaiEntrar.length === 1 ? "" : "m"}`}
          {vaiSair.length > 0 && ` · ${vaiSair.length} sai${vaiSair.length === 1 ? "" : "em"}`}
        </span>
        {(vaiEntrar.length > 0 || vaiSair.length > 0) && (
          <button type="button" className="btn sm ghost" onClick={limpar}>
            Desfazer
          </button>
        )}
        <button type="button" className="btn" onClick={() => fechar()}>
          Cancelar
        </button>
        <button
          type="button"
          className="btn primary"
          disabled={(!vaiEntrar.length && !vaiSair.length) || ocupado}
          onClick={() => (vaiSair.length ? setConfirmando(true) : salvar())}
        >
          Associar equipe
        </button>
      </div>
      {confirmando && (
        <Confirma
          msg={`Tirar ${vaiSair.length} pessoa${vaiSair.length === 1 ? "" : "s"} da escala?${contasQueSaem ? ` ${contasQueSaem} conta${contasQueSaem === 1 ? "" : "s"} a pagar de diária ainda não paga${contasQueSaem === 1 ? "" : "s"} também sai${contasQueSaem === 1 ? "" : "em"}.` : ""}`}
          rotulo="Tirar e salvar"
          onSim={salvar}
          onNao={() => setConfirmando(false)}
        />
      )}
    </>
  );
}

/** Linha do checklist: dados do cadastro e, para quem já está na escala, como está escalado. */
function PessoaLinha({
  p,
  naEscala,
  marcado,
  onMarcar,
  onEditar,
}: {
  p: EquipeResponseDTO;
  naEscala?: PessoaEquipeDTO;
  marcado: boolean;
  onMarcar: (on: boolean) => void;
  onEditar: () => void;
}) {
  const pago = !!naEscala?.pago;
  const cadastro = [p.funcaoPadrao, p.diariaPadrao ? `diária ${brl(p.diariaPadrao)}` : "sem diária", p.contato, p.pix && `Pix ${p.pix}`].filter(Boolean).join(" · ");
  const situacao = naEscala
    ? pago
      ? "Diária paga: não sai da escala"
      : marcado
        ? `Na escala como ${naEscala.funcao} · ${brl(naEscala.valor)}`
        : "Sai da escala ao salvar"
    : marcado
      ? "Entra na escala ao salvar"
      : null;
  return (
    <label className={`row static${pago ? " off" : ""}`} style={{ cursor: pago ? "default" : "pointer" }}>
      <input type="checkbox" className="czck" checked={marcado} disabled={pago} onChange={(e) => onMarcar(e.target.checked)} />
      <span className="grow">
        <span className="t wrap">{p.nome}</span>
        <span className="m">{cadastro}</span>
        {situacao && <span className="m">{situacao}</span>}
      </span>
      {naEscala && !marcado && <Pill tom="bad">sai</Pill>}
      {!naEscala && marcado && <Pill tom="good">entra</Pill>}
      {naEscala && marcado && <Pill>na escala</Pill>}
      <button
        type="button"
        className="btn sm ghost ico"
        aria-label={`Editar ${p.nome}`}
        title="Editar funcionário"
        onClick={(e) => {
          // Dentro do <label>: sem isto o clique também marcaria/desmarcaria a pessoa.
          e.preventDefault();
          e.stopPropagation();
          onEditar();
        }}
      >
        {ICONES.editar}
      </button>
    </label>
  );
}

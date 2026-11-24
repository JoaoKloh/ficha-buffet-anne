"use client";

import { useRef, useState } from "react";
import { Carregando, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import type { AnaliseBackupResponseDTO, BackupArquivoDTO, ModoRestauracao, RestaurarBackupResponseDTO } from "@/lib/models/backup";
import { URL_PLANILHA_BACKUP, analisarBackup, exportarBackup, registrarBackup, restaurarBackup, resumoBackup } from "@/lib/api/gestao/painel";
import { hoje, toIso } from "@/lib/utils/datas";
import { fdate, plural } from "@/lib/utils/numero";

const TAM_MAX = 60 * 1024 * 1024;

/** Data e hora locais de um carimbo ISO. */
const fdh = (iso: string | null | undefined) => {
  if (!iso) return "—";
  const d = new Date(iso);
  return isNaN(d.getTime()) ? "—" : `${fdate(toIso(d))} às ${d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}`;
};

/** Entrega um arquivo gerado na tela para o navegador salvar. */
function baixar(nome: string, conteudo: string, tipo = "application/json") {
  const url = URL.createObjectURL(new Blob([conteudo], { type: tipo }));
  const a = document.createElement("a");
  a.href = url;
  a.download = nome;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

async function salvarCopia(nome: string): Promise<void> {
  baixar(nome, JSON.stringify(await exportarBackup(), null, 1));
}

/**
 * Backup e restauração: salva no aparelho uma cópia de tudo que está
 * cadastrado e restaura de um backup .json (sem nunca apagar nada).
 */
export function BackupPainel() {
  const { abrir, recarregar } = useSheet();
  const { showToast } = useToast();
  const { dados, erro } = useDados(resumoBackup);
  const [ocupado, setOcupado] = useState(false);
  const arquivo = useRef<HTMLInputElement>(null);

  async function salvarJson() {
    setOcupado(true);
    try {
      await salvarCopia(`anne-gestao-backup-${hoje()}.json`);
      await registrarBackup();
      showToast("Backup salvo");
      recarregar();
    } catch (e) {
      showToast(msgErro(e, "Não foi possível salvar o arquivo agora."), "error");
    } finally {
      setOcupado(false);
    }
  }

  function escolher(ev: React.ChangeEvent<HTMLInputElement>) {
    const f = ev.target.files?.[0];
    ev.target.value = "";
    if (!f) return;
    if (f.size > TAM_MAX) return showToast("Arquivo grande demais para um backup do sistema.", "error");
    const r = new FileReader();
    r.onerror = () => showToast("Não consegui ler o arquivo.", "error");
    r.onload = async () => {
      let j: BackupArquivoDTO;
      try {
        j = JSON.parse(String(r.result || ""));
      } catch {
        return showToast("O arquivo não é um backup válido (não é JSON).", "error");
      }
      try {
        const an = await analisarBackup(j);
        abrir("Restaurar backup", <RestaurarSheet nomeArquivo={f.name} arquivo={j} analise={an} />);
      } catch (e) {
        showToast(msgErro(e, "Este arquivo não é um backup do Anne Gestão."), "error");
      }
    };
    r.readAsText(f);
  }

  if (!dados) return <Carregando erro={erro} />;
  return (
    <>
      <p className="note">Salva no seu aparelho uma cópia de tudo que está cadastrado agora. Os dados continuam guardados no sistema; o backup é uma garantia a mais.</p>
      <div className="summary">
        <span>
          Último backup completo: <b>{dados.ultimo ? fdh(dados.ultimo.ultimoEm) : "nenhum registrado"}</b>
          {dados.ultimo ? ` · ${dados.ultimo.total} registros` : ""}
        </span>
      </div>
      <div className="box">
        {dados.contagens.map((c) => (
          <div key={c.colecao} className="row static">
            <span className="grow">{c.rotulo}</span>
            <b className="num">{c.n}</b>
          </div>
        ))}
      </div>
      <div className="actions">
        <a className="btn" href={URL_PLANILHA_BACKUP} download>
          Planilha para consultar (.xlsx)
        </a>
        <button type="button" className="btn primary" onClick={salvarJson} disabled={ocupado}>
          Backup completo (.json)
        </button>
      </div>
      <p className="note">O .json é a cópia exata, que permite restaurar. A planilha é para ler e conferir.</p>
      <div className="sec">
        <h3>Restaurar</h3>
        <p className="note">Escolha um backup .json. Antes de gravar, o sistema mostra o que vai mudar e salva uma cópia do estado atual. Nada é apagado.</p>
        <div>
          <input ref={arquivo} type="file" accept=".json,application/json" hidden onChange={escolher} />
          <button type="button" className="btn" onClick={() => arquivo.current?.click()}>
            Restaurar de um backup…
          </button>
        </div>
      </div>
    </>
  );
}

type Fase = "revisar" | "gravando" | "feito";

function RestaurarSheet({ nomeArquivo, arquivo, analise }: { nomeArquivo: string; arquivo: BackupArquivoDTO; analise: AnaliseBackupResponseDTO }) {
  const { fechar, recarregar } = useSheet();
  const [modo, setModo] = useState<ModoRestauracao>("faltantes");
  const [fase, setFase] = useState<Fase>("revisar");
  const [salvando, setSalvando] = useState(false);
  const [semCopia, setSemCopia] = useState(false);
  const [res, setRes] = useState<RestaurarBackupResponseDTO | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  const nNov = analise.linhas.reduce((a, l) => a + l.novos, 0);
  const nDif = analise.linhas.reduce((a, l) => a + l.diferentes, 0);
  const tot = modo === "todos" ? nNov + nDif : nNov;

  async function gravar() {
    setFase("gravando");
    setErro(null);
    try {
      setRes(await restaurarBackup(nomeArquivo, arquivo, modo));
      setFase("feito");
      recarregar();
    } catch (e) {
      setErro(msgErro(e, "Não foi possível restaurar agora."));
      setFase("revisar");
    }
  }

  async function iniciar() {
    setSalvando(true);
    try {
      await salvarCopia(`anne-gestao-antes-da-restauracao-${hoje()}.json`);
    } catch {
      setSemCopia(true);
      setSalvando(false);
      return;
    }
    setSalvando(false);
    await gravar();
  }

  /** Relê o sistema: o que ainda falta (ou difere) do que foi pedido não está conferido. */
  async function conferir() {
    if (!res) return;
    try {
      const an = await analisarBackup(arquivo);
      const pend = an.linhas.reduce((a, l) => a + l.novos + (modo === "todos" ? l.diferentes : 0), 0);
      setRes({ ...res, conferidos: Math.max(0, res.total - pend) });
    } catch (e) {
      setErro(msgErro(e, "Não foi possível conferir agora."));
    }
  }

  return (
    <>
      <p className="note">
        Arquivo <b>{nomeArquivo}</b>
        {analise.exportadoEm ? ` · gerado em ${fdh(analise.exportadoEm)}` : ""}.
      </p>
      <div className="box list">
        {analise.linhas.map((l) => (
          <div key={l.colecao} className="row static">
            <span className="grow">
              <span className="t">{l.rotulo}</span>
              <span className="m">
                {l.total} no backup · {l.iguais} iguais
                {l.diferentes ? ` · ${plural(l.diferentes, "diferente", "diferentes")}` : ""}
                {l.soNoSistema ? ` · ${l.soNoSistema} só no sistema` : ""}
              </span>
            </span>
            {l.novos ? <span className="pill good">faltam {l.novos}</span> : <span className="pill">completo</span>}
          </div>
        ))}
      </div>
      <p className="note">
        &quot;Só no sistema&quot; são registros criados depois do backup: <b>a restauração não apaga nenhum deles</b>.
        {analise.ignoradas.length ? ` Ignorados: ${analise.ignoradas.join(", ")} (a Cozinha é recalculada sozinha).` : ""}
        {analise.invalidos ? ` ${analise.invalidos} registro(s) inválido(s) no arquivo serão ignorados.` : ""}
      </p>
      {erro && (
        <p className="erro-form" role="alert">
          {erro}
        </p>
      )}
      {fase === "revisar" && (
        <>
          <div className="sec">
            <h3>O que restaurar</h3>
            <label className="check">
              <input type="radio" name="rmodo" checked={modo === "faltantes"} onChange={() => setModo("faltantes")} /> Só o que está faltando ({nNov})
            </label>
            <label className="check">
              <input type="radio" name="rmodo" checked={modo === "todos"} onChange={() => setModo("todos")} /> O que falta e também voltar os diferentes à versão do backup ({nNov + nDif})
            </label>
            {modo === "todos" && nDif > 0 && <p className="warn-t">Atenção: as alterações feitas depois do backup {nDif > 1 ? `nesses ${nDif} registros serão desfeitas` : "neste registro serão desfeitas"}.</p>}
          </div>
          <p className="note">Antes de gravar, o sistema salva no seu aparelho uma cópia do estado atual, para você poder voltar atrás.</p>
          <div className="actions">
            <button type="button" className="btn" onClick={() => fechar()}>
              Cancelar
            </button>
            <button type="button" className="btn primary" disabled={!tot || salvando} onClick={iniciar}>
              {salvando ? "Salvando cópia do estado atual…" : tot ? `Restaurar ${plural(tot, "registro", "registros")}` : "Nada a restaurar"}
            </button>
          </div>
          {semCopia && (
            <div className="confirm">
              <p>A cópia do estado atual não foi salva. Quer restaurar mesmo assim?</p>
              <button type="button" className="btn sm" onClick={() => fechar()}>
                Cancelar
              </button>
              <button type="button" className="btn sm danger solid" onClick={gravar}>
                Restaurar sem cópia
              </button>
            </div>
          )}
        </>
      )}
      {fase === "gravando" && (
        <div className="summary">
          <b>Gravando… {tot} registros</b>
          <span className="note">Não feche esta página até terminar.</span>
        </div>
      )}
      {fase === "feito" && res && (
        <>
          <div className="summary">
            <b>{res.falhas.length ? `Restauração terminou com ${plural(res.falhas.length, "falha", "falhas")}` : "Restauração concluída"}</b>
            <span>
              {res.gravados} de {res.total} registros gravados.
            </span>
            <span className={res.conferidos === res.total ? "" : "warn-t"}>
              Conferência: {res.conferidos} de {res.total} registros conferidos no sistema, iguais ao backup.
            </span>
            {res.falhas.length > 0 && (
              <span className="neg">
                Falharam:{" "}
                {res.falhas
                  .slice(0, 8)
                  .map((f) => `${f.rotulo} ${f.id}`)
                  .join(", ")}
                {res.falhas.length > 8 ? "…" : ""}
              </span>
            )}
          </div>
          <div className="actions">
            <button type="button" className="btn" onClick={conferir}>
              Conferir de novo
            </button>
            <button type="button" className="btn primary" onClick={() => fechar(true)}>
              Fechar
            </button>
          </div>
        </>
      )}
    </>
  );
}

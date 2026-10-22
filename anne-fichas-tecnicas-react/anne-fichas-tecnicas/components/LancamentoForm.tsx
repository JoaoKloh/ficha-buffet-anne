"use client";

import { useState } from "react";
import type { ProducaoResponseDTO } from "@/lib/models/producao";
import type { Lancamento, NovoLancamento } from "@/lib/models/financeiro";
import { LancamentoCampos, camposVazios, mensagemDeErro, validarCampos } from "./LancamentoCampos";
import { LancamentoRow } from "./LancamentoList";

interface Props {
  /** Deve lançar em caso de erro, para a mensagem do backend aparecer no formulário. */
  onSave: (input: NovoLancamento) => Promise<void>;
  producoes: ProducaoResponseDTO[];
  /** Últimos lançados (mais recentes primeiro), mostrados abaixo do botão. */
  recentes: Lancamento[];
  onEdit: (l: Lancamento) => void;
  onDelete: (l: Lancamento) => void;
}

export function LancamentoForm({ onSave, producoes, recentes, onEdit, onDelete }: Props) {
  const [campos, setCampos] = useState(camposVazios);
  const [erros, setErros] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const nomesProducoes = new Map(producoes.map((p) => [p.id, p.nome]));

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const resultado = validarCampos(campos);
    if (!resultado.ok) {
      setErros(resultado.erros);
      return;
    }
    setErros([]);
    setSaving(true);
    try {
      await onSave(resultado.data);
      // Mantém tipo, categoria, datas e produções: é comum lançar vários itens parecidos em sequência.
      setCampos((prev) => ({ ...prev, descricao: "", valor: "" }));
    } catch (err) {
      // Em erro, os campos preenchidos são mantidos para o usuário corrigir.
      setErros([mensagemDeErro(err, "Não foi possível salvar o lançamento.")]);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="fin-panel fin-lancar" onSubmit={handleSubmit} noValidate>
      <LancamentoCampos
        idPrefix="fin-novo"
        value={campos}
        onChange={setCampos}
        producoes={producoes}
      />

      {erros.length > 0 && (
        <div className="error-text" role="alert">
          {erros.map((err, i) => (
            <div key={i}>{err}</div>
          ))}
        </div>
      )}

      <button type="submit" className="btn fin-submit" disabled={saving}>
        {saving ? "Salvando..." : "Lançar"}
      </button>

      {recentes.length > 0 && (
        <div className="fin-recentes">
          <div className="fin-label">Últimos lançados</div>
          <ul className="fin-list">
            {recentes.map((l) => (
              <LancamentoRow
                key={l.id}
                lancamento={l}
                nomesProducoes={nomesProducoes}
                onEdit={onEdit}
                onDelete={onDelete}
              />
            ))}
          </ul>
        </div>
      )}
    </form>
  );
}

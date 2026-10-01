"use client";

import { useState } from "react";
import type { NovoLancamento } from "@/lib/models/financeiro";
import { LancamentoCampos, camposVazios, mensagemDeErro, validarCampos } from "./LancamentoCampos";

interface Props {
  /** Deve lançar em caso de erro, para a mensagem do backend aparecer no formulário. */
  onSave: (input: NovoLancamento) => Promise<void>;
}

export function LancamentoForm({ onSave }: Props) {
  const [campos, setCampos] = useState(camposVazios);
  const [erros, setErros] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

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
      // Mantém tipo, categoria e data: é comum lançar vários itens parecidos em sequência.
      setCampos((prev) => ({ ...prev, descricao: "", valor: "" }));
    } catch (err) {
      // Em erro, os campos preenchidos são mantidos para o usuário corrigir.
      setErros([mensagemDeErro(err, "Não foi possível salvar o lançamento.")]);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="fin-panel" onSubmit={handleSubmit} noValidate>
      <h2 className="section-h">Novo lançamento</h2>
      <LancamentoCampos idPrefix="fin-novo" value={campos} onChange={setCampos} />

      {erros.length > 0 && (
        <div className="error-text" role="alert">
          {erros.map((err, i) => (
            <div key={i}>{err}</div>
          ))}
        </div>
      )}

      <button type="submit" className="btn" disabled={saving}>
        {saving ? "Salvando..." : "+ Adicionar lançamento"}
      </button>
    </form>
  );
}

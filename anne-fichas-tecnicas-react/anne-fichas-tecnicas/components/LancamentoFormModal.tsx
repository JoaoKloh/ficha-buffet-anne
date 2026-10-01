"use client";

import { useState } from "react";
import { Modal } from "./ui/Modal";
import type { Lancamento, NovoLancamento } from "@/lib/models/financeiro";
import { LancamentoCampos, camposDe, mensagemDeErro, validarCampos } from "./LancamentoCampos";

interface Props {
  initial: Lancamento;
  onClose: () => void;
  /** Deve lançar em caso de erro, para a mensagem do backend aparecer no modal. */
  onSave: (input: NovoLancamento) => Promise<void>;
}

export function LancamentoFormModal({ initial, onClose, onSave }: Props) {
  const [campos, setCampos] = useState(() => camposDe(initial));
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
    } catch (err) {
      setErros([mensagemDeErro(err, "Não foi possível atualizar o lançamento.")]);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Editar lançamento" onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <LancamentoCampos idPrefix="fin-editar" value={campos} onChange={setCampos} />

        {erros.length > 0 && (
          <div className="error-text" role="alert">
            {erros.map((err, i) => (
              <div key={i}>{err}</div>
            ))}
          </div>
        )}

        <div className="modal-actions">
          <button type="button" className="btn ghost" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button type="submit" className="btn" disabled={saving}>
            {saving ? "Salvando..." : "Salvar lançamento"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

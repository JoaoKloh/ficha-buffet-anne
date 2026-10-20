"use client";

import { useEffect, useState } from "react";
import { Modal } from "./ui/Modal";
import { FotoUploadField } from "./FotoUploadField";
import { CATEGORIAS } from "@/lib/models/category";
import { UpdatePratoRequestSchema } from "@/lib/models/prato";
import { listarIngredientesDisponiveis } from "@/lib/api/pratoIngredientes";
import type {
  PratoDetalhadoResponseDTO,
  UpdatePratoRequest,
  IngredienteResponseDTO,
} from "@/lib/models/prato";

interface Props {
  prato: PratoDetalhadoResponseDTO;
  onClose: () => void;
  onSave: (input: UpdatePratoRequest) => Promise<void>;
}

export function PratoEditModal({ prato, onClose, onSave }: Props) {
  const [nome, setNome] = useState(prato.nome);
  const [categoria, setCategoria] = useState(prato.categoria);
  const [foto, setFoto] = useState(prato.foto ?? "");
  const [rendQtd, setRendQtd] = useState(prato.rendQtd);
  const [rendUnid, setRendUnid] = useState(prato.rendUnid);
  const [porPessoa, setPorPessoa] = useState(prato.porPessoa);
  const [receita, setReceita] = useState(prato.receita ?? "");
  const [ingredientes, setIngredientes] = useState<IngredienteResponseDTO[]>(prato.ingredientes);
  const [errors, setErrors] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [uploadingFoto, setUploadingFoto] = useState(false);

  const [catalogo, setCatalogo] = useState<IngredienteResponseDTO[]>([]);
  const [novoIngredienteId, setNovoIngredienteId] = useState("");
  const [novoQtd, setNovoQtd] = useState(0);
  const [novoUnidade, setNovoUnidade] = useState("");

  useEffect(() => {
    listarIngredientesDisponiveis()
      .then(setCatalogo)
      .catch(() => setCatalogo([]));
  }, []);

  const disponiveis = catalogo.filter((c) => !ingredientes.some((i) => i.id === c.id));

  function removeIngrediente(id: number) {
    setIngredientes((prev) => prev.filter((i) => i.id !== id));
  }

  function handleSelectNovo(id: string) {
    setNovoIngredienteId(id);
    const selecionado = catalogo.find((c) => String(c.id) === id);
    setNovoUnidade(selecionado?.unidade ?? "");
  }

  function addIngrediente() {
    const selecionado = catalogo.find((c) => String(c.id) === novoIngredienteId);
    if (!selecionado || novoQtd <= 0) return;
    setIngredientes((prev) => [
      ...prev,
      { ...selecionado, qtd: novoQtd, unidade: novoUnidade.trim() || selecionado.unidade },
    ]);
    setNovoIngredienteId("");
    setNovoQtd(0);
    setNovoUnidade("");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const payload: UpdatePratoRequest = {
      id: prato.id,
      nome,
      categoria,
      foto: foto.trim() || null,
      rendQtd,
      rendUnid,
      porPessoa,
      receita,
      // i.qtd vem sempre preenchido aqui: estes itens vêm de
      // PratoDetalhadoResponseDTO.ingredientes (ficha técnica, construída a
      // partir de PratoIngredienteEntity), diferente do catálogo global onde
      // qtd é null. UpdatePratoRequest exige qtd por item (@NotNull @Positive).
      ingredientes: ingredientes.map((i) => ({
        ingredienteId: i.id,
        qtd: i.qtd ?? 0,
        unidade: i.unidade,
      })) as UpdatePratoRequest["ingredientes"],
    };
    const parsed = UpdatePratoRequestSchema.safeParse(payload);
    if (!parsed.success) {
      setErrors(parsed.error.issues.map((i) => i.message));
      return;
    }
    setErrors([]);
    setSaving(true);
    try {
      await onSave(parsed.data);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={`Editar prato #${prato.id}`} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <div className="row2">
          <div className="field-block">
            <label htmlFor="p_nome">Nome do prato</label>
            <input id="p_nome" type="text" value={nome} onChange={(e) => setNome(e.target.value)} />
          </div>
          <div className="field-block">
            <label htmlFor="p_categoria">Categoria</label>
            <select id="p_categoria" value={categoria} onChange={(e) => setCategoria(e.target.value)}>
              {CATEGORIAS.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
        </div>

        <FotoUploadField id="p_foto" value={foto} onChange={setFoto} onUploadingChange={setUploadingFoto} />

        <div className="row2">
          <div className="field-block">
            <label htmlFor="p_rendQtd">Rendimento da receita (qtd.)</label>
            <input
              id="p_rendQtd"
              type="number"
              min={1}
              step="1"
              value={rendQtd}
              onChange={(e) => setRendQtd(Number(e.target.value) || 1)}
            />
          </div>
          <div className="field-block">
            <label htmlFor="p_rendUnid">Unidade do rendimento</label>
            <input
              id="p_rendUnid"
              type="text"
              value={rendUnid}
              onChange={(e) => setRendUnid(e.target.value)}
              placeholder="Ex: unidades, g, bandejas"
            />
          </div>
        </div>

        <div className="field-block">
          <label htmlFor="p_porPessoa">Sugestão por pessoa</label>
          <input
            id="p_porPessoa"
            type="number"
            min={0}
            step="1"
            value={porPessoa}
            onChange={(e) => setPorPessoa(Number(e.target.value) || 0)}
          />
        </div>

        <div className="field-block">
          <label htmlFor="p_receita">Modo de preparo</label>
          <textarea
            id="p_receita"
            value={receita}
            onChange={(e) => setReceita(e.target.value)}
            placeholder="Passo a passo da receita..."
          />
        </div>

        <div className="section-label">Ingredientes vinculados</div>
        {ingredientes.length === 0 ? (
          <p className="dish-recipe-preview">Nenhum ingrediente vinculado.</p>
        ) : (
          <div className="assoc-list" style={{ marginBottom: 10 }}>
            {ingredientes.map((i) => (
              <div
                key={i.id}
                className="assoc-row"
                style={{ cursor: "default", justifyContent: "space-between" }}
              >
                <span>
                  <span className="assoc-name">{i.nome}</span>
                  {/* Fornecedor é opcional no cadastro — mostramos o rótulo
                      sem valor em vez de esconder a informação quando ausente. */}
                  <span className="assoc-meta">
                    {i.qtd != null ? `${i.qtd} ` : ""}
                    {i.unidade} · Fornecedor: {i.fornecedor ?? ""}
                  </span>
                </span>
                <button
                  type="button"
                  className="row-remove"
                  title="Remover do prato"
                  onClick={() => removeIngrediente(i.id)}
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        )}
        <div className="ing-row">
          <select value={novoIngredienteId} onChange={(e) => handleSelectNovo(e.target.value)}>
            <option value="">Selecione um ingrediente</option>
            {disponiveis.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome}
              </option>
            ))}
          </select>
          <input
            type="number"
            min={0}
            step="0.01"
            placeholder="Qtd"
            value={novoQtd || ""}
            onChange={(e) => setNovoQtd(Number(e.target.value) || 0)}
          />
          <input
            type="text"
            placeholder="Unidade"
            value={novoUnidade}
            onChange={(e) => setNovoUnidade(e.target.value)}
          />
          <button
            type="button"
            className="row-remove"
            title="Adicionar ao prato"
            onClick={addIngrediente}
            disabled={!novoIngredienteId || novoQtd <= 0}
          >
            +
          </button>
        </div>
        {disponiveis.length === 0 && catalogo.length > 0 && (
          <p className="dish-recipe-preview" style={{ marginTop: 4, marginBottom: 16 }}>
            Todos os ingredientes cadastrados já estão vinculados a este prato.
          </p>
        )}

        {errors.length > 0 && (
          <div className="error-text">
            {errors.map((err, i) => (
              <div key={i}>{err}</div>
            ))}
          </div>
        )}

        <div className="modal-actions">
          <button type="button" className="btn ghost" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button type="submit" className="btn" disabled={saving || uploadingFoto}>
            {saving ? "Salvando..." : uploadingFoto ? "Enviando foto..." : "Salvar prato"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

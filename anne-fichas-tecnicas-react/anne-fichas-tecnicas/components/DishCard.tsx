"use client";

import type { Dish } from "@/lib/models/dish";
import { fmtQuantidade } from "@/lib/utils/format";

interface Props {
  dish: Dish;
  onEdit: (dish: Dish) => void;
  onDelete: (dish: Dish) => void;
}

export function DishCard({ dish, onEdit, onDelete }: Props) {
  return (
    <div className="dish-card">
      {dish.foto ? (
        // Imagem enviada pelo usuário: buscada diretamente pelo browser (não pelo
        // servidor), evitando expor o backend a SSRF via proxy de otimização de imagem.
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={dish.foto}
          alt={dish.nome}
          className="dish-photo"
          style={{ objectFit: "cover" }}
          loading="lazy"
          referrerPolicy="no-referrer"
        />
      ) : (
        <div className="dish-photo empty">Sem foto</div>
      )}
      <div className="dish-body">
        <div className="dish-cat">{dish.categoria}</div>
        <div className="dish-name">{dish.nome}</div>
        <div className="dish-yield">
          Rende {fmtQuantidade(dish.rendQtd)} {dish.rendUnid}
          {dish.porPessoa ? ` · ${fmtQuantidade(dish.porPessoa)} por pessoa` : ""}
        </div>
        <div className="dish-recipe-preview">{dish.receita || "Sem receita cadastrada."}</div>
        <div className="dish-actions">
          <button type="button" className="btn small ghost" onClick={() => onEdit(dish)}>
            Editar
          </button>
          <button type="button" className="btn small danger" onClick={() => onDelete(dish)}>
            Excluir
          </button>
        </div>
      </div>
    </div>
  );
}

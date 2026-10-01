"use client";

import type { PratoDetalhadoResponseDTO } from "@/lib/models/prato";
import { fmtQuantidade } from "@/lib/utils/format";
import { CardMenu } from "./ui/CardMenu";

interface Props {
  dish: PratoDetalhadoResponseDTO;
  onEdit: (dish: PratoDetalhadoResponseDTO) => void;
  onDelete: (dish: PratoDetalhadoResponseDTO) => void;
  onAssociateProduction: (dish: PratoDetalhadoResponseDTO) => void;
}

export function DishCard({ dish, onEdit, onDelete, onAssociateProduction }: Props) {
  return (
    <div className="dish-card">
      <div className="dish-media">
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
        <CardMenu
          ariaLabel={`Mais opções para ${dish.nome}`}
          items={[
            { label: "Associar a Produção", onClick: () => onAssociateProduction(dish) },
          ]}
        />
      </div>
      <div className="dish-body">
        <div className="dish-cat">{dish.categoria}</div>
        <div className="dish-name">{dish.nome}</div>
        <div className="dish-yield">
          Rende {fmtQuantidade(dish.rendQtd)} {dish.rendUnid}
          {dish.porPessoa ? ` · ${fmtQuantidade(dish.porPessoa)} por pessoa` : ""}
        </div>
        <div className="dish-recipe-preview">{dish.receita || "Sem receita cadastrada."}</div>
        <div className="dish-actions">
          <button
            type="button"
            className="btn small ghost icon-btn"
            onClick={() => onEdit(dish)}
            aria-label={`Editar ${dish.nome}`}
            title="Editar prato"
          >
            <svg
              width="13"
              height="13"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z" />
            </svg>
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

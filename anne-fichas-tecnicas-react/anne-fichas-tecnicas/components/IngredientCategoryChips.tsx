"use client";

import { CATEGORIAS_INGREDIENTE } from "@/lib/models/ingredient";

interface Props {
  active: string;
  onChange: (value: string) => void;
}

export function IngredientCategoryChips({ active, onChange }: Props) {
  const options: string[] = ["Todos", ...CATEGORIAS_INGREDIENTE];
  return (
    <div className="chips">
      {options.map((c) => (
        <button
          key={c}
          type="button"
          className={`chip ${c === active ? "active" : ""}`}
          onClick={() => onChange(c)}
        >
          {c}
        </button>
      ))}
    </div>
  );
}

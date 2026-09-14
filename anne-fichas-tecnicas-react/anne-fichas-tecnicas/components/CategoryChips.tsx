"use client";

import { CATEGORIAS, type Categoria } from "@/lib/models/category";

interface Props {
  active: Categoria | "Todos";
  onChange: (value: Categoria | "Todos") => void;
}

export function CategoryChips({ active, onChange }: Props) {
  const options: (Categoria | "Todos")[] = ["Todos", ...CATEGORIAS];
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

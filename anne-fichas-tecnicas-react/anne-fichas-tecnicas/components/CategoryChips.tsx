"use client";

import { CATEGORIAS } from "@/lib/models/category";

interface Props {
  active: string;
  onChange: (value: string) => void;
}

export function CategoryChips({ active, onChange }: Props) {
  const options: string[] = ["Todos", ...CATEGORIAS];
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

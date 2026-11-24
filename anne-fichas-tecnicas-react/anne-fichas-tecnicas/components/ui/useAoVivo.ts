"use client";

import { useEffect, useState } from "react";

/**
 * Resultado calculado no BFF para um rascunho de formulário (custo da ficha,
 * CMV do prato). Espera a digitação parar antes de pedir.
 */
export function useAoVivo<T>(calcular: () => Promise<T>, chave: string, espera = 350): T | null {
  const [r, setR] = useState<{ chave: string; valor: T } | null>(null);
  useEffect(() => {
    let vivo = true;
    const t = setTimeout(() => {
      calcular()
        .then((v) => vivo && setR({ chave, valor: v }))
        .catch(() => {
          // resumo é informativo: falha de rede não bloqueia o formulário
        });
    }, espera);
    return () => {
      vivo = false;
      clearTimeout(t);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chave]);
  return r?.valor ?? null;
}

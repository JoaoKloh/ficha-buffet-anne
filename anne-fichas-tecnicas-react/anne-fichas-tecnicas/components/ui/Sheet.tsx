"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";

/**
 * Folha lateral do protótipo: painéis de detalhe e formulários que sobem por
 * cima da tela, empilhados (Voltar volta à folha anterior com o estado dela).
 *
 * Também guarda a "versão" dos dados: toda gravação chama recarregar(), e as
 * telas e folhas que leem do BFF refazem a leitura.
 */

interface SheetSpec {
  id: number;
  titulo: string;
  larga?: boolean;
  conteudo: React.ReactNode;
}

interface SheetCtx {
  abrir: (titulo: string, conteudo: React.ReactNode, opcoes?: { larga?: boolean }) => void;
  fechar: (todas?: boolean) => void;
  renomear: (titulo: string) => void;
  versao: number;
  recarregar: () => void;
}

const Ctx = createContext<SheetCtx | null>(null);

export function SheetProvider({ children }: { children: React.ReactNode }) {
  const [pilha, setPilha] = useState<SheetSpec[]>([]);
  const [versao, setVersao] = useState(0);
  const seq = useRef(0);

  const abrir = useCallback((titulo: string, conteudo: React.ReactNode, opcoes?: { larga?: boolean }) => {
    seq.current += 1;
    const id = seq.current;
    setPilha((p) => [...p, { id, titulo, conteudo, larga: opcoes?.larga }]);
  }, []);
  const fechar = useCallback((todas?: boolean) => setPilha((p) => (todas ? [] : p.slice(0, -1))), []);
  const renomear = useCallback((titulo: string) => setPilha((p) => (p.length ? [...p.slice(0, -1), { ...p[p.length - 1]!, titulo }] : p)), []);
  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  useEffect(() => {
    if (!pilha.length) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") fechar();
    };
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [pilha.length, fechar]);

  const valor = useMemo(() => ({ abrir, fechar, renomear, versao, recarregar }), [abrir, fechar, renomear, versao, recarregar]);
  const topo = pilha[pilha.length - 1];

  return (
    <Ctx.Provider value={valor}>
      {children}
      {topo && <div className="scrim" onClick={() => fechar(true)} />}
      {pilha.map((s, i) => {
        const ativa = i === pilha.length - 1;
        return (
          <div key={s.id} className={`sheet${s.larga ? " larga" : ""}`} role="dialog" aria-modal="true" aria-label={s.titulo} hidden={!ativa}>
            <div className="sheet-h">
              {pilha.length > 1 && (
                <button type="button" className="btn sm ghost" onClick={() => fechar()} aria-label="Voltar">
                  ‹ Voltar
                </button>
              )}
              <span className="ttl">{s.titulo}</span>
              <button type="button" className="btn sm ghost" onClick={() => fechar(true)} aria-label="Fechar">
                Fechar
              </button>
            </div>
            <div className="sheet-b">{s.conteudo}</div>
          </div>
        );
      })}
    </Ctx.Provider>
  );
}

export function useSheet(): SheetCtx {
  const c = useContext(Ctx);
  if (!c) throw new Error("useSheet deve ser usado dentro de <SheetProvider>");
  return c;
}

/**
 * Carrega um recurso do BFF e recarrega sempre que os dados mudam
 * (recarregar() após uma gravação) ou quando as dependências mudam.
 */
export function useDados<T>(carregar: () => Promise<T>, deps: React.DependencyList = []): { dados: T | null; erro: string | null; carregando: boolean } {
  const { versao } = useSheet();
  const chave = JSON.stringify([versao, ...deps]);
  const [estado, setEstado] = useState<{ dados: T | null; erro: string | null; chave: string | null }>({ dados: null, erro: null, chave: null });
  useEffect(() => {
    let vivo = true;
    carregar()
      .then((d) => vivo && setEstado({ dados: d, erro: null, chave }))
      .catch((e: unknown) => vivo && setEstado((s) => ({ dados: s.dados, erro: e instanceof Error ? e.message : "Não foi possível carregar.", chave })));
    return () => {
      vivo = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chave]);
  return { dados: estado.dados, erro: estado.erro, carregando: estado.chave !== chave };
}

"use client";

import { useRef, useState } from "react";
import { upload } from "@vercel/blob/client";
import { FOTO_TIPOS_ACEITOS, validarFoto } from "@/lib/models/prato";

interface Props {
  id: string;
  /** URL atual da foto ("" quando não há foto). */
  value: string;
  onChange: (url: string) => void;
  /** Avisa o formulário para travar o "Salvar" enquanto o envio não termina. */
  onUploadingChange: (uploading: boolean) => void;
}

/**
 * Campo de foto do prato: valida o arquivo, envia direto do browser para o
 * Vercel Blob (token gerado em app/api/prato/upload/route.ts) e devolve a URL
 * pública via onChange — é essa URL que o formulário manda no campo `foto`.
 */
export function FotoUploadField({ id, value, onChange, onUploadingChange }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [nomeArquivo, setNomeArquivo] = useState("");
  const [erro, setErro] = useState("");

  async function handleFile(file: File | undefined) {
    if (!file) return;
    // Limpa o input para que escolher o mesmo arquivo de novo dispare onChange.
    if (inputRef.current) inputRef.current.value = "";

    const invalido = validarFoto(file);
    if (invalido) {
      setErro(invalido);
      return;
    }

    setErro("");
    setNomeArquivo(file.name);
    setUploading(true);
    onUploadingChange(true);
    try {
      const blob = await upload(`pratos/${file.name}`, file, {
        access: "public",
        handleUploadUrl: "/api/prato/upload",
        contentType: file.type,
      });
      onChange(blob.url);
    } catch (err) {
      setNomeArquivo("");
      setErro(
        err instanceof Error && err.message
          ? `Não foi possível enviar a imagem: ${err.message}`
          : "Não foi possível enviar a imagem."
      );
    } finally {
      setUploading(false);
      onUploadingChange(false);
    }
  }

  function remover() {
    setNomeArquivo("");
    setErro("");
    onChange("");
  }

  return (
    <div className="field-block">
      <label htmlFor={id}>Foto do prato (opcional, até 5 MB)</label>
      {value && (
        // Mesma forma de exibir que o DishCard: img direto do browser, sem proxy.
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={value}
          alt="Foto atual do prato"
          className="dish-photo"
          style={{ objectFit: "cover", borderRadius: 4, marginBottom: 8 }}
          referrerPolicy="no-referrer"
        />
      )}
      <input
        ref={inputRef}
        id={id}
        type="file"
        accept={FOTO_TIPOS_ACEITOS.join(",")}
        disabled={uploading}
        onChange={(e) => handleFile(e.target.files?.[0])}
      />
      {(uploading || nomeArquivo || value) && (
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginTop: 6 }}>
          <span className="dish-recipe-preview" style={{ margin: 0 }}>
            {uploading ? `Enviando ${nomeArquivo}...` : nomeArquivo || "Foto atual"}
          </span>
          {value && !uploading && (
            <button type="button" className="btn ghost small" onClick={remover}>
              Remover foto
            </button>
          )}
        </div>
      )}
      {erro && <div className="error-text">{erro}</div>}
    </div>
  );
}

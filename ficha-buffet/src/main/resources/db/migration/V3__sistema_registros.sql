-- Registros do próprio sistema: quando foi o último backup completo e a última restauração.
CREATE TABLE sistema_registros (
    chave VARCHAR(40) PRIMARY KEY,
    dados JSONB NOT NULL DEFAULT '{}',
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

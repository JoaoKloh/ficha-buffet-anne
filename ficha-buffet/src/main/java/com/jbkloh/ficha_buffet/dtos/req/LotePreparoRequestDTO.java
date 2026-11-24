package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Lote de pré-preparo. Na criação vale o grupo AoCriar; na atualização, sem
 * preparacaoId só mudam validade, responsável e observações (lote com uso).
 */
public record LotePreparoRequestDTO(
    @NotNull(groups = AoCriar.class, message = "Escolha a preparação.")
    @Positive(message = "Escolha a preparação.")
    Long preparacaoId,
    @NotNull(groups = AoCriar.class, message = "Informe a quantidade produzida.")
    @Positive(message = "Informe a quantidade produzida.")
    Double qtd,
    @NotBlank(groups = AoCriar.class, message = "Informe a data de produção.")
    @Pattern(regexp = Dominio.RE_DATA, message = "Informe a data de produção.")
    String produzidoEm,
    // Vazia ou nula: sem validade.
    @Pattern(regexp = Dominio.RE_DATA_OU_VAZIA, message = "Validade inválida.")
    String validade,
    @Pattern(regexp = Dominio.RE_DESTINO_LOTE, message = "Destino do lote inválido.")
    String destino,
    @Positive(message = "Evento inválido.")
    Long eventoId,
    @Size(max = 100, message = "O responsável deve ter no máximo 100 caracteres.")
    String responsavel,
    @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
    String obs
) {
    /** Grupo de validação da criação do lote. */
    public interface AoCriar {}
}

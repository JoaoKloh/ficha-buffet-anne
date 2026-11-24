package com.jbkloh.ficha_buffet.dtos.req;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import com.jbkloh.ficha_buffet.model.PreparacaoEntity;

public record UpdatePratoRequest(

    @NotNull(message = "O id do prato precisa ser informado.")
    Long id,

    @NotBlank(message = "O nome do prato é obrigatório.")
    @Size(max = 255, message = "O nome do prato deve ter no máximo 255 caracteres.")
    String nome,

    @NotBlank(message = "A categoria é obrigatória.")
    @Pattern(regexp = "canape|volante|entrada|ilha|principal|acompanhamento|sobremesa|bebida|base", message = "Categoria de ficha inválida.")
    String categoria,

    @URL(message = "A foto deve ser uma URL válida.")
    String foto,

    @NotNull(message = "O rendimento da receita (qtd) é obrigatório.")
    @Positive(message = "O rendimento da receita deve ser maior que zero.")
    Double rendQtd,

    @NotBlank(message = "A unidade do rendimento é obrigatória.")
    @Pattern(regexp = "porções|un|kg|l", message = "A unidade do rendimento deve ser porções, un, kg ou l.")
    String rendUnid,

    @Min(value = 0, message = "A sugestão por pessoa não pode ser negativa.")
    Double porPessoa,

    @Size(max = 4000, message = "A receita/modo de preparo deve ter no máximo 4000 caracteres.")
    String receita,

    @NotEmpty(message = "O prato deve conter pelo menos um ingrediente.")
    List<@Valid CreatePratoItemIngredienteDTO> ingredientes

) {

    public static void atualizarEntidade(UpdatePratoRequest req, PreparacaoEntity prato) {
        prato.setNome(req.nome());
        prato.setCategoria(req.categoria());
        prato.setUrlImagem(req.foto());
        prato.setRendimento(req.rendQtd());
        prato.setUnidadeRendimento(req.rendUnid());
        prato.setPorcaoPadrao(req.porPessoa() != null ? req.porPessoa() : 1.0);
        prato.setModoPreparo(req.receita() != null ? req.receita() : "");
        prato.setAtualizadoEm(LocalDate.now());
    }
}
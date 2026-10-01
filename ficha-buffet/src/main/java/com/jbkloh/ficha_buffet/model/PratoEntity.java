package com.jbkloh.ficha_buffet.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "pratos")
@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor
public class PratoEntity {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "rendimento_da_receita")
    private Integer rendimentoReceita;

    @Column(name = "unidade")
    private String unidade;

    @Column(name = "descricao", length = 512)
    private String descricao;

    @Column(name = "rendimento_por_pessoa")
    private Double rendimentoPessoa;

    @Column(name = "img_url")
    private String urlImagem;

    // cascade/orphanRemoval alcançam só a linha de prato_ingrediente (a
    // associação), nunca a IngredientesEntity referenciada por ela — apagar
    // um prato não pode apagar ingrediente do catálogo global.
    @OneToMany(mappedBy = "prato", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PratoIngredienteEntity> itensIngredientes = new ArrayList<>();

    public void addIngrediente(IngredientesEntity ingrediente, Double quantidade) {
        PratoIngredienteEntity item = new PratoIngredienteEntity();
        item.setPrato(this);
        item.setIngrediente(ingrediente);
        item.setQuantidade(quantidade);
        this.itensIngredientes.add(item);
    }
} 
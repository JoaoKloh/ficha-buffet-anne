package com.jbkloh.ficha_buffet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "ingredientes")
@Setter 
@Getter 
@AllArgsConstructor 
@NoArgsConstructor 
public class IngredientesEntity {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (name = "categoria")
    private String categoria;

    @Column (name = "nome")
    private String nome;

    @Column (name = "unidade")
    private String unidade;

    @Column (name = "custo_por_unidade")
    private Double custo;

    @Column (name = "fornecedor")
    private String fornecedor;
}

package com.jbkloh.ficha_buffet.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Getter 
@Setter 
@Table(name = "producao")
@AllArgsConstructor 
@NoArgsConstructor
public class ProducaoEntity {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "quantidade_convidados")
    private Integer quantidade;

    @Column(name = "data_evento")
    private LocalDate data;

    // Sem CascadeType.REMOVE de propósito: apagar uma produção deve limpar só
    // as linhas de producao_prato (a associação), nunca os PratoEntity
    // vinculados — os pratos continuam existindo no catálogo.
    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
        name = "producao_prato",
        joinColumns = @JoinColumn(name = "producao_id"),
        inverseJoinColumns = @JoinColumn(name = "prato_id")
    )
    private List<PratoEntity> pratos = new ArrayList<>();
}
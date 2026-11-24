package com.jbkloh.ficha_buffet.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro do próprio sistema, um por chave: "backup" (último backup salvo) e "restauracao" (última restauração). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "sistema_registros")
public class RegistroSistemaEntity {

    @Id
    @Column(name = "chave", length = 40)
    private String chave;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados", nullable = false)
    private Map<String, Object> dados = new HashMap<>();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    public RegistroSistemaEntity(String chave) {
        this.chave = chave;
    }
}

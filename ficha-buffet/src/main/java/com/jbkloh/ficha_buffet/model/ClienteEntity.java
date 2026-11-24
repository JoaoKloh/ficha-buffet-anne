package com.jbkloh.ficha_buffet.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cliente do buffet (quem contrata o evento). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "clientes")
public class ClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    // PF ou PJ.
    @Column(name = "tipo", nullable = false, length = 2)
    private String tipo = "PF";

    @Column(name = "documento", nullable = false, length = 30)
    private String documento = "";

    @Column(name = "telefone", nullable = false, length = 40)
    private String telefone = "";

    @Column(name = "email", nullable = false, length = 120)
    private String email = "";

    // instagram, google, whatsapp, indicacao, outro ou vazio.
    @Column(name = "origem", nullable = false, length = 20)
    private String origem = "";

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();
}

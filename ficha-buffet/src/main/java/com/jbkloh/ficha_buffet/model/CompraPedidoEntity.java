package com.jbkloh.ficha_buffet.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * "Pedido feito" de um item da sugestão de compras. O item sai da lista até entrar
 * uma entrada dele com data igual ou posterior à do pedido; desmarcar apaga o registro.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "compras_pedidos")
public class CompraPedidoEntity {

    @Id
    @Column(name = "ingrediente_id")
    private Long ingredienteId;

    @Column(name = "pedido_em", nullable = false)
    private Instant pedidoEm = Instant.now();

    @Column(name = "pedido_por", nullable = false, length = 100)
    private String pedidoPor = "";

    public CompraPedidoEntity(Long ingredienteId) {
        this.ingredienteId = ingredienteId;
    }
}

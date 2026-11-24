package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/**
 * Item da sugestão de compras: comprar = reservas dos eventos no horizonte + mínimo − saldo físico,
 * arredondado à embalagem de compra. embalagens = 0 quando o item não tem fator de compra;
 * comprar está na unidade de estoque. eventos = códigos dos eventos que pedem o item.
 * pedido = pedido feito e mercadoria ainda não entrou.
 */
public record SugestaoCompraDTO(Long insumoId, String nome, String unidade, String unidadeCompra, String fornecedor, double saldo, double reservado,
        double minimo, double falta, int embalagens, double comprar, double valor, boolean semPreco, List<String> eventos, boolean pedido,
        String pedidoEm) {}

package com.jbkloh.ficha_buffet.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Caixa;
import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Estoque;
import com.jbkloh.ficha_buffet.calculo.EstoqueIntegrado;
import com.jbkloh.ficha_buffet.calculo.Eventos;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.calculo.NotasFiscais;
import com.jbkloh.ficha_buffet.calculo.Ordens;
import com.jbkloh.ficha_buffet.dtos.req.MarcarPedidoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueComprasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueConferenciaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueLoucasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueOpcoesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoquePerdasResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueSaldoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EstoqueValidadesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemEstoqueDetalheDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SaldoItemDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.service.DadosService.Parte;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.CompraPedidoEntity;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.repositories.CompraPedidoRepository;

import lombok.RequiredArgsConstructor;

/** Telas de estoque: saldo, ficha do item, louças, opções dos formulários e o estoque integrado (compras, validades, perdas e conferência). */
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final DadosService dadosService;
    private final CompraPedidoRepository compraPedidoRepository;
    private final Cadastro cadastro;

    private static SaldoItemDTO saldoItem(Dados d, Estoque est, InsumoResponseDTO i) {
        double fisico = est.fisico(i.id());
        List<SaldoItemDTO.Reserva> reservas = est.reservasDe(i.id()).entrySet().stream().filter(r -> r.getValue() > 0).map(r -> {
            EventoResponseDTO e = d.evento(r.getKey());
            return new SaldoItemDTO.Reserva(r.getKey(), e == null ? "" : e.codigo(), e == null ? "evento" : e.nome(), Formato.r3(r.getValue()));
        }).toList();
        return new SaldoItemDTO(i, fisico, est.reservado(i.id()), est.disponivel(i.id()), fisico > 0 && i.custo() > 0 ? fisico * i.custo() : 0,
            est.situacao(i), InsumoService.emFichas(d, i.id()), reservas, Dominio.reutilizavel(i.tipo()) ? Estoque.loucaFora(d, i.id()) : 0);
    }

    private static String rotuloEvento(EventoResponseDTO e) {
        return e.codigo() + " · " + e.nome() + Formato.sufixoData(e.dataEvento());
    }

    private static Comparator<EventoResponseDTO> porData() {
        return Comparator.comparing(e -> Formato.vazio(e.dataEvento()) ? "9999" : e.dataEvento());
    }

    /**
     * Opções de um formulário do estoque. `para` diz qual: cada um recebe só os campos que usa.
     * Movimento e nota fiscal têm seletor de item (com saldo), por isso leem o retrato completo.
     */
    @Transactional(readOnly = true)
    public EstoqueOpcoesResponseDTO opcoes(String para) {
        switch (para) {
            case "movimento": {
                Dados d = dadosService.carregar();
                return new EstoqueOpcoesResponseDTO(Opcoes.insumosPorCategoria(d.insumos), itensInfo(d),
                    d.eventos.stream().sorted(Comparator.comparing(EventoResponseDTO::dataEvento).reversed())
                        .map(e -> GestaoMapper.par(e.id(), e.nome() + Formato.sufixoData(e.dataEvento()))).toList(),
                    null, null, null, null, null, null, null);
            }
            case "nf": {
                Dados d = dadosService.carregar();
                CategoriaResponseDTO ab = Caixa.categoriaAB(d);
                return new EstoqueOpcoesResponseDTO(Opcoes.insumosPorCategoria(d.insumos), itensInfo(d), null,
                    d.eventos.stream().filter(Eventos::ativo).sorted(porData()).map(e -> GestaoMapper.par(e.id(), rotuloEvento(e))).toList(),
                    null, null, null,
                    Opcoes.categoriasPorGrupo(d, List.of("custo_direto", "despesa_variavel", "despesa_fixa")),
                    ab == null ? null : ab.id(),
                    NotasFiscais.fornecedores(d));
            }
            case "transferencia": {
                Dados d = dadosService.carregar();
                CategoriaResponseDTO ab = Caixa.categoriaAB(d);
                return new EstoqueOpcoesResponseDTO(Opcoes.insumosPorCategoria(d.insumos), itensInfo(d), null, null, null, null, null,
                    Opcoes.categoriasPorGrupo(d, List.of("custo_direto", "despesa_variavel", "despesa_fixa")), ab == null ? null : ab.id(), null);
            }
            case "lote": {
                Dados d = dadosService.carregar(EnumSet.of(Parte.PREPARACOES, Parte.EVENTOS));
                String tresDiasAtras = Formato.addDays(Formato.hoje(), -3);
                Map<Long, EstoqueOpcoesResponseDTO.PreparacaoInfo> preparacoes = new LinkedHashMap<>();
                d.preparacoes.forEach(p -> preparacoes.put(p.id(), new EstoqueOpcoesResponseDTO.PreparacaoInfo(p.nome(), p.unidadeRendimento(), p.rendimento(),
                    Estoque.validadeDiasDe(p), Dominio.rotulo(Dominio.CONSERVACOES, p.conservacao(), "—"))));
                return new EstoqueOpcoesResponseDTO(null, null, null, null,
                    d.eventos.stream().filter(e -> Eventos.ativo(e) && !Formato.vazio(e.dataEvento()) && e.dataEvento().compareTo(tresDiasAtras) >= 0).sorted(porData())
                        .map(e -> GestaoMapper.par(e.id(), rotuloEvento(e))).toList(),
                    Opcoes.preparacoesPorCategoria(d, false), preparacoes, null, null, null);
            }
            default:
                throw new AppException("Formulário de opções desconhecido.", HttpStatus.BAD_REQUEST);
        }
    }

    private static Map<Long, EstoqueOpcoesResponseDTO.ItemInfo> itensInfo(Dados d) {
        Estoque est = Estoque.calcular(d);
        Map<Long, EstoqueOpcoesResponseDTO.ItemInfo> itens = new LinkedHashMap<>();
        d.insumos.forEach(i -> itens.put(i.id(), new EstoqueOpcoesResponseDTO.ItemInfo(i.nome(), i.unidade(), i.fatorCompra(), i.unidadeCompra(),
            Dominio.reutilizavel(i.tipo()), est.fisico(i.id()), i.local(), i.custo())));
        return itens;
    }

    /** Saldo físico, reservado, disponível e situação de cada item, por local e nome. */
    @Transactional(readOnly = true)
    public EstoqueSaldoResponseDTO saldo() {
        Dados d = dadosService.carregar();
        Estoque est = Estoque.calcular(d);
        List<SaldoItemDTO> itens = d.insumos.stream().map(i -> saldoItem(d, est, i))
            .sorted(Formato.<SaldoItemDTO>porNome(x -> x.insumo().local()).thenComparing(Formato.porNome(x -> x.insumo().nome()))).toList();
        List<SaldoItemDTO> consumiveis = itens.stream().filter(x -> !Dominio.reutilizavel(x.insumo().tipo())).toList();
        return new EstoqueSaldoResponseDTO(new EstoqueSaldoResponseDTO.Kpis(
            Formato.soma(consumiveis, x -> Math.max(x.fisico(), 0) * x.insumo().custo()),
            (int) consumiveis.stream().filter(x -> x.fisico() > 0 && !(x.insumo().custo() > 0)).count(),
            (int) consumiveis.stream().filter(x -> x.fisico() > 0).count(),
            consumiveis.size(),
            (int) consumiveis.stream().filter(x -> {
                String situacao = x.situacao().get(1);
                return "Abaixo do mínimo".equals(situacao) || "Comprar".equals(situacao) || ("Sem saldo".equals(situacao) && x.insumo().estoqueMinimo() > 0);
            }).count(),
            (int) itens.stream().filter(x -> x.fisico() < 0).count()), itens, d.movimentos.isEmpty() && !d.insumos.isEmpty());
    }

    /** Ficha do item no estoque: saldo, reservas, os 100 movimentos gravados mais recentes e fichas que o usam. */
    @Transactional(readOnly = true)
    public ItemEstoqueDetalheDTO detalharItem(Long id) {
        Dados d = dadosService.carregar();
        InsumoResponseDTO i = d.insumo(id);
        if (i == null) {
            throw new AppException("Item não encontrado.", HttpStatus.NOT_FOUND);
        }
        return new ItemEstoqueDetalheDTO(saldoItem(d, Estoque.calcular(d), i),
            d.movimentos.stream().filter(m -> id.equals(m.insumoId())).sorted(MovimentoEstoqueService.MAIS_RECENTES).limit(100)
                .map(m -> MovimentoEstoqueService.listaItem(d, m)).toList(),
            d.preparacoes.stream().filter(p -> p.itens().stream().anyMatch(it -> id.equals(it.insumoId()))).map(PreparacaoResponseDTO::nome).toList());
    }

    /** Disponibilidade de louças e utensílios numa data (padrão: a do próximo evento que usa louças). */
    @Transactional(readOnly = true)
    public EstoqueLoucasResponseDTO loucas(String dataPedida) {
        Dados d = dadosService.carregar();
        Estoque est = Estoque.calcular(d);
        Map<Long, List<Eventos.NecessidadeLouca>> porEvento = Estoque.loucasPorEvento(d);
        String hoje = Formato.hoje();
        String data = dataPedida != null && dataPedida.matches(Dominio.RE_DATA) ? dataPedida
            : d.eventos.stream().filter(e -> Eventos.ativo(e) && e.dataEvento().compareTo(hoje) >= 0 && porEvento.containsKey(e.id()))
                .map(EventoResponseDTO::dataEvento).min(String::compareTo).orElse(hoje);
        return new EstoqueLoucasResponseDTO(data,
            d.eventos.stream().filter(e -> data.equals(e.dataEvento()) && Eventos.ativo(e))
                .map(e -> new EstoqueLoucasResponseDTO.EventoNaData(e.id(), e.codigo(), e.nome())).toList(),
            d.insumos.stream().filter(i -> Dominio.reutilizavel(i.tipo())).sorted(Formato.porNome(InsumoResponseDTO::nome)).map(i -> {
                double total = est.fisico(i.id());
                Map<Long, Double> reservas = Estoque.loucaReservadaNaData(d, porEvento, i.id(), data);
                double reservado = reservas.values().stream().mapToDouble(Double::doubleValue).sum();
                double fora = Estoque.loucaFora(d, i.id());
                return new EstoqueLoucasResponseDTO.Louca(i, total, reservado,
                    reservas.entrySet().stream().map(r -> new EstoqueLoucasResponseDTO.Reserva(d.evento(r.getKey()).codigo(), r.getValue())).toList(), fora,
                    total - reservado - fora);
            }).toList());
    }

    /* ------------------------------ Estoque integrado ------------------------------ */

    /** Sugestão de compras para os eventos dos próximos `horizonte` dias (7, 15, 30 ou 60) e os mínimos. */
    @Transactional(readOnly = true)
    public EstoqueComprasResponseDTO compras(int horizonte) {
        if (!Dominio.HORIZONTES_COMPRA.contains(horizonte)) {
            throw new AppException("Horizonte inválido.", HttpStatus.BAD_REQUEST);
        }
        Map<Long, EstoqueIntegrado.Pedido> pedidos = new HashMap<>();
        compraPedidoRepository.findAll().forEach(p -> {
            String em = p.getPedidoEm().toString();
            pedidos.put(p.getIngredienteId(), new EstoqueIntegrado.Pedido(Ordens.diaLocal(em), em));
        });
        return EstoqueIntegrado.compras(dadosService.carregar(), horizonte, pedidos, Formato.hoje());
    }

    /** Marca (com a hora e quem marcou) ou desmarca o "pedido feito" de um item. */
    @Transactional
    public void marcarPedido(MarcarPedidoRequestDTO req) {
        IngredientesEntity item = cadastro.ingredienteDoCorpo(req.insumoId(), "Item não encontrado.");
        if (Boolean.TRUE.equals(req.feito())) {
            CompraPedidoEntity pedido = compraPedidoRepository.findById(item.getId()).orElseGet(() -> new CompraPedidoEntity(item.getId()));
            pedido.setPedidoEm(Instant.now());
            String por = Usuario.atual();
            pedido.setPedidoPor(por.length() > 100 ? por.substring(0, 100) : por);
            compraPedidoRepository.save(pedido);
        } else {
            compraPedidoRepository.findById(item.getId()).ifPresent(compraPedidoRepository::delete);
        }
    }

    /** Insumos (pela validade da entrada) e pré-preparos vencidos ou vencendo em 7 dias. */
    @Transactional(readOnly = true)
    public EstoqueValidadesResponseDTO validades() {
        return EstoqueIntegrado.validades(dadosService.carregar(), Formato.hoje());
    }

    /** Perdas, avarias e descartes de lote do mês (AAAA-MM), com valor. */
    @Transactional(readOnly = true)
    public EstoquePerdasResponseDTO perdas(String mes) {
        if (mes == null || !mes.matches(Dominio.RE_MES)) {
            throw new AppException("Mês inválido.", HttpStatus.BAD_REQUEST);
        }
        return EstoqueIntegrado.perdas(dadosService.carregar(), mes);
    }

    /** Integridade do estoque: movimentos por origem e pontos para conferir. */
    @Transactional(readOnly = true)
    public EstoqueConferenciaResponseDTO conferencia() {
        return EstoqueIntegrado.conferencia(dadosService.carregar());
    }
}

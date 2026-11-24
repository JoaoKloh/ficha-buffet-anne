package com.jbkloh.ficha_buffet.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.calculo.NotasFiscais;
import com.jbkloh.ficha_buffet.calculo.Ordens;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoOrdemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExtraResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.FechamentoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemPlanoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LogisticaEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MapaProducaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OcorrenciaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OrcamentoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ParcelaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PlanoProducaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratoVendaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SetorProducaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ValoresPorCustoDTO;
import com.jbkloh.ficha_buffet.model.CategoriaFinanceiraEntity;
import com.jbkloh.ficha_buffet.model.ClienteEntity;
import com.jbkloh.ficha_buffet.model.ContratoEmbeddable;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.ExecucaoCozinhaEntity;
import com.jbkloh.ficha_buffet.model.ExtraEventoEntity;
import com.jbkloh.ficha_buffet.model.FechamentoEventoEntity;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.ItemCardapioEntity;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.model.LancamentoPagamentoEntity;
import com.jbkloh.ficha_buffet.model.LiberacaoEventoEntity;
import com.jbkloh.ficha_buffet.model.LogisticaEventoEntity;
import com.jbkloh.ficha_buffet.model.LotePreparoEntity;
import com.jbkloh.ficha_buffet.model.MovimentoEstoqueEntity;
import com.jbkloh.ficha_buffet.model.NotaFiscalEntity;
import com.jbkloh.ficha_buffet.model.OcorrenciaEventoEntity;
import com.jbkloh.ficha_buffet.model.OrcamentoEventoEntity;
import com.jbkloh.ficha_buffet.model.OrdemExecucaoEntity;
import com.jbkloh.ficha_buffet.model.OrdemPlanoEntity;
import com.jbkloh.ficha_buffet.model.ParcelaEntity;
import com.jbkloh.ficha_buffet.model.PlanoProducaoEntity;
import com.jbkloh.ficha_buffet.model.PlanoProducaoItemEntity;
import com.jbkloh.ficha_buffet.model.PratoVendaEntity;
import com.jbkloh.ficha_buffet.model.PreparacaoEntity;
import com.jbkloh.ficha_buffet.model.ProducaoEventoEntity;
import com.jbkloh.ficha_buffet.model.SetorProducaoEntity;
import com.jbkloh.ficha_buffet.model.json.CustosPorTipo;

/**
 * Converte as entidades JPA nos DTOs do Anne Gestão. Datas saem em texto ISO
 * ("AAAA-MM-DD"); campos de texto nulos saem vazios, como as telas esperam.
 */
public final class GestaoMapper {

    private GestaoMapper() {}

    private static double num(BigDecimal v) {
        return v == null ? 0 : v.doubleValue();
    }

    private static double num(Double v) {
        return v == null ? 0 : v;
    }

    private static Double opcional(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }

    private static String txt(String s) {
        return Formato.texto(s);
    }

    private static String iso(Object temporal) {
        return temporal == null ? null : temporal.toString();
    }

    private static Long id(Object entidadeOuNulo, java.util.function.Supplier<Long> id) {
        return entidadeOuNulo == null ? null : id.get();
    }

    public static ClienteResponseDTO cliente(ClienteEntity c) {
        return new ClienteResponseDTO(c.getId(), c.getNome(), c.getTipo(), txt(c.getDocumento()), txt(c.getTelefone()), txt(c.getEmail()), txt(c.getOrigem()),
            iso(c.getCriadoEm()));
    }

    public static CategoriaResponseDTO categoria(CategoriaFinanceiraEntity c) {
        return new CategoriaResponseDTO(c.getId(), c.getNome(), c.getGrupo(), c.getOrdem() == null ? 0 : c.getOrdem());
    }

    public static InsumoResponseDTO insumo(IngredientesEntity i) {
        return new InsumoResponseDTO(i.getId(), i.getNome(), i.getTipo(), i.getCategoria(), i.getUnidade(), i.getLocal(), txt(i.getUnidadeCompra()),
            i.getFatorCompra(), num(i.getEstoqueMinimo()), num(i.getCusto()), txt(i.getFornecedor()), iso(i.getAtualizadoEm()));
    }

    public static PreparacaoResponseDTO preparacao(PreparacaoEntity p) {
        return new PreparacaoResponseDTO(p.getId(), p.getNome(), p.getCategoria(), p.getPraca(), p.getConservacao(), p.getValidadeDias(),
            txt(p.getSetor()), p.getDnProducao(), p.getTempoMin(), txt(p.getEquipamentos()), txt(p.getArmazenamento()), p.isCompradoPronto(),
            num(p.getRendimento()), p.getUnidadeRendimento(), num(p.getPorcaoPadrao()),
            p.getItensIngredientes().stream().map(it -> new PreparacaoResponseDTO.Item(id(it.getIngrediente(), () -> it.getIngrediente().getId()),
                id(it.getSubPreparacao(), () -> it.getSubPreparacao().getId()), num(it.getQuantidade()), it.getFc() == null ? 1 : it.getFc())).toList(),
            p.getLoucas().stream().map(l -> new PreparacaoResponseDTO.Louca(l.getIngrediente().getId(), num(l.getQtd()))).toList(),
            p.getEtapas() == null ? List.of() : p.getEtapas(), p.getComponentes() == null ? List.of() : p.getComponentes(), txt(p.getObsEnvio()),
            txt(p.getModoPreparo()), iso(p.getAtualizadoEm()));
    }

    public static PratoVendaResponseDTO pratoVenda(PratoVendaEntity p) {
        return new PratoVendaResponseDTO(p.getId(), p.getNome(), p.getSecao(), num(p.getPreco()), txt(p.getCodigoPdv()), !Boolean.FALSE.equals(p.getAtivo()),
            txt(p.getObs()),
            p.getComponentes().stream().map(c -> new PratoVendaResponseDTO.Componente(c.getPreparacao().getId(), num(c.getQtd()))).toList(),
            p.getInsumos().stream().map(i -> new PratoVendaResponseDTO.Extra(i.getIngrediente().getId(), num(i.getQtd()))).toList(), iso(p.getAtualizadoEm()));
    }

    public static boolean temContrato(EventoEntity e) {
        return e.getContrato() != null && e.getContrato().getValorTotal() != null;
    }

    public static EventoResponseDTO evento(EventoEntity e) {
        EventoResponseDTO.Contrato contrato = null;
        if (temContrato(e)) {
            ContratoEmbeddable c = e.getContrato();
            contrato = new EventoResponseDTO.Contrato(txt(c.getNumero()), iso(c.getDataAssinatura()),
                c.getConvidadosContratados() == null ? 0 : c.getConvidadosContratados(), num(c.getValorPorConvidado()), num(c.getValorTotal()),
                num(c.getValorConvidadoExtra()), id(c.getCategoria(), () -> c.getCategoria().getId()));
        }
        return new EventoResponseDTO(e.getId(), e.getCodigo(), e.getNome(), e.getCliente().getId(), e.getTipo(), e.getStatus(),
            e.getDataEvento() == null ? "" : e.getDataEvento().toString(), Formato.hora(e.getHoraInicio()), txt(e.getLocal()), txt(e.getCidade()),
            e.getConvidadosPrevistos(), num(e.getMargem()), txt(e.getObservacoes()), contrato, iso(e.getCardapioFechadoEm()),
            e.getEnvioExtras() == null ? List.of() : e.getEnvioExtras(), e.getPlanoCozinha(), txt(e.getEquipeCozinha()), iso(e.getCriadoEm()));
    }

    public static ItemCardapioResponseDTO itemCardapio(ItemCardapioEntity c) {
        return new ItemCardapioResponseDTO(c.getId(), c.getEvento().getId(), c.getPreparacao().getId(), num(c.getPorcao()), c.getStatus(), c.getPraca(),
            txt(c.getResponsavel()));
    }

    public static ParcelaResponseDTO parcela(ParcelaEntity p, Long lancamentoId, double recebido) {
        return new ParcelaResponseDTO(p.getId(), p.getEvento().getId(), p.getNumero(), iso(p.getVencimento()), num(p.getValor()), iso(p.getPagoEm()),
            txt(p.getForma()), lancamentoId, recebido);
    }

    public static ExtraResponseDTO extra(ExtraEventoEntity x, Long lancamentoId) {
        return new ExtraResponseDTO(x.getId(), x.getEvento().getId(), x.getDescricao(), num(x.getQuantidade()), num(x.getValorUnitario()), lancamentoId);
    }

    public static LancamentoCaixaResponseDTO lancamento(LancamentoEntity l) {
        return new LancamentoCaixaResponseDTO(l.getId(), l.getTipo(), l.getCategoria().getId(), id(l.getEvento(), () -> l.getEvento().getId()),
            l.getDescricao(), num(l.getValor()), iso(l.getDataCompetencia()), iso(l.getDataPagamento()), iso(l.getVencimento()), l.getStatus(), l.getOrigem(),
            l.getConta(), id(l.getParcela(), () -> l.getParcela().getId()), id(l.getExtra(), () -> l.getExtra().getId()),
            id(l.getNotaFiscal(), () -> l.getNotaFiscal().getId()), l.getParcelaNF(), l.getFormaPag(), l.getFornecedor(), iso(l.getCriadoEm()), l.getUnidade(),
            l.getUnidadeOrigem(), l.getUnidadeDestino(), id(l.getInsumo(), () -> l.getInsumo().getId()), l.getItem(), l.getQtd(), l.getUnid(),
            l.getResponsavel(), l.getObs(), l.getPagamentos().stream().map(GestaoMapper::pagamento).toList(), l.getCriadoPor(), l.getAlteradoPor(),
            iso(l.getAlteradoEm()));
    }

    public static LancamentoCaixaResponseDTO.Pagamento pagamento(LancamentoPagamentoEntity p) {
        return new LancamentoCaixaResponseDTO.Pagamento(p.getId(), iso(p.getData()), num(p.getValor()), p.getConta(), txt(p.getForma()),
            iso(p.getRegistradoEm()), txt(p.getRegistradoPor()));
    }

    public static MovimentoResponseDTO movimento(MovimentoEstoqueEntity m) {
        return new MovimentoResponseDTO(m.getId(), m.getTipo(), m.getIngrediente().getId(), num(m.getQtd()), num(m.getQtdInformada()), m.getUnidade(),
            iso(m.getData()), txt(m.getMotivo()), id(m.getEvento(), () -> m.getEvento().getId()), opcional(m.getCustoUnit()), m.getOrigem(),
            id(m.getNotaFiscal(), () -> m.getNotaFiscal().getId()), iso(m.getCriadoEm()), iso(m.getValidade()), m.getUnidadeUso(), null);
    }

    public static NotaFiscalResponseDTO notaFiscal(NotaFiscalEntity n, List<Long> movIds, List<Long> lancIds) {
        List<NotaFiscalResponseDTO.Item> itens = n.getItens().stream().map(it -> new NotaFiscalResponseDTO.Item(txt(it.getDescricao()), txt(it.getCodigo()),
            txt(it.getUnidadeComercial()), id(it.getIngrediente(), () -> it.getIngrediente().getId()), num(it.getQtdNota()), it.getUnid(), num(it.getQtd()),
            opcional(it.getValorUnit()), num(it.getValor()), Boolean.TRUE.equals(it.getConferido()))).toList();
        List<NotaFiscalResponseDTO.Parcela> parcelas = n.getParcelas() == null ? List.of() : n.getParcelas().stream()
            .map(p -> new NotaFiscalResponseDTO.Parcela(iso(p.venc()), num(p.valor()), Boolean.TRUE.equals(p.pago()))).toList();
        double acrescimo = num(n.getAcrescimo());
        return new NotaFiscalResponseDTO(n.getId(), n.getFornecedor(), txt(n.getNumero()), iso(n.getEmissao()), id(n.getEvento(), () -> n.getEvento().getId()),
            !Boolean.FALSE.equals(n.getLancarCaixa()), id(n.getCategoria(), () -> n.getCategoria().getId()), n.getFormaPag(), parcelas,
            parcelas.stream().allMatch(NotaFiscalResponseDTO.Parcela::pago), n.getStatus(), itens, acrescimo, NotasFiscais.total(itens, acrescimo),
            txt(n.getChave()), txt(n.getSerie()), txt(n.getRazaoSocial()), txt(n.getCnpjFornecedor()), Boolean.TRUE.equals(n.getImportadaXml()),
            Boolean.TRUE.equals(n.getImportadaFoto()), movIds, lancIds, iso(n.getLancadaEm()), iso(n.getCriadoEm()), n.getUnidade());
    }

    public static LoteResponseDTO lote(LotePreparoEntity l) {
        return new LoteResponseDTO(l.getId(), l.getCodigo(), l.getPreparacao().getId(), num(l.getQtd()), num(l.getSaldo()), l.getUnidade(),
            iso(l.getProduzidoEm()), l.getValidade() == null ? "" : l.getValidade().toString(), l.getDestino(), id(l.getEvento(), () -> l.getEvento().getId()),
            // Lote de evento não dá baixa nos insumos: o checklist do evento já dá.
            !"evento".equals(l.getDestino()), txt(l.getResponsavel()), txt(l.getObs()),
            l.getHistorico().stream().map(h -> new LoteResponseDTO.Historico(h.getTipo(), iso(h.getData()), num(h.getQtd()), h.getPara(),
                id(h.getEvento(), () -> h.getEvento().getId()), txt(h.getObs()),
                iso(h.getEm()))).toList(),
            iso(l.getCriadoEm()), l.getUnidadeNegocio());
    }

    /** Chave da execução: evento + tipo + referência (item do cardápio, componente, insumo ou tarefa). */
    public static String chaveExecucao(String kind, Long eventoId, Long cardapioId, Long insumoId, String key) {
        return switch (kind) {
            case "item" -> "i_" + cardapioId;
            case "comp" -> "c_" + cardapioId + "__" + key;
            case "extra" -> "x_" + eventoId + "__" + key;
            case "louca" -> "l_" + eventoId + "__" + insumoId;
            case "tarefa" -> "t_" + eventoId + "__" + key;
            case "compras" -> "cp_" + eventoId;
            default -> "tr_" + eventoId;
        };
    }

    /* ------------------------------ Ordens de produção ------------------------------ */

    public static MapaProducaoDTO mapaProducao(ProducaoEventoEntity m) {
        return new MapaProducaoDTO(iso(m.getGeradoEm()), m.getLiberadoEm() == null ? "" : iso(m.getLiberadoEm()), txt(m.getResponsavel()),
            txt(m.getHorarios()));
    }

    public static Ordens.Plano ordemPlano(OrdemPlanoEntity p) {
        return new Ordens.Plano(p.getEvento().getId(), p.getChave(), p.getDn() == null ? 0 : p.getDn(), Formato.hora(p.getHora()), p.getSetor(),
            txt(p.getResponsavel()));
    }

    public static Ordens.Execucao ordemExecucao(OrdemExecucaoEntity x) {
        return new Ordens.Execucao(x.getEvento().getId(), x.getChave(), x.getStatus(), x.getStatusAnterior(), txt(x.getResponsavel()),
            new ExecucaoOrdemDTO(txt(x.getIniciadoPor()), iso(x.getIniciadoEm()), txt(x.getConcluidoPor()), iso(x.getConcluidoEm()), x.getQtd(),
                num(x.getPerda()), txt(x.getObs()), txt(x.getConferidoPor()), iso(x.getConferidoEm()), txt(x.getLote()), txt(x.getMotivo()),
                txt(x.getMotivoTxt()), x.getHistorico() == null ? List.of() : List.copyOf(x.getHistorico())));
    }

    public static Ordens.Liberacao liberacao(LiberacaoEventoEntity l) {
        return new Ordens.Liberacao(l.getEvento().getId(), l.getConferencias() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(l.getConferencias()),
            Formato.hora(l.getHoraSaida()), txt(l.getAutorizadoPor()), iso(l.getAutorizadoEm()), l.isComPendencias(), txt(l.getPendencias()));
    }

    public static PlanoProducaoResponseDTO planoProducao(PlanoProducaoEntity p) {
        return new PlanoProducaoResponseDTO(p.getId(), p.getTitulo(), p.getUnidade(), p.getStatus(), iso(p.getDataInicio()), iso(p.getDataFim()), txt(p.getObs()),
            p.getItens().stream().map(GestaoMapper::itemPlano).toList(), p.getPrevisao() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(p.getPrevisao()),
            iso(p.getCriadoEm()), iso(p.getAtualizadoEm()));
    }

    /** A chave do item no plano é o id da linha. */
    public static ItemPlanoDTO itemPlano(PlanoProducaoItemEntity x) {
        return new ItemPlanoDTO(String.valueOf(x.getId()), x.getPreparacao().getId(), num(x.getQtd()), x.isFeito(), iso(x.getFeitoEm()),
            id(x.getLote(), () -> x.getLote().getId()));
    }

    public static OcorrenciaDTO ocorrencia(OcorrenciaEventoEntity o) {
        return new OcorrenciaDTO(o.getId(), o.getEvento().getId(), o.getTipo(), txt(o.getQuem()), txt(o.getTexto()), txt(o.getAcao()), iso(o.getQuando()),
            o.isResolvida());
    }

    public static SetorProducaoDTO setor(SetorProducaoEntity s) {
        return new SetorProducaoDTO(s.getId(), s.getNome(), s.isAtivo());
    }

    public static ExecucaoResponseDTO execucao(ExecucaoCozinhaEntity x) {
        Long eventoId = x.getEvento().getId();
        Long cardapioId = id(x.getItemCardapio(), () -> x.getItemCardapio().getId());
        Long insumoId = id(x.getIngrediente(), () -> x.getIngrediente().getId());
        return new ExecucaoResponseDTO(x.getId(), chaveExecucao(x.getKind(), eventoId, cardapioId, insumoId, x.getReferencia()), x.getKind(), eventoId,
            cardapioId, insumoId, x.getReferencia(), x.getProduzido(), iso(x.getProduzidoEm()), txt(x.getExecutadoPor()), x.getEnviadoQ(),
            Boolean.TRUE.equals(x.getEnviado()), iso(x.getEnviadoEm()), txt(x.getSeparadoPor()), x.getRetorno(), txt(x.getConferidoPor()), txt(x.getObs()),
            x.getTempSaida(), x.getTempChegada(), x.getSaiu(), x.getVoltou(), x.getAvaria(), Boolean.TRUE.equals(x.getFeito()), txt(x.getResp()),
            Boolean.TRUE.equals(x.getDnSet()), x.getDn(), x.getItensComprados() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(x.getItensComprados()),
            txt(x.getChefPor()), Formato.hora(x.getChefHora()), txt(x.getCargaPor()), Formato.hora(x.getCargaHora()), txt(x.getRecebPor()),
            Formato.hora(x.getRecebHora()), txt(x.getRetornoPor()), Formato.hora(x.getRetornoHora()), txt(x.getVeiculo()), iso(x.getAtualizadoEm()));
    }

    /* ---------------------------- Evento como projeto ---------------------------- */

    public static LogisticaEventoDTO logistica(LogisticaEventoEntity l) {
        return new LogisticaEventoDTO(txt(l.getVeiculos()), txt(l.getMotorista()), txt(l.getViagens()), Formato.hora(l.getSaida()),
            Formato.hora(l.getMontagem()), Formato.hora(l.getRetorno()), txt(l.getObs()), iso(l.getAtualizadoEm()));
    }

    public static OrcamentoEventoDTO orcamento(OrcamentoEventoEntity o) {
        return new OrcamentoEventoDTO(opcional(o.getEquipe()), num(o.getTransporte()), num(o.getOutros()), iso(o.getAtualizadoEm()));
    }

    public static FechamentoEventoDTO fechamento(FechamentoEventoEntity f) {
        CustosPorTipo t = f.getPorTipo();
        return new FechamentoEventoDTO(iso(f.getFechadoEm()), txt(f.getFechadoPor()), num(f.getReceita()), num(f.getRecebido()), num(f.getCustoPrevisto()),
            num(f.getCustoRealizado()), num(f.getResultado()), opcional(f.getCmv()),
            new FechamentoEventoDTO.PorTipo(custos(t == null ? null : t.previsto()), custos(t == null ? null : t.realizado())),
            f.getPendencias() == null ? List.of() : List.copyOf(f.getPendencias()));
    }

    private static ValoresPorCustoDTO custos(CustosPorTipo.Valores v) {
        return v == null ? new ValoresPorCustoDTO(0, 0, 0, 0) : new ValoresPorCustoDTO(v.ab(), v.equipe(), v.transporte(), v.outros());
    }

    public static CustosPorTipo.Valores custos(ValoresPorCustoDTO v) {
        return new CustosPorTipo.Valores(v.ab(), v.equipe(), v.transporte(), v.outros());
    }

    /** Par [id, rótulo] das opções de select. */
    public static List<Object> par(Long id, String rotulo) {
        List<Object> par = new ArrayList<>(2);
        par.add(id);
        par.add(rotulo);
        return par;
    }
}

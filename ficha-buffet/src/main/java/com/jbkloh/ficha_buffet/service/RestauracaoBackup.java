package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Caixa;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExtraResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ParcelaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PratoVendaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.model.CategoriaFinanceiraEntity;
import com.jbkloh.ficha_buffet.model.ClienteEntity;
import com.jbkloh.ficha_buffet.model.ContratoEmbeddable;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.ExecucaoCozinhaEntity;
import com.jbkloh.ficha_buffet.model.ExtraEventoEntity;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.ItemCardapioEntity;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.model.LancamentoPagamentoEntity;
import com.jbkloh.ficha_buffet.model.LoteMovimentoEntity;
import com.jbkloh.ficha_buffet.model.LotePreparoEntity;
import com.jbkloh.ficha_buffet.model.MovimentoEstoqueEntity;
import com.jbkloh.ficha_buffet.model.NotaFiscalEntity;
import com.jbkloh.ficha_buffet.model.NotaFiscalItemEntity;
import com.jbkloh.ficha_buffet.model.ParcelaEntity;
import com.jbkloh.ficha_buffet.model.PratoIngredienteEntity;
import com.jbkloh.ficha_buffet.model.PratoLoucaEntity;
import com.jbkloh.ficha_buffet.model.PratoVendaComponenteEntity;
import com.jbkloh.ficha_buffet.model.PratoVendaEntity;
import com.jbkloh.ficha_buffet.model.PratoVendaInsumoEntity;
import com.jbkloh.ficha_buffet.model.PreparacaoEntity;
import com.jbkloh.ficha_buffet.model.json.ParcelaNf;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * Grava no banco um registro vindo do arquivo de backup, com o mesmo id que
 * ele tinha: os registros se referenciam por id. Registro que já existe é
 * atualizado; o que falta é criado com a sequência da tabela posicionada no id
 * do backup. Cada registro tem a própria transação, para uma falha não desfazer os demais.
 */
@Component
@RequiredArgsConstructor
public class RestauracaoBackup {

    /** Coleção do backup -> tabela, na ordem em que as referências entre elas permitem gravar. */
    static final Map<String, String> TABELAS = tabelas();

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper json;

    private static Map<String, String> tabelas() {
        Map<String, String> t = new java.util.LinkedHashMap<>();
        t.put("categorias", "categorias_financeiras");
        t.put("clientes", "clientes");
        t.put("insumos", "ingredientes");
        t.put("preparacoes", "pratos");
        t.put("eventos", "producao");
        t.put("cardapio", "producao_prato");
        t.put("parcelas", "parcelas");
        t.put("extras", "extras_evento");
        t.put("nfs", "notas_fiscais");
        t.put("lancamentos", "lancamentos_financeiros");
        t.put("movimentos", "movimentos_estoque");
        t.put("pratos_venda", "pratos_venda");
        t.put("lotes", "lotes_preparo");
        t.put("cozinha_exec", "cozinha_execucoes");
        return java.util.Collections.unmodifiableMap(t);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void gravar(String colecao, Long id, Map<String, Object> documento) {
        Map<String, Object> doc = new HashMap<>(documento);
        doc.put("id", id);
        switch (colecao) {
            case "categorias" -> categoria(json.convertValue(doc, CategoriaResponseDTO.class));
            case "clientes" -> cliente(json.convertValue(doc, ClienteResponseDTO.class));
            case "insumos" -> insumo(json.convertValue(doc, InsumoResponseDTO.class));
            case "preparacoes" -> preparacao(json.convertValue(doc, PreparacaoResponseDTO.class));
            case "eventos" -> evento(json.convertValue(doc, EventoResponseDTO.class));
            case "cardapio" -> itemCardapio(json.convertValue(doc, ItemCardapioResponseDTO.class));
            case "parcelas" -> parcela(json.convertValue(doc, ParcelaResponseDTO.class));
            case "extras" -> extra(json.convertValue(doc, ExtraResponseDTO.class));
            case "nfs" -> notaFiscal(json.convertValue(doc, NotaFiscalResponseDTO.class));
            case "lancamentos" -> lancamento(json.convertValue(doc, LancamentoCaixaResponseDTO.class));
            case "movimentos" -> movimento(json.convertValue(doc, MovimentoResponseDTO.class));
            case "pratos_venda" -> pratoVenda(json.convertValue(doc, PratoVendaResponseDTO.class));
            case "lotes" -> lote(json.convertValue(doc, LoteResponseDTO.class));
            case "cozinha_exec" -> execucao(json.convertValue(doc, ExecucaoResponseDTO.class));
            default -> throw new IllegalArgumentException("Coleção desconhecida: " + colecao);
        }
        em.flush();
    }

    /** Depois de criar registros com id escolhido, cada sequência volta a continuar do maior id da tabela. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ajustarSequencia(String colecao) {
        String tabela = TABELAS.get(colecao);
        Number maior = (Number) em.createNativeQuery("SELECT COALESCE(MAX(id), 0) FROM " + tabela).getSingleResult();
        posicionarSequencia(tabela, maior.longValue() + 1);
    }

    /**
     * O próximo id gerado pela tabela passa a ser {@code proximo}. Usa setval (basta UPDATE na
     * sequence) em vez de ALTER TABLE ... RESTART, que exige ser dono da tabela: a aplicação
     * conecta como anne_app, sem DDL.
     */
    private void posicionarSequencia(String tabela, long proximo) {
        em.createNativeQuery("SELECT setval(pg_get_serial_sequence(:tabela, 'id'), :proximo, false)")
                .setParameter("tabela", tabela)
                .setParameter("proximo", proximo)
                .getSingleResult();
    }

    /** O registro existente ou um novo, que nascerá com o id pedido. */
    private <E> E alvo(Class<E> tipo, String colecao, Long id, Supplier<E> novo) {
        E existente = em.find(tipo, id);
        if (existente != null) {
            return existente;
        }
        posicionarSequencia(TABELAS.get(colecao), id);
        return novo.get();
    }

    private <E> E ref(Class<E> tipo, Long id) {
        return id == null ? null : em.getReference(tipo, id);
    }

    private static LocalDate data(String iso) {
        return Formato.vazio(iso) ? null : LocalDate.parse(iso);
    }

    private static Instant instante(String iso) {
        return Formato.vazio(iso) ? Instant.now() : Instant.parse(iso);
    }

    private static BigDecimal dinheiro(double v) {
        return BigDecimal.valueOf(v);
    }

    private void cliente(ClienteResponseDTO d) {
        ClienteEntity c = alvo(ClienteEntity.class, "clientes", d.id(), ClienteEntity::new);
        c.setNome(d.nome());
        c.setTipo(d.tipo());
        c.setDocumento(Formato.texto(d.documento()));
        c.setTelefone(Formato.texto(d.telefone()));
        c.setEmail(Formato.texto(d.email()));
        c.setOrigem(Formato.texto(d.origem()));
        c.setCriadoEm(instante(d.criadoEm()));
        em.persist(c);
    }

    private void categoria(CategoriaResponseDTO d) {
        CategoriaFinanceiraEntity c = alvo(CategoriaFinanceiraEntity.class, "categorias", d.id(), CategoriaFinanceiraEntity::new);
        c.setNome(d.nome());
        c.setGrupo(d.grupo());
        c.setOrdem(d.ordem());
        em.persist(c);
    }

    private void insumo(InsumoResponseDTO d) {
        IngredientesEntity i = alvo(IngredientesEntity.class, "insumos", d.id(), IngredientesEntity::new);
        i.setNome(d.nome());
        i.setTipo(d.tipo());
        i.setCategoria(d.categoria());
        i.setUnidade(d.unidade());
        i.setLocal(d.local());
        i.setUnidadeCompra(Formato.texto(d.unidadeCompra()));
        i.setFatorCompra(d.fatorCompra());
        i.setEstoqueMinimo(d.estoqueMinimo());
        i.setCusto(dinheiro(d.custo()));
        i.setFornecedor(Formato.texto(d.fornecedor()));
        i.setAtualizadoEm(data(d.atualizadoEm()));
        em.persist(i);
    }

    private void preparacao(PreparacaoResponseDTO d) {
        PreparacaoEntity p = alvo(PreparacaoEntity.class, "preparacoes", d.id(), PreparacaoEntity::new);
        p.setNome(d.nome());
        p.setCategoria(d.categoria());
        p.setPraca(d.praca());
        p.setConservacao(d.conservacao());
        p.setValidadeDias(d.validadeDias());
        p.setSetor(Formato.texto(d.setor()));
        p.setDnProducao(d.dnProducao());
        p.setTempoMin(d.tempoMin());
        p.setEquipamentos(Formato.texto(d.equipamentos()));
        p.setArmazenamento(Formato.texto(d.armazenamento()));
        p.setCompradoPronto(d.compradoPronto());
        p.setRendimento(d.rendimento());
        p.setUnidadeRendimento(d.unidadeRendimento());
        p.setPorcaoPadrao(d.porcaoPadrao());
        p.setObsEnvio(Formato.texto(d.obsEnvio()));
        p.setModoPreparo(Formato.texto(d.modoPreparo()));
        p.setAtualizadoEm(data(d.atualizadoEm()));
        p.setEtapas(d.etapas() == null ? new ArrayList<>() : new ArrayList<>(d.etapas()));
        p.setComponentes(d.componentes() == null ? new ArrayList<>() : new ArrayList<>(d.componentes()));
        p.getItensIngredientes().clear();
        for (PreparacaoResponseDTO.Item it : d.itens() == null ? List.<PreparacaoResponseDTO.Item>of() : d.itens()) {
            PratoIngredienteEntity linha = new PratoIngredienteEntity();
            linha.setPreparacao(p);
            linha.setIngrediente(ref(IngredientesEntity.class, it.insumoId()));
            linha.setSubPreparacao(ref(PreparacaoEntity.class, it.subId()));
            linha.setQuantidade(it.qtd());
            linha.setFc(it.fc());
            linha.setOrdem(p.getItensIngredientes().size());
            p.getItensIngredientes().add(linha);
        }
        p.getLoucas().clear();
        for (PreparacaoResponseDTO.Louca l : d.loucas() == null ? List.<PreparacaoResponseDTO.Louca>of() : d.loucas()) {
            PratoLoucaEntity louca = new PratoLoucaEntity();
            louca.setPreparacao(p);
            louca.setIngrediente(ref(IngredientesEntity.class, l.insumoId()));
            louca.setQtd(l.qtd());
            louca.setOrdem(p.getLoucas().size());
            p.getLoucas().add(louca);
        }
        em.persist(p);
    }

    private void evento(EventoResponseDTO d) {
        EventoEntity e = alvo(EventoEntity.class, "eventos", d.id(), EventoEntity::new);
        e.setCodigo(d.codigo());
        e.setNome(d.nome());
        e.setCliente(ref(ClienteEntity.class, d.clienteId()));
        e.setTipo(d.tipo());
        e.setStatus(d.status());
        e.setDataEvento(data(d.dataEvento()));
        e.setHoraInicio(Formato.horaOpcional(d.horaInicio()));
        e.setLocal(Formato.texto(d.local()));
        e.setCidade(Formato.texto(d.cidade()));
        e.setConvidadosPrevistos(d.convidadosPrevistos());
        e.setMargem(d.margem());
        e.setObservacoes(Formato.texto(d.observacoes()));
        ContratoEmbeddable contrato = null;
        if (d.contrato() != null) {
            contrato = new ContratoEmbeddable();
            contrato.setNumero(Formato.texto(d.contrato().numero()));
            contrato.setDataAssinatura(data(d.contrato().dataAssinatura()));
            contrato.setConvidadosContratados(d.contrato().convidadosContratados());
            contrato.setValorPorConvidado(dinheiro(d.contrato().valorPorConvidado()));
            contrato.setValorTotal(dinheiro(d.contrato().valorTotal()));
            contrato.setValorConvidadoExtra(dinheiro(d.contrato().valorConvidadoExtra()));
            contrato.setCategoria(ref(CategoriaFinanceiraEntity.class, d.contrato().categoriaId()));
        }
        e.setContrato(contrato);
        e.setCardapioFechadoEm(data(d.cardapioFechadoEm()));
        e.setEnvioExtras(d.envioExtras() == null ? new ArrayList<>() : new ArrayList<>(d.envioExtras()));
        e.setPlanoCozinha(d.planoCozinha() == null ? null : new ArrayList<>(d.planoCozinha()));
        e.setEquipeCozinha(Formato.texto(d.equipeCozinha()));
        e.setCriadoEm(instante(d.criadoEm()));
        em.persist(e);
    }

    private void itemCardapio(ItemCardapioResponseDTO d) {
        ItemCardapioEntity c = alvo(ItemCardapioEntity.class, "cardapio", d.id(), ItemCardapioEntity::new);
        c.setEvento(ref(EventoEntity.class, d.eventoId()));
        c.setPreparacao(ref(PreparacaoEntity.class, d.preparacaoId()));
        c.setPorcao(d.porcao());
        c.setStatus(d.status());
        c.setPraca(d.praca());
        c.setResponsavel(Formato.texto(d.responsavel()));
        em.persist(c);
    }

    private void parcela(ParcelaResponseDTO d) {
        ParcelaEntity p = alvo(ParcelaEntity.class, "parcelas", d.id(), ParcelaEntity::new);
        p.setEvento(ref(EventoEntity.class, d.eventoId()));
        p.setNumero(d.numero());
        p.setVencimento(data(d.vencimento()));
        p.setValor(dinheiro(d.valor()));
        p.setPagoEm(data(d.pagoEm()));
        p.setForma(Formato.texto(d.forma()));
        em.persist(p);
    }

    private void extra(ExtraResponseDTO d) {
        ExtraEventoEntity x = alvo(ExtraEventoEntity.class, "extras", d.id(), ExtraEventoEntity::new);
        x.setEvento(ref(EventoEntity.class, d.eventoId()));
        x.setDescricao(d.descricao());
        x.setQuantidade(d.quantidade());
        x.setValorUnitario(dinheiro(d.valorUnitario()));
        em.persist(x);
    }

    private void notaFiscal(NotaFiscalResponseDTO d) {
        NotaFiscalEntity n = alvo(NotaFiscalEntity.class, "nfs", d.id(), NotaFiscalEntity::new);
        n.setFornecedor(d.fornecedor());
        n.setNumero(Formato.texto(d.numero()));
        n.setSerie(Formato.texto(d.serie()));
        n.setEmissao(data(d.emissao()));
        n.setEvento(ref(EventoEntity.class, d.eventoId()));
        n.setLancarCaixa(d.lancarCaixa());
        n.setCategoria(ref(CategoriaFinanceiraEntity.class, d.categoriaId()));
        n.setFormaPag(d.formaPag());
        n.setUnidade(Formato.vazio(d.unidade()) ? null : d.unidade());
        n.setParcelas(d.parcelas() == null ? new ArrayList<>()
            : new ArrayList<>(d.parcelas().stream().map(p -> new ParcelaNf(data(p.venc()), dinheiro(p.valor()), p.pago())).toList()));
        n.setStatus(d.status());
        n.setAcrescimo(dinheiro(d.acrescimo()));
        n.setChave(Formato.texto(d.chave()));
        n.setRazaoSocial(Formato.texto(d.razaoSocial()));
        n.setCnpjFornecedor(Formato.texto(d.cnpjFornecedor()));
        n.setImportadaXml(d.importadaXML());
        n.setImportadaFoto(d.importadaFoto());
        n.setLancadaEm(data(d.lancadaEm()));
        n.setCriadoEm(instante(d.criadoEm()));
        n.getItens().clear();
        for (NotaFiscalResponseDTO.Item it : d.itens() == null ? List.<NotaFiscalResponseDTO.Item>of() : d.itens()) {
            NotaFiscalItemEntity linha = new NotaFiscalItemEntity();
            linha.setNotaFiscal(n);
            linha.setOrdem(n.getItens().size());
            linha.setDescricao(Formato.texto(it.desc()));
            linha.setCodigo(Formato.texto(it.cod()));
            linha.setUnidadeComercial(Formato.texto(it.uCom()));
            linha.setIngrediente(ref(IngredientesEntity.class, it.insumoId()));
            linha.setQtdNota(it.qtdNota());
            linha.setUnid(it.unid());
            linha.setQtd(it.qtd());
            linha.setValorUnit(it.valorUnit() == null ? null : dinheiro(it.valorUnit()));
            linha.setValor(dinheiro(it.valor()));
            linha.setConferido(it.conferido());
            n.getItens().add(linha);
        }
        em.persist(n);
    }

    private void lancamento(LancamentoCaixaResponseDTO d) {
        LancamentoEntity l = alvo(LancamentoEntity.class, "lancamentos", d.id(), LancamentoEntity::new);
        l.setTipo(d.tipo());
        l.setCategoria(ref(CategoriaFinanceiraEntity.class, d.categoriaId()));
        l.setEvento(ref(EventoEntity.class, d.eventoId()));
        l.setDescricao(d.descricao());
        l.setValor(dinheiro(d.valor()));
        l.setDataCompetencia(data(d.dataCompetencia()));
        l.setDataPagamento(data(d.dataPagamento()));
        l.setVencimento(data(d.vencimento()));
        l.setStatus(d.status());
        l.setOrigem(d.origem());
        l.setConta(d.conta());
        l.setParcela(ref(ParcelaEntity.class, d.parcelaId()));
        l.setExtra(ref(ExtraEventoEntity.class, d.extraId()));
        l.setNotaFiscal(ref(NotaFiscalEntity.class, d.nfId()));
        l.setParcelaNF(d.parcelaNF());
        l.setFormaPag(d.formaPag());
        l.setFornecedor(d.fornecedor());
        l.setCriadoEm(instante(d.criadoEm()));
        l.setUnidade(Formato.vazio(d.unidade()) ? null : d.unidade());
        l.setUnidadeOrigem(d.unidadeOrigem());
        l.setUnidadeDestino(d.unidadeDestino());
        l.setInsumo(ref(IngredientesEntity.class, d.insumoId()));
        l.setItem(d.item());
        l.setQtd(d.qtd());
        l.setUnid(d.unid());
        l.setResponsavel(d.responsavel());
        l.setObs(d.obs());
        l.setCriadoPor(d.criadoPor());
        l.setAlteradoPor(d.alteradoPor());
        l.setAlteradoEm(Formato.vazio(d.alteradoEm()) ? null : instante(d.alteradoEm()));
        // O histórico de pagamentos vem com o lançamento. Backup anterior aos pagamentos em partes:
        // lançamento realizado vira um pagamento único na data de pagamento, como na migração.
        l.getPagamentos().clear();
        List<LancamentoCaixaResponseDTO.Pagamento> pagos = Caixa.pagos(d);
        for (LancamentoCaixaResponseDTO.Pagamento p : pagos) {
            LancamentoPagamentoEntity pg = new LancamentoPagamentoEntity();
            pg.setLancamento(l);
            pg.setData(data(p.data()));
            pg.setValor(dinheiro(p.valor()));
            pg.setConta(Formato.vazio(p.conta()) ? "banco" : p.conta());
            pg.setForma(Formato.texto(p.forma()));
            pg.setRegistradoEm(Formato.vazio(p.em()) ? l.getCriadoEm() : instante(p.em()));
            pg.setRegistradoPor(Formato.vazio(p.por()) ? "restauração" : p.por());
            l.getPagamentos().add(pg);
        }
        em.persist(l);
    }

    private void movimento(MovimentoResponseDTO d) {
        MovimentoEstoqueEntity m = alvo(MovimentoEstoqueEntity.class, "movimentos", d.id(), MovimentoEstoqueEntity::new);
        m.setTipo(d.tipo());
        m.setIngrediente(ref(IngredientesEntity.class, d.insumoId()));
        m.setQtd(d.qtd());
        m.setQtdInformada(d.qtdInformada());
        m.setUnidade(d.unidade());
        m.setData(data(d.data()));
        m.setMotivo(Formato.texto(d.motivo()));
        m.setEvento(ref(EventoEntity.class, d.eventoId()));
        m.setCustoUnit(d.custoUnit() == null ? null : dinheiro(d.custoUnit()));
        m.setOrigem(d.origem());
        m.setNotaFiscal(ref(NotaFiscalEntity.class, d.nfId()));
        m.setValidade(data(d.validade()));
        m.setUnidadeUso(Formato.vazio(d.unidadeUso()) ? null : d.unidadeUso());
        m.setCriadoEm(instante(d.criadoEm()));
        em.persist(m);
    }

    private void pratoVenda(PratoVendaResponseDTO d) {
        PratoVendaEntity p = alvo(PratoVendaEntity.class, "pratos_venda", d.id(), PratoVendaEntity::new);
        p.setNome(d.nome());
        p.setSecao(d.secao());
        p.setPreco(dinheiro(d.preco()));
        p.setCodigoPdv(Formato.texto(d.codigoPdv()));
        p.setAtivo(d.ativo());
        p.setObs(Formato.texto(d.obs()));
        p.setAtualizadoEm(data(d.atualizadoEm()));
        p.getComponentes().clear();
        for (PratoVendaResponseDTO.Componente c : d.componentes() == null ? List.<PratoVendaResponseDTO.Componente>of() : d.componentes()) {
            PratoVendaComponenteEntity componente = new PratoVendaComponenteEntity();
            componente.setPratoVenda(p);
            componente.setPreparacao(ref(PreparacaoEntity.class, c.preparacaoId()));
            componente.setQtd(c.qtd());
            componente.setOrdem(p.getComponentes().size());
            p.getComponentes().add(componente);
        }
        p.getInsumos().clear();
        for (PratoVendaResponseDTO.Extra x : d.extras() == null ? List.<PratoVendaResponseDTO.Extra>of() : d.extras()) {
            PratoVendaInsumoEntity insumo = new PratoVendaInsumoEntity();
            insumo.setPratoVenda(p);
            insumo.setIngrediente(ref(IngredientesEntity.class, x.insumoId()));
            insumo.setQtd(x.qtd());
            insumo.setOrdem(p.getInsumos().size());
            p.getInsumos().add(insumo);
        }
        em.persist(p);
    }

    private void lote(LoteResponseDTO d) {
        LotePreparoEntity l = alvo(LotePreparoEntity.class, "lotes", d.id(), LotePreparoEntity::new);
        l.setCodigo(d.codigo());
        l.setPreparacao(ref(PreparacaoEntity.class, d.preparacaoId()));
        l.setQtd(d.qtd());
        l.setSaldo(d.saldo());
        l.setUnidade(d.unidade());
        l.setProduzidoEm(data(d.produzidoEm()));
        l.setValidade(data(d.validade()));
        l.setDestino(d.destino());
        l.setEvento(ref(EventoEntity.class, d.eventoId()));
        l.setResponsavel(Formato.texto(d.responsavel()));
        l.setObs(Formato.texto(d.obs()));
        l.setCriadoEm(instante(d.criadoEm()));
        // Backup anterior à V12 não tem a unidade: o lote conta como restaurante.
        l.setUnidadeNegocio(d.unidadeNegocio() != null && d.unidadeNegocio().matches(Dominio.RE_UNIDADE_PLANO) ? d.unidadeNegocio() : null);
        l.getHistorico().clear();
        for (LoteResponseDTO.Historico h : d.historico() == null ? List.<LoteResponseDTO.Historico>of() : d.historico()) {
            LoteMovimentoEntity m = new LoteMovimentoEntity();
            m.setLote(l);
            m.setTipo(h.tipo());
            m.setData(data(h.data()));
            m.setQtd(h.qtd());
            m.setPara(h.para());
            m.setEvento(ref(EventoEntity.class, h.eventoId()));
            m.setObs(Formato.texto(h.obs()));
            m.setEm(instante(h.em()));
            l.getHistorico().add(m);
        }
        em.persist(l);
    }

    private void execucao(ExecucaoResponseDTO d) {
        ExecucaoCozinhaEntity x = alvo(ExecucaoCozinhaEntity.class, "cozinha_exec", d.id(), ExecucaoCozinhaEntity::new);
        x.setEvento(ref(EventoEntity.class, d.eventoId()));
        x.setKind(d.kind());
        x.setItemCardapio(ref(ItemCardapioEntity.class, d.cardapioId()));
        x.setIngrediente(ref(IngredientesEntity.class, d.insumoId()));
        x.setReferencia(d.key());
        x.setProduzido(d.produzido());
        x.setProduzidoEm(data(d.produzidoEm()));
        x.setExecutadoPor(Formato.texto(d.executadoPor()));
        x.setEnviadoQ(d.enviadoQ());
        x.setEnviado(d.enviado());
        x.setEnviadoEm(Formato.vazio(d.enviadoEm()) ? null : Instant.parse(d.enviadoEm()));
        x.setSeparadoPor(Formato.texto(d.separadoPor()));
        x.setRetorno(d.retorno());
        x.setConferidoPor(Formato.texto(d.conferidoPor()));
        x.setObs(Formato.texto(d.obs()));
        x.setTempSaida(d.tempSaida());
        x.setTempChegada(d.tempChegada());
        x.setSaiu(d.saiu());
        x.setVoltou(d.voltou());
        x.setAvaria(d.avaria());
        x.setFeito(d.feito());
        x.setResp(Formato.texto(d.resp()));
        x.setDnSet(d.dnSet());
        x.setDn(d.dn());
        x.setItensComprados(d.itens() == null ? new HashMap<>() : new HashMap<>(d.itens()));
        x.setChefPor(Formato.texto(d.chefPor()));
        x.setChefHora(Formato.horaOpcional(d.chefHora()));
        x.setCargaPor(Formato.texto(d.cargaPor()));
        x.setCargaHora(Formato.horaOpcional(d.cargaHora()));
        x.setRecebPor(Formato.texto(d.recebPor()));
        x.setRecebHora(Formato.horaOpcional(d.recebHora()));
        x.setRetornoPor(Formato.texto(d.retornoPor()));
        x.setRetornoHora(Formato.horaOpcional(d.retornoHora()));
        x.setVeiculo(Formato.texto(d.veiculo()));
        x.setAtualizadoEm(instante(d.atualizadoEm()));
        em.persist(x);
    }
}

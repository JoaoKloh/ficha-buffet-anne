package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Caixa;
import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Eventos;
import com.jbkloh.ficha_buffet.calculo.Fichas;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.AjustarEtapaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.ContratoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.EventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PatchEventoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CardapioEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoDetalheResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoOpcoesResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.DesvioCustoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EtapaEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.FechamentoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.IndicadoresEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventosResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoListaItemDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LogisticaEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OrcamentoEventoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ParcelaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PessoaEquipeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ProjetoEventoDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.service.DadosService.Parte;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.CategoriaFinanceiraEntity;
import com.jbkloh.ficha_buffet.model.ClienteEntity;
import com.jbkloh.ficha_buffet.model.ContratoEmbeddable;
import com.jbkloh.ficha_buffet.model.EtapaEventoEntity;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.model.ParcelaEntity;
import com.jbkloh.ficha_buffet.model.json.EnvioExtra;
import com.jbkloh.ficha_buffet.repositories.ClienteRepository;
import com.jbkloh.ficha_buffet.repositories.EquipeEventoRepository;
import com.jbkloh.ficha_buffet.repositories.EtapaEventoRepository;
import com.jbkloh.ficha_buffet.repositories.EventoRepository;
import com.jbkloh.ficha_buffet.repositories.ExecucaoCozinhaRepository;
import com.jbkloh.ficha_buffet.repositories.LiberacaoEventoRepository;
import com.jbkloh.ficha_buffet.repositories.OcorrenciaEventoRepository;
import com.jbkloh.ficha_buffet.repositories.OrdemExecucaoRepository;
import com.jbkloh.ficha_buffet.repositories.OrdemPlanoRepository;
import com.jbkloh.ficha_buffet.repositories.ProducaoEventoRepository;
import com.jbkloh.ficha_buffet.repositories.ExtraEventoRepository;
import com.jbkloh.ficha_buffet.repositories.ItemCardapioRepository;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;
import com.jbkloh.ficha_buffet.repositories.NotaFiscalRepository;
import com.jbkloh.ficha_buffet.repositories.ParcelaRepository;

import lombok.RequiredArgsConstructor;

/** Eventos no contrato do Anne Gestão: funil, ficha do evento e contrato com parcelas. */
@Service
@RequiredArgsConstructor
public class EventoService {

    private static final Map<String, Predicate<EventoResponseDTO>> FILTROS = Map.of(
        "ativos", e -> Eventos.ativo(e) && Dominio.rank(e.status()) < Dominio.rank("acertado"),
        "funil", e -> Dominio.rank(e.status()) < Dominio.rank("contratado"),
        "contratados", e -> Dominio.rank(e.status()) >= Dominio.rank("contratado") && Dominio.rank(e.status()) <= Dominio.rank("em_producao"),
        "realizados", e -> "realizado".equals(e.status()) || "acertado".equals(e.status()),
        "arquivo", e -> !Eventos.ativo(e),
        "todos", e -> true);

    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;
    private final LancamentoRepository lancamentoRepository;
    private final ParcelaRepository parcelaRepository;
    private final ExtraEventoRepository extraRepository;
    private final ExecucaoCozinhaRepository execucaoRepository;
    private final ProducaoEventoRepository producaoEventoRepository;
    private final OrdemPlanoRepository ordemPlanoRepository;
    private final OrdemExecucaoRepository ordemExecucaoRepository;
    private final LiberacaoEventoRepository liberacaoRepository;
    private final OcorrenciaEventoRepository ocorrenciaRepository;
    private final NotaFiscalRepository notaFiscalRepository;
    private final ItemCardapioRepository itemCardapioRepository;
    private final EtapaEventoRepository etapaRepository;
    private final EquipeEventoRepository escalaRepository;
    private final EquipeEventoService equipeEventoService;
    private final ProjetoEventoService projetoEventoService;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    /** Eventos do filtro (ativos por padrão), por data; a busca olha código, nome, local, cidade e cliente. */
    @Transactional(readOnly = true)
    public EventosResponseDTO listarEventos(String filtro, String busca) {
        Dados d = dadosService.carregar();
        String f = FILTROS.containsKey(filtro) ? filtro : "ativos";
        String q = Formato.busca(busca);
        Comparator<EventoListaItemDTO> porData = Comparator.comparing(x -> Formato.vazio(x.evento().dataEvento()) ? "9999" : x.evento().dataEvento());
        List<EventoListaItemDTO> eventos = d.eventos.stream().filter(FILTROS.get(f)).map(e -> Eventos.listaItem(d, e))
            .filter(x -> q.isEmpty() || Formato.contem(String.join(" ", x.evento().codigo(), x.evento().nome(), x.evento().local(), x.evento().cidade(),
                Formato.texto(x.clienteNome())), q))
            .sorted("realizados".equals(f) || "arquivo".equals(f) ? porData.reversed() : porData).toList();
        return new EventosResponseDTO((int) d.eventos.stream().filter(Eventos::ativo).count(), d.eventos.size(), eventos);
    }

    /**
     * Opções de um formulário do módulo de eventos. `para` diz qual: cada um recebe só os
     * campos que usa, e só os cadastros necessários são lidos do banco.
     */
    @Transactional(readOnly = true)
    public EventoOpcoesResponseDTO opcoes(String para) {
        switch (para) {
            case "evento": {
                Dados d = dadosService.carregar(EnumSet.of(Parte.CLIENTES));
                return new EventoOpcoesResponseDTO(
                    d.clientes.stream().sorted(Formato.porNome(ClienteResponseDTO::nome)).map(c -> GestaoMapper.par(c.id(), c.nome())).toList(),
                    null, null, null, null, null, null, null);
            }
            case "contrato": {
                Dados d = dadosService.carregar(EnumSet.of(Parte.CATEGORIAS));
                return new EventoOpcoesResponseDTO(null,
                    Caixa.categoriasOrdenadas(d).stream().filter(c -> "receita".equals(c.grupo())).map(c -> GestaoMapper.par(c.id(), c.nome())).toList(),
                    null, null, null, null, null, null);
            }
            case "lancamento": {
                Dados d = dadosService.carregar(EnumSet.of(Parte.CATEGORIAS, Parte.EVENTOS));
                Map<Long, String> datas = new LinkedHashMap<>();
                d.eventos.forEach(e -> datas.put(e.id(), e.dataEvento()));
                return new EventoOpcoesResponseDTO(null, null, Opcoes.categoriasPorGrupo(d, List.copyOf(Dominio.GRUPOS_DRE.keySet())), null, null,
                    recentesPrimeiro(d).stream().map(e -> GestaoMapper.par(e.id(), e.nome() + Formato.sufixoData(e.dataEvento()))).toList(),
                    datas, null);
            }
            case "cardapio": {
                Dados d = dadosService.carregar(EnumSet.of(Parte.PREPARACOES));
                Map<Long, EventoOpcoesResponseDTO.PreparacaoInfo> info = new LinkedHashMap<>();
                d.preparacoes.forEach(p -> info.put(p.id(), new EventoOpcoesResponseDTO.PreparacaoInfo(p.porcaoPadrao(), Fichas.pracaDe(p), p.unidadeRendimento(), p.rendimento())));
                return new EventoOpcoesResponseDTO(null, null, null, Opcoes.preparacoesPorCategoria(d, true), info, null, null, null);
            }
            case "copiar-cardapio": {
                Dados d = dadosService.carregar(EnumSet.of(Parte.EVENTOS, Parte.CARDAPIO));
                return new EventoOpcoesResponseDTO(null, null, null, null, null, null, null,
                    recentesPrimeiro(d).stream().filter(e -> !d.cardapioDe(e.id()).isEmpty())
                        .map(e -> new EventoOpcoesResponseDTO.EventoComCardapio(e.id(),
                            e.nome() + Formato.sufixoData(e.dataEvento()) + " (" + d.cardapioDe(e.id()).size() + " pratos)")).toList());
            }
            default:
                throw new AppException("Formulário de opções desconhecido.", HttpStatus.BAD_REQUEST);
        }
    }

    private static List<EventoResponseDTO> recentesPrimeiro(Dados d) {
        return d.eventos.stream().sorted(Comparator.comparing(EventoResponseDTO::dataEvento).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public EventoDetalheResponseDTO detalharEvento(Long id) {
        Dados d = dadosService.carregar();
        EventoResponseDTO e = d.evento(id);
        if (e == null) {
            throw new AppException("Evento não encontrado.", HttpStatus.NOT_FOUND);
        }
        ClienteResponseDTO c = d.cliente(e.clienteId());
        String prazo = Eventos.prazoCardapio(e);
        List<ParcelaResponseDTO> parcelas = d.parcelas.stream().filter(p -> id.equals(p.eventoId())).sorted(Comparator.comparingInt(ParcelaResponseDTO::numero)).toList();
        String hoje = Formato.hoje();
        List<LancamentoListaItemDTO> outros = d.lancamentos.stream()
            .filter(l -> id.equals(l.eventoId()) && !"parcela".equals(l.origem()) && !"extra".equals(l.origem()))
            .sorted(Comparator.comparing(LancamentoCaixaResponseDTO::dataCompetencia).reversed())
            .map(l -> Caixa.listaItem(d, l, hoje)).toList();
        Analise a = analisar(d, e);
        return new EventoDetalheResponseDTO(e, e.codigo(),
            c == null ? null : new EventoDetalheResponseDTO.Cliente(c.id(), c.nome(), !c.telefone().isEmpty() ? c.telefone() : c.email()),
            prazo, prazo == null ? null : Formato.diffDays(prazo, Formato.hoje()), Eventos.resultado(d, id, false), parcelas,
            Formato.round2(Formato.soma(parcelas, p -> p.pagoEm() != null ? p.valor() : p.recebido())), Formato.soma(parcelas, ParcelaResponseDTO::valor),
            d.extras.stream().filter(x -> id.equals(x.eventoId())).toList(), outros, a.cardapio(), Eventos.travado(d, e),
            parcelas.stream().anyMatch(p -> p.pagoEm() != null || p.recebido() > 0), Caixa.receitaPadrao(d, e.tipo()),
            a.etapas(), a.equipe(), a.indicadores(), a.desvios(),
            new ProjetoEventoDTO(a.logistica(), a.orcamento(), Eventos.assinaturas(d, id), a.fechamento(),
                Eventos.pendenciasFechamento(e, a.indicadores(), a.desvios(), a.etapas())));
    }

    /** Cardápio, escala, projeto, indicadores e etapas de um evento: a base do detalhe e do fechamento. */
    private record Analise(CardapioEventoDTO cardapio, List<PessoaEquipeResponseDTO> equipe, LogisticaEventoDTO logistica, OrcamentoEventoDTO orcamento,
            FechamentoEventoDTO fechamento, IndicadoresEventoDTO indicadores, List<DesvioCustoDTO> desvios, List<EtapaEventoDTO> etapas) {}

    private Analise analisar(Dados d, EventoResponseDTO e) {
        Long id = e.id();
        CardapioEventoDTO cardapio = Eventos.cardapioDoEvento(d, e);
        List<PessoaEquipeResponseDTO> equipe = equipeEventoService.escala(id);
        LogisticaEventoDTO logistica = projetoEventoService.logistica(id);
        OrcamentoEventoDTO orcamento = projetoEventoService.orcamento(id);
        FechamentoEventoDTO fechamento = projetoEventoService.fechamento(id);
        IndicadoresEventoDTO indicadores = Eventos.indicadores(d, e, cardapio, Formato.soma(equipe, PessoaEquipeResponseDTO::valor), orcamento);
        List<EtapaEventoDTO> etapas = Eventos.etapas(d, e, cardapio, etapasMarcadas(id), equipe.size(), logistica, fechamento);
        return new Analise(cardapio, equipe, logistica, orcamento, fechamento, indicadores, Eventos.desvios(indicadores), etapas);
    }

    /**
     * Fecha o evento: passa para Acertado e guarda o retrato do resultado (receita, custos previsto
     * e realizado, margem e etapas sem marcar) com data e quem fechou. As pendências não impedem o
     * fechamento. 409 se já está Acertado; 422 se perdido ou cancelado.
     */
    @Transactional
    public EventoResponseDTO fecharEvento(Long id) {
        EventoEntity evento = cadastro.evento(id);
        if ("acertado".equals(evento.getStatus())) {
            throw new AppException("Este evento já está fechado.", HttpStatus.CONFLICT);
        }
        if ("perdido".equals(evento.getStatus()) || "cancelado".equals(evento.getStatus())) {
            throw new AppException("Evento perdido ou cancelado não pode ser fechado.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Dados d = dadosService.carregar();
        Analise a = analisar(d, d.evento(id));
        projetoEventoService.gravarFechamento(evento, Eventos.retratoFechamento(a.indicadores(), a.etapas(), Formato.agora(), Usuario.atual()));
        evento.setStatus("acertado");
        return GestaoMapper.evento(evento);
    }

    @Transactional
    public EventoResponseDTO criarEvento(EventoRequestDTO req) {
        EventoEntity evento = new EventoEntity();
        aplicar(req, evento);
        evento.setCodigo(proximoCodigo(evento.getDataEvento()));
        return GestaoMapper.evento(eventoRepository.save(evento));
    }

    /** Mudar a data move a competência dos extras que estavam na data antiga (as parcelas competem ao próprio vencimento). */
    @Transactional
    public EventoResponseDTO atualizarEvento(Long id, EventoRequestDTO req) {
        EventoEntity evento = cadastro.evento(id);
        LocalDate dataAntiga = evento.getDataEvento();
        aplicar(req, evento);
        LocalDate dataNova = evento.getDataEvento();
        if (dataNova != null && dataAntiga != null && !dataNova.equals(dataAntiga)) {
            lancamentoRepository.findByEventoId(id).stream()
                .filter(l -> "extra".equals(l.getOrigem()) && dataAntiga.equals(l.getDataCompetencia()))
                .forEach(l -> l.setDataCompetencia(dataNova));
        }
        return GestaoMapper.evento(evento);
    }

    /** Etapa, margem, cardápio fechado ou reaberto e itens do checklist de envio. */
    @Transactional
    public EventoResponseDTO ajustarEvento(Long id, PatchEventoRequestDTO req) {
        EventoEntity evento = cadastro.evento(id);
        if (req.status() != null) {
            evento.setStatus(req.status());
        }
        if (req.margem() != null) {
            evento.setMargem(req.margem());
        }
        if (Boolean.TRUE.equals(req.cardapioFechado())) {
            evento.setCardapioFechadoEm(LocalDate.now());
            if (Dominio.rank(evento.getStatus()) < Dominio.rank("cardapio_fechado")) {
                evento.setStatus("cardapio_fechado");
            }
        } else if (Boolean.FALSE.equals(req.cardapioFechado())) {
            evento.setCardapioFechadoEm(null);
            if ("cardapio_fechado".equals(evento.getStatus())) {
                evento.setStatus("contratado");
            }
        }
        if (req.envioExtras() != null) {
            evento.setEnvioExtras(envioExtras(req.envioExtras()));
        }
        return GestaoMapper.evento(evento);
    }

    /** Cada item ganha uma chave estável: as execuções do checklist apontam para ela. */
    private List<EnvioExtra> envioExtras(List<PatchEventoRequestDTO.EnvioExtraItem> itens) {
        Set<String> usadas = new HashSet<>();
        itens.stream().map(PatchEventoRequestDTO.EnvioExtraItem::k).filter(k -> !Formato.vazio(k) && !k.startsWith("novo")).forEach(usadas::add);
        List<EnvioExtra> out = new ArrayList<>();
        int seq = 0;
        for (PatchEventoRequestDTO.EnvioExtraItem x : itens) {
            String k = x.k();
            while (Formato.vazio(k) || k.startsWith("novo") || (!k.equals(x.k()) && usadas.contains(k))) {
                k = "x" + Long.toString(System.currentTimeMillis(), 36) + seq++;
            }
            usadas.add(k);
            out.add(new EnvioExtra(k, x.sec(), x.n().trim(), x.u(), x.modo(), x.v(), Formato.vazio(x.obs()) ? null : x.obs().trim()));
        }
        return out;
    }

    /** Exclui em cascata, salvo evento com histórico de caixa ou estoque. */
    @Transactional
    public void apagarEvento(Long id) {
        EventoEntity evento = cadastro.evento(id);
        Dados d = dadosService.carregar();
        String travas = Eventos.travado(d, d.evento(id));
        if (!travas.isEmpty()) {
            throw new AppException("Este evento tem " + travas + ", por isso não pode ser excluído. Mude a etapa para Cancelado ou Perdido.", HttpStatus.CONFLICT);
        }
        // Notas em rascunho apontadas para o evento voltam a ser compra do estoque da fábrica.
        notaFiscalRepository.findByEventoId(id).forEach(n -> n.setEvento(null));
        // A escala sai antes das contas a pagar das diárias; as pessoas ficam no cadastro da equipe.
        escalaRepository.deleteAll(escalaRepository.findByEventoIdOrderByIdAsc(id));
        lancamentoRepository.deleteAll(lancamentoRepository.findByEventoId(id));
        parcelaRepository.deleteAll(parcelaRepository.findByEventoIdOrderByNumeroAsc(id));
        extraRepository.deleteAll(extraRepository.findByEventoId(id));
        execucaoRepository.deleteAll(execucaoRepository.findByEventoId(id));
        itemCardapioRepository.deleteAll(itemCardapioRepository.findByEventoId(id));
        etapaRepository.deleteAll(etapaRepository.findByEventoId(id));
        ordemPlanoRepository.deleteAll(ordemPlanoRepository.findByEventoId(id));
        ordemExecucaoRepository.deleteAll(ordemExecucaoRepository.findByEventoId(id));
        producaoEventoRepository.findByEventoId(id).ifPresent(producaoEventoRepository::delete);
        liberacaoRepository.findByEventoId(id).ifPresent(liberacaoRepository::delete);
        ocorrenciaRepository.deleteAll(ocorrenciaRepository.findByEventoId(id));
        projetoEventoService.apagarDoEvento(id);
        evento.getCardapio().clear();
        eventoRepository.delete(evento);
    }

    /* ------------------------------- Etapas ------------------------------- */

    /**
     * Marca ou desmarca uma etapa à mão, ou dispensa/inclui a degustação. A etapa detectada pelo
     * sistema continua feita mesmo desmarcada; a linha gravada sai quando nada mais está marcado.
     */
    @Transactional
    public void ajustarEtapa(Long id, AjustarEtapaRequestDTO req) {
        EventoEntity evento = cadastro.evento(id);
        if ("fechamento".equals(req.etapa())) {
            throw new AppException("O fechamento financeiro é concluído ao passar o evento para Acertado.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (req.dispensada() != null && !"degustacao".equals(req.etapa())) {
            throw new AppException("Só a degustação pode ser dispensada.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        EtapaEventoEntity etapa = etapaRepository.findByEventoIdAndEtapa(id, req.etapa()).orElseGet(() -> {
            EtapaEventoEntity nova = new EtapaEventoEntity();
            nova.setEvento(evento);
            nova.setEtapa(req.etapa());
            return nova;
        });
        if (req.feito() != null) {
            etapa.setFeito(req.feito());
        }
        if (req.dispensada() != null) {
            etapa.setDispensada(req.dispensada());
        }
        if (!etapa.isFeito() && !etapa.isDispensada()) {
            if (etapa.getId() != null) {
                etapaRepository.delete(etapa);
            }
            return;
        }
        etapa.setAtualizadoEm(Instant.now());
        etapa.setAtualizadoPor(Usuario.atual());
        etapaRepository.save(etapa);
    }

    private Map<String, Eventos.EtapaMarcada> etapasMarcadas(Long eventoId) {
        Map<String, Eventos.EtapaMarcada> m = new LinkedHashMap<>();
        etapaRepository.findByEventoId(eventoId).forEach(x ->
            m.put(x.getEtapa(), new Eventos.EtapaMarcada(x.isFeito(), x.isDispensada(), x.getAtualizadoEm() == null ? null : x.getAtualizadoEm().toString())));
        return m;
    }

    /* ------------------------------ Contrato ------------------------------ */

    /** Grava o contrato, passa o evento a Contratado e cria uma parcela e uma receita prevista por parcela. */
    @Transactional
    public void registrarContrato(Long id, ContratoRequestDTO req) {
        EventoEntity evento = cadastro.evento(id);
        if (GestaoMapper.temContrato(evento)) {
            throw new AppException("Este evento já tem contrato.", HttpStatus.CONFLICT);
        }
        if (evento.getDataEvento() == null) {
            throw new AppException("Defina a data do evento antes do contrato.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        CategoriaFinanceiraEntity categoria = aplicarContrato(req, evento);
        if (Dominio.rank(evento.getStatus()) < Dominio.rank("contratado")) {
            evento.setStatus("contratado");
        }
        LocalDate primeiro = Formato.data(req.primeiroVencimento(), "primeiro vencimento");
        List<Double> valores = Caixa.dividirParcelas(req.valorTotal().doubleValue(), req.nParcelas());
        for (int i = 0; i < valores.size(); i++) {
            BigDecimal valor = BigDecimal.valueOf(valores.get(i));
            ParcelaEntity parcela = new ParcelaEntity();
            parcela.setEvento(evento);
            parcela.setNumero(i + 1);
            LocalDate vencimento = primeiro.plusMonths((long) i * req.intervaloMeses());
            parcela.setVencimento(vencimento);
            parcela.setValor(valor);
            parcelaRepository.save(parcela);
            // Na DRE a receita do evento conta na data do evento; o vencimento serve às contas a receber e ao fluxo.
            LancamentoEntity receita = CaixaService.novoLancamento(categoria, evento, "Parcela " + (i + 1) + "/" + valores.size() + " · " + evento.getNome(),
                valor, vencimento, "parcela");
            receita.setVencimento(vencimento);
            receita.setUnidade("buffet");
            receita.setParcela(parcela);
            lancamentoRepository.save(receita);
        }
    }

    /** Não refaz as parcelas; só reclassifica as receitas se a categoria mudou. */
    @Transactional
    public void atualizarContrato(Long id, ContratoRequestDTO req) {
        EventoEntity evento = cadastro.evento(id);
        if (!GestaoMapper.temContrato(evento)) {
            throw new AppException("Este evento não tem contrato.", HttpStatus.NOT_FOUND);
        }
        CategoriaFinanceiraEntity categoria = aplicarContrato(req, evento);
        lancamentoRepository.findByEventoId(id).stream().filter(l -> "parcela".equals(l.getOrigem())).forEach(l -> {
            l.setCategoria(categoria);
            l.setTipo(CategoriaFinanceiraService.tipoDoGrupo(categoria.getGrupo()));
        });
    }

    @Transactional
    public void apagarContrato(Long id) {
        EventoEntity evento = cadastro.evento(id);
        List<ParcelaEntity> parcelas = parcelaRepository.findByEventoIdOrderByNumeroAsc(id);
        if (parcelas.stream().anyMatch(p -> p.getPagoEm() != null)
            || parcelas.stream().anyMatch(p -> lancamentoRepository.findByParcelaId(p.getId()).map(l -> !l.getPagamentos().isEmpty()).orElse(false))) {
            throw new AppException("O contrato tem parcela recebida, por isso não pode ser excluído.", HttpStatus.CONFLICT);
        }
        parcelas.forEach(p -> lancamentoRepository.findByParcelaId(p.getId()).ifPresent(lancamentoRepository::delete));
        parcelaRepository.deleteAll(parcelas);
        evento.setContrato(null);
    }

    private CategoriaFinanceiraEntity aplicarContrato(ContratoRequestDTO req, EventoEntity evento) {
        CategoriaFinanceiraEntity categoria = cadastro.categoriaDoCorpo(req.categoriaId(), "Crie uma categoria de receita em Caixa › Categorias.");
        ContratoEmbeddable contrato = new ContratoEmbeddable();
        contrato.setNumero(Formato.texto(req.numero()).trim());
        contrato.setDataAssinatura(Formato.data(req.dataAssinatura(), "data de assinatura"));
        contrato.setConvidadosContratados(req.convidadosContratados());
        contrato.setValorPorConvidado(req.valorPorConvidado());
        contrato.setValorTotal(req.valorTotal());
        contrato.setValorConvidadoExtra(req.valorConvidadoExtra());
        contrato.setCategoria(categoria);
        evento.setContrato(contrato);
        return categoria;
    }

    /* ------------------------------- Apoio ------------------------------- */

    private void aplicar(EventoRequestDTO req, EventoEntity evento) {
        evento.setNome(req.nome().trim());
        evento.setCliente(cliente(req));
        evento.setTipo(req.tipo());
        evento.setStatus(req.status());
        evento.setDataEvento(Formato.dataOpcional(req.dataEvento(), "data do evento"));
        evento.setHoraInicio(Formato.horaOpcional(req.horaInicio()));
        evento.setLocal(Formato.texto(req.local()).trim());
        evento.setCidade(Formato.texto(req.cidade()).trim());
        evento.setConvidadosPrevistos(req.convidadosPrevistos());
        evento.setMargem(req.margem());
        evento.setObservacoes(Formato.texto(req.observacoes()).trim());
    }

    /** Cliente existente ou um novo, criado junto com o evento. */
    private ClienteEntity cliente(EventoRequestDTO req) {
        if (req.clienteId() != null) {
            return clienteRepository.findById(req.clienteId())
                .orElseThrow(() -> new AppException("O cliente de id " + req.clienteId() + " não existe.", HttpStatus.UNPROCESSABLE_ENTITY));
        }
        ClienteEntity novo = new ClienteEntity();
        novo.setNome(req.novoCliente().nome().trim());
        novo.setTelefone(Formato.texto(req.novoCliente().telefone()).trim());
        return clienteRepository.save(novo);
    }

    /** Próximo código "AA-NNN" do ano do evento (ou do ano corrente, sem data). */
    private String proximoCodigo(LocalDate data) {
        String ano = String.format("%02d", (data != null ? data : LocalDate.now()).getYear() % 100);
        int maior = eventoRepository.findCodigosPorPrefixo(ano + "-%").stream().mapToInt(codigo -> {
            try {
                return Integer.parseInt(codigo.substring(3));
            } catch (NumberFormatException e) {
                return 0;
            }
        }).max().orElse(0);
        return ano + "-" + String.format("%03d", maior + 1);
    }
}

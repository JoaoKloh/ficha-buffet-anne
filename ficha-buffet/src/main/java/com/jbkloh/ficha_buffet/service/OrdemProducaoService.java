package com.jbkloh.ficha_buffet.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Cozinha;
import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Estoque;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.calculo.Ordens;
import com.jbkloh.ficha_buffet.dtos.req.AcaoOrdemRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.AutorizarLiberacaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.LiberarMapaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.MestreMapaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.PlanoOrdemRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarLiberacaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarSetoresRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LiberacaoExpedicaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OrdemProducaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SetorProducaoDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.ExecucaoCozinhaEntity;
import com.jbkloh.ficha_buffet.model.ItemCardapioEntity;
import com.jbkloh.ficha_buffet.model.LiberacaoEventoEntity;
import com.jbkloh.ficha_buffet.model.OrdemExecucaoEntity;
import com.jbkloh.ficha_buffet.model.OrdemPlanoEntity;
import com.jbkloh.ficha_buffet.model.ProducaoEventoEntity;
import com.jbkloh.ficha_buffet.model.SetorProducaoEntity;
import com.jbkloh.ficha_buffet.model.json.HistoricoOrdem;
import com.jbkloh.ficha_buffet.repositories.ExecucaoCozinhaRepository;
import com.jbkloh.ficha_buffet.repositories.ItemCardapioRepository;
import com.jbkloh.ficha_buffet.repositories.LiberacaoEventoRepository;
import com.jbkloh.ficha_buffet.repositories.OrdemExecucaoRepository;
import com.jbkloh.ficha_buffet.repositories.OrdemPlanoRepository;
import com.jbkloh.ficha_buffet.repositories.ProducaoEventoRepository;
import com.jbkloh.ficha_buffet.repositories.SetorProducaoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Ordens de produção da cozinha (aba Ordens e Painel do líder): mapa do evento, plano da gestão,
 * ações da equipe, setores e liberação para expedição. As ordens são geradas na leitura
 * (Ordens.gerar); aqui se grava só o que a gestão e a equipe decidem.
 */
@Service
@RequiredArgsConstructor
public class OrdemProducaoService {

    private static final int HISTORICO_MAX = 40;
    private static final Map<String, String> STATUS_LABEL = Map.of("pendente", "pendente", "em_producao", "em produção", "em_conferencia",
        "em conferência", "concluida", "concluída", "bloqueada", "bloqueada");

    private final ProducaoEventoRepository producaoEventoRepository;
    private final OrdemPlanoRepository planoRepository;
    private final OrdemExecucaoRepository execucaoOrdemRepository;
    private final LiberacaoEventoRepository liberacaoRepository;
    private final SetorProducaoRepository setorRepository;
    private final ExecucaoCozinhaRepository execucaoCozinhaRepository;
    private final ItemCardapioRepository itemCardapioRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    /* --------------------------------- Mapa do evento --------------------------------- */

    /** Gera o mapa de produção: as ordens passam a existir, visíveis só para a gestão até liberar. */
    @Transactional
    public void gerarMapa(Long eventoId) {
        EventoEntity evento = cadastro.evento(eventoId);
        if ("perdido".equals(evento.getStatus()) || "cancelado".equals(evento.getStatus())) {
            throw new AppException("Evento perdido ou cancelado não tem produção.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (producaoEventoRepository.findByEventoId(eventoId).isPresent()) {
            throw new AppException("O mapa de produção deste evento já foi gerado.", HttpStatus.CONFLICT);
        }
        ProducaoEventoEntity m = new ProducaoEventoEntity();
        m.setEvento(evento);
        m.setGeradoEm(Instant.now());
        m.setGeradoPor(Usuario.atual());
        producaoEventoRepository.save(m);
    }

    @Transactional
    public void salvarMestre(Long eventoId, MestreMapaRequestDTO req) {
        ProducaoEventoEntity m = mapa(eventoId);
        m.setResponsavel(Formato.texto(req.responsavel()).trim());
        m.setHorarios(Formato.texto(req.horarios()).trim());
    }

    /** Libera as ordens para a equipe ou recolhe de volta (a liberação mantém a data da primeira vez). */
    @Transactional
    public void liberarMapa(Long eventoId, LiberarMapaRequestDTO req) {
        ProducaoEventoEntity m = mapa(eventoId);
        if (!req.liberar()) {
            m.setLiberadoEm(null);
        } else if (m.getLiberadoEm() == null) {
            m.setLiberadoEm(Instant.now());
        }
    }

    /** Prazo, horário limite, setor e responsável de uma ordem do mapa. */
    @Transactional
    public void planejar(Long eventoId, PlanoOrdemRequestDTO req) {
        EventoEntity evento = mapa(eventoId).getEvento();
        Dados d = dadosService.carregar();
        exigirOrdem(d, eventoId, req.k());
        if (dadosService.setores().stream().noneMatch(s -> s.ativo() && s.id().equals(req.setor()))) {
            throw new AppException("Setor inválido.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        OrdemPlanoEntity p = planoRepository.findByEventoIdAndChave(eventoId, req.k()).orElseGet(() -> {
            OrdemPlanoEntity novo = new OrdemPlanoEntity();
            novo.setEvento(evento);
            novo.setChave(req.k());
            return novo;
        });
        p.setDn(req.dn());
        p.setHora(Formato.horaOpcional(Formato.texto(req.hora())));
        p.setSetor(req.setor());
        p.setResponsavel(Formato.texto(req.resp()).trim());
        p.setAtualizadoEm(Instant.now());
        p.setAtualizadoPor(Usuario.atual());
        planoRepository.save(p);
    }

    /* ---------------------------------- Ações da equipe ---------------------------------- */

    /** Ação numa ordem: iniciar, concluir, conferir, bloquear, desbloquear, reabrir ou responsavel. */
    @Transactional
    public void agir(String acao, AcaoOrdemRequestDTO req) {
        EventoEntity evento = cadastro.eventoDoCorpo(req.eventoId());
        mapa(evento.getId());
        Dados d = dadosService.carregar();
        OrdemProducaoDTO o = exigirOrdem(d, evento.getId(), req.k());
        OrdemExecucaoEntity x = execucaoOrdemRepository.findByEventoIdAndChave(evento.getId(), req.k()).orElseGet(() -> {
            OrdemExecucaoEntity nova = new OrdemExecucaoEntity();
            nova.setEvento(evento);
            nova.setChave(req.k());
            return nova;
        });
        String quem = Formato.texto(req.quem()).trim();
        Instant agora = Instant.now();
        switch (acao) {
            case "iniciar" -> {
                exigirStatus(o, "pendente");
                exigirNome(quem);
                String resp = Formato.texto(req.resp()).trim();
                x.setStatus("em_producao");
                x.setIniciadoPor(quem);
                x.setIniciadoEm(agora);
                x.setResponsavel(resp.isEmpty() ? quem : resp);
                historico(x, "iniciou", quem);
            }
            case "concluir" -> {
                exigirStatus(o, "em_producao");
                exigirNome(quem);
                Double qtd = null;
                double perda = 0;
                if (o.q() > 0) {
                    if (req.qtd() == null || !(req.qtd() > 0)) {
                        throw new AppException("Informe a quantidade produzida.", HttpStatus.BAD_REQUEST);
                    }
                    qtd = Formato.r3(req.qtd());
                    perda = req.perda() == null ? 0 : Formato.r3(req.perda());
                }
                x.setStatus("em_conferencia");
                x.setConcluidoPor(quem);
                x.setConcluidoEm(agora);
                x.setQtd(qtd);
                x.setPerda(perda);
                x.setObs(Formato.texto(req.obs()).trim());
                historico(x, "concluiu" + (qtd == null ? "" : " · " + Formato.fmtN(qtd) + " " + o.u()), quem);
            }
            case "conferir" -> {
                exigirStatus(o, "em_conferencia");
                exigirNome(quem);
                String lote = Formato.texto(evento.getCodigo()).replaceAll("[^A-Za-z0-9]", "") + "-" + req.k().substring(0, 1).toUpperCase()
                    + String.valueOf(System.currentTimeMillis()).substring(9);
                x.setStatus("concluida");
                x.setConferidoPor(quem);
                x.setConferidoEm(agora);
                x.setLote(lote);
                historico(x, "conferiu e aprovou · lote " + lote, quem);
                if ("principal".equals(o.etapa()) && o.cid() != null && x.getQtd() != null) {
                    registrarProduzidoNoChecklist(evento, o.cid(), x.getQtd(), Formato.vazio(x.getConcluidoPor()) ? quem : x.getConcluidoPor());
                }
            }
            case "bloquear" -> {
                exigirStatus(o, "pendente", "em_producao");
                exigirNome(quem);
                String texto = Formato.texto(req.texto()).trim();
                if (Formato.vazio(req.motivo()) || texto.isEmpty()) {
                    throw new AppException("Escolha o motivo e descreva o impedimento.", HttpStatus.BAD_REQUEST);
                }
                String motivo = Dominio.MOTIVOS_BLOQUEIO.get(req.motivo());
                x.setStatusAnterior(o.status());
                x.setStatus("bloqueada");
                x.setMotivo(motivo);
                x.setMotivoTxt(texto);
                historico(x, "bloqueou · " + motivo + " · " + texto, quem);
            }
            case "desbloquear" -> {
                exigirStatus(o, "bloqueada");
                String anterior = x.getStatusAnterior();
                x.setStatus(anterior != null && !"bloqueada".equals(anterior) && Dominio.STATUS_ORDEM.contains(anterior) ? anterior : "pendente");
                x.setMotivo("");
                x.setMotivoTxt("");
                historico(x, "desbloqueou", quem);
            }
            case "reabrir" -> {
                exigirStatus(o, "em_conferencia", "concluida");
                x.setStatus("em_producao");
                x.setMotivo("");
                x.setMotivoTxt("");
                historico(x, "reabriu", quem);
            }
            case "responsavel" -> {
                exigirNome(quem);
                String resp = Formato.texto(req.resp()).trim();
                if (resp.isEmpty()) {
                    throw new AppException("Informe o responsável.", HttpStatus.BAD_REQUEST);
                }
                x.setResponsavel(resp);
                historico(x, "responsável: " + resp, quem);
            }
            default -> throw new AppException("Ação inválida.", HttpStatus.NOT_FOUND);
        }
        x.setAtualizadoEm(agora);
        execucaoOrdemRepository.save(x);
    }

    /**
     * Ordem principal conferida: se o checklist ainda não tem a produção do prato, ela passa a ser a
     * quantidade conferida (a baixa no estoque continua vindo só da ordem, porque o evento tem mapa).
     */
    private void registrarProduzidoNoChecklist(EventoEntity evento, Long cardapioId, double qtd, String executadoPor) {
        ItemCardapioEntity item = itemCardapioRepository.findById(cardapioId).filter(c -> c.getEvento().getId().equals(evento.getId())).orElse(null);
        if (item == null) {
            return;
        }
        ExecucaoCozinhaEntity x = execucaoCozinhaRepository.findByEventoId(evento.getId()).stream()
            .filter(e -> "item".equals(e.getKind()) && e.getItemCardapio() != null && e.getItemCardapio().getId().equals(cardapioId)).findFirst()
            .orElse(null);
        if (x != null && x.getProduzido() != null && x.getProduzido() > 0) {
            return;
        }
        if (x == null) {
            x = new ExecucaoCozinhaEntity();
            x.setEvento(evento);
            x.setKind("item");
            x.setItemCardapio(item);
        }
        x.setProduzido(qtd);
        x.setProduzidoEm(LocalDate.now());
        x.setExecutadoPor(executadoPor.length() > 100 ? executadoPor.substring(0, 100) : executadoPor);
        x.setAtualizadoEm(Instant.now());
        execucaoCozinhaRepository.save(x);
    }

    private static void historico(OrdemExecucaoEntity x, String acao, String quem) {
        List<HistoricoOrdem> h = new ArrayList<>(x.getHistorico() == null ? List.of() : x.getHistorico());
        h.add(new HistoricoOrdem(Formato.agora(), quem, acao));
        x.setHistorico(new ArrayList<>(h.subList(Math.max(0, h.size() - HISTORICO_MAX), h.size())));
    }

    private static void exigirNome(String quem) {
        if (quem.isEmpty()) {
            throw new AppException("Informe seu nome.", HttpStatus.BAD_REQUEST);
        }
    }

    private static void exigirStatus(OrdemProducaoDTO o, String... permitidos) {
        if (!Set.of(permitidos).contains(o.status())) {
            throw new AppException("Esta ordem está " + STATUS_LABEL.getOrDefault(o.status(), o.status()) + ". Atualize a tela.", HttpStatus.CONFLICT);
        }
    }

    /* ------------------------------------ Setores ------------------------------------ */

    /** Renomeia, ativa/desativa e cria setores (id vazio). Setor não é excluído: ordens e fichas guardam o id. */
    @Transactional
    public List<SetorProducaoDTO> salvarSetores(SalvarSetoresRequestDTO req) {
        Map<String, SetorProducaoEntity> atuais = setorRepository.findAll().stream().collect(Collectors.toMap(SetorProducaoEntity::getId, Function.identity()));
        if (atuais.isEmpty()) {
            int ordem = 0;
            for (Map.Entry<String, String> e : Dominio.SETORES_PADRAO.entrySet()) {
                SetorProducaoEntity s = new SetorProducaoEntity();
                s.setId(e.getKey());
                s.setNome(e.getValue());
                s.setOrdem(ordem++);
                atuais.put(s.getId(), setorRepository.save(s));
            }
        }
        int proxima = atuais.values().stream().mapToInt(SetorProducaoEntity::getOrdem).max().orElse(-1) + 1;
        Map<String, Integer> novosNoLote = new HashMap<>();
        for (SalvarSetoresRequestDTO.Setor s : req.setores()) {
            String id = Formato.texto(s.id()).trim();
            SetorProducaoEntity e;
            if (id.isEmpty()) {
                e = new SetorProducaoEntity();
                String base = "s" + Long.toString(System.currentTimeMillis(), 36);
                int n = novosNoLote.merge(base, 1, Integer::sum);
                e.setId(n == 1 ? base : base + n);
                e.setOrdem(proxima++);
                atuais.put(e.getId(), e);
            } else {
                e = atuais.get(id);
                if (e == null) {
                    throw new AppException("Setor inválido.", HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
            e.setNome(s.nome().trim());
            e.setAtivo(!Boolean.FALSE.equals(s.ativo()));
            setorRepository.save(e);
        }
        return dadosService.setores();
    }

    /* ------------------------------ Liberação para expedição ------------------------------ */

    /** Conferências e horário de saída (antes da autorização). */
    @Transactional
    public void salvarLiberacao(Long eventoId, SalvarLiberacaoRequestDTO req) {
        EventoEntity evento = mapa(eventoId).getEvento();
        LiberacaoEventoEntity l = liberacao(evento);
        if (l.getAutorizadoEm() != null) {
            throw new AppException("A saída já foi liberada. Desfaça a liberação para mudar as conferências.", HttpStatus.CONFLICT);
        }
        if (req.ck() != null) {
            Map<String, Boolean> ck = new HashMap<>(l.getConferencias() == null ? Map.of() : l.getConferencias());
            ck.putAll(req.ck());
            l.setConferencias(ck);
        }
        if (req.horaSaida() != null) {
            l.setHoraSaida(Formato.horaOpcional(req.horaSaida()));
        }
        l.setAtualizadoEm(Instant.now());
        liberacaoRepository.save(l);
    }

    /**
     * Libera a saída. Com ordens abertas, itens do checklist que não podem sair, conferências
     * faltando ou sem horário, só com as pendências justificadas. Conclui a ordem de expedição.
     */
    @Transactional
    public void autorizarLiberacao(Long eventoId, AutorizarLiberacaoRequestDTO req) {
        EventoEntity evento = mapa(eventoId).getEvento();
        LiberacaoEventoEntity l = liberacao(evento);
        if (l.getAutorizadoEm() != null) {
            throw new AppException("A saída deste evento já foi liberada.", HttpStatus.CONFLICT);
        }
        Dados d = dadosService.carregar();
        CozinhaProjecaoDTO pj = Cozinha.projecao(d, d.evento(eventoId), Estoque.calcular(d));
        List<OrdemProducaoDTO> os = Ordens.ordens(d, d.evento(eventoId), Formato.hoje(), agoraHm());
        LiberacaoExpedicaoDTO estado = Ordens.liberacao(d, pj, os);
        String pendencias = Formato.texto(req.pendencias()).trim();
        if (!estado.ok() && pendencias.isEmpty()) {
            throw new AppException("Justifique as pendências: falta " + String.join(", ", estado.faltam()) + ".", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        String quem = req.quem().trim();
        Instant agora = Instant.now();
        l.setAutorizadoPor(quem);
        l.setAutorizadoEm(agora);
        l.setComPendencias(!estado.ok());
        l.setPendencias(estado.ok() ? "" : pendencias);
        l.setAtualizadoEm(agora);
        liberacaoRepository.save(l);

        OrdemExecucaoEntity xp = execucaoOrdemRepository.findByEventoIdAndChave(eventoId, Ordens.EXPEDICAO).orElseGet(() -> {
            OrdemExecucaoEntity nova = new OrdemExecucaoEntity();
            nova.setEvento(evento);
            nova.setChave(Ordens.EXPEDICAO);
            return nova;
        });
        if (!"concluida".equals(xp.getStatus())) {
            xp.setStatus("concluida");
            xp.setConcluidoPor(quem);
            xp.setConcluidoEm(agora);
            xp.setConferidoPor(quem);
            xp.setConferidoEm(agora);
            historico(xp, "expedição liberada", quem);
            xp.setAtualizadoEm(agora);
            execucaoOrdemRepository.save(xp);
        }
    }

    @Transactional
    public void desfazerLiberacao(Long eventoId) {
        mapa(eventoId);
        LiberacaoEventoEntity l = liberacaoRepository.findByEventoId(eventoId).filter(x -> x.getAutorizadoEm() != null)
            .orElseThrow(() -> new AppException("A saída deste evento ainda não foi liberada.", HttpStatus.CONFLICT));
        l.setAutorizadoEm(null);
        l.setAutorizadoPor("");
        l.setComPendencias(false);
        l.setAtualizadoEm(Instant.now());
    }

    private LiberacaoEventoEntity liberacao(EventoEntity evento) {
        return liberacaoRepository.findByEventoId(evento.getId()).orElseGet(() -> {
            LiberacaoEventoEntity nova = new LiberacaoEventoEntity();
            nova.setEvento(evento);
            return nova;
        });
    }

    /* ------------------------------------ Auxiliares ------------------------------------ */

    /** Mapa do evento (404 sem evento; 422 se o mapa ainda não foi gerado). */
    private ProducaoEventoEntity mapa(Long eventoId) {
        cadastro.evento(eventoId);
        return producaoEventoRepository.findByEventoId(eventoId)
            .orElseThrow(() -> new AppException("Gere o mapa de produção do evento primeiro.", HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private static OrdemProducaoDTO exigirOrdem(Dados d, Long eventoId, String k) {
        return Ordens.ordens(d, d.evento(eventoId), Formato.hoje(), agoraHm()).stream().filter(o -> o.k().equals(k)).findFirst()
            .orElseThrow(() -> new AppException("Esta ordem não existe mais no mapa do evento. Atualize a tela.", HttpStatus.UNPROCESSABLE_ENTITY));
    }

    static String agoraHm() {
        return Formato.hora(java.time.LocalTime.now());
    }
}

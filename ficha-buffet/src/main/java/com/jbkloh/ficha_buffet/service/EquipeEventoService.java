package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Caixa;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.PessoaEquipeRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.AssociarEquipeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancarEquipeResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PessoaEquipeResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.CategoriaFinanceiraEntity;
import com.jbkloh.ficha_buffet.model.EquipeEntity;
import com.jbkloh.ficha_buffet.model.EquipeEventoEntity;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.LancamentoEntity;
import com.jbkloh.ficha_buffet.repositories.EquipeEventoRepository;
import com.jbkloh.ficha_buffet.repositories.EquipeRepository;
import com.jbkloh.ficha_buffet.repositories.LancamentoRepository;
import com.jbkloh.ficha_buffet.service.DadosService.Parte;

import lombok.RequiredArgsConstructor;

/**
 * Escala de equipe do evento sobre o cadastro da equipe: a mesma pessoa é escalada em vários
 * eventos, cada vez com a função, os horários e a diária daquele evento. A diária pode virar
 * conta a pagar (origem "equipe") na data do evento.
 */
@Service
@RequiredArgsConstructor
public class EquipeEventoService {

    private static final String SEM_CATEGORIA = "Crie uma categoria de custo com \"mão de obra\" ou \"equipe\" no nome.";

    private final EquipeRepository equipeRepository;
    private final EquipeEventoRepository escalaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final EquipeService equipeService;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    /** Escala do evento por horário de chegada (sem horário primeiro) e função, como na tela. */
    @Transactional(readOnly = true)
    public List<PessoaEquipeResponseDTO> escala(Long eventoId) {
        Collator pt = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        return escalaRepository.findByEventoIdOrderByIdAsc(eventoId).stream().map(EquipeEventoService::resposta)
            .sorted(Comparator.comparing(PessoaEquipeResponseDTO::chegada).thenComparing(PessoaEquipeResponseDTO::funcao, pt)).toList();
    }

    /** Escala alguém: a pessoa escolhida (equipeId), a de mesmo nome no cadastro ou uma nova. */
    @Transactional
    public PessoaEquipeResponseDTO criar(Long eventoId, PessoaEquipeRequestDTO req) {
        EventoEntity evento = cadastro.evento(eventoId);
        EquipeEntity pessoa;
        if (req.equipeId() != null) {
            pessoa = equipeRepository.findById(req.equipeId())
                .orElseThrow(() -> new AppException("A pessoa escolhida não está no cadastro da equipe.", HttpStatus.UNPROCESSABLE_ENTITY));
        } else {
            pessoa = equipeService.porNome(req.nome(), null).orElseGet(() -> {
                // Pessoa nova: a função e a diária desta escala viram o padrão dela no cadastro.
                EquipeEntity nova = new EquipeEntity();
                nova.setNome(req.nome().trim());
                nova.setFuncaoPadrao(req.funcao());
                nova.setDiariaPadrao(req.valor());
                nova.setCriadoPor(Usuario.atual());
                return nova;
            });
        }
        // Pessoa já cadastrada: telefone e Pix informados agora atualizam o cadastro; vazios mantêm o que havia.
        if (!Formato.vazio(req.contato()) || pessoa.getId() == null) {
            pessoa.setContato(Formato.texto(req.contato()).trim());
        }
        if (!Formato.vazio(req.pix()) || pessoa.getId() == null) {
            pessoa.setPix(Formato.texto(req.pix()).trim());
        }
        if (pessoa.getId() != null && escalaRepository.existsByEventoIdAndPessoaId(eventoId, pessoa.getId())) {
            throw new AppException(pessoa.getNome() + " já está na escala deste evento.", HttpStatus.CONFLICT);
        }
        equipeRepository.save(pessoa);
        EquipeEventoEntity escala = new EquipeEventoEntity();
        escala.setEvento(evento);
        escala.setPessoa(pessoa);
        escala.setCriadoPor(Usuario.atual());
        aplicar(req, escala);
        return resposta(escalaRepository.save(escala));
    }

    /**
     * Edita a pessoa na escala. Nome, telefone e Pix mudam no cadastro da pessoa (valem para os
     * outros eventos); equipeId é ignorado. A conta a pagar ainda não paga acompanha a diária.
     */
    @Transactional
    public PessoaEquipeResponseDTO atualizar(Long eventoId, Long escalaId, PessoaEquipeRequestDTO req) {
        EquipeEventoEntity escala = daEscala(eventoId, escalaId);
        boolean pago = pago(escala.getLancamento());
        if (pago && req.valor().compareTo(escala.getValor()) != 0) {
            throw new AppException("A diária já foi paga; o valor não pode mudar aqui.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        EquipeEntity pessoa = escala.getPessoa();
        if (equipeService.porNome(req.nome(), pessoa.getId()).isPresent()) {
            throw new AppException("Já existe na equipe outra pessoa com o nome " + req.nome().trim() + ".", HttpStatus.CONFLICT);
        }
        pessoa.setNome(req.nome().trim());
        pessoa.setContato(Formato.texto(req.contato()).trim());
        pessoa.setPix(Formato.texto(req.pix()).trim());
        aplicar(req, escala);
        LancamentoEntity l = escala.getLancamento();
        if (l != null && !pago) {
            l.setValor(escala.getValor());
            l.setDescricao(descricao(escala));
            l.setFornecedor(pessoa.getNome());
            l.setAlteradoPor(Usuario.atual());
            l.setAlteradoEm(Instant.now());
        }
        return resposta(escala);
    }

    /** Tira da escala (a pessoa continua no cadastro da equipe); a conta a pagar ainda não paga sai junto. */
    @Transactional
    public void apagar(Long eventoId, Long escalaId) {
        EquipeEventoEntity escala = daEscala(eventoId, escalaId);
        if (pago(escala.getLancamento())) {
            throw new AppException("A diária desta pessoa já foi paga, por isso ela não pode sair da escala.", HttpStatus.CONFLICT);
        }
        tirar(List.of(escala));
    }

    /**
     * Escala em lote pelo checklist: a lista recebida é a escala inteira. Quem entra usa a função e
     * a diária padrão do cadastro (sem horários); quem sai leva junto a conta a pagar não paga.
     * Tudo ou nada: se alguém com diária paga sair da lista, nada muda.
     */
    @Transactional
    public AssociarEquipeResponseDTO associar(Long eventoId, List<Long> equipeIds) {
        EventoEntity evento = cadastro.evento(eventoId);
        Set<Long> ids = new LinkedHashSet<>(equipeIds);
        Map<Long, EquipeEntity> pessoas = equipeRepository.findAllById(ids).stream().collect(Collectors.toMap(EquipeEntity::getId, Function.identity()));
        if (pessoas.size() != ids.size()) {
            throw new AppException("Alguém marcado não está mais no cadastro da equipe. Feche e abra a lista de novo.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        List<EquipeEventoEntity> atuais = escalaRepository.findByEventoIdOrderByIdAsc(eventoId);
        List<EquipeEventoEntity> saem = atuais.stream().filter(x -> !ids.contains(x.getPessoa().getId())).toList();
        List<String> pagos = saem.stream().filter(x -> pago(x.getLancamento())).map(x -> x.getPessoa().getNome()).toList();
        if (!pagos.isEmpty()) {
            throw new AppException(String.join(", ", pagos) + (pagos.size() > 1 ? " têm" : " tem") + " diária paga, por isso não pode sair da escala.",
                HttpStatus.CONFLICT);
        }
        tirar(saem);
        Set<Long> jaNaEscala = atuais.stream().map(x -> x.getPessoa().getId()).collect(Collectors.toSet());
        String usuario = Usuario.atual();
        List<EquipeEventoEntity> entram = ids.stream().filter(id -> !jaNaEscala.contains(id)).map(id -> {
            EquipeEntity p = pessoas.get(id);
            EquipeEventoEntity x = new EquipeEventoEntity();
            x.setEvento(evento);
            x.setPessoa(p);
            x.setFuncao(p.getFuncaoPadrao());
            x.setValor(p.getDiariaPadrao());
            x.setCriadoPor(usuario);
            return x;
        }).toList();
        escalaRepository.saveAll(entram);
        return new AssociarEquipeResponseDTO(entram.size(), saem.size());
    }

    /** Diárias com valor e ainda fora do caixa viram contas a pagar previstas, com vencimento no dia do evento. */
    @Transactional
    public LancarEquipeResponseDTO lancar(Long eventoId) {
        EventoEntity evento = cadastro.evento(eventoId);
        LocalDate data = evento.getDataEvento() != null ? evento.getDataEvento() : LocalDate.now();
        List<EquipeEventoEntity> pendentes = escalaRepository.findByEventoIdOrderByIdAsc(eventoId).stream()
            .filter(x -> x.getLancamento() == null && x.getValor().signum() > 0).toList();
        if (pendentes.isEmpty()) {
            return new LancarEquipeResponseDTO(0, data.toString());
        }
        CategoriaResponseDTO c = Caixa.categoriaEquipe(dadosService.carregar(EnumSet.of(Parte.CATEGORIAS)));
        if (c == null) {
            throw new AppException(SEM_CATEGORIA, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        CategoriaFinanceiraEntity categoria = cadastro.categoriaDoCorpo(c.id(), SEM_CATEGORIA);
        for (EquipeEventoEntity x : pendentes) {
            LancamentoEntity l = CaixaService.novoLancamento(categoria, evento, descricao(x), x.getValor(), data, "equipe");
            l.setVencimento(data);
            l.setUnidade("buffet");
            l.setConta("banco");
            l.setFornecedor(x.getPessoa().getNome());
            l.setFormaPag(Formato.vazio(x.getPessoa().getPix()) ? null : "pix");
            x.setLancamento(lancamentoRepository.save(l));
        }
        return new LancarEquipeResponseDTO(pendentes.size(), data.toString());
    }

    /* ------------------------------- Apoio ------------------------------- */

    /** Tira da escala; a conta a pagar (não paga) sai depois da linha que aponta para ela. */
    private void tirar(List<EquipeEventoEntity> escalas) {
        List<LancamentoEntity> contas = escalas.stream().map(EquipeEventoEntity::getLancamento).filter(l -> l != null).toList();
        escalaRepository.deleteAll(escalas);
        escalaRepository.flush();
        lancamentoRepository.deleteAll(contas);
    }

    private EquipeEventoEntity daEscala(Long eventoId, Long escalaId) {
        cadastro.evento(eventoId);
        return escalaRepository.findByIdAndEventoId(escalaId, eventoId)
            .orElseThrow(() -> new AppException("Esta pessoa não está na escala do evento.", HttpStatus.NOT_FOUND));
    }

    private static void aplicar(PessoaEquipeRequestDTO req, EquipeEventoEntity escala) {
        escala.setFuncao(req.funcao());
        escala.setChegada(Formato.horaOpcional(req.chegada()));
        escala.setSaida(Formato.horaOpcional(req.saida()));
        escala.setValor(req.valor());
    }

    static String descricao(EquipeEventoEntity x) {
        return CaixaService.descricao("Equipe · " + x.getPessoa().getNome() + " (" + x.getFuncao() + ") · " + x.getEvento().getNome());
    }

    /** Diária paga no todo (realizado) ou em parte (algum pagamento registrado). */
    static boolean pago(LancamentoEntity l) {
        return l != null && ("realizado".equals(l.getStatus()) || CaixaService.pagoTotal(l).compareTo(BigDecimal.ZERO) > 0);
    }

    static PessoaEquipeResponseDTO resposta(EquipeEventoEntity x) {
        EquipeEntity p = x.getPessoa();
        LancamentoEntity l = x.getLancamento();
        return new PessoaEquipeResponseDTO(x.getId(), p.getId(), p.getNome(), x.getFuncao(), Formato.hora(x.getChegada()), Formato.hora(x.getSaida()),
            x.getValor().doubleValue(), p.getContato(), p.getPix(), l == null ? null : l.getId(), l == null ? null : l.getStatus(), pago(l));
    }
}

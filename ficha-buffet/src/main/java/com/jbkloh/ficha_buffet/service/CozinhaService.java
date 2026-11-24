package com.jbkloh.ficha_buffet.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Cozinha;
import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Estoque;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.calculo.Ordens;
import com.jbkloh.ficha_buffet.dtos.req.AvariaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.CriarOcorrenciaRequestDTO;
import com.jbkloh.ficha_buffet.dtos.req.SalvarExecucaoRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaProjecaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.CozinhaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LiberacaoExpedicaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OcorrenciaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.OrdemProducaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.SalvarExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.mapper.GestaoMapper;
import com.jbkloh.ficha_buffet.model.EventoEntity;
import com.jbkloh.ficha_buffet.model.ExecucaoCozinhaEntity;
import com.jbkloh.ficha_buffet.model.IngredientesEntity;
import com.jbkloh.ficha_buffet.model.ItemCardapioEntity;
import com.jbkloh.ficha_buffet.model.MovimentoEstoqueEntity;
import com.jbkloh.ficha_buffet.model.OcorrenciaEventoEntity;
import com.jbkloh.ficha_buffet.repositories.ExecucaoCozinhaRepository;
import com.jbkloh.ficha_buffet.repositories.ItemCardapioRepository;
import com.jbkloh.ficha_buffet.repositories.MovimentoEstoqueRepository;
import com.jbkloh.ficha_buffet.repositories.OcorrenciaEventoRepository;

import lombok.RequiredArgsConstructor;

/**
 * O que a cozinha vê (sem valores financeiros) e o que ela registra: tarefas, compras, checklist de envio,
 * transporte e ocorrências do evento.
 */
@Service
@RequiredArgsConstructor
public class CozinhaService {

    private static final Pattern CHAVE = Pattern.compile("(i|c|x|l|t|cp|tr)_(\\d+)(?:__(.+))?");
    private static final Map<String, String> PREFIXO = Map.of("item", "i", "comp", "c", "extra", "x", "louca", "l", "tarefa", "t", "compras", "cp",
        "transporte", "tr");
    private static final Set<String> NAO_NEGATIVOS = Set.of("produzido", "enviadoQ", "retorno", "saiu", "voltou", "avaria");
    private static final String SO_NUMEROS = "Use só números, ex.: 3,5";

    private final ExecucaoCozinhaRepository execucaoRepository;
    private final ItemCardapioRepository itemCardapioRepository;
    private final MovimentoEstoqueRepository movimentoRepository;
    private final OcorrenciaEventoRepository ocorrenciaRepository;
    private final Cadastro cadastro;
    private final DadosService dadosService;

    /**
     * Eventos em produção projetados para a cozinha, as execuções deles, as tarefas com os ajustes da cozinha
     * e, dos eventos com mapa gerado, as ordens de produção e a liberação para expedição (Painel do líder e aba Ordens).
     * As ocorrências vêm só dos eventos projetados, as mais recentes primeiro.
     */
    @Transactional(readOnly = true)
    public CozinhaResponseDTO retornarTudo() {
        Dados d = dadosService.carregar();
        String hoje = Formato.hoje();
        String agora = OrdemProducaoService.agoraHm();
        List<CozinhaProjecaoDTO> projecoes = Cozinha.projecoes(d, hoje);
        Set<Long> eventos = projecoes.stream().map(CozinhaProjecaoDTO::eventoId).collect(Collectors.toSet());
        List<OrdemProducaoDTO> ordens = new ArrayList<>();
        List<LiberacaoExpedicaoDTO> liberacoes = new ArrayList<>();
        for (CozinhaProjecaoDTO pj : projecoes) {
            if (pj.producao() == null) {
                continue;
            }
            List<OrdemProducaoDTO> os = Ordens.ordens(d, d.evento(pj.eventoId()), hoje, agora);
            ordens.addAll(os);
            liberacoes.add(Ordens.liberacao(d, pj, os));
        }
        List<OcorrenciaDTO> ocorrencias = ocorrenciaRepository.findAll(Sort.by(Sort.Direction.DESC, "quando", "id")).stream()
            .filter(o -> eventos.contains(o.getEvento().getId())).map(GestaoMapper::ocorrencia).toList();
        return new CozinhaResponseDTO(projecoes, d.execucoes.stream().filter(x -> eventos.contains(x.eventoId())).toList(),
            projecoes.stream().flatMap(p -> Cozinha.tarefas(d, p).stream()).toList(), ordens, liberacoes, ocorrencias, d.setores);
    }

    /* ------------------------------ Ocorrências ------------------------------ */

    /** Registra o que fugiu do planejado no evento, com o instante do registro. */
    @Transactional
    public OcorrenciaDTO criarOcorrencia(CriarOcorrenciaRequestDTO req) {
        OcorrenciaEventoEntity o = new OcorrenciaEventoEntity();
        o.setEvento(cadastro.evento(req.eventoId()));
        o.setTipo(req.tipo());
        o.setQuem(req.quem().trim());
        o.setTexto(req.texto().trim());
        o.setAcao(Formato.texto(req.acao()).trim());
        o.setQuando(Instant.now());
        return GestaoMapper.ocorrencia(ocorrenciaRepository.save(o));
    }

    /** Marca a ocorrência como resolvida ou reabre. */
    @Transactional
    public OcorrenciaDTO resolverOcorrencia(Long id, boolean resolvida) {
        OcorrenciaEventoEntity o = ocorrenciaRepository.findById(id)
            .orElseThrow(() -> new AppException("Ocorrência não encontrada.", HttpStatus.NOT_FOUND));
        o.setResolvida(resolvida);
        return GestaoMapper.ocorrencia(o);
    }

    /**
     * Grava os campos tocados (mescla com o registro atual). Regras do checklist:
     * produção carimba a data (e a baixa no estoque passa a valer); envio de
     * prato marca enviado; se a linha deixa de cumprir as condições de saída,
     * a quantidade enviada é apagada.
     */
    @Transactional
    public SalvarExecucaoResponseDTO salvarExecucao(SalvarExecucaoRequestDTO req) {
        EventoEntity evento = cadastro.evento(req.eventoId());
        Map<String, Object> campos = new HashMap<>(req.campos());
        ExecucaoCozinhaEntity x = execucaoRepository.findByEventoId(evento.getId()).stream()
            .filter(e -> GestaoMapper.execucao(e).chave().equals(req.chave())).findFirst().orElse(null);
        ExecucaoResponseDTO antes = x == null ? null : GestaoMapper.execucao(x);
        if (x == null) {
            x = nova(evento, req);
        }

        Boolean baixaAlterada = null;
        if (campos.containsKey("produzido")) {
            Double produzido = numero("produzido", campos.get("produzido"));
            boolean produziu = produzido != null && produzido > 0;
            x.setProduzidoEm(produziu ? LocalDate.now() : null);
            if ("item".equals(req.kind())) {
                baixaAlterada = produziu;
            }
        }
        for (Map.Entry<String, Object> campo : campos.entrySet()) {
            aplicarCampo(x, campo.getKey(), campo.getValue());
        }

        boolean envioApagado = false;
        if (Set.of("item", "comp", "extra", "louca").contains(req.kind())) {
            Dados d = dadosService.carregar();
            CozinhaProjecaoDTO pj = Cozinha.projecao(d, d.evento(evento.getId()), Estoque.calcular(d));
            Cozinha.Linha linha = Cozinha.linhas(pj).stream().filter(r -> r.id().equals(req.chave())).findFirst().orElse(null);
            if (linha != null) {
                boolean prato = "prato".equals(linha.tipo());
                boolean tocouEnvio = campos.containsKey(linha.campoEnvio());
                if (tocouEnvio && prato) {
                    marcarEnvio(x, x.getEnviadoQ() != null && x.getEnviadoQ() > 0);
                }
                Double enviadoAntes = antes == null ? null : Cozinha.enviado(linha, antes);
                if (!tocouEnvio && enviadoAntes != null && enviadoAntes > 0 && !Cozinha.podeSair(linha, GestaoMapper.execucao(x))) {
                    if ("saiu".equals(linha.campoEnvio())) {
                        x.setSaiu(null);
                    } else {
                        x.setEnviadoQ(null);
                    }
                    if (prato) {
                        marcarEnvio(x, false);
                    }
                    envioApagado = true;
                }
            }
        }
        x.setAtualizadoEm(Instant.now());
        return new SalvarExecucaoResponseDTO(GestaoMapper.execucao(execucaoRepository.save(x)), envioApagado, baixaAlterada);
    }

    private static void marcarEnvio(ExecucaoCozinhaEntity x, boolean enviado) {
        x.setEnviado(enviado);
        x.setEnviadoEm(enviado ? Instant.now() : null);
    }

    /** Registro novo: as referências (prato do cardápio, item, linha) vêm da própria chave. */
    private ExecucaoCozinhaEntity nova(EventoEntity evento, SalvarExecucaoRequestDTO req) {
        Matcher m = CHAVE.matcher(req.chave());
        if (!m.matches() || !m.group(1).equals(PREFIXO.get(req.kind()))) {
            throw new AppException("Chave do registro inválida.", HttpStatus.BAD_REQUEST);
        }
        long numero = Long.parseLong(m.group(2));
        String resto = m.group(3);
        ExecucaoCozinhaEntity x = new ExecucaoCozinhaEntity();
        x.setEvento(evento);
        x.setKind(req.kind());
        switch (req.kind()) {
            case "item", "comp" -> {
                ItemCardapioEntity item = itemCardapioRepository.findById(numero).filter(c -> c.getEvento().getId().equals(evento.getId()))
                    .orElseThrow(() -> new AppException("Este prato não está mais no cardápio do evento.", HttpStatus.UNPROCESSABLE_ENTITY));
                x.setItemCardapio(item);
                x.setReferencia("comp".equals(req.kind()) ? referencia(resto) : null);
            }
            case "louca" -> {
                exigirEvento(numero, evento);
                x.setIngrediente(cadastro.ingredienteDoCorpo(resto != null && resto.matches("\\d+") ? Long.parseLong(resto) : -1L,
                    "Esta louça não existe mais no cadastro."));
            }
            case "extra", "tarefa" -> {
                exigirEvento(numero, evento);
                x.setReferencia(referencia(resto));
            }
            default -> exigirEvento(numero, evento);
        }
        return x;
    }

    private static void exigirEvento(long numero, EventoEntity evento) {
        if (numero != evento.getId()) {
            throw new AppException("Chave do registro inválida.", HttpStatus.BAD_REQUEST);
        }
    }

    private static String referencia(String resto) {
        if (Formato.vazio(resto) || resto.length() > 60) {
            throw new AppException("Chave do registro inválida.", HttpStatus.BAD_REQUEST);
        }
        return resto;
    }

    private static Double numero(String campo, Object v) {
        if (v == null) {
            return null;
        }
        if (!(v instanceof Number n) || (n.doubleValue() < 0 && NAO_NEGATIVOS.contains(campo))) {
            throw new AppException(SO_NUMEROS, HttpStatus.BAD_REQUEST);
        }
        return n.doubleValue();
    }

    private static String texto(Object v, int max) {
        if (v != null && !(v instanceof String)) {
            throw new AppException("Texto inválido.", HttpStatus.BAD_REQUEST);
        }
        String s = v == null ? "" : ((String) v).trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static boolean logico(Object v) {
        if (!(v instanceof Boolean b)) {
            throw new AppException("Valor inválido.", HttpStatus.BAD_REQUEST);
        }
        return b;
    }

    private static void aplicarCampo(ExecucaoCozinhaEntity x, String campo, Object v) {
        switch (campo) {
            case "produzido" -> x.setProduzido(numero(campo, v));
            case "enviadoQ" -> x.setEnviadoQ(numero(campo, v));
            case "retorno" -> x.setRetorno(numero(campo, v));
            case "tempSaida" -> x.setTempSaida(numero(campo, v));
            case "tempChegada" -> x.setTempChegada(numero(campo, v));
            case "saiu" -> x.setSaiu(numero(campo, v));
            case "voltou" -> x.setVoltou(numero(campo, v));
            case "avaria" -> x.setAvaria(numero(campo, v));
            case "dn" -> {
                Double dn = numero(campo, v);
                if (dn != null && (dn < 0 || dn != Math.rint(dn))) {
                    throw new AppException(SO_NUMEROS, HttpStatus.BAD_REQUEST);
                }
                x.setDn(dn == null ? null : dn.intValue());
            }
            case "dnSet" -> x.setDnSet(logico(v));
            case "feito" -> x.setFeito(logico(v));
            case "executadoPor" -> x.setExecutadoPor(texto(v, 100));
            case "separadoPor" -> x.setSeparadoPor(texto(v, 100));
            case "conferidoPor" -> x.setConferidoPor(texto(v, 100));
            case "obs" -> x.setObs(texto(v, 300));
            case "resp" -> x.setResp(texto(v, 100));
            case "chefPor" -> x.setChefPor(texto(v, 100));
            case "cargaPor" -> x.setCargaPor(texto(v, 100));
            case "recebPor" -> x.setRecebPor(texto(v, 100));
            case "retornoPor" -> x.setRetornoPor(texto(v, 100));
            case "veiculo" -> x.setVeiculo(texto(v, 100));
            case "chefHora" -> x.setChefHora(Formato.horaOpcional(texto(v, 5)));
            case "cargaHora" -> x.setCargaHora(Formato.horaOpcional(texto(v, 5)));
            case "recebHora" -> x.setRecebHora(Formato.horaOpcional(texto(v, 5)));
            case "retornoHora" -> x.setRetornoHora(Formato.horaOpcional(texto(v, 5)));
            case "itens" -> {
                if (!(v instanceof Map<?, ?> itens) || itens.values().stream().anyMatch(b -> !(b instanceof Boolean))) {
                    throw new AppException("Lista de compras inválida.", HttpStatus.BAD_REQUEST);
                }
                Map<String, Boolean> comprados = new HashMap<>();
                itens.forEach((k, b) -> comprados.put(String.valueOf(k), (Boolean) b));
                x.setItensComprados(comprados);
            }
            default -> throw new AppException("Campo desconhecido: " + campo, HttpStatus.BAD_REQUEST);
        }
    }

    /** Louça que não voltou: lança a perda no estoque e registra a avaria no checklist. Devolve a quantidade lançada. */
    @Transactional
    public double lancarAvaria(AvariaRequestDTO req) {
        EventoEntity evento = cadastro.evento(req.eventoId());
        IngredientesEntity item = cadastro.ingrediente(req.insumoId());
        ExecucaoCozinhaEntity x = execucaoRepository.findByEventoId(evento.getId()).stream()
            .filter(e -> "louca".equals(e.getKind()) && e.getIngrediente() != null && e.getIngrediente().getId().equals(item.getId())).findFirst().orElse(null);
        double falta = x == null ? 0 : valor(x.getSaiu()) - valor(x.getVoltou()) - valor(x.getAvaria());
        if (!(falta > 0)) {
            throw new AppException("Não há falta para lançar.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        MovimentoEstoqueEntity perda = MovimentoEstoqueService.novo(item, "perda", -falta, falta, LocalDate.now(),
            "Avaria/perda no evento " + evento.getCodigo() + " · " + evento.getNome(), "checklist");
        perda.setEvento(evento);
        movimentoRepository.save(perda);
        x.setAvaria(valor(x.getAvaria()) + falta);
        x.setAtualizadoEm(Instant.now());
        return falta;
    }

    private static double valor(Double v) {
        return v == null ? 0 : v;
    }
}

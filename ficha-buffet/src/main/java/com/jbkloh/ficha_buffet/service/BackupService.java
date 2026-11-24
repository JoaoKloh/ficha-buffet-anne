package com.jbkloh.ficha_buffet.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.req.BackupArquivoDTO;
import com.jbkloh.ficha_buffet.dtos.req.RestaurarBackupRequestDTO;
import com.jbkloh.ficha_buffet.dtos.resp.AnaliseBackupResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.BackupResumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.RestaurarBackupResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.UltimoBackupDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;
import com.jbkloh.ficha_buffet.model.RegistroSistemaEntity;
import com.jbkloh.ficha_buffet.repositories.RegistroSistemaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Backup e restauração pelo próprio sistema. A restauração nunca apaga: só
 * cria o que falta e, se pedido, volta registros alterados à versão do backup.
 * Depois confere registro por registro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private static final String APP = "Anne Gestão";

    /** Coleções do arquivo de backup e o rótulo de cada uma, na ordem em que aparecem na tela. */
    private static final Map<String, String> ROTULOS = rotulos();

    private final DadosService dadosService;
    private final RegistroSistemaRepository registroRepository;
    private final RestauracaoBackup restauracao;
    private final ObjectMapper json;

    private static Map<String, String> rotulos() {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("clientes", "Clientes");
        r.put("eventos", "Eventos");
        r.put("parcelas", "Parcelas");
        r.put("extras", "Extras");
        r.put("lancamentos", "Lançamentos");
        r.put("categorias", "Categorias");
        r.put("insumos", "Insumos");
        r.put("preparacoes", "Fichas técnicas");
        r.put("cardapio", "Itens de cardápio");
        r.put("movimentos", "Movimentos de estoque");
        r.put("nfs", "Notas fiscais");
        r.put("pratos_venda", "Pratos do restaurante");
        r.put("lotes", "Lotes de pré-preparo");
        r.put("cozinha_exec", "Registros do checklist");
        return java.util.Collections.unmodifiableMap(r);
    }

    private static List<?> registros(Dados d, String colecao) {
        return switch (colecao) {
            case "clientes" -> d.clientes;
            case "eventos" -> d.eventos;
            case "parcelas" -> d.parcelas;
            case "extras" -> d.extras;
            case "lancamentos" -> d.lancamentos;
            case "categorias" -> d.categorias;
            case "insumos" -> d.insumos;
            case "preparacoes" -> d.preparacoes;
            case "cardapio" -> d.cardapio;
            case "movimentos" -> d.movimentos;
            case "nfs" -> d.nfs;
            case "pratos_venda" -> d.pratosVenda;
            case "lotes" -> d.lotes;
            default -> d.execucoes;
        };
    }

    /** Cada coleção como { id: registro sem o id }, o formato do arquivo de backup. */
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Map<String, Object>>> documentos(Dados d) {
        Map<String, Map<String, Map<String, Object>>> colecoes = new LinkedHashMap<>();
        for (String colecao : ROTULOS.keySet()) {
            Map<String, Map<String, Object>> docs = new LinkedHashMap<>();
            for (Object registro : registros(d, colecao)) {
                Map<String, Object> doc = new LinkedHashMap<>(json.convertValue(registro, Map.class));
                docs.put(String.valueOf(doc.remove("id")), doc);
            }
            colecoes.put(colecao, docs);
        }
        return colecoes;
    }

    /** Quantos registros há por coleção e quando foi o último backup completo. */
    @Transactional(readOnly = true)
    public BackupResumoResponseDTO resumo() {
        Dados d = dadosService.carregar();
        List<BackupResumoResponseDTO.Contagem> contagens = ROTULOS.entrySet().stream()
            .map(c -> new BackupResumoResponseDTO.Contagem(c.getKey(), c.getValue(), registros(d, c.getKey()).size())).toList();
        return new BackupResumoResponseDTO(ultimoBackup(), contagens, contagens.stream().mapToInt(BackupResumoResponseDTO.Contagem::n).sum());
    }

    /** Cópia exata de tudo, que permite restaurar. */
    @Transactional(readOnly = true)
    public BackupArquivoDTO exportar() {
        return new BackupArquivoDTO(APP, Formato.agora(), documentos(dadosService.carregar()));
    }

    /** Marca que um backup completo (.json) acabou de ser salvo. */
    @Transactional
    public UltimoBackupDTO registrar() {
        Dados d = dadosService.carregar();
        Map<String, Integer> contagens = new LinkedHashMap<>();
        ROTULOS.keySet().forEach(c -> contagens.put(c, registros(d, c).size()));
        UltimoBackupDTO ultimo = new UltimoBackupDTO(Formato.agora(), "json", contagens, contagens.values().stream().mapToInt(Integer::intValue).sum());
        gravarRegistro("backup", Map.of("ultimoEm", ultimo.ultimoEm(), "tipo", ultimo.tipo(), "contagens", contagens, "total", ultimo.total()));
        return ultimo;
    }

    @Transactional(readOnly = true)
    public UltimoBackupDTO ultimoBackup() {
        return registroRepository.findById("backup").map(r -> json.convertValue(r.getDados(), UltimoBackupDTO.class)).orElse(null);
    }

    /** Planilha .xlsx para consultar os dados (não serve para restaurar). */
    @Transactional(readOnly = true)
    public byte[] planilha() {
        return PlanilhaBackup.gerar(dadosService.carregar());
    }

    private void gravarRegistro(String chave, Map<String, Object> dados) {
        RegistroSistemaEntity registro = registroRepository.findById(chave).orElseGet(() -> new RegistroSistemaEntity(chave));
        registro.setDados(new HashMap<>(dados));
        registro.setAtualizadoEm(Instant.now());
        registroRepository.save(registro);
    }

    /* ------------------------------ Análise e restauração ------------------------------ */

    /** Comparação de uma coleção do arquivo com o sistema: ids novos, diferentes e contagens. */
    private record Comparacao(String colecao, int total, List<String> novos, List<String> diferentes, int iguais, int soNoSistema) {}

    private record Analise(List<Comparacao> linhas, List<String> ignoradas, int invalidos) {}

    /** Forma canônica de um valor JSON: chaves em ordem, números sem zeros à direita, nulo igual a ausente. */
    static String canonico(Object v) {
        if (v instanceof Map<?, ?> m) {
            return new TreeMap<>(m.entrySet().stream().filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> canonico(e.getValue())))).toString();
        }
        if (v instanceof List<?> l) {
            return l.stream().map(BackupService::canonico).toList().toString();
        }
        if (v instanceof Number n) {
            BigDecimal d = new BigDecimal(n.toString()).stripTrailingZeros();
            return d.signum() == 0 ? "0" : d.toPlainString();
        }
        return v == null ? "null" : v instanceof String s ? "\"" + s.replace("\"", "\\\"") + "\"" : v.toString();
    }

    /** Compara o arquivo com o que está gravado, sem alterar nada. */
    private Analise comparar(Dados d, BackupArquivoDTO arquivo) {
        if (arquivo.app() != null && !arquivo.app().isEmpty() && !APP.equals(arquivo.app())) {
            throw new AppException("Este backup é de outro sistema (" + arquivo.app().substring(0, Math.min(40, arquivo.app().length())) + ").",
                HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Map<String, Map<String, Map<String, Object>>> atuais = documentos(d);
        List<Comparacao> linhas = new ArrayList<>();
        List<String> ignoradas = new ArrayList<>();
        int invalidos = 0;
        for (Map.Entry<String, Map<String, Map<String, Object>>> colecao : arquivo.colecoes().entrySet()) {
            Map<String, Map<String, Object>> atual = atuais.get(colecao.getKey());
            Map<String, Map<String, Object>> docs = colecao.getValue() == null ? Map.of() : colecao.getValue();
            if (atual == null) {
                ignoradas.add(colecao.getKey());
                continue;
            }
            List<String> novos = new ArrayList<>();
            List<String> diferentes = new ArrayList<>();
            int iguais = 0;
            for (Map.Entry<String, Map<String, Object>> doc : docs.entrySet()) {
                if (doc.getValue() == null || !doc.getKey().matches("[1-9]\\d{0,14}")) {
                    invalidos++;
                } else if (!atual.containsKey(doc.getKey())) {
                    novos.add(doc.getKey());
                } else if (canonico(atual.get(doc.getKey())).equals(canonico(semId(doc.getValue())))) {
                    iguais++;
                } else {
                    diferentes.add(doc.getKey());
                }
            }
            linhas.add(new Comparacao(colecao.getKey(), docs.size(), novos, diferentes, iguais, (int) atual.keySet().stream().filter(id -> !docs.containsKey(id)).count()));
        }
        return new Analise(linhas, ignoradas, invalidos);
    }

    private static Map<String, Object> semId(Map<String, Object> doc) {
        Map<String, Object> corpo = new HashMap<>(doc);
        corpo.remove("id");
        return corpo;
    }

    /** Lê e confere o arquivo de backup contra o sistema, sem gravar nada. */
    @Transactional(readOnly = true)
    public AnaliseBackupResponseDTO analisar(BackupArquivoDTO arquivo) {
        Analise analise = comparar(dadosService.carregar(), arquivo);
        return new AnaliseBackupResponseDTO(Formato.texto(arquivo.exportadoEm()),
            analise.linhas().stream().map(l -> new AnaliseBackupResponseDTO.Linha(l.colecao(), ROTULOS.get(l.colecao()), l.total(), l.novos().size(),
                l.diferentes().size(), l.iguais(), l.soNoSistema())).toList(),
            analise.ignoradas(), analise.invalidos());
    }

    /**
     * Restaura do backup: cria o que falta e, no modo "todos", volta os
     * registros diferentes. Cada registro é gravado na própria transação; o
     * que falhar na primeira passada (por depender de outro ainda não gravado)
     * é tentado de novo no fim. Sem transação aqui de propósito.
     */
    public RestaurarBackupResponseDTO restaurar(RestaurarBackupRequestDTO req) {
        Map<String, Comparacao> porColecao = comparar(dadosService.carregar(), req.arquivo()).linhas().stream()
            .collect(Collectors.toMap(Comparacao::colecao, l -> l));
        boolean todos = "todos".equals(req.modo());
        List<String[]> alvos = new ArrayList<>(); // [coleção, id]
        for (String colecao : RestauracaoBackup.TABELAS.keySet()) {
            Comparacao c = porColecao.get(colecao);
            if (c != null) {
                c.novos().forEach(id -> alvos.add(new String[] { colecao, id }));
                if (todos) {
                    c.diferentes().forEach(id -> alvos.add(new String[] { colecao, id }));
                }
            }
        }
        List<String[]> pendentes = gravar(req.arquivo(), alvos);
        if (!pendentes.isEmpty() && pendentes.size() < alvos.size()) {
            pendentes = gravar(req.arquivo(), pendentes);
        }
        alvos.stream().map(a -> a[0]).distinct().forEach(restauracao::ajustarSequencia);

        Map<String, Map<String, Map<String, Object>>> depois = documentos(dadosService.carregar());
        int conferidos = 0;
        for (String[] alvo : alvos) {
            Map<String, Object> atual = depois.get(alvo[0]).get(alvo[1]);
            if (atual != null && canonico(atual).equals(canonico(semId(req.arquivo().colecoes().get(alvo[0]).get(alvo[1]))))) {
                conferidos++;
            }
        }
        List<RestaurarBackupResponseDTO.Falha> falhas = pendentes.stream()
            .map(a -> new RestaurarBackupResponseDTO.Falha(a[0], ROTULOS.get(a[0]), a[1])).toList();
        int gravados = alvos.size() - falhas.size();
        gravarRegistro("restauracao", Map.of("em", Formato.agora(), "arquivo", Formato.texto(req.nomeArquivo()).trim(),
            "exportadoEm", Formato.texto(req.arquivo().exportadoEm()), "modo", req.modo(), "gravados", gravados, "falhas", falhas.size()));
        return new RestaurarBackupResponseDTO(alvos.size(), gravados, falhas, conferidos);
    }

    /** Grava os registros pedidos e devolve os que falharam. */
    private List<String[]> gravar(BackupArquivoDTO arquivo, List<String[]> alvos) {
        List<String[]> falhas = new ArrayList<>();
        for (String[] alvo : alvos) {
            try {
                restauracao.gravar(alvo[0], Long.valueOf(alvo[1]), arquivo.colecoes().get(alvo[0]).get(alvo[1]));
            } catch (RuntimeException e) {
                log.warn("Restauração: falha em {} {}: {}", alvo[0], alvo[1], e.getMessage());
                falhas.add(alvo);
            }
        }
        return falhas;
    }
}

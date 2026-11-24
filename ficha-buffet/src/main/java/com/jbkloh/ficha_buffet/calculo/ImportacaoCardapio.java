package com.jbkloh.ficha_buffet.calculo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.jbkloh.ficha_buffet.dtos.resp.ImportarCardapioAnaliseResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ImportarCardapioAnaliseResponseDTO.Linha;
import com.jbkloh.ficha_buffet.dtos.resp.ImportarCardapioAnaliseResponseDTO.Sugestao;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;

/** Casa cada linha do cardápio colado com as fichas técnicas. */
public final class ImportacaoCardapio {

    private ImportacaoCardapio() {}

    private static final Set<String> IGNORADAS = Set.of("de", "da", "do", "das", "dos", "com", "ao", "aos", "a", "o", "as", "os", "e", "em", "na", "no",
        "nas", "nos", "mini", "molho", "sobre");

    private static List<String> palavras(String s) {
        return Arrays.stream(Formato.normTxt(s).split(" ")).filter(w -> !w.isEmpty() && !IGNORADAS.contains(w)).toList();
    }

    private static boolean parecida(String a, String b) {
        return a.equals(b) || (a.length() > 4 && b.startsWith(a.substring(0, a.length() - 1))) || (b.length() > 4 && a.startsWith(b.substring(0, b.length() - 1)));
    }

    /** As 3 fichas mais parecidas com a linha, da melhor para a pior. */
    public static List<Sugestao> sugestoes(Dados d, String linha) {
        List<String> a = palavras(linha);
        if (a.isEmpty()) {
            return List.of();
        }
        String na = Formato.normTxt(linha);
        return d.preparacoes.stream().map(p -> {
            List<String> b = palavras(p.nome());
            String nb = Formato.normTxt(p.nome());
            long comuns = a.stream().filter(w -> b.stream().anyMatch(x -> parecida(w, x))).count();
            double score = 2.0 * comuns / (a.size() + b.size());
            if (na.equals(nb)) {
                score = 1;
            } else if (nb.contains(na) || na.contains(nb)) {
                score = Math.max(score, 0.9);
            }
            return new Sugestao(p.id(), p.nome(), score, Fichas.fichaStatus(d, p));
        }).filter(x -> x.score() > 0).sorted(Comparator.comparingDouble(Sugestao::score).reversed()).limit(3).toList();
    }

    public static ImportarCardapioAnaliseResponseDTO analisar(Dados d, Long eventoId, String texto) {
        List<String> linhas = new ArrayList<>(Arrays.stream(texto.split("\\r?\\n"))
            .map(l -> l.replaceFirst("^[\\s\\-•*·–—\\d.)]+", "").trim()).filter(l -> !l.isEmpty()).toList());
        String cabecalho = null;
        if (!linhas.isEmpty() && (linhas.get(0).toLowerCase().matches(".*\\bconvidad.*") || linhas.get(0).matches(".*\\d{1,2}/\\d{1,2}.*"))) {
            cabecalho = linhas.remove(0);
        }
        Set<Long> jaNoCardapio = d.cardapioDe(eventoId).stream().map(ItemCardapioResponseDTO::preparacaoId).collect(Collectors.toSet());
        return new ImportarCardapioAnaliseResponseDTO(cabecalho, linhas.stream().map(linha -> {
            List<Sugestao> sugestoes = sugestoes(d, linha);
            Sugestao melhor = sugestoes.isEmpty() ? null : sugestoes.get(0);
            String situacao = "nao";
            if (melhor != null && melhor.score() >= 0.85) {
                situacao = jaNoCardapio.contains(melhor.preparacaoId()) ? "ja" : "ok";
            } else if (melhor != null && melhor.score() >= 0.4) {
                situacao = "parecido";
            }
            return new Linha(linha, situacao, sugestoes);
        }).toList());
    }
}

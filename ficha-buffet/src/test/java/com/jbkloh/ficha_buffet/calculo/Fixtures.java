package com.jbkloh.ficha_buffet.calculo;

import java.util.HashMap;
import java.util.List;

import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ExecucaoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LoteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.MovimentoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ParcelaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;
import com.jbkloh.ficha_buffet.model.json.ComponenteEnvio;
import com.jbkloh.ficha_buffet.model.json.EtapaPreparacao;

/** Fábricas de registros para os testes dos cálculos. */
final class Fixtures {

    private Fixtures() {}

    static InsumoResponseDTO insumo(long id, String nome, String tipo, String unidade, String local, double estoqueMinimo, double custo) {
        return new InsumoResponseDTO(id, nome, tipo, "Mercearia", unidade, local, "", null, estoqueMinimo, custo, "", "2026-01-01");
    }

    static InsumoResponseDTO insumo(long id, double custo) {
        return insumo(id, "Insumo " + id, "ingrediente", "kg", "Seco", 0, custo);
    }

    static PreparacaoResponseDTO.Item item(Long insumoId, Long subId, double qtd, double fc) {
        return new PreparacaoResponseDTO.Item(insumoId, subId, qtd, fc);
    }

    static PreparacaoResponseDTO prep(long id, String nome, String categoria, double rendimento, String unidade, List<PreparacaoResponseDTO.Item> itens,
            List<PreparacaoResponseDTO.Louca> loucas, List<EtapaPreparacao> etapas, List<ComponenteEnvio> componentes) {
        return new PreparacaoResponseDTO(id, nome, categoria, "Salgados", "refrigerado", null, "", null, null, "", "", false, rendimento, unidade, 1, itens, loucas, etapas, componentes, "", "",
            "2026-01-01");
    }

    static PreparacaoResponseDTO prep(long id, String nome, String categoria, double rendimento, PreparacaoResponseDTO.Item... itens) {
        return prep(id, nome, categoria, rendimento, "un", List.of(itens), List.of(), List.of(), List.of());
    }

    static EventoResponseDTO evento(long id, String status, String dataEvento, Integer convidados, double margem, String observacoes,
            EventoResponseDTO.Contrato contrato) {
        return new EventoResponseDTO(id, "26-00" + id, "Evento " + id, 1L, "casamento", status, dataEvento, "", "", "", convidados, margem, observacoes, contrato,
            null, List.of(), null, "", "2026-01-01T10:00:00Z");
    }

    static EventoResponseDTO evento(long id, String status, int convidados) {
        return evento(id, status, "2026-12-12", convidados, 0, "", null);
    }

    static ItemCardapioResponseDTO cardapio(long id, long eventoId, long preparacaoId, double porcao, String status) {
        return new ItemCardapioResponseDTO(id, eventoId, preparacaoId, porcao, status, null, "");
    }

    static CategoriaResponseDTO categoria(long id, String grupo) {
        return new CategoriaResponseDTO(id, "Cat " + id, grupo, 0);
    }

    static LancamentoCaixaResponseDTO lanc(long id, long categoriaId, Long eventoId, double valor, String status, String dataCompetencia) {
        return new LancamentoCaixaResponseDTO(id, categoriaId == 1 ? "receita" : "despesa", categoriaId, eventoId, "x", valor, dataCompetencia, null, null, status,
            "manual", "banco", null, null, null, null, null, null, "2026-01-01T00:00:00Z", null, null, null, null, null, null, null, null, null, List.of(), null,
            null, null);
    }

    static MovimentoResponseDTO mov(long id, long insumoId, double qtd, String tipo) {
        return new MovimentoResponseDTO(id, tipo, insumoId, qtd, Math.abs(qtd), "kg", "2026-11-01", "", null, null, "manual", null, "", null, null, null);
    }

    static LoteResponseDTO lote(long id, long preparacaoId, double qtd, double saldo, String validade, String destino) {
        return new LoteResponseDTO(id, "L" + id, preparacaoId, qtd, saldo, "un", "2026-11-01", validade, destino, null, !"evento".equals(destino), "", "",
            List.of(), "", null);
    }

    static ParcelaResponseDTO parcela(long id, long eventoId, String vencimento, double valor, String pagoEm) {
        return new ParcelaResponseDTO(id, eventoId, 1, vencimento, valor, pagoEm, "", null, pagoEm == null ? 0 : valor);
    }

    /** Execução só com os campos que os cálculos olham. */
    static ExecucaoResponseDTO exec(String chave, String kind, long eventoId, Long cardapioId, Double produzido, String executadoPor, String separadoPor,
            Double tempSaida, Double enviadoQ, boolean enviado, Double saiu, Double voltou, Double avaria, boolean feito, String resp, boolean dnSet, Integer dn) {
        return new ExecucaoResponseDTO(1L, chave, kind, eventoId, cardapioId, null, null, produzido, produzido == null ? null : "2026-11-05", executadoPor, enviadoQ,
            enviado, null, separadoPor, null, "", "", tempSaida, null, saiu, voltou, avaria, feito, resp, dnSet, dn, new HashMap<>(), "", "", "", "", "", "", "", "",
            "", "");
    }

    static ExecucaoResponseDTO producao(long cardapioId, double produzido) {
        return exec("i_" + cardapioId, "item", 1, cardapioId, produzido, "", "", null, null, false, null, null, null, false, "", false, null);
    }

    static ExecucaoResponseDTO checklist(Double produzido, String executadoPor, String separadoPor, Double tempSaida) {
        return exec("i_100", "item", 1, 100L, produzido, executadoPor, separadoPor, tempSaida, null, false, null, null, null, false, "", false, null);
    }
}

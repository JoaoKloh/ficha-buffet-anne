package com.jbkloh.ficha_buffet.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.jbkloh.ficha_buffet.calculo.Caixa;
import com.jbkloh.ficha_buffet.calculo.Dados;
import com.jbkloh.ficha_buffet.calculo.Dominio;
import com.jbkloh.ficha_buffet.calculo.Estoque;
import com.jbkloh.ficha_buffet.calculo.Eventos;
import com.jbkloh.ficha_buffet.calculo.Formato;
import com.jbkloh.ficha_buffet.dtos.resp.CategoriaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.LancamentoCaixaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ClienteResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.EventoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.InsumoResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ItemCardapioResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.ParcelaResponseDTO;
import com.jbkloh.ficha_buffet.dtos.resp.PreparacaoResponseDTO;

/** Planilha de consulta do backup: uma aba por assunto, com os nomes já resolvidos. Não serve para restaurar. */
final class PlanilhaBackup {

    private PlanilhaBackup() {}

    private static List<Object> l(Object... celulas) {
        return Arrays.asList(celulas);
    }

    private static String data(String iso) {
        return Formato.vazio(iso) ? "" : Formato.fdate(iso);
    }

    private static Object ouVazio(Number n) {
        return n == null || n.doubleValue() == 0 ? "" : n;
    }

    static byte[] gerar(Dados d) {
        Estoque est = Estoque.calcular(d);
        List<Xlsx.Aba> abas = new ArrayList<>();

        abas.add(new Xlsx.Aba("Eventos", List.of("Código", "Evento", "Data", "Hora", "Local", "Cidade", "Tipo", "Status", "Convidados", "Margem %", "R$/convidado",
            "Valor total", "Recebido", "A receber", "Assinatura", "Cliente", "Observações", "ID"),
            d.eventos.stream().sorted(Comparator.comparing(EventoResponseDTO::dataEvento)).map(e -> {
                EventoResponseDTO.Contrato c = e.contrato();
                double pago = Formato.soma(d.parcelas.stream().filter(p -> e.id().equals(p.eventoId())).toList(), p -> p.pagoEm() != null ? p.valor() : p.recebido());
                ClienteResponseDTO cliente = d.cliente(e.clienteId());
                return l(e.codigo(), e.nome(), data(e.dataEvento()), e.horaInicio(), e.local(), e.cidade(), Dominio.rotulo(Dominio.TIPOS_EVENTO, e.tipo(), "Evento"),
                    Dominio.rotulo(Dominio.STATUS_EVENTO, e.status(), "—"), ouVazio(e.convidadosPrevistos()), e.margem(), c == null ? "" : ouVazio(c.valorPorConvidado()),
                    c == null ? "" : c.valorTotal(), pago, (c == null ? 0 : c.valorTotal()) - pago, c == null ? "" : data(c.dataAssinatura()),
                    cliente == null ? "" : cliente.nome(), e.observacoes(), e.id());
            }).toList()));

        abas.add(new Xlsx.Aba("Parcelas", List.of("Evento", "Nº", "Vencimento", "Valor", "Pago em", "Forma", "ID"),
            d.parcelas.stream().sorted(Comparator.<ParcelaResponseDTO, String>comparing(p -> Formato.texto(d.nomeEvento(p.eventoId()))).thenComparingInt(ParcelaResponseDTO::numero))
                .map(p -> l(Formato.texto(d.nomeEvento(p.eventoId())), p.numero(), data(p.vencimento()), p.valor(), data(p.pagoEm()), p.forma(), p.id())).toList()));

        abas.add(new Xlsx.Aba("Lançamentos", List.of("Tipo", "Descrição", "Valor", "Status", "Vencimento", "Pagamento", "Competência", "Categoria", "Unidade",
            "Pago/recebido", "Saldo", "Fornecedor/cliente", "Evento", "Conta", "ID"),
            d.lancamentos.stream().map(x -> {
                CategoriaResponseDTO c = d.categoria(x.categoriaId());
                String unidade = Caixa.transferencia(x)
                    ? Dominio.UNIDADES_CURTO.getOrDefault(x.unidadeOrigem(), "") + " → " + Dominio.UNIDADES_CURTO.getOrDefault(x.unidadeDestino(), "")
                    : Dominio.UNIDADES_CURTO.getOrDefault(Caixa.unidadeDe(x), "Sem unidade");
                return l(x.tipo(), x.descricao(), x.valor(), x.status(), data(x.vencimento()), data(x.dataPagamento()), data(x.dataCompetencia()),
                    c == null ? "" : c.nome(), unidade, Caixa.pago(x), Caixa.saldo(x), Caixa.parte(d, x), Formato.texto(d.nomeEvento(x.eventoId())), x.conta(), x.id());
            }).toList()));

        abas.add(new Xlsx.Aba("Pagamentos", List.of("Lançamento", "Data", "Valor", "Conta", "Forma", "Registrado em", "Registrado por", "Lançamento ID"),
            d.lancamentos.stream().flatMap(x -> (x.pagamentos() == null ? List.<LancamentoCaixaResponseDTO.Pagamento>of() : x.pagamentos()).stream()
                .map(p -> l(x.descricao(), data(p.data()), p.valor(), Dominio.CONTAS.getOrDefault(p.conta(), Formato.texto(p.conta())),
                    Dominio.FORMAS_PAGAMENTO.getOrDefault(p.forma(), Formato.texto(p.forma())), Formato.texto(p.em()), Formato.texto(p.por()), x.id()))).toList()));

        abas.add(new Xlsx.Aba("Clientes", List.of("Nome", "Tipo", "Documento", "Telefone", "E-mail", "Origem", "ID"),
            d.clientes.stream().map(c -> l(c.nome(), c.tipo(), c.documento(), c.telefone(), c.email(), c.origem(), c.id())).toList()));

        abas.add(new Xlsx.Aba("Insumos", List.of("Insumo", "Categoria", "Tipo", "Unidade", "Custo", "Saldo em estoque", "Estoque mínimo", "Fornecedor", "Unid. compra",
            "Fator compra", "ID"),
            d.insumos.stream().sorted(Formato.porNome(InsumoResponseDTO::nome)).map(i -> l(i.nome(), i.categoria(), i.tipo(), i.unidade(), i.custo(), est.fisico(i.id()),
                ouVazio(i.estoqueMinimo()), i.fornecedor(), i.unidadeCompra(), ouVazio(i.fatorCompra()), i.id())).toList()));

        List<List<Object>> fichas = new ArrayList<>();
        for (PreparacaoResponseDTO p : d.preparacoes.stream().sorted(Formato.porNome(PreparacaoResponseDTO::nome)).toList()) {
            String categoria = Dominio.rotulo(Dominio.CATEGORIAS_PREPARACAO, p.categoria(), "Outros");
            String rendimento = Formato.fmtN(p.rendimento()) + " " + p.unidadeRendimento();
            if (p.itens().isEmpty()) {
                fichas.add(l(p.nome(), categoria, rendimento, "(sem ingredientes)", "", ""));
            }
            for (PreparacaoResponseDTO.Item it : p.itens()) {
                PreparacaoResponseDTO sub = d.preparacao(it.subId());
                InsumoResponseDTO insumo = d.insumo(it.insumoId());
                fichas.add(l(p.nome(), categoria, rendimento,
                    it.subId() != null ? "Sub-receita: " + (sub == null ? "" : sub.nome()) : insumo == null ? String.valueOf(it.insumoId()) : insumo.nome(), it.qtd(),
                    it.subId() != null ? (sub == null ? "" : sub.unidadeRendimento()) : insumo == null ? "" : insumo.unidade()));
            }
        }
        abas.add(new Xlsx.Aba("Fichas técnicas", List.of("Ficha", "Categoria", "Rendimento", "Insumo", "Qtd", "Unidade"), fichas));

        abas.add(new Xlsx.Aba("Modo de preparo", List.of("Ficha", "Praça", "Conservação", "Modo de preparo", "Etapas", "ID"),
            d.preparacoes.stream().map(p -> l(p.nome(), p.praca(), p.conservacao(), p.modoPreparo(),
                p.etapas().stream().map(t -> t.desc() + (t.dn() != null ? " (D-" + t.dn() + ")" : "")).collect(Collectors.joining(" | ")), p.id())).toList()));

        abas.add(new Xlsx.Aba("Cardápios", List.of("Evento", "Prato", "Status", "Porção/pessoa", "Responsável", "Praça", "ID"),
            d.cardapio.stream().sorted(Comparator.comparing((ItemCardapioResponseDTO c) -> Formato.texto(d.nomeEvento(c.eventoId())))).map(c -> {
                PreparacaoResponseDTO p = d.preparacao(c.preparacaoId());
                return l(Formato.texto(d.nomeEvento(c.eventoId())), p == null ? String.valueOf(c.preparacaoId()) : p.nome(), Eventos.statusItem(c), c.porcao(),
                    c.responsavel(), Formato.texto(c.praca()), c.id());
            }).toList()));

        abas.add(new Xlsx.Aba("Movimentos estoque", List.of("Data", "Tipo", "Insumo", "Qtd", "Motivo", "Evento", "ID"),
            d.movimentos.stream().map(m -> {
                InsumoResponseDTO i = d.insumo(m.insumoId());
                return l(data(m.data()), m.tipo(), i == null ? String.valueOf(m.insumoId()) : i.nome(), m.qtd(), m.motivo(), Formato.texto(d.nomeEvento(m.eventoId())), m.id());
            }).toList()));

        abas.add(new Xlsx.Aba("Notas fiscais", List.of("Emissão", "Fornecedor", "CNPJ", "Número", "Valor", "Destino", "Pagamento", "Parcelas", "Status", "Chave", "ID"),
            d.nfs.stream().map(n -> l(data(n.emissao()), n.fornecedor(), n.cnpjFornecedor(), n.numero(), n.total(),
                n.eventoId() == null ? "Estoque da fábrica" : Formato.texto(d.nomeEvento(n.eventoId())), Dominio.rotulo(Dominio.FORMAS_PAGAMENTO_NF, n.formaPag(), ""),
                n.parcelas().stream().map(p -> data(p.venc()) + " " + String.format("%.2f", p.valor()).replace(".", ",")).collect(Collectors.joining(" | ")), n.status(),
                n.chave(), n.id())).toList()));

        abas.add(new Xlsx.Aba("Categorias", List.of("Nome", "Grupo", "ID"), d.categorias.stream().map(c -> l(c.nome(), c.grupo(), c.id())).toList()));

        abas.add(new Xlsx.Aba("Checklist cozinha", List.of("Tipo", "Evento", "Item", "Produzido", "Executado por", "Separado por", "Temp. saída", "Enviado", "Qtd enviada",
            "Conferido no retorno", "Retorno", "Feito", "ID"),
            d.execucoes.stream().map(x -> {
                ItemCardapioResponseDTO c = d.itemCardapio(x.cardapioId());
                PreparacaoResponseDTO p = c == null ? null : d.preparacao(c.preparacaoId());
                Object item = x.cardapioId() != null ? (p == null ? String.valueOf(x.cardapioId()) : p.nome()) : !Formato.vazio(x.key()) ? x.key()
                    : x.insumoId() == null ? "" : x.insumoId();
                boolean enviado = x.enviado() || (x.enviadoQ() != null && x.enviadoQ() > 0) || x.saiu() != null;
                return l(x.kind(), Formato.texto(d.nomeEvento(x.eventoId())), item, x.produzido(), x.executadoPor(), x.separadoPor(), x.tempSaida(), enviado ? "sim" : "",
                    x.enviadoQ() != null ? x.enviadoQ() : x.saiu(), x.conferidoPor(), x.retorno(), x.feito() ? "sim" : "", x.id());
            }).toList()));

        return Xlsx.gerar(abas);
    }
}

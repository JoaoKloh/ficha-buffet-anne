package com.jbkloh.ficha_buffet.calculo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Constantes de domínio do Anne Gestão: valores aceitos, rótulos e ordem em que
 * aparecem nas telas. As expressões RE_* são usadas na validação dos DTOs.
 */
public final class Dominio {

    private Dominio() {}

    public static final String RE_DATA = "\\d{4}-\\d{2}-\\d{2}";
    public static final String RE_DATA_OU_VAZIA = "^$|" + RE_DATA;
    public static final String RE_HORA_OU_VAZIA = "^$|([01]\\d|2[0-3]):[0-5]\\d";
    public static final String RE_STATUS_EVENTO = "lead|orcamento_enviado|degustacao|contratado|cardapio_fechado|em_producao|realizado|acertado|perdido|cancelado";
    public static final String RE_TIPO_EVENTO = "casamento|15_anos|corporativo|coffee_break|outro";
    public static final String RE_GRUPO_DRE = "receita|deducao|custo_direto|despesa_variavel|despesa_fixa|depreciacao_financeiro_ir|investimento";
    public static final String RE_ORIGEM_CLIENTE = "^$|instagram|google|whatsapp|indicacao|outro";
    public static final String RE_TIPO_CLIENTE = "PF|PJ";
    public static final String RE_CONTA = "banco|banco_restaurante|caixa";
    public static final String RE_UNIDADE = "buffet|restaurante|chocolates|compartilhado";
    public static final String RE_FORMA_PAGAMENTO = "^$|pix|transferencia|boleto|cartao|dinheiro|prazo";
    public static final String RE_FORMA_RECEBIMENTO = "^$|pix|transferencia|boleto|cartao|dinheiro";
    public static final String RE_FORMA_PAGAMENTO_NF = "boleto|pix|transferencia|cartao|dinheiro|prazo";
    public static final String RE_TIPO_ITEM = "ingrediente|embalagem|descartavel|louca|utensilio|limpeza";
    public static final String RE_LOCAL_ESTOQUE = "Refrigerado|Congelado|Seco|Depósito|Depósito de louças|Depósito limpeza|Depósito de descartáveis";
    public static final String RE_CATEGORIA_INSUMO = "Proteínas e frutos do mar|Queijos e laticínios|Hortifruti|Mercearia|Bebidas|Descartáveis|Produtos de limpeza|Outros";
    public static final String RE_UNIDADE_INSUMO = "kg|l|un|dz|maço|garrafa|botijão";
    public static final String RE_UNIDADE_RENDIMENTO = "porções|un|kg|l";
    public static final String RE_CATEGORIA_PREPARACAO = "canape|volante|entrada|ilha|principal|acompanhamento|sobremesa|bebida|base";
    public static final String RE_PRACA = "Chef|Recebimento|Cozinha quente|Salgados|Confeitaria|Frios|No evento";
    public static final String RE_PRACA_OU_VAZIA = "^$|" + RE_PRACA;
    public static final String RE_CONSERVACAO = "refrigerado|congelado|quente|ambiente";
    public static final String RE_UNIDADE_COMPONENTE = "kg|L|un|maço";
    public static final String RE_STATUS_ITEM_CARDAPIO = "sim|definir|nao";
    public static final String RE_MOVIMENTO_MANUAL = "entrada|perda|ajuste|devolucao|sobra|consumo";
    public static final String RE_UNIDADE_OPERACIONAL = "buffet|restaurante|chocolates";
    public static final String RE_MES = "\\d{4}-(0[1-9]|1[0-2])";
    public static final String RE_SECAO_CARTA = "serra|mar|campo|executivo|cafe|sobremesa|bebida|outro";
    public static final String RE_DESTINO_LOTE = "restaurante|geral|evento";
    /** Planos de produção (tela Produção): unidades e situações. */
    public static final String RE_UNIDADE_PLANO = "restaurante|chocolates";
    public static final String RE_STATUS_PLANO = "planejado|em_producao|concluido";
    public static final String RE_PARA_LOTE = "restaurante|evento|outra|vencido|qualidade|acidente|sobra";
    public static final String RE_SECAO_ENVIO_EXTRA = "servico|loucas|descartaveis";
    public static final String RE_MODO_ENVIO_EXTRA = "pax|pax106|fix";
    public static final String RE_UNIDADE_ENVIO_EXTRA = "un|kg|L|cx|rolo";
    public static final String RE_KIND_EXECUCAO = "item|comp|extra|louca|tarefa|compras|transporte";
    public static final String RE_ETAPA_EVENTO = "cadastro|dados|contrato|degustacao|cardapio|fichas|compras|producao|equipe|logistica|carregamento|execucao|conferencia|fechamento";
    public static final String RE_FUNCAO_EQUIPE = "Maître|Garçom|Garçonete|Cozinheiro\\(a\\)|Auxiliar de cozinha|Copeira|Bartender|Recepcionista|Motorista|Coordenação|Limpeza";

    /** As 14 etapas do evento, na ordem do processo. */
    public static final Map<String, String> ETAPAS_EVENTO = mapa(
        "cadastro", "Cadastro do cliente e evento", "dados", "Data, local e convidados", "contrato", "Contrato e condições de pagamento",
        "degustacao", "Degustação", "cardapio", "Definição do cardápio", "fichas", "Fichas técnicas e custo previsto",
        "compras", "Planejamento de compras", "producao", "Ordem de produção", "equipe", "Escala de equipe", "logistica", "Logística e transporte",
        "carregamento", "Checklist de carregamento", "execucao", "Execução do evento", "conferencia", "Conferência de consumo e devoluções",
        "fechamento", "Fechamento financeiro");
    /** Etapas críticas: pendentes a esta quantidade de dias do evento (ou menos), aparecem como atrasadas. */
    public static final Map<String, Integer> ETAPAS_CRITICAS = Map.of("contrato", 30, "cardapio", 20, "fichas", 15, "equipe", 10, "logistica", 7, "compras", 7);
    /** Tipos de custo do evento, no previsto × realizado (classificados pelo nome da categoria, Caixa.tipoCusto). */
    public static final Map<String, String> TIPOS_CUSTO = mapa(
        "ab", "Alimentos e bebidas", "equipe", "Equipe", "transporte", "Transporte e logística", "outros", "Locação e outros");

    public static final Map<String, String> STATUS_EVENTO = mapa(
        "lead", "Lead", "orcamento_enviado", "Orçamento enviado", "degustacao", "Degustação", "contratado", "Contratado",
        "cardapio_fechado", "Cardápio fechado", "em_producao", "Em produção", "realizado", "Realizado", "acertado", "Acertado",
        "perdido", "Perdido", "cancelado", "Cancelado");
    private static final List<String> ORDEM_STATUS = List.copyOf(STATUS_EVENTO.keySet());

    public static final Map<String, String> TIPOS_EVENTO = mapa(
        "casamento", "Casamento", "15_anos", "15 anos", "corporativo", "Corporativo", "coffee_break", "Coffee break", "outro", "Outro");

    public static final Map<String, String> GRUPOS_DRE = mapa(
        "receita", "Receitas", "deducao", "Deduções", "custo_direto", "Custos diretos", "despesa_variavel", "Despesas variáveis",
        "despesa_fixa", "Despesas fixas", "depreciacao_financeiro_ir", "Depreciação, financeiro e IR", "investimento", "Investimentos (fora do resultado)");

    /** Unidades de negócio. A estrutura compartilhada é rateada entre as três operacionais. */
    public static final Map<String, String> UNIDADES = mapa(
        "buffet", "Buffet Anne", "restaurante", "Restaurante ANNE", "chocolates", "Chocolates Marie & Anne", "compartilhado", "Estrutura compartilhada");
    public static final Map<String, String> UNIDADES_CURTO = mapa(
        "buffet", "Buffet", "restaurante", "Restaurante", "chocolates", "Chocolates", "compartilhado", "Compartilhado");
    public static final List<String> UNIDADES_OPERACIONAIS = List.of("buffet", "restaurante", "chocolates");

    public static final Map<String, String> CONTAS = mapa("banco", "Banco Buffet", "banco_restaurante", "Banco Restaurante", "caixa", "Caixa (dinheiro)");
    public static final Map<String, String> FORMAS_PAGAMENTO = mapa(
        "pix", "Pix", "transferencia", "Transferência", "boleto", "Boleto", "cartao", "Cartão", "dinheiro", "Dinheiro", "prazo", "A prazo");

    public static final List<String> CATEGORIAS_INSUMO = List.of(
        "Proteínas e frutos do mar", "Queijos e laticínios", "Hortifruti", "Mercearia", "Bebidas", "Descartáveis", "Produtos de limpeza", "Outros");

    public static final Map<String, String> CATEGORIAS_PREPARACAO = mapa(
        "canape", "Canapés frios e quentes", "volante", "Volante forno e fogão", "entrada", "Mini porções e entradas", "ilha", "Ilha de antepasto",
        "principal", "Prato principal", "acompanhamento", "Acompanhamento", "sobremesa", "Sobremesa", "bebida", "Bebida",
        "base", "Sub-receita (massa, recheio, molho)");
    private static final List<String> ORDEM_CATEGORIA_PREPARACAO = List.copyOf(CATEGORIAS_PREPARACAO.keySet());

    public static final Map<String, String> PRACA_PADRAO = mapa(
        "canape", "Frios", "volante", "Salgados", "entrada", "Frios", "ilha", "Frios", "principal", "Cozinha quente",
        "acompanhamento", "Cozinha quente", "sobremesa", "Confeitaria", "bebida", "No evento", "base", "Cozinha quente");

    public static final Map<String, String> CONSERVACOES = mapa(
        "refrigerado", "Refrigerado (0 a 4 °C)", "congelado", "Congelado (−18 °C ou menos)", "quente", "Quente (60 °C ou mais)", "ambiente", "Ambiente");
    public static final Map<String, Integer> VALIDADE_PADRAO = Map.of("refrigerado", 3, "congelado", 30, "quente", 0, "ambiente", 5);

    public static final Map<String, String> FORMAS_PAGAMENTO_NF = mapa(
        "boleto", "Boleto", "pix", "PIX", "transferencia", "Transferência", "cartao", "Cartão", "dinheiro", "Dinheiro", "prazo", "A prazo (sem boleto)");

    public static final List<String> SECOES_CARTA = List.of("serra", "mar", "campo", "executivo", "cafe", "sobremesa", "bebida", "outro");
    public static final double META_CMV = 0.32;

    public static final List<String> UNIDADES_INTEIRAS = List.of("un", "dz", "maço", "garrafa", "botijão");

    /** Sinal de cada movimento manual no saldo (ajuste grava a diferença para a contagem). */
    public static final Map<String, Integer> SINAL_MOVIMENTO = Map.of("entrada", 1, "sobra", 1, "perda", -1, "devolucao", -1, "ajuste", 0, "consumo", -1);
    public static final Map<String, String> TIPOS_MOVIMENTO = mapa(
        "inventario", "Inventário", "entrada", "Entrada", "sobra", "Sobra que voltou", "perda", "Perda", "devolucao", "Devolução a fornecedor",
        "ajuste", "Ajuste de contagem", "baixa", "Baixa de produção", "consumo", "Consumo / uso");
    /** Origem dos movimentos gravados, para a conferência do estoque. */
    public static final Map<String, String> ORIGENS_MOVIMENTO = mapa(
        "nf", "Compra recebida (NF)", "manual", "Lançamento manual", "inventario", "Inventário", "checklist", "Checklist do evento", "consumo", "Consumo da unidade");
    public static final Map<String, String> MOTIVOS_DESCARTE_LOTE = mapa(
        "vencido", "Venceu", "qualidade", "Qualidade fora do padrão", "acidente", "Acidente ou contaminação", "sobra", "Sobra do serviço");
    /** Horizontes da sugestão de compras, em dias. */
    public static final List<Integer> HORIZONTES_COMPRA = List.of(7, 15, 30, 60);

    /** Posição da etapa no funil; quanto maior, mais adiante. */
    public static int rank(String status) {
        return Math.max(0, ORDEM_STATUS.indexOf(status));
    }

    public static int ordemCategoriaPreparacao(String categoria) {
        int i = ORDEM_CATEGORIA_PREPARACAO.indexOf(categoria);
        return i < 0 ? 99 : i;
    }

    public static boolean reutilizavel(String tipoItem) {
        return "louca".equals(tipoItem) || "utensilio".equals(tipoItem);
    }

    /** Rótulo de uma opção; devolve a própria chave se não existir e o padrão se a chave for vazia. */
    public static String rotulo(Map<String, String> opcoes, String chave, String padrao) {
        if (chave == null || chave.isEmpty()) {
            return padrao;
        }
        return opcoes.getOrDefault(chave, chave);
    }

    /* ------------------------- Ordens de produção da cozinha ------------------------- */

    public static final String RE_MOTIVO_BLOQUEIO = "insumo|equipamento|informacao|outro";
    public static final String RE_ITEM_LIBERACAO = "preparacoes|quantidades|embalagens|temperaturas|utensilios|equipamentos|bandejas|bebidas|veiculo";
    /** Tipos de ocorrência do evento (o rótulo é o próprio valor, como no frontend). */
    public static final String RE_TIPO_OCORRENCIA = "Atraso|Falta de insumo|Temperatura|Carregamento|Equipamento ou utensílio|Acidente|Pedido do cliente|Outro";

    /** Setores da cozinha quando ainda não há nenhum gravado. */
    public static final Map<String, String> SETORES_PADRAO = mapa(
        "quente", "Cozinha quente", "fria", "Cozinha fria", "confeitaria", "Confeitaria", "montagem", "Montagem e finalização",
        "expedicao", "Expedição e logística");
    public static final List<String> STATUS_ORDEM = List.of("pendente", "em_producao", "em_conferencia", "concluida", "bloqueada");
    /** Etapas das ordens, na ordem em que aparecem no mapa. */
    public static final List<String> ETAPAS_ORDEM = List.of("principal", "pre", "molho", "montagem", "expedicao");
    public static final Map<String, String> MOTIVOS_BLOQUEIO = mapa(
        "insumo", "Falta de insumo", "equipamento", "Falta de equipamento", "informacao", "Falta de informação", "outro", "Outro impedimento");
    /** Conferências da liberação para expedição. */
    public static final Map<String, String> ITENS_LIBERACAO = mapa(
        "preparacoes", "Todas as preparações separadas", "quantidades", "Quantidades conferidas com o checklist", "embalagens", "Embalagens e identificações",
        "temperaturas", "Temperaturas e condições de transporte", "utensilios", "Utensílios", "equipamentos", "Equipamentos",
        "bandejas", "Bandejas e materiais de serviço", "bebidas", "Bebidas e complementos", "veiculo", "Veículo conferido");
    /** Antecedência padrão da produção (dias antes do evento) pela praça da ficha; sub-receitas seguem a categoria. */
    public static final Map<String, Integer> DN_PRACA = Map.of("Frios", 7, "Cozinha quente", 6, "Salgados", 5);
    /** Dias antes do evento em que cada categoria é produzida, quando a praça não tem padrão. */
    public static final Map<String, Integer> DN_CATEGORIA = Map.of("canape", 1, "volante", 1, "entrada", 1, "ilha", 1, "principal", 1, "acompanhamento", 1,
        "sobremesa", 2, "bebida", 0, "base", 2);

    private static Map<String, String> mapa(String... pares) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put(pares[i], pares[i + 1]);
        }
        return java.util.Collections.unmodifiableMap(m);
    }
}

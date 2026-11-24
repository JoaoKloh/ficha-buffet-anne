/**
 * Constantes de domínio do Anne Gestão: enums com rótulos, na ordem em que
 * aparecem nas telas. Fonte única para formulários, filtros, cálculos e BFF.
 * Os valores (chaves) são os mesmos gravados no protótipo de referência.
 */

export type Opcao<K extends string = string> = readonly [K, string];

export const STATUS_EVENTO = [
  ["lead", "Lead"],
  ["orcamento_enviado", "Orçamento enviado"],
  ["degustacao", "Degustação"],
  ["contratado", "Contratado"],
  ["cardapio_fechado", "Cardápio fechado"],
  ["em_producao", "Em produção"],
  ["realizado", "Realizado"],
  ["acertado", "Acertado"],
  ["perdido", "Perdido"],
  ["cancelado", "Cancelado"],
] as const;
export type StatusEvento = (typeof STATUS_EVENTO)[number][0];
export const STATUS_EVENTO_VALORES = STATUS_EVENTO.map((s) => s[0]) as unknown as readonly [StatusEvento, ...StatusEvento[]];
/** Posição da etapa no funil; quanto maior, mais adiante. */
export const RANK_STATUS: Record<StatusEvento, number> = Object.fromEntries(
  STATUS_EVENTO.map((s, i) => [s[0], i])
) as Record<StatusEvento, number>;

export const TIPOS_EVENTO = [
  ["casamento", "Casamento"],
  ["15_anos", "15 anos"],
  ["corporativo", "Corporativo"],
  ["coffee_break", "Coffee break"],
  ["outro", "Outro"],
] as const;
export type TipoEvento = (typeof TIPOS_EVENTO)[number][0];

export const GRUPOS_DRE = [
  ["receita", "Receitas"],
  ["deducao", "Deduções"],
  ["custo_direto", "Custos diretos"],
  ["despesa_variavel", "Despesas variáveis"],
  ["despesa_fixa", "Despesas fixas"],
  ["depreciacao_financeiro_ir", "Depreciação, financeiro e IR"],
  ["investimento", "Investimentos (fora do resultado)"],
] as const;
export type GrupoDre = (typeof GRUPOS_DRE)[number][0];

/** Unidades de negócio. A estrutura compartilhada é rateada entre as três operacionais na DRE. */
export const UNIDADES = [
  ["buffet", "Buffet Anne"],
  ["restaurante", "Restaurante ANNE"],
  ["chocolates", "Chocolates Marie & Anne"],
  ["compartilhado", "Estrutura compartilhada"],
] as const;
export type Unidade = (typeof UNIDADES)[number][0];
export const UN_OPER = ["buffet", "restaurante", "chocolates"] as const;
export type UnidadeOperacional = (typeof UN_OPER)[number];
const UN_CURTO: Record<string, string> = { buffet: "Buffet", restaurante: "Restaurante", chocolates: "Chocolates", compartilhado: "Compartilhado" };
/** Nome curto da unidade; vazio ou desconhecida = "Sem unidade". */
export const unCurto = (u: string | null | undefined): string => (u && UN_CURTO[u]) || "Sem unidade";
export const unLabel = (u: string | null | undefined): string => (UNIDADES.find((x) => x[0] === u) ?? [u, "Sem unidade"])[1] ?? "Sem unidade";
/** Valor do filtro de unidade para "sem unidade". */
export const SEM_UNIDADE = "_sem";

export const ORIGENS_CLIENTE = [
  ["instagram", "Instagram"],
  ["google", "Google"],
  ["whatsapp", "WhatsApp"],
  ["indicacao", "Indicação"],
  ["outro", "Outro"],
] as const;
export type OrigemCliente = (typeof ORIGENS_CLIENTE)[number][0];

export const TIPOS_CLIENTE = [
  ["PF", "Pessoa física"],
  ["PJ", "Empresa"],
] as const;
export type TipoCliente = (typeof TIPOS_CLIENTE)[number][0];

export const CONTAS = [
  ["banco", "Banco Buffet"],
  ["banco_restaurante", "Banco Restaurante"],
  ["caixa", "Caixa (dinheiro)"],
] as const;
export type Conta = (typeof CONTAS)[number][0];

export const FORMAS_RECEBIMENTO = [
  ["pix", "Pix"],
  ["transferencia", "Transferência"],
  ["boleto", "Boleto"],
  ["cartao", "Cartão"],
  ["dinheiro", "Dinheiro"],
] as const;
export type FormaRecebimento = (typeof FORMAS_RECEBIMENTO)[number][0];

export const FORMAS_PAGAMENTO_NF = [
  ["boleto", "Boleto"],
  ["pix", "PIX"],
  ["transferencia", "Transferência"],
  ["cartao", "Cartão"],
  ["dinheiro", "Dinheiro"],
  ["prazo", "A prazo (sem boleto)"],
] as const;
export type FormaPagamentoNf = (typeof FORMAS_PAGAMENTO_NF)[number][0];

export const STATUS_LANCAMENTO = ["previsto", "realizado", "cancelado"] as const;
export type StatusLancamento = (typeof STATUS_LANCAMENTO)[number];
export const ORIGENS_LANCAMENTO = ["manual", "parcela", "extra", "nf", "equipe", "transferencia"] as const;
export type OrigemLancamento = (typeof ORIGENS_LANCAMENTO)[number];
export const TIPOS_LANCAMENTO = ["receita", "despesa", "transferencia"] as const;
export type TipoLancamento = (typeof TIPOS_LANCAMENTO)[number];

export const TIPOS_ITEM = [
  ["ingrediente", "Ingrediente"],
  ["embalagem", "Embalagem"],
  ["descartavel", "Descartáveis"],
  ["louca", "Louça"],
  ["utensilio", "Utensílio"],
  ["limpeza", "Produtos de limpeza"],
] as const;
export type TipoItem = (typeof TIPOS_ITEM)[number][0];
export const reutilizavel = (t: TipoItem): boolean => t === "louca" || t === "utensilio";

export const LOCAIS_ESTOQUE = [
  "Refrigerado",
  "Congelado",
  "Seco",
  "Depósito",
  "Depósito de louças",
  "Depósito limpeza",
  "Depósito de descartáveis",
] as const;
export type LocalEstoque = (typeof LOCAIS_ESTOQUE)[number];

export const CATEGORIAS_INSUMO = [
  "Proteínas e frutos do mar",
  "Queijos e laticínios",
  "Hortifruti",
  "Mercearia",
  "Bebidas",
  "Descartáveis",
  "Produtos de limpeza",
  "Outros",
] as const;
export type CategoriaInsumo = (typeof CATEGORIAS_INSUMO)[number];

export const UNIDADES_INSUMO = [
  ["kg", "kg"],
  ["l", "litro"],
  ["un", "unidade"],
  ["dz", "dúzia"],
  ["maço", "maço"],
  ["garrafa", "garrafa"],
  ["botijão", "botijão"],
] as const;
export type UnidadeInsumo = (typeof UNIDADES_INSUMO)[number][0];
/** Unidades em que a quantidade é sempre inteira. */
export const UNIDADES_INTEIRAS: readonly string[] = ["un", "dz", "maço", "garrafa", "botijão"];

export const UNIDADES_RENDIMENTO = [
  ["porções", "porções"],
  ["un", "unidades"],
  ["kg", "kg"],
  ["l", "litros"],
] as const;
export type UnidadeRendimento = (typeof UNIDADES_RENDIMENTO)[number][0];

export const CATEGORIAS_PREPARACAO = [
  ["canape", "Canapés frios e quentes"],
  ["volante", "Volante forno e fogão"],
  ["entrada", "Mini porções e entradas"],
  ["ilha", "Ilha de antepasto"],
  ["principal", "Prato principal"],
  ["acompanhamento", "Acompanhamento"],
  ["sobremesa", "Sobremesa"],
  ["bebida", "Bebida"],
  ["base", "Sub-receita (massa, recheio, molho)"],
] as const;
export type CategoriaPreparacao = (typeof CATEGORIAS_PREPARACAO)[number][0];

export const PRACAS = ["Chef", "Recebimento", "Cozinha quente", "Salgados", "Confeitaria", "Frios", "No evento"] as const;
export type Praca = (typeof PRACAS)[number];

export const CONSERVACOES = [
  ["refrigerado", "Refrigerado (0 a 4 °C)"],
  ["congelado", "Congelado (−18 °C ou menos)"],
  ["quente", "Quente (60 °C ou mais)"],
  ["ambiente", "Ambiente"],
] as const;
export type Conservacao = (typeof CONSERVACOES)[number][0];
export const VALIDADE_PADRAO: Record<Conservacao, number> = { refrigerado: 3, congelado: 30, quente: 0, ambiente: 5 };

export const UNIDADES_COMPONENTE = [
  ["kg", "kg"],
  ["L", "L"],
  ["un", "un"],
  ["maço", "maço"],
] as const;
export type UnidadeComponente = (typeof UNIDADES_COMPONENTE)[number][0];

export const STATUS_ITEM_CARDAPIO = [
  ["sim", "Sim"],
  ["definir", "A definir"],
  ["nao", "Não"],
] as const;
export type StatusItemCardapio = (typeof STATUS_ITEM_CARDAPIO)[number][0];

export const TIPOS_MOVIMENTO = {
  inventario: { rotulo: "Inventário", tom: "", sinal: 0 },
  entrada: { rotulo: "Entrada", tom: "good", sinal: 1 },
  sobra: { rotulo: "Sobra que voltou", tom: "good", sinal: 1 },
  perda: { rotulo: "Perda", tom: "bad", sinal: -1 },
  devolucao: { rotulo: "Devolução a fornecedor", tom: "warn", sinal: -1 },
  ajuste: { rotulo: "Ajuste de contagem", tom: "brass", sinal: 0 },
  baixa: { rotulo: "Baixa de produção", tom: "accent", sinal: -1 },
  consumo: { rotulo: "Consumo / uso", tom: "accent", sinal: -1 },
} as const;
export type TipoMovimento = keyof typeof TIPOS_MOVIMENTO;
export const MOVIMENTOS_MANUAIS = ["entrada", "perda", "ajuste", "devolucao", "sobra", "consumo"] as const;
export type TipoMovimentoManual = (typeof MOVIMENTOS_MANUAIS)[number];
export const ORIGENS_MOVIMENTO = ["manual", "inventario", "nf", "checklist", "consumo", "ordem", "lote"] as const;
export type OrigemMovimento = (typeof ORIGENS_MOVIMENTO)[number];

export const STATUS_NF = ["rascunho", "lancada"] as const;
export type StatusNf = (typeof STATUS_NF)[number];

export const SECOES_CARTA = [
  ["serra", "Serra"],
  ["mar", "Mar"],
  ["campo", "Campo"],
  ["executivo", "Almoço executivo"],
  ["cafe", "Café e biscoitos"],
  ["sobremesa", "Sobremesas"],
  ["bebida", "Bebidas"],
  ["outro", "Outros"],
] as const;
export type SecaoCarta = (typeof SECOES_CARTA)[number][0];
export const META_CMV = 0.32;

export const DESTINOS_LOTE = [
  ["restaurante", "Restaurante ANNE"],
  ["geral", "Estoque geral (restaurante e eventos)"],
  ["evento", "Evento"],
] as const;
export type DestinoLote = (typeof DESTINOS_LOTE)[number][0];
export const USOS_LOTE = [
  ["restaurante", "Serviço do restaurante"],
  ["evento", "Evento"],
  ["outra", "Outra preparação"],
] as const;
export const MOTIVOS_DESCARTE_LOTE = [
  ["vencido", "Venceu"],
  ["qualidade", "Qualidade fora do padrão"],
  ["acidente", "Acidente ou contaminação"],
  ["sobra", "Sobra do serviço"],
] as const;
export type ParaLote = (typeof USOS_LOTE)[number][0] | (typeof MOTIVOS_DESCARTE_LOTE)[number][0];

export const SECOES_ENVIO_EXTRA = [
  ["servico", "Bebidas e insumos do serviço"],
  ["loucas", "Prataria, louças e utensílios"],
  ["descartaveis", "Descartáveis e decor"],
] as const;
export type SecaoEnvioExtra = (typeof SECOES_ENVIO_EXTRA)[number][0];
export const MODOS_ENVIO_EXTRA = [
  ["pax", "por pessoa (com margem)"],
  ["pax106", "por pessoa × 1,06"],
  ["fix", "quantidade fixa"],
] as const;
export type ModoEnvioExtra = (typeof MODOS_ENVIO_EXTRA)[number][0];
export const UNIDADES_ENVIO_EXTRA = ["un", "kg", "L", "cx", "rolo"] as const;

/* ------------------------------ Evento como projeto ------------------------------ */

/** Tipos de custo do evento no previsto × realizado. */
export const CUSTO_TIPOS = [
  ["ab", "Alimentos e bebidas"],
  ["equipe", "Equipe"],
  ["transporte", "Transporte e logística"],
  ["outros", "Locação e outros"],
] as const;
export type CustoTipo = (typeof CUSTO_TIPOS)[number][0];

export const FUNCOES_EQUIPE = ["Maître", "Garçom", "Garçonete", "Cozinheiro(a)", "Auxiliar de cozinha", "Copeira", "Bartender", "Recepcionista", "Motorista", "Coordenação", "Limpeza"] as const;
export type FuncaoEquipe = (typeof FUNCOES_EQUIPE)[number];

/** As 14 etapas do evento, na ordem do processo. */
export const ETAPAS_EVENTO = [
  ["cadastro", "Cadastro do cliente e evento"],
  ["dados", "Data, local e convidados"],
  ["contrato", "Contrato e condições de pagamento"],
  ["degustacao", "Degustação"],
  ["cardapio", "Definição do cardápio"],
  ["fichas", "Fichas técnicas e custo previsto"],
  ["compras", "Planejamento de compras"],
  ["producao", "Ordem de produção"],
  ["equipe", "Escala de equipe"],
  ["logistica", "Logística e transporte"],
  ["carregamento", "Checklist de carregamento"],
  ["execucao", "Execução do evento"],
  ["conferencia", "Conferência de consumo e devoluções"],
  ["fechamento", "Fechamento financeiro"],
] as const;
export type EtapaEvento = (typeof ETAPAS_EVENTO)[number][0];

/* ---------------------------- Estoque integrado ---------------------------- */

export const HORIZONTES_COMPRA = [
  [7, "7 dias"],
  [15, "15 dias"],
  [30, "30 dias"],
  [60, "60 dias"],
] as const;
export type HorizonteCompra = (typeof HORIZONTES_COMPRA)[number][0];

/* ----------------------- Ordens de produção da cozinha ----------------------- */

export const SETORES_PADRAO = [
  ["quente", "Cozinha quente"],
  ["fria", "Cozinha fria"],
  ["confeitaria", "Confeitaria"],
  ["montagem", "Montagem e finalização"],
  ["expedicao", "Expedição e logística"],
] as const;

export const STATUS_ORDEM = [
  ["pendente", "Pendente", ""],
  ["em_producao", "Em produção", "accent"],
  ["em_conferencia", "Em conferência", "warn"],
  ["concluida", "Concluída", "good"],
  ["bloqueada", "Bloqueada", "bad"],
] as const;
export type StatusOrdem = (typeof STATUS_ORDEM)[number][0];

export const ETAPAS_ORDEM = [
  ["principal", "Preparação principal"],
  ["pre", "Pré-preparo"],
  ["molho", "Molho e acompanhamento"],
  ["montagem", "Finalização e montagem"],
  ["expedicao", "Acondicionamento e expedição"],
] as const;
export type EtapaOrdem = (typeof ETAPAS_ORDEM)[number][0];

export const MOTIVOS_BLOQUEIO = [
  ["insumo", "Falta de insumo"],
  ["equipamento", "Falta de equipamento"],
  ["informacao", "Falta de informação"],
  ["outro", "Outro impedimento"],
] as const;
export type MotivoBloqueio = (typeof MOTIVOS_BLOQUEIO)[number][0];

/** Antecedência de produção: dias antes do evento. */
export const DN_OPCOES = [
  [7, "D-7"],
  [6, "D-6"],
  [5, "D-5"],
  [3, "D-3"],
  [2, "D-2"],
  [1, "D-1"],
  [0, "Dia do evento"],
] as const;
export const dnLabel = (n: number | null | undefined): string => (n == null ? "—" : n === 0 ? "Dia do evento" : `D-${n}`);

/** Conferências da liberação para expedição. */
export const ITENS_LIBERACAO = [
  ["preparacoes", "Todas as preparações separadas"],
  ["quantidades", "Quantidades conferidas com o checklist"],
  ["embalagens", "Embalagens e identificações"],
  ["temperaturas", "Temperaturas e condições de transporte"],
  ["utensilios", "Utensílios"],
  ["equipamentos", "Equipamentos"],
  ["bandejas", "Bandejas e materiais de serviço"],
  ["bebidas", "Bebidas e complementos"],
  ["veiculo", "Veículo conferido"],
] as const;
export type ItemLiberacao = (typeof ITENS_LIBERACAO)[number][0];

export const TIPOS_OCORRENCIA = ["Atraso", "Falta de insumo", "Temperatura", "Carregamento", "Equipamento ou utensílio", "Acidente", "Pedido do cliente", "Outro"] as const;
export type TipoOcorrencia = (typeof TIPOS_OCORRENCIA)[number];

/* --------------------- Produção do restaurante e da chocolates --------------------- */

export const UNIDADES_PLANO = [
  ["restaurante", "Restaurante ANNE"],
  ["chocolates", "Chocolates Marie & Anne"],
] as const;
export type UnidadePlano = (typeof UNIDADES_PLANO)[number][0];

export const STATUS_PLANO = [
  ["planejado", "Planejado"],
  ["em_producao", "Em produção"],
  ["concluido", "Concluído"],
] as const;
export type StatusPlano = (typeof STATUS_PLANO)[number][0];

/** Rótulo de uma opção; devolve a própria chave se não existir. */
export function rotulo(opcoes: readonly Opcao[], chave: string | null | undefined, padrao = "—"): string {
  if (chave == null || chave === "") return padrao;
  return opcoes.find((o) => o[0] === chave)?.[1] ?? chave;
}

export const chaves = <K extends string>(opcoes: readonly Opcao<K>[]): [K, ...K[]] =>
  opcoes.map((o) => o[0]) as [K, ...K[]];

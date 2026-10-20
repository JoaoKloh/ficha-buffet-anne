import type { Ingrediente } from "@/lib/models/dish";

type TipoPrato =
  | "massa_recheada"
  | "stick"
  | "frito_moldado"
  | "harumaki"
  | "risoto"
  | "carpaccio"
  | "tartar"
  | "ceviche"
  | "mousse"
  | "massa_molho"
  | "proteina_principal"
  | "fixo"
  | "estacao"
  | "geral";

interface FichaGerada {
  ingredientes: Ingrediente[];
  receita: string;
}

const RECEITAS_ESPECIAIS: Record<string, { ing: [string, number, string][]; modo: string }> = {
  "Mini banoffee": {
    ing: [
      ["Bolacha amanteigada triturada", 200, "g"],
      ["Manteiga derretida", 60, "g"],
      ["Doce de leite", 350, "g"],
      ["Banana fatiada", 200, "g"],
      ["Chantilly", 150, "g"],
      ["Chocolate em raspas", 40, "g"],
    ],
    modo: "Fazer a base com bolacha triturada e manteiga, prensar em forminhas. Cobrir com doce de leite, fatias de banana e finalizar com chantilly e raspas de chocolate.",
  },
  "Brownie de chocolate com fondue de chocolate intenso": {
    ing: [
      ["Chocolate meio amargo", 300, "g"],
      ["Manteiga", 200, "g"],
      ["Açúcar", 250, "g"],
      ["Ovos", 150, "g"],
      ["Farinha de trigo", 100, "g"],
      ["Creme de leite fresco (para o fondue)", 100, "g"],
    ],
    modo: "Assar o brownie tradicional e cortar em mini porções. Servir quente com fondue de chocolate meio amargo derretido com creme de leite.",
  },
  "Mousse de chocolate meio amargo": {
    ing: [
      ["Chocolate meio amargo", 350, "g"],
      ["Creme de leite fresco", 400, "g"],
      ["Açúcar", 150, "g"],
      ["Gemas", 100, "g"],
    ],
    modo: "Derreter o chocolate, incorporar às gemas batidas e ao creme de leite fresco batido em ponto de chantilly. Gelar por no mínimo 2h antes de servir.",
  },
  "Cocada cremosa com toque de leite condensado": {
    ing: [
      ["Coco ralado fresco", 350, "g"],
      ["Leite condensado", 350, "g"],
      ["Leite", 200, "g"],
      ["Manteiga", 100, "g"],
    ],
    modo: "Levar todos os ingredientes ao fogo baixo, mexendo até soltar do fundo da panela. Distribuir em mini potes e gelar.",
  },
  "Churros com doce de leite argentino": {
    ing: [
      ["Massa de churros (água, farinha, manteiga, sal)", 550, "g"],
      ["Óleo para fritura", 250, "g"],
      ["Açúcar e canela", 100, "g"],
      ["Doce de leite argentino", 100, "g"],
    ],
    modo: "Fritar a massa de churros em óleo quente até dourar, passar no açúcar com canela e servir com doce de leite argentino para banhar.",
  },
};

function primeiraDescricao(nome: string): string {
  const semParenteses = nome.replace(/\s*\(.*?\)\s*/g, " ").trim();
  const m = semParenteses.match(/\b(?:de|com|ao|à)\b\s+(.*)/i);
  return (m ? m[1]! : semParenteses).replace(/[,.]$/, "").trim();
}

function classificarPrato(nomeOriginal: string): TipoPrato {
  const n = nomeOriginal.toLowerCase();
  if (/mousse|espuma/.test(n)) return "mousse";
  if (/carpaccio/.test(n)) return "carpaccio";
  if (/tartar|gravlax/.test(n)) return "tartar";
  if (/ceviche/.test(n)) return "ceviche";
  if (/risoto/.test(n)) return "risoto";
  if (/harumaki/.test(n)) return "harumaki";
  if (/croquet|croqueta|coxinha|bolinh|dadinho|kafta/.test(n)) return "frito_moldado";
  if (/\bstick\b/.test(n)) return "stick";
  if (/nhoque|ravioli/.test(n)) return "massa_molho";
  if (
    /quiche|vol-au-vent|tortinha|cestinha|sable|canapé|bigne|blinis|crostillant|tule|macaron|profiterole|trouxinha|cone com|bao de/.test(
      n
    )
  )
    return "massa_recheada";
  if (
    /filé|file mignon|medalh|escalop|robalo|linguado|peixe branco|bacalhau|picadinho|escondidinho|ragu de|bobó|arroz de|batata rústica|brandada|kani/.test(
      n
    )
  )
    return "proteina_principal";
  if (
    /item fixo|queijo |presunto|salame|pizza branca|grissini|frutas no mel|gateau|couscous|pirâmide|terrine|salada /.test(
      n
    )
  )
    return "fixo";
  if (/^(peça de|ilha de|bar de)/.test(n)) return "estacao";
  return "geral";
}

function cap(s: string): string {
  return s ? s.charAt(0).toUpperCase() + s.slice(1) : s;
}

const TEMPLATES: Record<TipoPrato, (rec: string) => { ing: [string, number, string][]; modo: string }> = {
  massa_recheada: (rec) => ({
    ing: [
      ["Massa (folhada, philo ou torradinha artesanal)", 350, "g"],
      [`Recheio de ${rec}`, 450, "g"],
      ["Creme ou queijo de finalização", 150, "g"],
      ["Ervas e temperos", 50, "g"],
    ],
    modo: `Montar a base de massa em mini forminhas, rechear com o preparo de ${rec} e finalizar conforme a receita. Assar ou montar a frio conforme o tipo de massa, e servir gelado ou morno.`,
  }),
  stick: (rec) => ({
    ing: [
      [cap(rec), 500, "g"],
      ["Farinha de trigo", 100, "g"],
      ["Ovos", 100, "g"],
      ["Farinha panko", 150, "g"],
      ["Óleo para fritura", 100, "g"],
      ["Sal e temperos", 50, "g"],
    ],
    modo: `Cortar ${rec} em formato de palito, empanar em farinha, ovo e panko, e fritar em óleo quente até dourar. Servir com o molho de acompanhamento indicado.`,
  }),
  frito_moldado: (rec) => ({
    ing: [
      ["Base (massa, batata ou proteína conforme receita)", 500, "g"],
      [`Recheio de ${rec}`, 300, "g"],
      ["Farinha panko para empanar", 150, "g"],
      ["Óleo para fritura", 50, "g"],
    ],
    modo: `Preparar a massa/base, moldar em porções pequenas com o recheio de ${rec}, empanar e fritar até dourar por igual.`,
  }),
  harumaki: (rec) => ({
    ing: [
      ["Massa de rolinho primavera", 300, "g"],
      [`Recheio de ${rec}`, 500, "g"],
      ["Óleo para fritura", 150, "g"],
      ["Molho de acompanhamento", 50, "g"],
    ],
    modo: `Rechear a massa de rolinho com o preparo de ${rec}, enrolar bem fechado e fritar em óleo quente até dourar. Servir com o molho indicado.`,
  }),
  risoto: (rec) => ({
    ing: [
      ["Arroz arbóreo", 350, "g"],
      ["Caldo (legumes ou carne)", 400, "ml"],
      [cap(rec), 150, "g"],
      ["Manteiga e parmesão", 100, "g"],
    ],
    modo: `Refogar o arroz, adicionar o caldo aos poucos mexendo sempre. Nos últimos minutos, incorporar ${rec}. Finalizar com manteiga e parmesão.`,
  }),
  carpaccio: (rec) => ({
    ing: [
      [`${cap(rec)} fatiado bem fino`, 700, "g"],
      ["Azeite extravirgem", 100, "ml"],
      ["Limão siciliano", 50, "g"],
      ["Lascas de parmesão ou acompanhamento", 100, "g"],
      ["Sal e pimenta", 50, "g"],
    ],
    modo: `Fatiar ${rec} bem fino, dispor em prato ou base individual, regar com azeite e limão, e finalizar com o acompanhamento indicado na receita.`,
  }),
  tartar: (rec) => ({
    ing: [
      [`${cap(rec)} picado fino`, 700, "g"],
      ["Temperos cítricos e ervas", 150, "g"],
      ["Base (torradinha ou blini)", 150, "g"],
    ],
    modo: `Picar ${rec} finamente à faca, temperar na hora do serviço e montar sobre a base escolhida.`,
  }),
  ceviche: (rec) => ({
    ing: [
      [cap(rec), 600, "g"],
      ["Suco cítrico (limão)", 150, "ml"],
      ["Cebola roxa", 100, "g"],
      ["Coentro", 50, "g"],
      ["Temperos", 100, "g"],
    ],
    modo: `Marinar ${rec} no suco cítrico com cebola roxa e temperos por curto período antes do serviço. Finalizar com coentro fresco.`,
  }),
  mousse: (rec) => ({
    ing: [
      [cap(rec), 400, "g"],
      ["Creme de leite fresco", 400, "g"],
      ["Açúcar ou tempero conforme receita", 150, "g"],
      ["Estabilizante (gelatina, se necessário)", 50, "g"],
    ],
    modo: `Preparar a base de ${rec}, incorporar delicadamente ao creme batido em ponto de chantilly. Gelar antes de servir.`,
  }),
  massa_molho: (rec) => ({
    ing: [
      ["Massa (nhoque ou ravioli)", 600, "g"],
      ["Molho de acompanhamento", 350, "g"],
      ["Queijo para finalizar", 50, "g"],
    ],
    modo: `Cozinhar a massa em água fervente até subir à superfície, escorrer e finalizar no molho de ${rec} indicado na receita.`,
  }),
  proteina_principal: (rec) => ({
    ing: [
      ["Proteína principal", 600, "g"],
      ["Molho indicado na receita", 250, "g"],
      ["Acompanhamento indicado na receita", 150, "g"],
    ],
    modo: `Selar ou grelhar a proteína no ponto, finalizar com o molho de ${rec} e montar com o acompanhamento indicado.`,
  }),
  fixo: () => ({
    ing: [["Produto pronto para porcionamento", 1000, "g"]],
    modo: "Item fixo de ilha/antepasto — apenas porcionar e dispor na montagem, sem preparo culinário adicional.",
  }),
  estacao: () => ({
    ing: [],
    modo: "Estação/ilha estacionada — montar conforme descrição do item, com reposição contínua durante o evento.",
  }),
  geral: (rec) => ({
    ing: [
      [cap(rec), 700, "g"],
      ["Temperos e finalização", 300, "g"],
    ],
    modo: `Preparar ${rec} conforme técnica padrão da casa e finalizar para o serviço.`,
  }),
};

/**
 * Gera um rascunho de ficha técnica (ingredientes + modo de preparo) a partir do nome do prato.
 * É só um ponto de partida — deve ser revisado pela cozinha antes de ir para produção real.
 */
export function gerarFicha(nome: string): FichaGerada {
  const especial = RECEITAS_ESPECIAIS[nome];
  if (especial) {
    return {
      ingredientes: especial.ing.map(([n, qtd, unidade]) => ({ nome: n, qtd, unidade })),
      receita: especial.modo,
    };
  }
  const tipo = classificarPrato(nome);
  const rec = primeiraDescricao(nome) || nome;
  const gerado = TEMPLATES[tipo](rec);
  return {
    ingredientes: gerado.ing.map(([n, qtd, unidade]) => ({ nome: n, qtd, unidade })),
    receita: gerado.modo,
  };
}

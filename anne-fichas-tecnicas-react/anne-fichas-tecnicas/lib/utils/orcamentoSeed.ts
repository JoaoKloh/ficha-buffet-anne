import type {
  CategoriaCardapio,
  FuncaoEquipe,
  GrupoItens,
  ItemCardapio,
  ItemQtdValor,
} from "@/lib/models/orcamento";

/**
 * Catálogo inicial de um orçamento novo (cardápio, bebidas, descartáveis, equipe
 * e louças), com os preços de referência da planilha de precificação. Quando
 * existir endpoint de catálogo no backend, isto passa a vir da API.
 */

function prato(nome: string, gramas: number, valorUnit: number, porcaoPax: number, incluir: boolean): ItemCardapio {
  return { nome, gramas, valorUnit, porcaoPax, incluir };
}

function item(nome: string, qtd: number, valorUnit: number, incluir = qtd > 0): ItemQtdValor {
  return { nome, qtd, valorUnit, incluir };
}

/** Itens de aluguel começam desmarcados e zerados — só o preço de referência importa. */
function aluguel(nome: string, valorUnit: number): ItemQtdValor {
  return item(nome, 0, valorUnit, false);
}

function funcao(
  funcao: string,
  profissionalPorPax: number,
  qtd: number,
  valorUnit: number,
  auto: boolean
): FuncaoEquipe {
  return { funcao, profissionalPorPax, qtd, diarias: 1, valorUnit, auto };
}

const vazia = (categoria: string): CategoriaCardapio => ({ categoria, baseQtd: "pax", itens: [] });

export const SEED_CARDAPIO: CategoriaCardapio[] = [
  {
    categoria: "Coquetel — Itens Frios",
    baseQtd: "pax",
    itens: [
      prato("tule de gergilim com tartar de salmão", 15, 1.8, 1.5, true),
      prato("rosbife com creme camenbert na crosta de parmesão", 15, 1.6, 1.5, true),
      prato("macaron com creme azul e frutas vermelhas", 15, 2, 1, true),
      prato("blinis com cour cream salmão defumado", 15, 2, 1, true),
      prato("musse foi grass na torradinha", 15, 1, 1.1, true),
      prato("bolinha de provolone com melado", 15, 1.2, 3, true),
      prato("bolinha de feijoada com geleia de pimenta", 15, 1.6, 2, true),
      prato("coxinha de costela", 15, 2, 3, true),
      prato("croqueta de carne", 15, 2, 2, true),
      prato("croquete alemão com mostarda escura", 15, 2, 2, true),
    ],
  },
  {
    categoria: "Coquetel — Itens Quentes",
    baseQtd: "pax",
    itens: [
      prato("camarão empanado na loucinha", 30, 1.4, 1.5, true),
      prato("bacalhau brulle", 80, 1.4, 1.5, true),
      prato("ravioli com fonduta de queijos e shitake", 80, 1.4, 1.2, true),
      prato("mini escalope ao file molho cogumelos e baroa", 100, 1.4, 1.2, true),
      prato("Vol-au-vent de brie com parma", 15, 1.4, 0, false),
      prato("PROVOLONE COM ALHO PORO GELEIA DE PIMENTA", 15, 1, 0, false),
      prato("CROCANTE DE QUEIJO com melado", 15, 1, 0, false),
      prato("BOLINHA DE FEIJOADA", 15, 1.2, 0, false),
      prato("Coxinha de Costela com geleia de Manjericão", 15, 1.4, 0, false),
      prato("dadinho de tapioca", 15, 1.5, 0, false),
      prato("risoto grano padano com crisps de parma", 120, 3, 0, false),
    ],
  },
  { categoria: "Sanduíches / Matinais", baseQtd: "pax", itens: [prato("Lanchinho ....", 60, 1.4, 1.2, false)] },
  { categoria: "Finger Food", baseQtd: "pax", itens: [prato("Ceviche", 60, 3.8, 1, false)] },
  { categoria: "Ilha de Antepastos", baseQtd: "pax", itens: [prato("Salame", 1000, 55, 0.015, false)] },
  vazia("Saladas / Entradas Frias"),
  vazia("Acompanhamentos"),
  vazia("Massas e Molhos"),
  vazia("Carnes Brancas — Aves"),
  vazia("Carnes — Peixes e Frutos do Mar"),
  vazia("Carnes Vermelhas"),
  vazia("Sobremesas"),
  vazia("Lanche da Madrugada"),
  {
    categoria: "Alimentação da Equipe (Staff)",
    baseQtd: "staff",
    itens: [
      prato("Misto quente", 1, 2.5, 2, false),
      prato("Cachorro Quente", 1, 2.5, 2, true),
      prato("Pão com manteiga", 1, 1.2, 2, true),
      prato("Macarrão", 200, 0.75, 1, false),
      prato("Molho a bolonhesa", 120, 1.5, 1, false),
      prato("Molho com salsicha", 120, 1, 1, false),
      prato("Arroz", 200, 0.8, 1, true),
      prato("Feijão", 80, 0.8, 1, true),
      prato("Picadinho", 120, 3, 1, true),
      prato("Estrogonofe de frango", 120, 3, 1, false),
      prato("Calabresa acebolada", 90, 2.5, 1, false),
      prato("Salada", 30, 2, 1, true),
    ],
  },
];

export const SEED_BEBIDAS: GrupoItens[] = [
  {
    nome: "Refrigerantes e Águas",
    itens: [
      item("Coca Cola 2 lts", 12, 12),
      item("Coca Zero 2 lts", 12, 12),
      item("Guaraná 2 lts", 12, 12),
      item("Guaraná Zero 2 lts", 0, 12),
      item("Coca cola lata 355 ml", 0, 2.6),
      item("Coca zero lata 355 ml", 0, 2.6),
      item("Guaraná zero lata 355 ml", 0, 2.13),
      item("Guarana lata 355 ml", 0, 2.13),
      item("Água Natural 20 litros", 2, 14),
      item("Água natural 1,5 litros", 12, 2.8),
      item("Água natural 350 ml", 0, 1.6),
      item("água natural copo", 0, 0.49),
      item("Água com gás 1,5 lt", 0, 1.9),
      item("Água com gás 350 ml", 0, 1.6),
    ],
  },
  {
    nome: "Sucos (Polpa / Bag / Natural)",
    itens: [
      item("Frutas Vermelhas", 20, 7.2),
      item("Uva", 0, 4.2),
      item("Abacaxi", 0, 5.2),
      item("Manga", 0, 5.2),
      item("Maracujá", 0, 6.7),
      item("Tangerina", 0, 5.2),
      item("Laranja", 0, 2.9),
      item("Abacaxi", 0, 2.8),
      item("Uva", 0, 2.8),
      item("Limão", 0, 2.8),
      item("Maracujá", 0, 2.8),
      item("Tangerina", 0, 2.9),
      item("Melancia", 0, 5.7),
      item("Abacaxi", 0, 4.7),
      item("Laranja", 0, 2.6),
    ],
  },
  {
    nome: "Bebidas Quentes",
    itens: [
      item("Água quente", 0, 0.3),
      item("Leite Quente", 0, 3.5),
      item("Café Coado", 0, 1.7),
      item("capsula nespresso", 0, 1.8),
    ],
  },
  { nome: "Bebidas Alcoólicas (inclusas no serviço)", itens: [item("Cerveja Heineken", 0, 6)] },
];

export const SEED_GELO: ItemQtdValor[] = [
  item("Gelo cubo", 2, 28),
  item("Gelo Britado", 2, 28),
  item("Gelo cubo Cozinha", 1, 15),
  item("Gelo Seco", 1, 12),
];

export const SEED_DESCARTAVEIS: GrupoItens[] = [
  {
    nome: "Serviço",
    itens: [
      item("Guardanapo coquetel 50 unds", 15, 5.5),
      item("Guardanapo personalizado", 0, 0),
      item("Canudo biodegrável", 0, 0),
      item("Palitinho de bambu", 0, 0),
      item("Palito nozinho", 0, 0),
      item("Cápsula sifão", 0, 0),
    ],
  },
  {
    nome: "Armazenamento",
    itens: [
      item("Bandeja de isopor rasa", 0, 0.03),
      item("Bandeja de isopor funda", 0, 0.05),
      item("Pote redondo com tampa P", 0, 0.4583),
      item("Pote redondo com tampa M", 0, 0.5583),
      item("Pote redondo com tampa G", 0, 1.0292),
    ],
  },
  {
    nome: "Higiene",
    itens: [
      item("Bobina de Plástico filme 1000", 1, 97.99),
      item("Bobina de saco descartável 35x50", 0, 0.1198),
      item("Bobina de saco descartável 20x30", 0, 0.0998),
      item("Luva G", 0, 0.456),
      item("Luva M", 0, 0.456),
      item("Máscara descartável", 0, 1.96),
      item("Papel alumínio", 0, 0),
      item("Papel Manteiga", 0, 0),
      item("Papel Toalha", 0, 0),
      item("Plástico filme pequeno", 0, 0),
      item("Touca de cabelo", 0, 0),
    ],
  },
  {
    nome: "Limpeza",
    itens: [
      item("Alcóol 70", 4, 0.3),
      item("Alcool gel", 0, 3.5),
      item("Detergente", 1, 1.3),
      item("Saco de lixo 100 lts", 1, 0.8),
      item("Pano multiuso 30 m", 1, 14.38),
      item("Esponja de louça", 1, 1),
      item("Esponja verde sintética", 1, 1),
    ],
  },
];

export const SEED_EQUIPE: FuncaoEquipe[] = [
  funcao("Cozinheiro extra para produção", 50, 0, 250, false),
  funcao("Chef", 1, 1, 350, false),
  funcao("Cozinheiro", 50, 2, 250, true),
  funcao("Copeiro", 120, 1, 200, true),
  funcao("Coordenador", 0, 0, 500, true),
  funcao("Maître", 150, 1, 250, true),
  funcao("Atendentes", 10, 10, 200, true),
  funcao("Atendente Vip (noivos / diretoria)", 0, 0, 0, true),
  funcao("Cambuza", 150, 1, 180, true),
  funcao("Auxiliar de produção", 100, 1, 180, true),
  funcao("Garçom Bilingue", 0, 0, 250, true),
  funcao("Bartender", 0, 0, 300, true),
  funcao("Outro", 0, 0, 0, true),
  funcao("Outro", 0, 0, 0, true),
];

export const SEED_LOUCAS: GrupoItens[] = [
  {
    nome: "Copos e Taças",
    itens: [
      aluguel("Copo caldereta", 0.8),
      aluguel("Copo old Fashioned", 0.8),
      aluguel("Copo shot", 0.5),
      aluguel("Taça de espumante (Volante)", 1.5),
      aluguel("Taça de Água (Volante)", 1.5),
      aluguel("Taça de vinho tinto (volante)", 1.5),
      aluguel("Taça de vinho branco (Volante)", 1.5),
      aluguel("Taça de espumante (montado na mesa)", 1.5),
      aluguel("Taça de Água (montado na mesa)", 1.5),
      aluguel("Taça de vinho tinto (montado na mesa)", 1.5),
      aluguel("Taça de vinho branco (montado na mesa)", 1.5),
      aluguel("Taça de chopp", 0.8),
      aluguel("Taça Champanhe Vintage (Coupe)", 1.4),
      aluguel("Taça dry martini", 1.4),
      aluguel("Taça irish coffee", 0.7),
      aluguel("Whisky Curto / On the rocks", 1),
      aluguel("Whisky Longo / Long drink", 1),
    ],
  },
  {
    nome: "Material de Bebidas",
    itens: [
      aluguel("Balde de gelo", 35),
      aluguel("Bandeja de antiderrapante (preta)", 8),
      aluguel("Bandeja de inox", 8),
      aluguel("Bandeja de prata", 8),
      aluguel("Bule de inox reposição café", 6),
      aluguel("Champanheira 1 garrafa", 0),
      aluguel("Champanheira 3 garrafas", 45),
      aluguel("Colher bailarina", 1),
      aluguel("Forro de bandeja", 15),
      aluguel("Garrafa térmica", 12),
      aluguel("Isopor para gelo britado 12 kg (Para cambuza / copa)", 15),
      aluguel("Isopor para gelo cubo 5kg (Para cambuza / copa)", 15),
      aluguel("Jarra de plástico para cambuza / copa", 0),
      aluguel("Jarra de prata", 30),
      aluguel("Jarra de vidro", 8),
      aluguel("Samovar", 30),
      aluguel("Suqueira", 20),
      aluguel("Tina Grande (Caixa plástica para gelar bebida)", 15),
      aluguel("Tina Pequena", 0),
    ],
  },
  {
    nome: "Miniaturas / Finger Food",
    itens: [
      aluguel("Mini pratinho quadrado de porcelana", 0.7),
      aluguel("Mini tigelinha canelada 6.5 (alt 3.5) (ramequim)", 0.75),
      aluguel("Mini forma canelada g d.8,5 a.5cm 120ml (ramequim)", 0.75),
      aluguel("Mini Tigelinha Dupla", 1),
      aluguel("Mini cubinho porcelana", 0.8),
      aluguel("Colher chinesa", 0.8),
      aluguel("Taça de licor", 1),
      aluguel("Taça bolinha", 1),
      aluguel("Mini bico de jaca transparente", 0.8),
    ],
  },
  {
    nome: "Bandejas e Salvinhas",
    itens: [
      aluguel("Total de bandejas de canapé", 15),
      aluguel("Bandeja redonda", 0),
      aluguel("Bandeja quadrada", 0),
      aluguel("Bandeja retangular", 0),
      aluguel("Salvinha redonda de prata", 6),
      aluguel("Salvinha redonda de vidro", 3),
    ],
  },
  {
    nome: "Pratos e Xícaras",
    itens: [
      aluguel("Bowl", 1.1),
      aluguel("cumbuca", 1.1),
      aluguel("Panelinha com alça", 1.1),
      aluguel("Prato de mesa", 1.5),
      aluguel("Prato de pão (Montagem de mesa)", 1.2),
      aluguel("Prato de sobremesa", 1.2),
      aluguel("Prato gourmet (disco voador)", 2.2),
      aluguel("Xícara de café (evento social / coquetel)", 0.7),
      aluguel("Xícara de chá (evento social / coquetel)", 0.7),
      aluguel("Xícara de café (coffee break)", 0.7),
      aluguel("Xícara de café (coffee break)", 0.7),
    ],
  },
  {
    nome: "Talheres",
    itens: [
      aluguel("Colher de Arroz", 6),
      aluguel("Colher de café (coffee break)", 0),
      aluguel("Colher de café (para café de saída)", 1.1),
      aluguel("Colher de café (para docinho ou finger food)", 1.1),
      aluguel("Colher de chá (coffee break)", 1.1),
      aluguel("Colher de chá (mesa de saída ev social)", 1.1),
      aluguel("Colher de chá (para sobremesa ou finger)", 1.1),
      aluguel("Colher de mesa (buffet)", 0),
      aluguel("Colher de mesa (empratado)", 0),
      aluguel("Colher de sobremesa (buffet)", 1.3),
      aluguel("Colher de sobremesa (empratado)", 0),
      aluguel("Concha", 4),
      aluguel("Conchinha", 3),
      aluguel("Faca de mesa (montada na mesa)", 1.8),
      aluguel("Faca de mesa (no buffet)", 1.8),
      aluguel("Faca de peixe (montada na mesa)", 1.8),
      aluguel("Faca de sobremesa (montada na mesa)", 1.8),
      aluguel("Garfo de mesa (montada na mesa)", 1.8),
      aluguel("Garfo de mesa no buffet)", 1.8),
      aluguel("Garfo de Peixe (montada na mesa)", 1.8),
      aluguel("Garfo de siri (finger food)", 1.1),
      aluguel("Garfo de sobremesa (ilha ou prato quente)", 1.8),
      aluguel("Garfo de sobremesa (sobremesa)", 1.8),
      aluguel("Pá de torta / Espátula de bolo", 3),
      aluguel("Pegador Tesoura", 6),
      aluguel("Pegador pinça", 2),
    ],
  },
  {
    nome: "Tecidos e Sousplat",
    itens: [
      aluguel("Guardanapo (no buffet)", 1.8),
      aluguel("Jogo americano", 3.5),
      aluguel("Toalha para buffet pranchão 2m", 18),
      aluguel("Toalha redonda para prancha de 1m", 15),
      aluguel("caminho de mesa", 8),
      aluguel("bandeja de colo", 0),
      aluguel("Sousplat", 5),
    ],
  },
  {
    nome: "Material Buffet — Itens Quentes",
    itens: [
      aluguel("Caçarola reposição 10 litros", 0),
      aluguel("Caçarola reposição 4 litros", 0),
      aluguel("Caçarola reposição 6 litros", 0),
      aluguel("Frigideira Maître de Hotel", 0),
      aluguel("GN redonda chafing dish", 0),
      aluguel("Gn reposição chafing dish", 0),
      aluguel("Rechaud Banho maria + Caçarola 10 litros", 0),
      aluguel("Rechaud Banho maria + Caçarola 4 litros", 0),
      aluguel("Rechaud Banho maria + Caçarola 6 litros", 0),
      aluguel("Rechaud Banho Maria Redondo - Chafing dish", 0),
      aluguel("Rechaud Banho Maria retangular - Chafing dish", 0),
      aluguel("Rechaud chapa redondo", 30),
      aluguel("Rechaud chapa retangular", 0),
      aluguel("Travessa redonda 40 cm", 0),
      aluguel("Travessa redonda 50 cm", 0),
      aluguel("Travessa redonda com alça", 0),
      aluguel("Travessa retangular 58 cm", 0),
      aluguel("Travessa retangular 70 cm", 0),
      aluguel("Travessa retangular com alça", 0),
    ],
  },
  {
    nome: "Material Buffet — Itens Frios",
    itens: [
      aluguel("Saladeira aberta 50 cm", 30),
      aluguel("Frutereira de vidro", 0),
      aluguel("Altura 12x12", 0),
      aluguel("Altura 15x15", 0),
      aluguel("Altura 18x18", 0),
      aluguel("Altura 24x24", 0),
      aluguel("Prato de bolo com pé 35 diam 9 cm alt", 0),
      aluguel("Prato de bolo com pé 35 diam 15 cm alt", 0),
      aluguel("Prato de bolo com pé 35 diam 22 cm alt", 0),
      aluguel("Tábua de madeira 100 x 39c", 0),
      aluguel("Tábua de prata 120 x 30", 0),
      aluguel("Roda gigante", 0),
      aluguel("Sorveteira", 0),
      aluguel("Molheira", 0),
    ],
  },
  {
    nome: "Estrutura",
    itens: [
      aluguel("Estufa a gás", 200),
      aluguel("Fogão 2 bocas", 80),
      aluguel("Fogão de indução", 180),
      aluguel("Forno a gás", 180),
      aluguel("Forno Elétrico", 90),
      aluguel("Fritadeira", 150),
      aluguel("Placa de canapé", 0),
      aluguel("Plate-mate (carrinho de transporte de pratos) 200", 0),
      aluguel("Pranchão de 1 metro", 0),
      aluguel("Pranchão de 2 metros", 0),
    ],
  },
];

export const SEED_PACOTE_BEBIDAS: ItemQtdValor[] = [
  item("Cerveja Heineken 355ml", 200, 4.6, false),
  item("Cerveja Stella Artois 275ml", 200, 3.65, false),
  item("Chopp Heineken 50L", 2, 750, false),
  item("Chopp Brahma 50L", 2, 650, false),
  item("Espumante Corte Viola 750ml", 33, 45, false),
  item("Vinho Tinto Cosecha Tarapacá 750ml", 25, 47, false),
  item("Whisky Black Label", 5, 125, false),
];

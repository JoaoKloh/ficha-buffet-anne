package com.jbkloh.ficha_buffet.calculo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.jbkloh.ficha_buffet.dtos.resp.DeParaSugestaoDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalImportadaDTO;
import com.jbkloh.ficha_buffet.dtos.resp.NotaFiscalResponseDTO;
import com.jbkloh.ficha_buffet.exceptions.AppException;

/** Regras das notas fiscais: totais, conversão de unidade, de-para do fornecedor e leitura do XML da NF-e. */
public final class NotasFiscais {

    private NotasFiscais() {}

    private static final Map<String, String> FORMA_POR_TPAG = Map.of("01", "dinheiro", "03", "cartao", "04", "cartao", "05", "prazo", "15", "boleto",
        "16", "transferencia", "17", "pix", "18", "transferencia", "99", "prazo");

    /** Quantidade da nota na unidade de estoque: em "compra", multiplica pelo fator da embalagem. */
    public static double qtdConvertida(double qtdNota, String unid, Double fatorCompra) {
        double f = "compra".equals(unid) && fatorCompra != null && fatorCompra > 0 ? fatorCompra : 1;
        return Formato.r3(qtdNota * f);
    }

    public static double somaItens(List<NotaFiscalResponseDTO.Item> itens) {
        return Formato.round2(Formato.soma(itens, NotaFiscalResponseDTO.Item::valor));
    }

    public static double total(List<NotaFiscalResponseDTO.Item> itens, double acrescimo) {
        return Formato.round2(somaItens(itens) + acrescimo);
    }

    /** Sugere o item do cadastro pelo histórico de notas do mesmo fornecedor (código, depois descrição). */
    public static DeParaSugestaoDTO deParaSugestao(Dados d, String fornecedor, String descricao, String codigo) {
        String f = Formato.normTxt(fornecedor);
        String desc = Formato.normTxt(descricao);
        String cod = Formato.texto(codigo).trim();
        if (f.isEmpty() || (desc.isEmpty() && cod.isEmpty())) {
            return null;
        }
        List<NotaFiscalResponseDTO> nfs = d.nfs.stream().filter(n -> Formato.normTxt(n.fornecedor()).equals(f))
            .sorted(Comparator.comparing(NotaFiscalResponseDTO::emissao).reversed()).toList();
        for (boolean porCodigo : new boolean[] { true, false }) {
            if (porCodigo ? cod.isEmpty() : desc.isEmpty()) {
                continue;
            }
            for (NotaFiscalResponseDTO n : nfs) {
                for (NotaFiscalResponseDTO.Item it : n.itens()) {
                    boolean casa = porCodigo ? cod.equals(Formato.texto(it.cod()).trim()) : desc.equals(Formato.normTxt(it.desc()));
                    if (casa && d.insumo(it.insumoId()) != null) {
                        return new DeParaSugestaoDTO(it.insumoId(), Formato.vazio(it.unid()) ? "estoque" : it.unid());
                    }
                }
            }
        }
        return null;
    }

    public static List<String> fornecedores(Dados d) {
        TreeSet<String> nomes = new TreeSet<>(Formato.porNome(s -> s));
        d.nfs.stream().map(NotaFiscalResponseDTO::fornecedor).filter(s -> !Formato.vazio(s)).forEach(nomes::add);
        d.insumos.stream().map(i -> i.fornecedor()).filter(s -> !Formato.vazio(s)).forEach(nomes::add);
        return new ArrayList<>(nomes);
    }

    /* --------------------------------- XML da NF-e --------------------------------- */

    private static Pattern elemento(String tag) {
        return Pattern.compile("<" + tag + "(\\s[^>]*)?>([\\s\\S]*?)</" + tag + ">");
    }

    /** Texto do primeiro elemento `tag` dentro de `xml`. */
    private static String tx(String xml, String tag) {
        Matcher m = elemento(tag).matcher(xml);
        return m.find() ? m.group(2).replaceAll("<!\\[CDATA\\[([\\s\\S]*?)\\]\\]>", "$1").trim() : "";
    }

    private static double nv(String xml, String tag) {
        try {
            double n = Double.parseDouble(tx(xml, tag));
            return Double.isFinite(n) ? n : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<String> blocos(String xml, String tag) {
        List<String> out = new ArrayList<>();
        Matcher m = elemento(tag).matcher(xml);
        while (m.find()) {
            out.add(m.group(2));
        }
        return out;
    }

    private static AppException xmlInvalido(String mensagem) {
        return new AppException(mensagem, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /** Lê o XML de uma NF-e (modelo 55) e devolve a nota pré-preenchida, sem ligar as linhas ao cadastro. */
    public static NotaFiscalImportadaDTO lerNFeXML(String txt) {
        if (Pattern.compile("<parsererror", Pattern.CASE_INSENSITIVE).matcher(txt).find() || !txt.stripLeading().startsWith("<")) {
            throw xmlInvalido("O arquivo não é um XML válido.");
        }
        Matcher infM = elemento("infNFe").matcher(txt);
        if (!infM.find()) {
            throw xmlInvalido("Este XML não é de uma NF-e (falta o bloco infNFe). Se for NFS-e ou NFC-e, lance manualmente.");
        }
        String inf = infM.group(2);
        Matcher idM = Pattern.compile("Id=\"([^\"]+)\"").matcher(Formato.texto(infM.group(1)));
        String chave = idM.find() ? idM.group(1).replaceFirst("^NFe", "") : "";
        if (chave.isEmpty()) {
            chave = tx(txt, "chNFe");
        }
        String ide = tx(inf, "ide");
        String emit = tx(inf, "emit");
        String cobr = tx(inf, "cobr");
        List<NotaFiscalResponseDTO.Item> itens = new ArrayList<>();
        for (String det : blocos(inf, "det")) {
            String pr = tx(det, "prod");
            String im = tx(det, "imposto");
            double custo = nv(pr, "vProd") - nv(pr, "vDesc") + nv(pr, "vFrete") + nv(pr, "vSeg") + nv(pr, "vOutro") + nv(tx(im, "IPI"), "vIPI")
                + nv(im, "vICMSST");
            itens.add(new NotaFiscalResponseDTO.Item(tx(pr, "xProd"), tx(pr, "cProd"), tx(pr, "uCom"), null, nv(pr, "qCom"), "estoque", 0, null,
                Formato.round2(custo), false));
        }
        double vNF = Formato.round2(nv(tx(inf, "ICMSTot"), "vNF"));
        if (vNF == 0) {
            vNF = somaItens(itens);
        }
        // Diferença entre o total da nota e a soma das linhas (frete, arredondamento) vai para a última linha.
        double dif = Formato.round2(vNF - somaItens(itens));
        if (!itens.isEmpty() && Math.abs(dif) >= 0.01) {
            NotaFiscalResponseDTO.Item u = itens.get(itens.size() - 1);
            itens.set(itens.size() - 1, new NotaFiscalResponseDTO.Item(u.desc(), u.cod(), u.uCom(), null, u.qtdNota(), "estoque", 0, null,
                Formato.round2(u.valor() + dif), false));
        }
        String dh = tx(ide, "dhEmi").isEmpty() ? tx(ide, "dEmi") : tx(ide, "dhEmi");
        String emissao = dh.length() >= 10 && dh.substring(0, 10).matches(Dominio.RE_DATA) ? dh.substring(0, 10) : Formato.hoje();
        List<NotaFiscalResponseDTO.Parcela> parcelas = new ArrayList<>();
        for (String dup : cobr.isEmpty() ? List.<String>of() : blocos(cobr, "dup")) {
            String venc = tx(dup, "dVenc");
            parcelas.add(new NotaFiscalResponseDTO.Parcela(venc.matches(Dominio.RE_DATA) ? venc : emissao, Formato.round2(nv(dup, "vDup")), false));
        }
        String forma = !parcelas.isEmpty() ? "boleto" : FORMA_POR_TPAG.getOrDefault(tx(tx(inf, "detPag"), "tPag"), "boleto");
        if (parcelas.isEmpty()) {
            parcelas.add(new NotaFiscalResponseDTO.Parcela(emissao, vNF, false));
        }
        String fantasia = tx(emit, "xFant");
        String cnpj = tx(emit, "CNPJ");
        return new NotaFiscalImportadaDTO(chave, tx(ide, "nNF"), tx(ide, "serie"), emissao, fantasia.isEmpty() ? tx(emit, "xNome") : fantasia,
            tx(emit, "xNome"), cnpj.isEmpty() ? tx(emit, "CPF") : cnpj, itens, vNF, 0, forma, parcelas, true, false, List.of(), null);
    }
}

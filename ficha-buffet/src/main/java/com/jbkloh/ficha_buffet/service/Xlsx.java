package com.jbkloh.ficha_buffet.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Gerador mínimo de planilha .xlsx (uma aba por lista, cabeçalho na primeira linha), sem dependências. */
final class Xlsx {

    private Xlsx() {}

    /** Aba da planilha: células são texto ou número; nulo sai vazio. */
    record Aba(String nome, List<String> colunas, List<List<Object>> linhas) {}

    static byte[] gerar(List<Aba> abas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            StringBuilder tipos = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
            StringBuilder pasta = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
            StringBuilder rels = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
            for (int i = 0; i < abas.size(); i++) {
                int n = i + 1;
                tipos.append("<Override PartName=\"/xl/worksheets/sheet").append(n)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                // O Excel aceita até 31 caracteres no nome da aba.
                String nome = abas.get(i).nome().length() > 31 ? abas.get(i).nome().substring(0, 31) : abas.get(i).nome();
                pasta.append("<sheet name=\"").append(xml(nome)).append("\" sheetId=\"").append(n).append("\" r:id=\"rId").append(n).append("\"/>");
                rels.append("<Relationship Id=\"rId").append(n)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(n).append(".xml\"/>");
                arquivo(zip, "xl/worksheets/sheet" + n + ".xml", planilha(abas.get(i)));
            }
            arquivo(zip, "[Content_Types].xml", tipos.append("</Types>").toString());
            arquivo(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            arquivo(zip, "xl/workbook.xml", pasta.append("</sheets></workbook>").toString());
            arquivo(zip, "xl/_rels/workbook.xml.rels", rels.append("</Relationships>").toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static String planilha(Aba aba) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        linha(sb, List.copyOf(aba.colunas()));
        aba.linhas().forEach(l -> linha(sb, l));
        return sb.append("</sheetData></worksheet>").toString();
    }

    private static void linha(StringBuilder sb, List<?> celulas) {
        sb.append("<row>");
        for (Object c : celulas) {
            if (c instanceof Number n) {
                sb.append("<c><v>").append(n).append("</v></c>");
            } else {
                sb.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(c == null ? "" : c.toString())).append("</t></is></c>");
            }
        }
        sb.append("</row>");
    }

    private static String xml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
    }

    private static void arquivo(ZipOutputStream zip, String caminho, String conteudo) throws IOException {
        zip.putNextEntry(new ZipEntry(caminho));
        zip.write(conteudo.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}

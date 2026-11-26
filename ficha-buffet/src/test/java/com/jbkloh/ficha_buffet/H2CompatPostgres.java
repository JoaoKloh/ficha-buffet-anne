package com.jbkloh.ficha_buffet;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Funções do PostgreSQL que o código usa em SQL nativo e o H2 dos testes não tem.
 * Registradas no H2 por src/test/resources/h2-compat-postgres.sql (INIT da URL).
 *
 * Em produção a RestauracaoBackup posiciona a sequence com
 * {@code setval(pg_get_serial_sequence(tabela, 'id'), proximo, false)}; aqui o
 * mesmo efeito vem do ALTER TABLE ... RESTART do H2.
 */
public final class H2CompatPostgres {

    private H2CompatPostgres() {}

    /** Devolve "tabela.coluna", que o setval abaixo entende. */
    public static String pgGetSerialSequence(String tabela, String coluna) {
        return tabela + "." + coluna;
    }

    public static long setval(Connection conexao, String sequencia, long valor, boolean chamado) throws SQLException {
        int ponto = sequencia.lastIndexOf('.');
        String tabela = sequencia.substring(0, ponto);
        String coluna = sequencia.substring(ponto + 1);
        if (!tabela.matches("[A-Za-z_][A-Za-z0-9_]*") || !coluna.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new SQLException("Identificador inválido: " + sequencia);
        }
        long proximo = chamado ? valor + 1 : valor;
        try (Statement st = conexao.createStatement()) {
            st.execute("ALTER TABLE " + tabela + " ALTER COLUMN " + coluna + " RESTART WITH " + proximo);
        }
        return valor;
    }
}

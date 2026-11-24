package com.jbkloh.ficha_buffet.calculo;

import java.math.RoundingMode;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import org.springframework.http.HttpStatus;

import com.jbkloh.ficha_buffet.exceptions.AppException;

/** Datas em "AAAA-MM-DD", arredondamentos e textos em pt-BR usados pelos cálculos e pelas telas. */
public final class Formato {

    private Formato() {}

    private static final Locale PT_BR = Locale.of("pt", "BR");

    public static String hoje() {
        return LocalDate.now().toString();
    }

    public static String agora() {
        return Instant.now().toString();
    }

    public static boolean vazio(String s) {
        return s == null || s.isEmpty();
    }

    public static String texto(String s) {
        return s == null ? "" : s;
    }

    public static String addDays(String iso, long n) {
        return LocalDate.parse(iso).plusDays(n).toString();
    }

    public static String addMonths(String iso, long n) {
        return LocalDate.parse(iso).plusMonths(n).toString();
    }

    /** a − b em dias. */
    public static long diffDays(String a, String b) {
        return ChronoUnit.DAYS.between(LocalDate.parse(b), LocalDate.parse(a));
    }

    public static double round2(double n) {
        return Math.round(n * 100) / 100.0;
    }

    public static double r3(double n) {
        return Math.round(n * 1000) / 1000.0;
    }

    public static <T> double soma(List<T> lista, ToDoubleFunction<T> valor) {
        double s = 0;
        for (T x : lista) {
            s += valor.applyAsDouble(x);
        }
        return s;
    }

    /** Data obrigatória de um campo de entrada; inválida vira 400. */
    public static LocalDate data(String iso, String campo) {
        try {
            return LocalDate.parse(iso);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new AppException("Data inválida em " + campo + ".", HttpStatus.BAD_REQUEST);
        }
    }

    /** Data opcional: nula ou vazia vira null. */
    public static LocalDate dataOpcional(String iso, String campo) {
        return vazio(iso) ? null : data(iso, campo);
    }

    /** Hora opcional "HH:MM": nula ou vazia vira null. */
    public static LocalTime horaOpcional(String hora) {
        if (vazio(hora)) {
            return null;
        }
        try {
            return LocalTime.parse(hora);
        } catch (DateTimeParseException e) {
            throw new AppException("Hora inválida: use HH:MM.", HttpStatus.BAD_REQUEST);
        }
    }

    public static String iso(LocalDate d) {
        return d == null ? null : d.toString();
    }

    public static String hora(LocalTime t) {
        return t == null ? "" : t.toString().substring(0, 5);
    }

    /** "DD/MM/AAAA" ou travessão. */
    public static String fdate(String iso) {
        return vazio(iso) ? "—" : iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    /** " · DD/MM/AAAA" para rótulos de evento; vazio sem data. */
    public static String sufixoData(String iso) {
        return vazio(iso) ? "" : " · " + fdate(iso);
    }

    public static String brl(double n) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(n);
    }

    /** Número com até 3 casas, no formato pt-BR. */
    public static String fmtN(double n) {
        return decimal(n, 3);
    }

    /** Quantidade: inteira em un/dz/maço/garrafa/botijão; até 2 casas abaixo de 10 e 1 casa acima. */
    public static String fmtQ(double n, String unidade) {
        if (!Double.isFinite(n)) {
            return "—";
        }
        if (Dominio.UNIDADES_INTEIRAS.contains(unidade)) {
            return String.valueOf((long) Math.ceil(n - 1e-9));
        }
        return decimal(n, n < 10 ? 2 : 1);
    }

    private static String decimal(double n, int casas) {
        DecimalFormat f = new DecimalFormat("#,##0." + "#".repeat(casas), DecimalFormatSymbols.getInstance(PT_BR));
        f.setRoundingMode(RoundingMode.HALF_UP);
        return f.format(n);
    }

    /** Ordem alfabética do português (ignora acentos e caixa). */
    public static <T> Comparator<T> porNome(Function<T, String> nome) {
        Collator collator = Collator.getInstance(PT_BR);
        return (a, b) -> collator.compare(texto(nome.apply(a)), texto(nome.apply(b)));
    }

    /** Texto sem acentos, em minúsculas, só com letras, números e espaços simples. */
    public static String normTxt(String s) {
        return java.text.Normalizer.normalize(texto(s), java.text.Normalizer.Form.NFD)
            .replaceAll("[\\u0300-\\u036f]", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static boolean contem(String texto, String buscaMinuscula) {
        return texto(texto).toLowerCase(PT_BR).contains(buscaMinuscula);
    }

    public static String busca(String busca) {
        return texto(busca).trim().toLowerCase(PT_BR);
    }
}

package com.jbkloh.ficha_buffet.model.json;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Parcela combinada na nota fiscal: vencimento, valor e se já foi paga. */
public record ParcelaNf(LocalDate venc, BigDecimal valor, Boolean pago) {}

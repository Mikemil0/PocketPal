package com.pocketpal.app.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formato de montos (§5.2): "NumberFormat/DecimalFormat con el símbolo elegido;
 * COP sin decimales y con punto de miles ($480.000)".
 *
 * Solo el formato de COP aparece en el Figma; USD, MXN y EUR se formatean con
 * 2 decimales y símbolo ISO como aproximación (registrado en §14 del SDD).
 */
object MoneyFormat {

    fun format(amount: Double?, currency: String): String {
        val value = amount ?: 0.0
        return when (currency.uppercase(Locale.ROOT)) {
            "EUR" -> SYMBOL_EURO + grouped(value, decimals = 2)
            "USD", "MXN" -> SYMBOL_DOLLAR + grouped(value, decimals = 2)
            else -> SYMBOL_DOLLAR + grouped(value, decimals = 0)
        }
    }

    /**
     * Monto del campo "límite mensual" mientras se escribe: solo dígitos, con el
     * símbolo y los separadores de la moneda ($600.000, §8.9).
     */
    fun formatInput(digits: String, currency: String): String {
        if (digits.isEmpty()) return ""
        val value = digits.toLongOrNull() ?: return ""
        return SYMBOL_DOLLAR + grouped(value.toDouble(), decimals = 0)
    }

    /** Cantidad de movimientos de una fila ("48 movimientos"); 0 hasta el Módulo 3 (§6). */
    private fun grouped(value: Double, decimals: Int): String {
        val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val pattern = if (decimals == 0) "#,##0" else "#,##0.00"
        return DecimalFormat(pattern, symbols).format(value)
    }

    private const val SYMBOL_DOLLAR = "$"
    private const val SYMBOL_EURO = "€"
}

package com.typingfrontier.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {

    /**
     * Formata o valor monetário para a nova moeda "Frons".
     * Regras: 
     * - abaixo de 1.000: número completo;
     * - 1.000 ou mais: K;
     * - 1.000.000 ou mais: KK;
     * - 1.000.000.000 ou mais: KKK;
     * - Máximo de duas casas decimais, vírgula como separador, remove zeros desnecessários.
     */
    fun formatar(valor: Int): String {
        return when {
            valor >= 1_000_000_000 -> formatarDecimal(valor.toDouble() / 1_000_000_000.0) + "KKK Frons"
            valor >= 1_000_000 -> formatarDecimal(valor.toDouble() / 1_000_000.0) + "KK Frons"
            valor >= 1_000 -> formatarDecimal(valor.toDouble() / 1_000.0) + "K Frons"
            else -> "$valor Frons"
        }
    }

    private fun formatarDecimal(valor: Double): String {
        val symbols = DecimalFormatSymbols(Locale("pt", "BR"))
        symbols.decimalSeparator = ','
        val df = DecimalFormat("#.##", symbols)
        return df.format(valor)
    }

    /**
     * Exibe um diálogo informativo com o valor exato, utilizando separador de milhares.
     */
    fun mostrarSaldoExato(context: Context, valor: Int) {
        val symbols = DecimalFormatSymbols(Locale("pt", "BR"))
        symbols.groupingSeparator = '.'
        val df = DecimalFormat("#,###", symbols)
        val valorFormatado = df.format(valor)

        AlertDialog.Builder(context)
            .setTitle("Saldo Detalhado")
            .setMessage("Saldo: $valorFormatado Frons")
            .setPositiveButton("OK", null)
            .show()
    }
}

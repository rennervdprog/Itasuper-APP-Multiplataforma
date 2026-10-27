package com.example.shared.util

/**
 * Máscaras e validações de documentos/telefone — migração 1:1 do
 * `com.example.ui.utils.Masks` do app Android. Kotlin puro, sem
 * dependências de plataforma (pronto para commonMain).
 */
object Masks {

    fun formatCpfCnpj(input: String): String {
        val digits = input.filter { it.isDigit() }.take(14)
        return if (digits.length <= 11) {
            // Format as CPF: 000.000.000-00
            val sb = StringBuilder()
            for (i in digits.indices) {
                if (i == 3 || i == 6) sb.append('.')
                if (i == 9) sb.append('-')
                sb.append(digits[i])
            }
            sb.toString()
        } else {
            // Format as CNPJ: 00.000.000/0001-00
            val sb = StringBuilder()
            for (i in digits.indices) {
                if (i == 2 || i == 5) sb.append('.')
                if (i == 8) sb.append('/')
                if (i == 12) sb.append('-')
                sb.append(digits[i])
            }
            sb.toString()
        }
    }

    fun formatPhone(input: String): String {
        val digits = input.filter { it.isDigit() }.take(11)
        val sb = StringBuilder()
        if (digits.isNotEmpty()) {
            sb.append('(')
            val ddd = digits.take(2)
            sb.append(ddd)
            if (digits.length >= 2) sb.append(") ")

            val rest = digits.drop(2)
            if (rest.length > 4 && digits.length == 11) {
                sb.append(rest.take(5)).append('-').append(rest.drop(5))
            } else if (rest.length > 4) {
                sb.append(rest.take(4)).append('-').append(rest.drop(4))
            } else {
                sb.append(rest)
            }
        }
        return sb.toString()
    }

    fun isValidCpfOrCnpj(input: String): Boolean {
        val digits = input.filter { it.isDigit() }
        if (digits.length == 11) {
            return isValidCpf(digits)
        } else if (digits.length == 14) {
            return isValidCnpj(digits)
        }
        return false
    }

    private fun isValidCpf(cpf: String): Boolean {
        if (cpf.all { it == cpf[0] }) return false
        try {
            val numbers = cpf.map { it.toString().toInt() }
            var sum1 = 0
            for (i in 0..8) {
                sum1 += numbers[i] * (10 - i)
            }
            var rev1 = 11 - (sum1 % 11)
            if (rev1 == 10 || rev1 == 11) rev1 = 0
            if (rev1 != numbers[9]) return false

            var sum2 = 0
            for (i in 0..9) {
                sum2 += numbers[i] * (11 - i)
            }
            var rev2 = 11 - (sum2 % 11)
            if (rev2 == 10 || rev2 == 11) rev2 = 0
            return rev2 == numbers[10]
        } catch (e: Exception) {
            return false
        }
    }

    private fun isValidCnpj(cnpj: String): Boolean {
        if (cnpj.all { it == cnpj[0] }) return false
        try {
            val numbers = cnpj.map { it.toString().toInt() }
            val weight1 = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
            var sum1 = 0
            for (i in 0..11) {
                sum1 += numbers[i] * weight1[i]
            }
            var rev1 = 11 - (sum1 % 11)
            if (rev1 >= 10) rev1 = 0
            if (rev1 != numbers[12]) return false

            val weight2 = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
            var sum2 = 0
            for (i in 0..12) {
                sum2 += numbers[i] * weight2[i]
            }
            var rev2 = 11 - (sum2 % 11)
            if (rev2 >= 10) rev2 = 0
            return rev2 == numbers[13]
        } catch (e: Exception) {
            return false
        }
    }

    fun isValidPhone(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits.length in 10..11
    }

    /** Formata o CEP como `XXXXX-XXX` (igual ao `formatCep` da web). */
    fun formatCep(input: String): String {
        val digits = input.filter { it.isDigit() }.take(8)
        if (digits.length <= 5) return digits
        return "${digits.take(5)}-${digits.drop(5)}"
    }
}

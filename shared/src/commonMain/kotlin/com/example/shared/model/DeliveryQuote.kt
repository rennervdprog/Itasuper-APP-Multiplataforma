package com.example.shared.model

/** Converte nomes de estados recebidos de GPS/perfil para a sigla esperada pela resolução oficial. */
fun normalizeBrazilianUf(value: String): String {
    val normalized = value.trim()
        .uppercase()
        .replace(Regex("[ÀÁÂÃÄÅ]"), "A")
        .replace(Regex("[ÈÉÊË]"), "E")
        .replace(Regex("[ÌÍÎÏ]"), "I")
        .replace(Regex("[ÒÓÔÕÖ]"), "O")
        .replace(Regex("[ÙÚÛÜ]"), "U")
        .replace(Regex("[ÝŸ]"), "Y")
        .replace(Regex("[Ç]"), "C")
        .replace(Regex("[Ñ]"), "N")
        .replace(Regex("\\s+"), " ")
    val fullNames = mapOf(
        "ACRE" to "AC", "ALAGOAS" to "AL", "AMAPA" to "AP", "AMAZONAS" to "AM",
        "BAHIA" to "BA", "CEARA" to "CE", "DISTRITO FEDERAL" to "DF", "ESPIRITO SANTO" to "ES",
        "GOIAS" to "GO", "MARANHAO" to "MA", "MATO GROSSO" to "MT", "MATO GROSSO DO SUL" to "MS",
        "MINAS GERAIS" to "MG", "PARA" to "PA", "PARAIBA" to "PB", "PARANA" to "PR",
        "PERNAMBUCO" to "PE", "PIAUI" to "PI", "RIO DE JANEIRO" to "RJ", "RIO GRANDE DO NORTE" to "RN",
        "RIO GRANDE DO SUL" to "RS", "RONDONIA" to "RO", "RORAIMA" to "RR", "SANTA CATARINA" to "SC",
        "SAO PAULO" to "SP", "SERGIPE" to "SE", "TOCANTINS" to "TO"
    )
    return fullNames[normalized] ?: normalized.take(2)
}

/**
 * Endereço enviado para a cotação central. O backend continua sendo a fonte
 * definitiva de normalização, coordenadas, elegibilidade e preço.
 */
data class DeliveryAddressInput(
    val street: String,
    val number: String,
    val complement: String = "",
    val neighborhood: String,
    val city: String,
    val state: String,
    val cep: String
) {
    fun normalizedCep(): String = cep.filter(Char::isDigit)

    fun normalizedState(): String = normalizeBrazilianUf(state)

    /** Igual ao Capacitor: cidade e UF são opcionais no cliente e o backend as completa pelo CEP. */
    fun isComplete(): Boolean = street.trim().isNotBlank() &&
        number.trim().isNotBlank() &&
        neighborhood.trim().isNotBlank() &&
        normalizedCep().length == 8

    fun requestKey(storeId: String?, subtotal: Double): String = listOf(
        storeId.orEmpty(),
        subtotal.toString().let { if ("." in it) it else "$it.00" },
        street.trim(),
        number.trim(),
        complement.trim(),
        neighborhood.trim(),
        city.trim(),
        normalizedState(),
        normalizedCep()
    ).joinToString("|")
}

data class DeliveryQuoteDestination(
    val normalizedAddress: String,
    val cep: String,
    val city: String,
    val state: String,
    val neighborhood: String,
    val latitude: Double,
    val longitude: Double,
    val precision: String
)

data class DeliveryQuoteDistance(
    val km: Double = 0.0,
    val source: String = "haversine",
    val maxKm: Double? = null,
    val eligible: Boolean = false
)

data class DeliveryQuotePricing(
    val storeDeliveryBase: Double = 0.0,
    val platformFeeCustomer: Double = 0.0,
    val platformFeeStoreAbsorbed: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val vipOverrideApplied: Double? = null,
    val splitMode: String? = null,
    val planType: String? = null,
    val freeDeliveryApplied: Boolean = false
)

data class DeliveryQuote(
    val fulfillment: String,
    val destination: DeliveryQuoteDestination?,
    val distance: DeliveryQuoteDistance,
    val pricing: DeliveryQuotePricing,
    val policyVersion: Int = 1
) {
    val isSuccessfulDelivery: Boolean
        get() = fulfillment.equals("delivery", ignoreCase = true) &&
            destination != null &&
            destination.latitude.isFinite() &&
            destination.longitude.isFinite() &&
            pricing.deliveryFee.isFinite()

    companion object {
        fun pickup(): DeliveryQuote = DeliveryQuote(
            fulfillment = "pickup",
            destination = null,
            distance = DeliveryQuoteDistance(eligible = true),
            pricing = DeliveryQuotePricing(deliveryFee = 0.0),
            policyVersion = 1
        )
    }
}

data class DeliveryQuoteFailure(
    val reason: String = "quote_unreachable",
    val distanceKm: Double? = null,
    val maxDistanceKm: Double? = null
) {
    fun userMessage(): String = when (reason) {
        "delivery_unavailable" -> "Esta loja não está aceitando pedidos de entrega no momento."
        "no_driver_available" -> "Esta loja está sem entregador disponível no momento. Você ainda pode escolher retirada."
        "delivery_availability_unavailable" -> "Não foi possível confirmar a disponibilidade de entrega agora. Tente novamente em instantes."
        "outside_delivery_area" -> {
            if (distanceKm != null && maxDistanceKm != null) {
                "O endereço de entrega está a ${((distanceKm * 10).toInt() / 10.0).toString().replace(".", ",")} km da loja. Limite de ${((maxDistanceKm * 10).toInt() / 10.0).toString().replace(".", ",")} km."
            } else {
                "O endereço informado está fora da área de entrega da loja."
            }
        }
        "address_unavailable", "address_not_found" -> "Não localizamos esse endereço. Revise rua, número, bairro e CEP."
        "missing_street" -> "Informe a rua para calcular a entrega."
        "missing_number" -> "Informe o número do endereço para calcular a entrega."
        "missing_neighborhood" -> "Informe o bairro para calcular a entrega."
        "missing_city", "invalid_state" -> "Não foi possível completar cidade e UF pelo CEP. Revise o CEP e tente novamente."
        "unauthorized" -> "Sua sessão expirou. Entre novamente para confirmar o endereço."
        "invalid_cep" -> "Informe um CEP válido com 8 dígitos para calcular a entrega."
        else -> "Não foi possível calcular a entrega agora. Seu carrinho foi preservado — tente novamente em instantes."
    }
}

data class DeliveryQuoteResult(
    val quote: DeliveryQuote? = null,
    val failure: DeliveryQuoteFailure? = null
) {
    val isSuccess: Boolean get() = quote?.isSuccessfulDelivery == true
}

/** Snapshot serializado no metadata do pedido no instante da criação. */
data class DeliveryQuoteSnapshot(
    val policyVersion: Int,
    val distanceKm: Double,
    val distanceSource: String,
    val maxDeliveryKm: Double?,
    val destinationPrecision: String,
    val storeDeliveryBase: Double,
    val platformFeeCustomer: Double,
    val platformFeeStoreAbsorbed: Double,
    val deliveryFee: Double,
    val vipOverrideApplied: Double?,
    val splitMode: String?,
    val planType: String?,
    val freeDeliveryApplied: Boolean
)

fun DeliveryQuote.toSnapshot(): DeliveryQuoteSnapshot = DeliveryQuoteSnapshot(
    policyVersion = policyVersion,
    distanceKm = distance.km,
    distanceSource = distance.source,
    maxDeliveryKm = distance.maxKm,
    destinationPrecision = destination?.precision ?: "cep",
    storeDeliveryBase = pricing.storeDeliveryBase,
    platformFeeCustomer = pricing.platformFeeCustomer,
    platformFeeStoreAbsorbed = pricing.platformFeeStoreAbsorbed,
    deliveryFee = pricing.deliveryFee,
    vipOverrideApplied = pricing.vipOverrideApplied,
    splitMode = pricing.splitMode,
    planType = pricing.planType,
    freeDeliveryApplied = pricing.freeDeliveryApplied
)

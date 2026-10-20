package com.example.shared.network

import com.example.shared.model.LegalAcceptanceMode
import com.example.shared.model.LegalAcceptanceResult
import com.example.shared.model.LegalChange
import com.example.shared.model.Order
import com.example.shared.model.PendingLegalChanges
import com.example.shared.model.AddonItem
import com.example.shared.model.PizzaAddonGroup
import com.example.shared.model.PizzaBorder
import com.example.shared.model.PizzaSizeCatalogItem
import com.example.shared.model.Product
import com.example.shared.model.PizzaLegacySize
import com.example.shared.model.SavedAddress
import com.example.shared.model.Coupon
import com.example.shared.model.Store
import com.example.shared.model.StoreOpeningHour
import com.example.shared.model.StoreSettings
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.delete
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.round

/**
 * Fase 4 — rede real (Ktor, commonMain).
 *
 * Porte dos endpoints do SupabaseClient do app Android (OkHttp manual) para o
 * HttpClient Ktor compartilhado ([createDefaultHttpClient]). O parsing usa
 * kotlinx.serialization (JsonObject, estilo opt* do app original) em vez de
 * org.json, que não existe no commonMain.
 *
 * Endpoints migrados, nesta ordem:
 *  (a) listagem de lojas   -> [fetchActiveStores]
 *  (b) produtos da loja    -> [fetchProductsForStore]
 *  (c) auth login/cadastro -> [signIn]/[signUp]
 *  (d) criação de pedido   -> [submitOrder]
 *
 * Apenas a Home (a) está ligada aos dados reais por enquanto; o restante
 * continua com mocks + TODO(fase-rede) nas telas até ser ligado.
 */

/** Resultado do catálogo: uma lista vazia pode ser uma resposta operacional válida. */
data class StoreCatalogResult(
    val isSuccess: Boolean,
    val stores: List<Store>
)

data class SupabaseAuthResponse(
    val isSuccess: Boolean,
    val userId: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long? = null,
    val email: String? = null,
    val errorMessage: String? = null
)

data class OrderSubmissionResponse(
    val isSuccess: Boolean,
    val orderId: String? = null,
    val createdAt: String? = null,
    val errorMessage: String? = null
)

data class SupabaseOperationResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

/** Porte de RemoteCustomerProfile do app Android (SupabaseClient.kt). */
data class RemoteCustomerProfile(
    val fullName: String = "",
    val email: String = "",
    val document: String = "",
    val whatsapp: String = "",
    val deliveryPin: String = "",
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val referencePoint: String = "",
    val pixType: String = "",
    val pixKey: String = "",
    val termsVersionAccepted: String = "",
    val privacyVersionAccepted: String = ""
)

/** Porte de CepAddress do app Android (resposta do ViaCEP). */
data class CepAddress(
    val cep: String,
    val street: String,
    val neighborhood: String,
    val city: String,
    val state: String
)

/** Porte de PixPaymentResponse do app Android (SupabaseClient.kt). */
data class PixPaymentResponse(
    val isSuccess: Boolean,
    val pixCode: String? = null,
    val qrCodeBase64: String? = null,
    val errorMessage: String? = null
)

object SupabaseApi {

    private const val MIN_CLIENT_CATALOG_PRODUCTS = 5

    private val client by lazy { createDefaultHttpClient() }

    private val json = Json { ignoreUnknownKeys = true }

    // ------------------------------------------------------------------
    // Helpers de parsing (estilo opt* do app Android, sobre JsonObject)
    // ------------------------------------------------------------------

    private fun JsonObject.optString(key: String, default: String = ""): String =
        (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull ?: default

    private fun JsonObject.optNullableString(key: String): String? {
        val s = (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
            ?: return null
        return s.takeIf { it != "null" }
    }

    private fun JsonObject.optDouble(key: String, default: Double = 0.0): Double =
        (this[key] as? JsonPrimitive)?.doubleOrNull ?: default

    private fun JsonObject.optBoolean(key: String, default: Boolean = false): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull ?: default

    private fun JsonObject.optInt(key: String, default: Int = 0): Int =
        (this[key] as? JsonPrimitive)?.intOrNull ?: default

    /** Objeto aninhado que pode vir como JsonObject ou como string JSON (colunas JSONB). */
    private fun JsonObject.optJsonObject(key: String): JsonObject? {
        val el = this[key] ?: return null
        if (el is JsonObject) return el
        val s = (el as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.startsWith("{") }
            ?: return null
        return runCatching { json.parseToJsonElement(s).jsonObject }.getOrNull()
    }

    private fun JsonObject.optJsonArray(key: String): JsonArray? = this[key] as? JsonArray

    private fun parseArray(text: String): JsonArray =
        runCatching { json.parseToJsonElement(text).jsonArray }
            .getOrElse { JsonArray(emptyList()) }

    private fun JsonObjectBuilder.putBlankAsNull(key: String, value: String) {
        putStr(key, value.ifBlank { null })
    }

    // JsonObjectBuilder.put só aceita JsonElement nesta versão do
    // kotlinx.serialization — estes helpers envolvem os primitivos.
    private fun JsonObjectBuilder.putStr(key: String, value: String?) {
        put(key, if (value == null) JsonNull else JsonPrimitive(value))
    }

    private fun JsonObjectBuilder.putNum(key: String, value: Number?) {
        put(key, if (value == null) JsonNull else JsonPrimitive(value))
    }

    private fun JsonObjectBuilder.putBool(key: String, value: Boolean) {
        put(key, JsonPrimitive(value))
    }

    /** "R$ 4,90" — sem String.format (indisponível no commonMain). */
    private fun formatBrl(value: Double): String {
        val cents = round(value * 100)
        val intPart = (cents / 100).toLong()
        val decPart = kotlin.math.abs(cents % 100).toLong()
        return "R$ $intPart,${decPart.toString().padStart(2, '0')}"
    }

    private fun parseErrorMessage(jsonText: String, defaultMsg: String): String {
        return try {
            val obj = json.parseToJsonElement(jsonText).jsonObject
            obj.optNullableString("error_description")
                ?: obj.optNullableString("msg")
                ?: obj.optNullableString("message")
                ?: defaultMsg
        } catch (_: Exception) {
            defaultMsg
        }
    }

    // ------------------------------------------------------------------
    // Camada HTTP (Ktor) — espelha os headers do app Android
    // ------------------------------------------------------------------

    private suspend fun restGetRaw(url: String, bearer: String = SUPABASE_ANON_KEY): Pair<Boolean, String> {
        return try {
            val response: HttpResponse = client.get(url) {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $bearer")
                header("Accept", "application/json")
            }
            response.status.isSuccess() to response.bodyAsText()
        } catch (_: Exception) {
            false to ""
        }
    }

    private suspend fun restPostRaw(
        url: String,
        bodyJson: String,
        bearer: String = SUPABASE_ANON_KEY,
        prefer: String? = null
    ): Pair<Boolean, String> {
        return try {
            val response: HttpResponse = client.post(url) {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $bearer")
                header("Accept", "application/json")
                if (prefer != null) header("Prefer", prefer)
                contentType(ContentType.Application.Json)
                setBody(bodyJson)
            }
            response.status.isSuccess() to response.bodyAsText()
        } catch (_: Exception) {
            false to ""
        }
    }

    private suspend fun restPatchRaw(
        url: String,
        bodyJson: String,
        bearer: String = SUPABASE_ANON_KEY,
        prefer: String? = null
    ): Pair<Boolean, String> {
        return try {
            val response: HttpResponse = client.patch(url) {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $bearer")
                header("Accept", "application/json")
                if (prefer != null) header("Prefer", prefer)
                contentType(ContentType.Application.Json)
                setBody(bodyJson)
            }
            response.status.isSuccess() to response.bodyAsText()
        } catch (_: Exception) {
            false to ""
        }
    }

    private suspend fun restDeleteRaw(
        url: String,
        bearer: String = SUPABASE_ANON_KEY,
        prefer: String? = null
    ): Pair<Boolean, String> {
        return try {
            val response: HttpResponse = client.delete(url) {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $bearer")
                header("Accept", "application/json")
                if (prefer != null) header("Prefer", prefer)
            }
            response.status.isSuccess() to response.bodyAsText()
        } catch (_: Exception) {
            false to ""
        }
    }

    private suspend fun authPostRaw(path: String, bodyJson: String): Pair<Boolean, String> {
        return try {
            val response: HttpResponse = client.post("$SUPABASE_URL/auth/v1/$path") {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                contentType(ContentType.Application.Json)
                setBody(bodyJson)
            }
            response.status.isSuccess() to response.bodyAsText()
        } catch (_: Exception) {
            false to ""
        }
    }

    // ------------------------------------------------------------------
    // (a) LISTAGEM DE LOJAS — porte de SupabaseClient.fetchActiveStores
    // ------------------------------------------------------------------

    private data class OpeningHour(
        val storeId: String,
        val dayOfWeek: Int,
        val dayOfWeekStr: String,
        val openTime: String,
        val closeTime: String,
        val isClosedAllDay: Boolean
    )

    private suspend fun fetchOpeningHours(): List<OpeningHour> {
        val (ok, text) = restGetRaw("$SUPABASE_URL/rest/v1/opening_hours?select=*")
        if (!ok || text.isBlank()) return emptyList()
        return try {
            parseArray(text).mapNotNull { el ->
                val item = el.jsonObject
                val storeId = item.optString("store_id")
                if (storeId.isBlank()) return@mapNotNull null
                OpeningHour(
                    storeId = storeId,
                    dayOfWeek = item.optInt("day_of_week", -1),
                    dayOfWeekStr = item.optString("day_of_week", ""),
                    openTime = item.optString("open_time", "08:00"),
                    closeTime = item.optString("close_time", "22:00"),
                    isClosedAllDay = item.optBoolean("is_closed_all_day", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun formatDeliveryTime(raw: String, settings: JsonObject?): String {
        val normalized = raw.trim()
        val hasValidRawValue = normalized.isNotBlank() &&
            !normalized.equals("null", ignoreCase = true) &&
            !normalized.equals("undefined", ignoreCase = true) &&
            normalized != "-"
        if (hasValidRawValue) {
            return if (normalized.contains("min", ignoreCase = true)) normalized else "$normalized min"
        }
        val min = settings?.optDouble("delivery_time_min", Double.NaN) ?: Double.NaN
        val max = settings?.optDouble("delivery_time_max", Double.NaN) ?: Double.NaN
        return if (min.isFinite() && max.isFinite() && min > 0 && max > 0) {
            "${min.toInt()}-${max.toInt()} min"
        } else {
            ""
        }
    }

    private fun checkIsStoreOpenNow(
        storeId: String,
        isForceClosed: Boolean,
        defaultIsOpen: Boolean,
        openingHours: List<OpeningHour>
    ): Boolean {
        if (isForceClosed) return false
        val storeHours = openingHours.filter { it.storeId == storeId }
        if (storeHours.isEmpty()) return defaultIsOpen

        // Mesmo fuso e convenção do app Android/web: América/São Paulo, domingo = 0.
        val now = Clock.System.now().toLocalDateTime(TimeZone.of("America/Sao_Paulo"))
        val dayOfWeek = (now.dayOfWeek.ordinal + 1) % 7
        val currentMinutes = now.hour * 60 + now.minute

        val todayHour = storeHours.firstOrNull { it.dayOfWeek == dayOfWeek } ?: storeHours.find { hour ->
            when {
                hour.dayOfWeekStr.equals("domingo", true) && dayOfWeek == 0 -> true
                hour.dayOfWeekStr.equals("segunda", true) && dayOfWeek == 1 -> true
                (hour.dayOfWeekStr.equals("terca", true) || hour.dayOfWeekStr.equals("terça", true)) && dayOfWeek == 2 -> true
                hour.dayOfWeekStr.equals("quarta", true) && dayOfWeek == 3 -> true
                hour.dayOfWeekStr.equals("quinta", true) && dayOfWeek == 4 -> true
                hour.dayOfWeekStr.equals("sexta", true) && dayOfWeek == 5 -> true
                (hour.dayOfWeekStr.equals("sabado", true) || hour.dayOfWeekStr.equals("sábado", true)) && dayOfWeek == 6 -> true
                else -> false
            }
        }

        if (todayHour == null) return defaultIsOpen
        if (todayHour.isClosedAllDay) return false

        fun parseMinutes(timeStr: String): Int {
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                return h * 60 + m
            }
            return 0
        }

        val openMin = parseMinutes(todayHour.openTime)
        val closeMin = parseMinutes(todayHour.closeTime)
        if (openMin == closeMin && openMin == 0) return true
        return if (closeMin > openMin) {
            currentMinutes in openMin..closeMin
        } else {
            currentMinutes >= openMin || currentMinutes <= closeMin
        }
    }

    /** Taxa-base ao cliente via RPC pública (mesma usada pela cotação). null = indisponível. */
    suspend fun fetchOfficialCustomerDeliveryFee(storeId: String): Double? {
        if (storeId.isBlank()) return null
        val body = buildJsonObject { putStr("_store_id", storeId) }.toString()
        val (ok, text) = restPostRaw("$SUPABASE_URL/rest/v1/rpc/compute_store_delivery_fee", body)
        if (!ok || text.isBlank()) return null
        return try {
            val el = json.parseToJsonElement(text)
            val obj = when (el) {
                is JsonArray -> el.firstOrNull()?.jsonObject
                is JsonObject -> el
                else -> null
            } ?: return null
            obj.optDouble("customer_total", Double.NaN).takeIf { it.isFinite() && it >= 0.0 }
        } catch (_: Exception) {
            null
        }
    }

    /** IDs de lojas com ao menos um entregador online (RPC pública). null = RPC indisponível. */
    suspend fun fetchStoreIdsWithOnlineDrivers(): Set<String>? {
        val (ok, text) = restPostRaw("$SUPABASE_URL/rest/v1/rpc/stores_with_online_drivers", "{}")
        if (!ok || text.isBlank()) return null
        return try {
            parseArray(text).mapNotNull { el ->
                (el as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
            }.toSet()
        } catch (_: Exception) {
            null
        }
    }

    /** WhatsApp público dos donos das lojas (fallback do campo whatsapp da loja). */
    private suspend fun fetchStoreOwnerWhatsapps(ownerIds: List<String>): Map<String, String> {
        if (ownerIds.isEmpty()) return emptyMap()
        val ownerFilter = ownerIds.joinToString(",")
        val (ok, text) = restGetRaw(
            "$SUPABASE_URL/rest/v1/profiles?select=user_id,whatsapp_number&user_id=in.($ownerFilter)"
        )
        if (!ok || text.isBlank()) return emptyMap()
        return try {
            buildMap {
                parseArray(text).forEach { el ->
                    val profile = el.jsonObject
                    val userId = profile.optString("user_id").trim()
                    val whatsapp = profile.optString("whatsapp_number").trim()
                    if (userId.isNotBlank() && whatsapp.isNotBlank()) put(userId, whatsapp)
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** Mesmo conceito de "produto visível no catálogo" do detalhe da loja. */
    private fun isClientCatalogProduct(item: JsonObject): Boolean {
        val name = item.optString("name").trim()
        if (name.isBlank() || item.optBoolean("sold_by_weight", false)) return false
        val metadata = item.optJsonObject("metadata") ?: return true
        return metadata.optBoolean("pdv_only", false) != true &&
            metadata.optBoolean("hidden", false) != true
    }

    private suspend fun fetchClientProductCount(storeId: String): Int? {
        if (storeId.isBlank()) return null
        var offset = 0
        var visibleCount = 0
        val pageSize = 100
        repeat(100) {
            val (ok, body) = restGetRaw(
                "$SUPABASE_URL/rest/v1/products?select=id,name,sold_by_weight,metadata" +
                    "&store_id=eq.$storeId&order=id.asc&limit=$pageSize&offset=$offset"
            )
            if (!ok) return null
            val page = parseArray(body)
            for (element in page) {
                if (isClientCatalogProduct(element.jsonObject)) visibleCount++
                if (visibleCount >= MIN_CLIENT_CATALOG_PRODUCTS) return MIN_CLIENT_CATALOG_PRODUCTS
            }
            if (page.size < pageSize) return visibleCount
            offset += pageSize
        }
        return visibleCount
    }

    suspend fun fetchActiveStores(): StoreCatalogResult {
        return try {
            val openingHours = fetchOpeningHours()

            var (ok, responseText) = restGetRaw(
                "$SUPABASE_URL/rest/v1/stores_public?select=*&status=eq.ativo&order=rating.desc"
            )
            // Fallback se o enum de status causar erro no Postgres (mesmo do app Android).
            if (!ok) {
                val fallback = restGetRaw("$SUPABASE_URL/rest/v1/stores_public?select=*&order=rating.desc")
                ok = fallback.first
                responseText = fallback.second
            }
            if (!ok || responseText.isBlank()) return StoreCatalogResult(false, emptyList())

            val jsonArray = parseArray(responseText)
            val ownerWhatsappById = fetchStoreOwnerWhatsapps(
                jsonArray.mapNotNull { el ->
                    el.jsonObject.optString("owner_id", "").takeIf { it.isNotBlank() }
                }.distinct()
            )

            val storeList = mutableListOf<Store>()
            for (element in jsonArray) {
                val item = element.jsonObject
                val status = item.optString("status", "ativo")
                if (status.equals("inativo", ignoreCase = true) ||
                    status.equals("bloqueado", ignoreCase = true) ||
                    status.equals("desativado", ignoreCase = true) ||
                    status.equals("inactive", ignoreCase = true) ||
                    status.equals("blocked", ignoreCase = true) ||
                    status.equals("disabled", ignoreCase = true)
                ) continue

                val id = item.optString("id")
                val name = item.optString("name", "Loja ItaSuper")
                val category = item.optString("category", "Geral")
                val rating = item.optDouble("rating", 5.0).let { if (it.isNaN()) 5.0 else it }
                val imageUrl = item.optString(
                    "logo_url",
                    item.optString("image_url", item.optString("banner_url", ""))
                )
                val bannerUrl = item.optString("banner_url", imageUrl)
                val isForceClosed = item.optBoolean("force_closed", false)
                val defaultIsOpen = item.optBoolean("is_open", true)
                val preorderEnabled = item.optBoolean("preorder_enabled", false)
                val preorderMinutesBefore = item.optInt("preorder_minutes_before", 0).coerceAtLeast(0)
                val computedIsOpen = checkIsStoreOpenNow(id, isForceClosed, defaultIsOpen, openingHours)

                val ownFee = item.optDouble("own_delivery_fee", 0.0)
                val deliveryFeeType = item.optString("delivery_fee_type", "fixed")
                val deliveryBaseKm = item.optDouble("delivery_base_km", 0.0)
                val deliveryFeeBase = item.optDouble("delivery_fee_base", ownFee)
                val deliveryFeePerKm = item.optDouble("delivery_fee_per_km", 0.0)
                val platformFee = item.optDouble("delivery_fee", 0.0)
                val deliveryMode = item.optString("delivery_mode", "platform")
                val platformFeeSplit = item.optString("platform_fee_split", "cliente")
                val planType = item.optString("plan_type", "")
                val autonomyLifetimeFree = item.optBoolean("autonomy_lifetime_free", false)
                val pixDirectEnabled = item.optBoolean("pix_direto_enabled", false)
                val pixDirectKey = item.optString("pix_direto_key", "").trim()
                val pixDirectKeyType = item.optString("pix_direto_key_type", "").trim()
                val pixDirectBeneficiary = item.optString("pix_direto_beneficiary", "").trim()
                val pixDirectInstructions = item.optString("pix_direto_instructions", "").trim()
                val platformDeliverySplitOverride =
                    (item["platform_delivery_split_override"] as? JsonPrimitive)?.doubleOrNull
                val isAutonomy = planType.equals("autonomy", true) || autonomyLifetimeFree
                val platformSplit = if (isAutonomy) 0.0 else (platformDeliverySplitOverride ?: 0.99)
                val platformAddToCustomer = when (platformFeeSplit.lowercase()) {
                    "meio_a_meio" -> round((platformSplit / 2.0) * 100.0) / 100.0
                    "lojista" -> 0.0
                    else -> platformSplit
                }
                val rawFee = when (deliveryMode.lowercase()) {
                    "pickup" -> 0.0
                    "platform" -> platformFee
                    "own", "direto" -> ownFee + platformAddToCustomer
                    else -> platformFee
                }
                val isFree = deliveryMode.equals("pickup", true) || rawFee <= 0.0
                val deliveryFeeText = when {
                    deliveryMode.equals("pickup", true) -> "Retirada"
                    rawFee > 0.0 -> formatBrl(rawFee)
                    else -> "Grátis"
                }
                val rawDeliveryTime = item.optString("estimated_delivery_time", "").trim()
                val minimumOrder = item.optDouble("minimum_order_value", 0.0)
                val createdAt = item.optString("created_at", "")
                val lat = (item["latitude"] as? JsonPrimitive)?.doubleOrNull
                val lng = (item["longitude"] as? JsonPrimitive)?.doubleOrNull
                val addressStreet = item.optString("address_street", "").trim()
                val addressNumber = item.optString("address_number", "").trim()
                val addressNeighborhood = item.optString("address_neighborhood", "").trim()
                val addressCity = item.optString("address_city", "").trim()
                val addressState = item.optString("address_state", "").trim()
                val addressCep = item.optString("address_cep", "").trim()
                val addressReference = item.optString("address_reference", "").trim()
                val storeAddress = listOf(
                    listOf(addressStreet, addressNumber).filter { it.isNotBlank() }.joinToString(", "),
                    addressNeighborhood,
                    listOf(addressCity, addressState).filter { it.isNotBlank() }.joinToString(" - ")
                ).filter { it.isNotBlank() }.joinToString(", ")
                val ownerId = item.optString("owner_id", "").trim()
                val storeWhatsapp = item.optString("whatsapp", item.optString("phone", "")).trim()
                    .ifBlank { ownerWhatsappById[ownerId].orEmpty() }

                val storeOpeningHours = openingHours.filter { it.storeId == id }.map { oh ->
                    StoreOpeningHour(
                        storeId = oh.storeId,
                        dayOfWeek = oh.dayOfWeek,
                        dayOfWeekStr = oh.dayOfWeekStr,
                        openTime = oh.openTime,
                        closeTime = oh.closeTime,
                        isClosedAllDay = oh.isClosedAllDay
                    )
                }

                val secCats = mutableListOf<String>()
                val secArr = item.optJsonArray("secondary_categories") ?: item.optJsonArray("categories")
                if (secArr != null) {
                    for (catEl in secArr) {
                        val catVal = (catEl as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
                        if (catVal.isNotBlank() && catVal != "null") secCats.add(catVal)
                    }
                } else {
                    val secStr = item.optString("secondary_categories", item.optString("categories", ""))
                    if (secStr.isNotBlank()) secCats.addAll(secStr.split(",").map { it.trim() })
                }

                val settingsObj = item.optJsonObject("settings")
                val pizzaConfig = settingsObj?.optJsonObject("pizza_config")
                val pastelConfig = settingsObj?.optJsonObject("pastel_config")
                val pizzaSizesCatalog = buildList {
                    val sizes = settingsObj?.optJsonArray("pizza_sizes_catalog")
                    sizes?.forEach { sizeEl ->
                        val size = sizeEl.jsonObject
                        val sizeId = size.optString("id").trim()
                        val sizeName = size.optString("name").trim()
                        if (sizeId.isNotBlank() && sizeName.isNotBlank()) {
                            add(
                                PizzaSizeCatalogItem(
                                    id = sizeId,
                                    name = sizeName,
                                    description = size.optString("description").trim(),
                                    maxFlavors = size.optInt("maxFlavors", size.optInt("max_flavors", 4))
                                        .coerceIn(1, 4),
                                    active = size.optBoolean("active", true)
                                )
                            )
                        }
                    }
                }
                val pizzaPriceMatrix = buildMap<String, Map<String, Double>> {
                    val matrix = settingsObj?.optJsonObject("pizza_price_matrix")
                    matrix?.keys?.forEach { categoryId ->
                        val categoryMatrix = matrix.optJsonObject(categoryId) ?: return@forEach
                        val prices = buildMap<String, Double> {
                            categoryMatrix.keys.forEach { sizeId ->
                                val price = categoryMatrix.optDouble(sizeId, 0.0)
                                if (price > 0.0) put(sizeId, price)
                            }
                        }
                        if (prices.isNotEmpty()) put(categoryId, prices)
                    }
                }
                val storeSettings = StoreSettings(
                    pizzaHalfEnabled = settingsObj?.optBoolean("pizza_half_enabled", true) ?: true,
                    pastelHalfEnabled = settingsObj?.optBoolean("pastel_half_enabled", true) ?: true,
                    pizzaMaxFlavors = pizzaConfig?.optInt("max_flavors", 4)
                        ?: (settingsObj?.optInt("pizza_max_flavors", 4) ?: 4),
                    pastelMaxFlavors = pastelConfig?.optInt("max_flavors", 4)
                        ?: (settingsObj?.optInt("pastel_max_flavors", 4) ?: 4),
                    pastelMaxComplements = pastelConfig?.optInt("max_complements", 3)
                        ?: (settingsObj?.optInt("pastel_max_complements", 3) ?: 3),
                    pizzaPriceMode = settingsObj?.optString("pizza_price_mode", "maior") ?: "maior",
                    pizzaSizesCatalog = pizzaSizesCatalog,
                    pizzaPriceMatrix = pizzaPriceMatrix,
                    pastelPriceMode = settingsObj?.optString("pastel_price_mode", "maior") ?: "maior",
                    pizzaSingleSize = settingsObj?.optBoolean("pizza_single_size", false) ?: false,
                    pastelSingleSize = settingsObj?.optBoolean("pastel_single_size", false) ?: false,
                    acceptPixOnline = settingsObj?.optBoolean("accept_pix_online", false) ?: false,
                    acceptPixMachine = settingsObj?.optBoolean("accept_pix_machine", false) ?: false,
                    acceptCard = settingsObj?.optBoolean("accept_card", true) ?: true,
                    acceptCash = settingsObj?.optBoolean("accept_cash", true) ?: true
                )

                storeList.add(
                    Store(
                        id = id,
                        name = name,
                        category = category,
                        secondaryCategories = secCats,
                        rating = rating,
                        deliveryTime = formatDeliveryTime(rawDeliveryTime, settingsObj),
                        deliveryFee = deliveryFeeText,
                        isFreeDelivery = isFree,
                        isOpen = computedIsOpen,
                        forceClosed = isForceClosed,
                        preorderEnabled = preorderEnabled,
                        preorderMinutesBefore = preorderMinutesBefore,
                        distanceKm = null,
                        logoUrl = imageUrl,
                        bannerUrl = bannerUrl,
                        minOrder = minimumOrder,
                        createdAt = createdAt,
                        slug = item.optString("slug", ""),
                        status = status,
                        latitude = lat,
                        longitude = lng,
                        settings = storeSettings,
                        deliveryMode = deliveryMode,
                        ownDeliveryFee = ownFee,
                        deliveryFeeType = deliveryFeeType,
                        deliveryBaseKm = deliveryBaseKm,
                        deliveryFeeBase = deliveryFeeBase,
                        deliveryFeePerKm = deliveryFeePerKm,
                        platformDeliveryFee = platformFee,
                        platformFeeSplit = platformFeeSplit,
                        planType = planType,
                        platformDeliverySplitOverride = platformDeliverySplitOverride,
                        autonomyLifetimeFree = autonomyLifetimeFree,
                        pixDirectEnabled = pixDirectEnabled,
                        pixDirectKey = pixDirectKey,
                        pixDirectKeyType = pixDirectKeyType,
                        pixDirectBeneficiary = pixDirectBeneficiary,
                        pixDirectInstructions = pixDirectInstructions,
                        addressStreet = addressStreet,
                        addressNumber = addressNumber,
                        addressNeighborhood = addressNeighborhood,
                        addressCity = addressCity,
                        addressState = addressState,
                        addressCep = addressCep,
                        addressReference = addressReference,
                        address = storeAddress,
                        whatsapp = storeWhatsapp,
                        openingHours = storeOpeningHours
                    )
                )
            }

            // Regra de vitrine do cliente: lojas exclusivas de PDV ou com menos de
            // 5 produtos visíveis ficam fora (mesma regra do app Android).
            val productCounts = coroutineScope {
                storeList.map { store -> async { store.id to fetchClientProductCount(store.id) } }
                    .awaitAll().toMap()
            }
            val onlineDriverStoreIds = fetchStoreIdsWithOnlineDrivers()
            val catalogStores = storeList
                .filter { store ->
                    !store.planType.equals("pdv_only", ignoreCase = true) &&
                        (productCounts[store.id]?.let { it >= MIN_CLIENT_CATALOG_PRODUCTS } ?: true)
                }
                .map { sourceStore ->
                    // A falta de motoboy não remove a loja: bloqueia só a entrega.
                    val driverAvailabilityKnown = sourceStore.deliveryMode.equals("own", ignoreCase = true) &&
                        onlineDriverStoreIds != null
                    sourceStore.copy(
                        hasAvailableDriver = when {
                            sourceStore.deliveryMode.equals("own", ignoreCase = true) ->
                                onlineDriverStoreIds?.contains(sourceStore.id)
                            else -> true
                        },
                        deliveryAvailabilityMessage = when {
                            driverAvailabilityKnown && onlineDriverStoreIds?.contains(sourceStore.id) == false ->
                                "Esta loja está sem entregador disponível no momento."
                            else -> ""
                        }
                    )
                }

            // A vitrine consulta a mesma RPC da cotação para o preço-base de cada loja.
            val withOfficialFees = coroutineScope {
                catalogStores.map { store ->
                    async {
                        val officialFee = fetchOfficialCustomerDeliveryFee(store.id)
                        if (officialFee == null || store.deliveryMode.equals("pickup", ignoreCase = true)) {
                            store
                        } else {
                            store.copy(
                                deliveryFee = if (officialFee <= 0.0) "Grátis" else formatBrl(officialFee),
                                officialCustomerDeliveryFee = officialFee,
                                isFreeDelivery = officialFee <= 0.0
                            )
                        }
                    }
                }.awaitAll()
            }

            StoreCatalogResult(isSuccess = true, stores = withOfficialFees)
        } catch (_: Exception) {
            StoreCatalogResult(isSuccess = false, stores = emptyList())
        }
    }

    // ------------------------------------------------------------------
    // (b) PRODUTOS DA LOJA — porte de SupabaseClient.fetchProductsForStore
    // ------------------------------------------------------------------

    suspend fun fetchProductsForStore(storeId: String): List<Product> {
        val (ok, text) = restGetRaw(
            "$SUPABASE_URL/rest/v1/products?select=*&store_id=eq.$storeId&order=name.asc"
        )
        if (!ok || text.isBlank()) return emptyList()
        return try {
            val productsList = mutableListOf<Product>()
            for (element in parseArray(text)) {
                val item = element.jsonObject
                val id = item.optString("id")
                val name = item.optString("name")
                if (name.isBlank()) continue
                if (item.optBoolean("sold_by_weight", false)) continue

                val metadataObj = item.optJsonObject("metadata")
                val isPdvOnly = metadataObj?.optBoolean("pdv_only", false) ?: false
                val isHidden = metadataObj?.optBoolean("hidden", false) ?: false
                if (isPdvOnly || isHidden) continue

                var hasStuffedCrust = item.optBoolean("has_stuffed_crust", false)
                var isCombo = item.optBoolean("is_combo", false)
                var isPastelFlavor = item.optBoolean("is_pastel_flavor", false)
                var isBeverage = item.optBoolean("is_beverage", false)
                var requiresPrescription = false
                var isControlled = false
                var pharmacySaleMode = "platform_checkout"
                var pharmacyType = ""
                var activeIngredient = ""
                var dosage = ""
                var pharmaForm = ""
                var manufacturer = ""
                var packQuantity = ""
                var isGeneric = false
                val pizzaSizeOverrides = mutableMapOf<String, Double>()
                val pizzaUnavailableSizeIds = mutableSetOf<String>()
                val legacyPizzaSizes = mutableListOf<PizzaLegacySize>()
                var pizzaCategoryId = ""
                if (metadataObj != null) {
                    if (metadataObj["has_stuffed_crust"] != null) {
                        hasStuffedCrust = metadataObj.optBoolean("has_stuffed_crust", false)
                    }
                    if (metadataObj["is_combo"] != null) {
                        isCombo = metadataObj.optBoolean("is_combo", false)
                    }
                    if (metadataObj["is_pastel_flavor"] != null) {
                        isPastelFlavor = metadataObj.optBoolean("is_pastel_flavor", false)
                    }
                    if (metadataObj["is_beverage"] != null) {
                        isBeverage = metadataObj.optBoolean("is_beverage", false)
                    }
                    requiresPrescription = metadataObj.optBoolean("requires_prescription", false)
                    isControlled = metadataObj.optBoolean("controlled", false)
                    pharmacySaleMode = metadataObj.optString("sale_mode", "platform_checkout")
                        .ifBlank { "platform_checkout" }
                    pharmacyType = metadataObj.optString("pharma_type", "")
                    activeIngredient = metadataObj.optString("active_ingredient", "")
                    dosage = metadataObj.optString("dosage", "")
                    pharmaForm = metadataObj.optString("pharma_form", "")
                    manufacturer = metadataObj.optString("manufacturer", "")
                    packQuantity = metadataObj.optString("pack_quantity", "")
                    isGeneric = metadataObj.optBoolean("is_generic", false)
                    metadataObj.optJsonObject("pizza_size_overrides")?.keys?.forEach { sizeId ->
                        val priceForSize = metadataObj.optJsonObject("pizza_size_overrides")
                            ?.optDouble(sizeId, 0.0) ?: 0.0
                        if (priceForSize > 0.0) pizzaSizeOverrides[sizeId] = priceForSize
                    }
                    metadataObj.optJsonArray("pizza_unavailable_sizes")?.forEach { sizeEl ->
                        (sizeEl as? JsonPrimitive)?.contentOrNull?.trim()
                            ?.takeIf { it.isNotBlank() }?.let(pizzaUnavailableSizeIds::add)
                    }
                    metadataObj.optJsonArray("sizes")?.forEach { sizeEl ->
                        val legacySize = sizeEl.jsonObject
                        val legacyName = legacySize.optString("name").trim()
                        val legacyPrice = legacySize.optDouble("price", 0.0)
                        if (legacyName.isNotBlank() && legacyPrice > 0.0) {
                            legacyPizzaSizes.add(PizzaLegacySize(legacyName, legacyPrice))
                        }
                    }
                    pizzaCategoryId = metadataObj.optString("pizza_category_id", "").trim()
                }

                val rawDesc = item.optString("description", "")
                val description = if (rawDesc.trim() == "null") "" else rawDesc

                productsList.add(
                    Product(
                        id = id,
                        storeId = storeId,
                        name = name,
                        description = description,
                        price = item.optDouble("price", 0.0),
                        category = item.optString("category", "Geral"),
                        sectionId = item.optNullableString("section_id"),
                        imageUrl = item.optString("image_url", ""),
                        isAvailable = item.optBoolean("is_available", true),
                        hasStuffedCrust = hasStuffedCrust,
                        isCombo = isCombo,
                        isPastelFlavor = isPastelFlavor,
                        isBeverage = isBeverage,
                        requiresPrescription = requiresPrescription,
                        isControlled = isControlled,
                        pharmacySaleMode = pharmacySaleMode,
                        pharmacyType = pharmacyType,
                        activeIngredient = activeIngredient,
                        dosage = dosage,
                        pharmaForm = pharmaForm,
                        manufacturer = manufacturer,
                        packQuantity = packQuantity,
                        isGeneric = isGeneric,
                        pizzaCategoryId = pizzaCategoryId,
                        pizzaSizeOverrides = pizzaSizeOverrides,
                        pizzaUnavailableSizeIds = pizzaUnavailableSizeIds,
                        legacyPizzaSizes = legacyPizzaSizes
                    )
                )
            }
            productsList
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Valida um cupom no banco (tabela coupons).
     * Retorna o cupom se válido, null se inválido/expirado/esgotado.
     */
    suspend fun fetchCoupon(code: String, storeId: String? = null): Coupon? {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) return null
        return try {
            var url = "$SUPABASE_URL/rest/v1/coupons?select=*&code=eq.$cleanCode&is_active=eq.true"
            if (storeId != null) {
                url += "&or=(store_id.eq.$storeId,store_id.is.null)"
            }
            val (ok, json) = restGetRaw(url)
            if (!ok || json.isBlank()) return null
            val array = Json.parseToJsonElement(json).jsonArray
            val obj = array.firstOrNull()?.jsonObject ?: return null
            
            // Verifica expiração
            val expiresAt = obj["expires_at"]?.jsonPrimitive?.contentOrNull
            if (expiresAt != null) {
                // Formato ISO - comparação simples
                // TODO: parse adequado da data
            }
            
            // Verifica limite de usos
            val maxUses = obj["max_uses"]?.jsonPrimitive?.intOrNull
            val usedCount = obj["used_count"]?.jsonPrimitive?.intOrNull ?: 0
            if (maxUses != null && usedCount >= maxUses) return null
            
            Coupon(
                id = obj["id"]?.jsonPrimitive?.content ?: return null,
                code = obj["code"]?.jsonPrimitive?.content ?: cleanCode,
                discountType = obj["discount_type"]?.jsonPrimitive?.content ?: "percentage",
                discountValue = obj["discount_value"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                minOrderValue = obj["min_order_value"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                maxUses = maxUses,
                usedCount = usedCount,
                isActive = true,
                firstOrderOnly = obj["first_order_only"]?.jsonPrimitive?.booleanOrNull ?: false,
                storeId = obj["store_id"]?.jsonPrimitive?.contentOrNull
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Busca os pedidos do cliente (tabela orders).
     * Retorna lista vazia em caso de erro (sem mocks).
     */
    suspend fun fetchOrdersForClient(userId: String): List<Order> {
        if (userId.isBlank()) return emptyList()
        return try {
            val (ok, json) = restGetRaw(
                "$SUPABASE_URL/rest/v1/orders?select=id,store_id,total,status,created_at,payment_method&client_id=eq.$userId&order=created_at.desc&limit=50"
            )
            if (!ok || json.isBlank()) return emptyList()
            val array = Json.parseToJsonElement(json).jsonArray
            array.mapNotNull { el ->
                try {
                    val o = el.jsonObject
                    Order(
                        id = o["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                        storeId = o["store_id"]?.jsonPrimitive?.content ?: "",
                        storeName = "", // Será preenchido via join se necessário
                        items = emptyList(), // Itens carregados separadamente se necessário
                        subtotal = 0.0,
                        deliveryFee = 0.0,
                        total = o["total"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        paymentMethod = o["payment_method"]?.jsonPrimitive?.content ?: "",
                        deliveryAddress = "",
                        status = o["status"]?.jsonPrimitive?.content ?: "",
                        createdAt = o["created_at"]?.jsonPrimitive?.content ?: ""
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ------------------------------------------------------------------
    // (c) AUTH — porte de SupabaseClient.signIn / signUp
    // ------------------------------------------------------------------

    private fun parseAuthResponse(
        ok: Boolean,
        text: String,
        email: String,
        defaultError: String
    ): SupabaseAuthResponse {
        if (ok && text.isNotBlank()) {
            return try {
                val payload = json.parseToJsonElement(text).jsonObject
                val userObj = payload.optJsonObject("user")
                SupabaseAuthResponse(
                    isSuccess = true,
                    userId = userObj?.optString("id") ?: payload.optString("id"),
                    accessToken = payload.optNullableString("access_token"),
                    refreshToken = payload.optNullableString("refresh_token"),
                    expiresAt = (payload["expires_at"] as? JsonPrimitive)?.longOrNull
                        ?.takeIf { it > 0L },
                    email = userObj?.optString("email") ?: email
                )
            } catch (_: Exception) {
                SupabaseAuthResponse(isSuccess = false, errorMessage = defaultError)
            }
        }
        return SupabaseAuthResponse(
            isSuccess = false,
            errorMessage = parseErrorMessage(text, defaultError)
        )
    }

    suspend fun signIn(email: String, password: String): SupabaseAuthResponse {
        val body = buildJsonObject {
            putStr("email", email.trim())
            putStr("password", password)
        }.toString()
        val (ok, text) = authPostRaw("token?grant_type=password", body)
        return parseAuthResponse(ok, text, email, "E-mail ou senha incorretos")
    }

    suspend fun signUp(email: String, password: String): SupabaseAuthResponse {
        val body = buildJsonObject {
            putStr("email", email.trim())
            putStr("password", password)
        }.toString()
        val (ok, text) = authPostRaw("signup", body)
        return parseAuthResponse(ok, text, email, "Erro ao criar conta")
    }

    // ------------------------------------------------------------------
    // (d) CRIAÇÃO DE PEDIDO — porte de SupabaseClient.submitOrder
    // ------------------------------------------------------------------

    suspend fun submitOrder(
        order: Order,
        clientId: String,
        accessToken: String,
        needsChange: Boolean,
        changeFor: Double?
    ): OrderSubmissionResponse {
        return try {
            val bearer = if (accessToken.isNotBlank()) accessToken else SUPABASE_ANON_KEY
            val orderJson = buildJsonObject {
                putStr("client_id", clientId)
                putStr("store_id", order.storeId)
                putNum("subtotal", order.subtotal)
                putNum("delivery_fee", order.deliveryFee)
                putNum("wallet_discount", order.walletDiscount)
                putNum("loyalty_points_used", order.loyaltyPointsUsed)
                putNum("loyalty_discount", order.loyaltyDiscount)
                putNum("total_price", order.total)
                putStr("payment_method", order.paymentMethod)
                putStr("neighborhood", order.neighborhood)
                putStr("address_details", order.deliveryAddress)
                putBlankAsNull("delivery_cep", order.deliveryCep)
                putBlankAsNull("delivery_city", order.deliveryCity)
                putBlankAsNull("delivery_state", order.deliveryState)
                putNum("client_lat", order.clientLatitude)
                putNum("client_lng", order.clientLongitude)
                putNum("delivery_fee_absorbed_by_store", order.deliveryFeeAbsorbedByStore)
                order.deliveryQuoteSnapshot?.let { snapshot ->
                    put("metadata", buildJsonObject {
                        put("delivery_quote", buildJsonObject {
                            putNum("policy_version", snapshot.policyVersion)
                            putNum("distance_km", snapshot.distanceKm)
                            putStr("distance_source", snapshot.distanceSource)
                            putNum("max_delivery_km", snapshot.maxDeliveryKm)
                            putStr("destination_precision", snapshot.destinationPrecision)
                            putNum("store_delivery_base", snapshot.storeDeliveryBase)
                            putNum("platform_fee_customer", snapshot.platformFeeCustomer)
                            putNum("platform_fee_store_absorbed", snapshot.platformFeeStoreAbsorbed)
                            putNum("delivery_fee", snapshot.deliveryFee)
                            putNum("vip_override_applied", snapshot.vipOverrideApplied)
                            putStr("split_mode", snapshot.splitMode)
                            putStr("plan_type", snapshot.planType)
                            putBool("free_delivery_applied", snapshot.freeDeliveryApplied)
                        })
                    })
                }
                putBool("needs_change", needsChange)
                putNum("change_for", changeFor)
                putBlankAsNull("scheduled_for", order.scheduledFor)
                putBlankAsNull("release_at", order.releaseAt)
                putStr("status", order.status)
            }.toString()

            val (orderOk, orderText) = restPostRaw(
                "$SUPABASE_URL/rest/v1/orders", orderJson, bearer, prefer = "return=representation"
            )
            if (!orderOk || orderText.isBlank()) {
                return OrderSubmissionResponse(
                    isSuccess = false,
                    errorMessage = parseErrorMessage(orderText, "Não foi possível criar o pedido.")
                )
            }
            val createdOrder = runCatching {
                parseArray(orderText).firstOrNull()?.jsonObject
            }.getOrNull()
                ?: return OrderSubmissionResponse(
                    false,
                    errorMessage = "O servidor não devolveu o identificador do pedido."
                )
            val orderId = createdOrder.optString("id")
            if (orderId.isBlank()) {
                return OrderSubmissionResponse(
                    false,
                    errorMessage = "O servidor não devolveu o identificador do pedido."
                )
            }

            val itemsJson = buildJsonArray {
                order.items.forEach { cartItem ->
                    val addonsJson = buildJsonArray {
                        cartItem.selectedAddons.forEach { addon ->
                            add(
                                buildJsonObject {
                                    putStr("id", addon.itemId)
                                    putStr("name", addon.itemName)
                                    putNum("price", addon.itemPrice)
                                    putStr("group_id", addon.groupId)
                                    putStr("groupName", addon.groupName)
                                    putBool("priceReplacesBase", addon.priceReplacesBase)
                                }
                            )
                        }
                    }
                    add(
                        buildJsonObject {
                            putStr("order_id", orderId)
                            putStr("product_id", cartItem.product.id)
                            putNum("quantity", cartItem.quantity)
                            putNum("unit_price", cartItem.unitPrice)
                            // O app Android grava a representação JSON dos adicionais
                            // como string neste campo JSONB.
                            if (addonsJson.isNotEmpty()) putStr("addons", addonsJson.toString())
                            else put("addons", JsonNull)
                            putStr("observations", cartItem.notes.ifBlank { null })
                        }
                    )
                }
            }.toString()

            val (itemsOk, itemsText) = restPostRaw(
                "$SUPABASE_URL/rest/v1/order_items", itemsJson, bearer, prefer = "return=minimal"
            )
            if (!itemsOk) {
                return OrderSubmissionResponse(
                    isSuccess = false,
                    errorMessage = parseErrorMessage(
                        itemsText,
                        "O pedido foi criado, mas não foi possível registrar os itens."
                    )
                )
            }

            OrderSubmissionResponse(
                isSuccess = true,
                orderId = orderId,
                createdAt = createdOrder.optString("created_at").ifBlank { null }
            )
        } catch (_: Exception) {
            OrderSubmissionResponse(
                isSuccess = false,
                errorMessage = "Falha de conexão ao enviar o pedido."
            )
        }
    }

    // ------------------------------------------------------------------
    // (d2) LOJA POR ID — porte de SupabaseClient.fetchStoreById do app
    // Android: reutiliza o mapper central do fetchActiveStores.
    // ------------------------------------------------------------------

    suspend fun fetchStoreById(storeId: String): Store? {
        if (storeId.isBlank()) return null
        return try {
            fetchActiveStores().stores.firstOrNull { it.id == storeId }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Busca os endereços salvos do usuário (tabela saved_addresses), com o JWT
     * do usuário — porte de SupabaseClient.fetchSavedAddresses do app Android.
     * Retorna lista vazia em caso de erro (sem mocks).
     */
    suspend fun fetchSavedAddresses(userId: String, accessToken: String): List<SavedAddress> {
        if (userId.isBlank() || accessToken.isBlank()) return emptyList()
        return try {
            val (ok, json) = restGetRaw(
                "$SUPABASE_URL/rest/v1/saved_addresses?select=id,label,street,number,complement,neighborhood,city,state,reference_point,is_default,cep,latitude,longitude,pin_confirmed&user_id=eq.$userId&order=is_default.desc,created_at.desc",
                bearer = accessToken
            )
            if (!ok || json.isBlank()) return emptyList()
            parseSavedAddresses(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseSavedAddresses(json: String): List<SavedAddress> {
        return try {
            val array = Json.parseToJsonElement(json).jsonArray
            array.mapNotNull { el ->
                try {
                    val o = el.jsonObject
                    SavedAddress(
                        id = o["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                        label = (o["label"]?.jsonPrimitive?.content ?: "").ifBlank { "Casa" },
                        street = o["street"]?.jsonPrimitive?.content ?: "",
                        number = o["number"]?.jsonPrimitive?.content ?: "",
                        complement = o["complement"]?.jsonPrimitive?.content ?: "",
                        neighborhood = o["neighborhood"]?.jsonPrimitive?.content ?: "",
                        city = o["city"]?.jsonPrimitive?.content ?: "",
                        state = com.example.shared.model.normalizeBrazilianUf(o["state"]?.jsonPrimitive?.content ?: ""),
                        referencePoint = o["reference_point"]?.jsonPrimitive?.content ?: "",
                        cep = o["cep"]?.jsonPrimitive?.content ?: "",
                        latitude = o["latitude"]?.jsonPrimitive?.doubleOrNull,
                        longitude = o["longitude"]?.jsonPrimitive?.doubleOrNull,
                        pinConfirmed = o["pin_confirmed"]?.jsonPrimitive?.booleanOrNull ?: false,
                        isDefault = o["is_default"]?.jsonPrimitive?.booleanOrNull ?: false
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ------------------------------------------------------------------
    // (d3) PIX — porte de SupabaseClient.generatePixForOrder +
    // uploadPixDirectProof do app Android.
    // ------------------------------------------------------------------

    /**
     * Gera o PIX online do pedido pela Edge Function `payment-router`
     * (action "order_pix"), igual ao app Android.
     */
    suspend fun generatePixForOrder(
        order: Order,
        payerFullName: String,
        payerDocument: String,
        accessToken: String
    ): PixPaymentResponse {
        return try {
            val document = payerDocument.filter { it.isDigit() }
            if (document.length != 11) {
                return PixPaymentResponse(
                    false,
                    errorMessage = "Cadastre um CPF válido no perfil antes de pagar com PIX."
                )
            }
            val nameParts = payerFullName.trim().ifBlank { "Cliente ItaSuper" }.split(Regex("\\s+"))
            val firstName = nameParts.firstOrNull().orEmpty().ifBlank { "Cliente" }
            val lastName = nameParts.drop(1).joinToString(" ").ifBlank { "ItaSuper" }
            val requestJson = buildJsonObject {
                putStr("action", "order_pix")
                putStr("order_id", order.id)
                putNum("amount", order.total)
                putStr(
                    "description",
                    "Pedido #${order.id.take(6).uppercase()} - ${order.storeName.ifBlank { "ItaSuper" }}"
                )
                putStr("payer_first_name", firstName)
                putStr("payer_last_name", lastName)
                putStr("payer_cpf", document)
            }.toString()
            val (ok, text) = restPostRaw(
                "$SUPABASE_URL/functions/v1/payment-router",
                requestJson,
                bearer = accessToken
            )
            if (!ok || text.isBlank()) {
                return PixPaymentResponse(
                    false,
                    errorMessage = parseErrorMessage(text, "Não foi possível gerar o PIX.")
                )
            }
            val payload = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
                ?: return PixPaymentResponse(false, errorMessage = "Não foi possível gerar o PIX.")
            if (payload.optBoolean("rate_limited", false)) {
                return PixPaymentResponse(
                    false,
                    errorMessage = "Muitas tentativas de PIX. Aguarde alguns minutos e tente novamente."
                )
            }
            val gatewayError = payload.optString("error").trim()
            if (gatewayError.isNotBlank()) {
                return PixPaymentResponse(false, errorMessage = gatewayError)
            }
            val pixCode = payload.optString("pix_code", payload.optString("qr_code")).trim().ifBlank { null }
            val qrCodeBase64 =
                payload.optString("qr_code_url", payload.optString("qr_code_base64")).trim().ifBlank { null }
            if (pixCode == null && qrCodeBase64 == null) {
                return PixPaymentResponse(
                    false,
                    errorMessage = "O servidor não devolveu o QR Code do PIX."
                )
            }
            PixPaymentResponse(true, pixCode = pixCode, qrCodeBase64 = qrCodeBase64)
        } catch (_: Exception) {
            PixPaymentResponse(false, errorMessage = "Falha de conexão ao gerar o PIX.")
        }
    }

    /**
     * Envia o comprovante do PIX Direto ao storage `pix-proofs` e vincula o
     * arquivo ao pedido pela RPC oficial `attach_pix_proof`.
     * (Porte de SupabaseClient.uploadPixDirectProof do app Android.)
     */
    suspend fun uploadPixDirectProof(
        orderId: String,
        storeId: String,
        bytes: ByteArray,
        mimeType: String,
        extension: String,
        accessToken: String
    ): Result<Unit> {
        return try {
            if (bytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("O comprovante está vazio."))
            }
            if (mimeType !in setOf("image/jpeg", "image/png", "application/pdf")) {
                return Result.failure(IllegalArgumentException("Envie um comprovante JPG, PNG ou PDF."))
            }
            if (bytes.size > 5 * 1024 * 1024) {
                return Result.failure(IllegalArgumentException("O comprovante deve ter no máximo 5 MB."))
            }
            val normalizedExtension = extension.lowercase().ifBlank { "jpg" }
            val path = "$storeId/$orderId.$normalizedExtension"
            val uploadResponse: HttpResponse = client.post("$SUPABASE_URL/storage/v1/object/pix-proofs/$path") {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $accessToken")
                header("x-upsert", "true")
                contentType(ContentType.parse(mimeType))
                setBody(bytes)
            }
            val uploadBody = uploadResponse.bodyAsText()
            if (!uploadResponse.status.isSuccess()) {
                return Result.failure(
                    IllegalStateException(parseErrorMessage(uploadBody, "Não foi possível enviar o comprovante."))
                )
            }
            val attachJson = buildJsonObject {
                putStr("p_order_id", orderId)
                putStr("p_proof_path", path)
            }.toString()
            val (attachOk, attachBody) = restPostRaw(
                "$SUPABASE_URL/rest/v1/rpc/attach_pix_proof",
                attachJson,
                accessToken
            )
            if (!attachOk) {
                runCatching {
                    client.delete("$SUPABASE_URL/storage/v1/object/pix-proofs/$path") {
                        header("apikey", SUPABASE_ANON_KEY)
                        header("Authorization", "Bearer $accessToken")
                    }
                }
                return Result.failure(
                    IllegalStateException(parseErrorMessage(attachBody, "O comprovante não pôde ser associado ao pedido."))
                )
            }
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Falha de conexão ao enviar o comprovante."))
        }
    }

    // ------------------------------------------------------------------
    // (i) DOCUMENTOS LEGAIS — porte de SupabaseClient.fetchPendingLegalChanges
    //     + recordLegalAcceptance do app Android. Usados pelo gate de aceite
    //     (LegalConsentState, ITEM 5/6).
    // ------------------------------------------------------------------

    private const val LEGAL_AUDIENCE_CLIENT = "cliente"

    /** Versões de Termos/Privacidade já aceitas no perfil (`profiles`). */
    suspend fun fetchLegalAcceptedVersions(userId: String, accessToken: String): Pair<String?, String?> {
        if (userId.isBlank() || accessToken.isBlank()) return null to null
        val (ok, body) = restGetRaw(
            "$SUPABASE_URL/rest/v1/profiles?select=terms_version_accepted,privacy_version_accepted&user_id=eq.$userId&limit=1",
            bearer = accessToken
        )
        if (!ok || body.isBlank()) return null to null
        val obj = parseArray(body).firstOrNull() as? JsonObject ?: return null to null
        return obj.optNullableString("terms_version_accepted") to
            obj.optNullableString("privacy_version_accepted")
    }

    /**
     * Consulta o mesmo RPC utilizado pelo app Android. As versões não são fixadas no
     * app: o backend informa qual documento mudou e quais versões exigem novo aceite.
     */
    suspend fun fetchPendingLegalChanges(
        termsAccepted: String?,
        privacyAccepted: String?,
        accessToken: String? = null
    ): PendingLegalChanges? {
        // O público-alvo entrou na sobrecarga de três argumentos da v6.6. Sem ele o cliente
        // recebe também mudanças de lojista e motoboy, e a que o afeta fica soterrada.
        // Só um banco sem a sobrecarga (42883) justifica repetir na assinatura antiga:
        // falha de rede ou RLS não deve virar uma segunda requisição.
        return when (val result = requestPendingLegalChanges(termsAccepted, privacyAccepted, accessToken, LEGAL_AUDIENCE_CLIENT)) {
            is LegalChangesResponse.Unsupported ->
                (requestPendingLegalChanges(termsAccepted, privacyAccepted, accessToken, audience = null)
                    as? LegalChangesResponse.Success)?.value
            is LegalChangesResponse.Success -> result.value
            is LegalChangesResponse.Failed -> null
        }
    }

    /** Distingue "o banco não tem a sobrecarga" de "a consulta falhou". */
    private sealed interface LegalChangesResponse {
        data class Success(val value: PendingLegalChanges) : LegalChangesResponse
        data object Unsupported : LegalChangesResponse
        data object Failed : LegalChangesResponse
    }

    private suspend fun requestPendingLegalChanges(
        termsAccepted: String?,
        privacyAccepted: String?,
        accessToken: String?,
        audience: String?
    ): LegalChangesResponse {
        val requestBody = buildJsonObject {
            putStr("_terms_accepted", termsAccepted?.ifBlank { "0" } ?: "0")
            putStr("_privacy_accepted", privacyAccepted?.ifBlank { "0" } ?: "0")
            if (audience != null) putStr("_audience", audience)
        }.toString()
        val bearer = accessToken?.takeIf { it.isNotBlank() } ?: SUPABASE_ANON_KEY
        val (ok, body) = restPostRaw(
            "$SUPABASE_URL/rest/v1/rpc/get_pending_legal_changes",
            requestBody,
            bearer = bearer
        )
        if (!ok || body.isBlank()) {
            // restPostRaw não expõe o código HTTP; a distinção é só pelos marcadores
            // no corpo (o PostgREST devolve 404 + PGRST202 quando falta a sobrecarga).
            if (audience != null && isMissingFunctionError(body)) return LegalChangesResponse.Unsupported
            return LegalChangesResponse.Failed
        }
        val result = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return LegalChangesResponse.Failed
        val pending = PendingLegalChanges(
            needsTerms = result.optBoolean("needs_terms", false),
            needsPrivacy = result.optBoolean("needs_privacy", false),
            currentTermsVersion = result.optString("current_terms_version"),
            currentPrivacyVersion = result.optString("current_privacy_version"),
            termsChanges = parseLegalChanges(result.optJsonArray("terms_changes")),
            privacyChanges = parseLegalChanges(result.optJsonArray("privacy_changes")),
            // Campo ausente (banco anterior à v6.6) significa binding, nunca notice.
            mode = LegalAcceptanceMode.fromRemote(result.optNullableString("mode")),
            termsEffectiveDate = result.optNullableString("terms_effective_date"),
            privacyEffectiveDate = result.optNullableString("privacy_effective_date"),
            daysUntilEffective = (result["days_until_effective"] as? JsonPrimitive)?.intOrNull
        )
        return LegalChangesResponse.Success(pending)
    }

    private fun parseLegalChanges(array: JsonArray?): List<LegalChange> {
        if (array == null) return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            LegalChange(
                version = obj.optString("version"),
                effectiveDate = obj.optString("effective_date"),
                section = obj.optString("section"),
                changeType = obj.optString("change_type"),
                summary = obj.optString("summary"),
                legalBasis = obj.optNullableString("legal_basis")
            )
        }
    }

    /** Registra o aceite auditável e atualiza as versões no perfil, como no Android. */
    suspend fun recordLegalAcceptance(
        userId: String,
        accessToken: String,
        termsVersion: String,
        privacyVersion: String,
        acceptanceMode: LegalAcceptanceMode = LegalAcceptanceMode.BINDING
    ): LegalAcceptanceResult {
        if (userId.isBlank() || accessToken.isBlank() || termsVersion.isBlank() || privacyVersion.isBlank()) {
            return LegalAcceptanceResult(false, "Não foi possível identificar os documentos para aceite.")
        }
        // Instant ISO-8601 UTC ("2026-10-18T06:05:12.345Z"), como o SimpleDateFormat do Android.
        val acceptedAt = Clock.System.now().toString()
        fun acceptanceBody(includeMode: Boolean) = buildJsonObject {
            putStr("user_id", userId)
            putStr("terms_version", termsVersion)
            putStr("privacy_version", privacyVersion)
            putStr("user_agent", "ItaSuper KMP")
            putStr("accepted_at", acceptedAt)
            if (includeMode) putStr("acceptance_mode", acceptanceMode.remoteValue)
        }.toString()

        // A prova do consentimento não pode falhar por causa de uma coluna acessória:
        // se acceptance_mode ainda não existe (42703 / PGRST204), grava sem ela.
        val (firstOk, firstBody) = restPostRaw(
            "$SUPABASE_URL/rest/v1/terms_acceptance",
            acceptanceBody(includeMode = true),
            bearer = accessToken,
            prefer = "return=minimal"
        )
        if (!firstOk) {
            if (!isMissingColumnError(firstBody)) {
                return LegalAcceptanceResult(
                    false,
                    parseErrorMessage(firstBody, "Não foi possível registrar seu aceite.")
                )
            }
            val (retryOk, retryBody) = restPostRaw(
                "$SUPABASE_URL/rest/v1/terms_acceptance",
                acceptanceBody(includeMode = false),
                bearer = accessToken,
                prefer = "return=minimal"
            )
            if (!retryOk) {
                return LegalAcceptanceResult(
                    false,
                    parseErrorMessage(retryBody, "Não foi possível registrar seu aceite.")
                )
            }
        }

        val profileBody = buildJsonObject {
            putStr("terms_version_accepted", termsVersion)
            putStr("privacy_version_accepted", privacyVersion)
            putStr("terms_accepted_at", acceptedAt)
        }.toString()
        val (profileOk, profileText) = restPatchRaw(
            "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId",
            profileBody,
            bearer = accessToken,
            prefer = "return=minimal"
        )
        if (!profileOk) {
            return LegalAcceptanceResult(
                false,
                parseErrorMessage(profileText, "O aceite foi registrado, mas não foi possível atualizar seu perfil.")
            )
        }
        return LegalAcceptanceResult(true)
    }

    private fun isMissingFunctionError(body: String): Boolean =
        body.contains("42883") ||
            body.contains("PGRST202") ||
            body.contains("Could not find the function", ignoreCase = true)

    /** Coluna inexistente: 42703 no Postgres, PGRST204 no cache de schema do PostgREST. */
    private fun isMissingColumnError(body: String): Boolean =
        body.contains("42703") ||
            body.contains("PGRST204") ||
            (body.contains("Could not find the", ignoreCase = true) && body.contains("column", ignoreCase = true))

    // ------------------------------------------------------------------
    // Montador de pizza/pastel — bordas e adicionais (espelha a web)
    // ------------------------------------------------------------------

    /** Bordas da pizza (tabela pizza_borders). */
    suspend fun fetchPizzaBorders(storeId: String): List<PizzaBorder> {
        val (ok, text) = restGetRaw(
            "$SUPABASE_URL/rest/v1/pizza_borders?store_id=eq.$storeId&is_available=eq.true&order=sort_order.asc"
        )
        if (!ok || text.isBlank()) return emptyList()
        return try {
            parseArray(text).mapNotNull { element ->
                val item = element.jsonObject
                val id = item.optString("id")
                if (id.isBlank()) return@mapNotNull null
                PizzaBorder(
                    id = id,
                    storeId = storeId,
                    name = item.optString("name"),
                    price = item.optDouble("price", 0.0),
                    isAvailable = item.optBoolean("is_available", true),
                    sortOrder = item.optInt("sort_order", 0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Complementos do pastel (tabela pastel_borders). */
    suspend fun fetchPastelBorders(storeId: String): List<PizzaBorder> {
        val (ok, text) = restGetRaw(
            "$SUPABASE_URL/rest/v1/pastel_borders?store_id=eq.$storeId&is_available=eq.true&order=sort_order.asc"
        )
        if (!ok || text.isBlank()) return emptyList()
        return try {
            parseArray(text).mapNotNull { element ->
                val item = element.jsonObject
                val id = item.optString("id")
                if (id.isBlank()) return@mapNotNull null
                PizzaBorder(
                    id = id,
                    storeId = storeId,
                    name = item.optString("name"),
                    price = item.optDouble("price", 0.0),
                    isAvailable = item.optBoolean("is_available", true),
                    sortOrder = item.optInt("sort_order", 0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Grupos de adicionais ligados aos sabores escolhidos.
     * Deduplica por grupo (um sabor pode repetir o mesmo grupo).
     */
    suspend fun fetchFlavorAddonGroups(flavorIds: List<String>): List<PizzaAddonGroup> {
        if (flavorIds.isEmpty()) return emptyList()
        val idsParam = flavorIds.joinToString(",")
        val (ok, text) = restGetRaw(
            "$SUPABASE_URL/rest/v1/product_addon_groups" +
                "?product_id=in.($idsParam)" +
                "&select=addon_group:addon_groups(id,name,min_select,max_select,price_replaces_base,addon_items(id,name,price))"
        )
        if (!ok || text.isBlank()) return emptyList()
        return try {
            val seen = mutableSetOf<String>()
            val result = mutableListOf<PizzaAddonGroup>()
            for (element in parseArray(text)) {
                val row = element.jsonObject
                val groupObj = row["addon_group"]?.jsonObject ?: continue
                val groupId = groupObj.optString("id")
                if (groupId.isBlank() || !seen.add(groupId)) continue
                val items = mutableListOf<AddonItem>()
                try {
                    val itemsArray = groupObj["addon_items"]?.jsonArray
                    itemsArray?.forEach { itemEl ->
                        val itemObj = itemEl.jsonObject
                        val itemId = itemObj.optString("id")
                        if (itemId.isNotBlank()) {
                            items.add(
                                AddonItem(
                                    id = itemId,
                                    groupId = groupId,
                                    name = itemObj.optString("name"),
                                    price = itemObj.optDouble("price", 0.0)
                                )
                            )
                        }
                    }
                } catch (_: Exception) {
                }
                items.sortBy { it.name }
                result.add(
                    PizzaAddonGroup(
                        id = groupId,
                        name = groupObj.optString("name"),
                        minSelect = groupObj.optInt("min_select", 0),
                        maxSelect = groupObj.optInt("max_select", 99).takeIf { it > 0 } ?: 99,
                        priceReplacesBase = groupObj.optBoolean("price_replaces_base", false),
                        items = items
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ------------------------------------------------------------------
    // (e) SESSÃO E PERFIL — porte de SupabaseClient.refreshSession,
    // requestPasswordRecovery, fetchCustomerProfile, insertProfile,
    // updateCustomerPersonalProfile, updateUserProfileAddress,
    // updateCustomerDeliveryPin, updateUserProfileNumber,
    // deleteCustomerAccount e fetchAddressByCep do app Android.
    // ------------------------------------------------------------------

    /** Renova a sessão a partir do refresh token (grant refresh_token). */
    suspend fun refreshSession(refreshToken: String): SupabaseAuthResponse {
        if (refreshToken.isBlank()) {
            return SupabaseAuthResponse(false, errorMessage = "Sua sessão expirou. Entre novamente.")
        }
        val body = buildJsonObject {
            putStr("refresh_token", refreshToken)
        }.toString()
        val (ok, text) = authPostRaw("token?grant_type=refresh_token", body)
        val parsed = parseAuthResponse(ok, text, "", "Sua sessão expirou. Entre novamente.")
        // Igual ao original: sucesso só se o novo access_token não for vazio.
        return if (parsed.isSuccess && !parsed.accessToken.isNullOrBlank()) parsed
        else SupabaseAuthResponse(false, errorMessage = parsed.errorMessage ?: "Sua sessão expirou. Entre novamente.")
    }

    /** Envia o e-mail de redefinição de senha (mesmo link do web). */
    suspend fun requestPasswordRecovery(email: String): SupabaseOperationResult {
        val body = buildJsonObject {
            putStr("email", email.trim())
            putStr("redirect_to", "https://itasuper.com.br/cliente?mode=reset")
        }.toString()
        val (ok, text) = authPostRaw("recover", body)
        return if (ok) SupabaseOperationResult(true)
        else SupabaseOperationResult(false, parseErrorMessage(text, "Não foi possível enviar o e-mail de recuperação."))
    }

    /** Lê o perfil do cliente em `profiles` com o JWT do usuário. */
    suspend fun fetchCustomerProfile(userId: String, accessToken: String): RemoteCustomerProfile? {
        if (userId.isBlank() || accessToken.isBlank()) return null
        val select = "full_name,email,document,whatsapp_number,phone,delivery_pin,cep,street,number," +
            "complement,neighborhood,city,state,reference_point,pix_type,pix_key," +
            "terms_version_accepted,privacy_version_accepted"
        val (ok, text) = restGetRaw(
            "$SUPABASE_URL/rest/v1/profiles?select=$select&user_id=eq.$userId&limit=1",
            bearer = accessToken
        )
        if (!ok || text.isBlank()) return null
        return try {
            val row = parseArray(text).firstOrNull()?.jsonObject ?: return null
            val whatsappNumber = row.optString("whatsapp_number").trim()
            RemoteCustomerProfile(
                fullName = row.optString("full_name").trim(),
                email = row.optString("email").trim(),
                document = row.optString("document").trim(),
                whatsapp = whatsappNumber.ifBlank { row.optString("phone").trim() },
                deliveryPin = row.optString("delivery_pin").trim(),
                cep = row.optString("cep").trim(),
                street = row.optString("street").trim(),
                number = row.optString("number"),
                complement = row.optString("complement").trim(),
                neighborhood = row.optString("neighborhood").trim(),
                city = row.optString("city").trim(),
                state = com.example.shared.model.normalizeBrazilianUf(row.optString("state")),
                referencePoint = row.optString("reference_point").trim(),
                pixType = row.optString("pix_type").trim(),
                pixKey = row.optString("pix_key").trim(),
                termsVersionAccepted = row.optString("terms_version_accepted").trim(),
                privacyVersionAccepted = row.optString("privacy_version_accepted").trim()
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Insere a linha do perfil após o cadastro (tabela `profiles`). */
    suspend fun insertProfile(
        userId: String,
        fullName: String,
        document: String,
        whatsappNumber: String,
        email: String,
        deliveryPin: String,
        accessToken: String? = null
    ): Boolean {
        val body = buildJsonObject {
            putStr("user_id", userId)
            putStr("full_name", fullName)
            putStr("document", document)
            putStr("whatsapp_number", whatsappNumber)
            putStr("email", email)
            putStr("delivery_pin", deliveryPin)
        }.toString()
        val bearer = accessToken?.takeIf { it.isNotBlank() } ?: SUPABASE_ANON_KEY
        val (ok, _) = restPostRaw("$SUPABASE_URL/rest/v1/profiles", body, bearer = bearer, prefer = "return=minimal")
        return ok
    }

    /** Atualiza nome e documento do perfil. */
    suspend fun updateCustomerPersonalProfile(
        userId: String,
        accessToken: String,
        fullName: String,
        document: String
    ): Boolean {
        if (userId.isBlank() || accessToken.isBlank()) return false
        val body = buildJsonObject {
            putStr("full_name", fullName)
            putStr("document", document)
        }.toString()
        val (ok, _) = restPatchRaw(
            "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId",
            body, bearer = accessToken, prefer = "return=minimal"
        )
        return ok
    }

    /** Atualiza o endereço embutido no perfil (grava o WhatsApp em whatsapp_number e phone). */
    suspend fun updateUserProfileAddress(
        userId: String,
        accessToken: String,
        cep: String,
        street: String,
        number: String,
        complement: String,
        neighborhood: String,
        city: String,
        state: String,
        referencePoint: String,
        whatsapp: String
    ): Boolean {
        if (userId.isBlank()) return false
        val body = buildJsonObject {
            putStr("cep", cep)
            putStr("street", street)
            putStr("number", number)
            putStr("complement", complement)
            putStr("neighborhood", neighborhood)
            putStr("city", city)
            putStr("state", com.example.shared.model.normalizeBrazilianUf(state))
            putStr("reference_point", referencePoint)
            putStr("whatsapp_number", whatsapp)
            putStr("phone", whatsapp)
        }.toString()
        val bearer = accessToken.takeIf { it.isNotBlank() } ?: SUPABASE_ANON_KEY
        val (ok, _) = restPatchRaw(
            "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId",
            body, bearer = bearer, prefer = "return=minimal"
        )
        return ok
    }

    /** Atualiza o PIN de entrega do perfil. */
    suspend fun updateCustomerDeliveryPin(userId: String, accessToken: String, pin: String): Boolean {
        if (userId.isBlank() || accessToken.isBlank()) return false
        val body = buildJsonObject {
            putStr("delivery_pin", pin)
        }.toString()
        val (ok, _) = restPatchRaw(
            "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId",
            body, bearer = accessToken, prefer = "return=minimal"
        )
        return ok
    }

    /** Atualiza só o número do endereço do perfil. */
    suspend fun updateUserProfileNumber(userId: String, accessToken: String, number: String): Boolean {
        if (userId.isBlank()) return false
        val body = buildJsonObject {
            putStr("number", number)
        }.toString()
        val bearer = accessToken.takeIf { it.isNotBlank() } ?: SUPABASE_ANON_KEY
        val (ok, _) = restPatchRaw(
            "$SUPABASE_URL/rest/v1/profiles?user_id=eq.$userId",
            body, bearer = bearer, prefer = "return=minimal"
        )
        return ok
    }

    /**
     * Exclui a conta do cliente via Edge Function `delete-account`.
     * Retorna null em sucesso; em erro, a mensagem. Igual ao original, o POST
     * leva SOMENTE o header Authorization (sem apikey).
     */
    suspend fun deleteCustomerAccount(accessToken: String, reason: String): String? {
        if (accessToken.isBlank()) return "Sua sessão expirou. Entre novamente."
        return try {
            val body = buildJsonObject {
                putStr("reason", reason.ifBlank { "Solicitação do usuário" })
            }.toString()
            val response: HttpResponse = client.post("$SUPABASE_URL/functions/v1/delete-account") {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val text = response.bodyAsText()
            if (response.status.isSuccess()) null
            else {
                val parsed = try {
                    json.parseToJsonElement(text).jsonObject.optNullableString("error")
                } catch (_: Exception) { null }
                parsed ?: "Não foi possível excluir a conta. Tente novamente."
            }
        } catch (_: Exception) {
            "Não foi possível excluir a conta. Tente novamente."
        }
    }

    /** Busca endereço pelo CEP na API pública ViaCEP (sem autenticação). */
    suspend fun fetchAddressByCep(cep: String): CepAddress? {
        val digits = cep.filter { it.isDigit() }
        if (digits.length != 8) return null
        return try {
            val response: HttpResponse = client.get("https://viacep.com.br/ws/$digits/json/")
            if (!response.status.isSuccess()) return null
            val obj = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val isError = (obj["erro"] as? JsonPrimitive)?.booleanOrNull == true
            if (isError) null
            else CepAddress(
                cep = digits,
                street = obj.optString("logradouro"),
                neighborhood = obj.optString("bairro"),
                city = obj.optString("localidade"),
                state = obj.optString("uf")
            )
        } catch (_: Exception) {
            null
        }
    }
}

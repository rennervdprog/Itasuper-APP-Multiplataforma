package com.example.shared.session

import com.example.shared.model.UserSession
import com.example.shared.model.normalizeBrazilianUf
import com.example.shared.network.SupabaseApi
import com.example.shared.platform.SecureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Porte do UserSessionRepository do app Android original: fonte única de
 * verdade da sessão do cliente em memória (StateFlow<UserSession>), com
 * persistência via [SecureStorage].
 *
 * Regras do original preservadas:
 * - A sessão só é válida com user_id + refresh_token gravados.
 * - Refresh que falha NÃO desloga o usuário.
 * - A sincronização de perfil remoto faz merge com `remote.ifBlank { current }`
 *   (campo remoto vazio nunca apaga o valor local).
 * - `updateActiveLocation` grava a localização temporária do aparelho e NUNCA
 *   altera o endereço de cadastro.
 */
object UserSessionRepository {

    // Chaves de persistência. As 5 primeiras são as mesmas já usadas pelo
    // AuthScreen/CartState — não renomear sem migrar os leitores.
    private const val KEY_USER_ID = "session_user_id"
    private const val KEY_ACCESS_TOKEN = "session_access_token"
    private const val KEY_REFRESH_TOKEN = "session_refresh_token"
    private const val KEY_USER_EMAIL = "session_user_email"
    private const val KEY_EXPIRES_AT = "session_expires_at"
    private const val KEY_NAME = "session_profile_name"
    private const val KEY_DOCUMENT = "session_profile_document"
    private const val KEY_WHATSAPP = "session_profile_whatsapp"
    private const val KEY_PIN = "session_profile_pin"
    private const val KEY_CEP = "session_addr_cep"
    private const val KEY_STREET = "session_addr_street"
    private const val KEY_NUMBER = "session_addr_number"
    private const val KEY_COMPLEMENT = "session_addr_complement"
    private const val KEY_NEIGHBORHOOD = "session_addr_neighborhood"
    private const val KEY_CITY = "session_addr_city"
    private const val KEY_STATE = "session_addr_state"
    private const val KEY_REFERENCE = "session_addr_reference"
    private const val KEY_PIX_TYPE = "session_pix_type"
    private const val KEY_PIX_KEY = "session_pix_key"
    private const val KEY_LOC_STREET = "session_loc_street"
    private const val KEY_LOC_NUMBER = "session_loc_number"
    private const val KEY_LOC_NEIGHBORHOOD = "session_loc_neighborhood"
    private const val KEY_LOC_CEP = "session_loc_cep"
    private const val KEY_LOC_CITY = "session_loc_city"
    private const val KEY_LOC_STATE = "session_loc_state"
    private const val KEY_LOC_LAT = "session_loc_lat"
    private const val KEY_LOC_LNG = "session_loc_lng"
    private const val KEY_LOC_UPDATED_AT = "session_loc_updated_at"

    private val allKeys = listOf(
        KEY_USER_ID, KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, KEY_USER_EMAIL, KEY_EXPIRES_AT,
        KEY_NAME, KEY_DOCUMENT, KEY_WHATSAPP, KEY_PIN,
        KEY_CEP, KEY_STREET, KEY_NUMBER, KEY_COMPLEMENT, KEY_NEIGHBORHOOD, KEY_CITY,
        KEY_STATE, KEY_REFERENCE, KEY_PIX_TYPE, KEY_PIX_KEY,
        KEY_LOC_STREET, KEY_LOC_NUMBER, KEY_LOC_NEIGHBORHOOD, KEY_LOC_CEP, KEY_LOC_CITY,
        KEY_LOC_STATE, KEY_LOC_LAT, KEY_LOC_LNG, KEY_LOC_UPDATED_AT
    )

    private val storage by lazy { SecureStorage() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _session = MutableStateFlow(UserSession())
    val session: StateFlow<UserSession> = _session.asStateFlow()

    val current: UserSession get() = _session.value

    /** Restaura a sessão local (chamado na abertura, antes da UI compor). */
    fun initialize() {
        if (_session.value.isSessionRestored) return
        val restored = try {
            val userId = storage.get(KEY_USER_ID).orEmpty()
            val refreshToken = storage.get(KEY_REFRESH_TOKEN).orEmpty()
            val accessToken = storage.get(KEY_ACCESS_TOKEN).orEmpty()
            if (userId.isBlank() || refreshToken.isBlank()) {
                UserSession(isSessionRestored = true)
            } else {
                UserSession(
                    isLoggedIn = true,
                    userId = userId,
                    name = storage.get(KEY_NAME).orEmpty(),
                    email = storage.get(KEY_USER_EMAIL).orEmpty(),
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    accessTokenExpiresAt = storage.get(KEY_EXPIRES_AT)?.toLongOrNull() ?: 0L,
                    isSessionRestored = true,
                    cpfCnpj = storage.get(KEY_DOCUMENT).orEmpty(),
                    whatsapp = storage.get(KEY_WHATSAPP).orEmpty(),
                    deliveryPin = storage.get(KEY_PIN).orEmpty(),
                    addressStreet = storage.get(KEY_STREET).orEmpty(),
                    addressNumber = storage.get(KEY_NUMBER).orEmpty(),
                    addressNeighborhood = storage.get(KEY_NEIGHBORHOOD).orEmpty(),
                    addressCity = storage.get(KEY_CITY).orEmpty(),
                    addressCep = storage.get(KEY_CEP).orEmpty(),
                    addressState = storage.get(KEY_STATE).orEmpty(),
                    addressComplement = storage.get(KEY_COMPLEMENT).orEmpty(),
                    addressReferencePoint = storage.get(KEY_REFERENCE).orEmpty(),
                    activeLocationStreet = storage.get(KEY_LOC_STREET).orEmpty(),
                    activeLocationNumber = storage.get(KEY_LOC_NUMBER).orEmpty(),
                    activeLocationNeighborhood = storage.get(KEY_LOC_NEIGHBORHOOD).orEmpty(),
                    activeLocationCep = storage.get(KEY_LOC_CEP).orEmpty(),
                    activeLocationCity = storage.get(KEY_LOC_CITY).orEmpty(),
                    activeLocationState = storage.get(KEY_LOC_STATE).orEmpty(),
                    activeLocationLatitude = storage.get(KEY_LOC_LAT)?.toDoubleOrNull(),
                    activeLocationLongitude = storage.get(KEY_LOC_LNG)?.toDoubleOrNull(),
                    activeLocationUpdatedAt = storage.get(KEY_LOC_UPDATED_AT)?.toLongOrNull() ?: 0L,
                    pixKeyType = storage.get(KEY_PIX_TYPE).orEmpty(),
                    pixKey = storage.get(KEY_PIX_KEY).orEmpty()
                )
            }
        } catch (_: Throwable) {
            UserSession(isSessionRestored = true)
        }
        _session.value = restored
        if (restored.isLoggedIn) {
            scope.launch { synchronizeProfileFromRemote() }
        }
    }

    /** Login: grava os tokens e sincroniza o perfil remoto em seguida. */
    fun login(
        email: String,
        userId: String,
        accessToken: String,
        refreshToken: String,
        expiresAtSeconds: Long?
    ) {
        val base = UserSession(
            isLoggedIn = true,
            userId = userId,
            email = email,
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresAt = expiresAtSeconds ?: 0L,
            isSessionRestored = true
        )
        _session.value = base
        persist(base)
        scope.launch { synchronizeProfileFromRemote() }
    }

    /** Cadastro: grava a sessão completa (perfil recém-criado incluso). */
    fun register(
        name: String,
        email: String,
        cpfCnpj: String,
        whatsapp: String,
        pin: String,
        userId: String,
        accessToken: String,
        refreshToken: String,
        expiresAtSeconds: Long?
    ) {
        val created = UserSession(
            isLoggedIn = true,
            userId = userId,
            name = name,
            email = email,
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresAt = expiresAtSeconds ?: 0L,
            isSessionRestored = true,
            cpfCnpj = cpfCnpj,
            whatsapp = whatsapp,
            deliveryPin = pin
        )
        _session.value = created
        persist(created)
    }

    /** Logout: limpa memória e disco. */
    fun logout() {
        _session.value = UserSession(isSessionRestored = true)
        try {
            allKeys.forEach { storage.remove(it) }
        } catch (_: Throwable) {
            // Falha ao limpar não impede o logout local.
        }
    }

    /**
     * Renova o access token via refresh token. Retorna o novo token ou null.
     * Refresh que falha NÃO desloga (regra do original).
     */
    suspend fun refreshAccessToken(): String? {
        val currentSession = _session.value
        if (currentSession.refreshToken.isBlank()) return null
        val result = SupabaseApi.refreshSession(currentSession.refreshToken)
        if (!result.isSuccess || result.accessToken.isNullOrBlank()) return null
        val updated = currentSession.copy(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken?.takeIf { it.isNotBlank() } ?: currentSession.refreshToken,
            accessTokenExpiresAt = result.expiresAt ?: currentSession.accessTokenExpiresAt
        )
        _session.value = updated
        persist(updated)
        return updated.accessToken
    }

    /**
     * Porte do `refreshSession()` do original (chamado no onResume e na
     * abertura): renova o access token via refresh token e sincroniza o
     * perfil em seguida. Sem refresh token, só sincroniza o perfil.
     * Refresh que falha NÃO desloga (regra do original).
     */
    fun refreshSession() {
        val currentSession = _session.value
        if (!currentSession.isLoggedIn) return
        if (currentSession.refreshToken.isBlank()) {
            scope.launch { synchronizeProfileFromRemote() }
            return
        }
        scope.launch {
            refreshAccessToken()
            synchronizeProfileFromRemote()
        }
    }

    /**
     * Busca o perfil remoto e faz merge na sessão (remoto vazio não apaga o
     * local). Chamado na abertura do app (equivalente ao refreshSession +
     * synchronizeProfile do onResume do original).
     */
    suspend fun synchronizeProfileFromRemote() {
        val currentSession = _session.value
        if (!currentSession.isLoggedIn || currentSession.userId.isBlank() || currentSession.accessToken.isBlank()) return
        val remote = SupabaseApi.fetchCustomerProfile(currentSession.userId, currentSession.accessToken) ?: return
        // Anti-troca de conta: se a sessão mudou enquanto buscava, descarta.
        if (_session.value.userId != currentSession.userId) return
        val merged = currentSession.copy(
            name = remote.fullName.ifBlank { currentSession.name },
            email = remote.email.ifBlank { currentSession.email },
            cpfCnpj = remote.document.ifBlank { currentSession.cpfCnpj },
            whatsapp = remote.whatsapp.ifBlank { currentSession.whatsapp },
            deliveryPin = remote.deliveryPin.ifBlank { currentSession.deliveryPin },
            addressCep = remote.cep.ifBlank { currentSession.addressCep },
            addressStreet = remote.street.ifBlank { currentSession.addressStreet },
            addressNumber = remote.number.ifBlank { currentSession.addressNumber },
            addressComplement = remote.complement.ifBlank { currentSession.addressComplement },
            addressNeighborhood = remote.neighborhood.ifBlank { currentSession.addressNeighborhood },
            addressCity = remote.city.ifBlank { currentSession.addressCity },
            addressState = remote.state.ifBlank { currentSession.addressState },
            addressReferencePoint = remote.referencePoint.ifBlank { currentSession.addressReferencePoint },
            pixKeyType = remote.pixType.ifBlank { currentSession.pixKeyType },
            pixKey = remote.pixKey.ifBlank { currentSession.pixKey }
        )
        _session.value = merged
        persist(merged)
    }

    /** Atualização local após salvar dados pessoais no backend. */
    fun updatePersonal(name: String, document: String) {
        val updated = _session.value.copy(name = name, cpfCnpj = document)
        _session.value = updated
        persist(updated)
    }

    /** Atualização local após salvar o endereço do perfil no backend. */
    fun updateProfile(
        cep: String,
        street: String,
        number: String,
        complement: String,
        neighborhood: String,
        city: String,
        state: String,
        referencePoint: String,
        whatsapp: String
    ) {
        val currentSession = _session.value
        val updated = currentSession.copy(
            addressCep = cep,
            addressStreet = street,
            addressNumber = number,
            addressComplement = complement,
            addressNeighborhood = neighborhood,
            addressCity = city,
            addressState = normalizeBrazilianUf(state),
            addressReferencePoint = referencePoint,
            whatsapp = whatsapp,
            // Preserva a chave PIX da sessão (regra do original).
            pixKeyType = currentSession.pixKeyType,
            pixKey = currentSession.pixKey
        )
        _session.value = updated
        persist(updated)
    }

    /** Atualização local após salvar o PIN de entrega no backend. */
    fun updatePin(pin: String) {
        val updated = _session.value.copy(deliveryPin = pin)
        _session.value = updated
        persist(updated)
    }

    /**
     * Grava a localização temporária do aparelho (GPS). NUNCA altera o
     * endereço de cadastro — é a fonte de cidade/distância enquanto ativa.
     */
    fun updateActiveLocation(
        street: String,
        number: String,
        neighborhood: String,
        cep: String,
        city: String,
        state: String,
        latitude: Double?,
        longitude: Double?
    ) {
        val updated = _session.value.copy(
            activeLocationStreet = street,
            activeLocationNumber = number,
            activeLocationNeighborhood = neighborhood,
            activeLocationCep = cep,
            activeLocationCity = city,
            activeLocationState = normalizeBrazilianUf(state),
            activeLocationLatitude = latitude,
            activeLocationLongitude = longitude,
            activeLocationUpdatedAt = Clock.System.now().toEpochMilliseconds()
        )
        _session.value = updated
        persist(updated)
    }

    /** Limpa a localização ativa (a cidade volta a ser a do cadastro). */
    fun clearActiveLocation() {
        val updated = _session.value.copy(
            activeLocationStreet = "",
            activeLocationNumber = "",
            activeLocationNeighborhood = "",
            activeLocationCep = "",
            activeLocationCity = "",
            activeLocationState = "",
            activeLocationLatitude = null,
            activeLocationLongitude = null,
            activeLocationUpdatedAt = 0L
        )
        _session.value = updated
        persist(updated)
    }

    private fun persist(session: UserSession) {
        try {
            storage.save(KEY_USER_ID, session.userId)
            storage.save(KEY_ACCESS_TOKEN, session.accessToken)
            storage.save(KEY_REFRESH_TOKEN, session.refreshToken)
            storage.save(KEY_USER_EMAIL, session.email)
            storage.save(KEY_EXPIRES_AT, session.accessTokenExpiresAt.toString())
            storage.save(KEY_NAME, session.name)
            storage.save(KEY_DOCUMENT, session.cpfCnpj)
            storage.save(KEY_WHATSAPP, session.whatsapp)
            storage.save(KEY_PIN, session.deliveryPin)
            storage.save(KEY_CEP, session.addressCep)
            storage.save(KEY_STREET, session.addressStreet)
            storage.save(KEY_NUMBER, session.addressNumber)
            storage.save(KEY_COMPLEMENT, session.addressComplement)
            storage.save(KEY_NEIGHBORHOOD, session.addressNeighborhood)
            storage.save(KEY_CITY, session.addressCity)
            storage.save(KEY_STATE, session.addressState)
            storage.save(KEY_REFERENCE, session.addressReferencePoint)
            storage.save(KEY_PIX_TYPE, session.pixKeyType)
            storage.save(KEY_PIX_KEY, session.pixKey)
            storage.save(KEY_LOC_STREET, session.activeLocationStreet)
            storage.save(KEY_LOC_NUMBER, session.activeLocationNumber)
            storage.save(KEY_LOC_NEIGHBORHOOD, session.activeLocationNeighborhood)
            storage.save(KEY_LOC_CEP, session.activeLocationCep)
            storage.save(KEY_LOC_CITY, session.activeLocationCity)
            storage.save(KEY_LOC_STATE, session.activeLocationState)
            storage.save(KEY_LOC_LAT, session.activeLocationLatitude?.toString() ?: "")
            storage.save(KEY_LOC_LNG, session.activeLocationLongitude?.toString() ?: "")
            storage.save(KEY_LOC_UPDATED_AT, session.activeLocationUpdatedAt.toString())
        } catch (_: Throwable) {
            // Falha de persistência não derruba a sessão em memória.
        }
    }
}

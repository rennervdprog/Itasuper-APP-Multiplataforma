package com.example.shared.model

/**
 * Localização do entregador (tabela `driver_locations` no Supabase).
 * Espelha com.example.data.remote.DriverLocation do app Android nativo.
 * TODO(fase-rede): buscar via Ktor/SupabaseClient (fetchDriverLocation) em vez de mock.
 */
data class DriverLocation(
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    /** ISO UTC da última atualização (ex: "2026-10-18T01:30:00Z"). */
    val updatedAt: String = ""
)

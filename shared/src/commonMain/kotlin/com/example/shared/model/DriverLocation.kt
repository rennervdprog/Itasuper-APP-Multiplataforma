package com.example.shared.model

/**
 * Localização do entregador (tabela `driver_locations` no Supabase).
 * Espelha com.example.data.remote.DriverLocation do app Android nativo.
 * Busca real em `SupabaseApi.fetchDriverLocation` (polling de 20s na tela
 * de rastreio, igual ao original).
 */
data class DriverLocation(
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    /** ISO UTC da última atualização (ex: "2026-10-18T01:30:00Z"). */
    val updatedAt: String = ""
)

package com.example.shared.model

import com.example.shared.network.SupabaseApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Fase 5 do alinhamento web × KMP (G11): o montador passa a ler os DOIS
 * caminhos de vínculo dos grupos de sabor — direto (addon_groups.product_id)
 * e vinculado (product_addon_groups) — como o fetchProductAddons da web.
 */
class FlavorAddonGroupsTest {

    private fun group(id: String, name: String, sortOrder: Int, replacesBase: Boolean = false) =
        PizzaAddonGroup(
            id = id,
            name = name,
            minSelect = 0,
            maxSelect = 3,
            priceReplacesBase = replacesBase,
            items = listOf(
                AddonItem(id = "$id-i1", groupId = id, name = "Item 1", price = 5.0, sortOrder = 1),
                AddonItem(id = "$id-i2", groupId = id, name = "Item 2", price = 7.0, sortOrder = 0)
            ),
            sortOrder = sortOrder
        )

    @Test
    fun diretoGanhaNoConflitoDeId() {
        // Mesmo grupo nos dois caminhos: vale o direto (regra da web).
        val direct = listOf(group("g1", "Direto", sortOrder = 2, replacesBase = true))
        val linked = listOf(group("g1", "Vinculado", sortOrder = 1, replacesBase = false))
        val merged = SupabaseApi.mergeFlavorAddonGroups(direct, linked)
        assertEquals(1, merged.size)
        assertEquals("Direto", merged[0].name)
        assertEquals(true, merged[0].priceReplacesBase)
    }

    @Test
    fun vinculadoSemConflitoEntra() {
        val direct = listOf(group("g1", "Direto", sortOrder = 2))
        val linked = listOf(group("g2", "Vinculado", sortOrder = 1))
        val merged = SupabaseApi.mergeFlavorAddonGroups(direct, linked)
        assertEquals(listOf("g2", "g1"), merged.map { it.id })
    }

    @Test
    fun ordenadoPorSortOrderEstavel() {
        val direct = listOf(
            group("g1", "A", sortOrder = 2),
            group("g2", "B", sortOrder = 0)
        )
        val linked = listOf(group("g3", "C", sortOrder = 0))
        val merged = SupabaseApi.mergeFlavorAddonGroups(direct, linked)
        // Empate no sort_order mantém direto antes de vinculado (ordem estável).
        assertEquals(listOf("g2", "g3", "g1"), merged.map { it.id })
    }

    @Test
    fun entradasVaziasDevolvemVazio() {
        assertEquals(emptyList(), SupabaseApi.mergeFlavorAddonGroups(emptyList(), emptyList()))
        assertEquals(1, SupabaseApi.mergeFlavorAddonGroups(listOf(group("g1", "A", 0)), emptyList()).size)
        assertEquals(1, SupabaseApi.mergeFlavorAddonGroups(emptyList(), listOf(group("g1", "A", 0))).size)
    }
}

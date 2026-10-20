package com.example.shared.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperTextPrimary
import com.example.shared.theme.ItaSuperTextSecondary

/**
 * Diálogos de endereço/localização da Home (ITEM 3/6).
 *
 * Migração 1:1 de `com.example.ui.home.AddressLocationDialogs` do app Android
 * nativo. Estrutura, textos, ordem e espaçamento idênticos ao original;
 * apenas o pacote do tema mudou (`com.example.ui.theme` →
 * `com.example.shared.theme`).
 *
 * `AddressDraft` também foi migrado para cá: no app nativo ele mora no
 * `HomeViewModel.kt`; no KMP o estado fica na tela, então o rascunho viaja
 * junto dos diálogos que o consomem.
 *
 * Fluxos reais ligados: GPS com permissão + geocodificação reversa
 * (`LocationProvider.getCurrentAddress`), busca de CEP via ViaCEP e
 * salvamento do endereço no perfil (`SupabaseApi`), tudo na HomeScreen.
 */
data class AddressDraft(
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val referencePoint: String = "",
    val whatsapp: String = ""
)

@Composable
fun LocationOrAddressDialog(
    visible: Boolean,
    registeredStreet: String,
    registeredNumber: String,
    registeredNeighborhood: String,
    registeredCity: String,
    currentStreet: String,
    currentNumber: String,
    currentNeighborhood: String,
    currentCity: String,
    isRefreshingLocation: Boolean,
    onDismiss: () -> Unit,
    onUseRegisteredAddress: () -> Unit,
    onAllowLocation: () -> Unit,
    onRegisterAddress: () -> Unit
) {
    if (!visible) return

    val registeredLine = listOf(registeredStreet.trim(), registeredNumber.trim())
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val registeredDetails = listOf(registeredNeighborhood.trim(), registeredCity.trim())
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val currentLine = listOf(currentStreet.trim(), currentNumber.trim())
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val currentDetails = listOf(currentNeighborhood.trim(), currentCity.trim())
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val hasRegisteredAddress = registeredLine.isNotBlank() || registeredDetails.isNotBlank()
    val hasCurrentAddress = currentLine.isNotBlank() || currentDetails.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = ItaSuperPrimary
            )
        },
        title = {
            Text(
                text = "Escolha o endereço de entrega",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = ItaSuperTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Selecione um endereço cadastrado ou use sua localização atual.",
                    textAlign = TextAlign.Center,
                    color = ItaSuperTextSecondary
                )

                if (hasRegisteredAddress) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onUseRegisteredAddress),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF7F0),
                        border = BorderStroke(1.dp, ItaSuperPrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Endereço cadastrado",
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperTextPrimary
                                )
                                Text(
                                    text = registeredLine.ifBlank { registeredDetails },
                                    color = ItaSuperTextPrimary,
                                    maxLines = 1
                                )
                                if (registeredLine.isNotBlank() && registeredDetails.isNotBlank()) {
                                    Text(
                                        text = registeredDetails,
                                        color = ItaSuperTextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selecionar endereço cadastrado",
                                tint = ItaSuperPrimary
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onRegisterAddress,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.PinDrop, contentDescription = null, tint = ItaSuperPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Cadastrar endereço", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = onAllowLocation,
                    enabled = !isRefreshingLocation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isRefreshingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = ItaSuperPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Usar localização atual", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                        if (hasCurrentAddress) {
                            Text(
                                text = listOf(currentLine, currentDetails)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = ItaSuperTextSecondary,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                "Atualizar pelo GPS",
                                style = MaterialTheme.typography.bodySmall,
                                color = ItaSuperTextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onRegisterAddress) {
                Text("Editar endereço", color = ItaSuperPrimary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressRegistrationSheet(
    visible: Boolean,
    draft: AddressDraft,
    isLookingUpCep: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onDraftChange: ((AddressDraft) -> AddressDraft) -> Unit,
    onLookupCep: () -> Unit,
    onSave: () -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Complete seu endereço",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = ItaSuperTextPrimary
            )
            Text(
                text = "Usaremos a cidade informada para mostrar lojas que atendem sua região.",
                style = MaterialTheme.typography.bodyMedium,
                color = ItaSuperTextSecondary
            )

            AddressField(
                label = "CEP",
                value = draft.cep,
                keyboardType = KeyboardType.Number,
                onValueChange = { value -> onDraftChange { it.copy(cep = value.filter(Char::isDigit).take(8)) } },
                trailing = {
                    if (isLookingUpCep) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp), strokeWidth = 2.dp)
                    } else {
                        OutlinedButton(onClick = onLookupCep, enabled = draft.cep.filter(Char::isDigit).length == 8) {
                            Text("Buscar")
                        }
                    }
                }
            )
            AddressField("Rua", draft.street) { value -> onDraftChange { it.copy(street = value) } }
            AddressField("Número", draft.number, KeyboardType.Text) { value -> onDraftChange { it.copy(number = value) } }
            AddressField("Complemento", draft.complement) { value -> onDraftChange { it.copy(complement = value) } }
            AddressField("Bairro", draft.neighborhood) { value -> onDraftChange { it.copy(neighborhood = value) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AddressField(
                    label = "Cidade",
                    value = draft.city,
                    modifier = Modifier.weight(1f),
                    onValueChange = { value -> onDraftChange { it.copy(city = value) } }
                )
                AddressField(
                    label = "UF",
                    value = draft.state,
                    modifier = Modifier.width(88.dp),
                    onValueChange = { value -> onDraftChange { it.copy(state = value.uppercase().take(2)) } }
                )
            }
            AddressField("Ponto de referência", draft.referencePoint) { value -> onDraftChange { it.copy(referencePoint = value) } }
            AddressField("WhatsApp com DDD", draft.whatsapp, KeyboardType.Phone) { value -> onDraftChange { it.copy(whatsapp = value.filter { char -> char.isDigit() }.take(13)) } }

            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onSave,
                enabled = !isSaving && !isLookingUpCep,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.height(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar endereço e continuar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddressField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailing,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary)
    )
}

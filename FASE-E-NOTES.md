# Fase E — Restante do mapa do app original (notificações, pedidos, checkout, loja)

Commits:
- Parte 1 `813771e7` — notificações reais, bloqueio de farmácia, ações de
  pedidos/PIX, API completa (pedidos/avaliações/benefícios/cotação/endereços/
  cardápio), vitrine com adicionais reais. (Build quebrou por import
  duplicado de `AddonItem`; corrigido na parte 2.)
- Parte 2 `6e616766` — checkout completo.
- Parte 3 `94b10270` — avaliações na loja, favoritos, repetir pedido,
  compartilhar, comprovante.

## O que foi portado

### Notificações (NotificationsViewModel)
- `fetchClientNotifications` via edge `client-notifications` (Bearer);
  erro/token vazio LANÇA (a tela mostra "Não foi possível carregar...").
- `markClientNotificationRead` (PATCH `read_at`), polling de 15s, dispensar
  local persistente, marcar todas como lidas otimista, tocar navega pros
  pedidos quando há `order_id`.

### Pedidos (OrdersViewModel)
- Refresh real com a regra do original: falha transitória NÃO apaga a
  lista exibida; lojas avaliadas fazem merge (união).
- Cancelar (`apply_cancellation_policy`), confirmar entrega
  (`client_confirm_delivery`), avaliar (UPSERT em reviews + review_tags,
  nota ≤3 exige comentário ≥12), reembolso PIX Direto
  (`create_pix_direto_refund_case`, motivo fixo "other", elegibilidade via
  RefundEligibility) — mensagens idênticas às do original.
- PIX online real (payment-router) com QR Base64 exibido via Coil e copiar
  código via clipboard do Compose; PIX Direto com a chave real da loja.
- Repetir pedido: repõe a sacola; outra loja → StoreSwitchBottomSheet
  antes de esvaziar; navega pra sacola.
- Comprovante: seletor nativo (expect/actual) + upload real.

### Farmácia (3 camadas)
- `CartHolder.isPharmacyBlocked` usado no `addProduct` (retorna false =
  bloqueado) e na re-checagem do `placeOrder`; vitrine mostra o aviso
  "Este item não pode ser adicionado à sacola." (AlertDialog — KMP não tem
  SnackbarHost) e NÃO esvazia a sacola ao confirmar troca se bloqueado.

### Checkout (CartRepository + OrdersViewModel.checkoutOrder)
- Perfil de entrega da loja registrado pela vitrine; taxa fixa oficial
  entra com chave `fixed:<loja>` sem depender de endereço.
- Cotação oficial sincronizada (`synchronizeDeliveryQuote`): retirada →
  pickup; taxa fixa → imediata; por km → edge `quote-delivery` com dedupe
  por chave (endereço+subtotal), falha reabre o editor e limpa a cotação.
- `placeOrder`: validações do original (farmácia, sessão, troco ≥ total,
  endereço completo, portões da cotação), confirmação final da taxa fixa
  na central (taxa mudou > R$ 0,009 → avisa e para), snapshot da cotação +
  coordenadas + taxa absorvida no pedido, status por pagamento/pré-pedido,
  agendamento/release em ISO; benefícios e cupom DEPOIS do insert:
  `redeemLoyaltyPoints` → `applyWalletDiscount` (só conta se a RPC
  confirmar) → `registerCouponUse`.
- Cupom com todas as guardas: expiração, limite de usos, mínimo, uso
  anterior (`coupon_uses`), primeiro pedido (`hasNonCancelledOrders`) —
  validação que falha (null) NUNCA libera o cupom.
- Benefícios: saldo da carteira, config e pontos da loja com as fórmulas
  do `recalculateBenefits` (teto por % do subtotal, mínimo de resgate).
- Endereços: lista real + endereço do perfil mesclado
  (`__profile_address__`), salvar via `createSavedAddress` (sem endpoint
  de update, igual ao original), ViaCEP real preenchendo o editor.
- Política de localização: GPS fresco ≤ 5 min; divergência > 300m (ou
  cidade/rua/número diferentes) abre a confirmação; "usar localização"
  preenche o editor SÓ com dados do GPS (nunca CEP salvo) e exige cotação
  pronta pra confirmar.
- Sacola revalida contra o catálogo ao abrir (remove indisponíveis com
  aviso), igual ao `validateAgainstCatalog`.

### Loja
- Vitrine por `fetchStoreById` (cache mantido em falha), seções reais de
  `menu_sections` (derivadas viram fallback), disponibilidade de entrega
  própria via RPC 1×, modal do produto com adicionais reais (grupos
  diretos + vinculados, min/max, seleção única/múltipla, preço com
  adicionais e `priceReplacesBase`).
- "Sobre a loja": avaliações reais (aggregate 90d + reviews públicas com
  tags e respostas), histograma 5→1.
- Favoritos com persistência local (porte do FavoriteStoresStorage) na
  vitrine e no Sobre a loja.

### Plataforma (expect/actual novos)
- `rememberTextSharer`: Intent ACTION_SEND (Android) /
  UIActivityViewController (iOS).
- `rememberFilePicker`: OpenDocument jpeg/png/pdf (Android) /
  UIDocumentPickerViewController (iOS), bytes + mime + extensão.

## Desvios registrados (intencionais)
1. `quoteDelivery`: o original renova o token e tenta de novo em 401
   (`executeWithTokenRefresh`); o KMP faz tentativa única (o SupabaseApi
   não acessa a sessão — a camada de checkout pode renovar e repetir).
2. `submitOrderRating` retorna só Boolean; o original guarda o erro
   detalhado num var global (`SupabaseClient.lastRatingError`, wart
   conhecido do próprio código).
3. `createSavedAddress` devolve o endereço COM city/state da resposta; o
   original omite esses dois campos no objeto retornado (superset
   inofensivo).
4. `fetchAddonsForProduct`: o fallback de chave `addon_group_id` também
   vale pros itens (no original, itens leem só `group_id`) — superset
   inofensivo.
5. Repetir pedido com conflito de loja NÃO navega antes da confirmação
   (no original navega e a folha fica no estado da tela anterior; no KMP a
   tela desmontaria e perderia a confirmação).
6. `fetchMenuSectionsForStore`/`fetchAddonGroupsForStore` mantêm o
   fallback fiel do original: se a query filtrada falhar, busca a tabela
   inteira sem filtro.
7. Snackbar do GPS na Home/Search ("Não foi possível obter a localização
   atual...") não portado: KMP não tem SnackbarHost (registrado na Fase C).
8. Mapa do rastreio segue placeholder (WebView/Leaflet não portável pro
   commonMain) — registrado na Fase D.

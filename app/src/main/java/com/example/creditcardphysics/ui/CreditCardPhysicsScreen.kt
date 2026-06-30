package com.example.creditcardphysics.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Stable
private data class CreditCardModel(
  val id: Int,
  val bank: String,
  val holder: String,
  val maskedNumber: String,
  val balance: String,
  val badge: String,
  val gradient: List<Color>,
  val accent: Color,
)

private object WalletPhysicsConfig {
  const val MaxRotationZ = 9f
  const val MaxRotationY = 13f
  const val DragScale = 1.045f
  const val RestScale = 1f
  const val ActiveElevation = 34f
  const val RestElevation = 16f
  const val SwipeThreshold = 118f
  const val ExpandThreshold = 126f
  const val VerticalThresholdRatio = 0.25f
  const val HorizontalThresholdRatio = 0.18f

  val CardSpring = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = Spring.StiffnessLow,
    visibilityThreshold = 0.25f,
  )
  val GestureSpring = spring<Float>(
    dampingRatio = 0.78f,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = 0.2f,
  )
  val ScaleSpring = spring<Float>(
    dampingRatio = 0.78f,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = 0.002f,
  )
}

private enum class WalletMode {
  Browsing,
  Details,
}

private enum class GestureAxis {
  Undecided,
  Horizontal,
  Vertical,
}
private data class WalletLayoutMetrics(
  val cardWidthPx: Float,
  val cardHeightPx: Float,
  val viewportHeightPx: Float,
  val collapsedGapPx: Float,
  val collapsedXStepPx: Float,
  val expandedMinGapPx: Float,
  val expandedMaxGapPx: Float,
  val focusedSideOffsetPx: Float,
)

@Stable
private class PhysicalCardState(
  val model: CreditCardModel,
  initialX: Float = 0f,
  initialY: Float = 0f,
) {
  val x = Animatable(initialX)
  val y = Animatable(initialY)
  val scale = Animatable(0.96f)
  val rotationZ = Animatable(0f)
  val rotationX = Animatable(0f)
  val rotationY = Animatable(0f)
  val flipRotation = Animatable(0f)
  val elevation = Animatable(WalletPhysicsConfig.RestElevation)
  val alpha = Animatable(1f)
  var isSelected by mutableStateOf(false)
  var isDragging by mutableStateOf(false)
  var isShowingBack by mutableStateOf(false)
  var completedDrag by mutableStateOf(false)
  var completedVerticalDrag by mutableStateOf(false)
  var velocity by mutableStateOf(Offset.Zero)
  var dragDelta by mutableStateOf(Offset.Zero)
  var dragStartPosition by mutableStateOf(Offset.Zero)
  var xJob: Job? = null
  var yJob: Job? = null
  var scaleJob: Job? = null
  var rotationJob: Job? = null
  var flipJob: Job? = null
  var elevationJob: Job? = null
  var alphaJob: Job? = null

  fun cancelMotion() {
    xJob?.cancel()
    yJob?.cancel()
    scaleJob?.cancel()
    rotationJob?.cancel()
    flipJob?.cancel()
    elevationJob?.cancel()
    alphaJob?.cancel()
  }
}

@Stable
private class WalletState(
  models: List<CreditCardModel>,
) {
  val cards = models.map { PhysicalCardState(it) }
  val cardOrder = mutableStateListOf(*models.indices.toList().toTypedArray())
  var mode by mutableStateOf(WalletMode.Browsing)
  var selectedIndex by mutableIntStateOf(0)
  var draggingIndex by mutableIntStateOf(-1)

  fun orderSlot(index: Int): Int = cardOrder.indexOf(index)
}

private class WalletInteractionController(
  private val state: WalletState,
  private val scope: CoroutineScope,
) {
  fun enter(metrics: WalletLayoutMetrics) {
    state.cards.forEachIndexed { index, card ->
      card.yJob?.cancel()
      card.yJob = scope.launch {
        delay(index * 58L)
        card.x.snapTo(0f)
        card.y.snapTo(-42f)
        card.scale.snapTo(0.92f)
        card.alpha.snapTo(1f)
        card.y.animateTo(targetFor(index, metrics).y, WalletPhysicsConfig.CardSpring)
      }
      card.scaleJob?.cancel()
      card.scaleJob = scope.launch {
        delay(index * 58L)
        card.scale.animateTo(targetFor(index, metrics).scale, WalletPhysicsConfig.ScaleSpring)
      }
    }
    updateSelection()
  }

  fun settle(metrics: WalletLayoutMetrics) {
    updateSelection()
    state.cards.forEachIndexed { index, card ->
      val target = targetFor(index, metrics)
      val delayMillis = when (state.mode) {
        WalletMode.Browsing -> state.orderSlot(index) * 22L
        WalletMode.Details -> state.orderSlot(index) * 28L
      }
      card.xJob?.cancel()
      card.xJob = scope.launch {
        delay(delayMillis)
        card.x.animateTo(target.x, WalletPhysicsConfig.CardSpring)
      }
      card.yJob?.cancel()
      card.yJob = scope.launch {
        delay(delayMillis)
        card.y.animateTo(target.y, WalletPhysicsConfig.CardSpring)
      }
      card.scaleJob?.cancel()
      card.scaleJob = scope.launch {
        delay(delayMillis)
        card.scale.animateTo(target.scale, WalletPhysicsConfig.ScaleSpring)
      }
      card.rotationJob?.cancel()
      card.rotationJob = scope.launch {
        card.rotationZ.animateTo(target.rotationZ, WalletPhysicsConfig.CardSpring)
        card.rotationX.animateTo(target.rotationX, WalletPhysicsConfig.GestureSpring)
        card.rotationY.animateTo(target.rotationY, WalletPhysicsConfig.GestureSpring)
      }
      card.elevationJob?.cancel()
      card.elevationJob = scope.launch {
        card.elevation.animateTo(target.elevation, WalletPhysicsConfig.CardSpring)
      }
      card.alphaJob?.cancel()
      card.alphaJob = scope.launch {
        delay(delayMillis)
        card.alpha.animateTo(target.alpha, WalletPhysicsConfig.CardSpring)
      }
      card.flipJob?.cancel()
      card.flipJob = scope.launch {
        if (state.mode != WalletMode.Details || index != state.selectedIndex) {
          card.isShowingBack = false
          card.flipRotation.animateTo(0f, WalletPhysicsConfig.CardSpring)
        }
      }
    }
  }

  fun beginDrag(index: Int) {
    state.cards[index].cancelMotion()
    state.draggingIndex = index
    state.cards[index].isDragging = true
    state.cards[index].completedDrag = false
    state.cards[index].completedVerticalDrag = false
    state.cards[index].dragDelta = Offset.Zero
    state.cards[index].dragStartPosition = Offset(state.cards[index].x.value, state.cards[index].y.value)
    state.cards[index].scaleJob = scope.launch { state.cards[index].scale.animateTo(WalletPhysicsConfig.DragScale, WalletPhysicsConfig.ScaleSpring) }
    state.cards[index].elevationJob = scope.launch { state.cards[index].elevation.animateTo(WalletPhysicsConfig.ActiveElevation, WalletPhysicsConfig.CardSpring) }
  }

  fun prepareTouch(index: Int) {
    state.cards[index].cancelMotion()
    state.cards[index].dragStartPosition = Offset(state.cards[index].x.value, state.cards[index].y.value)
  }

  fun drag(index: Int, delta: Offset, velocity: Offset, metrics: WalletLayoutMetrics) {
    val card = state.cards[index]
    if (card.completedDrag || card.completedVerticalDrag || index != state.cardOrder.first()) return

    card.velocity = velocity
    card.dragDelta += delta

    val dragX = card.dragDelta.x
    val dragY = card.dragDelta.y
    val threshold = metrics.cardWidthPx * WalletPhysicsConfig.HorizontalThresholdRatio
    val verticalThreshold = WalletPhysicsConfig.ExpandThreshold

    if (abs(dragX) > threshold && abs(dragX) > abs(dragY)) {
      if (state.mode == WalletMode.Details) {
        flipCard(index, if (dragX < 0f) -1f else 1f)
      } else {
        sendCardToBack(index, metrics, if (dragX < 0f) -1f else 1f)
      }
      return
    }

    if (abs(dragY) > verticalThreshold && abs(dragY) > abs(dragX)) {
      handleVerticalDrag(dragY, metrics)
      return
    }

    val dragOrigin = card.dragStartPosition

    card.xJob?.cancel()
    card.yJob?.cancel()
    card.rotationJob?.cancel()
    card.xJob = scope.launch {
      val targetX = if (state.mode == WalletMode.Details) dragOrigin.x else dragOrigin.x + dragX
      card.x.snapTo(targetX)
    }
    card.yJob = scope.launch { card.y.snapTo(dragOrigin.y) }
    card.rotationJob = scope.launch {
      card.rotationZ.snapTo((dragX / metrics.cardWidthPx * 8f + velocity.x / 240f).coerceIn(-WalletPhysicsConfig.MaxRotationZ, WalletPhysicsConfig.MaxRotationZ))
      card.rotationY.snapTo((dragX / metrics.cardWidthPx * WalletPhysicsConfig.MaxRotationY).coerceIn(-WalletPhysicsConfig.MaxRotationY, WalletPhysicsConfig.MaxRotationY))
      card.rotationX.snapTo(0f)
    }
  }

  fun endDrag(
    index: Int,
    metrics: WalletLayoutMetrics,
    releaseVelocity: Offset = state.cards[index].velocity,
  ) {
    val card = state.cards[index]
    card.velocity = releaseVelocity
    card.isDragging = false
    state.draggingIndex = -1

    if (card.completedDrag) {
      card.completedDrag = false
    } else if (card.completedVerticalDrag) {
      card.completedVerticalDrag = false
    } else if (abs(card.dragDelta.x) > metrics.cardWidthPx * WalletPhysicsConfig.HorizontalThresholdRatio) {
      if (state.mode == WalletMode.Details) {
        flipCard(index, if (card.dragDelta.x < 0f) -1f else 1f)
      } else {
        sendCardToBack(index, metrics, if (card.dragDelta.x < 0f) -1f else 1f)
      }
    } else if (abs(card.dragDelta.y) > WalletPhysicsConfig.ExpandThreshold) {
      handleVerticalDrag(card.dragDelta.y, metrics)
    } else {
      settle(metrics)
    }
    card.dragDelta = Offset.Zero
  }

  fun handleCardTap(index: Int, metrics: WalletLayoutMetrics) {
    if (index != state.cardOrder.first()) return

    state.selectedIndex = index
    state.mode = if (state.mode == WalletMode.Details) WalletMode.Browsing else WalletMode.Details
    settle(metrics)
  }

  fun handleOutsideTap(metrics: WalletLayoutMetrics) {
    if (state.mode == WalletMode.Details) {
      state.mode = WalletMode.Browsing
      settle(metrics)
    }
  }

  fun selectNextCard(metrics: WalletLayoutMetrics) {
    val current = state.cardOrder.removeAt(0)
    state.cardOrder.add(current)
    state.selectedIndex = state.cardOrder.first()
    settle(metrics)
  }

  fun selectPreviousCard(metrics: WalletLayoutMetrics) {
    val previous = state.cardOrder.removeAt(state.cardOrder.lastIndex)
    state.cardOrder.add(0, previous)
    state.selectedIndex = previous
    settle(metrics)
  }

  fun handleHorizontalSwipe(deltaX: Float, metrics: WalletLayoutMetrics) {
    val threshold = metrics.cardWidthPx * WalletPhysicsConfig.HorizontalThresholdRatio
    when {
      abs(deltaX) > threshold -> sendCardToBack(state.selectedIndex, metrics, if (deltaX < 0f) -1f else 1f)
      else -> settle(metrics)
    }
  }

  private fun handleVerticalDrag(deltaY: Float, metrics: WalletLayoutMetrics) {
    val topIndex = state.cardOrder.first()
    val card = state.cards[topIndex]
    card.completedVerticalDrag = true
    card.dragDelta = Offset.Zero
    card.isDragging = false
    state.draggingIndex = -1

    state.mode = when {
      deltaY < 0f -> WalletMode.Details
      state.mode == WalletMode.Details -> WalletMode.Browsing
      else -> state.mode
    }
    settle(metrics)
  }

  private fun flipCard(index: Int, direction: Float) {
    val card = state.cards[index]
    card.completedDrag = true
    card.dragDelta = Offset.Zero
    card.isDragging = false
    state.draggingIndex = -1
    card.xJob?.cancel()
    card.rotationJob?.cancel()
    card.flipJob?.cancel()

    card.xJob = scope.launch {
      card.x.animateTo(0f, WalletPhysicsConfig.CardSpring)
    }
    card.rotationJob = scope.launch {
      card.rotationZ.animateTo(0f, WalletPhysicsConfig.CardSpring)
      card.rotationX.animateTo(0f, WalletPhysicsConfig.GestureSpring)
      card.rotationY.animateTo(0f, WalletPhysicsConfig.GestureSpring)
    }
    card.flipJob = scope.launch {
      card.isShowingBack = !card.isShowingBack
      val signedTurn = if (direction < 0f) -180f else 180f
      val target = if (card.isShowingBack) signedTurn else 0f
      card.flipRotation.animateTo(target, WalletPhysicsConfig.CardSpring)
      card.completedDrag = false
    }
  }

  private fun sendCardToBack(index: Int, metrics: WalletLayoutMetrics, direction: Float) {
    val current = index
    val card = state.cards[current]
    card.completedDrag = true
    card.dragDelta = Offset.Zero
    state.draggingIndex = -1

    card.xJob?.cancel()
    card.yJob?.cancel()
    card.rotationJob?.cancel()
    card.scaleJob?.cancel()

    card.xJob = scope.launch {
      card.x.animateTo(
        targetValue = direction * metrics.cardWidthPx * 0.72f,
        animationSpec = tween(durationMillis = 220),
      )
    }
    card.rotationJob = scope.launch {
      card.rotationZ.animateTo(
        targetValue = direction * WalletPhysicsConfig.MaxRotationZ,
        animationSpec = tween(durationMillis = 220),
      )
    }
    card.scaleJob = scope.launch {
      card.scale.animateTo(
        targetValue = 0.98f,
        animationSpec = tween(durationMillis = 220),
      )
    }
    scope.launch {
      delay(170L)
      card.isDragging = false
      state.cardOrder.remove(current)
      state.cardOrder.add(current)
      state.selectedIndex = state.cardOrder.first()
      card.completedDrag = false
      settle(metrics)
    }
  }

  private fun updateSelection() {
    state.cards.forEachIndexed { index, card ->
      card.isSelected = index == state.selectedIndex
    }
  }

  private fun targetFor(index: Int, metrics: WalletLayoutMetrics): CardTarget {
    val signed = signedSlot(index)
    val selectedY = if (state.mode == WalletMode.Details) {
      metrics.viewportHeightPx * 0.15f
    } else {
      metrics.viewportHeightPx * 0.50f - metrics.cardHeightPx * 0.50f
    }
    val depth = min(abs(signed), 4)
    val side = when {
      signed < 0 -> -1f
      signed > 0 -> 1f
      else -> 0f
    }
    return when (signed) {
      0 -> CardTarget(
        x = 0f,
        y = selectedY,
        scale = if (state.mode == WalletMode.Details) 0.98f else 1.02f,
        rotationZ = 0f,
        elevation = WalletPhysicsConfig.ActiveElevation,
        alpha = 1f,
      )
      else -> CardTarget(
        x = if (state.mode == WalletMode.Details) {
          side * metrics.cardWidthPx * 0.18f
        } else {
          side * depth * metrics.collapsedXStepPx * 2.4f
        },
        y = if (state.mode == WalletMode.Details) {
          metrics.viewportHeightPx + metrics.cardHeightPx * 0.35f
        } else {
          selectedY + depth * metrics.collapsedGapPx * 1.45f
        },
        scale = if (state.mode == WalletMode.Details) {
          0.90f
        } else {
          when (depth) {
            1 -> 0.97f
            2 -> 0.94f
            3 -> 0.91f
            else -> 0.88f
          }
        },
        rotationZ = if (state.mode == WalletMode.Details) {
          side * 4f
        } else {
          side * when (depth) {
            1 -> 2.1f
            2 -> -1.6f
            3 -> 1.1f
            else -> 0f
          }
        },
        rotationY = if (state.mode == WalletMode.Details) 0f else side * -3f,
        elevation = WalletPhysicsConfig.RestElevation,
        alpha = if (state.mode == WalletMode.Details) 0f else 1f,
      )
    }
  }

  private fun signedSlot(index: Int): Int {
    val slot = state.orderSlot(index)
    if (slot == 0) return 0
    return if (slot <= state.cards.size / 2) slot else slot - state.cards.size
  }
}

private data class CardTarget(
  val x: Float,
  val y: Float,
  val scale: Float,
  val rotationZ: Float,
  val rotationX: Float = 0f,
  val rotationY: Float = 0f,
  val elevation: Float,
  val alpha: Float,
)

@Composable
private fun CardDetailsPanel(
  card: CreditCardModel,
  progress: Float,
  modifier: Modifier = Modifier,
) {
  var cardEnabled by remember(card.id) { mutableStateOf(true) }
  var contactlessEnabled by remember(card.id) { mutableStateOf(true) }
  var onlineEnabled by remember(card.id) { mutableStateOf(true) }
  var internationalEnabled by remember(card.id) { mutableStateOf(false) }
  val density = LocalDensity.current

  Box(
    modifier = modifier
      .graphicsLayer {
        alpha = progress
        translationY = with(density) { (1f - progress) * 220.dp.toPx() }
      }
      .clip(RoundedCornerShape(28.dp))
      .background(Color(0xFF0F1420).copy(alpha = 0.92f))
      .padding(horizontal = 22.dp, vertical = 18.dp),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "${card.bank} ${card.badge}",
            color = Color.White.copy(alpha = 0.58f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
          )
          Text(
            text = card.maskedNumber,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
          )
        }
        Text(
          text = if (cardEnabled) "ATIVO" else "BLOQUEADO",
          color = if (cardEnabled) card.accent else Color(0xFFFF8A8A),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.sp,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CardInfoItem(
          label = "Limite disponível",
          value = card.balance,
          modifier = Modifier.weight(1f),
        )
        CardInfoItem(
          label = "Vencimento",
          value = "12 JUL",
          modifier = Modifier.weight(1f),
        )
      }

      CardToggleRow(
        title = "Cartão ativo",
        subtitle = "Bloquear ou liberar novas compras",
        checked = cardEnabled,
        onCheckedChange = { cardEnabled = it },
      )
      CardToggleRow(
        title = "Pagamento por aproximação",
        subtitle = "Usar NFC em maquininhas compatíveis",
        checked = contactlessEnabled,
        onCheckedChange = { contactlessEnabled = it },
      )
      CardToggleRow(
        title = "Compras online",
        subtitle = "Autorizar pagamentos em apps e sites",
        checked = onlineEnabled,
        onCheckedChange = { onlineEnabled = it },
      )
      CardToggleRow(
        title = "Uso internacional",
        subtitle = "Liberar compras fora do país",
        checked = internationalEnabled,
        onCheckedChange = { internationalEnabled = it },
      )
    }
  }
}

@Composable
private fun CardInfoItem(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(Color.White.copy(alpha = 0.07f))
      .padding(horizontal = 14.dp, vertical = 12.dp),
  ) {
    Text(
      text = label,
      color = Color.White.copy(alpha = 0.50f),
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      letterSpacing = 0.sp,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = value,
      color = Color.White,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.sp,
    )
  }
}

@Composable
private fun CardToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f),
    ) {
      Text(
        text = title,
        color = Color.White.copy(alpha = 0.90f),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        color = Color.White.copy(alpha = 0.48f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
  }
}

@Composable
fun CreditCardPhysicsScreen() {
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val walletState = remember { WalletState(creditCards) }
  val controller = remember(walletState) { WalletInteractionController(walletState, scope) }
  val expandedGlow by animateFloatAsState(
    targetValue = if (walletState.mode == WalletMode.Details) 1f else 0f,
    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
    label = "walletGlow",
  )
  val detailsProgress by animateFloatAsState(
    targetValue = if (walletState.mode == WalletMode.Details) 1f else 0f,
    animationSpec = WalletPhysicsConfig.CardSpring,
    label = "detailsProgress",
  )

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          0f to Color(0xFF05060A),
          0.48f to Color(0xFF10131C),
          1f to Color(0xFF050507),
        ),
      ),
  ) {
    val cardWidth = 324.dp
    val cardHeight = 204.dp
    val metrics = WalletLayoutMetrics(
      cardWidthPx = with(density) { cardWidth.toPx() },
      cardHeightPx = with(density) { cardHeight.toPx() },
      viewportHeightPx = with(density) { maxHeight.toPx() },
      collapsedGapPx = with(density) { 18.dp.toPx() },
      collapsedXStepPx = with(density) { 6.dp.toPx() },
      expandedMinGapPx = with(density) { 86.dp.toPx() },
      expandedMaxGapPx = with(density) { 110.dp.toPx() },
      focusedSideOffsetPx = with(density) { 46.dp.toPx() },
    )

    LaunchedEffect(Unit) {
      controller.enter(metrics)
      controller.settle(metrics)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = Color(0xFF8B5CF6).copy(alpha = 0.10f + expandedGlow * 0.05f),
        radius = size.width * 0.58f,
        center = Offset(size.width * 0.50f, size.height * 0.34f),
      )
      drawCircle(
        color = Color(0xFF14B8A6).copy(alpha = 0.07f),
        radius = size.width * 0.44f,
        center = Offset(size.width * 0.16f, size.height * 0.86f),
      )
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .clickable { controller.handleOutsideTap(metrics) },
    )

    WalletStack(
      walletState = walletState,
      controller = controller,
      cardWidth = cardWidth,
      cardHeight = cardHeight,
      metrics = metrics,
      modifier = Modifier.fillMaxSize(),
    )

    CardDetailsPanel(
      card = walletState.cards[walletState.selectedIndex].model,
      progress = detailsProgress,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 24.dp, vertical = 34.dp),
    )
  }
}

@Composable
private fun WalletStack(
  walletState: WalletState,
  controller: WalletInteractionController,
  cardWidth: Dp,
  cardHeight: Dp,
  metrics: WalletLayoutMetrics,
  modifier: Modifier = Modifier,
) {
  val orderedCards by remember(walletState) {
    derivedStateOf {
      walletState.cards.sortedBy { card ->
        val index = walletState.cards.indexOf(card)
        if (card.isDragging) Int.MAX_VALUE else walletState.cards.lastIndex - walletState.orderSlot(index)
      }
    }
  }

  Box(
    modifier = modifier,
    contentAlignment = Alignment.TopCenter,
  ) {
    orderedCards.forEach { card ->
      val cardIndex = walletState.cards.indexOf(card)
      val forwardSlot = walletState.orderSlot(cardIndex)
      val cardZIndex = when (walletState.mode) {
        WalletMode.Browsing,
        WalletMode.Details -> if (cardIndex == walletState.selectedIndex) {
          walletState.cards.size + 2f
        } else {
          (walletState.cards.size - forwardSlot).toFloat()
        }
      }
      PhysicalCreditCard(
        card = card,
        cardIndex = cardIndex,
        gesturesEnabled = cardIndex == walletState.cardOrder.first(),
        zIndex = if (card.isDragging) 100f else cardZIndex,
        controller = controller,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        metrics = metrics,
      )
    }
  }
}

@Composable
private fun PhysicalCreditCard(
  card: PhysicalCardState,
  cardIndex: Int,
  gesturesEnabled: Boolean,
  zIndex: Float,
  controller: WalletInteractionController,
  cardWidth: Dp,
  cardHeight: Dp,
  metrics: WalletLayoutMetrics,
) {
  var velocityTracker by remember { mutableStateOf(VelocityTracker()) }
  var lastPosition by remember { mutableStateOf(Offset.Zero) }
  val glowAlpha by animateFloatAsState(
    targetValue = if (card.isSelected) 0.22f else 0.06f,
    animationSpec = tween(260),
    label = "cardGlow",
  )

  Box(
    modifier = Modifier
      .offset {
        IntOffset(
          x = card.x.value.roundToInt(),
          y = card.y.value.roundToInt(),
        )
      }
      .zIndex(zIndex)
      .graphicsLayer {
        scaleX = card.scale.value
        scaleY = card.scale.value
        rotationZ = card.rotationZ.value
        rotationX = card.rotationX.value
        rotationY = card.rotationY.value + card.flipRotation.value
        alpha = card.alpha.value
        cameraDistance = 18f * density
        shadowElevation = card.elevation.value
        shape = RoundedCornerShape(28.dp)
        clip = true
      }
      .size(cardWidth, cardHeight)
      .clip(RoundedCornerShape(28.dp))
      .background(Brush.linearGradient(card.model.gradient))
      .then(
        if (gesturesEnabled) {
          Modifier.pointerInput(card.model.id, gesturesEnabled) {
        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          controller.prepareTouch(cardIndex)
          velocityTracker = VelocityTracker().also {
            it.addPosition(down.uptimeMillis, down.position)
          }
          lastPosition = down.position

          var dragged = false
          var pointerId = down.id

          while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) break

            val dragAmount = change.positionChange()
            if (dragAmount != Offset.Zero) {
              if (!dragged) {
                dragged = true
                controller.beginDrag(cardIndex)
              }
              velocityTracker.addPosition(change.uptimeMillis, change.position)
              val frameVelocity = (change.position - lastPosition) * 60f
              lastPosition = change.position
              controller.drag(cardIndex, dragAmount, frameVelocity, metrics)
              change.consume()
            }
          }

          if (dragged) {
            val velocity = velocityTracker.calculateVelocity()
            controller.endDrag(cardIndex, metrics, Offset(velocity.x, velocity.y))
          } else {
            controller.handleCardTap(cardIndex, metrics)
          }
        }
      }
        } else {
          Modifier
        },
      ),
  ) {
    if (abs(card.flipRotation.value) <= 90f) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = Color.White.copy(alpha = glowAlpha),
        radius = size.maxDimension * 0.55f,
        center = Offset(size.width * 0.92f, size.height * 0.08f),
      )
      drawCircle(
        color = Color.Black.copy(alpha = 0.18f),
        radius = size.maxDimension * 0.34f,
        center = Offset(size.width * 0.04f, size.height * 1.04f),
      )
      drawRoundRect(
        brush = Brush.verticalGradient(
          listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
        ),
      )
      val lineColor = Color.White.copy(alpha = 0.055f)
      repeat(9) { line ->
        drawLine(
          color = lineColor,
          start = Offset(0f, line * size.height / 8f),
          end = Offset(size.width, line * size.height / 8f + size.width * 0.18f),
          strokeWidth = 1f,
        )
      }
    }

    Canvas(
      modifier = Modifier
        .padding(start = 24.dp, top = 70.dp)
        .size(42.dp, 32.dp),
    ) {
      drawRoundRect(
        brush = Brush.linearGradient(listOf(Color(0xFFE8D8A4), Color(0xFF9D7F35))),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx(), 7.dp.toPx()),
      )
      drawLine(Color.Black.copy(alpha = 0.22f), Offset(size.width * 0.5f, 0f), Offset(size.width * 0.5f, size.height), 1.3f)
      drawLine(Color.Black.copy(alpha = 0.18f), Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), 1.3f)
    }

    Text(
      text = card.model.bank,
      color = Color.White.copy(alpha = 0.90f),
      fontSize = 15.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.sp,
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 24.dp, top = 23.dp),
    )
    Text(
      text = card.model.balance,
      color = Color.White,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.sp,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(end = 24.dp, top = 22.dp),
    )
    Text(
      text = card.model.maskedNumber,
      color = Color.White.copy(alpha = 0.88f),
      fontSize = 23.sp,
      fontWeight = FontWeight.Medium,
      letterSpacing = 0.sp,
      modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(start = 24.dp, top = 42.dp),
    )
    Text(
      text = card.model.holder,
      color = Color.White.copy(alpha = 0.76f),
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.sp,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 24.dp, bottom = 24.dp),
    )
    Text(
      text = card.model.badge,
      color = Color.White.copy(alpha = 0.82f),
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.sp,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 24.dp, bottom = 24.dp),
    )
    } else {
      CardBackFace(
        card = card.model,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            rotationY = 180f
          },
      )
    }
  }
}

@Composable
private fun CardBackFace(
  card: CreditCardModel,
  modifier: Modifier = Modifier,
) {
  val backGradient = when (card.id) {
    0 -> listOf(Color(0xFF101624), Color(0xFF24324D), Color(0xFF182A34))
    2 -> listOf(Color(0xFF08242B), Color(0xFF15576A), Color(0xFF102A47))
    else -> listOf(Color(0xFF111827), card.gradient.last(), Color(0xFF05060A))
  }

  Box(
    modifier = modifier
      .background(
        Brush.linearGradient(backGradient),
      ),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        color = Color.Black.copy(alpha = 0.34f),
        topLeft = Offset(0f, size.height * 0.18f),
        size = Size(size.width, size.height * 0.20f),
      )
      drawRoundRect(
        color = Color.White.copy(alpha = 0.16f),
        topLeft = Offset(size.width * 0.08f, size.height * 0.48f),
        size = Size(size.width * 0.57f, size.height * 0.16f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx()),
      )
      drawRoundRect(
        color = Color.White.copy(alpha = 0.76f),
        topLeft = Offset(size.width * 0.72f, size.height * 0.48f),
        size = Size(size.width * 0.22f, size.height * 0.16f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
      )
      repeat(7) { line ->
        drawLine(
          color = Color.White.copy(alpha = 0.055f),
          start = Offset(size.width * 0.10f, size.height * (0.72f + line * 0.028f)),
          end = Offset(size.width * 0.90f, size.height * (0.72f + line * 0.028f)),
          strokeWidth = 1f,
        )
      }
    }

    Text(
      text = card.bank,
      color = Color.White.copy(alpha = 0.88f),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.sp,
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 24.dp, top = 23.dp),
    )
    Box(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 26.dp, top = 24.dp)
        .size(width = 72.dp, height = 32.dp),
    ) {
      Text(
        text = "742",
        color = Color.Black.copy(alpha = 0.82f),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        modifier = Modifier.align(Alignment.Center),
      )
    }
    Text(
      text = card.holder,
      color = Color.White.copy(alpha = 0.72f),
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.sp,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 24.dp, bottom = 24.dp),
    )
    Text(
      text = "ASSINATURA AUTORIZADA",
      color = Color.White.copy(alpha = 0.42f),
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.sp,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 24.dp, bottom = 24.dp),
    )
  }
}

private val creditCards = listOf(
  CreditCardModel(
    id = 0,
    bank = "AURORA",
    holder = "ANDY RUBIN",
    maskedNumber = "**** 4829",
    balance = "$8,420",
    badge = "PLATINUM",
    gradient = listOf(Color(0xFF111827), Color(0xFF4338CA), Color(0xFF0F766E)),
    accent = Color(0xFF14B8A6),
  ),
  CreditCardModel(
    id = 1,
    bank = "NOVA",
    holder = "ANDY RUBIN",
    maskedNumber = "**** 7304",
    balance = "$2,180",
    badge = "BLACK",
    gradient = listOf(Color(0xFF050505), Color(0xFF171A20), Color(0xFF2A2F3A)),
    accent = Color(0xFF9CA3AF),
  ),
  CreditCardModel(
    id = 2,
    bank = "WISELY",
    holder = "ANDY RUBIN",
    maskedNumber = "**** 1198",
    balance = "$5,960",
    badge = "WORLD",
    gradient = listOf(Color(0xFF062A2F), Color(0xFF0891B2), Color(0xFF1D4ED8)),
    accent = Color(0xFF38BDF8),
  ),
  CreditCardModel(
    id = 3,
    bank = "EMBER",
    holder = "ANDY RUBIN",
    maskedNumber = "**** 6042",
    balance = "$1,740",
    badge = "GOLD",
    gradient = listOf(Color(0xFF2A1608), Color(0xFFB45309), Color(0xFF7C2D12)),
    accent = Color(0xFFF59E0B),
  ),
  CreditCardModel(
    id = 4,
    bank = "VECTOR",
    holder = "ANDY RUBIN",
    maskedNumber = "**** 9081",
    balance = "$12,300",
    badge = "SIGNATURE",
    gradient = listOf(Color(0xFF052E16), Color(0xFF16A34A), Color(0xFF0F172A)),
    accent = Color(0xFF22C55E),
  ),
)

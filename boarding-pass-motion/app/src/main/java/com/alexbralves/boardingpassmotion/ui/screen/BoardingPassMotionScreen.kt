package com.alexbralves.boardingpassmotion.ui.screen

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.alexbralves.boardingpassmotion.data.TicketRealtimeDataSource
import com.alexbralves.boardingpassmotion.model.BoardingPass
import com.alexbralves.boardingpassmotion.theme.Gold
import com.alexbralves.boardingpassmotion.theme.Ink
import com.alexbralves.boardingpassmotion.theme.Night
import com.alexbralves.boardingpassmotion.theme.SkyAccent
import com.alexbralves.boardingpassmotion.theme.SoftText
import com.alexbralves.boardingpassmotion.theme.Ticket
import com.alexbralves.boardingpassmotion.theme.TicketMuted
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class TicketValidationState {
  WaitingForScan,
  Scanning,
  Boarded,
}

@Composable
fun BoardingPassMotionScreen(autoExpand: Boolean = false) {
  val context = LocalContext.current
  val pass = remember { BoardingPass() }
  val realtime = remember { TicketRealtimeDataSource() }
  val secondFold = remember { Animatable(0f) }
  val thirdFold = remember { Animatable(0f) }
  val cardFlip = remember { Animatable(0f) }
  val press = remember { Animatable(0f) }
  val finishBounce = remember { Animatable(0f) }
  var open by remember { mutableStateOf(false) }
  var running by remember { mutableStateOf(false) }
  var validationState by remember { mutableStateOf(TicketValidationState.WaitingForScan) }
  var lastScanCount by remember { mutableStateOf<Int?>(null) }
  val scope = rememberCoroutineScope()

  suspend fun expandTicket() {
    running = true
    open = true
    press.animateTo(1f, tween(82, easing = FastOutSlowInEasing))
    press.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.58f))
    secondFold.animateTo(1f, slowPaperTween())
    delay(142)
    thirdFold.animateTo(1f, slowPaperTween())
    finishBounce.snapTo(1f)
    finishBounce.animateTo(0f, spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.36f))
    running = false
  }

  suspend fun foldTicket() {
    running = true
    open = false
    if (cardFlip.value != 0f) {
      cardFlip.animateTo(0f, flipTween())
    }
    press.animateTo(1f, tween(72, easing = FastOutSlowInEasing))
    press.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.62f))
    thirdFold.animateTo(0f, slowPaperTween())
    delay(105)
    secondFold.animateTo(0f, slowPaperTween())
    validationState = TicketValidationState.WaitingForScan
    lastScanCount = null
    running = false
  }

  suspend fun flipQrSide() {
    running = true
    cardFlip.animateTo(if (cardFlip.value < 0.5f) 1f else 0f, flipTween())
    running = false
  }

  LaunchedEffect(autoExpand) {
    if (autoExpand) {
      delay(900)
      expandTicket()
    }
  }

  LaunchedEffect(open, cardFlip.value > 0.98f) {
    if (!open || cardFlip.value <= 0.98f) return@LaunchedEffect

    realtime.scanCountChanges(pass.code).collect { scanCount ->
      val previous = lastScanCount
      lastScanCount = scanCount
      if (previous != null && scanCount > previous && validationState == TicketValidationState.WaitingForScan) {
        vibrateShort(context)
        validationState = TicketValidationState.Scanning
      }
    }
  }

  LaunchedEffect(validationState) {
    if (validationState == TicketValidationState.Scanning) {
      delay(700)
      validationState = TicketValidationState.Boarded
    }
  }

  LaunchedEffect(validationState) {
    if (validationState == TicketValidationState.Boarded) {
      delay(2_000)
      if (!running && open) {
        foldTicket()
      }
    }
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFF263B68), Night, Color(0xFF02040A)),
            center = Offset(260f, 100f),
            radius = 1280f,
          ),
        ),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 22.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FoldingBoardingPass(
          pass = pass,
          secondProgress = secondFold.value,
          thirdProgress = thirdFold.value,
          pressProgress = press.value,
          bounceProgress = finishBounce.value,
          flipProgress = cardFlip.value,
          validationState = validationState,
          modifier =
            Modifier.clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
            ) {
              if (!running && cardFlip.value < 0.5f) {
                scope.launch {
                  if (open) foldTicket() else expandTicket()
                }
              }
            },
        )
        if (secondFold.value > 0.98f && thirdFold.value > 0.98f) {
          Spacer(Modifier.height(18.dp))
          QrSideButton(
            showingQr = cardFlip.value > 0.5f,
            onClick = {
              if (!running) {
                scope.launch {
                  if (cardFlip.value < 0.5f) {
                    validationState = TicketValidationState.WaitingForScan
                    lastScanCount = null
                  }
                  flipQrSide()
                }
              }
            },
          )
        }
      }
    }
  }
}

@Composable
private fun FoldingBoardingPass(
  pass: BoardingPass,
  secondProgress: Float,
  thirdProgress: Float,
  pressProgress: Float,
  bounceProgress: Float,
  flipProgress: Float,
  validationState: TicketValidationState,
  modifier: Modifier = Modifier,
) {
  val panelHeight = 168.dp
  val creaseHeight = 10.dp
  val easedSecond = physicalEase(secondProgress)
  val easedThird = physicalEase(thirdProgress)
  val totalHeight = panelHeight * (1f + easedSecond + easedThird)
  val pressScale = 1f - pressProgress * 0.018f
  val bounceLift = 5.dp * bounceProgress
  val openingPeek = (secondProgress / 0.18f).coerceIn(0f, 1f)
  val finishingPeek = ((1f - thirdProgress) / 0.18f).coerceIn(0f, 1f)
  val foldPeek = minOf(openingPeek, finishingPeek)
  val corner = 22.dp
  val firstBottomCorner = corner * (1f - easedSecond)
  val middleBottomCorner = corner * (1f - easedThird)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(totalHeight)
        .offset(y = -bounceLift)
        .shadow(22.dp + pressProgress.dp * 16f, RoundedCornerShape(corner), clip = false)
        .graphicsLayer {
          scaleX = pressScale
          scaleY = pressScale
          rotationY = 180f * flipProgress
          cameraDistance = 48f * density
        },
  ) {
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (flipProgress < 0.5f) 1f else 0f }) {
      FoldedPanel(
        progress = 1f,
        foldedAngle = 0f,
        modifier =
          Modifier
            .zIndex(30f)
            .fillMaxWidth()
            .height(panelHeight),
        shape =
          RoundedCornerShape(
            topStart = corner,
            topEnd = corner,
            bottomStart = firstBottomCorner,
            bottomEnd = firstBottomCorner,
          ),
      ) {
        FirstPanel(pass = pass)
      }

      CreaseShadow(
        progress = easedSecond,
        modifier =
          Modifier
            .zIndex(29f)
            .offset(y = panelHeight - creaseHeight / 2)
            .height(creaseHeight),
      )

      FoldedPanel(
        progress = easedSecond,
        foldedAngle = -176f,
        modifier =
          Modifier
            .zIndex(20f)
            .offset(y = panelHeight)
            .fillMaxWidth()
            .height(panelHeight),
        shape =
          RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = middleBottomCorner,
            bottomEnd = middleBottomCorner,
          ),
      ) {
        SecondPanel(pass = pass, progress = easedSecond)
      }

      CreaseShadow(
        progress = easedThird,
        modifier =
          Modifier
            .zIndex(19f)
            .offset(y = panelHeight + panelHeight * easedSecond - creaseHeight / 2)
            .height(creaseHeight),
      )

      FoldedPanel(
        progress = easedThird,
        foldedAngle = -176f,
        modifier =
          Modifier
            .zIndex(10f)
            .offset(y = panelHeight + panelHeight * easedSecond)
            .fillMaxWidth()
            .height(panelHeight),
        shape =
          RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = corner,
            bottomEnd = corner,
          ),
      ) {
        ThirdPanel(pass = pass, progress = easedThird)
      }
    }
    BoardingPassBackSide(
      pass = pass,
      validationState = validationState,
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            rotationY = 180f
            alpha = if (flipProgress >= 0.5f) 1f else 0f
          },
      corner = corner,
    )
  }
}

@Composable
private fun QrSideButton(showingQr: Boolean, onClick: () -> Unit) {
  Box(
    modifier =
      Modifier
        .clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.10f))
        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onClick,
        )
        .padding(horizontal = 18.dp, vertical = 11.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = if (showingQr) "Ver ticket" else "Ver QR Code",
      style = MaterialTheme.typography.labelLarge,
      color = Color.White,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun BoardingPassBackSide(
  pass: BoardingPass,
  validationState: TicketValidationState,
  modifier: Modifier = Modifier,
  corner: androidx.compose.ui.unit.Dp,
) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(corner))
        .background(Color.White),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 26.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        PlainTicketField("Departure", pass.time)
        PlainTicketField("Arrival", "13:00", alignment = Alignment.End)
      }
      PlainTicketField("Your Plane", "Jetpack Compose 7896 ZX", alignment = Alignment.CenterHorizontally)
      Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
          color = Color(0xFFB6BDC8),
          start = Offset(0f, 0f),
          end = Offset(size.width, 0f),
          strokeWidth = 1.2f,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        )
      }
      ValidationStatus(state = validationState)
      Box(contentAlignment = Alignment.Center) {
        AnimatedQRCode(content = pass.code, modifier = Modifier.size(190.dp))
        if (validationState == TicketValidationState.Scanning) {
          QrScanBand(Modifier.size(190.dp))
        }
      }
    }
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(82.dp)
          .background(Color(0xFF0759B4))
          .padding(horizontal = 28.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text("PASSENGER", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
        Text(pass.passengerName.uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Black)
      }
      Column(horizontalAlignment = Alignment.End) {
        Text("SEAT", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
        Text(pass.seat, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Black)
      }
    }
  }
}

@Composable
private fun ValidationStatus(state: TicketValidationState) {
  val label =
    when (state) {
      TicketValidationState.WaitingForScan -> "Waiting for validation..."
      TicketValidationState.Scanning -> "Reading boarding pass..."
      TicketValidationState.Boarded -> "Boarding Approved"
    }
  val approved = state == TicketValidationState.Boarded
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ValidationIcon(state)
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (approved) Color(0xFF0B8F5A) else Ink,
      fontWeight = FontWeight.Black,
    )
  }
}

@Composable
private fun ValidationIcon(state: TicketValidationState) {
  val color =
    when (state) {
      TicketValidationState.WaitingForScan -> Color(0xFF0759B4)
      TicketValidationState.Scanning -> Gold
      TicketValidationState.Boarded -> Color(0xFF0B8F5A)
    }
  Canvas(
    modifier =
      Modifier
        .size(22.dp)
        .clip(CircleShape)
        .background(color.copy(alpha = 0.12f))
        .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
        .padding(5.dp),
  ) {
    when (state) {
      TicketValidationState.WaitingForScan -> drawCircle(color, radius = size.minDimension * 0.26f)
      TicketValidationState.Scanning -> {
        drawLine(color, Offset(size.width * 0.15f, size.height * 0.5f), Offset(size.width * 0.85f, size.height * 0.5f), strokeWidth = 2.4f)
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.15f), Offset(size.width * 0.5f, size.height * 0.85f), strokeWidth = 2.4f)
      }
      TicketValidationState.Boarded -> {
        drawLine(color, Offset(size.width * 0.12f, size.height * 0.54f), Offset(size.width * 0.38f, size.height * 0.78f), strokeWidth = 3.4f)
        drawLine(color, Offset(size.width * 0.38f, size.height * 0.78f), Offset(size.width * 0.88f, size.height * 0.22f), strokeWidth = 3.4f)
      }
    }
  }
}

@Composable
private fun QrScanBand(modifier: Modifier = Modifier) {
  val progress = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    progress.snapTo(0f)
    progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
  }
  Canvas(
    modifier =
      modifier
        .clip(RoundedCornerShape(16.dp))
        .alpha(if (progress.value < 1f) 1f else 0f),
  ) {
    val bandHeight = size.height * 0.24f
    val y = (size.height + bandHeight) * progress.value - bandHeight
    drawRect(
      brush =
        Brush.verticalGradient(
          colors =
            listOf(
              Color.Transparent,
              SkyAccent.copy(alpha = 0.18f),
              Color.White.copy(alpha = 0.62f),
              SkyAccent.copy(alpha = 0.18f),
              Color.Transparent,
            ),
          startY = y,
          endY = y + bandHeight,
        ),
      topLeft = Offset(0f, y),
      size = Size(size.width, bandHeight),
    )
  }
}

@Composable
private fun FoldedPanel(
  progress: Float,
  foldedAngle: Float,
  modifier: Modifier,
  shape: RoundedCornerShape,
  content: @Composable () -> Unit,
) {
  val faceProgress = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
  val backProgress = (1f - faceProgress).coerceIn(0f, 1f)
  val panelAlpha =
    if (foldedAngle == 0f) {
      1f
    } else {
      ((progress - 0.04f) / 0.16f).coerceIn(0f, 1f)
    }
  Box(
    modifier =
      modifier
        .graphicsLayer {
          transformOrigin = TransformOrigin(0.5f, 0f)
          rotationX = foldedAngle * (1f - progress)
          translationY = (1f - progress) * -5f * density
          alpha = panelAlpha
          cameraDistance = 28f * density
        }
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color.White, Ticket, Color(0xFFEAF0FA))))
        .border(1.dp, Color.White.copy(alpha = 0.72f), shape),
  ) {
    Box(Modifier.graphicsLayer { alpha = faceProgress }) {
      content()
    }
    FoldBackFace(backProgress)
    FoldSurfaceLight(progress)
  }
}

@Composable
private fun FoldBackFace(progress: Float) {
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .graphicsLayer { alpha = progress }
        .background(
          Brush.verticalGradient(
            colors =
              listOf(
                Color(0xFFE4EAF4),
                Color(0xFFF7F9FC),
                Color(0xFFD6DFEC),
              ),
          ),
        ),
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val y = size.height * 0.55f
      drawLine(
        color = Color(0xFFB7C3D5).copy(alpha = 0.42f),
        start = Offset(size.width * 0.08f, y),
        end = Offset(size.width * 0.92f, y),
        strokeWidth = 1.3f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
      )
    }
  }
}

@Composable
private fun FoldSurfaceLight(progress: Float) {
  Canvas(Modifier.fillMaxSize()) {
    val shadowProgress = ((progress - 0.08f) / 0.24f).coerceIn(0f, 1f)
    val foldShadowHeight = size.height * 0.18f
    drawRect(
      brush =
        Brush.verticalGradient(
          colors =
            listOf(
              Color.Black.copy(alpha = (0.18f * (1f - progress) * shadowProgress).coerceIn(0f, 0.18f)),
              Color.Transparent,
            ),
          startY = 0f,
          endY = foldShadowHeight,
        ),
      size = Size(size.width, foldShadowHeight),
    )
    drawRect(
      brush =
        Brush.verticalGradient(
          colors =
            listOf(
              Color.Transparent,
              Color.White.copy(alpha = (0.10f * progress).coerceIn(0f, 0.10f)),
            ),
          startY = size.height * 0.62f,
          endY = size.height,
        ),
      topLeft = Offset(0f, size.height * 0.62f),
      size = Size(size.width, size.height * 0.38f),
    )
  }
}

@Composable
private fun CreaseShadow(progress: Float, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.fillMaxWidth()) {
    val shadowProgress = ((progress - 0.05f) / 0.2f).coerceIn(0f, 1f)
    if (shadowProgress <= 0f) return@Canvas
    val y = size.height / 2f
    val closed = 1f - progress
    drawRect(
      brush =
        Brush.verticalGradient(
          colors =
            listOf(
              Color.Transparent,
              Color.Black.copy(alpha = (0.24f * closed + 0.08f) * shadowProgress),
              Color.Transparent,
            ),
        ),
      size = size,
    )
    drawLine(
      color = Color(0xFFBCC7D8).copy(alpha = (0.26f + progress * 0.32f) * shadowProgress),
      start = Offset(0f, y),
      end = Offset(size.width, y),
      strokeWidth = 1.4f,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
    )
  }
}

@Composable
private fun FirstPanel(pass: BoardingPass) {
  BoardingPassFrontHero(pass = pass, modifier = Modifier.fillMaxSize())
}

@Composable
private fun BoardingPassFrontHero(pass: BoardingPass, modifier: Modifier = Modifier) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(Brush.verticalGradient(listOf(Color(0xFF0870D8), Color(0xFF005EBB))))
        .padding(horizontal = 22.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = pass.airline.uppercase(),
      style = MaterialTheme.typography.labelLarge,
      color = Color.White,
      fontWeight = FontWeight.Black,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    HeroRouteGraphic(Modifier.fillMaxWidth().height(58.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      HeroAirport(pass.originCode, pass.originCity, pass.time, Alignment.Start)
      HeroAirport(pass.destinationCode, pass.destinationCity, "11:30 AM", Alignment.End)
    }
  }
}

@Composable
private fun HeroAirport(code: String, city: String, time: String, alignment: Alignment.Horizontal) {
  Column(horizontalAlignment = alignment) {
    Text(code, style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black)
    Text(city.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
    Text(time, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
  }
}

@Composable
private fun HeroRouteGraphic(modifier: Modifier = Modifier) {
  Canvas(modifier) {
    val start = Offset(size.width * 0.18f, size.height * 0.82f)
    val end = Offset(size.width * 0.82f, size.height * 0.82f)
    val control = Offset(size.width * 0.5f, size.height * 0.02f)
    val path = androidx.compose.ui.graphics.Path().apply {
      moveTo(start.x, start.y)
      quadraticTo(control.x, control.y, end.x, end.y)
    }
    drawPath(
      path = path,
      color = Color.White.copy(alpha = 0.72f),
      style = Stroke(width = 1.4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f))),
    )
    drawCircle(Color.White, radius = 5.2f, center = start)
    drawCircle(Color.White, radius = 5.2f, center = end)
    val planeCenter = Offset(size.width * 0.47f, size.height * 0.35f)
    drawLine(Color.White, Offset(planeCenter.x - 24f, planeCenter.y + 4f), Offset(planeCenter.x + 24f, planeCenter.y - 10f), strokeWidth = 8f)
    drawLine(Color.White, Offset(planeCenter.x - 4f, planeCenter.y - 2f), Offset(planeCenter.x - 24f, planeCenter.y - 18f), strokeWidth = 6f)
    drawLine(Color.White, Offset(planeCenter.x + 4f, planeCenter.y - 4f), Offset(planeCenter.x - 4f, planeCenter.y + 18f), strokeWidth = 5f)
  }
}

@Composable
private fun SecondPanel(pass: BoardingPass, progress: Float) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .graphicsLayer { alpha = revealContent(progress) }
        .background(Color.White)
        .padding(horizontal = 22.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      PlainTicketField("Flight", pass.flightNumber, highlight = true)
      PlainTicketField("Date", pass.date, alignment = Alignment.End)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      PlainTicketField("Gate", pass.gate)
      PlainTicketField("Boarding", pass.boardingTime, alignment = Alignment.End)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      PlainTicketField("Class", pass.cabin.uppercase())
      PlainTicketField("Flight Time", pass.flightDuration, alignment = Alignment.End)
    }
  }
}

@Composable
private fun PlainTicketField(
  label: String,
  value: String,
  alignment: Alignment.Horizontal = Alignment.Start,
  highlight: Boolean = false,
) {
  Column(horizontalAlignment = alignment) {
    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = SoftText)
    Text(
      value,
      style = MaterialTheme.typography.labelLarge,
      color = if (highlight) Color(0xFF0759B4) else Ink,
      fontWeight = FontWeight.Black,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ThirdPanel(pass: BoardingPass, progress: Float) {
  Row(
    modifier =
      Modifier
        .fillMaxSize()
        .graphicsLayer { alpha = revealContent(progress) }
        .background(Color.White)
        .padding(horizontal = 22.dp, vertical = 20.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      PlainTicketField("Passenger", pass.passengerName.uppercase(), highlight = true)
      PlainTicketField("Seat", pass.seat, highlight = true)
    }
    MiniQrCode(content = pass.code, modifier = Modifier.size(86.dp))
  }
}

@Composable
private fun TicketHeader(pass: BoardingPass) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(SkyAccent, Color(0xFF5EC7FF)))),
        contentAlignment = Alignment.Center,
      ) {
        Text("A", color = Color.White, fontWeight = FontWeight.Black)
      }
      Spacer(Modifier.width(10.dp))
      Column {
        Text(pass.airline, style = MaterialTheme.typography.titleMedium, color = Ink)
        Text(pass.cabin, style = MaterialTheme.typography.labelSmall, color = Gold)
      }
    }
    StatusChip(pass.boardingStatus)
  }
}

@Composable
private fun AirportCode(code: String, city: String, alignment: Alignment.Horizontal) {
  Column(horizontalAlignment = alignment) {
    Text(code, style = MaterialTheme.typography.displaySmall, color = Ink, maxLines = 1)
    Text(city, style = MaterialTheme.typography.labelLarge, color = SoftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun RouteLine(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.height(34.dp)) {
    val y = size.height / 2
    drawLine(
      color = TicketMuted,
      start = Offset(0f, y),
      end = Offset(size.width, y),
      strokeWidth = 3f,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
    )
    drawCircle(SkyAccent, radius = 5.5f, center = Offset(size.width * 0.5f, y))
  }
}

@Composable
private fun LabeledValue(label: String, value: String, large: Boolean = false) {
  Column {
    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = SoftText)
    Text(
      value,
      style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
      color = Ink,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun InfoPill(label: String, value: String, modifier: Modifier = Modifier) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White.copy(alpha = 0.72f))
        .border(1.dp, TicketMuted, RoundedCornerShape(16.dp))
        .padding(horizontal = 12.dp, vertical = 9.dp),
  ) {
    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = SoftText)
    Text(value, style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun StatusChip(text: String, modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(999.dp))
        .background(SkyAccent.copy(alpha = 0.12f))
        .border(1.dp, SkyAccent.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
        .padding(horizontal = 12.dp, vertical = 7.dp),
  ) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = SkyAccent)
  }
}

@Composable
private fun MiniQrCode(content: String, modifier: Modifier = Modifier) {
  RealQrCode(
    content = content,
    modifier =
      modifier
        .clip(RoundedCornerShape(12.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFDDE4EF), RoundedCornerShape(12.dp))
        .padding(7.dp),
  )
}

@Composable
private fun AnimatedQRCode(content: String, modifier: Modifier = Modifier) {
  val progress = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    progress.snapTo(0f)
    progress.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
  }
  RealQrCode(
    content = content,
    modifier =
      modifier
        .aspectRatio(1f)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFDDE4EF), RoundedCornerShape(16.dp))
        .padding(10.dp)
        .graphicsLayer {
          alpha = progress.value
          scaleX = 0.92f + progress.value * 0.08f
          scaleY = 0.92f + progress.value * 0.08f
        },
    visibleCells = progress.value,
  )
}

@Composable
private fun RealQrCode(content: String, modifier: Modifier, visibleCells: Float = 1f) {
  val matrix = remember(content) { QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 29, 29) }
  Canvas(modifier = modifier) {
    val dimension = matrix.width
    val gap = size.width / dimension
    for (row in 0 until dimension) {
      for (col in 0 until dimension) {
        if (matrix[col, row] && (row * dimension + col) < (dimension * dimension * visibleCells).roundToInt()) {
          drawRoundRect(
            color = Ink,
            topLeft = Offset(col * gap, row * gap),
            size = Size(gap, gap),
            cornerRadius = CornerRadius(1.6f),
          )
        }
      }
    }
  }
}

@Composable
private fun AnimatedBarcode(modifier: Modifier = Modifier) {
  val progress = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    progress.snapTo(0f)
    progress.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
  }
  Canvas(
    modifier =
      modifier
        .clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(alpha = 0.78f))
        .drawBehind {
          drawRoundRect(
            color = TicketMuted.copy(alpha = 0.8f),
            style = Stroke(width = 1.dp.toPx()),
            cornerRadius = CornerRadius(14.dp.toPx()),
          )
        }
        .padding(10.dp),
  ) {
    var x = 0f
    var index = 0
    while (x < size.width * progress.value) {
      val width = (3 + (index * 7 % 13)).toFloat()
      val height = size.height * (0.52f + ((index * 5) % 6) * 0.07f)
      drawRoundRect(
        color = Ink.copy(alpha = 0.86f),
        topLeft = Offset(x, (size.height - height) / 2),
        size = Size(width, height),
        cornerRadius = CornerRadius(2f),
      )
      x += width + 5f + (index % 3)
      index++
    }
  }
}

private fun paperSpring() =
  spring<Float>(
    dampingRatio = 0.72f,
    stiffness = Spring.StiffnessLow,
    visibilityThreshold = 0.002f,
  )

private fun slowPaperTween() = tween<Float>(durationMillis = 474, easing = FastOutSlowInEasing)

private fun flipTween() = tween<Float>(durationMillis = 620, easing = FastOutSlowInEasing)

private fun physicalEase(value: Float): Float {
  val clamped = value.coerceIn(0f, 1f)
  return clamped * clamped * (3f - 2f * clamped)
}

private fun revealContent(progress: Float): Float = ((progress - 0.42f) / 0.58f).coerceIn(0f, 1f)

private fun vibrateShort(context: Context) {
  val vibrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
      manager.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    vibrator.vibrate(VibrationEffect.createOneShot(42, VibrationEffect.DEFAULT_AMPLITUDE))
  } else {
    @Suppress("DEPRECATION")
    vibrator.vibrate(42)
  }
}

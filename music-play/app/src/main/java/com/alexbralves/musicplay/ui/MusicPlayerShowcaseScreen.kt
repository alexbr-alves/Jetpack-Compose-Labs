package com.alexbralves.musicplay.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.alexbralves.musicplay.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val EasePremium = CubicBezierEasing(0.22f, 0.75f, 0.18f, 1f)
private const val AlbumSwapDurationMillis = 1800
private const val AlbumTextCommitDelayMillis = 900
private val OuterBlue = Color(0xFFFFF4B8)
private val OuterSteel = Color(0xFFFFD84D)
private val PanelWhite = Color(0xFFFFFBE8)
private val SheetTop = Color(0xFFFFE85C)
private val SheetBottom = Color(0xFFFFC928)
private val Sky = Color(0xFFFFD400)
private val Ink = Color(0xFF293241)
private val Muted = Color(0xFF7B8794)
private val SoftLine = Color(0xBFFFFFFF)

private enum class ShowcaseState {
  Player,
  TrackList,
  SelectingTrack,
  TrackChanged,
  AlbumChanged,
  PlaylistPicker,
  PlaylistDone,
  PlaylistsOverview,
}

@Stable
private data class Track(
  val title: String,
  val artist: String,
  val duration: String,
  val durationSeconds: Int,
)

@Stable
private data class AlbumUi(
  val title: String,
  val subtitle: String,
  val artist: String,
  val colors: List<Color>,
  val waveform: Color,
  val coverRes: Int?,
  val discRes: Int?,
)

@Stable
private data class PlaylistUi(
  val title: String,
  val artist: String,
  val colors: List<Color>,
  val coverRes: Int? = null,
  val discRes: Int? = null,
  val tracks: List<Track>,
)

private fun track(
  artist: String,
  title: String,
  duration: String,
): Track {
  val parts = duration.split(":")
  val seconds = parts.getOrNull(0).orEmpty().toIntOrNull().orZero() * 60 + parts.getOrNull(1).orEmpty().toIntOrNull().orZero()
  return Track(title, artist, duration, seconds)
}

private fun Int?.orZero(): Int = this ?: 0

private val jamiroquaiTracks =
  listOf(
    Track("Virtual Insanity", "Jamiroquai", "5:40", 340),
    Track("Cosmic Girl", "Jamiroquai", "4:03", 243),
    Track("Use the Force", "Jamiroquai", "4:00", 240),
    Track("Everyday", "Jamiroquai", "4:28", 268),
    Track("Alright", "Jamiroquai", "4:23", 263),
    Track("High Times", "Jamiroquai", "5:58", 358),
    Track("Drifting Along", "Jamiroquai", "4:06", 246),
    Track("Didjeritama", "Jamiroquai", "3:50", 230),
    Track("Travelling Without Moving", "Jamiroquai", "3:40", 220),
    Track("You Are My Love", "Jamiroquai", "3:56", 236),
    Track("Spend a Lifetime", "Jamiroquai", "4:14", 254),
    Track("Funktion (faixa bônus)", "Jamiroquai", "8:27", 507),
  )

private val playlists =
  listOf(
    PlaylistUi("Virtual Insanity", "Jamiroquai", listOf(Color(0xFFFFE100), Color(0xFFFFD400), Color(0xFF111111)), R.drawable.jamiroquai_album_cover, R.drawable.jamiroquai_vinyl, jamiroquaiTracks),
    PlaylistUi(
      "Dark Side",
      "Pink Floyd",
      listOf(Color(0xFF020617), Color(0xFF111827), Color(0xFFE11D48)),
      R.drawable.pink_floyd_dark_side_cover,
      tracks =
        listOf(
          track("Pink Floyd", "Speak to Me", "1:30"),
          track("Pink Floyd", "Breathe (In the Air)", "2:43"),
          track("Pink Floyd", "On the Run", "3:36"),
          track("Pink Floyd", "Time", "6:53"),
          track("Pink Floyd", "The Great Gig in the Sky", "4:44"),
          track("Pink Floyd", "Money", "6:22"),
          track("Pink Floyd", "Us and Them", "7:49"),
          track("Pink Floyd", "Any Colour You Like", "3:26"),
          track("Pink Floyd", "Brain Damage", "3:46"),
          track("Pink Floyd", "Eclipse", "2:12"),
        ),
    ),
    PlaylistUi(
      "Nevermind",
      "Nirvana",
      listOf(Color(0xFF0284C7), Color(0xFF0EA5E9), Color(0xFF172554)),
      R.drawable.nirvana_nevermind_cover,
      tracks =
        listOf(
          track("Nirvana", "Smells Like Teen Spirit", "5:01"),
          track("Nirvana", "In Bloom", "4:14"),
          track("Nirvana", "Come as You Are", "3:39"),
          track("Nirvana", "Breed", "3:03"),
          track("Nirvana", "Lithium", "4:17"),
          track("Nirvana", "Polly", "2:57"),
          track("Nirvana", "Territorial Pissings", "2:23"),
          track("Nirvana", "Drain You", "3:43"),
          track("Nirvana", "Lounge Act", "2:36"),
          track("Nirvana", "Stay Away", "3:32"),
          track("Nirvana", "On a Plain", "3:16"),
          track("Nirvana", "Something in the Way", "3:52"),
          track("Nirvana", "Endless, Nameless", "6:44"),
        ),
    ),
    PlaylistUi(
      "Dirt",
      "Alice in Chains",
      listOf(Color(0xFF7C2D12), Color(0xFFF97316), Color(0xFF1C1917)),
      R.drawable.alice_in_chains_dirt_cover,
      tracks =
        listOf(
          track("Alice in Chains", "Them Bones", "2:30"),
          track("Alice in Chains", "Dam That River", "3:09"),
          track("Alice in Chains", "Rain When I Die", "6:01"),
          track("Alice in Chains", "Down in a Hole", "5:38"),
          track("Alice in Chains", "Sickman", "5:29"),
          track("Alice in Chains", "Rooster", "6:15"),
          track("Alice in Chains", "Junkhead", "5:09"),
          track("Alice in Chains", "Dirt", "5:16"),
          track("Alice in Chains", "God Smack", "3:51"),
          track("Alice in Chains", "Hate to Feel", "5:16"),
          track("Alice in Chains", "Angry Chair", "4:47"),
          track("Alice in Chains", "Would?", "3:28"),
        ),
    ),
    PlaylistUi(
      "Rust in Peace",
      "Megadeth",
      listOf(Color(0xFF1D4ED8), Color(0xFF2563EB), Color(0xFFFACC15)),
      R.drawable.megadeth_rust_in_peace_cover,
      tracks =
        listOf(
          track("Megadeth", "Holy Wars... The Punishment Due", "6:36"),
          track("Megadeth", "Hangar 18", "5:14"),
          track("Megadeth", "Take No Prisoners", "3:28"),
          track("Megadeth", "Five Magics", "5:42"),
          track("Megadeth", "Poison Was the Cure", "2:58"),
          track("Megadeth", "Lucretia", "3:58"),
          track("Megadeth", "Tornado of Souls", "5:19"),
          track("Megadeth", "Dawn Patrol", "1:51"),
          track("Megadeth", "Rust in Peace... Polaris", "5:36"),
        ),
    ),
    PlaylistUi(
      "Abbey Road",
      "The Beatles",
      listOf(Color(0xFF0F766E), Color(0xFF94A3B8), Color(0xFFF8FAFC)),
      R.drawable.the_beatles_abbey_road_cover,
      tracks =
        listOf(
          track("The Beatles", "Come Together", "4:20"),
          track("The Beatles", "Something", "3:03"),
          track("The Beatles", "Maxwell's Silver Hammer", "3:27"),
          track("The Beatles", "Oh! Darling", "3:27"),
          track("The Beatles", "Octopus's Garden", "2:51"),
          track("The Beatles", "I Want You (She's So Heavy)", "7:47"),
          track("The Beatles", "Here Comes the Sun", "3:05"),
          track("The Beatles", "Because", "2:45"),
          track("The Beatles", "You Never Give Me Your Money", "4:02"),
          track("The Beatles", "Sun King", "2:26"),
          track("The Beatles", "Mean Mr. Mustard", "1:06"),
          track("The Beatles", "Polythene Pam", "1:12"),
          track("The Beatles", "She Came In Through the Bathroom Window", "1:57"),
          track("The Beatles", "Golden Slumbers", "1:31"),
          track("The Beatles", "Carry That Weight", "1:36"),
          track("The Beatles", "The End", "2:20"),
          track("The Beatles", "Her Majesty", "0:23"),
        ),
    ),
    PlaylistUi(
      "Master of Puppets",
      "Metallica",
      listOf(Color(0xFF450A0A), Color(0xFF991B1B), Color(0xFFE5E7EB)),
      R.drawable.metallica_master_of_puppets_cover,
      tracks =
        listOf(
          track("Metallica", "Battery", "5:12"),
          track("Metallica", "Master of Puppets", "8:36"),
          track("Metallica", "The Thing That Should Not Be", "6:37"),
          track("Metallica", "Welcome Home (Sanitarium)", "6:28"),
          track("Metallica", "Disposable Heroes", "8:17"),
          track("Metallica", "Leper Messiah", "5:40"),
          track("Metallica", "Orion", "8:27"),
          track("Metallica", "Damage, Inc.", "5:32"),
        ),
    ),
    PlaylistUi(
      "Back in Black",
      "AC/DC",
      listOf(Color(0xFF020617), Color(0xFF111827), Color(0xFFF8FAFC)),
      R.drawable.acdc_back_in_black_cover,
      tracks =
        listOf(
          track("AC/DC", "Hells Bells", "5:12"),
          track("AC/DC", "Shoot to Thrill", "5:17"),
          track("AC/DC", "What Do You Do for Money Honey", "3:33"),
          track("AC/DC", "Givin the Dog a Bone", "3:30"),
          track("AC/DC", "Let Me Put My Love into You", "4:15"),
          track("AC/DC", "Back in Black", "4:15"),
          track("AC/DC", "You Shook Me All Night Long", "3:30"),
          track("AC/DC", "Have a Drink on Me", "3:58"),
          track("AC/DC", "Shake a Leg", "4:06"),
          track("AC/DC", "Rock and Roll Ain't Noise Pollution", "4:15"),
        ),
    ),
  )

private fun PlaylistUi.toAlbumUi(): AlbumUi =
  AlbumUi(
    title = title,
    subtitle = "$artist - $title",
    artist = artist,
    colors = colors,
    waveform = colors.getOrElse(1) { Color(0xFFFFD400) },
    coverRes = coverRes,
    discRes = discRes,
  )

@Composable
fun MusicPlayerShowcaseScreen(
  modifier: Modifier = Modifier,
  autoPlay: Boolean = false,
) {
  var state by remember { mutableStateOf(ShowcaseState.Player) }
  var currentTrackIndex by remember { mutableStateOf(0) }
  var progressSeconds by remember { mutableStateOf(12) }
  var isPlaying by remember { mutableStateOf(true) }
  var selectedTrackIndex by remember { mutableStateOf<Int?>(null) }
  var activePlaylistIndex by remember { mutableStateOf(0) }
  var pendingPlaylistIndex by remember { mutableStateOf<Int?>(null) }
  var outgoingAlbum by remember { mutableStateOf<AlbumUi?>(null) }
  var discTransitionKey by remember { mutableStateOf(0) }
  val albumSwapProgress = remember { Animatable(0f) }
  val activeTracks = playlists[activePlaylistIndex].tracks
  val currentTrack = activeTracks[currentTrackIndex.coerceIn(activeTracks.indices)]
  val album = playlists[activePlaylistIndex].toAlbumUi()
  val pendingAlbum = pendingPlaylistIndex?.let { playlists[it].toAlbumUi() }
  val environmentTop by animateColorAsState(
    targetValue = album.colors[0].copy(alpha = 0.44f),
    animationSpec = tween(620, easing = EasePremium),
    label = "environmentTop",
  )
  val environmentMiddle by animateColorAsState(
    targetValue = album.colors[1].copy(alpha = 0.70f),
    animationSpec = tween(620, easing = EasePremium),
    label = "environmentMiddle",
  )
  val environmentBottom by animateColorAsState(
    targetValue = album.colors[2].copy(alpha = 0.74f),
    animationSpec = tween(620, easing = EasePremium),
    label = "environmentBottom",
  )
  val statusBarGradientTop by animateColorAsState(
    targetValue = statusBarGradientColor(album),
    animationSpec = tween(620, easing = EasePremium),
    label = "statusBarGradientTop",
  )

  fun selectTrack(index: Int) {
    currentTrackIndex = index.coerceIn(activeTracks.indices)
    progressSeconds = 0
    isPlaying = true
    discTransitionKey += 1
  }

  fun nextTrack() {
    selectTrack((currentTrackIndex + 1) % activeTracks.size)
  }

  fun previousTrack() {
    selectTrack(if (currentTrackIndex == 0) activeTracks.lastIndex else currentTrackIndex - 1)
  }

  fun selectPlaylist(index: Int) {
    if (pendingPlaylistIndex != null) return
    val safeIndex = index.coerceIn(playlists.indices)
    if (safeIndex == activePlaylistIndex) {
      pendingPlaylistIndex = null
      outgoingAlbum = null
      state = ShowcaseState.Player
      return
    }
    outgoingAlbum = album
    pendingPlaylistIndex = safeIndex
    state = ShowcaseState.Player
  }

  LaunchedEffect(autoPlay) {
    if (!autoPlay) return@LaunchedEffect
    while (true) {
      selectTrack(0)
      state = ShowcaseState.Player
      delay(1000)
      state = ShowcaseState.TrackList
      delay(1250)
      selectTrack(1)
      state = ShowcaseState.AlbumChanged
      delay(1300)
      state = ShowcaseState.PlaylistPicker
      delay(1400)
      selectPlaylist((activePlaylistIndex + 1) % playlists.size)
      delay(1800)
      state = ShowcaseState.Player
      delay(1200)
    }
  }

  LaunchedEffect(state, selectedTrackIndex) {
    val selectedIndex = selectedTrackIndex
    if (state != ShowcaseState.SelectingTrack || selectedIndex == null) return@LaunchedEffect
    delay(150)
    selectTrack(selectedIndex)
    delay(520)
    state = ShowcaseState.Player
    selectedTrackIndex = null
  }

  LaunchedEffect(pendingPlaylistIndex) {
    val selectedIndex = pendingPlaylistIndex
    if (selectedIndex == null) return@LaunchedEffect
    albumSwapProgress.snapTo(0f)
    delay(560)
    state = ShowcaseState.PlaylistDone
    val swapAnimation =
      launch {
        albumSwapProgress.animateTo(
          targetValue = 1f,
          animationSpec = tween(AlbumSwapDurationMillis, easing = FastOutSlowInEasing),
        )
      }
    delay(AlbumTextCommitDelayMillis.toLong())
    activePlaylistIndex = selectedIndex
    currentTrackIndex = 0
    progressSeconds = 0
    isPlaying = true
    swapAnimation.join()
    state = ShowcaseState.Player
    pendingPlaylistIndex = null
    outgoingAlbum = null
    albumSwapProgress.snapTo(0f)
  }

  LaunchedEffect(isPlaying, activePlaylistIndex, currentTrackIndex) {
    while (isPlaying) {
      delay(1000)
      progressSeconds += 1
      if (progressSeconds >= activeTracks[currentTrackIndex.coerceIn(activeTracks.indices)].durationSeconds) {
        nextTrack()
      }
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(statusBarGradientTop, environmentTop, environmentMiddle, environmentBottom)))
        .statusBarsPadding()
        .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    PhoneMockup(
      state = state,
      album = album,
      pendingAlbum = pendingAlbum,
      outgoingAlbum = outgoingAlbum,
      albumSwapProgress = albumSwapProgress.value,
      currentTrack = currentTrack,
      activeTracks = activeTracks,
      selectedTrackIndex = selectedTrackIndex,
      activePlaylistIndex = activePlaylistIndex,
      discTransitionKey = discTransitionKey,
      progressSeconds = progressSeconds,
      isPlaying = isPlaying,
      onAllTracks = { state = ShowcaseState.TrackList },
      onTrackSelected = {
        selectedTrackIndex = activeTracks.indexOf(it).takeIf { index -> index >= 0 }
        state = ShowcaseState.SelectingTrack
      },
      onTrackListDismiss = { state = ShowcaseState.Player },
      onPlayPause = { isPlaying = !isPlaying },
      onPrevious = ::previousTrack,
      onNext = ::nextTrack,
      onSeek = { ratio ->
        progressSeconds = (currentTrack.durationSeconds * ratio).toInt().coerceIn(0, currentTrack.durationSeconds)
      },
      onPlus = {
        if (pendingPlaylistIndex != null) return@PhoneMockup
        state =
          if (
            state == ShowcaseState.PlaylistPicker ||
              state == ShowcaseState.PlaylistsOverview
          ) {
            ShowcaseState.Player
          } else {
            ShowcaseState.PlaylistPicker
          }
      },
      onPlaylistSelected = ::selectPlaylist,
      onDoneContinue = { state = ShowcaseState.PlaylistsOverview },
      onOverviewDone = {
        selectTrack(0)
        state = ShowcaseState.Player
      },
    )
  }
}

@Composable
private fun PhoneMockup(
  state: ShowcaseState,
  album: AlbumUi,
  pendingAlbum: AlbumUi?,
  outgoingAlbum: AlbumUi?,
  albumSwapProgress: Float,
  currentTrack: Track,
  activeTracks: List<Track>,
  selectedTrackIndex: Int?,
  activePlaylistIndex: Int,
  discTransitionKey: Int,
  progressSeconds: Int,
  isPlaying: Boolean,
  onAllTracks: () -> Unit,
  onTrackSelected: (Track) -> Unit,
  onTrackListDismiss: () -> Unit,
  onPlayPause: () -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onSeek: (Float) -> Unit,
  onPlus: () -> Unit,
  onPlaylistSelected: (Int) -> Unit,
  onDoneContinue: () -> Unit,
  onOverviewDone: () -> Unit,
) {
  BoxWithConstraints(contentAlignment = Alignment.Center) {
    val phoneWidth = minOf(maxWidth - 28.dp, 390.dp)
    val phoneHeight = minOf(maxHeight - 28.dp, 780.dp)
    val transition = updateTransition(state, label = "showcase")
    val compact by transition.animateFloat(
      transitionSpec = {
        if (targetState == ShowcaseState.TrackList) {
          tween(620, easing = EasePremium)
        } else {
          tween(450, easing = EasePremium)
        }
      },
      label = "compact",
    ) { if (it == ShowcaseState.TrackList) 1f else 0f }
    val sheetProgress by transition.animateFloat(
      transitionSpec = {
        when {
          targetState == ShowcaseState.TrackList -> tween(660, easing = EasePremium)
          targetState == ShowcaseState.SelectingTrack -> tween(450, easing = FastOutSlowInEasing)
          else -> tween(500, easing = FastOutSlowInEasing)
        }
      },
      label = "sheet",
    ) { if (it == ShowcaseState.TrackList) 1f else 0f }
    val pickerProgress by transition.animateFloat(
      transitionSpec = { tween(560, easing = EasePremium) },
      label = "picker",
    ) { if (it == ShowcaseState.PlaylistPicker) 1f else 0f }
    val playerPickerProgress by transition.animateFloat(
      transitionSpec = { tween(520, easing = EasePremium) },
      label = "playerPicker",
    ) { if (it == ShowcaseState.PlaylistPicker) 1f else 0f }
    val overviewAlpha by transition.animateFloat(
      transitionSpec = { tween(520, easing = FastOutSlowInEasing) },
      label = "overviewAlpha",
    ) { if (it == ShowcaseState.PlaylistsOverview) 1f else 0f }
    val playerPanelColor by animateColorAsState(
      targetValue = playerPanelColor(album),
      animationSpec = tween(620, easing = EasePremium),
      label = "playerPanelColor",
    )
    val panelHeight by transition.animateDp(
      transitionSpec = {
        if (targetState == ShowcaseState.TrackList) {
          tween(620, easing = EasePremium)
        } else {
          tween(500, easing = EasePremium)
        }
      },
      label = "panelHeight",
    ) {
      when (it) {
        ShowcaseState.TrackList -> phoneHeight * 0.62f
        ShowcaseState.PlaylistPicker -> phoneHeight * 0.56f
        ShowcaseState.PlaylistsOverview -> phoneHeight * 0.20f
        else -> phoneHeight
      }
    }

    Box(
      modifier =
        Modifier
          .width(phoneWidth)
          .height(phoneHeight)
          .clip(RoundedCornerShape(42.dp))
          .background(Color.Transparent),
    ) {
      if (state == ShowcaseState.PlaylistPicker || pickerProgress > 0.01f) {
        PlaylistCardStack(
          progress = pickerProgress,
          doneProgress = albumSwapProgress,
          playlists = playlists,
          activePlaylistIndex = activePlaylistIndex,
          selectionEnabled = state == ShowcaseState.PlaylistPicker && pendingAlbum == null,
          onPlaylistSelected = onPlaylistSelected,
          modifier = Modifier.matchParentSize().zIndex(3f),
        )
      }

      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .zIndex(4f)
            .graphicsLayer {
              scaleX = 1f - compact * 0.035f
              scaleY = 1f - compact * 0.055f
              translationY = -compact * 22f
              shadowElevation = 0f
            }
            .clip(RoundedCornerShape(bottomStart = 38.dp, bottomEnd = 38.dp))
            .background(playerPanelColor),
      ) {
        PlayerPanel(
          state = state,
          album = album,
          outgoingAlbum = outgoingAlbum,
          incomingAlbum = pendingAlbum,
          currentTrack = currentTrack,
          discTransitionKey = discTransitionKey,
          progressSeconds = progressSeconds,
          isPlaying = isPlaying,
          compactProgress = compact,
          pickerProgress = playerPickerProgress,
          albumSwapProgress = albumSwapProgress,
          onPlayPause = onPlayPause,
          onPrevious = onPrevious,
          onNext = onNext,
          onSeek = onSeek,
          onAllTracks = onAllTracks,
          onPlus = onPlus,
        )
      }

      if (state == ShowcaseState.TrackList || state == ShowcaseState.SelectingTrack || sheetProgress > 0.01f) {
        TrackListSheet(
          progress = sheetProgress,
          tracks = activeTracks,
          album = album,
          currentTrackIndex = activeTracks.indexOf(currentTrack).takeIf { it >= 0 } ?: 0,
          isPlaying = isPlaying,
          selectedTrackIndex = selectedTrackIndex,
          isSelecting = state == ShowcaseState.SelectingTrack,
          onDismiss = onTrackListDismiss,
          onTrackSelected = onTrackSelected,
          modifier = Modifier.matchParentSize().zIndex(8f),
        )
      }

      if (overviewAlpha > 0.01f) {
        PlaylistsOverviewScreen(
          progress = overviewAlpha,
          playlists = playlists,
          onReturn = onOverviewDone,
          modifier = Modifier.matchParentSize().zIndex(6f),
        )
      }

    }
  }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PlayerPanel(
  state: ShowcaseState,
  album: AlbumUi,
  outgoingAlbum: AlbumUi?,
  incomingAlbum: AlbumUi?,
  currentTrack: Track,
  discTransitionKey: Int,
  progressSeconds: Int,
  isPlaying: Boolean,
  compactProgress: Float,
  pickerProgress: Float,
  albumSwapProgress: Float,
  onPlayPause: () -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onSeek: (Float) -> Unit,
  onAllTracks: () -> Unit,
  onPlus: () -> Unit,
) {
  val contentAlpha = 1f - compactProgress * 0.18f
  val trackSelectorColor by animateColorAsState(
    targetValue = album.waveform,
    animationSpec = tween(620, easing = EasePremium),
    label = "trackSelectorColor",
  )
  val discScale = remember { Animatable(1f) }
  val coverAlpha = remember { Animatable(1f) }
  val rotationBoost = remember { Animatable(0f) }
  val playPulse = remember { Animatable(1f) }
  val discInteractionSource = remember { MutableInteractionSource() }
  val discRotation = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  LaunchedEffect(isPlaying) {
    while (isPlaying) {
      discRotation.animateTo(
        targetValue = discRotation.value + 360f,
        animationSpec = tween(16000, easing = LinearEasing),
      )
      discRotation.snapTo(discRotation.value % 360f)
    }
  }
  LaunchedEffect(discTransitionKey) {
    rotationBoost.snapTo(0f)
    discScale.snapTo(1f)
    coverAlpha.snapTo(1f)
    scope.launch {
      rotationBoost.animateTo(165f, tween(520, easing = FastOutSlowInEasing))
      rotationBoost.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
    }
    scope.launch {
      delay(60)
      coverAlpha.animateTo(0.56f, tween(140, easing = FastOutSlowInEasing))
      coverAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
    }
    discScale.animateTo(0.96f, tween(170, easing = FastOutSlowInEasing))
    discScale.animateTo(1.04f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium))
    discScale.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow))
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(horizontal = 28.dp, vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    PlayerHeader(album = album)
    Spacer(Modifier.height(18.dp))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = currentTrack.title,
        color = Ink,
        fontSize = if (currentTrack.title.length > 20) 23.sp else 27.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 27.sp,
      )
      Spacer(Modifier.height(5.dp))
      Text("${currentTrack.artist} - ${album.title}", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(18.dp))
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .weight(1f)
          .clip(RoundedCornerShape(180.dp))
          .clickable(
            interactionSource = discInteractionSource,
            indication = null,
          ) {
            onPlayPause()
            scope.launch {
              playPulse.snapTo(0.94f)
              playPulse.animateTo(1.04f, tween(150, easing = EasePremium))
              playPulse.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium))
            }
          }
          .graphicsLayer {
            alpha = contentAlpha
            translationY = -compactProgress * 26f
            scaleX = 1f - pickerProgress * 0.1f
            scaleY = 1f - pickerProgress * 0.1f
          },
      contentAlignment = Alignment.Center,
    ) {
      OrganicWaveLines(
        modifier =
          Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.78f),
      )
      if (albumSwapProgress > 0.01f && outgoingAlbum != null && incomingAlbum != null) {
        SwappingVinylDiscs(
          progress = albumSwapProgress,
          outgoingAlbum = outgoingAlbum,
          incomingAlbum = incomingAlbum,
          baseRotation = discRotation.value,
          modifier =
            Modifier
              .fillMaxWidth(0.74f)
              .aspectRatio(1f),
        )
      } else {
        VinylDisc(
          album = album,
          baseRotation = discRotation.value,
          rotationBoost = rotationBoost.value,
          coverAlpha = coverAlpha.value,
          modifier =
            Modifier
              .fillMaxWidth(0.74f)
              .aspectRatio(1f)
              .graphicsLayer {
                scaleX = discScale.value * playPulse.value
                scaleY = discScale.value * playPulse.value
                alpha = if (state == ShowcaseState.AlbumChanged || state == ShowcaseState.SelectingTrack) 0.96f else 1f
              },
        )
      }
    }
    Spacer(Modifier.height(14.dp))
    Row(
      Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(formatSeconds(progressSeconds), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      AudioWaveform(
        color = album.waveform,
        progress = progressSeconds / currentTrack.durationSeconds.toFloat(),
        onSeek = onSeek,
        modifier =
          Modifier
            .weight(1f)
            .height(42.dp)
            .padding(horizontal = 12.dp)
            .graphicsLayer {
              alpha = 1f - compactProgress * 0.5f
              translationY = compactProgress * 18f
            },
      )
      Text(currentTrack.duration, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(12.dp))
    PlayerControls(
      album = album,
      plusActive = state == ShowcaseState.PlaylistPicker,
      isPlaying = isPlaying,
      onPrevious = onPrevious,
      onPlayPause = {
        onPlayPause()
        scope.launch {
          playPulse.snapTo(0.94f)
          playPulse.animateTo(1.04f, tween(150, easing = EasePremium))
          playPulse.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium))
        }
      },
      onNext = onNext,
      onPlus = onPlus,
      modifier =
        Modifier.graphicsLayer {
          alpha = 1f - compactProgress * 0.42f
          translationY = compactProgress * 18f
        },
    )
    Spacer(Modifier.height(18.dp))
    Column(
      modifier =
        Modifier
          .clip(RoundedCornerShape(18.dp))
          .clickable(onClick = onAllTracks)
          .padding(horizontal = 42.dp, vertical = 14.dp)
          .graphicsLayer {
            alpha = 1f - pickerProgress
            translationY = compactProgress * 16f
          },
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text("ALL TRACKS", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
      Spacer(Modifier.height(5.dp))
      Box(
        Modifier
          .width(34.dp)
          .height(3.dp)
          .clip(RoundedCornerShape(99.dp))
          .background(trackSelectorColor.copy(alpha = 0.9f)),
      )
    }
  }
}

@Composable
private fun PlayerHeader(
  album: AlbumUi? = null,
  onBackClick: () -> Unit = {},
) {
  val buttonColor = album?.let { albumButtonSurfaceColor(it) } ?: OuterSteel.copy(alpha = 0.78f)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SoftIconButton(
      IconKind.Back,
      buttonSize = 42.dp,
      containerColor = buttonColor,
      contentColor = readableOnColor(buttonColor),
      onClick = onBackClick,
    )
    Spacer(Modifier.size(52.dp))
  }
}

@Composable
private fun VinylDisc(
  album: AlbumUi,
  baseRotation: Float,
  rotationBoost: Float,
  coverAlpha: Float,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .shadow(24.dp, CircleShape, ambientColor = Color(0x33293241), spotColor = Color(0x30293241))
        .graphicsLayer { rotationZ = baseRotation + rotationBoost }
        .clip(CircleShape),
  ) {
    if (album.discRes != null) {
      Image(
        painter = painterResource(album.discRes),
        contentDescription = "${album.title} vinyl",
        modifier =
          Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = coverAlpha },
        contentScale = ContentScale.Crop,
      )
    } else {
      GenericVinylDisc(
        album = album,
        modifier =
          Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = coverAlpha },
      )
    }
    Canvas(Modifier.matchParentSize()) {
      val radius = size.minDimension / 2f
      val center = Offset(size.width / 2f, size.height / 2f)
      drawCircle(
        color = album.colors[0].copy(alpha = 0.08f),
        radius = radius * 0.98f,
        center = center,
      )
      for (i in 0..9) {
        drawCircle(
          color = Color.Black.copy(alpha = 0.05f + (i % 2) * 0.02f),
          radius = radius * (0.30f + i * 0.065f),
          center = center,
          style = Stroke(width = 1.1f),
        )
      }
      drawArc(
        color = Color.White.copy(alpha = 0.22f),
        startAngle = -44f,
        sweepAngle = 72f,
        useCenter = false,
        topLeft = Offset(radius * 0.12f, radius * 0.10f),
        size = Size(radius * 1.72f, radius * 1.72f),
        style = Stroke(width = radius * 0.055f, cap = StrokeCap.Round),
        blendMode = BlendMode.Screen,
      )
    }
  }
}

@Composable
private fun SwappingVinylDiscs(
  progress: Float,
  outgoingAlbum: AlbumUi,
  incomingAlbum: AlbumUi,
  baseRotation: Float,
  modifier: Modifier = Modifier,
) {
  val swap = EasePremium.transform(progress.coerceIn(0f, 1f))
  BoxWithConstraints(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    val travel = maxWidth * 1.32f
    VinylDisc(
      album = outgoingAlbum,
      baseRotation = baseRotation,
      rotationBoost = -180f * swap,
      coverAlpha = 1f,
      modifier =
        Modifier
          .matchParentSize()
          .graphicsLayer {
            translationX = -travel.toPx() * swap
            alpha = (1f - swap * 1.15f).coerceIn(0f, 1f)
            scaleX = 1f - swap * 0.04f
            scaleY = 1f - swap * 0.04f
          },
    )
    VinylDisc(
      album = incomingAlbum,
      baseRotation = baseRotation,
      rotationBoost = 180f * swap,
      coverAlpha = 1f,
      modifier =
        Modifier
          .matchParentSize()
          .graphicsLayer {
            translationX = travel.toPx() * (1f - swap)
            alpha = (0.35f + swap * 0.65f).coerceIn(0f, 1f)
            scaleX = 0.94f + swap * 0.06f
            scaleY = 0.94f + swap * 0.06f
          },
    )
  }
}

@Composable
private fun GenericVinylDisc(
  album: AlbumUi,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.background(Brush.radialGradient(listOf(album.colors[2], Color(0xFF050505))))) {
    Canvas(Modifier.matchParentSize()) {
      val radius = size.minDimension / 2f
      val center = Offset(size.width / 2f, size.height / 2f)
      drawCircle(album.colors[0].copy(alpha = 0.18f), radius * 0.98f, center)
      for (i in 0..15) {
        drawCircle(
          color = Color.White.copy(alpha = if (i % 3 == 0) 0.075f else 0.035f),
          radius = radius * (0.18f + i * 0.052f),
          center = center,
          style = Stroke(width = 1.1f),
        )
      }
      drawCircle(album.colors[1], radius * 0.20f, center)
      drawCircle(Color.White.copy(alpha = 0.88f), radius * 0.045f, center)
    }
    if (album.coverRes != null) {
      Image(
        painter = painterResource(album.coverRes),
        contentDescription = "${album.title} center label",
        modifier =
          Modifier
            .align(Alignment.Center)
            .fillMaxSize(0.28f)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
      )
    }
  }
}

@Composable
private fun OrganicWaveLines(modifier: Modifier = Modifier) {
  val infinite = rememberInfiniteTransition(label = "organicLines")
  val phase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "phase",
  )

  Canvas(modifier = modifier) {
    repeat(5) { line ->
      val path = Path()
      val yBase = size.height * (0.26f + line * 0.115f)
      path.moveTo(size.width * 0.04f, yBase)
      for (step in 1..5) {
        val x = size.width * step / 5f
        val wave = sin((step + line + phase * 2f) * PI).toFloat() * size.height * 0.06f
        path.cubicTo(
          x - size.width * 0.12f,
          yBase - wave - line * 3f,
          x - size.width * 0.05f,
          yBase + wave,
          x,
          yBase + wave * 0.45f,
        )
      }
      drawPath(
        path = path,
        color = SoftLine.copy(alpha = 0.20f - line * 0.02f),
        style = Stroke(width = 2.4f, cap = StrokeCap.Round),
      )
    }
  }
}

@Composable
private fun AudioWaveform(
  color: Color,
  progress: Float,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val bars =
    remember {
      listOf(0.22f, 0.48f, 0.33f, 0.72f, 0.38f, 0.92f, 0.58f, 0.44f, 0.76f, 0.36f, 0.62f, 0.28f, 0.82f, 0.55f, 0.31f, 0.68f, 0.46f, 0.24f)
    }
  Canvas(
    modifier =
      modifier.pointerInput(Unit) {
        detectTapGestures { tap ->
          onSeek((tap.x / size.width).coerceIn(0f, 1f))
        }
      },
  ) {
    val gap = size.width / (bars.size * 1.75f)
    val stroke = gap * 0.74f
    val progressX = size.width * progress.coerceIn(0f, 1f)
    bars.forEachIndexed { index, value ->
      val x = gap + index * gap * 1.75f
      val h = size.height * value
      drawLine(
        color = color.copy(alpha = if (x <= progressX) 1f else 0.28f),
        start = Offset(x, (size.height - h) / 2f),
        end = Offset(x, (size.height + h) / 2f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
      )
    }
  }
}

@Composable
private fun PlayerControls(
  album: AlbumUi,
  plusActive: Boolean,
  isPlaying: Boolean,
  onPrevious: () -> Unit,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  onPlus: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val normalButtonColor = albumButtonSurfaceColor(album)
  val normalIconColor = readableOnColor(normalButtonColor)
  val primaryButtonColor = albumPrimaryButtonSurfaceColor(album)
  val primaryIconColor = readableOnColor(primaryButtonColor)
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SoftIconButton(IconKind.Shuffle, buttonSize = 40.dp, containerColor = normalButtonColor, contentColor = normalIconColor)
    SoftIconButton(IconKind.Previous, buttonSize = 50.dp, containerColor = normalButtonColor, contentColor = normalIconColor, onClick = onPrevious)
    SoftIconButton(
      if (isPlaying) IconKind.Pause else IconKind.Play,
      buttonSize = 66.dp,
      containerColor = primaryButtonColor,
      contentColor = primaryIconColor,
      onClick = onPlayPause,
    )
    SoftIconButton(IconKind.Next, buttonSize = 50.dp, containerColor = normalButtonColor, contentColor = normalIconColor, onClick = onNext)
    SoftIconButton(
      IconKind.List,
      buttonSize = 36.dp,
      containerColor = if (plusActive) primaryButtonColor else normalButtonColor,
      contentColor = if (plusActive) primaryIconColor else normalIconColor,
      onClick = onPlus,
    )
  }
}

@Composable
private fun TrackListSheet(
  progress: Float,
  tracks: List<Track>,
  album: AlbumUi,
  currentTrackIndex: Int,
  isPlaying: Boolean,
  selectedTrackIndex: Int?,
  isSelecting: Boolean,
  onDismiss: () -> Unit,
  onTrackSelected: (Track) -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val easedProgress = EasePremium.transform(progress.coerceIn(0f, 1f))
  val sheetTop = album.colors.getOrElse(0) { SheetTop }
  val sheetBottom = album.colors.getOrElse(1) { SheetBottom }
  val sheetContent = readableOnColor(sheetBottom)
  Box(
    modifier = modifier.graphicsLayer { alpha = 0.92f + easedProgress * 0.08f },
  ) {
    Box(
      modifier =
        Modifier
          .matchParentSize()
          .background(Color.Black.copy(alpha = 0.08f * easedProgress)),
    )
    Column(
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .fillMaxHeight(0.62f)
          .graphicsLayer {
            translationY = with(density) { (1f - easedProgress) * 520.dp.toPx() }
            scaleX = 0.985f + easedProgress * 0.015f
            scaleY = 0.985f + easedProgress * 0.015f
          }
          .shadow(24.dp, RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp), ambientColor = Color(0x33293241), spotColor = Color(0x26293241))
          .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
          .background(Brush.verticalGradient(listOf(sheetTop, sheetBottom)))
          .padding(horizontal = 28.dp, vertical = 22.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(Unit) {
              var dragDistance = 0f
              detectVerticalDragGestures(
                onDragStart = { dragDistance = 0f },
                onVerticalDrag = { _, dragAmount -> dragDistance += dragAmount },
                onDragEnd = {
                  if (dragDistance > 28.dp.toPx()) {
                    onDismiss()
                  }
                },
              )
            }
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier
              .width(48.dp)
              .height(5.dp)
              .clip(RoundedCornerShape(99.dp))
              .background(sheetContent.copy(alpha = 0.34f)),
        )
      }
      LazyColumn(
        modifier =
          Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 22.dp),
      ) {
        itemsIndexed(tracks) { index, track ->
          TrackRow(
            track = track,
            progress = progress,
            isCurrentTrack = currentTrackIndex == index,
            isCurrentPlaying = isPlaying && currentTrackIndex == index,
            isSelected = selectedTrackIndex == index,
            isDimmed = isSelecting && selectedTrackIndex != null && selectedTrackIndex != index,
            contentColor = sheetContent,
            delayIndex = index,
            onClick = { onTrackSelected(track) },
          )
        }
      }
    }
  }
}

@Composable
private fun TrackRow(
  track: Track,
  progress: Float,
  isCurrentTrack: Boolean,
  isCurrentPlaying: Boolean,
  isSelected: Boolean,
  isDimmed: Boolean,
  contentColor: Color,
  delayIndex: Int,
  onClick: () -> Unit,
) {
  val staggeredProgress =
    EasePremium.transform(((progress - delayIndex * 0.026f) / 0.48f).coerceIn(0f, 1f))
  val focusScale by animateFloatAsState(
    targetValue = if (isSelected) 1.025f else 1f,
    animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMedium),
    label = "trackFocusScale",
  )
  val focusAlpha by animateFloatAsState(
    targetValue = if (isDimmed) 0.46f else 1f,
    animationSpec = tween(160, easing = FastOutSlowInEasing),
    label = "trackFocusAlpha",
  )
  val playScale by animateFloatAsState(
    targetValue = if (isSelected) 1.16f else 1f,
    animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
    label = "trackPlayScale",
  )
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .graphicsLayer {
          alpha = staggeredProgress * focusAlpha
          translationY = (1f - staggeredProgress) * 22.dp.toPx()
          scaleX = focusScale
          scaleY = focusScale
        }
        .clip(RoundedCornerShape(20.dp))
        .background(
          when {
            isSelected -> contentColor.copy(alpha = 0.22f)
            isCurrentTrack -> contentColor.copy(alpha = 0.14f)
            else -> Color.Transparent
          },
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 6.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SoftIconButton(
      if (isCurrentPlaying) IconKind.Pause else IconKind.Play,
      buttonSize = 34.dp,
      containerColor = contentColor.copy(alpha = if (isCurrentTrack || isSelected) 0.26f else 0.15f),
      contentColor = contentColor,
      modifier = Modifier.graphicsLayer {
        scaleX = playScale
        scaleY = playScale
      },
      onClick = onClick,
    )
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(track.title, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(track.artist, color = contentColor.copy(alpha = 0.68f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
    Text(track.duration, color = contentColor.copy(alpha = 0.76f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
  }
}

private fun readableOnColor(color: Color): Color {
  val luminance = color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f
  return if (luminance < 0.48f) Color.White else Ink
}

private fun playerPanelColor(album: AlbumUi): Color {
  val firstTint = album.colors.getOrElse(0) { PanelWhite }
  val secondTint = album.colors.getOrElse(1) { firstTint }
  return blendColors(blendColors(PanelWhite, firstTint, 0.18f), secondTint, 0.08f)
}

private fun statusBarGradientColor(album: AlbumUi): Color {
  val firstTint = album.colors.getOrElse(0) { PanelWhite }
  val secondTint = album.colors.getOrElse(1) { firstTint }
  return blendColors(blendColors(PanelWhite, firstTint, 0.32f), secondTint, 0.14f)
}

private fun albumButtonSurfaceColor(album: AlbumUi): Color {
  val panel = playerPanelColor(album)
  val accent = album.colors.getOrElse(1) { album.waveform }
  return blendColors(panel, accent, 0.34f)
}

private fun albumPrimaryButtonSurfaceColor(album: AlbumUi): Color {
  val fallback = album.colors.getOrElse(1) { album.waveform }
  val accent = album.colors.getOrElse(2) { fallback }
  return blendColors(fallback, accent, 0.48f)
}

private fun albumButtonFallbackColor(): Color =
  blendColors(PanelWhite, OuterSteel, 0.46f)

private fun blendColors(
  start: Color,
  end: Color,
  fraction: Float,
): Color {
  val safeFraction = fraction.coerceIn(0f, 1f)
  return Color(
    red = lerp(start.red, end.red, safeFraction),
    green = lerp(start.green, end.green, safeFraction),
    blue = lerp(start.blue, end.blue, safeFraction),
    alpha = lerp(start.alpha, end.alpha, safeFraction),
  )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PlaylistCardStack(
  progress: Float,
  doneProgress: Float,
  playlists: List<PlaylistUi>,
  activePlaylistIndex: Int,
  selectionEnabled: Boolean,
  onPlaylistSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var activeAlbumIndex by remember(activePlaylistIndex) { mutableStateOf(activePlaylistIndex) }
  var dragOffset by remember { mutableStateOf(0f) }
  val density = LocalDensity.current
  val dragLimitPx = with(density) { 140.dp.toPx() }
  val dragThresholdPx = with(density) { 54.dp.toPx() }
  val dragProgress = (abs(dragOffset) / dragLimitPx).coerceIn(0f, 1f)

  Box(
    modifier =
      modifier
        .graphicsLayer { alpha = progress * (1f - doneProgress * 0.25f) }
        .background(Color.Transparent)
        .padding(horizontal = 28.dp, vertical = 26.dp)
        .pointerInput(playlists.size, selectionEnabled) {
          if (!selectionEnabled) return@pointerInput
          detectHorizontalDragGestures(
            onDragEnd = {
              activeAlbumIndex =
                when {
                  dragOffset < -dragThresholdPx -> (activeAlbumIndex + 1) % playlists.size
                  dragOffset > dragThresholdPx -> (activeAlbumIndex - 1 + playlists.size) % playlists.size
                  else -> activeAlbumIndex
                }
              dragOffset = 0f
            },
            onDragCancel = { dragOffset = 0f },
            onHorizontalDrag = { _, dragAmount ->
              dragOffset = (dragOffset + dragAmount).coerceIn(-dragLimitPx, dragLimitPx)
            },
          )
        },
    contentAlignment = Alignment.BottomCenter,
  ) {
    AnimatedContent(
      targetState = playlists[activeAlbumIndex],
      transitionSpec = {
        (fadeIn(tween(260, easing = FastOutSlowInEasing)) + slideInVertically { it / 3 }) togetherWith
          (fadeOut(tween(160, easing = FastOutSlowInEasing)) + slideOutVertically { -it / 3 })
      },
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 224.dp)
          .graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 18.dp.toPx()
          }
          .zIndex(48f),
      label = "activeAlbumLabel",
    ) { album ->
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = album.title,
          color = Ink,
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
          text = album.artist,
          color = Muted,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    playlists.forEachIndexed { index, playlist ->
      val slot = (index - activeAlbumIndex + playlists.size) % playlists.size
      val cardProgress by animateFloatAsState(
        targetValue = if (progress > 0.12f) 1f else 0f,
        animationSpec =
          spring(
            dampingRatio = 0.72f,
            stiffness = 180f,
            visibilityThreshold = 0.001f,
          ),
        label = "albumEntry",
      )
      val entryDelay by animateFloatAsState(
        targetValue = if (progress > 0.12f) 1f else 0f,
        animationSpec = tween(420, delayMillis = slot * 70, easing = EasePremium),
        label = "albumStagger",
      )
      val slotSpec =
        blendedAlbumSlot(
          slot = slot,
          lastSlot = playlists.lastIndex,
          dragOffset = dragOffset,
          dragProgress = dragProgress,
        )
      val entryProgress = cardProgress * entryDelay
      PlaylistCard(
        playlist = playlist,
        modifier =
          Modifier
            .width(218.dp)
            .aspectRatio(1f)
            .graphicsLayer {
              alpha = slotSpec.alpha * entryProgress * (1f - doneProgress * slot * 0.35f)
              translationX = with(density) { slotSpec.translationX.dp.toPx() } + if (slot == 0) dragOffset else 0f
              translationY =
                with(density) {
                  (slotSpec.translationY + (1f - entryProgress) * (220f + slot * 30f)).dp.toPx()
                } - doneProgress * 210.dp.toPx()
              rotationZ = slotSpec.rotation
              scaleX = 0.62f + (slotSpec.scale - 0.62f) * entryProgress + doneProgress * if (slot == 0) 0.15f else -0.12f
              scaleY = 0.62f + (slotSpec.scale - 0.62f) * entryProgress + doneProgress * if (slot == 0) 0.15f else -0.12f
              shadowElevation = with(density) { slotSpec.shadow.dp.toPx() }
            }
            .zIndex(slotSpec.depth)
            .clickable(enabled = selectionEnabled && slot == 0 && progress > 0.75f, onClick = { onPlaylistSelected(index) }),
      )
    }
  }
}

private data class AlbumSlotSpec(
  val translationX: Float,
  val translationY: Float,
  val scale: Float,
  val rotation: Float,
  val alpha: Float,
  val shadow: Float,
  val depth: Float,
)

private fun albumSlotSpec(slot: Int): AlbumSlotSpec =
  when (slot) {
    0 -> AlbumSlotSpec(0f, 0f, 1f, 0f, 1f, 18f, 40f)
    1 -> AlbumSlotSpec(36f, 18f, 0.92f, 4f, 0.90f, 12f, 30f)
    2 -> AlbumSlotSpec(-34f, 36f, 0.84f, -4f, 0.75f, 8f, 20f)
    3 -> AlbumSlotSpec(62f, 52f, 0.76f, 6f, 0.55f, 4f, 10f)
    4 -> AlbumSlotSpec(-58f, 66f, 0.70f, -7f, 0.38f, 2f, 4f)
    5 -> AlbumSlotSpec(12f, 82f, 0.64f, 2f, 0.22f, 1f, 2f)
    else -> AlbumSlotSpec(84f, 92f, 0.58f, 8f, 0.14f, 0f, 1f)
  }

private fun blendedAlbumSlot(
  slot: Int,
  lastSlot: Int,
  dragOffset: Float,
  dragProgress: Float,
): AlbumSlotSpec {
  val base = albumSlotSpec(slot)
  val target =
    when {
      dragOffset < 0f && slot == 0 -> albumSlotSpec(lastSlot)
      dragOffset < 0f && slot == 1 -> albumSlotSpec(0)
      dragOffset > 0f && slot == 0 -> albumSlotSpec(1)
      dragOffset > 0f && slot == lastSlot -> albumSlotSpec(0)
      else -> base
    }
  return AlbumSlotSpec(
    translationX = lerp(base.translationX, target.translationX, dragProgress),
    translationY = lerp(base.translationY, target.translationY, dragProgress),
    scale = lerp(base.scale, target.scale, dragProgress),
    rotation = lerp(base.rotation, target.rotation, dragProgress),
    alpha = lerp(base.alpha, target.alpha, dragProgress),
    shadow = lerp(base.shadow, target.shadow, dragProgress),
    depth = lerp(base.depth, target.depth, dragProgress),
  )
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
  start + (stop - start) * fraction.coerceIn(0f, 1f)

@Composable
private fun PlaylistCard(
  playlist: PlaylistUi,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .shadow(18.dp, RoundedCornerShape(8.dp), ambientColor = Color(0x30293241), spotColor = Color(0x24293241))
        .clip(RoundedCornerShape(8.dp))
        .background(Brush.linearGradient(playlist.colors)),
  ) {
    if (playlist.coverRes != null) {
      Image(
        painter = painterResource(playlist.coverRes),
        contentDescription = "${playlist.title} cover",
        modifier = Modifier.matchParentSize(),
        contentScale = ContentScale.Crop,
      )
    } else {
      AbstractCover(playlist.colors, Modifier.matchParentSize())
    }
  }
}

@Composable
private fun DoneConfirmation(
  progress: Float,
  onContinue: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = progress > 0.08f,
    enter = fadeIn(tween(260)) + scaleIn(spring(dampingRatio = 0.72f), initialScale = 0.92f),
    exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.96f),
    modifier = modifier,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(bottom = 62.dp)
          .clickable(enabled = progress > 0.78f, onClick = onContinue)
          .graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 24.dp.toPx()
          },
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Bottom,
    ) {
      Box(
        modifier =
          Modifier
            .size(68.dp)
            .shadow(18.dp, CircleShape, ambientColor = Color(0x3338BDF8), spotColor = Color(0x338F5CEB))
            .clip(CircleShape)
            .background(Ink),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(Modifier.size(28.dp)) {
          drawLine(Color.White, Offset(size.width * 0.18f, size.height * 0.52f), Offset(size.width * 0.42f, size.height * 0.74f), strokeWidth = 4.5f, cap = StrokeCap.Round)
          drawLine(Color.White, Offset(size.width * 0.42f, size.height * 0.74f), Offset(size.width * 0.84f, size.height * 0.24f), strokeWidth = 4.5f, cap = StrokeCap.Round)
        }
      }
      Spacer(Modifier.height(18.dp))
      Text("DONE!", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
      Text("Track added to your playlist.", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
  }
}

@Composable
private fun PlaylistsOverviewScreen(
  progress: Float,
  playlists: List<PlaylistUi>,
  onReturn: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .graphicsLayer {
          alpha = progress
          translationY = (1f - progress) * 80.dp.toPx()
        }
        .clip(RoundedCornerShape(42.dp))
        .background(PanelWhite)
        .clickable(enabled = progress > 0.85f, onClick = onReturn),
  ) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)) {
      PlayerHeader(onBackClick = onReturn)
      Spacer(Modifier.height(24.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        listOf("OVERVIEW", "MY ALBUNS", "ALBUMS", "ALL TRACKS").forEachIndexed { index, label ->
          Text(
            text = label,
            color = if (index == 1) Ink else Muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier =
              Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(if (index == 1) Color.White else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 6.dp),
          )
        }
      }
      Spacer(Modifier.height(22.dp))
      Text("My albums:", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
      Spacer(Modifier.height(18.dp))
      LazyRow(contentPadding = PaddingValues(end = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(playlists) { playlist ->
          PlaylistCard(
            playlist = playlist,
            modifier =
              Modifier
                .width(238.dp)
                .aspectRatio(1f),
          )
        }
      }
      Spacer(Modifier.height(22.dp))
      Text("Album of the week:", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
      Spacer(Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        MiniPlaylistCard(playlists[1], Modifier.weight(1f))
        MiniPlaylistCard(playlists[2], Modifier.weight(1f))
      }
    }
    SoftIconButton(
      IconKind.List,
      buttonSize = 36.dp,
      dark = true,
      onClick = onReturn,
      modifier =
        Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 24.dp, bottom = 24.dp),
    )
  }
}

@Composable
private fun MiniPlaylistCard(
  playlist: PlaylistUi,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .aspectRatio(1f)
        .shadow(12.dp, RoundedCornerShape(8.dp), ambientColor = Color(0x20293241), spotColor = Color(0x1A293241))
        .clip(RoundedCornerShape(8.dp))
        .background(Brush.linearGradient(playlist.colors)),
  ) {
    if (playlist.coverRes != null) {
      Image(
        painter = painterResource(playlist.coverRes),
        contentDescription = "${playlist.title} cover",
        modifier = Modifier.matchParentSize(),
        contentScale = ContentScale.Crop,
      )
    } else {
      AbstractCover(playlist.colors, Modifier.matchParentSize())
    }
  }
}

@Composable
private fun AbstractCover(
  colors: List<Color>,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    drawCircle(colors[1].copy(alpha = 0.72f), radius = size.minDimension * 0.42f, center = Offset(size.width * 0.26f, size.height * 0.28f))
    drawCircle(colors[2].copy(alpha = 0.58f), radius = size.minDimension * 0.46f, center = Offset(size.width * 0.78f, size.height * 0.22f))
    drawRoundRect(
      color = Color.White.copy(alpha = 0.18f),
      topLeft = Offset(size.width * 0.20f, size.height * 0.50f),
      size = Size(size.width * 0.70f, size.height * 0.22f),
      cornerRadius = CornerRadius(60f, 60f),
      style = Stroke(width = 3.5f),
    )
  }
}

private enum class IconKind {
  Back,
  Shuffle,
  Previous,
  Play,
  Pause,
  Next,
  List,
}

@Composable
private fun SoftIconButton(
  kind: IconKind,
  buttonSize: Dp,
  dark: Boolean = false,
  containerColor: Color? = null,
  contentColor: Color? = null,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (pressed) 0.90f else 1f,
    animationSpec = spring(dampingRatio = 0.64f, stiffness = Spring.StiffnessMedium),
    label = "buttonPress",
  )
  val resolvedContainerColor = containerColor ?: if (dark) Ink else albumButtonFallbackColor()
  val resolvedContentColor = contentColor ?: if (dark) Color.White else readableOnColor(resolvedContainerColor)
  val animatedContainerColor by animateColorAsState(
    targetValue = resolvedContainerColor,
    animationSpec = tween(420, easing = EasePremium),
    label = "buttonContainerColor",
  )
  val animatedContentColor by animateColorAsState(
    targetValue = resolvedContentColor,
    animationSpec = tween(420, easing = EasePremium),
    label = "buttonContentColor",
  )
  Box(
    modifier =
      modifier
        .size(maxOf(buttonSize, 52.dp))
        .clip(CircleShape)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        ),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier
          .size(buttonSize)
          .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
          }
          .shadow(if (dark) 14.dp else 10.dp, CircleShape, ambientColor = Color(0x22293241), spotColor = Color(0x1C293241))
          .clip(CircleShape)
          .background(animatedContainerColor),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(Modifier.size(buttonSize * 0.46f)) {
      val c = animatedContentColor
      val sw = size.minDimension * 0.09f
      when (kind) {
        IconKind.Back -> {
          drawLine(c, Offset(size.width * 0.72f, size.height * 0.20f), Offset(size.width * 0.32f, size.height * 0.50f), sw, StrokeCap.Round)
          drawLine(c, Offset(size.width * 0.32f, size.height * 0.50f), Offset(size.width * 0.72f, size.height * 0.80f), sw, StrokeCap.Round)
        }
        IconKind.Shuffle -> {
          drawLine(c, Offset(size.width * 0.16f, size.height * 0.30f), Offset(size.width * 0.36f, size.height * 0.30f), sw, StrokeCap.Round)
          drawLine(c, Offset(size.width * 0.36f, size.height * 0.30f), Offset(size.width * 0.66f, size.height * 0.70f), sw, StrokeCap.Round)
          drawLine(c, Offset(size.width * 0.16f, size.height * 0.70f), Offset(size.width * 0.36f, size.height * 0.70f), sw, StrokeCap.Round)
          drawLine(c, Offset(size.width * 0.36f, size.height * 0.70f), Offset(size.width * 0.66f, size.height * 0.30f), sw, StrokeCap.Round)
          drawLine(c, Offset(size.width * 0.66f, size.height * 0.30f), Offset(size.width * 0.84f, size.height * 0.30f), sw, StrokeCap.Round)
          drawLine(c, Offset(size.width * 0.66f, size.height * 0.70f), Offset(size.width * 0.84f, size.height * 0.70f), sw, StrokeCap.Round)
          triangle(c, Offset(size.width * 0.84f, size.height * 0.20f), Offset(size.width * 0.84f, size.height * 0.40f), Offset(size.width * 0.96f, size.height * 0.30f))
          triangle(c, Offset(size.width * 0.84f, size.height * 0.60f), Offset(size.width * 0.84f, size.height * 0.80f), Offset(size.width * 0.96f, size.height * 0.70f))
        }
        IconKind.Previous -> {
          drawLine(c, Offset(size.width * 0.25f, size.height * 0.24f), Offset(size.width * 0.25f, size.height * 0.76f), sw, StrokeCap.Round)
          triangle(c, Offset(size.width * 0.72f, size.height * 0.24f), Offset(size.width * 0.34f, size.height * 0.50f), Offset(size.width * 0.72f, size.height * 0.76f))
        }
        IconKind.Play -> triangle(c, Offset(size.width * 0.35f, size.height * 0.24f), Offset(size.width * 0.76f, size.height * 0.50f), Offset(size.width * 0.35f, size.height * 0.76f))
        IconKind.Pause -> {
          drawLine(c, Offset(size.width * 0.38f, size.height * 0.24f), Offset(size.width * 0.38f, size.height * 0.76f), sw * 1.25f, StrokeCap.Round)
          drawLine(c, Offset(size.width * 0.62f, size.height * 0.24f), Offset(size.width * 0.62f, size.height * 0.76f), sw * 1.25f, StrokeCap.Round)
        }
        IconKind.Next -> {
          drawLine(c, Offset(size.width * 0.75f, size.height * 0.24f), Offset(size.width * 0.75f, size.height * 0.76f), sw, StrokeCap.Round)
          triangle(c, Offset(size.width * 0.28f, size.height * 0.24f), Offset(size.width * 0.66f, size.height * 0.50f), Offset(size.width * 0.28f, size.height * 0.76f))
        }
        IconKind.List -> {
          drawRoundRect(
            color = c.copy(alpha = 0.34f),
            topLeft = Offset(size.width * 0.26f, size.height * 0.18f),
            size = Size(size.width * 0.56f, size.height * 0.56f),
            cornerRadius = CornerRadius(sw * 1.45f, sw * 1.45f),
            style = Stroke(width = sw * 0.72f, cap = StrokeCap.Round),
          )
          drawRoundRect(
            color = c,
            topLeft = Offset(size.width * 0.18f, size.height * 0.26f),
            size = Size(size.width * 0.56f, size.height * 0.56f),
            cornerRadius = CornerRadius(sw * 1.45f, sw * 1.45f),
            style = Stroke(width = sw * 0.92f, cap = StrokeCap.Round),
          )
          drawCircle(
            color = c,
            radius = size.minDimension * 0.11f,
            center = Offset(size.width * 0.46f, size.height * 0.54f),
            style = Stroke(width = sw * 0.66f, cap = StrokeCap.Round),
          )
          drawCircle(
            color = c,
            radius = sw * 0.32f,
            center = Offset(size.width * 0.46f, size.height * 0.54f),
          )
        }
      }
    }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.triangle(
  color: Color,
  p1: Offset,
  p2: Offset,
  p3: Offset,
) {
  val path = Path().apply {
    moveTo(p1.x, p1.y)
    lineTo(p2.x, p2.y)
    lineTo(p3.x, p3.y)
    close()
  }
  drawPath(path, color)
}

private fun formatSeconds(totalSeconds: Int): String {
  val safeSeconds = totalSeconds.coerceAtLeast(0)
  val minutes = safeSeconds / 60
  val seconds = safeSeconds % 60
  return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Preview(showBackground = true, widthDp = 430, heightDp = 860)
@Composable
private fun MusicPlayerShowcasePreview() {
  MusicPlayerShowcaseScreen(autoPlay = false)
}

package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Song
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.sin

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    
    // Scan music early once on launch
    LaunchedEffect(Unit) {
        viewModel.scanMusic(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is Screen.Splash -> SplashScreen(onFinished = {
                viewModel.navigateTo(Screen.Home)
            })
            is Screen.Home -> HomeScreen(viewModel = viewModel)
            is Screen.PlaylistDetails -> PlaylistDetailsScreen(
                viewModel = viewModel,
                playlistId = screen.playlistId,
                playlistName = screen.name
            )
            is Screen.AlbumDetails -> LibraryGroupDetailScreen(
                viewModel = viewModel,
                title = screen.albumName,
                songsFlow = viewModel.allSongs.map { songs -> songs.filter { it.album == screen.albumName } },
                onBack = { viewModel.navigateTo(Screen.Home) }
            )
            is Screen.ArtistDetails -> LibraryGroupDetailScreen(
                viewModel = viewModel,
                title = screen.artistName,
                songsFlow = viewModel.allSongs.map { songs -> songs.filter { it.artist == screen.artistName } },
                onBack = { viewModel.navigateTo(Screen.Home) }
            )
            is Screen.FolderDetails -> LibraryGroupDetailScreen(
                viewModel = viewModel,
                title = screen.folderPath,
                songsFlow = viewModel.allSongs.map { songs -> songs.filter { it.folder == screen.folderPath } },
                onBack = { viewModel.navigateTo(Screen.Home) }
            )
        }

        // Persistent Mini Player on top of any screen except Splash
        if (currentScreen != Screen.Splash) {
            val currentSong by viewModel.currentSong.collectAsState()
            if (currentSong != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp) // Leave room for bottom bar if needed, or 16dp
                        .padding(horizontal = 16.dp)
                ) {
                    MiniPlayer(viewModel = viewModel)
                }
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "VinylSpin"
    )

    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1200, easing = EaseInOutCubic), label = "Fade"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianDark,
                        Color(0xFF100B1A),
                        ObsidianDark
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterAlignmentLineMap() ?: Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Spinning Holographic Vinyl Logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .rotate(rotationAngle)
            ) {
                // outer ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, HotPink, NeonCyan, Color.Transparent),
                            radius = size.minDimension / 1.8f
                        ),
                        style = Stroke(width = 6f)
                    )
                }
                // Vinyl body
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F1015))
                ) {
                    // Small rings
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (r in 20..70 step 10) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = r.dp.toPx(),
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }
                }
                // Center pink sticker
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(HotPink)
                )
                // Center hole
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(ObsidianDark)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
                modifier = Modifier.testTag("app_logo_text")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Vibrant Fluid Sound",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Alignment helper to bypass potential compile mismatch
private fun Alignment.Companion.CenterAlignmentLineMap(): Alignment.Horizontal? = CenterHorizontally


// ==========================================
// 2. DASHBOARD / HOME SCREEN
// ==========================================
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf("all_songs") } // all_songs, albums, artists, recent, folders
    val songsList by viewModel.allSongs.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredSongs by viewModel.searchResults.collectAsState()

    val albumsMap by viewModel.albumsFlow.collectAsState()
    val artistsMap by viewModel.artistsFlow.collectAsState()
    val foldersMap by viewModel.foldersFlow.collectAsState()
    val recentSongs by viewModel.recentSongs.collectAsState(initial = emptyList())
    val favoritesList by viewModel.favorites.collectAsState(initial = emptyList())
    val playlistsList by viewModel.playlists.collectAsState(initial = emptyList())

    // Permission request handler (Android 13+ and backward compatibility)
    val readAudioPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanMusic(context)
        }
    }

    // Floating create playlist variables
    val showCreate by viewModel.showCreatePlaylistDialog.collectAsState()
    var newPlaylistName by remember { mutableStateOf("") }
    var dialogTab by remember { mutableStateOf("STANDARD") } // STANDARD, SMART, M3U
    var smartTypeSelection by remember { mutableStateOf("most_played") } // recently_played, most_played, added_range
    var smartParamDays by remember { mutableStateOf("30") }
    var m3uImportData by remember { mutableStateOf("#EXTM3U\n#EXTINF:230,Retro Waves\ndemo_1\n#EXTINF:180,Night Breeze\ndemo_3") }
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showCreate) {
        Dialog(onDismissRequest = { viewModel.showCreatePlaylistDialog.value = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "أنشئ قائمة تشغيل",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(2.dp)
                    ) {
                        listOf("STANDARD" to "عادية", "SMART" to "ذكية", "M3U" to "استيراد M3U").forEach { (tabKey, label) ->
                            val isSel = dialogTab == tabKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) NeonCyan else Color.Transparent)
                                    .clickable { dialogTab = tabKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) ObsidianDark else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Common playlist name input (used for all modes)
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("اسم قائمة التشغيل", color = Color.Gray) },
                        placeholder = { Text("مثال: أغاني المفضلة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = NeonCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab specific layouts
                    when (dialogTab) {
                        "STANDARD" -> {
                            Text(
                                text = "تسمح لك قوائم التشغيل العادية باختيار، إضافة، ترتيب وحذف الأغاني يدوياً من خلال السحب.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        "SMART" -> {
                            Text(
                                text = "اختر معايير ذكية. سيتم ملء قائمة التشغيل هذه تلقائياً وديناميكياً:",
                                fontSize = 11.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Choices: Most Played, Recently Played, Added Recently
                            val smartModes = listOf(
                                "most_played" to "الأكثر تشغيلاً (مرات سماع عالية)",
                                "recently_played" to "المشغلة حديثاً",
                                "added_range" to "المضافة حديثاً (ضمن إطار زمن المضافة)"
                            )

                            smartModes.forEach { (key, label) ->
                                val isSel = smartTypeSelection == key
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                        .clickable { smartTypeSelection = key }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSel,
                                        onClick = { smartTypeSelection = key },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan, unselectedColor = Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = label, fontSize = 11.sp, color = Color.White)
                                }
                            }

                            if (smartTypeSelection == "added_range") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الإطار الزمني للمقاطعة (بالأيام): ", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = smartParamDays,
                                        onValueChange = { smartParamDays = it },
                                        singleLine = true,
                                        modifier = Modifier.width(80.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            }
                        }
                        "M3U" -> {
                            Text(
                                text = "ألصق محتوى ملف M3U مباشرة أدناه:",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = m3uImportData,
                                onValueChange = { m3uImportData = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.showCreatePlaylistDialog.value = false }) {
                            Text("إلغاء", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val nameInput = newPlaylistName.ifBlank {
                                    when (dialogTab) {
                                        "STANDARD" -> "قائمتي الموسيقية"
                                        "SMART" -> "ذكية - " + (if (smartTypeSelection == "most_played") "الأكثر تشغيلاً" else if (smartTypeSelection == "recently_played") "المشغلة حديثاً" else "المضافة حديثاً")
                                        else -> "قائمة مستوردة"
                                    }
                                }

                                when (dialogTab) {
                                    "STANDARD" -> {
                                        viewModel.createPlaylist(nameInput)
                                    }
                                    "SMART" -> {
                                        val pParam = if (smartTypeSelection == "added_range") smartParamDays else null
                                        viewModel.createSmartPlaylist(nameInput, smartTypeSelection, pParam)
                                    }
                                    "M3U" -> {
                                        viewModel.importM3U(nameInput, m3uImportData) { _ -> }
                                    }
                                }

                                newPlaylistName = ""
                                viewModel.showCreatePlaylistDialog.value = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text(
                                text = if (dialogTab == "M3U") "استيراد" else "إنشاء",
                                color = ObsidianDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        Dialog(onDismissRequest = { showThemeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "مظهر التطبيق الرئيسي",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "اختر ثيم الألوان ووضع المظهر المفضل لديك",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val currentThemeMode by viewModel.themeMode.collectAsState()

                    Text(
                        text = "الوضع (ليلي / نهاري)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf(
                            Triple("light", "نهاري", Icons.Filled.WbSunny),
                            Triple("dark", "ليلي", Icons.Filled.NightsStay),
                            Triple("system", "تلقائي", Icons.Filled.Settings)
                        )
                        modes.forEach { (modeKey, modeName, icon) ->
                            val isModeSelected = currentThemeMode == modeKey
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isModeSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.setThemeMode(modeKey)
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isModeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = modeName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isModeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "مظهر الألوان المفضل",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    val currentSelectedTheme by viewModel.selectedTheme.collectAsState()

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppTheme.values().forEach { theme ->
                            val isSelected = currentSelectedTheme == theme
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        viewModel.setTheme(theme)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(theme.primaryColor)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1.0f)) {
                                    Text(
                                        text = theme.displayNameAr,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = theme.displayNameEn,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(
                        onClick = { showThemeDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("إغلاق", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GOOD EVENING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { launcher.launch(readAudioPermission) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan Storage",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .background(Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                                .clickable { showThemeDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Change Theme",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp).align(Alignment.Center)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Modern Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setQuery(it) },
                    placeholder = { Text(stringResource(R.string.search_hint), color = Color.White.copy(alpha = 0.3f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                    ),
                    singleLine = true
                )
            }
        }
    ) { innerPadding ->
        if (searchQuery.isNotEmpty()) {
            SearchExperienceView(viewModel = viewModel, innerPadding = innerPadding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = 90.dp) // Leave screen spacing for active bottom player banner
            ) {
            // Horizontal Categories Selector (Visual Filter Buttons)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val subTabs = listOf(
                        "all_songs" to R.string.library_all_songs,
                        "albums" to R.string.library_albums,
                        "artists" to R.string.library_artists,
                        "playlists" to R.string.library_playlists,
                        "folders" to R.string.library_folders,
                        "favorites" to R.string.library_favorites,
                        "recently_played" to R.string.library_recent
                    )
                    items(subTabs) { tab ->
                        val isSelected = activeSubTab == tab.first
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) NeonCyan
                                    else ObsidianCard
                                )
                                .clickable { activeSubTab = tab.first }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = stringResource(tab.second),
                                color = if (isSelected) VibrantLilac else MaterialTheme.colorScheme.onBackground,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Optional Onboarding/Empty Hint
            if (songsList.isEmpty() && !isScanning) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Empty music list",
                            tint = NeonCyan,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_songs),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_songs_desc),
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { launcher.launch(readAudioPermission) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text(stringResource(R.string.grant_permission), color = ObsidianDark)
                        }
                    }
                }
            }

            // MAIN SECTIONS SWITCHER based on category selector
            when (activeSubTab) {
                "all_songs" -> {
                    if (searchQuery.isNotEmpty()) {
                        item {
                            Text(
                                text = "Search Results (${filteredSongs.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        items(filteredSongs) { song ->
                            SongItem(song = song, onClick = {
                                val idx = filteredSongs.indexOfFirst { it.id == song.id }
                                viewModel.playSong(filteredSongs, idx, context)
                            }, onFavoriteToggle = { viewModel.toggleFavorite(song) })
                        }
                    } else {
                        item {
                            Text(
                                text = "All Local Tracks (${songsList.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        items(songsList) { song ->
                            SongItem(song = song, onClick = {
                                val idx = songsList.indexOfFirst { it.id == song.id }
                                viewModel.playSong(songsList, idx, context)
                            }, onFavoriteToggle = { viewModel.toggleFavorite(song) })
                        }
                    }
                }

                "albums" -> {
                    item {
                        FlowRowLayout(albumsMap.keys.toList()) { album ->
                            LibraryGridCard(
                                title = album,
                                subtitle = "${albumsMap[album]?.size ?: 0} tracks",
                                artUrl = albumsMap[album]?.firstOrNull()?.albumArtUri,
                                onClick = { viewModel.navigateTo(Screen.AlbumDetails(album)) }
                            )
                        }
                    }
                }

                "artists" -> {
                    item {
                        FlowRowLayout(artistsMap.keys.toList()) { artist ->
                            LibraryGridCard(
                                title = artist,
                                subtitle = "${artistsMap[artist]?.size ?: 0} tracks",
                                artUrl = artistsMap[artist]?.firstOrNull()?.albumArtUri,
                                onClick = { viewModel.navigateTo(Screen.ArtistDetails(artist)) }
                            )
                        }
                    }
                }

                "folders" -> {
                    item {
                        FlowRowLayout(foldersMap.keys.toList()) { folder ->
                            LibraryGridCard(
                                title = folder,
                                subtitle = "${foldersMap[folder]?.size ?: 0} tracks",
                                icon = Icons.Default.Folder,
                                onClick = { viewModel.navigateTo(Screen.FolderDetails(folder)) }
                            )
                        }
                    }
                }

                "recently_played" -> {
                    if (recentSongs.isEmpty()) {
                        item { EmptyTabStateHint(stringResource(R.string.empty_recent)) }
                    } else {
                        items(recentSongs) { song ->
                            SongItem(song = song, onClick = {
                                val idx = recentSongs.indexOfFirst { it.id == song.id }
                                viewModel.playSong(recentSongs, idx, context)
                            }, onFavoriteToggle = { viewModel.toggleFavorite(song) })
                        }
                    }
                }

                "favorites" -> {
                    if (favoritesList.isEmpty()) {
                        item { EmptyTabStateHint(stringResource(R.string.empty_favorites)) }
                    } else {
                        items(favoritesList) { song ->
                            SongItem(song = song, onClick = {
                                val idx = favoritesList.indexOfFirst { it.id == song.id }
                                viewModel.playSong(favoritesList, idx, context)
                            }, onFavoriteToggle = { viewModel.toggleFavorite(song) })
                        }
                    }
                }

                "playlists" -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Playlists",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            TextButton(onClick = { viewModel.showCreatePlaylistDialog.value = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Playlist", tint = NeonCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.playlist_create), color = NeonCyan)
                            }
                        }
                    }

                    if (playlistsList.isEmpty()) {
                        item { EmptyTabStateHint(stringResource(R.string.empty_playlists)) }
                    } else {
                        items(playlistsList) { playlist ->
                            PlaylistItemRow(
                                title = playlist.name,
                                isSmart = playlist.isSmart,
                                smartType = playlist.smartType,
                                onClick = { viewModel.navigateTo(Screen.PlaylistDetails(playlist.id, playlist.name)) },
                                onDelete = { viewModel.deletePlaylist(playlist.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// Minimal Flow layout wrapper to keep layout simple and compile-safe on 100% Android Compose
@Composable
fun FlowRowLayout(keys: List<String>, content: @Composable (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        val chunkedKeys = keys.chunked(2)
        chunkedKeys.forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        content(item)
                    }
                }
                if (pair.size < 2) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ==========================================
// 3. MINI PLAYER COMPONENT
// ==========================================
@Composable
fun MiniPlayer(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progressMs by viewModel.progressMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()

    var showFullPlayer by remember { mutableStateOf(false) }

    if (currentSong == null) return

    val progressFraction = if (durationMs > 0) progressMs.toFloat() / durationMs else 0f

    // Animated full-screen sliding player overlay
    AnimatedVisibility(
        visible = showFullPlayer,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(400, easing = EaseOutExpo)
        )
    ) {
        ExpandedPlayerScreen(
            viewModel = viewModel,
            onCollapse = { showFullPlayer = false }
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showFullPlayer = true }
            .testTag("mini_player")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glow art container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (currentSong!!.albumArtUri != null) {
                        AsyncImage(
                            model = currentSong!!.albumArtUri,
                            contentDescription = "Artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(NeonCyan, VibrantLilac))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = ObsidianDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong!!.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong!!.artist,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.next() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Slim progress indicator line at the base of the miniplayer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .background(Brush.horizontalGradient(listOf(NeonCyan, VibrantLilac)))
                )
            }
        }
    }
}

// ==========================================
// 4. FULL EXPANDED MUSIC PLAYER VIEW
// ==========================================
@Composable
fun ExpandedPlayerScreen(
    viewModel: MainViewModel,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progressMs by viewModel.progressMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val sleepTimerRes by viewModel.sleepTimerMinutes.collectAsState()
    val eqEnabled by viewModel.equalizerEnabled.collectAsState()
    val eqBands by viewModel.equalizerBands.collectAsState()

    var activeExtraPanel by remember { mutableStateOf("none") } // "none", "lyrics", "equalizer", "speed", "timer"

    if (currentSong == null) return

    val formattedProgress = formatTime(progressMs)
    val formattedDuration = formatTime(durationMs)
    val sliderValue = if (durationMs > 0) progressMs.toFloat() / durationMs else 0f

    // Animated artwork ring rotation based on playback status
    val infiniteTransition = rememberInfiniteTransition(label = "ArtworkSpin")
    val spinningAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ArtworkSpin"
    )

    Scaffold(
        containerColor = ObsidianDark,
        modifier = Modifier
            .fillMaxSize()
            .testTag("expanded_player"),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(
                    text = stringResource(R.string.tab_home),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Row {
                    // Equalizer toggle shortcut
                    IconButton(onClick = { activeExtraPanel = if (activeExtraPanel == "equalizer") "none" else "equalizer" }) {
                        Icon(Icons.Default.Equalizer, contentDescription = "Equalizer", tint = if (eqEnabled) NeonCyan else Color.White)
                    }
                    // Share shortcut
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Listening to '${currentSong!!.title}' by '${currentSong!!.artist}' using Melodix Player! \uD83C\uDFB5")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Track Details"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // CENTRAL VISUAL AREA (Artwork or Secondary Panel)
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (activeExtraPanel) {
                    "none" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Glowing Rotating Album Vinyl Frame
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(280.dp)
                                    .rotate(if (isPlaying) spinningAngle else 0f)
                            ) {
                                // Outer Neon glow halo
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(NeonCyan, VibrantLilac, HotPink, NeonCyan),
                                            center = Offset(size.width / 2, size.height / 2)
                                        ),
                                        style = Stroke(width = 8f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(255.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                ) {
                                    if (currentSong!!.albumArtUri != null) {
                                        AsyncImage(
                                            model = currentSong!!.albumArtUri,
                                            contentDescription = "Album Art",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Brush.radialGradient(listOf(NeonCyan, ObsidianDark))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(72.dp))
                                        }
                                    }
                                }
                                // Center record pinhole
                                Spacer(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ObsidianDark)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Interactive Wave Visualizer (Dancing waves drawn on Canvas)
                            CanvasAudioVisualizer(isPlaying = isPlaying)
                        }
                    }

                    "lyrics" -> LyricsScreen { activeExtraPanel = "none" }
                    "equalizer" -> EqualizerSheet(viewModel = viewModel, eqEnabled = eqEnabled, eqBands = eqBands) { activeExtraPanel = "none" }
                    "speed" -> SpeedSpeedControl(viewModel = viewModel, activeSpeed = speed) { activeExtraPanel = "none" }
                    "timer" -> SleepTimerSheet(viewModel = viewModel, activeTimer = sleepTimerRes) { activeExtraPanel = "none" }
                }
            }

            // CONTROLS & SONG INFO SEGMENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Song details row with favorite toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong!!.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentSong!!.artist,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Real-time favoritism toggle from database
                    val isFavorite by viewModel.isFavoriteFlow(currentSong!!.id).collectAsState(initial = false)
                    IconButton(
                        onClick = { viewModel.toggleFavorite(currentSong!!) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (isFavorite) HotPink else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Timeline Track slider controls
                Slider(
                    value = sliderValue,
                    onValueChange = { newVal ->
                        val targetMs = (newVal * durationMs).toLong()
                        viewModel.seekTo(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formattedProgress, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(text = formattedDuration, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary media board control keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Toggle
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleMode) NeonCyan else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Skip Back
                    IconButton(onClick = { viewModel.previous() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    // Circular Giant Glowing Play Pause key
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(NeonCyan, VibrantLilac))
                            )
                            .clickable { viewModel.togglePlayPause() }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "PlayPauseState",
                            tint = ObsidianDark,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Skip Next
                    IconButton(onClick = { viewModel.next() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    // Repeat State cyclical button
                    IconButton(onClick = { viewModel.nextRepeatMode() }) {
                        val tint = if (repeatMode != com.example.service.PlaybackRepeatMode.NONE) NeonCyan else Color.White.copy(alpha = 0.3f)
                        val icon = when (repeatMode) {
                            com.example.service.PlaybackRepeatMode.ONE -> Icons.Default.Refresh
                            else -> Icons.Default.Repeat
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom shelf quick menus (Lyrics toggler, Play speed, Sleep timer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { activeExtraPanel = if (activeExtraPanel == "lyrics") "none" else "lyrics" }) {
                        Icon(Icons.Default.Lyrics, contentDescription = null, tint = if (activeExtraPanel == "lyrics") NeonCyan else Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.lyrics), color = if (activeExtraPanel == "lyrics") NeonCyan else Color.White, fontSize = 12.sp)
                    }

                    TextButton(onClick = { activeExtraPanel = if (activeExtraPanel == "speed") "none" else "speed" }) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = if (activeExtraPanel == "speed") NeonCyan else Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.playback_speed), color = if (activeExtraPanel == "speed") NeonCyan else Color.White, fontSize = 12.sp)
                    }

                    TextButton(onClick = { activeExtraPanel = if (activeExtraPanel == "timer") "none" else "timer" }) {
                        val active = sleepTimerRes != null
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = if (active) NeonCyan else Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (active) "${sleepTimerRes}m" else stringResource(R.string.sleep_timer),
                            color = if (active) NeonCyan else Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. CANVAS AUDIO VISUALIZER
// ==========================================
@Composable
fun CanvasAudioVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizersTransition")
    
    // Wave motion delta parameters
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "WaveValue"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .padding(vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barsCount = 28
            val barSpacing = 6f
            val baseBarWidth = (width - (barsCount - 1) * barSpacing) / barsCount

            for (i in 0 until barsCount) {
                // Calculate amplitude wave height based on index, play state, and running timer offsets
                val ampFactor = if (isPlaying) {
                    0.25f + 0.65f * kotlin.math.abs(kotlin.math.sin((i.toFloat() / barsCount) * Math.PI.toFloat() * 3f + waveOffset))
                } else {
                    0.08f + 0.05f * kotlin.math.abs(kotlin.math.sin((i.toFloat() / barsCount) * Math.PI.toFloat() * 2f))
                }
                
                val barHeight = (height * ampFactor).toFloat()
                val x = (i * (baseBarWidth + barSpacing)).toFloat()
                val y = (height - barHeight).toFloat()

                // Draw gradient filled capsule bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonCyan, VibrantLilac)
                    ),
                    topLeft = Offset(x, y),
                    size = Size(baseBarWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
        }
    }
}

// ==========================================
// 6. DETAILED DRAWER PRESETS SHEETS
// ==========================================

// --- LYRICS PANEL ---
@Composable
fun LyricsScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.02f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.lyrics_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Large high-contrast scrollable text frame
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            val lines = listOf(
                "Walking down the neon highway",
                "Stars reflecting on your face",
                "Analog synth plays in a dynamic cascade",
                "Lost inside a vibrant, retro space.",
                "",
                "Let the low beats take you over",
                "Rhythms pulsing through the air",
                "Melodix brings the digital closer",
                "Vibrations moving everywhere.",
                "",
                "[Chorus]",
                "Oh, neon light, wash out the pain",
                "Dancing inside the holographic rain",
                "Singing along with soundwave keys",
                "Ethereal walks inside the cosmic breeze..."
            )
            items(lines) { line ->
                val queryHighlight = line.startsWith("[")
                Text(
                    text = line,
                    fontSize = if (queryHighlight) 18.sp else 21.sp,
                    fontWeight = if (queryHighlight) FontWeight.Bold else FontWeight.Medium,
                    color = if (queryHighlight) HotPink else Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 32.sp
                )
            }
        }
    }
}

// --- EQUALIZER SHEET ---
@Composable
fun EqualizerSheet(
    viewModel: MainViewModel,
    eqEnabled: Boolean,
    eqBands: List<Int>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.02f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stringResource(R.string.equalizer_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Text("Native Filter Presets", fontSize = 11.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5 sliding bars mapping Bass, Mid-bass, Vocal, Treble, Presence
        val bandNames = listOf(
            stringResource(R.string.eq_bass) to "60Hz",
            stringResource(R.string.eq_mid) to "230Hz",
            stringResource(R.string.eq_vocal) to "910Hz",
            stringResource(R.string.eq_treble) to "4kHz",
            stringResource(R.string.eq_presence) to "14kHz"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(5) { i ->
                val bandInfo = bandNames[i]
                val currentDb = eqBands.getOrElse(i) { 0 }
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = bandInfo.first, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "${currentDb}dB (${bandInfo.second})", fontSize = 12.sp, color = NeonCyan)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = (currentDb + 15).toFloat() / 30f,
                        onValueChange = { newVal ->
                            if (eqEnabled) {
                                val targetDb = (newVal * 30f - 15f).toInt()
                                viewModel.setEqualizerBandLevel(i, targetDb)
                            }
                        },
                        enabled = eqEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = if (eqEnabled) NeonCyan else Color.DarkGray,
                            activeTrackColor = if (eqEnabled) NeonCyan else Color.DarkGray
                        )
                    )
                }
            }
        }
    }
}

// --- SPEED CONTROL ---
@Composable
fun SpeedSpeedControl(
    viewModel: MainViewModel,
    activeSpeed: Float,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.02f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.playback_speed_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Current Speed: ${activeSpeed}x",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            speeds.chunked(3).forEach { rowSpeeds ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSpeeds.forEach { sp ->
                        val isSelected = activeSpeed == sp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonCyan else Color.White.copy(alpha = 0.05f))
                                .clickable { viewModel.setSpeed(sp) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sp}x",
                                color = if (isSelected) ObsidianDark else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SLEEP TIMER SHEET ---
@Composable
fun SleepTimerSheet(
    viewModel: MainViewModel,
    activeTimer: Int?,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.02f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.sleep_timer_select), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (activeTimer != null) "Time remaining: $activeTimer mins" else "No Sleep Timer Active",
            fontSize = 18.sp,
            color = if (activeTimer != null) NeonCyan else Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        val timerPresets = listOf(5, 10, 15, 30, 45, 60)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            timerPresets.chunked(3).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { time ->
                        val isSelected = activeTimer == time
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonCyan else Color.White.copy(alpha = 0.05f))
                                .clickable { viewModel.startSleepTimer(time) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$time min",
                                color = if (isSelected) ObsidianDark else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (activeTimer != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.cancelSleepTimer() },
                colors = ButtonDefaults.buttonColors(containerColor = HotPink),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_cancel).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 7. COMPONENT ROW CARDS RENDERERS
// ==========================================

// --- INDIVIDUAL SONG ROW LIST ITEM ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: Song,
    isMusicStream: Boolean = false,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onFavoriteToggle
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tiny artwork
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(NeonCyan, VibrantLilac))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = ObsidianDark)
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isMusicStream) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1DB954).copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "بث مباشر",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1DB954)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•  " + song.album,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.35f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = formatTime(song.duration),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// --- LIBRARY FOLDER/ARTIST GRID CARD ---
@Composable
fun LibraryGridCard(
    title: String,
    subtitle: String,
    artUrl: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (artUrl != null) {
                    AsyncImage(
                        model = artUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = icon ?: Icons.Default.Album,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- PLAYLIST ROW ITEM ---
@Composable
fun PlaylistItemRow(
    title: String,
    isSmart: Boolean = false,
    smartType: String? = null,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSmart) Brush.horizontalGradient(listOf(VibrantLilac, NeonCyan))
                            else Brush.horizontalGradient(listOf(NeonCyan, VibrantLilac))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSmart) Icons.Default.AutoAwesome else Icons.Default.List,
                        contentDescription = "Playlist",
                        tint = ObsidianDark
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isSmart) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VibrantLilac.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "SMART",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VibrantLilac
                                )
                            }
                        }
                    }
                    if (isSmart) {
                        val criteriaLabel = when (smartType) {
                            "recently_played" -> "Recently Played Tracker"
                            "most_played" -> "Most Played Tracker"
                            "added_range" -> "Recently Added Tracker"
                            else -> "Dynamic criteria"
                        }
                        Text(
                            text = criteriaLabel,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = HotPink)
            }
        }
    }
}

// ==========================================
// 8. DETAIL SUB-SCREENS
// ==========================================

// --- PLAYLIST DETAILS VIEWER ---
@Composable
fun PlaylistDetailsScreen(
    viewModel: MainViewModel,
    playlistId: Long,
    playlistName: String
) {
    val context = LocalContext.current
    val playlistSongs by viewModel.getSongsForPlaylist(playlistId).collectAsState(initial = emptyList())
    val songsList by viewModel.allSongs.collectAsState()

    val playlistsList by viewModel.playlists.collectAsState(initial = emptyList())
    val currentPlaylist = playlistsList.find { it.id == playlistId }
    val isSmart = currentPlaylist?.isSmart == true
    val smartType = currentPlaylist?.smartType ?: ""

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxHeight(0.7f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add Songs to Playlist",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        modifier = Modifier.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(songsList) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlistId, song)
                                        showAddDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    if (song.albumArtUri != null) {
                                        AsyncImage(
                                            model = song.albumArtUri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(song.artist, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { showAddDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.action_cancel), color = Color.Gray)
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = ObsidianDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = playlistName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        if (isSmart) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VibrantLilac.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Smart",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VibrantLilac
                                )
                            }
                        }
                    }
                    val subtitle = if (isSmart) {
                        val criteriaLabel = when (smartType) {
                            "most_played" -> "الأكثر تشغيلاً"
                            "recently_played" -> "المشغلة مؤخراً"
                            "added_range" -> "المضافة حديثاً"
                            else -> "ديناميكية"
                        }
                        "ذكية ($criteriaLabel) • ${playlistSongs.size} أغنية"
                    } else {
                        "${playlistSongs.size} أغنية"
                    }
                    Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                
                // Export M3U Button
                if (playlistSongs.isNotEmpty()) {
                    IconButton(onClick = {
                        val m3uText = viewModel.exportM3U(playlistSongs)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("M3U Playlist", m3uText)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "تم تصدير قائمة M3U إلى الحافظة بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "تصدير M3U", tint = VibrantLilac)
                    }
                }

                if (!isSmart) {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة مسار موسيقي", tint = NeonCyan)
                    }
                }
            }
        }
    ) { innerPadding ->
        if (playlistSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("This playlist is empty", color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Add Songs", color = ObsidianDark)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = 90.dp)
            ) {
                items(playlistSongs) { song ->
                    // Customize list row with long press deletions
                    SwipeToDeleteSongRow(
                        song = song,
                        onClick = {
                            val idx = playlistSongs.indexOfFirst { it.id == song.id }
                            viewModel.playSong(playlistSongs, idx, context)
                        },
                        onDelete = { viewModel.removeSongFromPlaylist(playlistId, song.id) }
                    )
                }
            }
        }
    }
}

// Custom Row with rapid tap removal action
@Composable
fun SwipeToDeleteSongRow(
    song: Song,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(NeonCyan, VibrantLilac))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = ObsidianDark)
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                Text(song.artist, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.RemoveCircle, contentDescription = "Remove", tint = HotPink)
            }
        }
    }
}

// --- GENERAL FILTERED GROUP DETAIL SCREEN (ALBUM, ARTIST, FOLDER) ---
@Composable
fun LibraryGroupDetailScreen(
    viewModel: MainViewModel,
    title: String,
    songsFlow: Flow<List<Song>>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val groupSongs by songsFlow.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = ObsidianDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "${groupSongs.size} songs", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 90.dp)
        ) {
            items(groupSongs) { song ->
                SongItem(
                    song = song,
                    onClick = {
                        val idx = groupSongs.indexOfFirst { it.id == song.id }
                        viewModel.playSong(groupSongs, idx, context)
                    },
                    onFavoriteToggle = { viewModel.toggleFavorite(song) }
                )
            }
        }
    }
}


// ==========================================
// 9. UTILITY RE-USE HELPERS
// ==========================================

@Composable
fun EmptyTabStateHint(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = text, color = Color.Gray, fontSize = 14.sp)
    }
}

private fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

// ==========================================
// 10. REAL-TIME SEARCH EXPERIENCE WIDGETS
// ==========================================

@Composable
fun SearchExperienceView(
    viewModel: MainViewModel,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchType by viewModel.searchFilterType.collectAsState()
    val searchSort by viewModel.searchSortOrder.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val results by viewModel.filteredAndSortedSearchResults.collectAsState()

    val spotifyAuthToken by viewModel.spotifyAuthToken.collectAsState()
    val spotifyUser by viewModel.spotifyUser.collectAsState()
    val isSpotifyAuthenticating by viewModel.isSpotifyAuthenticating.collectAsState()
    val isSpotifySearching by viewModel.isSpotifySearching.collectAsState()

    var showSpotifyLoginDialog by remember { mutableStateOf(false) }
    var spotifyUsernameInput by remember { mutableStateOf("") }

    if (showSpotifyLoginDialog) {
        Dialog(onDismissRequest = { showSpotifyLoginDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "ربط بث سبوتيفاي المباشر",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1DB954)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هذا يحاكي ميزة الاتصال المباشر وتشغيل الموسيقى عبر سبوتيفاي المميز. يمكنك البحث والتشغيل الفوري.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = spotifyUsernameInput,
                        onValueChange = { spotifyUsernameInput = it },
                        placeholder = { Text("أدخل اسم مستخدم سبوتيفاي") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSpotifyLoginDialog = false }) {
                            Text("إلغاء", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.loginToSpotify(spotifyUsernameInput)
                                showSpotifyLoginDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                        ) {
                            Text("اتصال", color = ObsidianDark)
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(bottom = 90.dp)
    ) {
        // --- 1. Real-time Auto-complete Suggestions ---
        if (suggestions.isNotEmpty()) {
            item {
                Text(
                    text = "الاقتراحات",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(suggestions) { suggestion ->
                        SuggestionChip(text = suggestion, onClick = {
                            viewModel.setQuery(suggestion)
                        })
                    }
                }
            }
        }

        // --- 2. Live Type Filter and Order Action Row ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الفرز والتصفية المرئية",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )

                    // Alphabetical toggle
                    TextButton(
                        onClick = {
                            viewModel.searchSortOrder.value = if (searchSort == "ASC") "DESC" else "ASC"
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (searchSort == "ASC") Icons.Default.SortByAlpha else Icons.Default.Sort,
                            contentDescription = "ترتيب",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (searchSort == "ASC") "أ-ي" else "ي-أ",
                            fontSize = 12.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filterTypes = listOf(
                        "ALL" to "الكل",
                        "SONG" to "الأغاني",
                        "ALBUM" to "الألبومات",
                        "ARTIST" to "الفنانون"
                    )
                    filterTypes.forEach { (key, label) ->
                        val isSel = searchType == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) NeonCyan else ObsidianCard)
                                .clickable { viewModel.searchFilterType.value = key }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) ObsidianDark else Color.White
                            )
                        }
                    }
                }
            }
        }

        // --- 3. Spotify Premium Live Stream Card ---
        item {
            SpotifyInteractiveCard(
                spotifyAuthToken = spotifyAuthToken,
                spotifyUser = spotifyUser,
                isAuthenticating = isSpotifyAuthenticating,
                onConnectClick = { showSpotifyLoginDialog = true },
                onDisconnectClick = { viewModel.logoutSpotify() }
            )
        }

        // --- 4. Search Results Display list ---
        if (results.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SentimentDissatisfied,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isSpotifySearching) "جاري البحث في الكتالوج الافتراضي وسبوتيفاي..." else " لم يتم العثور على أغانٍ أو نتائج مطابقة",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "التطابقات الموسيقية (${results.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            itemsIndexed(results) { idx, song ->
                SongItem(
                    song = song,
                    isMusicStream = song.id.startsWith("spotify_"),
                    onClick = {
                        viewModel.playSong(results, idx, context)
                    },
                    onFavoriteToggle = {
                        viewModel.toggleFavorite(song)
                    }
                )
            }
        }
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(10.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontSize = 11.sp, color = Color.White)
        }
    }
}

@Composable
fun SpotifyInteractiveCard(
    spotifyAuthToken: String?,
    spotifyUser: String?,
    isAuthenticating: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (spotifyAuthToken != null) Color(0x1F1DB954) else ObsidianSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1DB954)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = ObsidianDark,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "بث سبوتيفاي",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1DB954)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (spotifyAuthToken != null) {
                    Text(
                        text = "تم تسجيل الدخول كـ: $spotifyUser",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Text(
                        text = "تم تمكين البث الموسيقي المباشر • تشغيل سلس",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "افتح إمكانية البحث والبث المباشر للأغاني.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            if (isAuthenticating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF1DB954),
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick = { if (spotifyAuthToken != null) onDisconnectClick() else onConnectClick() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (spotifyAuthToken != null) Color.Red.copy(alpha = 0.2f) else Color(0xFF1DB954)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (spotifyAuthToken != null) "فصل" else "اتصال",
                        fontSize = 11.sp,
                        color = if (spotifyAuthToken != null) Color.White else ObsidianDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

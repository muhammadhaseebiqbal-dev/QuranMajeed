package com.haseeb.quranapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.haseeb.quranapp.data.local.entity.SurahEntity
import com.haseeb.quranapp.data.download.SurahDownloadManager
import com.haseeb.quranapp.data.download.DownloadStatus
import com.haseeb.quranapp.data.download.DownloadState
import com.haseeb.quranapp.ui.audio.AudioUtils
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onSurahClick: (Int) -> Unit,
    onJuzClick: (Int) -> Unit, 
    onResumeClick: (surahId: Int, ayahNum: Int) -> Unit, // Add callback
    onAskClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val surahs by viewModel.surahs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val bookmarkedSurahId by viewModel.bookmarkedSurahId.collectAsState()
    val bookmarkedAyahNum by viewModel.bookmarkedAyahNum.collectAsState()

    // Download Manager (injected via HomeViewModel)
    val downloadManager = viewModel.downloadManager
    val downloadStates by downloadManager.downloadStates.collectAsState()
    
    // Refresh bookmarks on resume
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshBookmarks()
        // Auto-download removed

    }

    // Tab and Pager State
    val tabs = listOf("Surah", "Juz")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Load Hadith (Bilingual)
    val context = androidx.compose.ui.platform.LocalContext.current
    val hadith = androidx.compose.runtime.remember {
        try {
            val inputStream = context.assets.open("hadiths.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val json = String(buffer, java.nio.charset.Charset.forName("UTF-8"))
            val array = org.json.JSONArray(json)
            val randomIndex = (0 until array.length()).random()
            val obj = array.getJSONObject(randomIndex)
            Triple(obj.getString("text"), obj.optString("text_ur", ""), obj.getString("source"))
        } catch (e: Exception) {
            Triple("Be kind to your parents.", "اپنے والدین سے حسن سلوک کرو۔", "Quran 17:23")
        }
    }

    Scaffold(
        floatingActionButton = {
            val audioVm = com.haseeb.quranapp.ui.audio.AudioUtils.getViewModel()
            val isGlobalPlaying by audioVm.isPlaying.collectAsState()
            val activeSurahId by audioVm.currentSurahId.collectAsState()
            val currentAyahIndex by audioVm.currentAyahIndex.collectAsState()
            
            if (activeSurahId != null) {
                ExtendedFloatingActionButton(
                    onClick = { onResumeClick(activeSurahId!!, currentAyahIndex + 1) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = {
                        IconButton(onClick = { audioVm.togglePlayPause() }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (isGlobalPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                contentDescription = if (isGlobalPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    text = {
                        val sName = surahs.find { it.id == activeSurahId }?.nameSimple ?: "Surah"
                        Text("Active: $sName ${currentAyahIndex + 1}", style = MaterialTheme.typography.labelLarge)
                    }
                )
            }
        },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Quran Majeed", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Resume Reading Button
                if (bookmarkedSurahId != -1 && bookmarkedAyahNum != -1) {
                    val surahName = surahs.find { it.id == bookmarkedSurahId }?.nameSimple ?: "Surah $bookmarkedSurahId"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { onResumeClick(bookmarkedSurahId, bookmarkedAyahNum) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Resume Reading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text("$surahName - Ayah $bookmarkedAyahNum", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                // Daily Hadith Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Hadith | حدیثِ مبارکہ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${hadith.first}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        if (hadith.second.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"${hadith.second}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "- ${hadith.third}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
                
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { 
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && surahs.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page == 0) {
                        // Surah List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        items(
                                items = surahs,
                                key = { it.id }
                            ) { surah ->
                                SurahItem(
                                    surah = surah,
                                    onClick = { onSurahClick(surah.id) },
                                    downloadManager = downloadManager
                                )
                            }
                        }
                    } else {
                        // Juz List (1 to 30)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(30) { index ->
                                val juzId = index + 1
                                JuzItem(
                                    juzId = juzId,
                                    onClick = { onJuzClick(juzId) },
                                    downloadManager = downloadManager
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JuzItem(juzId: Int, onClick: () -> Unit, downloadManager: SurahDownloadManager? = null) {
    val juzNames = listOf(
        "Alm", "Sayaqool", "Tilkal Rusul", "Lan Tana Loo", "Wal Mohsanat",
        "La Yuhibbullah", "Wa Iza Samiu", "Wa Lau Annana", "Qalal Malao", "Wa A'lamu",
        "Yatazeroon", "Wa Mamin Da'abat", "Wa Ma Ubrioo", "Rubama", "Subhanallazi",
        "Qal Alam", "Iqtaraba", "Qadd Aflaha", "Wa Qalallazina", "A'man Khalaqa",
        "Utlu Ma Oohi", "Wa Manyaqnut", "Wa Mali", "Faman Azlam", "Elahe Yuruddo",
        "Ha'a Meem", "Qala Fama Khatbukum", "Qadd Sami Allah", "Tabarakallazi", "Amma Yatasa'aloon"
    )

    val juzArabicNames = listOf(
        "الم", "سيقول", "تلك الرسل", "لن تنالوا", "والمحصنات",
        "لا يحب الله", "وإذا سمعوا", "ولو أننا", "قال الملأ", "واعلموا",
        "يعتذرون", "وما من دابة", "وما أبرئ", "ربما", "سبحان الذي",
        "قال ألم", "اقترب", "قد أفلح", "وقال الذين", "أمن خلق",
        "اتل ما أوحى", "ومن يقنت", "وما لي", "فمن أظلم", "إليه يرد",
        "حم", "قال فما خطبكم", "قد سمع الله", "تبارك الذي", "عم يتساءلون"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Text(
                    text = juzId.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Juz $juzId",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = juzNames.getOrElse(juzId - 1) { "Juz $juzId" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arabic Name
            Text(
                text = juzArabicNames.getOrElse(juzId - 1) { "" },
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Download Button would require mapping Juz to Surahs, so we skip for Juz
        }
    }
}

@Composable
fun SurahItem(surah: SurahEntity, onClick: () -> Unit, downloadManager: SurahDownloadManager? = null) {
    val downloadStates = if (downloadManager != null) {
        downloadManager.downloadStates.collectAsState().value
    } else {
        emptyMap()
    }
    val state = downloadStates[surah.id]
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = surah.id.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            // English Name & Verses
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameSimple,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${surah.versesCount} Verses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arabic Name
            Text(
                text = surah.nameArabic,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Download Button / Progress / Checkmark
            if (downloadManager != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp)
                ) {
                    when (state?.status) {
                        DownloadStatus.DOWNLOADING -> {
                            com.haseeb.quranapp.ui.components.WavyCircularProgress(
                                progress = state.progress,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                progressColor = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        DownloadStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> {
                            IconButton(
                                onClick = { downloadManager.downloadSurah(surah.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Surah",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

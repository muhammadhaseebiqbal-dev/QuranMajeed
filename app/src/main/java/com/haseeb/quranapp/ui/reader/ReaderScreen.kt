package com.haseeb.quranapp.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import android.widget.TextView
import androidx.hilt.navigation.compose.hiltViewModel
import com.haseeb.quranapp.data.local.entity.AyahEntity

import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    scrollToAyah: Int? = null
) {
    val ayahs by viewModel.ayahs.collectAsState()
    val tafsirText by viewModel.tafsirState.collectAsState()
    
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Logic to show sheet when tafsir is loaded
    androidx.compose.runtime.LaunchedEffect(tafsirText) {
        if (tafsirText != null && !showBottomSheet) {
            showBottomSheet = true
        }
    }

    // Get Surah ID from ViewModel or passed args (simplified here to rely on ViewModel data or arg)
    val surahId = ayahs.firstOrNull()?.surahId ?: 1

    // Audio State
    // Audio State
    val currentSurahId by com.haseeb.quranapp.ui.audio.AudioUtils.getViewModel().currentSurahId.collectAsState()
    val isPlaying by com.haseeb.quranapp.ui.audio.AudioUtils.getViewModel().isPlaying.collectAsState()
    val audioViewModel = com.haseeb.quranapp.ui.audio.AudioUtils.getViewModel()
    val currentAyahIndex by audioViewModel.currentAyahIndex.collectAsState()

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(currentAyahIndex, currentSurahId) {
        if (currentSurahId == surahId && currentAyahIndex >= 0 && currentAyahIndex < ayahs.size) {
            // Scroll to the current playing ayah. Give a small offset if needed, but top is fine.
            listState.animateScrollToItem(currentAyahIndex)
        }
    }

    // Auto-scroll to bookmarked Ayah on load
    androidx.compose.runtime.LaunchedEffect(ayahs, scrollToAyah) {
        if (scrollToAyah != null && ayahs.isNotEmpty()) {
            val idx = ayahs.indexOfFirst { it.ayahNumber == scrollToAyah }
            if (idx != -1) {
                listState.scrollToItem(idx)
            }
        }
    }

    val bookmarkedSurahId by viewModel.bookmarkedSurahId.collectAsState()
    val bookmarkedAyahNum by viewModel.bookmarkedAyahNum.collectAsState()

    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Reader") }, 
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        sheetContent = {
            if (ayahs.isNotEmpty()) {
                val isExpanded = bottomSheetState.targetValue == SheetValue.Expanded
                com.haseeb.quranapp.ui.audio.AudioPlayerSheet(
                    surahId = surahId,
                    isExpanded = isExpanded
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        },
        sheetPeekHeight = if (ayahs.isNotEmpty()) 110.dp else 0.dp
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = ayahs,
                key = { it.id } 
            ) { ayah ->
                // Check if this ayah is currently playing
                // Note: currentAyahIndex from player is 0-indexed relative to the playlist.
                // The playlist is usually the entire surah.
                // So ayah.ayahNumber - 1 == index (approx, if we load full surah).
                val isCurrent = (currentSurahId == ayah.surahId) && (currentAyahIndex == (ayah.ayahNumber - 1))
                val isBookmarked = (bookmarkedSurahId == ayah.surahId && bookmarkedAyahNum == ayah.ayahNumber)
                
                AyahItem(
                    ayah = ayah,
                    isHighlight = isCurrent,
                    isBookmarked = isBookmarked,
                    onBookmarkClick = {
                        viewModel.toggleBookmark(ayah.surahId, ayah.ayahNumber)
                    },
                    onTafsirClick = {
                        viewModel.fetchTafsir(ayah.surahId, ayah.ayahNumber)
                        showBottomSheet = true
                    },
                    onPlayClick = {
                        val audioVm = audioViewModel
                        if (audioVm.currentSurahId.value != surahId) {
                            audioVm.playSurah(surahId, startAyahIndex = ayah.ayahNumber - 1)
                        } else {
                            audioVm.seekToAyah(ayah.ayahNumber - 1)
                        }
                    }
                )
            }
        }
        
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showBottomSheet = false 
                    viewModel.clearTafsir()
                },
                sheetState = sheetState
            ) {
                val scrollState = androidx.compose.foundation.rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Tafsir (Explanation)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (tafsirText == "Loading Tafsir...") {
                         CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        val tText = tafsirText ?: ""
                        // 160 = Urdu Ibn Kathir, 159 = Urdu Bayan ul Quran, 16 = Arabic Muyassar
                        val isRtlTafsir = viewModel.currentTafsirId in listOf(160, 159, 16)
                        
                        Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(scrollState)) {
                            val cleanText = tText.replace(Regex("""(?:\[\d+\]|\(\d+\))"""), "")
                            val fontColor = MaterialTheme.colorScheme.onSurface.toArgb()
                            
                            AndroidView(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                factory = { context ->
                                    TextView(context).apply {
                                        textSize = 18f
                                        setLineSpacing(0f, 1.4f)
                                    }
                                },
                                update = { textView ->
                                    textView.setTextColor(fontColor)
                                    textView.text = androidx.core.text.HtmlCompat.fromHtml(
                                        cleanText.replace("\n", "<br>"), 
                                        androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
                                    )
                                    textView.layoutDirection = if (isRtlTafsir) android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR
                                    textView.textAlignment = android.view.View.TEXT_ALIGNMENT_VIEW_START
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AyahItem(
    ayah: AyahEntity, 
    isHighlight: Boolean = false,
    isBookmarked: Boolean = false,
    onBookmarkClick: () -> Unit = {},
    onTafsirClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlight) 4.dp else 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Arabic Text
            Text(
                text = ayah.textUthmani,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif, 
                    lineHeight = 40.sp,
                    color = MaterialTheme.colorScheme.onSurface 
                ),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Translation
            if (!ayah.textTranslation.isNullOrEmpty()) {
                val transText = ayah.textTranslation.toString()
                // Simple Arabic/Urdu character detection
                val isUrdu = transText.any { it in '\u0600'..'\u06FF' }
                // Strip <sup> and other HTML tags returned by API
                val cleanTextHtml = androidx.core.text.HtmlCompat.fromHtml(
                    transText, 
                    androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
                ).toString()
                
                // Remove footnote numbers like [1], (1), or just digits left over from HTML stripping
                val cleanText = cleanTextHtml.replace(Regex("""(?:\[\d+\]|\(\d+\)|\b\d+\b)"""), "").replace(Regex("""\s+"""), " ").trim()

                Text(
                    text = cleanText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (isUrdu) TextAlign.Right else TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                 Text(
                    text = "Loading Translation...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats & Actions
            Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.CenterVertically
            ) {
                 Text(
                    text = "${ayah.surahId}:${ayah.ayahNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                 )
                
                // Tafsir and Bookmark Buttons
                Row {
                    IconButton(onClick = onPlayClick) {
                        Icon(
                            Icons.Default.PlayArrow, 
                            contentDescription = "Play", 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onBookmarkClick) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, 
                            contentDescription = "Bookmark", 
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onTafsirClick) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = "Tafsir", 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

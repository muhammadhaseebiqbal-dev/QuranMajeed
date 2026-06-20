package com.haseeb.quranapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val selectedReciter by viewModel.selectedReciterId.collectAsState()
    val selectedTranslation by viewModel.selectedTranslationId.collectAsState()
    val selectedTafsir by viewModel.selectedTafsirId.collectAsState()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Preferences", 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            SettingsCard(
                title = "Audio Reciter",
                description = "Choose your preferred reciter for audio playback.",
                icon = Icons.Rounded.Headset,
                options = viewModel.reciterOptions,
                selectedId = selectedReciter,
                onSelected = { viewModel.updateReciter(it) }
            )

            SettingsCard(
                title = "Translation Language",
                description = "Select the language for verse translations.",
                icon = Icons.Rounded.Translate,
                options = viewModel.translationOptions,
                selectedId = selectedTranslation,
                onSelected = { viewModel.updateTranslation(it) }
            )

            SettingsCard(
                title = "Tafsir Source",
                description = "Select the source for detailed exegesis.",
                icon = Icons.Rounded.MenuBook,
                options = viewModel.tafsirOptions,
                selectedId = selectedTafsir,
                onSelected = { viewModel.updateTafsir(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Info, 
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Translations and Tafsirs are bundled for instant offline reading. Downloading a Surah will now only fetch the Audio Recitation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2f
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector,
    options: List<SelectionOption>,
    selectedId: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.id == selectedId } ?: options.firstOrNull()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedOption?.name ?: "Select an option",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    option.name, 
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (option.id == selectedId) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            onClick = {
                                onSelected(option.id)
                                expanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

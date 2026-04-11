package com.haseeb.quranapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            SettingsDropdownMenu(
                title = "Audio Reciter",
                options = viewModel.reciterOptions,
                selectedId = selectedReciter,
                onSelected = { viewModel.updateReciter(it) }
            )

            SettingsDropdownMenu(
                title = "Translation Language",
                options = viewModel.translationOptions,
                selectedId = selectedTranslation,
                onSelected = { viewModel.updateTranslation(it) }
            )

            SettingsDropdownMenu(
                title = "Tafsir Source",
                options = viewModel.tafsirOptions,
                selectedId = selectedTafsir,
                onSelected = { viewModel.updateTafsir(it) }
            )

            Text(
                text = "Note: Translations and Tafsirs are bundled for instant offline reading. Downloading a Surah will now only fetch the Audio Recitation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdownMenu(
    title: String,
    options: List<SelectionOption>,
    selectedId: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.id == selectedId } ?: options.first()

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedOption.name,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                 leadingIcon = {
                     Icon(
                         imageVector = Icons.Default.AccountCircle,
                         contentDescription = null,
                         tint = MaterialTheme.colorScheme.primary
                     )
                 },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        leadingIcon = {
                             Icon(
                                 imageVector = Icons.Default.AccountCircle,
                                 contentDescription = null,
                                 tint = MaterialTheme.colorScheme.secondary
                             )
                        },
                        onClick = {
                            onSelected(option.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

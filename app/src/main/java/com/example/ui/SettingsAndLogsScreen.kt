package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ApiLogger
import com.example.util.LogEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndLogsScreen(
    viewModel: HomeworkViewModel,
    modifier: Modifier = Modifier
) {
    val logs by ApiLogger.logs.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()
    val currentPersonality by viewModel.tutorPersonality.collectAsState()
    val currentComplexity by viewModel.explanationComplexity.collectAsState()

    var apiKeyInput by remember { mutableStateOf(userApiKey) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("PrepAlly Preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Tweak your AI tutor parameters to fit your custom learning preferences.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        
        // Tutor Persona Style
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        "Tutor Persona Style",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val personalities = listOf("Balanced Tutor", "Socratic Guide", "Strict Examiner", "Casual Buddy")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        personalities.forEach { p ->
                            FilterChip(
                                selected = currentPersonality == p,
                                onClick = { viewModel.updatePersonality(p) },
                                label = { Text(p) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Explanation Complexity
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        "Explanation Complexity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val complexities = listOf("Detailed Step-by-Step", "Focus Formulas", "Quick Cheat Sheet")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        complexities.forEach { c ->
                            FilterChip(
                                selected = currentComplexity == c,
                                onClick = { viewModel.updateComplexity(c) },
                                label = { Text(c) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Wipe History Data
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            var showResetConfirmation by remember { mutableStateOf(false) }
            
            if (!showResetConfirmation) {
                OutlinedButton(
                    onClick = { showResetConfirmation = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("clear_all_history_trigger")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, "Sweep")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wipe All Tutoring Saves", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Delete alright? This will clean all offline homework records irreversibly.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(onClick = { showResetConfirmation = false }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    viewModel.clearAllHistory()
                                    showResetConfirmation = false
                                    android.widget.Toast.makeText(context, "Saves wiped cleanly!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Yes, Delete All")
                            }
                        }
                    }
                }
            }
        }

        // API Key Set
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        "Relay Network Configuration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Relay API Key") },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input_settings"),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.updateApiKey(apiKeyInput.trim()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Key")
                    }
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Diagnostic API Logs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = { 
                        val allLogs = logs.joinToString("\n\n") { "${it.formattedTime} [${it.level}] ${it.tag}: ${it.message}${if(it.details != null) "\n" + it.details else ""}" }
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, allLogs)
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Logs", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { ApiLogger.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Text("No network activity recorded.", color = Color.Gray, modifier = Modifier.padding(8.dp))
            }
        }

        items(logs) { log ->
            LogEntryCard(log)
        }
    }
}

@Composable
fun LogEntryCard(log: LogEntry) {
    var expanded by remember { mutableStateOf(false) }
    val color = when (log.level) {
        "ERROR" -> MaterialTheme.colorScheme.error
        "INFO" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (log.details != null) expanded = !expanded },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(log.tag, fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp)
                }
                Text(log.formattedTime, fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(log.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            
            if (log.details != null) {
                if (expanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = log.details,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

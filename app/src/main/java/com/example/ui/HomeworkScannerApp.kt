package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.ChatMessage
import com.example.data.HomeworkEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Defining beautiful subject configurations
data class SubjectTheme(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val containerColor: Color,
    val backgroundBrush: Brush
)

val Subjects = listOf(
    SubjectTheme(
        "Math", "Mathematics", Icons.Default.Calculate,
        Color(0xFF0288D1), Color(0xFFE1F5FE),
        Brush.horizontalGradient(listOf(Color(0xFF0288D1), Color(0xFF03A9F4)))
    ),
    SubjectTheme(
        "Science", "Science & Bio", Icons.Default.Science,
        Color(0xFF388E3C), Color(0xFFE8F5E9),
        Brush.horizontalGradient(listOf(Color(0xFF388E3C), Color(0xFF4CAF50)))
    ),
    SubjectTheme(
        "History", "History & Social", Icons.Default.HourglassEmpty,
        Color(0xFFF57C00), Color(0xFFFFF3E0),
        Brush.horizontalGradient(listOf(Color(0xFFF57C00), Color(0xFFFF9800)))
    ),
    SubjectTheme(
        "Literature", "Literature & Arts", Icons.Default.MenuBook,
        Color(0xFF7B1FA2), Color(0xFFF3E5F5),
        Brush.horizontalGradient(listOf(Color(0xFF7B1FA2), Color(0xFF9C27B0)))
    ),
    SubjectTheme(
        "Programming", "Coding & Logic", Icons.Default.Code,
        Color(0xFF00796B), Color(0xFFE0F2F1),
        Brush.horizontalGradient(listOf(Color(0xFF00796B), Color(0xFF009688)))
    ),
    SubjectTheme(
        "Other", "General Homework", Icons.Default.Assignment,
        Color(0xFF455A64), Color(0xFFECEFF1),
        Brush.horizontalGradient(listOf(Color(0xFF455A64), Color(0xFF607D8B)))
    )
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScannerApp(viewModel: HomeworkViewModel = viewModel()) {
    val context = LocalContext.current
    val currentSolution by viewModel.currentSolution.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Scan & Solver, 1 = Solution View, 2 = History
    var showSettingsDialog by remember { mutableStateOf(false) }

    // If a solution gets solved successfully, automatically navigate to Tab 1 (Solution view)
    LaunchedEffect(currentSolution) {
        if (currentSolution != null) {
            activeTab = 1
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "App Emblem",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PrepAlly",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("preferences_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "App Settings and Tuning",
                            tint = if (viewModel.isApiKeyAvailable()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.CameraEnhance, "Scan Solver") },
                    label = { Text("Scanner") },
                    modifier = Modifier.testTag("tab_scanner")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            if (currentSolution != null) {
                                Badge { Text("●", fontSize = 6.sp) }
                            }
                        }) {
                            Icon(Icons.Default.Lightbulb, "Tutor Room")
                        }
                    },
                    label = { Text("Tutor") },
                    modifier = Modifier.testTag("tab_tutor")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.HistoryEdu, "Saves History") },
                    label = { Text("Saves") },
                    modifier = Modifier.testTag("tab_saves")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() with slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() with slideOutHorizontally { width -> width } + fadeOut()
                    } using SizeTransform(clip = false)
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ScannerTab(
                        viewModel = viewModel,
                        onSolveTriggered = {
                            activeTab = 1
                        }
                    )
                    1 -> SolutionTab(
                        viewModel = viewModel,
                        onScanAgainRequested = {
                            activeTab = 0
                            viewModel.clearActiveSolution()
                        }
                    )
                    2 -> SavesTab(
                        viewModel = viewModel,
                        onSolutionSelected = {
                            viewModel.selectSolution(it)
                            activeTab = 1
                        }
                    )
                }
            }

            // Global Overlay loading spinner
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.testTag("global_loading_spinner"))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "AI Tutor analyzing homework...",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Gemini 3.5 Flash formulating explanations",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // API warning alert when keys aren't loaded in Gemini 
            if (!viewModel.isApiKeyAvailable() && activeTab == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { showSettingsDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert Key",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(
                                    "Gemini API Secret is missing",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Tap here to configure a temporary key or use AI Studio Secrets.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Edit Key",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for active preferences setup and tutor tuning
    if (showSettingsDialog) {
        val currentPersonality by viewModel.tutorPersonality.collectAsState()
        val currentComplexity by viewModel.explanationComplexity.collectAsState()
        var tempKeyText by remember { mutableStateOf(userApiKey) }
        var showResetConfirmation by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, "Pref Settings", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PrepAlly Preferences", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Tweak your AI tutor parameters to fit your custom learning preferences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    HorizontalDivider()

                    // Selection 1: Tutor Personality Selection
                    Text(
                        "Tutor Persona Style",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val personalities = listOf("Balanced Tutor", "Socratic Guide", "Strict Examiner", "Casual Buddy")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        personalities.forEach { p ->
                            val isSelected = currentPersonality == p
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updatePersonality(p) },
                                label = { Text(p, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Selection 2: Complexity Detail
                    Text(
                        "Explanation Complexity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val complexities = listOf("Detailed Step-by-Step", "Focus Formulas", "Quick Cheat Sheet")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        complexities.forEach { c ->
                            val isSelected = currentComplexity == c
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateComplexity(c) },
                                label = { Text(c, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider()

                    // Key setup input
                    Text(
                        "Gemini API Secret",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = tempKeyText,
                        onValueChange = { tempKeyText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_text_input"),
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        label = { Text("Enter secret override") }
                    )

                    HorizontalDivider()

                    // Destructive wipe sweeps
                    if (!showResetConfirmation) {
                        OutlinedButton(
                            onClick = { showResetConfirmation = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_all_history_trigger")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeleteSweep, "Sweep")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wipe All Tutoring Saves", fontSize = 11.sp)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Delete alright? This will clean all offline homework records irreversibly.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TextButton(onClick = { showResetConfirmation = false }) {
                                    Text("Cancel", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.clearAllHistory()
                                        showResetConfirmation = false
                                        Toast.makeText(context, "Saves wiped cleanly!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Yes, Delete All", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateApiKey(tempKeyText.trim())
                        showSettingsDialog = false
                    },
                    modifier = Modifier.testTag("api_key_save_button")
                ) {
                    Text("Save preferences")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun ScannerTab(
    viewModel: HomeworkViewModel,
    onSolveTriggered: () -> Unit
) {
    val context = LocalContext.current
    var selectedSubject by remember { mutableStateOf("Math") }
    var questionText by remember { mutableStateOf("") }
    
    // Captured photo Uri state
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Captured video Uri state
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    
    // We create files to receive captures
    val tempFilePair = remember(context) { 
        createTempImageFile(context) 
    }
    val tempVideoFilePair = remember(context) {
        createTempVideoFile(context)
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = tempFilePair.second
            videoUri = null // Clear video because we have an image
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            videoUri = null // Clear video because we have an image
        }
    }

    val videoRecorderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            videoUri = tempVideoFilePair.second
            imageUri = null // Clear image because we have a video
        }
    }

    val videoChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            imageUri = null // Clear image because we have a video
        }
    }

    val activeSubjectConfig = remember(selectedSubject) {
        Subjects.find { it.id == selectedSubject } ?: Subjects.first()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App intro visual
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(activeSubjectConfig.primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activeSubjectConfig.icon,
                        contentDescription = "Subject Icon",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Solving in ${activeSubjectConfig.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Take a photo or type details to begin instant explanation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Horizontal Subject Selector Row
        Text(
            "Select Homework Subject",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Subjects.forEach { sub ->
                val isSelected = sub.id == selectedSubject
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedSubject = sub.id },
                    label = { Text(sub.id) },
                    leadingIcon = {
                        Icon(
                            imageVector = sub.icon,
                            contentDescription = sub.id,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else sub.primaryColor
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = sub.primaryColor,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("subject_chip_${sub.id.lowercase()}")
                )
            }
        }

        // Photo Scanner Module Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (imageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    ) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Scanned Homework Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("delete_selected_photo")
                        ) {
                            Icon(Icons.Default.Close, "Remove Photo", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.FilterFrames,
                        contentDescription = "Scanning Panel",
                        modifier = Modifier
                            .size(56.dp)
                            .padding(bottom = 8.dp),
                        tint = activeSubjectConfig.primaryColor
                    )
                    Text(
                        "Scan Homework Assignment",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Position text inside camera frame clearly",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(tempFilePair.second) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("camera_launch_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = activeSubjectConfig.primaryColor)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoCamera, "Camera")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("gallery_launch_button"),
                        border = BorderStroke(1.dp, activeSubjectConfig.primaryColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = activeSubjectConfig.primaryColor)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, "Library")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload File", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    "Or Teach with an Explanation Video",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { videoRecorderLauncher.launch(tempVideoFilePair.second) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("video_record_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, "Record")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record Video", fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { videoChooserLauncher.launch("video/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("video_choose_button"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VideoLibrary, "Files")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Video", fontSize = 11.sp)
                        }
                    }
                }

                if (videoUri != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayCircle, "Video Attached", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Video homework loaded", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Retrieved frame processed automatically", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            IconButton(
                                onClick = { videoUri = null },
                                modifier = Modifier.testTag("delete_selected_video")
                            ) {
                                Icon(Icons.Default.Close, "Remove Video")
                            }
                        }
                    }
                }
            }
        }

        // Query text card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Optional Text Details / Question Prompt",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    placeholder = {
                        Text(
                            "Type your specific equations, paragraphs, coding questions, or explain what topic you want to check (e.g. explain step-by-step)",
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("homework_question_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Solve CTA Primary button
        Button(
            onClick = {
                viewModel.solveHomework(
                    subject = selectedSubject,
                    questionText = questionText,
                    imageUri = imageUri,
                    videoUri = videoUri
                )
                onSolveTriggered()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("solve_homework_button"),
            colors = ButtonDefaults.buttonColors(containerColor = activeSubjectConfig.primaryColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.AutoFixHigh, "Solve Assignment")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Ask PrepAlly to Tutor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Quick Tutorial Tips
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Quick Tips",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.Start)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, "Check", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ensure good lighting and hold the camera steady", fontSize = 11.sp, color = Color.Gray)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, "Check", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Combine math photos with typed questions for specialized results", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SolutionTab(
    viewModel: HomeworkViewModel,
    onScanAgainRequested: () -> Unit
) {
    val current by viewModel.currentSolution.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var followUpText by remember { mutableStateOf("") }

    if (current == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (errorMessage != null) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Analysis Failed",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = errorMessage ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onScanAgainRequested) {
                    Text("Go Back")
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Empty active solution",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No active solution analyzed yet",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Go back to the Scanner tab to submit your image scan or type assignments representing complex topics.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    } else {
        val activeSolution = current!!
        val activeSubjectTheme = remember(activeSolution.subject) {
            Subjects.find { it.id == activeSolution.subject } ?: Subjects.first()
        }

        val scrollState = rememberScrollState()

        Column(modifier = Modifier.fillMaxSize()) {
            // Summary header card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = activeSubjectTheme.containerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(activeSubjectTheme.primaryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activeSubjectTheme.icon,
                            contentDescription = "Subj Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeSolution.subject,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = activeSubjectTheme.primaryColor
                        )
                        Text(
                            text = if (activeSolution.questionText.length > 50) {
                                activeSolution.questionText.take(48) + "..."
                            } else {
                                activeSolution.questionText
                            },
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }

                    // Save Favorite option
                    IconButton(
                        onClick = { viewModel.toggleFavorite(activeSolution) },
                        modifier = Modifier.testTag("toggle_favorite_solution")
                    ) {
                        Icon(
                            imageVector = if (activeSolution.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (activeSolution.isFavorite) Color.Red else Color.Gray
                        )
                    }

                    // Close active solution option
                    IconButton(
                        onClick = { viewModel.clearActiveSolution() },
                        modifier = Modifier.testTag("clear_active_solution")
                    ) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }
            }

            // Central scrollable visualizer
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Homework scanned Image Preview
                    if (activeSolution.imageUrl != null) {
                        val file = File(activeSolution.imageUrl)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Scanned Assignment context",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }

                    // Explaining Video context Player
                    if (activeSolution.videoUrl != null) {
                        val videoFile = File(activeSolution.videoUrl)
                        if (videoFile.exists()) {
                            Text(
                                "Attached Video context",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AndroidView(
                                        factory = { context ->
                                            VideoView(context).apply {
                                                setVideoPath(videoFile.absolutePath)
                                                val mediaController = android.widget.MediaController(context)
                                                mediaController.setAnchorView(this)
                                                setMediaController(mediaController)
                                                setOnPreparedListener { it.start() }
                                                setOnErrorListener { _, _, _ -> true }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Render original prompt in quote format
                    Text(
                        "Scanned Question context",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = activeSolution.questionText.ifEmpty { "View Homework Scan visual above" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Solutions parsed heading
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, "Response", tint = activeSubjectTheme.primaryColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AI Tutor Explanation",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // Copy solution button
                        val clipContext = LocalContext.current
                        TextButton(
                            onClick = {
                                copyToClipboard(clipContext, activeSolution.solutionText)
                            },
                            modifier = Modifier.testTag("copy_solution_text_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Answer", fontSize = 12.sp)
                            }
                        }
                    }

                    // Render Gemini Solution parsed beautiful Markdown
                    SolutionMarkdownRenderer(
                        text = activeSolution.solutionText,
                        primaryColor = activeSubjectTheme.primaryColor
                    )

                    // Render continuing Conversation history if any
                    val history = viewModel.getCurrentSolutionHistory()
                    if (history.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Continuing Discussion Threads",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = activeSubjectTheme.primaryColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        history.forEach { chat ->
                            ChatBubbleItem(
                                chat = chat,
                                primaryColor = activeSubjectTheme.primaryColor,
                                containerColor = activeSubjectTheme.containerColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onScanAgainRequested,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("scan_again_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraEnhance, "Scan Another Help")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Another Homework", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Extra space at bottom to permit scrolling over input bar
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Error Message Banner for follow-ups
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Persistent Chat follow-up input bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = followUpText,
                        onValueChange = { followUpText = it },
                        placeholder = { Text("Ask follow-up question...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("follow_up_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (followUpText.trim().isNotEmpty()) {
                                viewModel.askFollowUp(followUpText)
                                followUpText = ""
                                keyboardController?.hide()
                            }
                        })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (followUpText.trim().isNotEmpty()) {
                                viewModel.askFollowUp(followUpText)
                                followUpText = ""
                                keyboardController?.hide()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = activeSubjectTheme.primaryColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("send_follow_up_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Ask",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavesTab(
    viewModel: HomeworkViewModel,
    onSolutionSelected: (HomeworkEntity) -> Unit
) {
    val solutions by viewModel.allSolutions.collectAsState()
    var filterOnlyFavorites by remember { mutableStateOf(false) }

    val filteredSolutions = remember(solutions, filterOnlyFavorites) {
        if (filterOnlyFavorites) {
            solutions.filter { it.isFavorite }
        } else {
            solutions
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Saves title details
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Tutoring Lab Saves",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Browse saved explanations and follow-ups",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Favorites filter
            IconButton(
                onClick = { filterOnlyFavorites = !filterOnlyFavorites },
                modifier = Modifier.testTag("toggle_favorite_filter")
            ) {
                Icon(
                    imageVector = if (filterOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Filter Stars",
                    tint = if (filterOnlyFavorites) Color.Red else Color.Gray
                )
            }
        }

        if (filteredSolutions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (filterOnlyFavorites) Icons.Default.FavoriteBorder else Icons.Default.HistoryEdu,
                        contentDescription = "Book empty state",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (filterOnlyFavorites) "No favorited saves matches" else "No saved homeworks yet",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (filterOnlyFavorites) "Study solutions and favorite important answers to find them here easily." else "Go back to scan, input coding or general assignments and find summaries preserved here.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSolutions, key = { it.id }) { item ->
                    val subjTheme = Subjects.find { it.id == item.subject } ?: Subjects.first()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSolutionSelected(item) }
                            .testTag("saved_hw_item_${item.id}"),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(subjTheme.primaryColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = subjTheme.icon,
                                            contentDescription = item.subject,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.subject,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = subjTheme.primaryColor
                                    )
                                }

                                Text(
                                    text = formatTimestamp(item.timestamp),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = item.questionText.ifEmpty { "Visual Image Scan Homework details" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.solutionText.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val totalChats = remember(item.historyJson) {
                                        // Simple count of follow-ups by counting objects
                                        item.historyJson.split("isUser").size - 1
                                    }
                                    if (totalChats > 0) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("$totalChats chat turns", fontSize = 11.sp) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = subjTheme.containerColor,
                                                labelColor = subjTheme.primaryColor
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(item) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Fav",
                                            tint = if (item.isFavorite) Color.Red else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteSolution(item) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_saved_item_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Drop",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Render dynamic Markdown and LaTeX equations beautifully
fun colorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return String.format("#%02x%02x%02x", red, green, blue)
}

fun generateMathHtml(
    rawText: String,
    textColorHex: String,
    bgColorHex: String,
    accentColorHex: String,
    fontSizePx: Int
): String {
    val b64Content = Base64.encodeToString(rawText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            
            <!-- Load KaTeX CSS -->
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
            
            <style>
                :root {
                    --bg-color: $bgColorHex;
                    --text-color: $textColorHex;
                    --accent-color: $accentColorHex;
                }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                    font-size: ${fontSizePx}px;
                    color: var(--text-color);
                    background-color: var(--bg-color);
                    margin: 0;
                    padding: 2px 4px;
                    line-height: 1.55;
                }
                * {
                    box-sizing: border-box;
                }
                p {
                    margin: 0 0 10px 0;
                }
                h1, h2, h3, h4, h5 {
                    color: var(--accent-color);
                    margin-top: 14px;
                    margin-bottom: 6px;
                    font-weight: 700;
                    line-height: 1.3;
                }
                h1 { font-size: 1.3em; border-bottom: 1px solid rgba(128,128,128,0.25); padding-bottom: 4px; }
                h2 { font-size: 1.22em; border-bottom: 1px solid rgba(128,128,128,0.15); padding-bottom: 2px; }
                h3 { font-size: 1.12em; }
                
                ul, ol {
                    margin: 0 0 10px 0;
                    padding-left: 18px;
                }
                li {
                    margin-bottom: 4px;
                }
                
                code {
                    font-family: Consolas, Menlo, Monaco, "Courier New", monospace;
                    background-color: rgba(128,128,128,0.12);
                    padding: 1px 4px;
                    border-radius: 4px;
                    font-size: 0.88em;
                }
                
                pre {
                    background-color: #1a1a1a;
                    color: #d4d4d4;
                    padding: 10px;
                    border-radius: 8px;
                    overflow-x: auto;
                    margin: 10px 0;
                    border: 1px solid rgba(128,128,128,0.22);
                }
                pre code {
                    background-color: transparent;
                    padding: 0;
                    border-radius: 0;
                    color: inherit;
                    font-size: 0.85em;
                }
                
                .katex-display {
                    overflow-x: auto;
                    overflow-y: hidden;
                    padding: 6px 0;
                    margin: 6px 0;
                    max-width: 100%;
                }
                
                blockquote {
                    border-left: 4px solid var(--accent-color);
                    margin: 10px 0;
                    padding-left: 10px;
                    color: rgba(128, 128, 128, 0.85);
                    font-style: italic;
                    background-color: rgba(128, 128, 128, 0.04);
                    padding-top: 3px;
                    padding-bottom: 3px;
                    border-radius: 0 4px 4px 0;
                }

                hr {
                    border: none;
                    border-top: 1px solid rgba(128, 128, 128, 0.2);
                    margin: 14px 0;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 12px 0;
                    font-size: 0.9em;
                }
                th, td {
                    border: 1px solid rgba(128,128,128,0.25);
                    padding: 8px;
                    text-align: left;
                }
                th {
                    background-color: rgba(128,128,128,0.08);
                    color: var(--accent-color);
                    font-weight: 700;
                }
                tr:nth-child(even) {
                    background-color: rgba(128,128,128,0.02);
                }
            </style>
            
            <script>
                // Core variables and math handlers defined before script triggers
                const b64Content = "$b64Content";
                
                function decodeBase64Utf8(base64) {
                    try {
                        const binString = atob(base64);
                        const bytes = Uint8Array.from(binString, (m) => m.codePointAt(0));
                        return new TextDecoder().decode(bytes);
                    } catch (e) {
                        console.error("Base64 decoding failure", e);
                        return "Decoding failure: " + e.message;
                    }
                }

                function sendHeight() {
                    if (window.AndroidInterface) {
                        const contentHeight = document.getElementById('content').offsetHeight || document.body.offsetHeight;
                        window.AndroidInterface.onHeightReceived(contentHeight);
                    }
                }

                function renderContent() {
                    try {
                        if (typeof marked === 'undefined' || typeof katex === 'undefined' || typeof renderMathInElement === 'undefined') {
                            // Render raw text as placeholder until all scripts load
                            const rawContent = decodeBase64Utf8(b64Content);
                            document.getElementById('content').innerText = rawContent;
                            setTimeout(sendHeight, 50);
                            return;
                        }

                        const rawContent = decodeBase64Utf8(b64Content);
                        
                        // 1. PRE-PROCESS: Extract math blocks to protect them from Marked markdown interpretation
                        const mathBlocks = [];
                        const mathRegex = /(\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\$[^$\n]+?\$|\\\(.*?\\\))/g;
                        
                        const safeText = rawContent.replace(mathRegex, function(match) {
                            mathBlocks.push(match);
                            // Standard placeholder text with no underscores or special formatting characters
                            return "MATHPLACEHOLDER" + (mathBlocks.length - 1) + "XYZ";
                        });
                        
                        // 2. PARSE MARKDOWN WITH MARKED
                        let htmlContent = "";
                        if (typeof marked === 'function') {
                            htmlContent = marked(safeText);
                        } else if (typeof marked === 'object' && typeof marked.parse === 'function') {
                            htmlContent = marked.parse(safeText);
                        } else {
                            htmlContent = safeText;
                        }
                        
                        // 3. POST-PROCESS: Place original math equations back into HTML
                        const restoredHtml = htmlContent.replace(/MATHPLACEHOLDER(\d+)XYZ/g, function(match, index) {
                            return mathBlocks[parseInt(index, 10)];
                        });
                        
                        // 4. INJECT TO DOM
                        document.getElementById('content').innerHTML = restoredHtml;
                        
                        // 5. RENDER LATEX EQUATIONS VIA KATEX AUTO-RENDERER
                        renderMathInElement(document.getElementById('content'), {
                            delimiters: [
                                {left: "$$", right: "$$", display: true},
                                {left: "\\[", right: "\\]", display: true},
                                {left: "$", right: "$", display: false},
                                {left: "\\(", right: "\\)", display: false}
                            ],
                            throwOnError: false
                        });
                        
                    } catch (e) {
                        console.error("Typesetting error", e);
                        document.getElementById('content').innerText = "Typesetting Error: " + e.message;
                    }
                    setTimeout(sendHeight, 50);
                }

                // Safely report heights at key moments (initial load, after typesetting, and when fonts are ready)
                window.onload = function() {
                    renderContent();
                    setTimeout(sendHeight, 100);
                    setTimeout(sendHeight, 400);
                };

                // Trigger height report when web fonts (including KaTeX symbols) are fully loaded
                if (document.fonts && typeof document.fonts.ready === 'object') {
                    document.fonts.ready.then(function() {
                        sendHeight();
                        setTimeout(sendHeight, 200);
                    });
                }
            </script>
            
            <!-- Load external rendering libraries asynchronously with onloads -->
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js" onload="renderContent()"></script>
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js" onload="renderContent()"></script>
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js" onload="renderContent()"></script>
        </head>
        <body>
            <div id="content"></div>
        </body>
        </html>
    """.trimIndent()
}

@Composable
fun LaTeXWebView(
    text: String,
    primaryColor: Color,
    textColor: Color,
    backgroundColor: Color,
    fontSizeSp: Int
) {
    var webViewHeight by remember { mutableStateOf(100.dp) }
    
    val textColorHex = remember(textColor) { colorToHex(textColor) }
    val bgColorHex = remember(backgroundColor) { 
        if (backgroundColor == Color.Transparent) "transparent" else colorToHex(backgroundColor) 
    }
    val accentColorHex = remember(primaryColor) { colorToHex(primaryColor) }

    val htmlContent = remember(text, textColorHex, bgColorHex, accentColorHex, fontSizeSp) {
        generateMathHtml(text, textColorHex, bgColorHex, accentColorHex, fontSizeSp)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                // Enable web view debugging so developers can inspect inside Chrome DevTools via USB / local emulator
                WebView.setWebContentsDebuggingEnabled(true)

                webViewClient = android.webkit.WebViewClient()
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.d(
                            "WebViewConsole",
                            "[${"$"}{consoleMessage?.messageLevel()}] ${"$"}{consoleMessage?.message()} " +
                            "-- From line ${"$"}{consoleMessage?.lineNumber()} of ${"$"}{consoleMessage?.sourceId()}"
                        )
                        return true
                    }
                }

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onHeightReceived(heightPx: Float) {
                        post {
                            val density = ctx.resources.displayMetrics.density
                            val calculatedHeightDp = (heightPx / density) + 8f
                            val currentHeightDp = webViewHeight.value
                            // Guard: Only update WebView height in Compose if the change is significant (> 4dp)
                            // or it's the initial default height report. This prevents dynamic loop jitters.
                            if (currentHeightDp == 100f || Math.abs(calculatedHeightDp - currentHeightDp) > 4f) {
                                webViewHeight = calculatedHeightDp.coerceAtLeast(35f).dp
                            }
                        }
                    }
                }, "AndroidInterface")
            }
        },
        update = { webView ->
            // PREVENT INFINITE RELOAD LOOP:
            // Check if the loaded content is different to prevent refreshing and killing the page state repeatedly.
            val loadedContent = webView.tag as? String
            if (loadedContent != htmlContent) {
                webView.tag = htmlContent
                webView.loadDataWithBaseURL(
                    "https://localhost",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(webViewHeight)
    )
}

@Composable
fun SolutionMarkdownRenderer(
    text: String,
    primaryColor: Color,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = Color.Transparent,
    fontSizeSp: Int = 15
) {
    LaTeXWebView(
        text = text,
        primaryColor = primaryColor,
        textColor = textColor,
        backgroundColor = backgroundColor,
        fontSizeSp = fontSizeSp
    )
}

@Composable
fun ChatBubbleItem(
    chat: ChatMessage,
    primaryColor: Color,
    containerColor: Color
) {
    val alignStyle = if (chat.isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (chat.isUser) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val textStyleColor = if (chat.isUser) primaryColor else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = alignStyle
    ) {
        Text(
            text = if (chat.isUser) "You" else "PrepAlly Tutor",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 2.dp, start = 6.dp, end = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (chat.isUser) 16.dp else 2.dp,
                bottomEnd = if (chat.isUser) 2.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                SolutionMarkdownRenderer(
                    text = chat.text,
                    primaryColor = primaryColor,
                    textColor = textStyleColor,
                    fontSizeSp = 13
                )
            }
        }
    }
}

// Sealed model for rich Markdown outputs
sealed class MarkdownBlock {
    data class Header(val level: Int, val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class BulletItem(val content: String) : MarkdownBlock()
    data class Paragraph(val content: String) : MarkdownBlock()
}

// Custom simple parser to break raw markdown lines down into styled items
fun parseMarkdownToBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    var insideCode = false
    var currentCodeText = StringBuilder()
    var currentLanguage = ""

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (insideCode) {
                blocks.add(MarkdownBlock.CodeBlock(currentLanguage, currentCodeText.toString()))
                currentCodeText = StringBuilder()
                insideCode = false
            } else {
                currentLanguage = trimmed.removePrefix("```").trim()
                insideCode = true
            }
            continue
        }

        if (insideCode) {
            currentCodeText.append(line).append("\n")
            continue
        }

        if (trimmed.isEmpty()) continue

        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val title = trimmed.drop(level).trim()
            blocks.add(MarkdownBlock.Header(level, title))
        } else if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
            val body = trimmed.substring(1).trim()
            blocks.add(MarkdownBlock.BulletItem(cleanMarkdownFormatting(body)))
        } else {
            blocks.add(MarkdownBlock.Paragraph(cleanMarkdownFormatting(line)))
        }
    }

    if (insideCode) {
        blocks.add(MarkdownBlock.CodeBlock(currentLanguage, currentCodeText.toString()))
    }

    return blocks
}

// Strip out minor formatting like asterisks or subheadings for simple clean texts
fun cleanMarkdownFormatting(text: String): String {
    return text.replace("**", "")
        .replace("_", "")
        .replace("`", "")
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Copied Solution", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied explanation to clipboard!", Toast.LENGTH_SHORT).show()
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Helper to safely provision a cache file path for Camera images
fun createTempImageFile(context: Context): Pair<File, Uri> {
    val tempFile = File(
        context.cacheDir,
        "hw_scan_captured_${System.currentTimeMillis()}.jpg"
    )
    if (tempFile.exists()) tempFile.delete()
    tempFile.createNewFile()
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, tempFile)
    return Pair(tempFile, uri)
}

// Helper to safely provision a cache file path for recorded explanations
fun createTempVideoFile(context: Context): Pair<File, Uri> {
    val tempFile = File(
        context.cacheDir,
        "hw_tutor_video_${System.currentTimeMillis()}.mp4"
    )
    if (tempFile.exists()) tempFile.delete()
    tempFile.createNewFile()
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, tempFile)
    return Pair(tempFile, uri)
}

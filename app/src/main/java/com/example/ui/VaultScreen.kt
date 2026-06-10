package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VaultItem
import com.example.viewmodel.VaultViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showDeleteConfirmDialog by remember { mutableStateOf<VaultItem?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Modern M3 color elements
    val darkMainBg = Color(0xFF0D0F17)
    val cardSurfaceBg = Color(0xFF161925)
    val accentGold = Color(0xFFFFB13B)

    // Launchers for system native secure photo/video picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importMultipleMedia(context, uris, isVideo = false)
            }
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importMultipleMedia(context, uris, isVideo = true)
            }
        }
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(darkMainBg),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ClosedCaptionDisabled,
                            contentDescription = "Safe Lock",
                            tint = accentGold,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "הכספת הסודית",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Help Guide",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = { viewModel.lockVault() }) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock Securely",
                            tint = accentGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkMainBg,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                // Add Video FAB
                ExtendedFloatingActionButton(
                    text = { Text("הוסף סרטונים", color = Color.Black, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color.Black) },
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    containerColor = accentGold,
                    shape = RoundedCornerShape(16.dp)
                )

                // Add Photo FAB
                ExtendedFloatingActionButton(
                    text = { Text("הוסף תמונות", color = Color.White, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White) },
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        containerColor = darkMainBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Secondary notifications / Toast bar
                AnimatedVisibility(
                    visible = feedbackMessage != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    feedbackMessage?.let { text ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = accentGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = text,
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "סגור",
                                    color = Color.Black.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable { viewModel.clearMessage() }
                                        .padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Hebrew Custom Tabs (Photos / Videos)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = darkMainBg,
                    contentColor = accentGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = accentGold
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "תמונות (${photos.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "סרטונים (${videos.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    )
                }

                // Grid contents
                val currentItems = if (selectedTab == 0) photos else videos
                if (currentItems.isEmpty()) {
                    EmptyVaultState(
                        isVideoTab = selectedTab == 1,
                        accentGold = accentGold
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(currentItems, key = { it.id }) { item ->
                            VaultMediaItemCard(
                                item = item,
                                file = viewModel.getFileForItem(context, item),
                                onClick = { viewModel.selectItem(item) },
                                onDeleteClick = { showDeleteConfirmDialog = item },
                                accentGold = accentGold,
                                cardBg = cardSurfaceBg
                            )
                        }
                    }
                }
            }

            // Foreground importing circular loader
            if (isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f))
                        .clickable(enabled = false) {}, // Scrim
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardSurfaceBg),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = accentGold)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "מייבא מדיה באבטחה...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "הקובץ מועתק לאחסון הפרטי ונמחק באבטחה מהגלריה הציבורית",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete item confirmation Dialog
    showDeleteConfirmDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    text = "מחיקה לצמיתות?",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "האם אתה בטוח שברצונך למחוק קובץ זה מהכספת? הפעולה אינה הפיכה והקובץ יימחק לחלוטין מהמכשיר.",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItem(context, item)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("מחק לצמיתות", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("ביטול", color = Color.White)
                }
            },
            containerColor = cardSurfaceBg,
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    // Info guide Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = "מידע על הכספת",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold,
                    color = accentGold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🔒 סודיות מוחלטת",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "הקבצים שמועברים לכאן נשמרים בתיקיית האפליקציה המאובטחת והם נעלמים מהגלריה שלך. אף אפליקציה אחרת במכשיר לא יכולה לראות אותם.",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 13.sp
                    )

                    Text(
                        text = "📱 מחיקה מלאה בהסרה",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "אם תסיר את האפליקציה מהמכשיר, מטעמי אבטחה של אנדרואיד כל הקבצים שבתוך הכספת יימחקו לצמיתות מהמכשיר באבטחה.",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 13.sp
                    )

                    Text(
                        text = "📌 שימו לב",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "לאחר ייבוא ואישור הקובץ, מומלץ לוודא שהקובץ נמחק מהגלריה הציבורית שלך ולרוקן את 'סל המיחזור' במכשיר כדי להסיר עקבות לחלוטין.",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = accentGold)
                ) {
                    Text("הבנתי", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = cardSurfaceBg
        )
    }
}

@Composable
fun EmptyVaultState(
    isVideoTab: Boolean,
    accentGold: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(accentGold.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVideoTab) Icons.Default.VideoLibrary else Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = accentGold,
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isVideoTab) "אין עדיין סרטונים" else "אין עדיין תמונות",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isVideoTab) {
                "לחץ על 'הוסף סרטונים' למטה כדי לייבא סרטונים פרטיים מהגלריה של המכשיר שלך בצורה מאובטחת."
            } else {
                "לחץ על 'הוסף תמונות' למטה כדי לייבא תמונות פרטיות ולהסתיר אותן מהגלריה הציבורית."
            },
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultMediaItemCard(
    item: VaultItem,
    file: File,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    accentGold: Color,
    cardBg: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteClick
            ),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.fileType == "PHOTO") {
                AsyncImage(
                    model = file,
                    contentDescription = item.originalName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Video thumbnail placeholder / cover design
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF2E3245), Color(0xFF13151D))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Video",
                            tint = accentGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.originalName,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Quick hover overlay tag to delete or indicate type
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.fileType == "VIDEO") Icons.Default.Videocam else Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.82f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

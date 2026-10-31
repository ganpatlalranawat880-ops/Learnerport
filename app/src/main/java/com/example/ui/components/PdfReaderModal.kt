package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResourceItem
import com.example.data.pdf.PdfFileManager
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderModal(
  resource: ResourceItem,
  currentPage: Int,
  zoomLevel: Float,
  onClose: () -> Unit,
  onNextPage: () -> Unit,
  onPrevPage: () -> Unit,
  onZoomIn: () -> Unit,
  onZoomOut: () -> Unit,
  onResetZoom: () -> Unit,
  onDownload: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var pdfFile by remember { mutableStateOf<File?>(null) }
  var totalPages by remember { mutableIntStateOf(1) }
  var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isLoadingPdf by remember { mutableStateOf(true) }
  var isRenderingPage by remember { mutableStateOf(false) }

  // Interactive Pan & Zoom states
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
    scale = (scale * zoomChange).coerceIn(0.8f, 3.5f)
    offset += offsetChange
  }

  var pdfLoadError by remember { mutableStateOf<String?>(null) }
  var retryTrigger by remember { mutableIntStateOf(0) }

  // Effect to load or ensure PDF file
  LaunchedEffect(resource.id, retryTrigger) {
    isLoadingPdf = true
    pdfLoadError = null
    try {
      withContext(Dispatchers.IO) {
        val file = PdfFileManager.ensurePdfFileForResource(context, resource)
        pdfFile = file
        totalPages = PdfFileManager.getPageCount(file)
      }
    } catch (e: Throwable) {
      pdfLoadError = e.localizedMessage ?: "Unable to load PDF from cloud storage."
    } finally {
      isLoadingPdf = false
    }
  }

  // Effect to render native PDF page bitmap when page or file changes
  LaunchedEffect(pdfFile, currentPage) {
    val file = pdfFile ?: return@LaunchedEffect
    isRenderingPage = true
    val bmp = PdfFileManager.renderPdfPageBitmap(file, currentPage, targetWidth = 1080)
    pageBitmap = bmp
    isRenderingPage = false
    // Reset transform when changing page
    scale = zoomLevel
    offset = Offset.Zero
  }

  // Synchronize topBar zoom controls with scale
  LaunchedEffect(zoomLevel) {
    scale = zoomLevel
  }

  val isOfflineAvailable = resource.isDownloaded ||
    PdfFileManager.isPdfDownloaded(context, resource.id) ||
    (!resource.localFilePath.isNullOrBlank() && File(resource.localFilePath).exists())

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = resource.title,
              maxLines = 1,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "PDF • Page ${currentPage + 1} of $totalPages",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (isOfflineAvailable) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFFDCFCE7)
                ) {
                  Text(
                    text = "Offline Ready",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF166534),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Document")
          }
        },
        actions = {
          // Zoom out
          IconButton(onClick = onZoomOut, enabled = scale > 0.8f) {
            Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom Out")
          }
          // Zoom in
          IconButton(onClick = onZoomIn, enabled = scale < 3.0f) {
            Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom In")
          }
          // Reset zoom
          IconButton(onClick = {
            scale = 1f
            offset = Offset.Zero
            onResetZoom()
          }) {
            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Zoom")
          }
          // Download / Offline toggle
          IconButton(onClick = onDownload) {
            Icon(
              imageVector = if (isOfflineAvailable) Icons.Default.DownloadDone else Icons.Default.Download,
              contentDescription = "Download PDF",
              tint = if (isOfflineAvailable) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    bottomBar = {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Previous Page Button
          OutlinedButton(
            onClick = onPrevPage,
            enabled = currentPage > 0,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("pdf_prev_page_button")
          ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Previous", fontSize = 12.sp)
          }

          // Page indicator chip
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = "${currentPage + 1} / $totalPages",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }

          // Next Page Button
          Button(
            onClick = onNextPage,
            enabled = currentPage < totalPages - 1,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue),
            modifier = Modifier.testTag("pdf_next_page_button")
          ) {
            Text("Next", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .background(Color(0xFF0F172A)),
      contentAlignment = Alignment.Center
    ) {
      when {
        isLoadingPdf -> {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            CircularProgressIndicator(color = LearnerBlue)
            Text(
              text = "Loading document PDF...",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
        pdfLoadError != null -> {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ErrorOutline,
              contentDescription = null,
              tint = Color(0xFFEF4444),
              modifier = Modifier.size(48.dp)
            )
            Text(
              text = "PDF Unavailable",
              color = Color.White,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = pdfLoadError ?: "This document could not be retrieved from cloud storage.",
              color = Color.LightGray,
              fontSize = 12.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              OutlinedButton(
                onClick = onClose,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
              ) {
                Text("Close")
              }
              Button(
                onClick = { retryTrigger += 1 },
                colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry")
              }
            }
          }
        }
        pageBitmap != null -> {
          // Native Rendered PDF Page with pinch-to-zoom and pan gestures
          Box(
            modifier = Modifier
              .fillMaxSize()
              .transformable(state = transformableState),
            contentAlignment = Alignment.Center
          ) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              shadowElevation = 8.dp,
              modifier = Modifier
                .padding(12.dp)
                .graphicsLayer(
                  scaleX = scale,
                  scaleY = scale,
                  translationX = offset.x,
                  translationY = offset.y
                )
            ) {
              Image(
                bitmap = pageBitmap!!.asImageBitmap(),
                contentDescription = "PDF Page ${currentPage + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                  .wrapContentSize()
                  .testTag("native_pdf_page_image")
              )
            }
          }
        }
        else -> {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = Color(0xFFF59E0B),
              modifier = Modifier.size(40.dp)
            )
            Text(
              text = "Unable to render page ${currentPage + 1}",
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Please retry or download the document for offline study.",
              color = Color.LightGray,
              fontSize = 12.sp
            )
          }
        }
      }

      // Floating Rendering Overlay Indicator
      if (isRenderingPage) {
        Surface(
          shape = CircleShape,
          color = Color.Black.copy(alpha = 0.6f),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rendering page...", color = Color.White, fontSize = 11.sp)
          }
        }
      }
    }
  }
}

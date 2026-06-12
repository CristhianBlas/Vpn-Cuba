package com.example

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.io.InputStream
import java.util.concurrent.Executors

// Color Palette consistent with main workspace
private val CyberDarkCard = Color(0xFF0F1420)
private val CyberGreen = Color(0xFF00FFCC)
private val BorderColor = Color(0x19FFFFFF)
private val CyberTextColor = Color(0xFFFFFFFF)
private val CyberTextSecondary = Color(0xFF8C9CAB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerDialog(
    onDismissRequest: () -> Unit,
    onServerImported: (VpnServer) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    
    // Permission state tracker
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Camera, 1: Gallery

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE605080E)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Nav Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .background(Color(0x11FFFFFF), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Importar Configuración QR",
                        style = TextStyle(
                            color = CyberTextColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.size(40.dp))
                }

                // Cyberpunk Tabs Selector
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberDarkCard)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabItem(
                        selected = selectedTab == 0,
                        icon = Icons.Default.Search,
                        text = "Escanear Cámara",
                        onClick = { selectedTab = 0 }
                    )
                    TabItem(
                        selected = selectedTab == 1,
                        icon = Icons.Default.Add,
                        text = "Cargar Imagen",
                        onClick = { selectedTab = 1 }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedTab == 0) {
                        if (hasCameraPermission) {
                            LiveCameraScannerView(
                                onQrCodeScanned = { decoded ->
                                    val server = QRDecoder.parseServerCode(decoded)
                                    if (server != null) {
                                        onServerImported(server)
                                        Toast.makeText(context, "¡Servidor ${server.country} Importado!", Toast.LENGTH_SHORT).show()
                                        onDismissRequest()
                                    } else {
                                        Log.w("QRScanner", "QR scanned but unable to parse: $decoded")
                                    }
                                }
                            )
                        } else {
                            // Permission Warning Block
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Permiso de cámara necesario",
                                    style = TextStyle(color = CyberTextColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Falta el permiso de cámara para poder usar el visor de códigos QR. Por favor actívalo.",
                                    style = TextStyle(color = CyberTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                                )
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Otorgar Permiso", style = TextStyle(color = Color.Black, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    } else {
                        // Gallery selection screen
                        GalleryPickerView(
                            onServerParsed = { server ->
                                onServerImported(server)
                                Toast.makeText(context, "¡Servidor ${server.country} Importado con éxito!", Toast.LENGTH_SHORT).show()
                                onDismissRequest()
                            },
                            onFailure = {
                                Toast.makeText(context, "No se encontró un código QR de servidor válido en la imagen.", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Formatos Soportados:\nURLs de Flash VPN (flashvpn://add?), JSON estructurados o cadenas separadas por |",
                        style = TextStyle(color = CyberTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.TabItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val bgModifier = if (selected) {
        Modifier
            .background(CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .border(1.dp, CyberGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .then(bgModifier)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) CyberGreen else CyberTextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = TextStyle(
                color = if (selected) CyberGreen else CyberTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun LiveCameraScannerView(
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Floating scanner line animation
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    var isProcessing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            if (isProcessing) {
                imageProxy.close()
                return@setAnalyzer
            }

            val buffer = imageProxy.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val source = PlanarYUVLuminanceSource(
                data,
                imageProxy.width,
                imageProxy.height,
                0,
                0,
                imageProxy.width,
                imageProxy.height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val reader = MultiFormatReader()
                val result = reader.decode(binaryBitmap)
                val text = result.text
                if (!text.isNullOrEmpty()) {
                    isProcessing = true
                    // Call callback on UI thread
                    ContextCompat.getMainExecutor(context).execute {
                        onQrCodeScanned(text)
                    }
                }
            } catch (e: Exception) {
                // Ignore decoding exception if QR frame is partial
            } finally {
                imageProxy.close()
            }
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("LiveCameraScanner", "Camera binding failed: ${e.message}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
    ) {
        // Camera live stream View
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Futuristic translucent cyberpunk alignment mask overlays
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val scanBoxSize = minOf(canvasWidth, canvasHeight) * 0.72f
            val left = (canvasWidth - scanBoxSize) / 2f
            val top = (canvasHeight - scanBoxSize) / 2f

            // 1. Draw outer dim masks
            // Top dim
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, top)
            )
            // Bottom dim
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, top + scanBoxSize),
                size = Size(canvasWidth, canvasHeight - (top + scanBoxSize))
            )
            // Left dim
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, top),
                size = Size(left, scanBoxSize)
            )
            // Right dim
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(left + scanBoxSize, top),
                size = Size(canvasWidth - (left + scanBoxSize), scanBoxSize)
            )

            // 2. Beautiful neon glowing scanning box frame
            drawRoundRect(
                color = CyberGreen,
                topLeft = Offset(left, top),
                size = Size(scanBoxSize, scanBoxSize),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // 3. Glowing animated laser line sweeping up and down
            val lineY = top + (scanBoxSize * scanProgress)
            drawLine(
                color = CyberGreen,
                start = Offset(left + 12.dp.toPx(), lineY),
                end = Offset(left + scanBoxSize - 12.dp.toPx(), lineY),
                strokeWidth = 3.dp.toPx()
            )
        }

        // Helper instruction text overlapping inside the frame
        Text(
            text = "Coloca el código QR dentro del recuadro",
            style = TextStyle(
                color = CyberGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun GalleryPickerView(
    onServerParsed: (VpnServer) -> Unit,
    onFailure: () -> Unit
) {
    val context = LocalContext.current

    // Launch gallery image picking activity
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val decodedText = QRDecoder.decodeBitmap(bitmap)
                    if (decodedText != null) {
                        val server = QRDecoder.parseServerCode(decodedText)
                        if (server != null) {
                            onServerParsed(server)
                        } else {
                            onFailure()
                        }
                    } else {
                        onFailure()
                    }
                } else {
                    onFailure()
                }
            } catch (e: Exception) {
                Log.e("QRScanner", "Error processing photo: ", e)
                onFailure()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberDarkCard)
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .clickable { imageLauncher.launch("image/*") }
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(CyberGreen.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, CyberGreen.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Abrir Galería",
                tint = CyberGreen,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Seleccionar Captura de Pantalla",
            style = TextStyle(color = CyberTextColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pulsa aquí para buscar un código QR guardado en tu galería o carpeta de descargas.\n\nEs ideal si te mandaron la configuración de Flash VPN por WhatsApp o Telegram.",
            style = TextStyle(color = CyberTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}


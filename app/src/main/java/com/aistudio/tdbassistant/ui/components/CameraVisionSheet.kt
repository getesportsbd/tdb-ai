package com.aistudio.tdbassistant.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@Composable
fun TdbVisionScreen(
    isGenerating: Boolean,
    latestVisionReply: String?,
    onAnalyzeBitmap: (Bitmap, String) -> Unit,
    onPickPhotoUri: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraActive by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var customQuestion by remember { mutableStateOf("") }
    var cameraStatus by remember { mutableStateOf("ক্যামেরা বন্ধ আছে। লাইভ প্রিভিউ দেখতে Start চাপুন।") }
    var textureRef by remember { mutableStateOf<TextureView?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            cameraActive = true
            cameraStatus = "লাইভ ক্যামেরা চালু"
        } else {
            cameraStatus = "ক্যামেরা অনুমতি দেওয়া হয়নি। চাইলে নিচে গ্যালারি থেকে ছবি নির্বাচন করতে পারেন।"
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickPhotoUri(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "TDB Vision · ক্যামেরা ও বিশ্লেষণ",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "কোনো গোপন রেকর্ডিং নয়—কেবল আপনার অনুমোদিত লাইভ প্রিভিউ বা নির্বাচিত ছবি বিশ্লেষণ করে",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Camera Preview / Captured Frame Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    capturedBitmap != null -> {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured frame for vision analysis",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { capturedBitmap = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Retake", tint = Color.White)
                        }
                    }
                    cameraActive && hasCameraPermission -> {
                        Camera2PreviewBox(
                            onTextureReady = { textureRef = it },
                            onStatusChange = { cameraStatus = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VideocamOff,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = cameraStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Visible Camera Active Indicator
                if (cameraActive && capturedBitmap == null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = CircleShape,
                        color = Color(0xFF0E7A48)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6EE7B7))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CAMERA LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Start / Stop / Capture / Photo Picker Controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (cameraActive) {
                        cameraActive = false
                        cameraStatus = "ক্যামেরা বন্ধ করা হয়েছে।"
                    } else {
                        capturedBitmap = null
                        if (hasCameraPermission) {
                            cameraActive = true
                            cameraStatus = "লাইভ ক্যামেরা চালু"
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("vision_toggle_camera")
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (cameraActive) "Stop Camera" else "Start Camera")
            }

            OutlinedButton(
                onClick = {
                    val shot = textureRef?.bitmap
                    if (shot != null) {
                        capturedBitmap = shot
                        cameraActive = false
                    }
                },
                enabled = cameraActive && textureRef?.isAvailable == true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("vision_capture_button")
            ) {
                Text("Capture Frame")
            }
        }

        OutlinedButton(
            onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vision_pick_photo")
        ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("গ্যালারি থেকে ছবি নির্বাচন করুন (Gemini Vision-এ পাঠান)")
        }

        // Analysis Prompts
        OutlinedTextField(
            value = customQuestion,
            onValueChange = { customQuestion = it },
            label = { Text("ছবি সম্পর্কে প্রশ্ন লিখুন (ঐচ্ছিক)") },
            placeholder = { Text("যেমন: এই ছবিতে কী লেখা আছে?") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val bmp = capturedBitmap ?: textureRef?.bitmap
                    if (bmp != null) {
                        onAnalyzeBitmap(
                            bmp,
                            customQuestion.ifBlank { "সামনে কী কী বস্তু ও দৃশ্য দেখা যাচ্ছে তা বাংলায় পরিষ্কারভাবে বলো।" }
                        )
                    }
                },
                enabled = !isGenerating && (capturedBitmap != null || (cameraActive && textureRef?.isAvailable == true)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("vision_analyze_scene")
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("দৃশ্য শনাক্ত")
            }

            OutlinedButton(
                onClick = {
                    val bmp = capturedBitmap ?: textureRef?.bitmap
                    if (bmp != null) {
                        onAnalyzeBitmap(
                            bmp,
                            "এই ছবির ভেতরের সমস্ত লেখা (OCR) হুবহু পড়ে বের করো এবং বাংলায় সংক্ষেপে অর্থ বলো।"
                        )
                    }
                },
                enabled = !isGenerating && (capturedBitmap != null || (cameraActive && textureRef?.isAvailable == true)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("vision_analyze_ocr")
            ) {
                Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("OCR লেখা পড়ুন")
            }
        }

        if (!latestVisionReply.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TDB Vision ফলাফল",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = latestVisionReply,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun Camera2PreviewBox(
    onTextureReady: (TextureView) -> Unit,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraHolder = remember { Camera2Holder(context) }

    DisposableEffect(Unit) {
        onDispose {
            cameraHolder.close()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        onTextureReady(this@apply)
                        cameraHolder.openBackCamera(surface, onStatusChange)
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        cameraHolder.close()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        }
    )
}

private class Camera2Holder(private val context: Context) {
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    @SuppressLint("MissingPermission")
    fun openBackCamera(surfaceTexture: SurfaceTexture, onError: (String) -> Unit) {
        runCatching {
            val ht = HandlerThread("TdbCamera2").also { it.start() }
            thread = ht
            val h = Handler(ht.looper)
            handler = h

            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.firstOrNull { id ->
                val facing = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: manager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                onError("এই ডিভাইসে কোনো ক্যামেরা পাওয়া যায়নি।")
                return
            }

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    val surface = Surface(surfaceTexture)
                    val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(surface)
                    }
                    @Suppress("DEPRECATION")
                    camera.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                captureSession = session
                                runCatching {
                                    session.setRepeatingRequest(builder.build(), null, h)
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                onError("ক্যামেরা প্রিভিউ কনফিগার করা যায়নি।")
                            }
                        },
                        h
                    )
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    onError("ক্যামেরা ত্রুটি (কোড $error)।")
                }
            }, h)
        }.onFailure {
            onError(it.localizedMessage ?: "ক্যামেরা চালু করা যায়নি।")
        }
    }

    fun close() {
        runCatching { captureSession?.close() }
        captureSession = null
        runCatching { cameraDevice?.close() }
        cameraDevice = null
        thread?.quitSafely()
        thread = null
        handler = null
    }
}

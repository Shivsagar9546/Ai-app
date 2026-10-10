package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

@android.annotation.SuppressLint("InvalidFragmentVersionForActivityResult")
class FloatingImagePickerActivity : ComponentActivity() {

    private fun safeFinish() {
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleImageUri(uri)
        } else {
            FloatingAssistantService.activeServiceInstance?.showPopup()
            safeFinish()
        }
    }
 
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            captureScreen(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Screen capture cancelled", Toast.LENGTH_SHORT).show()
            FloatingAssistantService.activeServiceInstance?.showBubble()
            safeFinish()
        }
    }

    private val pickVisualMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            handleImageUri(uri)
        } else {
            FloatingAssistantService.activeServiceInstance?.showPopup()
            safeFinish()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_GALLERY
        when (mode) {
            MODE_SCREEN_CAPTURE -> {
                try {
                    val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                    screenCaptureLauncher.launch(captureIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Screen capture not supported: ${e.message}", Toast.LENGTH_SHORT).show()
                    FloatingAssistantService.activeServiceInstance?.showBubble()
                    safeFinish()
                }
            }

            else -> {
                try {
                    pickVisualMediaLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                } catch (e: Exception) {
                    try {
                        getContentLauncher.launch("image/*")
                    } catch (e2: Exception) {
                        Toast.makeText(this, "No image picker available: ${e2.localizedMessage}", Toast.LENGTH_SHORT).show()
                        safeFinish()
                    }
                }
            }
        }
    }

    private fun handleImageUri(uri: Uri?) {
        if (uri == null) {
            FloatingAssistantService.activeServiceInstance?.showPopup()
            safeFinish()
            return
        }
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(this, "Could not read image data", Toast.LENGTH_SHORT).show()
                FloatingAssistantService.activeServiceInstance?.showPopup()
                safeFinish()
                return
            }

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            var sampleSize = 1
            val maxDim = 1280
            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth > maxDim || origHeight > maxDim) {
                val halfWidth = origWidth / 2
                val halfHeight = origHeight / 2
                while ((halfWidth / sampleSize) >= maxDim && (halfHeight / sampleSize) >= maxDim) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)

            if (bitmap != null) {
                val scaled = scaleDownBitmap(bitmap, 1280)
                onImageSelectedCallback?.invoke(scaled)
            } else {
                Toast.makeText(this, "Could not load image file", Toast.LENGTH_SHORT).show()
                FloatingAssistantService.activeServiceInstance?.showPopup()
            }
        } catch (t: Throwable) {
            Toast.makeText(this, "Error loading image: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            FloatingAssistantService.activeServiceInstance?.showPopup()
        } finally {
            safeFinish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            onImageSelectedCallback = null
        }
    }

    private fun captureScreen(resultCode: Int, resultData: Intent) {
        val fgs = FloatingAssistantService.activeServiceInstance
        fgs?.promoteToMediaProjectionFgs()

        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        val projection = try {
            mpManager?.getMediaProjection(resultCode, resultData)
        } catch (e: Exception) {
            android.util.Log.e("FloatingImagePicker", "getMediaProjection error", e)
            null
        }

        if (projection == null) {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            fgs?.demoteFromMediaProjectionFgs()
            fgs?.showBubble()
            safeFinish()
            return
        }

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val width = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
        val height = if (metrics.heightPixels > 0) metrics.heightPixels else 2400
        val density = if (metrics.densityDpi > 0) metrics.densityDpi else 400

        val handlerThread = HandlerThread("ScreenCaptureBackground").apply { start() }
        val backgroundHandler = Handler(handlerThread.looper)
        val mainHandler = Handler(Looper.getMainLooper())

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val isProcessed = AtomicBoolean(false)
        var virtualDisplay: android.hardware.display.VirtualDisplay? = null

        // Android 14+ (API 34+) REQUIRES registering a MediaProjection.Callback BEFORE createVirtualDisplay()
        val callback = object : android.media.projection.MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
            }
        }
        try {
            projection.registerCallback(callback, backgroundHandler)
        } catch (e: Exception) {
            try {
                projection.registerCallback(callback, Handler(Looper.getMainLooper()))
            } catch (_: Throwable) {}
        }

        val timeoutRunnable = Runnable {
            if (isProcessed.compareAndSet(false, true)) {
                cleanupProjectionSafely(virtualDisplay, imageReader, projection, backgroundHandler, handlerThread)
                mainHandler.post {
                    fgs?.demoteFromMediaProjectionFgs()
                    fgs?.showBubble()
                    Toast.makeText(this@FloatingImagePickerActivity, "Screen capture timed out", Toast.LENGTH_SHORT).show()
                    safeFinish()
                }
            }
        }
        backgroundHandler.postDelayed(timeoutRunnable, 3500)

        imageReader.setOnImageAvailableListener({ reader ->
            if (isProcessed.get()) return@setOnImageAvailableListener

            val image = try {
                reader.acquireLatestImage() ?: reader.acquireNextImage()
            } catch (_: Throwable) {
                null
            } ?: return@setOnImageAvailableListener

            if (!isProcessed.compareAndSet(false, true)) {
                try { image.close() } catch (_: Throwable) {}
                return@setOnImageAvailableListener
            }

            backgroundHandler.removeCallbacks(timeoutRunnable)

            try {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                try { image.close() } catch (_: Throwable) {}

                val cleanBitmap = if (bitmap.width != width) {
                    val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    if (cropped !== bitmap) {
                        bitmap.recycle()
                    }
                    cropped
                } else {
                    bitmap
                }

                cleanupProjectionSafely(virtualDisplay, imageReader, projection, backgroundHandler, handlerThread)

                mainHandler.post {
                    fgs?.demoteFromMediaProjectionFgs()
                    onImageSelectedCallback?.invoke(cleanBitmap)
                    safeFinish()
                }
            } catch (t: Throwable) {
                try { image.close() } catch (_: Throwable) {}
                cleanupProjectionSafely(virtualDisplay, imageReader, projection, backgroundHandler, handlerThread)
                mainHandler.post {
                    fgs?.demoteFromMediaProjectionFgs()
                    fgs?.showBubble()
                    Toast.makeText(this@FloatingImagePickerActivity, "Capture processing failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    safeFinish()
                }
            }
        }, backgroundHandler)

        virtualDisplay = try {
            projection.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                backgroundHandler
            )
        } catch (e: Exception) {
            android.util.Log.e("FloatingImagePicker", "createVirtualDisplay failed", e)
            null
        }

        if (virtualDisplay == null) {
            backgroundHandler.removeCallbacks(timeoutRunnable)
            try { projection.stop() } catch (_: Throwable) {}
            try { imageReader.close() } catch (_: Throwable) {}
            try { handlerThread.quitSafely() } catch (_: Throwable) {}
            fgs?.demoteFromMediaProjectionFgs()
            fgs?.showBubble()
            Toast.makeText(this, "Virtual display setup failed", Toast.LENGTH_SHORT).show()
            safeFinish()
            return
        }
    }

    private fun cleanupProjectionSafely(
        virtualDisplay: android.hardware.display.VirtualDisplay?,
        imageReader: ImageReader?,
        projection: android.media.projection.MediaProjection?,
        backgroundHandler: Handler?,
        handlerThread: HandlerThread?
    ) {
        try {
            imageReader?.setOnImageAvailableListener(null, null)
        } catch (_: Throwable) {}
        try {
            virtualDisplay?.setSurface(null)
        } catch (_: Throwable) {}
        try {
            virtualDisplay?.release()
        } catch (_: Throwable) {}
        try {
            projection?.stop()
        } catch (_: Throwable) {}

        // Delay closing ImageReader so the native BufferQueue producer in SurfaceFlinger
        // cleanly completes disconnection without logging "BufferQueue has been abandoned"
        val closeRunnable = Runnable {
            try {
                imageReader?.close()
            } catch (_: Throwable) {}
            try {
                handlerThread?.quitSafely()
            } catch (_: Throwable) {}
        }

        if (backgroundHandler != null && handlerThread?.isAlive == true) {
            backgroundHandler.postDelayed(closeRunnable, 1000)
        } else {
            closeRunnable.run()
        }
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        return if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val newW = if (bitmap.width > bitmap.height) maxDim else (maxDim * ratio).toInt()
            val newH = if (bitmap.width > bitmap.height) (maxDim / ratio).toInt() else maxDim
            val scaled = Bitmap.createScaledBitmap(bitmap, newW.coerceAtLeast(1), newH.coerceAtLeast(1), true)
            if (scaled != bitmap) {
                bitmap.recycle()
            }
            scaled
        } else {
            bitmap
        }
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val MODE_GALLERY = "mode_gallery"
        const val MODE_SCREEN_CAPTURE = "mode_screen_capture"

        private var onImageSelectedCallback: ((Bitmap) -> Unit)? = null

        fun launchScreenCapture(context: Context, onPicked: (Bitmap) -> Unit) {
            onImageSelectedCallback = onPicked
            val intent = Intent(context, FloatingImagePickerActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_SCREEN_CAPTURE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }

        fun launchGalleryPicker(context: Context, onPicked: (Bitmap) -> Unit) {
            onImageSelectedCallback = onPicked
            val intent = Intent(context, FloatingImagePickerActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_GALLERY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
    }
}

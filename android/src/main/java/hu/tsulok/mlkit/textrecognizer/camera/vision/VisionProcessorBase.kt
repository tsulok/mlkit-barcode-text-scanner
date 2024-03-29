package hu.tsulok.mlkit.textrecognizer.camera.vision

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.*
import com.google.android.odml.image.BitmapMlImageBuilder
import com.google.android.odml.image.ByteBufferMlImageBuilder
import com.google.android.odml.image.MediaMlImageBuilder
import com.google.android.odml.image.MlImage
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import hu.tsulok.mlkit.textrecognizer.domain.model.RecognizerResult
import java.nio.ByteBuffer
import java.util.*

/**
 * Abstract base class for ML Kit frame processors. Subclasses need to implement {@link
 * #onSuccess(T, FrameMetadata, GraphicOverlay)} to define what they want to with the detection
 * results and {@link #detectInImage(VisionImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
abstract class VisionProcessorBase<T>(
    private val context: Context
) : VisionImageProcessor {

    companion object {
        private const val TAG = "VisionProcessorBase"
    }

    private var activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val fpsTimer = Timer()
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    // Whether this processor is already shut down
    private var isShutdown = false

    // Used to calculate latency, running in the same thread, no sync needed.
    private var numRuns = 0
    private var totalFrameMs = 0L
    private var maxFrameMs = 0L
    private var minFrameMs = Long.MAX_VALUE
    private var totalDetectorMs = 0L
    private var maxDetectorMs = 0L
    private var minDetectorMs = Long.MAX_VALUE

    // Frame count that have been processed so far in an one second interval to calculate FPS.
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null

    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null

    // To keep the images and metadata in process.
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null

    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null

    init {
        fpsTimer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    framesPerSecond = frameProcessedInOneSecondInterval
                    frameProcessedInOneSecondInterval = 0
                }
            },
            0,
            1000
        )
    }

    // -----------------Code for processing single still image----------------------------------------
    override fun processBitmap(bitmap: Bitmap?) {
        val frameStartMs = SystemClock.elapsedRealtime()

        if (isMlImageEnabled(context)) {
            val mlImage = BitmapMlImageBuilder(bitmap!!).build()
            requestDetectInImage(
                mlImage,
                /* originalCameraImage= */ null,
                /* shouldShowFps= */ false,
                frameStartMs
            )
            mlImage.close()
            return
        }

        requestDetectInImage(
            InputImage.fromBitmap(bitmap!!, 0),
            /* originalCameraImage= */ null,
            /* shouldShowFps= */ false,
            frameStartMs
        )
    }

    // -----------------Code for processing live preview frame from Camera1 API-----------------------
    @Synchronized
    override fun processByteBuffer(
        data: ByteBuffer?,
        frameMetadata: FrameMetadata?
    ) {
        latestImage = data
        latestImageMetaData = frameMetadata
        if (processingImage == null && processingMetaData == null) {
            processLatestImage()
        }
    }

    @Synchronized
    private fun processLatestImage() {
        processingImage = latestImage
        processingMetaData = latestImageMetaData
        latestImage = null
        latestImageMetaData = null
        if (processingImage != null && processingMetaData != null && !isShutdown) {
            processImage(processingImage!!, processingMetaData!!)
        }
    }

    private fun processImage(
        data: ByteBuffer,
        frameMetadata: FrameMetadata,
    ) {
        val frameStartMs = SystemClock.elapsedRealtime()
        // If live viewport is on (that is the underneath surface view takes care of the camera preview
        // drawing), skip the unnecessary bitmap creation that used for the manual preview drawing.
        val bitmap = null
//            if (PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.context)) null
//            else BitmapUtils.getBitmap(data, frameMetadata)

        if (isMlImageEnabled(context)) {
            val mlImage =
                ByteBufferMlImageBuilder(
                    data,
                    frameMetadata.width,
                    frameMetadata.height,
                    MlImage.IMAGE_FORMAT_NV21
                )
                    .setRotation(frameMetadata.rotation)
                    .build()
            requestDetectInImage(
                mlImage,
                bitmap, /* shouldShowFps= */
                true,
                frameStartMs
            ).addOnSuccessListener(executor) { processLatestImage() }

            // This is optional. Java Garbage collection can also close it eventually.
            mlImage.close()
            return
        }

        requestDetectInImage(
            InputImage.fromByteBuffer(
                data,
                frameMetadata.width,
                frameMetadata.height,
                frameMetadata.rotation,
                InputImage.IMAGE_FORMAT_NV21
            ),
            bitmap,
            /* shouldShowFps= */ true,
            frameStartMs
        )
            .addOnSuccessListener(executor) { processLatestImage() }
    }

    // -----------------Code for processing live preview frame from CameraX API-----------------------
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @ExperimentalGetImage
    override fun processImageProxy(image: ImageProxy) {
        val frameStartMs = SystemClock.elapsedRealtime()
        if (isShutdown) {
            return
        }
        var bitmap: Bitmap? = null
//        if (!PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.context)) {
//            bitmap = BitmapUtils.getBitmap(image)
//        }

        if (isMlImageEnabled(context)) {
            val mlImage =
                MediaMlImageBuilder(image.image!!).setRotation(image.imageInfo.rotationDegrees)
                    .build()
            requestDetectInImage(
                mlImage,
                /* originalCameraImage= */ bitmap,
                /* shouldShowFps= */ true,
                frameStartMs
            )
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                // Currently MlImage doesn't support ImageProxy directly, so we still need to call
                // ImageProxy.close() here.
                .addOnCompleteListener { image.close() }

            return
        }

        requestDetectInImage(
            InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees),
            /* originalCameraImage= */ bitmap,
            /* shouldShowFps= */ true,
            frameStartMs
        )
            // When the image is from CameraX analysis use case, must call image.close() on received
            // images when finished using them. Otherwise, new images may not be received or the camera
            // may stall.
            .addOnCompleteListener { image.close() }
    }

    // -----------------Common processing logic-------------------------------------------------------
    private fun requestDetectInImage(
        image: InputImage,
        originalCameraImage: Bitmap?,
        shouldShowFps: Boolean,
        frameStartMs: Long
    ): Task<T> {
        return setUpListener(
            detectInImage(image),
            originalCameraImage,
            shouldShowFps,
            frameStartMs
        )
    }

    private fun requestDetectInImage(
        image: MlImage,
        originalCameraImage: Bitmap?,
        shouldShowFps: Boolean,
        frameStartMs: Long
    ): Task<T> {
        return setUpListener(
            detectInImage(image),
            originalCameraImage,
            shouldShowFps,
            frameStartMs
        )
    }

    private fun setUpListener(
        task: Task<T>,
        originalCameraImage: Bitmap?,
        shouldShowFps: Boolean,
        frameStartMs: Long
    ): Task<T> {
        val detectorStartMs = SystemClock.elapsedRealtime()
        return task
            .addOnSuccessListener(
                executor
            ) { results: T ->
                val endMs = SystemClock.elapsedRealtime()
                val currentFrameLatencyMs = endMs - frameStartMs
                val currentDetectorLatencyMs = endMs - detectorStartMs
                if (numRuns >= 500) {
                    resetLatencyStats()
                }
                numRuns++
                frameProcessedInOneSecondInterval++
                totalFrameMs += currentFrameLatencyMs
                maxFrameMs = Math.max(currentFrameLatencyMs, maxFrameMs)
                minFrameMs = Math.min(currentFrameLatencyMs, minFrameMs)
                totalDetectorMs += currentDetectorLatencyMs
                maxDetectorMs = Math.max(currentDetectorLatencyMs, maxDetectorMs)
                minDetectorMs = Math.min(currentDetectorLatencyMs, minDetectorMs)

                // Only log inference info once per second. When frameProcessedInOneSecondInterval is
                // equal to 1, it means this is the first frame processed during the current second.
                if (frameProcessedInOneSecondInterval == 1) {
                    Log.d(TAG, "Num of Runs: $numRuns")
                    Log.d(
                        TAG,
                        "Frame latency: max=" +
                                maxFrameMs +
                                ", min=" +
                                minFrameMs +
                                ", avg=" +
                                totalFrameMs / numRuns
                    )
                    Log.d(
                        TAG,
                        "Detector latency: max=" +
                                maxDetectorMs +
                                ", min=" +
                                minDetectorMs +
                                ", avg=" +
                                totalDetectorMs / numRuns
                    )
                    val mi = ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(mi)
                    val availableMegs: Long = mi.availMem / 0x100000L
                    Log.d(TAG, "Memory available in system: $availableMegs MB")
                }

                this@VisionProcessorBase.onSuccess(results)
            }
            .addOnFailureListener(
                executor,
                OnFailureListener { e: Exception ->
                    val error = "Failed to process. Error: " + e.localizedMessage
                    Toast.makeText(
                        context,
                        """
          $error
          Cause: ${e.cause}
          """.trimIndent(),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    Log.d(TAG, error)
                    e.printStackTrace()
                    this@VisionProcessorBase.onFailure(e)
                }
            )
    }

    override fun stop() {
        executor.shutdown()
        isShutdown = true
        resetLatencyStats()
        fpsTimer.cancel()
    }

    private fun resetLatencyStats() {
        numRuns = 0
        totalFrameMs = 0
        maxFrameMs = 0
        minFrameMs = Long.MAX_VALUE
        totalDetectorMs = 0
        maxDetectorMs = 0
        minDetectorMs = Long.MAX_VALUE
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected open fun detectInImage(image: MlImage): Task<T> {
        return Tasks.forException(
            MlKitException(
                "MlImage is currently not demonstrated for this feature",
                MlKitException.INVALID_ARGUMENT
            )
        )
    }

    protected abstract fun onSuccess(results: T)

    protected abstract fun onFailure(e: Exception)

    protected open fun isMlImageEnabled(context: Context?): Boolean {
        return false
    }
}

package hu.tsulok.mlkit.textrecognizer.camera.vision

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.MlKitException
import java.nio.ByteBuffer


/**
 * An interface to process the images with different vision detectors and custom image models.
 */
interface VisionImageProcessor {
    /**
     * Processes a bitmap image.
     */
    fun processBitmap(bitmap: Bitmap?)

    /**
     * Processes ByteBuffer image data, e.g. used for Camera1 live preview case.
     */
    @Throws(MlKitException::class)
    fun processByteBuffer(
        data: ByteBuffer?, frameMetadata: FrameMetadata?
    )

    /**
     * Processes ImageProxy image data, e.g. used for CameraX live preview case.
     */
    @Throws(MlKitException::class)
    fun processImageProxy(image: ImageProxy)

    /**
     * Stops the underlying machine learning model and release resources.
     */
    fun stop()
}

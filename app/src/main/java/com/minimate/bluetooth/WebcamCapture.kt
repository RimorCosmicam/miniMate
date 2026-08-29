package com.minimate.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import androidx.core.content.ContextCompat
import com.minimate.touchpad.model.WebcamLens
import com.minimate.touchpad.model.WebcamResolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

data class WebcamCaptureState(
    val running: Boolean = false,
    val framesCaptured: Long = 0,
    val error: String? = null
)

/**
 * Preview-less Camera2 producer for the Mac virtual camera. JPEG output keeps the
 * phone side small and lets Core Image perform the complete stacked filter pass.
 */
class WebcamCapture(
    context: Context,
    private val bridge: BluetoothAudioBridge
) {
    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val worker = HandlerThread("MiniMate-Webcam").apply { start() }
    private val handler = Handler(worker.looper)
    private val _state = MutableStateFlow(WebcamCaptureState())
    val state: StateFlow<WebcamCaptureState> = _state.asStateFlow()

    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var generation = 0

    @SuppressLint("MissingPermission")
    fun start(
        lens: WebcamLens,
        resolution: WebcamResolution,
        fps: Int,
        zoom: Float,
        exposure: Float
    ) {
        stop()
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            _state.value = WebcamCaptureState(error = "Camera permission is required")
            return
        }
        val thisGeneration = ++generation
        val cameraId = selectCamera(lens) ?: run {
            _state.value = WebcamCaptureState(error = "Requested camera is unavailable")
            return
        }
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val size = selectJpegSize(characteristics, resolution)
        reader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 3).apply {
            setOnImageAvailableListener({ source ->
                val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    bridge.sendWebcamFrame(bytes)
                    _state.update { it.copy(framesCaptured = it.framesCaptured + 1, error = null) }
                } finally {
                    image.close()
                }
            }, handler)
        }
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                if (thisGeneration != generation) { device.close(); return }
                camera = device
                createSession(device, characteristics, reader!!, lens, fps, zoom, exposure)
            }
            override fun onDisconnected(device: CameraDevice) {
                device.close(); if (thisGeneration == generation) _state.value = WebcamCaptureState(error = "Camera disconnected")
            }
            override fun onError(device: CameraDevice, error: Int) {
                device.close(); if (thisGeneration == generation) _state.value = WebcamCaptureState(error = "Camera error $error")
            }
        }, handler)
    }

    private fun createSession(
        device: CameraDevice,
        characteristics: CameraCharacteristics,
        output: ImageReader,
        lens: WebcamLens,
        fps: Int,
        zoom: Float,
        exposure: Float
    ) {
        device.createCaptureSession(listOf(output.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(captureSession: CameraCaptureSession) {
                if (device !== camera) { captureSession.close(); return }
                session = captureSession
                val request = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                    addTarget(output.surface)
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                    set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, bestFpsRange(characteristics, fps))
                    val compensationRange = characteristics[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]
                    val step = characteristics[CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP]?.toFloat() ?: 0f
                    if (compensationRange != null && step > 0f) {
                        val ev = (exposure.coerceIn(-1f, 1f) * 2f / step).toInt().coerceIn(compensationRange.lower, compensationRange.upper)
                        set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ev)
                    }
                    cropForZoom(characteristics, zoom)?.let { set(CaptureRequest.SCALER_CROP_REGION, it) }
                    set(CaptureRequest.JPEG_QUALITY, 91.toByte())
                    val sensor = characteristics[CameraCharacteristics.SENSOR_ORIENTATION] ?: 0
                    set(CaptureRequest.JPEG_ORIENTATION, if (lens == WebcamLens.FRONT) (360 - sensor) % 360 else sensor)
                }.build()
                captureSession.setRepeatingRequest(request, null, handler)
                _state.update { it.copy(running = true, error = null) }
            }
            override fun onConfigureFailed(captureSession: CameraCaptureSession) {
                _state.value = WebcamCaptureState(error = "Camera configuration failed")
            }
        }, handler)
    }

    private fun selectCamera(lens: WebcamLens): String? {
        val facing = if (lens == WebcamLens.FRONT) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
        return cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)[CameraCharacteristics.LENS_FACING] == facing
        }
    }

    private fun selectJpegSize(characteristics: CameraCharacteristics, requested: WebcamResolution): Size {
        val sizes = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            ?.getOutputSizes(ImageFormat.JPEG).orEmpty()
        if (sizes.isEmpty()) return Size(requested.width, requested.height)
        val aspect = requested.width.toFloat() / requested.height
        return sizes.minByOrNull {
            abs(it.width - requested.width) + abs(it.height - requested.height) +
                (abs(it.width.toFloat() / it.height - aspect) * 4_000).toInt()
        } ?: sizes.first()
    }

    private fun bestFpsRange(characteristics: CameraCharacteristics, requested: Int): Range<Int> {
        val ranges = characteristics[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES].orEmpty()
        return ranges.minByOrNull { abs(it.upper - requested) * 3 + abs(it.lower - requested) }
            ?: Range(requested, requested)
    }

    private fun cropForZoom(characteristics: CameraCharacteristics, requested: Float): Rect? {
        val active = characteristics[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE] ?: return null
        val maximum = characteristics[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM] ?: 1f
        val zoom = requested.coerceIn(1f, maximum)
        val width = (active.width() / zoom).toInt()
        val height = (active.height() / zoom).toInt()
        val left = active.left + (active.width() - width) / 2
        val top = active.top + (active.height() - height) / 2
        return Rect(left, top, left + width, top + height)
    }

    fun stop() {
        generation++
        runCatching { session?.stopRepeating() }
        session?.close(); session = null
        camera?.close(); camera = null
        reader?.close(); reader = null
        _state.update { it.copy(running = false) }
    }

    fun close() {
        stop()
        worker.quitSafely()
    }
}

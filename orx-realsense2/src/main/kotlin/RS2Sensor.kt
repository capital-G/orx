package org.openrndr.extra.realsense2

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.librealsense2.*
import org.bytedeco.librealsense2.global.realsense2.*
import org.openrndr.draw.ColorBuffer
import org.openrndr.events.Event
import java.nio.ByteBuffer

private val rs2Context by lazy {
    val error = rs2_error()
    val ctx = rs2_create_context(RS2_API_VERSION, error)
    ctx
}

private fun rs2_error.check() {
    if (!this.isNull) {
        val function = rs2_get_failed_function(this)
        val args = rs2_get_failed_args(this)
        val errorMessage = rs2_get_error_message(this)
        error("$errorMessage in $function with $args")
    }
}

enum class RS2DepthFormat {
    UINT16
}
class RS2SensorDescription(private val deviceList: rs2_device_list, private val deviceIndex: Int) {

    /**
     * open realsense sensor from description entry
     */
    fun open(
            depthWidth: Int = 640,
            depthHeight: Int = 480,
            depthFps: Int = 30,
            depthFormat: RS2DepthFormat = RS2DepthFormat.UINT16

    ): RS2Sensor {
        val error = rs2_error()
        val device = rs2_create_device(deviceList, deviceIndex, error);
        error.check()

        val pipeline = rs2_create_pipeline(rs2Context, error)
        error.check()

        val config = rs2_create_config(error)
        error.check()

        rs2_config_enable_stream(config, RS2_STREAM_DEPTH, 0, depthWidth, depthHeight, RS2_FORMAT_Z16, depthFps, error)
        error.check()

        val pipelineProfile = rs2_pipeline_start_with_config(pipeline, config, error)
        error.check()
        return RS2Sensor(device, pipeline, pipelineProfile)
    }
}

class RS2FrameEvent(private val frame: rs2_frame, val frameWidth: Int, val frameHeight: Int) {
    val frameData: ByteBuffer
    get() {
        val error = rs2_error()
        val pointer = rs2_get_frame_data(frame, error)
        val buffer = BytePointer(pointer).capacity(frameWidth * frameHeight * 2L).asByteBuffer()
        error.check()
        error.close()
        pointer.close()
        return buffer
    }

    fun copyTo(target : ColorBuffer) {
        target.write(frameData)
    }
}

abstract class Sensor {
    /**
     * depth frame received event, triggered from [waitForFrames]
     */
    val depthFrameReceived = Event<RS2FrameEvent>()

    /**
     * wait for frames to arrives
     */
    abstract fun waitForFrames()

    /**
     * destroy the sensor
     */
    abstract fun destroy()
}

class DummySensor : Sensor() {
    override fun waitForFrames() {
    }

    override fun destroy() {
    }
}

class RS2Sensor(
        private val device: rs2_device,
        private val pipeline: rs2_pipeline,
        private val pipelineProfile: rs2_pipeline_profile

) : Sensor() {
    override fun waitForFrames() {
        val error = rs2_error()
        val frames = rs2_pipeline_wait_for_frames(pipeline, RS2_DEFAULT_TIMEOUT, error)
        error.check()

        val frameCount = rs2_embedded_frames_count(frames, error)
        error.check()

        for (i in 0 until frameCount) {
            val frame = rs2_extract_frame(frames, i, error)
            error.check()
            val cmp = rs2_is_frame_extendable_to(frame, RS2_EXTENSION_DEPTH_FRAME, error)
            if (cmp != 0) {
                val width = rs2_get_frame_width(frame, error)
                error.check()
                val height = rs2_get_frame_height(frame, error)
                error.check()
                val eventMessage = RS2FrameEvent(frame, width, height)
                depthFrameReceived.trigger(eventMessage)
                rs2_release_frame(frame)
            }
        }
        rs2_release_frame(frames)
    }

    override fun destroy() {
        val error = rs2_error()
        rs2_pipeline_stop(pipeline, error)
        error.check()
    }

    companion object {
        /**
         * list all connected Realsense devices
         */
        fun listSensors(): List<RS2SensorDescription> {
            val error = rs2_error()
            val deviceList = rs2_query_devices(rs2Context, error)
            error.check()
            val deviceCount = rs2_get_device_count(deviceList, error)
            error.check()
            return if (deviceCount == 0) {
                emptyList()
            } else {
                (0 until deviceCount).map {
                    RS2SensorDescription(deviceList, it)
                }
            }
        }
        /**
         * open the first available sensor or a dummy sensor if no real sensors are available
         */
        fun openFirstOrDummy() : Sensor {
            return listSensors().firstOrNull()?.open() ?: DummySensor()
        }
    }
}
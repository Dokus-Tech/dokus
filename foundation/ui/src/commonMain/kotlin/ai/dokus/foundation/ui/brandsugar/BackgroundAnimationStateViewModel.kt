package ai.thepredict.ui.brandsugar

import ai.thepredict.app.core.viewmodel.BaseViewModel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class Dot(
    val position: Offset,
    val velocity: Offset,
    val radius: Float,
    val color: Color
)

data class BackgroundAnimationState(
    val dots: List<Dot>,
    val blurProgress: Float,
    val boxSize: Offset
)

class BackgroundAnimationViewModel : BaseViewModel<BackgroundAnimationState>(initialState) {
    private companion object {
        private const val DOT_COUNT = 12
        private val initialBoxSize = Offset(2000f, 2000f)
        private val initialState by lazy {
            BackgroundAnimationState(
                dots = List(DOT_COUNT) {
                    Dot(
                        position = Offset(
                            Random.nextFloat() * initialBoxSize.x,
                            Random.nextFloat() * initialBoxSize.y
                        ),
                        velocity = Offset(
                            (Random.nextFloat() - 0.5f) * 5.0f,
                            (Random.nextFloat() - 0.5f) * 5.0f
                        ),
                        radius = Random.nextFloat() * 48f + 32f,
                        color = Color.White.copy(alpha = 0.11f + Random.nextFloat() * 0.22f)
                    )
                },
                blurProgress = 98f,
                boxSize = initialBoxSize
            )
        }
    }

    private val subscriberCount = MutableStateFlow(0)
    private var animationJob: Job? = null

    fun start() {
        val count = subscriberCount.value + 1
        subscriberCount.value = count
        if (count == 1) {
            animationJob = scope.launch {
                animateBlur()
                animateDots()
            }
        }
    }

    fun stop() {
        val count = (subscriberCount.value - 1).coerceAtLeast(0)
        subscriberCount.value = count
        if (count == 0) {
            animationJob?.cancel()
            animationJob = null
        }
    }

    private suspend fun animateBlur() {
        val min = 8f
        val max = 98f
        val durationMs = 800f
        val frameIntervalMs = 16f
        val step = (max - min) / (durationMs / frameIntervalMs)
        var current = max
        while (current > min) {
            current = (current - step).coerceAtLeast(min)
            mutableState.value = mutableState.value.copy(blurProgress = current)
            delay(frameIntervalMs.toLong())
        }
        mutableState.value = mutableState.value.copy(blurProgress = min)
    }

    private suspend fun animateDots() {
        while (true) {
            val box = mutableState.value.boxSize
            val updatedDots = mutableState.value.dots.map { dot ->
                var pos = Offset(dot.position.x + dot.velocity.x, dot.position.y + dot.velocity.y)
                var vel = dot.velocity
                if (pos.x < 0f || pos.x > box.x) vel = Offset(-vel.x, vel.y)
                if (pos.y < 0f || pos.y > box.y) vel = Offset(vel.x, -vel.y)
                pos = Offset(
                    pos.x.coerceIn(0f, box.x),
                    pos.y.coerceIn(0f, box.y)
                )
                dot.copy(position = pos, velocity = vel)
            }
            mutableState.value = mutableState.value.copy(dots = updatedDots)
            delay(16)
        }
    }

    fun setBoxSize(newSize: Offset) {
        mutableState.value = mutableState.value.copy(boxSize = newSize)
    }
}
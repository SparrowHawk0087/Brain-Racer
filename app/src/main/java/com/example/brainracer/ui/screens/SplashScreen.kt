package com.example.brainracer.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brainracer.R
import kotlinx.coroutines.delay

/**
 * Цвета фона splash. Совпадают с градиентом из ic_launcher_foreground.xml,
 * чтобы переход системный splash → Compose splash → основной фон был визуально слитным.
 */
private val SplashGradientStart = Color(0xFF8789E2) // см. windowSplashScreenBackground
private val SplashGradientEnd   = Color(0xFFCB99D0)

// «Жидкие» highlight-слои: нежные акценты поверх базового градиента.
private val HighlightWarm = Color(0xFFFFC4E1) // тёплый розово-перламутровый
private val HighlightCool = Color(0xFF7B7DE0) // холодный сине-фиолетовый
private val HighlightSoft = Color(0xFFFFFFFF) // белый «глянец»

/**
 * Анимированный SplashScreen приложения Brain Racer.
 *
 * Фон рисуется через [drawBehind] и состоит из нескольких **дрейфующих радиальных
 * градиентов** поверх базового линейного — это даёт мягкое «жидкое стекло»
 * без резких квадратных границ, которые получаются у `Modifier.blur` с дефолтным
 * `BlurredEdgeTreatment.Rectangle`. Каждый highlight медленно двигается с разной
 * частотой → визуальной зацикленности нет, фон ощущается живым.
 *
 * Поверх фона:
 *  - Иконка `ic_launcher_foreground` в стеклянном контейнере с pulsing.
 *  - 3 орбитальные точки, медленно вращающиеся вокруг иконки.
 *  - Каскадно появляющиеся заголовок и подзаголовок.
 *  - Точечный индикатор загрузки (3 точки, мигают по очереди).
 *
 * @param onFinished вызывается ровно один раз после [minDisplayMs] — обычно ведёт на auth/home.
 * @param minDisplayMs минимальная длительность показа splash.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    minDisplayMs: Long = 1700
) {
    // ── Каскадные one-shot анимации появления ─────────────────────────────
    val iconAlpha    = remember { Animatable(0f) }
    val iconScale    = remember { Animatable(0.7f) }
    val titleAlpha   = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val dotsAlpha    = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        iconAlpha.animateTo(1f, animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(220)
        titleOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(220)
        titleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(420)
        subtitleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(620)
        dotsAlpha.animateTo(1f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
    }

    // ── Минимальная длительность показа ────────────────────────────────────
    LaunchedEffect(Unit) {
        delay(minDisplayMs)
        onFinished()
    }

    // ── Бесконечные циклические анимации ───────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "splashLoop")

    // Дрейф «жидких» highlight-слоёв. Несинхронные периоды (9 / 11 / 13 сек) —
    // фоновое изображение никогда не повторяется визуально на коротких отрезках.
    val drift1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashDrift1"
    )
    val drift2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashDrift2"
    )
    val drift3 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashDrift3"
    )

    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashPulse"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue  = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashGlow"
    )
    val orbitDeg by infinite.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splashOrbit"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Весь фон рисуется одним проходом — пять радиальных градиентов поверх
            // линейной базы. Каждый радиал заканчивается Color.Transparent → нет
            // прямоугольных границ слоя, как было у Modifier.blur.
            .drawBehind {
                // 1) Базовый диагональный градиент.
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(SplashGradientStart, SplashGradientEnd),
                        start = Offset(0f, 0f),
                        end   = Offset(size.width * 1.1f, size.height * 1.1f)
                    )
                )

                // 2) Тёплый розовый highlight в верхней области, медленно дрейфует.
                val warmCenter = Offset(
                    x = size.width  * (0.78f + (drift1 - 0.5f) * 0.20f),
                    y = size.height * (0.20f + (drift2 - 0.5f) * 0.15f)
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            HighlightWarm.copy(alpha = 0.32f),
                            HighlightWarm.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = warmCenter,
                        radius = size.minDimension * 0.95f
                    )
                )

                // 3) Холодный сине-фиолетовый highlight в нижней области, другая частота.
                val coolCenter = Offset(
                    x = size.width  * (0.18f + (drift2 - 0.5f) * 0.20f),
                    y = size.height * (0.78f + (drift3 - 0.5f) * 0.15f)
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            HighlightCool.copy(alpha = 0.34f),
                            HighlightCool.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = coolCenter,
                        radius = size.minDimension * 0.95f
                    )
                )

                // 4) Лёгкий белый «глянец» рядом с центром — приподнимает карточку с иконкой.
                val sheenCenter = Offset(
                    x = size.width  * (0.50f + (drift3 - 0.5f) * 0.10f),
                    y = size.height * (0.42f + (drift1 - 0.5f) * 0.10f)
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            HighlightSoft.copy(alpha = 0.10f),
                            HighlightSoft.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = sheenCenter,
                        radius = size.minDimension * 0.75f
                    )
                )

                // 5) Очень мягкая виньетка к нижним углам — фокусирует взгляд на центре.
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.10f)
                        ),
                        center = Offset(size.width / 2f, size.height * 0.55f),
                        radius = size.maxDimension * 0.65f
                    )
                )
            }
    ) {
        // ── Центральный блок ──────────────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {

                // Glow за иконкой — крупное радиальное сияние, «дышит» с pulse.
                // Без clip(CircleShape): радиальный градиент сам уходит в Transparent →
                // граница слоя не видна, артефактов нет.
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .graphicsLayer {
                            alpha = iconAlpha.value * glowAlpha
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Орбитальные точки (3 шт, по 120° друг от друга).
                repeat(3) { i ->
                    val angle = orbitDeg + i * 120f
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer {
                                alpha = iconAlpha.value * 0.85f
                                rotationZ = angle
                            }
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .align(Alignment.TopCenter)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                        )
                    }
                }

                // Сама иконка
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .graphicsLayer {
                            alpha = iconAlpha.value
                            scaleX = iconScale.value * pulseScale
                            scaleY = iconScale.value * pulseScale
                        }
                        .shadow(
                            elevation = 30.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = Color.White.copy(alpha = 0.9f),
                            ambientColor = Color.White.copy(alpha = 0.6f)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Brain Racer",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(190.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Brain Racer",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleOffsetY.value
                }
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Готовим тренировку для мозга...",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value }
            )

            Spacer(Modifier.height(28.dp))

            DotsLoadingIndicator(visibleAlpha = dotsAlpha.value)
        }
    }
}

/**
 * Точечный индикатор: 3 круглые точки, по очереди мигают (alpha 0.25 ↔ 1.0)
 * с постоянным сдвигом фаз — даёт ощущение непрерывной активности.
 */
@Composable
private fun DotsLoadingIndicator(visibleAlpha: Float) {
    val infinite = rememberInfiniteTransition(label = "splashDots")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { i ->
            val a by infinite.animateFloat(
                initialValue = 0.25f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 700,
                        delayMillis = i * 180,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "splashDot$i"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer { alpha = a * visibleAlpha }
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

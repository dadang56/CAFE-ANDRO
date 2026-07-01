package com.lavana.dapoer.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lavana.dapoer.R
import com.lavana.dapoer.ui.theme.ForestGreen
import com.lavana.dapoer.ui.theme.OrangeAccent
import com.lavana.dapoer.ui.theme.OrangeJco
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 700))
    }
    LaunchedEffect(Unit) {
        // Masuk dengan sedikit overshoot yang halus (bukan template spinner).
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(1100)
        onAnimationFinished()
    }

    // "Breathing" lembut agar logo terasa hidup.
    val infinite = rememberInfiniteTransition(label = "breath")
    val breath by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen))),
        contentAlignment = Alignment.Center
    ) {
        // Aksen lingkaran lembut (offset agar boleh keluar tepi — bukan padding negatif).
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-50).dp)
                .height(200.dp)
                .fillMaxWidth(0.55f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 60.dp)
                .height(180.dp)
                .fillMaxWidth(0.5f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )

        Image(
            painter = painterResource(id = R.drawable.logo_lavana),
            contentDescription = "Logo Dapoer Lavana",
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(150.dp)
                .alpha(alpha.value)
                .scale(scale.value * breath),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.White)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
                .alpha(alpha.value),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = OrangeAccent,
                strokeWidth = 3.dp,
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

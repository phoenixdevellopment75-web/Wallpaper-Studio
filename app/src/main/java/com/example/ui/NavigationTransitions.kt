package com.example.ui

import android.os.Build
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Production navigation transition specifications for Wallpaper Studio.
 *
 * Implements smooth blurred return transitions across Settings and sub-screens:
 * - When drilling into a setting: Parent screen blurs (16.dp when enabled) and scales down to 0.94f with fadeOut().
 * - When returning (back gesture or arrow tap): Blur animates from 16.dp down to 0.dp concurrently with
 *   spring scale (scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f))).
 * - Honors the "Disable Blur Effects" toggle: Falls back to clean alpha crossfades and spring scale without blur.
 */
object NavigationTransitions {

    /**
     * Damping ratio = 0.78f, stiffness = 340f for tactile, responsive Material 3 navigation.
     */
    val ReturnSpringSpec: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.78f,
        stiffness = 340f
    )

    val ReturnIntOffsetSpec: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.78f,
        stiffness = 340f
    )

    /**
     * Creates the animated drill-down or return ContentTransform for AnimatedContent.
     */
    fun createTransition(
        isDrillDown: Boolean,
        disableBlurEffects: Boolean = false
    ): ContentTransform {
        return if (isDrillDown) {
            // Drill-down: Child slides in from right, parent scales to 0.94f and fades out
            val enter = slideInHorizontally(
                animationSpec = ReturnIntOffsetSpec,
                initialOffsetX = { it }
            ) + fadeIn(animationSpec = ReturnSpringSpec)

            val exit = scaleOut(
                animationSpec = ReturnSpringSpec,
                targetScale = 0.94f
            ) + slideOutHorizontally(
                animationSpec = ReturnIntOffsetSpec,
                targetOffsetX = { -it / 6 }
            ) + fadeOut(animationSpec = ReturnSpringSpec)

            enter togetherWith exit
        } else {
            // Return: Parent glides forward from 0.94f with spring scale, child slides out to right
            val enter = scaleIn(
                animationSpec = ReturnSpringSpec,
                initialScale = 0.94f
            ) + slideInHorizontally(
                animationSpec = ReturnIntOffsetSpec,
                initialOffsetX = { -it / 6 }
            ) + fadeIn(animationSpec = ReturnSpringSpec)

            val exit = slideOutHorizontally(
                animationSpec = ReturnIntOffsetSpec,
                targetOffsetX = { it }
            ) + fadeOut(animationSpec = ReturnSpringSpec)

            enter togetherWith exit
        }
    }
}

/**
 * Animates the background blur between 16.dp and 0.dp based on navigation depth,
 * honoring the disableBlurEffects preference.
 * When viewing the Settings menu or any active screen normally, targetBlur is strictly 0.dp.
 */
@Composable
fun rememberAnimatedNavigationBlur(
    isDrilledDown: Boolean,
    disableBlurEffects: Boolean
): State<Dp> {
    val targetBlur = if (!disableBlurEffects && isDrilledDown) 16.dp else 0.dp
    return animateDpAsState(
        targetValue = targetBlur,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "settings_nav_blur"
    )
}

/**
 * Applies navigation blur if enabled by user preferences and supported by Android runtime (API 31+).
 */
fun Modifier.navigationTransitionEffect(
    blurRadius: Dp,
    disableBlurEffects: Boolean
): Modifier {
    if (disableBlurEffects || blurRadius <= 0.dp || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return this
    }
    return this.blur(blurRadius)
}

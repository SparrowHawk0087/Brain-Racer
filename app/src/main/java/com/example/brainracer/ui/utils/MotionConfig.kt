package com.example.brainracer.ui.utils

enum class MotionPreset {
    Soft,
    Springy,
    UltraSoft
}

data class PressMotionSpec(
    val pressedScale: Float,
    val dampingRatio: Float,
    val stiffness: Float
)

data class NavMotionSpec(
    val tabFadeInMs: Int,
    val tabFadeOutMs: Int,
    val enterFadeMs: Int,
    val exitFadeMs: Int,
    val popEnterFadeMs: Int,
    val popExitFadeMs: Int,
    val enterOffsetFraction: Float,
    val exitOffsetFraction: Float,
    val popEnterOffsetFraction: Float,
    val popExitOffsetFraction: Float,
    val dampingRatio: Float,
    val stiffness: Float
)

data class ExpandMotionSpec(
    val enterDampingRatio: Float,
    val enterStiffness: Float,
    val exitDampingRatio: Float,
    val exitStiffness: Float,
    val sizeDampingRatio: Float,
    val sizeStiffness: Float,
    val iconDampingRatio: Float,
    val iconStiffness: Float
)

object AppMotionConfig {
    // Single switch point for the whole app.
    val currentPreset: MotionPreset = MotionPreset.UltraSoft

    val press: PressMotionSpec
        get() = when (currentPreset) {
            MotionPreset.Soft -> PressMotionSpec(
                pressedScale = 0.982f,
                dampingRatio = 0.80f,
                stiffness = 300f
            )
            MotionPreset.Springy -> PressMotionSpec(
                pressedScale = 0.975f,
                dampingRatio = 0.60f,
                stiffness = 250f
            )
            MotionPreset.UltraSoft -> PressMotionSpec(
                pressedScale = 0.988f,
                dampingRatio = 0.88f,
                stiffness = 220f
            )
        }

    val nav: NavMotionSpec
        get() = when (currentPreset) {
            MotionPreset.Soft -> NavMotionSpec(
                tabFadeInMs = 180,
                tabFadeOutMs = 160,
                enterFadeMs = 210,
                exitFadeMs = 180,
                popEnterFadeMs = 200,
                popExitFadeMs = 170,
                enterOffsetFraction = 0.10f,
                exitOffsetFraction = 0.08f,
                popEnterOffsetFraction = 0.08f,
                popExitOffsetFraction = 0.10f,
                dampingRatio = 1.0f,
                stiffness = 260f
            )
            MotionPreset.Springy -> NavMotionSpec(
                tabFadeInMs = 200,
                tabFadeOutMs = 180,
                enterFadeMs = 230,
                exitFadeMs = 200,
                popEnterFadeMs = 220,
                popExitFadeMs = 190,
                enterOffsetFraction = 0.12f,
                exitOffsetFraction = 0.10f,
                popEnterOffsetFraction = 0.10f,
                popExitOffsetFraction = 0.12f,
                dampingRatio = 0.78f,
                stiffness = 220f
            )
            MotionPreset.UltraSoft -> NavMotionSpec(
                tabFadeInMs = 240,
                tabFadeOutMs = 220,
                enterFadeMs = 260,
                exitFadeMs = 230,
                popEnterFadeMs = 240,
                popExitFadeMs = 220,
                enterOffsetFraction = 0.06f,
                exitOffsetFraction = 0.05f,
                popEnterOffsetFraction = 0.05f,
                popExitOffsetFraction = 0.06f,
                dampingRatio = 1.0f,
                stiffness = 180f
            )
        }

    val expand: ExpandMotionSpec
        get() = when (currentPreset) {
            MotionPreset.Soft -> ExpandMotionSpec(
                enterDampingRatio = 0.82f,
                enterStiffness = 260f,
                exitDampingRatio = 1.0f,
                exitStiffness = 280f,
                sizeDampingRatio = 0.84f,
                sizeStiffness = 250f,
                iconDampingRatio = 0.86f,
                iconStiffness = 270f
            )
            MotionPreset.Springy -> ExpandMotionSpec(
                enterDampingRatio = 0.72f,
                enterStiffness = 220f,
                exitDampingRatio = 0.94f,
                exitStiffness = 240f,
                sizeDampingRatio = 0.76f,
                sizeStiffness = 210f,
                iconDampingRatio = 0.74f,
                iconStiffness = 225f
            )
            MotionPreset.UltraSoft -> ExpandMotionSpec(
                enterDampingRatio = 0.90f,
                enterStiffness = 180f,
                exitDampingRatio = 1.0f,
                exitStiffness = 200f,
                sizeDampingRatio = 0.92f,
                sizeStiffness = 170f,
                iconDampingRatio = 0.90f,
                iconStiffness = 190f
            )
        }
}

package com.cortex.app.core.navigation

import kotlinx.serialization.Serializable

sealed interface CortexRoute {

    @Serializable
    data object Home : CortexRoute

    @Serializable
    data class Lesson(
        val lessonId: String,
        val restart: Boolean = false,
    ) : CortexRoute

    @Serializable
    data object Library : CortexRoute

    @Serializable
    data object Review : CortexRoute

    @Serializable
    data object Progress : CortexRoute

    @Serializable
    data object Session : CortexRoute
}

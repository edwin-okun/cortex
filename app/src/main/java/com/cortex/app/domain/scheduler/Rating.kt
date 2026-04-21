package com.cortex.app.domain.scheduler

enum class Rating(val grade: Int) {
    Again(1), Hard(2), Good(3), Easy(4);

    operator fun minus(other: Int): Int = grade - other
    operator fun compareTo(other: Int): Int = grade.compareTo(other)
}

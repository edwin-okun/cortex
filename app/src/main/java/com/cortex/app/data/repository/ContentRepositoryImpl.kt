package com.cortex.app.data.repository

import com.cortex.app.data.local.content.lessons.HashMaps
import com.cortex.app.data.local.content.lessons.Recursion
import com.cortex.app.data.local.content.lessons.Sorting
import com.cortex.app.data.local.content.lessons.Traversals
import com.cortex.app.data.local.content.lessons.TwoPointers
import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.repository.ContentRepository

class ContentRepositoryImpl : ContentRepository {

    private val catalog: Map<String, Lesson> = listOf(
        TwoPointers,
        HashMaps,
        Recursion,
        Traversals,
        Sorting,
    ).associateBy { it.id }

    override fun getLesson(lessonId: String): Lesson? = catalog[lessonId]
    override fun getAllLessons(): List<Lesson> = catalog.values.toList()
}

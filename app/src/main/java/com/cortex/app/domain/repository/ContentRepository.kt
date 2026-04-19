package com.cortex.app.domain.repository

import com.cortex.app.domain.model.Lesson

interface ContentRepository {
    fun getLesson(lessonId: String): Lesson?
    fun getAllLessons(): List<Lesson>
}

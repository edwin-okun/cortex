package com.cortex.app.domain.usecase

import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.repository.ContentRepository

class GetLessonUseCase(private val contentRepository: ContentRepository) {
    operator fun invoke(lessonId: String): Lesson? = contentRepository.getLesson(lessonId)
}

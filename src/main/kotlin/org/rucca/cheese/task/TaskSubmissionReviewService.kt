package org.rucca.cheese.task

import java.time.LocalDateTime
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TaskSubmissionReviewDTO
import org.rucca.cheese.model.TaskSubmissionReviewDetailDTO
import org.rucca.cheese.task.error.TaskSubmissionAlreadyReviewedError
import org.rucca.cheese.task.error.TaskSubmissionNotReviewedYetError
import org.springframework.stereotype.Service

@Service
class TaskSubmissionReviewService(
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val TaskSubmissionReviewRepository: TaskSubmissionReviewRepository,
) {
    fun getReviewDTO(submissionId: IdType): TaskSubmissionReviewDTO {
        val reviewOpt = TaskSubmissionReviewRepository.findBySubmissionId(submissionId)
        return if (reviewOpt.isPresent) {
            val review = reviewOpt.get()
            TaskSubmissionReviewDTO(
                reviewed = true,
                detail =
                    TaskSubmissionReviewDetailDTO(
                        accepted = review.accepted!!,
                        score = review.score!!,
                        comment = review.comment!!,
                    ),
            )
        } else {
            TaskSubmissionReviewDTO(reviewed = false)
        }
    }

    fun ensureSubmissionExists(submissionId: IdType) {
        if (!taskSubmissionRepository.existsById(submissionId)) {
            throw NotFoundError("task submission", submissionId)
        }
    }

    fun ensureReviewNotExist(submissionId: IdType) {
        if (TaskSubmissionReviewRepository.existsBySubmissionId(submissionId)) {
            throw TaskSubmissionAlreadyReviewedError(submissionId)
        }
    }

    fun createReview(submissionId: IdType, accepted: Boolean, score: Int, comment: String) {
        ensureSubmissionExists(submissionId)
        ensureReviewNotExist(submissionId)
        val review =
            TaskSubmissionReview(
                submission = TaskSubmission().apply { id = submissionId },
                accepted = accepted,
                score = score,
                comment = comment,
            )
        TaskSubmissionReviewRepository.save(review)
    }

    private fun getTaskSubmissionReview(submissionId: IdType): TaskSubmissionReview {
        return TaskSubmissionReviewRepository.findBySubmissionId(submissionId).orElseThrow {
            throw TaskSubmissionNotReviewedYetError(submissionId)
        }
    }

    fun deleteReview(submissionId: IdType) {
        val review = getTaskSubmissionReview(submissionId)
        review.deletedAt = LocalDateTime.now()
        TaskSubmissionReviewRepository.save(review)
    }

    fun updateReviewAccepted(submissionId: IdType, accepted: Boolean) {
        val review = getTaskSubmissionReview(submissionId)
        review.accepted = accepted
        TaskSubmissionReviewRepository.save(review)
    }

    fun updateReviewScore(submissionId: IdType, score: Int) {
        val review = getTaskSubmissionReview(submissionId)
        review.score = score
        TaskSubmissionReviewRepository.save(review)
    }

    fun updateReviewComment(submissionId: IdType, comment: String) {
        val review = getTaskSubmissionReview(submissionId)
        review.comment = comment
        TaskSubmissionReviewRepository.save(review)
    }
}

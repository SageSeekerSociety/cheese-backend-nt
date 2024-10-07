package org.rucca.cheese.task

import java.time.LocalDateTime
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TaskSubmissionReviewDTO
import org.rucca.cheese.model.TaskSubmissionReviewDetailDTO
import org.rucca.cheese.space.SpaceUserRankService
import org.rucca.cheese.task.error.TaskSubmissionAlreadyReviewedError
import org.rucca.cheese.task.error.TaskSubmissionNotReviewedYetError
import org.springframework.stereotype.Service

@Service
class TaskSubmissionReviewService(
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val taskSubmissionReviewRepository: TaskSubmissionReviewRepository,
    private val spaceUserRankService: SpaceUserRankService,
) {
    fun getReviewDTO(submissionId: IdType): TaskSubmissionReviewDTO {
        val reviewOpt = taskSubmissionReviewRepository.findBySubmissionId(submissionId)
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
        if (taskSubmissionReviewRepository.existsBySubmissionId(submissionId)) {
            throw TaskSubmissionAlreadyReviewedError(submissionId)
        }
    }

    /*
     * @Returns Has upgraded submitter's rank
     */
    fun createReview(
        submissionId: IdType,
        accepted: Boolean,
        score: Int,
        comment: String,
    ): Boolean {
        ensureSubmissionExists(submissionId)
        ensureReviewNotExist(submissionId)
        val review =
            TaskSubmissionReview(
                submission = TaskSubmission().apply { id = submissionId },
                accepted = accepted,
                score = score,
                comment = comment,
            )
        taskSubmissionReviewRepository.save(review)
        return tryUpdateParticipantRank(review)
    }

    private fun getTaskSubmissionReview(submissionId: IdType): TaskSubmissionReview {
        return taskSubmissionReviewRepository.findBySubmissionId(submissionId).orElseThrow {
            throw TaskSubmissionNotReviewedYetError(submissionId)
        }
    }

    fun deleteReview(submissionId: IdType) {
        val review = getTaskSubmissionReview(submissionId)
        review.deletedAt = LocalDateTime.now()
        taskSubmissionReviewRepository.save(review)
    }

    /*
     * @Returns Has upgraded submitter's rank
     */
    fun updateReviewAccepted(submissionId: IdType, accepted: Boolean): Boolean {
        val review = getTaskSubmissionReview(submissionId)
        review.accepted = accepted
        taskSubmissionReviewRepository.save(review)
        return tryUpdateParticipantRank(review)
    }

    fun updateReviewScore(submissionId: IdType, score: Int) {
        val review = getTaskSubmissionReview(submissionId)
        review.score = score
        taskSubmissionReviewRepository.save(review)
    }

    fun updateReviewComment(submissionId: IdType, comment: String) {
        val review = getTaskSubmissionReview(submissionId)
        review.comment = comment
        taskSubmissionReviewRepository.save(review)
    }

    private fun tryUpdateParticipantRank(review: TaskSubmissionReview): Boolean {
        if (!review.accepted!!) return false
        val submission = taskSubmissionRepository.findById(review.submission!!.id!!).get()
        val membership = submission.membership!!
        val task = membership.task!!
        val needUpgradeRank =
            task.submitterType == TaskSubmitterType.USER && task.space != null && task.rank != null
        if (needUpgradeRank) {
            return spaceUserRankService.upgradeRank(
                task.space!!.id!!,
                membership.memberId!!,
                task.rank!!
            )
        } else return false
    }
}

/*
 *  Description: This file implements the TaskSubmissionReviewService class.
 *               It is responsible for CRUD operations on task submission reviews.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task.service

import java.time.LocalDateTime
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TaskSubmissionReviewDTO
import org.rucca.cheese.model.TaskSubmissionReviewDetailDTO
import org.rucca.cheese.space.SpaceUserRankService
import org.rucca.cheese.task.*
import org.rucca.cheese.task.error.TaskSubmissionAlreadyReviewedError
import org.rucca.cheese.task.error.TaskSubmissionNotReviewedYetError
import org.rucca.cheese.task.event.TaskMembershipStatusUpdateEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class TaskSubmissionReviewService(
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val taskSubmissionReviewRepository: TaskSubmissionReviewRepository,
    private val spaceUserRankService: SpaceUserRankService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(TaskSubmissionReviewService::class.java)

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
                .let { taskSubmissionReviewRepository.save(it) }
        val membershipId = review.submission?.membership?.id
        if (membershipId != null) {
            eventPublisher.publishEvent(TaskMembershipStatusUpdateEvent(this, membershipId))
            log.debug(
                "Published status update event after saving/updating review {} for membership {}",
                review.id,
                membershipId,
            )
        } else {
            log.warn("Saved TaskSubmissionReview {} has no associated membership ID.", review.id)
        }
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
        val savedReview = taskSubmissionReviewRepository.save(review)
        val membershipId = savedReview.submission?.membership?.id
        if (membershipId != null) {
            eventPublisher.publishEvent(TaskMembershipStatusUpdateEvent(this, membershipId))
            log.debug(
                "Published status update event after saving/updating review {} for membership {}",
                savedReview.id,
                membershipId,
            )
        } else {
            log.warn(
                "Saved TaskSubmissionReview {} has no associated membership ID.",
                savedReview.id,
            )
        }
        return tryUpdateParticipantRank(review)
    }

    fun updateReviewScore(submissionId: IdType, score: Int) {
        val review = getTaskSubmissionReview(submissionId)
        review.score = score
        val savedReview = taskSubmissionReviewRepository.save(review)
        val membershipId = savedReview.submission?.membership?.id
        if (membershipId != null) {
            eventPublisher.publishEvent(TaskMembershipStatusUpdateEvent(this, membershipId))
            log.debug(
                "Published status update event after saving/updating review {} for membership {}",
                savedReview.id,
                membershipId,
            )
        } else {
            log.warn(
                "Saved TaskSubmissionReview {} has no associated membership ID.",
                savedReview.id,
            )
        }
    }

    fun updateReviewComment(submissionId: IdType, comment: String) {
        val review = getTaskSubmissionReview(submissionId)
        review.comment = comment
        val savedReview = taskSubmissionReviewRepository.save(review)
        val membershipId = savedReview.submission?.membership?.id
        if (membershipId != null) {
            eventPublisher.publishEvent(TaskMembershipStatusUpdateEvent(this, membershipId))
            log.debug(
                "Published status update event after saving/updating review {} for membership {}",
                savedReview.id,
                membershipId,
            )
        } else {
            log.warn(
                "Saved TaskSubmissionReview {} has no associated membership ID.",
                savedReview.id,
            )
        }
    }

    private fun tryUpdateParticipantRank(review: TaskSubmissionReview): Boolean {
        if (!review.accepted!!) return false
        val submission = taskSubmissionRepository.findById(review.submission!!.id!!).get()
        val membership = submission.membership!!
        val task = membership.task!!
        val needUpgradeRank =
            task.submitterType == TaskSubmitterType.USER &&
                task.space != null &&
                task.space.enableRank!! &&
                task.rank != null
        if (needUpgradeRank) {
            return spaceUserRankService.upgradeRank(
                task.space!!.id!!,
                membership.memberId!!,
                task.rank!!,
            )
        } else return false
    }
}

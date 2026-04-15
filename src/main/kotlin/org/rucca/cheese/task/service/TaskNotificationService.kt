package org.rucca.cheese.task.service

import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.notification.event.NotificationTriggerEvent
import org.rucca.cheese.notification.models.NotificationType
import org.rucca.cheese.space.SpaceService
import org.rucca.cheese.team.TeamService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class TaskNotificationService(
    private val eventPublisher: ApplicationEventPublisher,
    private val spaceService: SpaceService,
    private val teamService: TeamService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getSpaceOwnerAndAdminIds(spaceId: IdType): Set<Long> {
        return spaceService.getSpaceAdminAndOwnerIds(spaceId)
    }

    fun getTeamOwnerAndAdminIds(teamId: IdType): Set<Long> {
        return teamService.getTeamAdminAndOwnerIds(teamId)
    }

    fun publishToSpaceOwners(
        spaceId: IdType,
        type: NotificationType,
        payload: Map<String, Any>,
        actorId: Long? = null,
    ) {
        publishNotification(
            recipientIds = getSpaceOwnerAndAdminIds(spaceId),
            type = type,
            payload = payload,
            actorId = actorId,
        )
    }

    fun publishToTeamOwners(
        teamId: IdType,
        type: NotificationType,
        payload: Map<String, Any>,
        actorId: Long? = null,
    ) {
        publishNotification(
            recipientIds = getTeamOwnerAndAdminIds(teamId),
            type = type,
            payload = payload,
            actorId = actorId,
        )
    }

    fun publishToParticipantOrTeamOwners(
        memberId: IdType,
        isTeam: Boolean,
        type: NotificationType,
        payload: Map<String, Any>,
        actorId: Long? = null,
    ) {
        if (isTeam) {
            publishToTeamOwners(memberId, type, payload, actorId)
        } else {
            publishNotification(setOf(memberId), type, payload, actorId)
        }
    }

    fun publishNotification(
        recipientIds: Set<Long>,
        type: NotificationType,
        payload: Map<String, Any>,
        actorId: Long? = null,
    ) {
        if (recipientIds.isEmpty()) {
            log.warn(
                "Attempted to publish task notification type {} but recipient set was empty. Payload: {}",
                type,
                payload,
            )
            return
        }

        val event =
            NotificationTriggerEvent(
                source = this,
                recipientIds = recipientIds,
                type = type,
                payload = payload,
                actorId = actorId,
            )
        eventPublisher.publishEvent(event)
        log.info(
            "Published task notification event: type={}, recipients={}, actor={}",
            type,
            recipientIds,
            actorId,
        )
    }

    fun buildTaskPayload(
        taskId: IdType,
        taskName: String,
        spaceId: IdType,
        spaceName: String,
        actorId: IdType? = null,
        actorName: String? = null,
        extraFields: Map<String, Any> = emptyMap(),
    ): Map<String, Any> {
        return buildPayload(
            taskId = taskId,
            taskName = taskName,
            spaceId = spaceId,
            spaceName = spaceName,
            actorId = actorId,
            actorName = actorName,
            extraFields = extraFields,
        )
    }

    fun buildMembershipPayload(
        taskId: IdType,
        taskName: String,
        spaceId: IdType,
        spaceName: String,
        membershipId: IdType,
        participantId: IdType,
        participantName: String,
        participantType: String,
        teamId: IdType? = null,
        teamName: String? = null,
        actorId: IdType? = null,
        actorName: String? = null,
        extraFields: Map<String, Any> = emptyMap(),
    ): Map<String, Any> {
        return buildPayload(
            taskId = taskId,
            taskName = taskName,
            spaceId = spaceId,
            spaceName = spaceName,
            actorId = actorId,
            actorName = actorName,
            extraFields =
                buildMap {
                    put("membership", createEntityReference("task_membership", membershipId))
                    put("membershipId", membershipId)
                    put("participantId", participantId)
                    put("participantName", participantName)
                    put("participantType", participantType)
                    put("participant", createEntityReference(participantType, participantId))
                    if (teamId != null) {
                        put("teamId", teamId)
                        put("team", createEntityReference("team", teamId))
                    }
                    if (teamName != null) {
                        put("teamName", teamName)
                    }
                    putAll(extraFields)
                },
        )
    }

    fun buildSubmissionPayload(
        taskId: IdType,
        taskName: String,
        spaceId: IdType,
        spaceName: String,
        membershipId: IdType,
        participantId: IdType,
        participantName: String,
        participantType: String,
        submissionId: IdType,
        submissionVersion: Int,
        submitterId: IdType,
        submitterName: String,
        teamId: IdType? = null,
        teamName: String? = null,
        actorId: IdType? = null,
        actorName: String? = null,
        extraFields: Map<String, Any> = emptyMap(),
    ): Map<String, Any> {
        return buildPayload(
            taskId = taskId,
            taskName = taskName,
            spaceId = spaceId,
            spaceName = spaceName,
            actorId = actorId,
            actorName = actorName,
            extraFields =
                buildMap {
                    put("membership", createEntityReference("task_membership", membershipId))
                    put("membershipId", membershipId)
                    put("participantId", participantId)
                    put("participantName", participantName)
                    put("participantType", participantType)
                    put("participant", createEntityReference(participantType, participantId))
                    if (teamId != null) {
                        put("teamId", teamId)
                        put("team", createEntityReference("team", teamId))
                    }
                    if (teamName != null) {
                        put("teamName", teamName)
                    }
                    put("submission", createEntityReference("task_submission", submissionId))
                    put("submissionId", submissionId)
                    put("submissionVersion", submissionVersion)
                    put("submitterId", submitterId)
                    put("submitterName", submitterName)
                    put("submitter", createEntityReference("user", submitterId))
                    putAll(extraFields)
                },
        )
    }

    fun buildReviewPayload(
        taskId: IdType,
        taskName: String,
        spaceId: IdType,
        spaceName: String,
        membershipId: IdType,
        participantId: IdType,
        participantName: String,
        participantType: String,
        submissionId: IdType,
        submissionVersion: Int,
        accepted: Boolean,
        score: Int,
        comment: String,
        teamId: IdType? = null,
        teamName: String? = null,
        reviewerId: IdType? = null,
        reviewerName: String? = null,
        actorId: IdType? = null,
        actorName: String? = null,
        extraFields: Map<String, Any> = emptyMap(),
    ): Map<String, Any> {
        return buildPayload(
            taskId = taskId,
            taskName = taskName,
            spaceId = spaceId,
            spaceName = spaceName,
            actorId = actorId,
            actorName = actorName,
            extraFields =
                buildMap {
                    put("membership", createEntityReference("task_membership", membershipId))
                    put("membershipId", membershipId)
                    put("participantId", participantId)
                    put("participantName", participantName)
                    put("participantType", participantType)
                    put("participant", createEntityReference(participantType, participantId))
                    if (teamId != null) {
                        put("teamId", teamId)
                        put("team", createEntityReference("team", teamId))
                    }
                    if (teamName != null) {
                        put("teamName", teamName)
                    }
                    put("submission", createEntityReference("task_submission", submissionId))
                    put("submissionId", submissionId)
                    put("submissionVersion", submissionVersion)
                    put("accepted", accepted)
                    put("score", score)
                    put("comment", comment)
                    if (reviewerId != null) {
                        put("reviewerId", reviewerId)
                        put("reviewer", createEntityReference("user", reviewerId))
                    }
                    if (reviewerName != null) {
                        put("reviewerName", reviewerName)
                    }
                    putAll(extraFields)
                },
        )
    }

    private fun buildPayload(
        taskId: IdType,
        taskName: String,
        spaceId: IdType,
        spaceName: String,
        actorId: IdType?,
        actorName: String?,
        extraFields: Map<String, Any>,
    ): Map<String, Any> {
        return buildMap {
            put("task", createEntityReference("task", taskId))
            put("taskId", taskId)
            put("taskName", taskName)
            put("space", createEntityReference("space", spaceId))
            put("spaceId", spaceId)
            put("spaceName", spaceName)
            if (actorId != null) {
                put("actorId", actorId)
                put("actor", createEntityReference("user", actorId))
            }
            if (actorName != null) {
                put("actorName", actorName)
            }
            putAll(extraFields)
        }
    }

    private fun createEntityReference(type: String, id: IdType): Map<String, String> {
        return mapOf("type" to type, "id" to id.toString())
    }
}

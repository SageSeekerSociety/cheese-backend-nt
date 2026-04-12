package org.rucca.cheese.notification.services

import freemarker.template.Configuration
import java.io.StringWriter
import java.util.Locale
import org.rucca.cheese.notification.models.NotificationType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NotificationEmailBodyRenderer(private val freemarkerConfiguration: Configuration) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun render(type: NotificationType, payload: Map<String, Any>, locale: Locale): String {
        val model = buildModel(type, payload)
        return renderTemplate("notifications/${type.name.lowercase()}.ftlh", locale, model)
    }

    private fun renderTemplate(
        templateName: String,
        locale: Locale,
        model: Map<String, Any?>,
    ): String {
        return try {
            processTemplate(templateName, locale, model)
        } catch (firstFailure: Exception) {
            log.warn(
                "Failed rendering template {} for locale {}: {}",
                templateName,
                locale,
                firstFailure.message,
            )
            processTemplate("notifications/default.ftlh", locale, model)
        }
    }

    private fun processTemplate(
        templateName: String,
        locale: Locale,
        model: Map<String, Any?>,
    ): String {
        val template = freemarkerConfiguration.getTemplate(templateName, locale)
        return StringWriter().use { writer ->
            template.process(model, writer)
            writer.toString()
        }
    }

    private fun buildModel(type: NotificationType, payload: Map<String, Any>): Map<String, Any?> {
        val summary = summaryFor(type, payload)
        val note = noteFor(type, payload)
        return buildMap {
            put("type", type.name)
            put("heading", humanize(type.name))
            put("summary", summary)
            if (note != null) {
                put("note", note)
            }
            putAll(payload)
            putDerivedNames(payload)
        }
    }

    private fun MutableMap<String, Any?>.putDerivedNames(payload: Map<String, Any>) {
        put(
            "actorName",
            firstNamedValue(
                payload,
                "actorName",
                "actor",
                "mentioner",
                "replier",
                "inviter",
                "requester",
                "accepter",
                "decliner",
                "canceler",
                "approver",
                "rejector",
                "participant",
                "submitter",
                "reviewer",
            ),
        )
        put("taskName", firstStringValue(payload, "taskName"))
        put("spaceName", firstStringValue(payload, "spaceName"))
        put("projectName", firstStringValue(payload, "projectName"))
        put("teamName", firstStringValue(payload, "teamName"))
        put("discussionTitle", firstStringValue(payload, "discussionTitle"))
        put("role", firstStringValue(payload, "role")?.lowercase())
        put("reactionEmoji", firstStringValue(payload, "reactionEmoji"))
        put("participantName", firstStringValue(payload, "participantName"))
        put("submitterName", firstStringValue(payload, "submitterName"))
        put("reviewerName", firstStringValue(payload, "reviewerName"))
        put(
            "reason",
            firstNonBlankString(payload, "reason", "rejectReason", "rejectionReason", "comment"),
        )
    }

    private fun summaryFor(type: NotificationType, payload: Map<String, Any>): String {
        val actorName =
            firstNamedValue(
                payload,
                "actorName",
                "actor",
                "mentioner",
                "replier",
                "inviter",
                "requester",
                "accepter",
                "decliner",
                "canceler",
                "approver",
                "rejector",
                "participant",
                "submitter",
                "reviewer",
            ) ?: "Someone"
        val discussionTitle = firstStringValue(payload, "discussionTitle") ?: "the discussion"
        val reactionEmoji = firstStringValue(payload, "reactionEmoji") ?: "a reaction"
        val projectName = firstStringValue(payload, "projectName") ?: "the project"
        val taskName = firstStringValue(payload, "taskName") ?: "Untitled task"
        val spaceName = firstStringValue(payload, "spaceName") ?: "the space"
        val teamName = firstStringValue(payload, "teamName") ?: "the team"
        val role = firstStringValue(payload, "role")?.lowercase() ?: "member"
        val participantName =
            firstStringValue(payload, "participantName")
                ?: firstNamedValue(payload, "participant")
                ?: "Someone"
        val submitterName =
            firstStringValue(payload, "submitterName")
                ?: firstNamedValue(payload, "submitter")
                ?: "Someone"
        val reviewerName =
            firstStringValue(payload, "reviewerName")
                ?: firstNamedValue(payload, "reviewer")
                ?: "Someone"
        val submissionVersion = payload["submissionVersion"]?.toString() ?: "1"

        return when (type) {
            NotificationType.MENTION ->
                "$actorName mentioned you in discussion \"$discussionTitle\""
            NotificationType.REPLY -> "$actorName replied to your comment in \"$discussionTitle\""
            NotificationType.REACTION -> "$actorName reacted with $reactionEmoji to your item"
            NotificationType.PROJECT_INVITE ->
                "$actorName invited you to join the project \"$projectName\""
            NotificationType.DEADLINE_REMIND ->
                "Task \"$taskName\" in project \"$projectName\" is due soon"
            NotificationType.TEAM_JOIN_REQUEST -> "$actorName requested to join team \"$teamName\""
            NotificationType.TEAM_INVITATION ->
                "$actorName invited you to join team \"$teamName\" as a $role"
            NotificationType.TEAM_REQUEST_APPROVED ->
                "Your request to join team \"$teamName\" was approved"
            NotificationType.TEAM_REQUEST_REJECTED ->
                "Your request to join team \"$teamName\" was rejected"
            NotificationType.TEAM_INVITATION_ACCEPTED ->
                "$actorName accepted your invitation to join team \"$teamName\""
            NotificationType.TEAM_INVITATION_DECLINED ->
                "$actorName declined your invitation to join team \"$teamName\""
            NotificationType.TEAM_INVITATION_CANCELED ->
                "The invitation to join team \"$teamName\" was canceled by $actorName"
            NotificationType.TEAM_REQUEST_CANCELED ->
                "$actorName canceled their request to join team \"$teamName\""
            NotificationType.TASK_PENDING_APPROVAL ->
                "$actorName submitted task \"$taskName\" in space \"$spaceName\" for approval"
            NotificationType.TASK_APPROVED ->
                "Task \"$taskName\" in space \"$spaceName\" was approved by $actorName"
            NotificationType.TASK_REJECTED ->
                "Task \"$taskName\" in space \"$spaceName\" was rejected by $actorName"
            NotificationType.TASK_RESUBMITTED ->
                "$actorName resubmitted task \"$taskName\" in space \"$spaceName\""
            NotificationType.TASK_PARTICIPANT_APPLIED ->
                "$participantName applied to participate in task \"$taskName\""
            NotificationType.TASK_PARTICIPANT_APPROVED ->
                "$participantName was approved for task \"$taskName\" by $actorName"
            NotificationType.TASK_PARTICIPANT_REJECTED ->
                "$participantName was rejected for task \"$taskName\" by $actorName"
            NotificationType.TASK_PARTICIPANT_AUTO_REJECTED ->
                "$participantName was auto-rejected for task \"$taskName\""
            NotificationType.TASK_SUBMISSION_CREATED ->
                "$submitterName created submission v$submissionVersion for task \"$taskName\""
            NotificationType.TASK_SUBMISSION_UPDATED ->
                "$submitterName updated submission v$submissionVersion for task \"$taskName\""
            NotificationType.TASK_SUBMISSION_APPROVED ->
                "Submission v$submissionVersion for task \"$taskName\" was approved by $reviewerName"
            NotificationType.TASK_SUBMISSION_REJECTED ->
                "Submission v$submissionVersion for task \"$taskName\" was rejected by $reviewerName"
        }
    }

    private fun noteFor(type: NotificationType, payload: Map<String, Any>): String? {
        val score = payload["score"]?.toString()?.takeIf { it.isNotBlank() }
        val comment = payload["comment"]?.toString()?.takeIf { it.isNotBlank() }
        val reason = firstNonBlankString(payload, "reason", "rejectReason", "rejectionReason")
        return when (type) {
            NotificationType.TASK_REJECTED,
            NotificationType.TASK_PARTICIPANT_REJECTED,
            NotificationType.TASK_PARTICIPANT_AUTO_REJECTED,
            NotificationType.TASK_SUBMISSION_REJECTED -> reason ?: comment
            NotificationType.TASK_SUBMISSION_APPROVED -> score?.let { "Score: $it" } ?: comment
            NotificationType.TASK_PARTICIPANT_APPROVED -> comment
            else -> null
        }
    }

    private fun firstStringValue(payload: Map<String, Any>, key: String): String? {
        return payload[key]?.let { value ->
            when (value) {
                is String -> value.takeIf { it.isNotBlank() }
                is Map<*, *> -> value["name"]?.toString()?.takeIf { it.isNotBlank() }
                else -> value.toString().takeIf { it.isNotBlank() }
            }
        }
    }

    private fun firstNamedValue(payload: Map<String, Any>, vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key -> firstStringValue(payload, key) }
    }

    private fun firstNonBlankString(payload: Map<String, Any>, vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            payload[key]?.toString()?.takeIf { it.isNotBlank() }
        }
    }

    private fun humanize(value: String): String {
        return value.lowercase().replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
    }
}

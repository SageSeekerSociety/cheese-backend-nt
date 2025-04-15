package org.rucca.cheese.notification.services // Corrected package name convention

// package exists
import java.util.Locale
import org.rucca.cheese.notification.config.NotificationProperties
import org.rucca.cheese.notification.models.NotificationChannel // Ensure correct import if models
import org.rucca.cheese.notification.models.NotificationType // Ensure correct import
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.stereotype.Service

@Service
class NotificationTemplateService(
    private val messageSource: MessageSource,
    private val notificationProperties: NotificationProperties,
    private val userService: UserService,
    private val teamService: TeamService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Renders the title for a notification using indexed arguments. */
    fun renderTitle(
        type: NotificationType,
        channel: NotificationChannel, // Channel might influence key in future, kept for signature
        // consistency
        payload: Map<String, Any>,
        locale: Locale,
    ): String {
        val messageKey = "notification.${type.name.lowercase()}.title"
        // Build the ordered argument array based on the type
        val argsArray = buildArgumentsArray(type, payload, "title")
        // Use the standard getMessage method with the arguments array
        return getMessage(messageKey, argsArray, locale, "Default Title: ${type.name}")
    }

    /** Renders the body for a notification using indexed arguments. */
    fun renderBody(
        type: NotificationType,
        channel: NotificationChannel, // Check for channel-specific keys like '.html'
        payload: Map<String, Any>,
        locale: Locale,
    ): String {
        val channelSuffix =
            if (channel == NotificationChannel.EMAIL) ".html" else "" // Keep suffix logic if needed
        val messageKey = "notification.${type.name.lowercase()}.body$channelSuffix"
        // Build the ordered argument array based on the type
        val argsArray = buildArgumentsArray(type, payload, "body")
        // Use the standard getMessage method with the arguments array
        return getMessage(messageKey, argsArray, locale, "Default Body: ${type.name}")
    }

    /** Renders the title for an aggregated notification using indexed arguments. */
    fun renderAggregatedTitle(
        type: NotificationType,
        metadata: Map<String, Any>,
        locale: Locale,
    ): String {
        val messageKey = "notification.${type.name.lowercase()}.aggregated.title"
        val argsArray = buildArgumentsArrayForAggregation(type, metadata, "title")
        return getMessage(messageKey, argsArray, locale, "Aggregated Update")
    }

    /** Renders the body for an aggregated notification using indexed arguments. */
    fun renderAggregatedBody(
        type: NotificationType,
        metadata: Map<String, Any>,
        locale: Locale,
    ): String {
        val messageKey = "notification.${type.name.lowercase()}.aggregated.body"
        val argsArray = buildArgumentsArrayForAggregation(type, metadata, "body")
        return getMessage(messageKey, argsArray, locale, "Aggregated Update Details")
    }

    /** Retrieves the message from MessageSource using indexed arguments. */
    private fun getMessage(
        key: String,
        args: Array<Any?>,
        locale: Locale,
        defaultMessage: String,
    ): String {
        log.debug("Attempting to render message key '{}' with locale: {}", key, locale)
        return try {
            // Use MessageSource directly with the object array
            messageSource.getMessage(key, args, locale)
        } catch (e: NoSuchMessageException) {
            log.warn("Missing i18n message key: {} for locale {}", key, locale)
            // Provide a more informative fallback
            "$defaultMessage (Key: $key, Locale: $locale, Args: ${args.joinToString { it?.toString() ?: "null" }})"
        } catch (e: Exception) {
            log.error("Error formatting message for key {}: {}", key, e.message, e)
            // Fallback in case of formatting errors
            "$defaultMessage (Formatting Error for Key: $key)"
        }
    }

    /**
     * Builds the ordered argument array for standard notifications. The order MUST match the
     * placeholders {0}, {1} etc. in the properties file for the given type.
     */
    private fun buildArgumentsArray(
        type: NotificationType,
        payload: Map<String, Any>,
        context: String,
    ): Array<Any?> {
        // Carefully define the argument order for each notification type
        return when (type) {
            // Args: {0}=mentionerUsername, {1}=discussionTitle
            NotificationType.MENTION ->
                arrayOf(
                    getUsername(payload["mentionerId"] as? Long),
                    payload["discussionTitle"], // Assuming discussionTitle is directly in payload
                )
            // Args: {0}=replierUsername, {1}=discussionTitle
            NotificationType.REPLY ->
                arrayOf(getUsername(payload["replierId"] as? Long), payload["discussionTitle"])
            // Args: {0}=reactorUsername, {1}=reactionEmoji
            NotificationType.REACTION ->
                arrayOf(
                    getUsername(payload["reactorId"] as? Long),
                    payload["reactionEmoji"], // Assuming reactionEmoji is directly in payload
                )
            // Args: {0}=inviterUsername, {1}=projectName
            NotificationType.PROJECT_INVITE ->
                arrayOf(
                    getUsername(payload["inviterId"] as? Long),
                    payload["projectName"], // Assuming projectName is directly in payload
                )
            // Args: {0}=taskName, {1}=projectName
            NotificationType.DEADLINE_REMIND ->
                arrayOf(
                    payload["taskName"], // Assuming taskName is directly in payload
                    payload["projectName"],
                )

            // --- Team Notifications ---
            // Args: {0}=requesterUsername, {1}=teamName
            NotificationType.TEAM_JOIN_REQUEST ->
                arrayOf(
                    getUsername(payload["requesterId"] as? Long),
                    getTeamName(payload["teamId"] as? Long),
                    // Note: message payload['message'] is not typically included as a positional
                    // arg
                )
            // Args: {0}=inviterUsername, {1}=teamName, {2}=role
            NotificationType.TEAM_INVITATION ->
                arrayOf(
                    getUsername(payload["inviterId"] as? Long),
                    getTeamName(payload["teamId"] as? Long),
                    (payload["role"] as? String)?.lowercase() ?: "member", // Role as {2}
                    // Note: message payload['message'] is not typically included as a positional
                    // arg
                )
            // Args: {0}=teamName, {1}=approverUsername
            NotificationType.TEAM_REQUEST_APPROVED ->
                arrayOf(
                    getTeamName(payload["teamId"] as? Long),
                    getUsername(payload["approverId"] as? Long, "Admin"),
                )
            // Args: {0}=teamName, {1}=rejectorUsername
            NotificationType.TEAM_REQUEST_REJECTED ->
                arrayOf(
                    getTeamName(payload["teamId"] as? Long),
                    getUsername(payload["rejectorId"] as? Long, "Admin"),
                )
            // Args: {0}=accepterUsername, {1}=teamName
            NotificationType.TEAM_INVITATION_ACCEPTED ->
                arrayOf(
                    getUsername(payload["accepterId"] as? Long),
                    getTeamName(payload["teamId"] as? Long),
                )
            // Args: {0}=declinerUsername, {1}=teamName
            NotificationType.TEAM_INVITATION_DECLINED ->
                arrayOf(
                    getUsername(payload["declinerId"] as? Long),
                    getTeamName(payload["teamId"] as? Long),
                )
            // Args: {0}=teamName, {1}=cancelerUsername (Admin)
            NotificationType.TEAM_INVITATION_CANCELED ->
                arrayOf(
                    getTeamName(payload["teamId"] as? Long),
                    getUsername(payload["cancelerId"] as? Long, "Admin"), // Canceler is admin
                )
            // Args: {0}=cancelerUsername (User), {1}=teamName
            NotificationType.TEAM_REQUEST_CANCELED ->
                arrayOf(
                    getUsername(payload["cancelerId"] as? Long, "User"), // Canceler is user
                    getTeamName(payload["teamId"] as? Long),
                )
        }
    }

    /**
     * Builds the ordered argument array for aggregated notifications. The order MUST match the
     * placeholders {0}, {1} etc. in the properties file.
     */
    private fun buildArgumentsArrayForAggregation(
        type: NotificationType,
        metadata: Map<String, Any>,
        context: String,
    ): Array<Any?> {
        return when (type) {
            NotificationType.REACTION -> {
                // Retrieve aggregated data from metadata
                @Suppress("UNCHECKED_CAST")
                val actors = metadata["reactorUsernames"] as? List<String> ?: emptyList()
                val totalCount = metadata["totalCount"] as? Int ?: actors.size
                val maxActors = notificationProperties.aggregationMaxActorsInTitle
                val actorsToShow = actors.take(maxActors)
                val remainingCount = totalCount - actorsToShow.size

                // Prepare args array matching the .properties keys ({0}=first, {1}=second,
                // {2}=count)
                arrayOf(
                    actorsToShow.getOrNull(0), // {0} - First actor name, or null
                    actorsToShow.getOrNull(1), // {1} - Second actor name, or null
                    remainingCount, // {2} - Remaining count (as Int)
                    // Add other common elements if needed, e.g., target item name?
                )
            }
            // Add cases for other aggregatable types here
            else -> emptyArray() // Default empty array if type is not handled for aggregation
        }
    }

    // --- Helper Methods ---
    /**
     * Fetches username, returns default description if ID is null or user not found. Returns
     * nullable String.
     */
    private fun getUsername(userId: Long?, defaultDesc: String = "Someone"): String? {
        return userId?.let { id ->
            try {
                // TODO: Consider using a more specific method if available (e.g., findUsernameById)
                // to avoid fetching the full DTO if only the name is needed.
                userService.getUserDto(id).username
            } catch (e: Exception) {
                log.warn("Could not fetch username for ID {}: {}", id, e.message)
                null // Return null on error to distinguish from defaultDesc for null ID
            }
        } ?: defaultDesc.takeIf { userId == null } // Return default only if ID itself was null
    }

    /**
     * Fetches team name, returns default name if ID is null or team not found. Returns nullable
     * String.
     */
    private fun getTeamName(teamId: Long?, defaultName: String = "the team"): String? {
        return teamId?.let { id ->
            try {
                teamService.getTeamSummaryDTO(id).name
            } catch (e: Exception) {
                log.warn("Could not fetch team name for ID {}: {}", id, e.message)
                null // Return null on error
            }
        } ?: defaultName.takeIf { teamId == null } // Return default only if ID itself was null
    }
}

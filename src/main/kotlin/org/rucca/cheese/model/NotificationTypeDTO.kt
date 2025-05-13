package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * The type category of the notification. Values:
 * MENTION,REPLY,REACTION,PROJECT_INVITE,DEADLINE_REMIND,TEAM_JOIN_REQUEST,TEAM_INVITATION,TEAM_REQUEST_APPROVED,TEAM_REQUEST_REJECTED,TEAM_INVITATION_ACCEPTED,TEAM_INVITATION_DECLINED,TEAM_INVITATION_CANCELED,TEAM_REQUEST_CANCELED
 */
enum class NotificationTypeDTO(@get:JsonValue val value: kotlin.String) {

    MENTION("MENTION"),
    REPLY("REPLY"),
    REACTION("REACTION"),
    PROJECT_INVITE("PROJECT_INVITE"),
    DEADLINE_REMIND("DEADLINE_REMIND"),
    TEAM_JOIN_REQUEST("TEAM_JOIN_REQUEST"),
    TEAM_INVITATION("TEAM_INVITATION"),
    TEAM_REQUEST_APPROVED("TEAM_REQUEST_APPROVED"),
    TEAM_REQUEST_REJECTED("TEAM_REQUEST_REJECTED"),
    TEAM_INVITATION_ACCEPTED("TEAM_INVITATION_ACCEPTED"),
    TEAM_INVITATION_DECLINED("TEAM_INVITATION_DECLINED"),
    TEAM_INVITATION_CANCELED("TEAM_INVITATION_CANCELED"),
    TEAM_REQUEST_CANCELED("TEAM_REQUEST_CANCELED");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): NotificationTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}

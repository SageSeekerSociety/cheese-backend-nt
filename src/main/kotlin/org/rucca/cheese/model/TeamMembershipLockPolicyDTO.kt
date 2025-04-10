package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: NO_LOCK,LOCK_ON_APPROVAL */
enum class TeamMembershipLockPolicyDTO(@get:JsonValue val value: kotlin.String) {

    NO_LOCK("NO_LOCK"),
    LOCK_ON_APPROVAL("LOCK_ON_APPROVAL");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): TeamMembershipLockPolicyDTO {
            return values().first { it -> it.value == value }
        }
    }
}

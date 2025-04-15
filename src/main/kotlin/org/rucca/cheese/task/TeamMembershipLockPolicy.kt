package org.rucca.cheese.task

import org.rucca.cheese.model.TeamMembershipLockPolicyDTO

enum class TeamMembershipLockPolicy {
    NO_LOCK, // Team members can be changed freely (with warnings/checks if applicable)
    LOCK_ON_APPROVAL, // Team members are locked once the TaskMembership is approved
}

fun TeamMembershipLockPolicy.toDTO(): TeamMembershipLockPolicyDTO {
    return TeamMembershipLockPolicyDTO.forValue(this.name)
}

fun TeamMembershipLockPolicyDTO.toEntity(): TeamMembershipLockPolicy {
    return TeamMembershipLockPolicy.valueOf(this.value)
}

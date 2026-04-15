package org.rucca.cheese.task.event

import org.rucca.cheese.common.persistent.IdType
import org.springframework.context.ApplicationEvent

/** Event published when a TaskMembership's status might need recalculation. */
class TaskMembershipStatusUpdateEvent(source: Any, val membershipId: IdType) :
    ApplicationEvent(source)

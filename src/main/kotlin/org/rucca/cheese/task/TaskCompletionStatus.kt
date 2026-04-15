package org.rucca.cheese.task

import org.rucca.cheese.model.TaskCompletionStatusDTO

// Define the possible completion statuses for a task membership
enum class TaskCompletionStatus {
    /** The participant is enrolled but has not submitted anything yet. */
    NOT_SUBMITTED,

    /** The participant has submitted, and the latest submission is awaiting review. */
    PENDING_REVIEW,

    /**
     * The latest submission was rejected, but the task allows resubmission and the deadline has not
     * passed (if applicable).
     */
    REJECTED_RESUBMITTABLE,

    /** The participant has at least one submission that has been accepted. */
    SUCCESS,

    /**
     * The participant has failed the task. This occurs if:
     * - The latest submission was rejected and resubmission is not allowed or the deadline has
     *   passed.
     * - The deadline has passed and no submission was accepted (including cases where the latest
     *   submission was never reviewed).
     */
    FAILED,
}

fun TaskCompletionStatus.toDTO(): TaskCompletionStatusDTO {
    return TaskCompletionStatusDTO.forValue(this.name)
}

fun TaskCompletionStatus.toEntity(): TaskCompletionStatus {
    return TaskCompletionStatus.valueOf(this.name)
}

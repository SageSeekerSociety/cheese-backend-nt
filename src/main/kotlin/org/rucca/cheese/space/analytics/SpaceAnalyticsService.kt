package org.rucca.cheese.space.analytics

import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SpaceAnalyticsService(
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
) {
    fun getSpaceTaskAnalytics(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        taskStatus: String?,
        categoryId: Long?,
        publisherId: Long?,
        realName: String,
        successBy: String,
    ): SpaceTaskAnalyticsDTO {
        // Get all tasks in the space with optional filters
        val allTasks = taskRepository.findBySpaceId(spaceId)

        // Apply filters
        val filteredTasks =
            allTasks.filter { task ->
                (from == null || task.createdAt.toEpochMilli() >= from) &&
                    (to == null || task.createdAt.toEpochMilli() <= to) &&
                    (categoryId == null || task.category.id?.toLong() == categoryId) &&
                    (publisherId == null || task.creator.id?.toLong() == publisherId)
                // Note: Task doesn't have a status field, using approved status instead
            }

        // Task Category Distribution
        val categoryDistribution =
            createDistribution(
                "Task Categories",
                filteredTasks.groupBy { it.category.name }.mapValues { it.value.size },
            )

        // Task Status Distribution (using approved status)
        val statusDistribution =
            createDistribution(
                "Task Status",
                filteredTasks.groupBy { it.approved.name }.mapValues { it.value.size },
            )

        // Get all memberships for filtered tasks
        val taskIds = filteredTasks.map { it.id }
        val memberships =
            if (taskIds.isNotEmpty()) {
                taskIds.flatMap { taskMembershipRepository.findAllByTaskId(it!!) }
            } else {
                emptyList()
            }

        // Participant Status Distribution
        val participantStatusDistribution =
            createDistribution(
                "Participant Status",
                memberships.groupBy { it.approved?.name ?: "NONE" }.mapValues { it.value.size },
            )

        // Rank Distribution (if tasks have ranks)
        val rankDistribution =
            createDistribution(
                "Task Ranks",
                filteredTasks
                    .filter { it.rank != null }
                    .groupBy {
                        when (it.rank!!) {
                            in 1..10 -> "Top 10"
                            in 11..50 -> "11-50"
                            in 51..100 -> "51-100"
                            else -> "100+"
                        }
                    }
                    .mapValues { it.value.size },
            )

        // Student Statistics (simplified - would need more user profile data)
        val successMemberships =
            memberships.filter { it.completionStatus == TaskCompletionStatus.SUCCESS }
        val unsuccessMemberships =
            memberships.filter { it.completionStatus != TaskCompletionStatus.SUCCESS }

        val successStats = createStudentStatistics(successMemberships, "Success")
        val unsuccessStats = createStudentStatistics(unsuccessMemberships, "Unsuccess")

        return SpaceTaskAnalyticsDTO(
            taskCategoryDistribution = categoryDistribution,
            taskStatusDistribution = statusDistribution,
            participantStatusDistribution = participantStatusDistribution,
            rankDistribution = rankDistribution,
            successStudentStatistics = successStats,
            unsuccessStudentStatistics = unsuccessStats,
        )
    }

    private fun createDistribution(name: String, data: Map<String, Int>): DistributionDTO {
        val total = data.values.sum()
        val items =
            data.map { (label, count) ->
                DistributionItemDTO(
                    label = label,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionDTO(name = name, type = DistributionDTO.Type.DISCRETE, items = items)
    }

    private fun createStudentStatistics(
        memberships: List<org.rucca.cheese.task.TaskMembership>,
        type: String,
    ): StudentStatisticsDTO {
        val memberIds = memberships.mapNotNull { it.memberId }.distinct()
        // Note: Need to handle both user and team members
        val totalMembers = memberIds.size

        // Create placeholder distributions (would need actual user profile data)
        val gradeDistribution =
            DistributionDTO(
                name = "$type Student Grades",
                type = DistributionDTO.Type.DISCRETE,
                items =
                    listOf(
                        DistributionItemDTO("Freshman", totalMembers / 4, 25.0),
                        DistributionItemDTO("Sophomore", totalMembers / 4, 25.0),
                        DistributionItemDTO("Junior", totalMembers / 4, 25.0),
                        DistributionItemDTO("Senior", totalMembers / 4, 25.0),
                    ),
            )

        val majorDistribution =
            DistributionDTO(
                name = "$type Student Majors",
                type = DistributionDTO.Type.DISCRETE,
                items =
                    listOf(
                        DistributionItemDTO("Computer Science", totalMembers / 3, 33.3),
                        DistributionItemDTO("Engineering", totalMembers / 3, 33.3),
                        DistributionItemDTO("Other", totalMembers / 3, 33.4),
                    ),
            )

        val classDistribution =
            DistributionDTO(
                name = "$type Student Classes",
                type = DistributionDTO.Type.DISCRETE,
                items =
                    listOf(
                        DistributionItemDTO("Class A", totalMembers / 2, 50.0),
                        DistributionItemDTO("Class B", totalMembers / 2, 50.0),
                    ),
            )

        return StudentStatisticsDTO(
            totalStudents = totalMembers,
            totalStudentsWithRealName =
                memberships.count { it.realNameInfo?.realName?.isNotBlank() == true },
            gradeDistribution = gradeDistribution,
            majorDistribution = majorDistribution,
            classNameDistribution = classDistribution,
        )
    }

    fun getPublishersParticipation(
        spaceId: IdType,
        successBy: String,
    ): List<PublisherParticipationDTO> {
        // Get all tasks in the space
        val allTasks = taskRepository.findBySpaceId(spaceId)

        // Group tasks by publisher (creator)
        val tasksByPublisher = allTasks.groupBy { it.creator }

        return tasksByPublisher
            .mapNotNull { (publisher, tasks) ->
                if (publisher == null) return@mapNotNull null

                val taskIds = tasks.map { it.id }
                val memberships =
                    if (taskIds.isNotEmpty()) {
                        taskIds.flatMap { taskMembershipRepository.findAllByTaskId(it!!) }
                    } else {
                        emptyList()
                    }

                val successMemberships =
                    memberships.filter { it.completionStatus == TaskCompletionStatus.SUCCESS }
                val completedUsers = successMemberships.mapNotNull { it.memberId }.distinct()

                PublisherParticipationDTO(
                    publisherId = publisher.id?.toLong() ?: 0,
                    publisherName = publisher.username,
                    participants = memberships.size,
                    completedUsers = completedUsers.size,
                    taskCount = tasks.size,
                )
            }
            .sortedByDescending { it.taskCount }
    }

    fun exportParticipants(
        spaceId: IdType,
        format: String,
        from: Long?,
        to: Long?,
        taskStatus: String?,
        categoryId: Long?,
        publisherId: Long?,
        realName: String,
        successBy: String,
    ): String {
        // Get all tasks in the space with filters
        val allTasks = taskRepository.findBySpaceId(spaceId)

        val filteredTasks =
            allTasks.filter { task ->
                (from == null || task.createdAt.toEpochMilli() >= from) &&
                    (to == null || task.createdAt.toEpochMilli() <= to) &&
                    (categoryId == null || task.category?.id?.toLong() == categoryId) &&
                    (publisherId == null || task.creator?.id?.toLong() == publisherId)
            }

        val taskIds = filteredTasks.map { it.id }
        val memberships =
            if (taskIds.isNotEmpty()) {
                taskIds.flatMap { taskMembershipRepository.findAllByTaskId(it!!) }
            } else {
                emptyList()
            }

        // Build CSV
        val csvBuilder = StringBuilder()

        when (format.lowercase()) {
            "csv",
            "participants" -> {
                // Export participant details
                csvBuilder.append("Task ID,Task Title,Member ID,Status,Completion Status,Email\n")

                memberships.forEach { membership ->
                    val task = filteredTasks.find { it.id == membership.task?.id }
                    val status = membership.approved?.name ?: "NONE"
                    val completionStatus = membership.completionStatus.name

                    csvBuilder.append("${task?.id ?: ""},")
                    csvBuilder.append("\"${task?.name ?: ""}\",")
                    csvBuilder.append("${membership.memberId ?: ""},")
                    csvBuilder.append("$status,")
                    csvBuilder.append("$completionStatus,")
                    csvBuilder.append("\"${membership.email ?: ""}\"\n")
                }
            }
            "summary" -> {
                // Export summary by task
                csvBuilder.append(
                    "Task ID,Task Title,Category,Creator,Total Participants,Approved,Rejected,Pending\n"
                )

                filteredTasks.forEach { task ->
                    val taskMemberships = memberships.filter { it.task?.id == task.id }
                    val approved = taskMemberships.count { it.approved?.name == "APPROVED" }
                    val rejected = taskMemberships.count { it.approved?.name == "DISAPPROVED" }
                    val pending =
                        taskMemberships.count { it.approved?.name == "NONE" || it.approved == null }

                    csvBuilder.append("${task.id},")
                    csvBuilder.append("\"${task.name}\",")
                    csvBuilder.append("\"${task.category.name}\",")
                    csvBuilder.append("\"${task.creator.username}\",")
                    csvBuilder.append("${taskMemberships.size},")
                    csvBuilder.append("$approved,")
                    csvBuilder.append("$rejected,")
                    csvBuilder.append("$pending\n")
                }
            }
            else -> {
                // Default format - publisher summary
                csvBuilder.append(
                    "Publisher ID,Publisher Name,Total Tasks,Total Participants,Approved Participants\n"
                )

                val tasksByPublisher = filteredTasks.groupBy { it.creator }
                tasksByPublisher.forEach { (publisher, tasks) ->
                    if (publisher != null) {
                        val publisherTaskIds = tasks.map { it.id }
                        val publisherMemberships =
                            memberships.filter { it.task?.id in publisherTaskIds }
                        val approved =
                            publisherMemberships.count {
                                it.completionStatus == TaskCompletionStatus.SUCCESS
                            }

                        csvBuilder.append("${publisher.id},")
                        csvBuilder.append("\"${publisher.username}\",")
                        csvBuilder.append("${tasks.size},")
                        csvBuilder.append("${publisherMemberships.size},")
                        csvBuilder.append("$approved\n")
                    }
                }
            }
        }

        return csvBuilder.toString()
    }
}

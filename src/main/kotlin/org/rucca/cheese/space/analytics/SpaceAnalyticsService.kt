package org.rucca.cheese.space.analytics

import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembership
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SpaceAnalyticsService(
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
    private val userRepository: UserRepository,
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
                    (publisherId == null || task.creator.id?.toLong() == publisherId) &&
                    (taskStatus.isNullOrEmpty() || task.approved.name == taskStatus)
            }

        // Task Category Distribution
        val categoryDistribution =
            createDistribution(
                "Task Categories",
                filteredTasks.groupBy { it.category.name }.mapValues { it.value.size },
            )

        // Task Status Distribution (using approved status)
        val taskStatusCounts = mutableMapOf("NONE" to 0, "APPROVED" to 0, "DISAPPROVED" to 0)
        filteredTasks
            .groupBy { it.approved.name }
            .forEach { (status, tasks) -> taskStatusCounts[status] = tasks.size }
        val statusDistribution = createDistribution("Task Status", taskStatusCounts)

        // Get all memberships for filtered tasks
        val taskIds = filteredTasks.mapNotNull { it.id }
        val allMemberships =
            if (taskIds.isNotEmpty()) {
                taskIds.flatMap { taskId -> taskMembershipRepository.findAllByTaskId(taskId) }
            } else {
                emptyList()
            }

        // Apply realName filter
        val memberships =
            when (realName) {
                "with" -> allMemberships.filter { it.realNameInfo?.realName?.isNotBlank() == true }
                "without" -> allMemberships.filter { it.realNameInfo?.realName?.isBlank() != false }
                else -> allMemberships // "all" or empty
            }

        // Participant Status Distribution
        val participantStatusCounts = mutableMapOf("NONE" to 0, "APPROVED" to 0, "DISAPPROVED" to 0)
        memberships
            .groupBy { it.approved?.name ?: "NONE" }
            .forEach { (status, members) -> participantStatusCounts[status] = members.size }
        val participantStatusDistribution =
            createDistribution("Participant Status", participantStatusCounts)

        // Rank Distribution (if tasks have ranks)
        val rankDistribution =
            createDistribution(
                "Task Ranks",
                filteredTasks
                    .mapNotNull { task -> task.rank?.let { rank -> rank to task } }
                    .groupBy { (rank, _) ->
                        when (rank) {
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

        // Count unique members with real name (not total memberships)
        val membersWithRealName =
            memberships
                .filter { it.realNameInfo?.realName?.isNotBlank() == true }
                .mapNotNull { it.memberId }
                .distinct()
                .size

        // Calculate actual distributions from real data
        // If no real data exists, return empty distributions instead of fake data
        val gradeDistribution =
            DistributionDTO(
                name = "$type Participant Grades",
                type = DistributionDTO.Type.DISCRETE,
                items =
                    if (totalMembers > 0) {
                        val gradesWithData = memberships.mapNotNull { it.realNameInfo?.grade }
                        val gradeCounts = gradesWithData.groupingBy { it }.eachCount()
                        val totalWithGrade = gradesWithData.size

                        if (totalWithGrade > 0) {
                            gradeCounts.map { (grade, count) ->
                                DistributionItemDTO(
                                    label = grade,
                                    count = count,
                                    percentage = (count * 100.0 / totalWithGrade),
                                )
                            }
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    },
            )

        val majorDistribution =
            DistributionDTO(
                name = "$type Participant Majors",
                type = DistributionDTO.Type.DISCRETE,
                items =
                    if (totalMembers > 0) {
                        val majorsWithData = memberships.mapNotNull { it.realNameInfo?.major }
                        val majorCounts = majorsWithData.groupingBy { it }.eachCount()
                        val totalWithMajor = majorsWithData.size

                        if (totalWithMajor > 0) {
                            majorCounts.map { (major, count) ->
                                DistributionItemDTO(
                                    label = major,
                                    count = count,
                                    percentage = (count * 100.0 / totalWithMajor),
                                )
                            }
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    },
            )

        val classDistribution =
            DistributionDTO(
                name = "$type Participant Classes",
                type = DistributionDTO.Type.DISCRETE,
                items =
                    if (totalMembers > 0) {
                        val classesWithData = memberships.mapNotNull { it.realNameInfo?.className }
                        val classCounts = classesWithData.groupingBy { it }.eachCount()
                        val totalWithClass = classesWithData.size

                        if (totalWithClass > 0) {
                            classCounts.map { (className, count) ->
                                DistributionItemDTO(
                                    label = className,
                                    count = count,
                                    percentage = (count * 100.0 / totalWithClass),
                                )
                            }
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    },
            )

        return StudentStatisticsDTO(
            totalStudents = totalMembers,
            totalStudentsWithRealName = membersWithRealName,
            gradeDistribution = gradeDistribution,
            majorDistribution = majorDistribution,
            classNameDistribution = classDistribution,
        )
    }

    fun getPublishersParticipation(
        spaceId: IdType,
        successBy: String,
    ): List<PublisherParticipationDTO> {
        // Get all tasks in the space grouped by publisher
        val allTasks = taskRepository.findBySpaceId(spaceId)

        return allTasks
            .groupBy { task -> task.creator?.id?.toLong() }
            .map { (publisherId, publisherTasks) ->
                val memberships =
                    publisherTasks.flatMap { task ->
                        val taskId = task.id ?: return@flatMap emptyList<TaskMembership>()
                        taskMembershipRepository.findAllByTaskId(taskId)
                    }

                val participantCount = memberships.size
                val completedCount =
                    memberships.count { it.completionStatus == TaskCompletionStatus.SUCCESS }

                PublisherParticipationDTO(
                    publisherId = publisherId ?: 0L,
                    publisherName =
                        publisherTasks.firstNotNullOfOrNull { it.creator?.username } ?: "",
                    participants = participantCount,
                    completedUsers = completedCount,
                    taskCount = publisherTasks.size,
                )
            }
            .sortedByDescending { it.completedUsers ?: 0 }
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

        val taskIds = filteredTasks.mapNotNull { it.id }
        val memberships =
            if (taskIds.isNotEmpty()) {
                taskIds.flatMap { taskId -> taskMembershipRepository.findAllByTaskId(taskId) }
            } else {
                emptyList()
            }

        // Build CSV
        val csvBuilder = StringBuilder()

        when (format.lowercase()) {
            "csv",
            "participants" -> {
                // Export participant details with more comprehensive data
                csvBuilder.append(
                    "Task ID,Task Title,Category,Task Rank,Task Creator,Created At,Deadline,Member ID,Username,Real Name,Student ID,Grade,Major,Class,Phone,Email,Apply Reason,Reject Reason,Approval Status,Completion Status,Is Team,Join Date\n"
                )

                memberships.forEach { membership ->
                    val task = filteredTasks.find { it.id == membership.task?.id }
                    val status = membership.approved?.name ?: "NONE"
                    val completionStatus = membership.completionStatus.name
                    // Get user info from repository
                    val member =
                        membership.memberId?.let {
                            userRepository.findById(it.toInt()).orElse(null)
                        }

                    csvBuilder.append("${task?.id ?: ""},")
                    csvBuilder.append("\"${task?.name ?: ""}\",")
                    csvBuilder.append("\"${task?.category?.name ?: ""}\",")
                    csvBuilder.append("${task?.rank ?: ""},")
                    csvBuilder.append("\"${task?.creator?.username ?: ""}\",")
                    csvBuilder.append("${task?.createdAt ?: ""},")
                    csvBuilder.append("${task?.deadline ?: ""},")
                    csvBuilder.append("${membership.memberId ?: ""},")
                    csvBuilder.append("\"${member?.username ?: ""}\",")
                    csvBuilder.append("\"${membership.realNameInfo?.realName ?: ""}\",")
                    csvBuilder.append("\"${membership.realNameInfo?.studentId ?: ""}\",")
                    csvBuilder.append("\"${membership.realNameInfo?.grade ?: ""}\",")
                    csvBuilder.append("\"${membership.realNameInfo?.major ?: ""}\",")
                    csvBuilder.append("\"${membership.realNameInfo?.className ?: ""}\",")
                    csvBuilder.append("\"${membership.phone ?: ""}\",")
                    csvBuilder.append("\"${membership.email ?: ""}\",")
                    csvBuilder.append("\"${membership.applyReason ?: ""}\",")
                    csvBuilder.append("\"${membership.rejectReason ?: ""}\",")
                    csvBuilder.append("$status,")
                    csvBuilder.append("$completionStatus,")
                    csvBuilder.append("${membership.isTeam},")
                    csvBuilder.append("${membership.createdAt}\n")
                }
            }
            "summary" -> {
                // Export summary by task
                csvBuilder.append(
                    "Task ID,Task Title,Category,Rank,Creator,Created At,Deadline,Total Participants,Approved,Rejected,Pending,Completed,Task Status\n"
                )

                filteredTasks.forEach { task ->
                    val taskMemberships = memberships.filter { it.task?.id == task.id }
                    val approved = taskMemberships.count { it.approved?.name == "APPROVED" }
                    val rejected = taskMemberships.count { it.approved?.name == "DISAPPROVED" }
                    val pending =
                        taskMemberships.count { it.approved?.name == "NONE" || it.approved == null }
                    val completed =
                        taskMemberships.count {
                            it.completionStatus == TaskCompletionStatus.SUCCESS
                        }

                    csvBuilder.append("${task.id},")
                    csvBuilder.append("\"${task.name}\",")
                    csvBuilder.append("\"${task.category.name}\",")
                    csvBuilder.append("${task.rank ?: ""},")
                    csvBuilder.append("\"${task.creator.username}\",")
                    csvBuilder.append("${task.createdAt},")
                    csvBuilder.append("${task.deadline ?: ""},")
                    csvBuilder.append("${taskMemberships.size},")
                    csvBuilder.append("$approved,")
                    csvBuilder.append("$rejected,")
                    csvBuilder.append("$pending,")
                    csvBuilder.append("$completed,")
                    csvBuilder.append("${task.approved.name}\n")
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

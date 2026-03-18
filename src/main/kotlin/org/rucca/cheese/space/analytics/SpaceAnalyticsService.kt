package org.rucca.cheese.space.analytics

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.task.Task
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembership
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.task.TaskSubmissionRepository
import org.rucca.cheese.task.TaskSubmissionReviewRepository
import org.rucca.cheese.task.TeamMemberRealNameInfo
import org.rucca.cheese.task.service.TaskMembershipSnapshotService
import org.rucca.cheese.user.UserRepository
import org.rucca.cheese.user.models.AccessModuleType
import org.rucca.cheese.user.models.AccessType
import org.rucca.cheese.user.services.UserRealNameService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SpaceAnalyticsService(
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val taskSubmissionReviewRepository: TaskSubmissionReviewRepository,
    private val userRepository: UserRepository,
    private val taskMembershipSnapshotService: TaskMembershipSnapshotService,
    private val userRealNameService: UserRealNameService,
    private val queryService: SpaceAnalyticsQueryService =
        SpaceAnalyticsQueryService(
            taskRepository = taskRepository,
            taskMembershipRepository = taskMembershipRepository,
            taskSubmissionRepository = taskSubmissionRepository,
            taskSubmissionReviewRepository = taskSubmissionReviewRepository,
            taskMembershipSnapshotService = taskMembershipSnapshotService,
        ),
) {
    private data class TaskAnalyticsAggregate(
        val task: Task,
        val memberships: List<TaskMembership>,
        val submittedParticipantCount: Int,
        val pendingParticipantApprovalCount: Int,
        val approvedParticipantCount: Int,
        val rejectedParticipantCount: Int,
        val pendingReviewCount: Int,
        val resubmittableCount: Int,
        val successfulParticipantCount: Int,
        val failedParticipantCount: Int,
    ) {
        val participantCount: Int = memberships.size
        val submissionConversionRate: Double =
            if (approvedParticipantCount == 0) 0.0
            else submittedParticipantCount.toDouble() / approvedParticipantCount.toDouble()
        val successRate: Double =
            if (participantCount == 0) 0.0
            else successfulParticipantCount.toDouble() / participantCount.toDouble()
    }

    fun getSpaceAnalyticsOverview(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        groupBy: String,
    ): SpaceAnalyticsOverviewDTO =
        queryService.getOverview(
            spaceId = spaceId,
            from = from,
            to = to,
            categoryId = categoryId,
            publisherId = publisherId,
            taskApproved = taskApproved,
            groupBy = groupBy,
        )

    fun getSpaceTaskAnalytics(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        hasPendingReview: Boolean?,
        hasPendingApproval: Boolean?,
        sortBy: String,
        sortOrder: String,
    ): SpaceTaskAnalyticsDTO =
        queryService.getTasks(
            spaceId = spaceId,
            from = from,
            to = to,
            categoryId = categoryId,
            publisherId = publisherId,
            taskApproved = taskApproved,
            hasPendingReview = hasPendingReview,
            hasPendingApproval = hasPendingApproval,
            sortBy = sortBy,
            sortOrder = sortOrder,
        )

    fun getSpaceAnalyticsPublishers(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        taskApproved: String?,
        sortBy: String,
        sortOrder: String,
    ): SpaceAnalyticsPublishersDTO =
        queryService.getPublishers(
            spaceId = spaceId,
            from = from,
            to = to,
            categoryId = categoryId,
            taskApproved = taskApproved,
            sortBy = sortBy,
            sortOrder = sortOrder,
        )

    fun getSpaceAnalyticsParticipants(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        participationApproved: String?,
        completionStatus: String?,
        realName: String,
        groupBy: String,
    ): SpaceAnalyticsParticipantsDTO =
        queryService.getParticipants(
            spaceId = spaceId,
            from = from,
            to = to,
            categoryId = categoryId,
            publisherId = publisherId,
            taskApproved = taskApproved,
            participationApproved = participationApproved,
            completionStatus = completionStatus,
            realName = realName,
            groupBy = groupBy,
        )

    fun getSpaceAnalyticsAlerts(spaceId: IdType): SpaceAnalyticsAlertsDTO =
        queryService.getAlerts(spaceId)

    @Transactional
    fun exportSpaceAnalyticsParticipants(
        accessorId: IdType,
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        participationApproved: String?,
        completionStatus: String?,
        realName: String,
    ): String =
        queryService
            .exportParticipantsCsv(
                spaceId = spaceId,
                from = from,
                to = to,
                categoryId = categoryId,
                publisherId = publisherId,
                taskApproved = taskApproved,
                participationApproved = participationApproved,
                completionStatus = completionStatus,
                realName = realName,
            )
            .also { exportPayload ->
                auditSpaceParticipantExport(
                    accessorId = accessorId,
                    spaceId = spaceId,
                    memberships = exportPayload.memberships,
                    from = from,
                    to = to,
                    categoryId = categoryId,
                    publisherId = publisherId,
                    taskApproved = taskApproved,
                    participationApproved = participationApproved,
                    completionStatus = completionStatus,
                    realName = realName,
                )
            }
            .csv

    fun exportSpaceAnalyticsTasks(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        hasPendingReview: Boolean?,
        hasPendingApproval: Boolean?,
    ): String =
        queryService.exportTasksCsv(
            spaceId = spaceId,
            from = from,
            to = to,
            categoryId = categoryId,
            publisherId = publisherId,
            taskApproved = taskApproved,
            hasPendingReview = hasPendingReview,
            hasPendingApproval = hasPendingApproval,
        )

    fun exportSpaceAnalyticsPublishers(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        taskApproved: String?,
    ): String =
        queryService.exportPublishersCsv(
            spaceId = spaceId,
            from = from,
            to = to,
            categoryId = categoryId,
            taskApproved = taskApproved,
        )

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

    private fun filterTasks(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
    ): List<Task> =
        taskRepository.findBySpaceId(spaceId).filter { task ->
            (from == null || task.createdAt.toEpochMilli() >= from) &&
                (to == null || task.createdAt.toEpochMilli() <= to) &&
                (categoryId == null || task.category.id?.toLong() == categoryId) &&
                (publisherId == null || task.creator.id?.toLong() == publisherId) &&
                (taskApproved.isNullOrBlank() || task.approved.name == taskApproved)
        }

    private fun studentCountOf(membership: TaskMembership): Int =
        if (membership.isTeam) membership.teamMembersRealNameInfo.size else 1

    private fun safeRate(numerator: Int, denominator: Int): Double =
        if (denominator == 0) 0.0 else numerator.toDouble() / denominator.toDouble()

    private fun buildTimeSeries(
        timestamps: List<LocalDateTime>,
        groupBy: String,
    ): List<TimeSeriesPointDTO> =
        timestamps
            .groupingBy { bucketStart(it, groupBy) }
            .eachCount()
            .toSortedMap()
            .map { (bucket, count) -> TimeSeriesPointDTO(bucket = bucket, count = count) }

    private fun bucketStart(timestamp: LocalDateTime, groupBy: String): Long {
        val zoneId = ZoneId.systemDefault()
        val date =
            when (groupBy.lowercase()) {
                "week" -> timestamp.toLocalDate().with(DayOfWeek.MONDAY)
                "month" -> timestamp.toLocalDate().withDayOfMonth(1)
                else -> timestamp.toLocalDate()
            }

        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
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
                    val realNameInfo = firstStudentRealNameInfoOf(membership)
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
                    csvBuilder.append("\"${realNameInfo?.realName ?: ""}\",")
                    csvBuilder.append("\"${realNameInfo?.studentId ?: ""}\",")
                    csvBuilder.append("\"${realNameInfo?.grade ?: ""}\",")
                    csvBuilder.append("\"${realNameInfo?.major ?: ""}\",")
                    csvBuilder.append("\"${realNameInfo?.className ?: ""}\",")
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

    private fun firstStudentRealNameInfoOf(
        membership: TaskMembership
    ): TaskParticipantRealNameInfoDTO? =
        if (membership.isTeam) {
            membership.teamMembersRealNameInfo.firstOrNull()?.let { teamMember ->
                taskMembershipSnapshotService.getRealNameInfoForTeamMemberSnapshot(
                    membership,
                    teamMember,
                )
            }
        } else {
            taskMembershipSnapshotService.getRealNameInfoFromMembership(membership)
        }

    private fun auditSpaceParticipantExport(
        accessorId: IdType,
        spaceId: IdType,
        memberships: List<TaskMembership>,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        participationApproved: String?,
        completionStatus: String?,
        realName: String,
    ) {
        val accessReason =
            buildParticipantExportAccessReason(
                from = from,
                to = to,
                categoryId = categoryId,
                publisherId = publisherId,
                taskApproved = taskApproved,
                participationApproved = participationApproved,
                completionStatus = completionStatus,
                realName = realName,
            )
        memberships.flatMap(::exportedUserIdsOf).distinct().forEach { targetUserId ->
            userRealNameService.logAccess(
                accessorId = accessorId,
                targetId = targetUserId,
                accessReason = accessReason,
                accessType = AccessType.EXPORT,
                moduleType = AccessModuleType.SPACE,
                moduleEntityId = spaceId,
            )
        }
    }

    private fun exportedUserIdsOf(membership: TaskMembership): List<IdType> =
        if (membership.isTeam) {
            membership.teamMembersRealNameInfo.map(TeamMemberRealNameInfo::memberId)
        } else {
            listOfNotNull(membership.memberId)
        }

    private fun buildParticipantExportAccessReason(
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        participationApproved: String?,
        completionStatus: String?,
        realName: String,
    ): String =
        "Export space analytics participants with filters: " +
            listOf(
                    "from=$from",
                    "to=$to",
                    "categoryId=$categoryId",
                    "publisherId=$publisherId",
                    "taskApproved=${taskApproved ?: ""}",
                    "participationApproved=${participationApproved ?: ""}",
                    "completionStatus=${completionStatus ?: ""}",
                    "realName=$realName",
                )
                .joinToString(",")
}

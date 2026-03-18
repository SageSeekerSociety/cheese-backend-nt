package org.rucca.cheese.space.analytics

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.model.DistributionDTO
import org.rucca.cheese.model.DistributionItemDTO
import org.rucca.cheese.model.SpaceAnalyticsAlertsDTO
import org.rucca.cheese.model.SpaceAnalyticsCategorySummaryDTO
import org.rucca.cheese.model.SpaceAnalyticsEntityMetricsDTO
import org.rucca.cheese.model.SpaceAnalyticsOverviewDTO
import org.rucca.cheese.model.SpaceAnalyticsOverviewSummaryDTO
import org.rucca.cheese.model.SpaceAnalyticsParticipantDistributionsDTO
import org.rucca.cheese.model.SpaceAnalyticsParticipantEntityMetricsDTO
import org.rucca.cheese.model.SpaceAnalyticsParticipantStudentMetricsDTO
import org.rucca.cheese.model.SpaceAnalyticsParticipantTrendsDTO
import org.rucca.cheese.model.SpaceAnalyticsParticipantsDTO
import org.rucca.cheese.model.SpaceAnalyticsPublisherMetricsDTO
import org.rucca.cheese.model.SpaceAnalyticsPublisherSummaryDTO
import org.rucca.cheese.model.SpaceAnalyticsPublishersDTO
import org.rucca.cheese.model.SpaceAnalyticsStudentMetricsDTO
import org.rucca.cheese.model.SpaceAnalyticsTaskDTO
import org.rucca.cheese.model.SpaceAnalyticsTaskDistributionsDTO
import org.rucca.cheese.model.SpaceAnalyticsTrendsDTO
import org.rucca.cheese.model.SpaceTaskAnalyticsDTO
import org.rucca.cheese.model.TimeSeriesPointDTO
import org.rucca.cheese.task.Task
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembership
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.task.TaskSubmission
import org.rucca.cheese.task.TaskSubmissionRepository
import org.rucca.cheese.task.TaskSubmissionReview
import org.rucca.cheese.task.TaskSubmissionReviewRepository
import org.rucca.cheese.task.TeamMemberRealNameInfo
import org.rucca.cheese.task.service.TaskMembershipSnapshotService

class SpaceAnalyticsQueryService(
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val taskSubmissionReviewRepository: TaskSubmissionReviewRepository,
    private val taskMembershipSnapshotService: TaskMembershipSnapshotService,
) {
    private companion object {
        const val STALLED_TASK_THRESHOLD_DAYS = 14L
        const val OVERDUE_UNREVIEWED_SUBMISSION_THRESHOLD_DAYS = 7L
        const val INACTIVE_PUBLISHER_THRESHOLD_DAYS = 30L
    }

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

    private data class PublisherAnalyticsAggregate(
        val publisherId: Long,
        val publisherName: String,
        val taskCount: Int,
        val participantCount: Int,
        val approvedParticipantCount: Int,
        val submittedParticipantCount: Int,
        val successfulParticipantCount: Int,
        val lastTaskCreatedAt: Long,
    ) {
        val avgParticipantsPerTask: Double =
            if (taskCount == 0) 0.0 else participantCount.toDouble() / taskCount.toDouble()
        val submissionConversionRate: Double =
            if (approvedParticipantCount == 0) 0.0
            else submittedParticipantCount.toDouble() / approvedParticipantCount.toDouble()
        val successRate: Double =
            if (participantCount == 0) 0.0
            else successfulParticipantCount.toDouble() / participantCount.toDouble()
    }

    private data class ParticipantAnalyticsScope(
        val tasks: List<Task>,
        val memberships: List<TaskMembership>,
        val submissionsByMembershipId: Map<IdType, List<TaskSubmission>>,
    )

    fun getOverview(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        groupBy: String,
    ): SpaceAnalyticsOverviewDTO {
        val filteredTasks = filterTasks(spaceId, from, to, categoryId, publisherId, taskApproved)
        val membershipsByTaskId = loadMembershipsByTaskId(filteredTasks)
        val memberships = filteredTasks.flatMap { membershipsByTaskId[it.id].orEmpty() }
        val submissionsByMembershipId = loadSubmissionsByMembershipId(memberships)

        val approvedMemberships = memberships.filter { it.approved == ApproveType.APPROVED }
        val submittedMemberships =
            memberships.filter { submissionsByMembershipId[it.id].orEmpty().isNotEmpty() }
        val successfulMemberships =
            memberships.filter { it.completionStatus == TaskCompletionStatus.SUCCESS }

        return SpaceAnalyticsOverviewDTO(
            summary = SpaceAnalyticsOverviewSummaryDTO(spaceId = spaceId, from = from, to = to),
            entityMetrics =
                SpaceAnalyticsEntityMetricsDTO(
                    taskCount = filteredTasks.size,
                    publisherCount =
                        filteredTasks.mapNotNull { it.creator.id?.toLong() }.distinct().size,
                    participantCount = memberships.size,
                    approvedParticipantCount = approvedMemberships.size,
                    submittedParticipantCount = submittedMemberships.size,
                    successfulParticipantCount = successfulMemberships.size,
                    participationConversionRate =
                        safeRate(approvedMemberships.size, memberships.size),
                    submissionConversionRate =
                        safeRate(submittedMemberships.size, approvedMemberships.size),
                    successRate = safeRate(successfulMemberships.size, memberships.size),
                ),
            studentMetrics =
                SpaceAnalyticsStudentMetricsDTO(
                    studentCount = memberships.sumOf(::studentCountOf),
                    approvedStudentCount = approvedMemberships.sumOf(::studentCountOf),
                    successfulStudentCount = successfulMemberships.sumOf(::studentCountOf),
                ),
            taskDistributions =
                SpaceAnalyticsTaskDistributionsDTO(
                    byCategory =
                        createDistribution(
                            "Task Categories",
                            filteredTasks.groupingBy { it.category.name }.eachCount(),
                        ),
                    byApprovalStatus =
                        createDistribution(
                            "Task Approval Status",
                            ApproveType.entries.associate { status ->
                                status.name to
                                    filteredTasks.count { task -> task.approved == status }
                            },
                        ),
                    byCompletionStatus =
                        createDistribution(
                            "Participant Completion Status",
                            TaskCompletionStatus.entries.associate { status ->
                                status.name to
                                    memberships.count { membership ->
                                        membership.completionStatus == status
                                    }
                            },
                        ),
                ),
            trends =
                SpaceAnalyticsTrendsDTO(
                    tasksCreated = buildTimeSeries(filteredTasks.map { it.createdAt }, groupBy),
                    participantsJoined = buildTimeSeries(memberships.map { it.createdAt }, groupBy),
                    submissionsCreated =
                        buildTimeSeries(
                            submissionsByMembershipId.values.flatten().map { it.createdAt },
                            groupBy,
                        ),
                    successesAchieved =
                        buildTimeSeries(successfulMemberships.map { it.updatedAt }, groupBy),
                ),
        )
    }

    fun getTasks(
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
    ): SpaceTaskAnalyticsDTO {
        val filteredTasks = filterTasks(spaceId, from, to, categoryId, publisherId, taskApproved)
        val membershipsByTaskId = loadMembershipsByTaskId(filteredTasks)
        val submissionsByMembershipId =
            loadSubmissionsByMembershipId(membershipsByTaskId.values.flatten())
        val reviewsBySubmissionId =
            loadReviewsBySubmissionId(submissionsByMembershipId.values.flatten())

        val aggregates =
            filteredTasks
                .map { task ->
                    val memberships = membershipsByTaskId[task.id].orEmpty()
                    TaskAnalyticsAggregate(
                        task = task,
                        memberships = memberships,
                        submittedParticipantCount =
                            memberships.count {
                                submissionsByMembershipId[it.id].orEmpty().isNotEmpty()
                            },
                        pendingParticipantApprovalCount =
                            memberships.count { it.approved == ApproveType.NONE },
                        approvedParticipantCount =
                            memberships.count { it.approved == ApproveType.APPROVED },
                        rejectedParticipantCount =
                            memberships.count { it.approved == ApproveType.DISAPPROVED },
                        pendingReviewCount =
                            memberships.count {
                                latestSubmissionOf(it, submissionsByMembershipId)?.let { latest ->
                                    reviewsBySubmissionId[latest.id] == null
                                } == true
                            },
                        resubmittableCount =
                            memberships.count {
                                it.completionStatus == TaskCompletionStatus.REJECTED_RESUBMITTABLE
                            },
                        successfulParticipantCount =
                            memberships.count {
                                it.completionStatus == TaskCompletionStatus.SUCCESS
                            },
                        failedParticipantCount =
                            memberships.count { it.completionStatus == TaskCompletionStatus.FAILED },
                    )
                }
                .filter { aggregate ->
                    (hasPendingReview == null ||
                        (aggregate.pendingReviewCount > 0) == hasPendingReview) &&
                        (hasPendingApproval == null ||
                            (aggregate.pendingParticipantApprovalCount > 0) == hasPendingApproval)
                }

        val comparator =
            when (sortBy) {
                "participantCount" -> compareBy<TaskAnalyticsAggregate> { it.participantCount }
                "successRate" -> compareBy<TaskAnalyticsAggregate> { it.successRate }
                "pendingReviewCount" -> compareBy<TaskAnalyticsAggregate> { it.pendingReviewCount }
                else -> compareBy { it.task.createdAt }
            }

        val sortedAggregates =
            if (sortOrder.equals("asc", ignoreCase = true)) {
                aggregates.sortedWith(comparator)
            } else {
                aggregates.sortedWith(comparator.reversed())
            }

        return SpaceTaskAnalyticsDTO(
            tasks =
                sortedAggregates.map { aggregate ->
                    SpaceAnalyticsTaskDTO(
                        taskId = aggregate.task.id!!.toLong(),
                        taskName = aggregate.task.name,
                        publisher =
                            SpaceAnalyticsPublisherSummaryDTO(
                                id = aggregate.task.creator.id!!.toLong(),
                                name = aggregate.task.creator.username ?: "",
                            ),
                        category =
                            SpaceAnalyticsCategorySummaryDTO(
                                id = aggregate.task.category.id!!.toLong(),
                                name = aggregate.task.category.name,
                            ),
                        approved = aggregate.task.approved.convert(),
                        createdAt = aggregate.task.createdAt.toEpochMilli(),
                        participantCount = aggregate.participantCount,
                        pendingParticipantApprovalCount = aggregate.pendingParticipantApprovalCount,
                        approvedParticipantCount = aggregate.approvedParticipantCount,
                        rejectedParticipantCount = aggregate.rejectedParticipantCount,
                        submittedParticipantCount = aggregate.submittedParticipantCount,
                        pendingReviewCount = aggregate.pendingReviewCount,
                        resubmittableCount = aggregate.resubmittableCount,
                        successfulParticipantCount = aggregate.successfulParticipantCount,
                        failedParticipantCount = aggregate.failedParticipantCount,
                        submissionConversionRate = aggregate.submissionConversionRate,
                        successRate = aggregate.successRate,
                        deadline = aggregate.task.deadline?.toEpochMilli(),
                    )
                }
        )
    }

    fun getPublishers(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        taskApproved: String?,
        sortBy: String,
        sortOrder: String,
    ): SpaceAnalyticsPublishersDTO {
        val filteredTasks = filterTasks(spaceId, from, to, categoryId, null, taskApproved)
        val membershipsByTaskId = loadMembershipsByTaskId(filteredTasks)
        val submissionsByMembershipId =
            loadSubmissionsByMembershipId(membershipsByTaskId.values.flatten())
        val reviewsBySubmissionId =
            loadReviewsBySubmissionId(submissionsByMembershipId.values.flatten())

        val aggregates =
            filteredTasks
                .groupBy { task -> task.creator.id!!.toLong() }
                .map { (publisherId, publisherTasks) ->
                    val memberships =
                        publisherTasks.flatMap { task -> membershipsByTaskId[task.id].orEmpty() }
                    val approvedParticipantCount =
                        memberships.count { it.approved == ApproveType.APPROVED }
                    val submittedParticipantCount =
                        memberships.count {
                            submissionsByMembershipId[it.id].orEmpty().isNotEmpty()
                        }
                    val successfulParticipantCount =
                        memberships.count { it.completionStatus == TaskCompletionStatus.SUCCESS }
                    PublisherAnalyticsAggregate(
                        publisherId = publisherId,
                        publisherName = publisherTasks.first().creator.username ?: "",
                        taskCount = publisherTasks.size,
                        participantCount = memberships.size,
                        approvedParticipantCount = approvedParticipantCount,
                        submittedParticipantCount = submittedParticipantCount,
                        successfulParticipantCount = successfulParticipantCount,
                        lastTaskCreatedAt =
                            publisherTasks.maxOf { task -> task.createdAt.toEpochMilli() },
                    )
                }

        val comparator =
            when (sortBy) {
                "participantCount" -> compareBy<PublisherAnalyticsAggregate> { it.participantCount }
                "successRate" -> compareBy<PublisherAnalyticsAggregate> { it.successRate }
                "lastTaskCreatedAt" ->
                    compareBy<PublisherAnalyticsAggregate> { it.lastTaskCreatedAt }
                else -> compareBy { it.taskCount }
            }

        val sortedAggregates =
            if (sortOrder.equals("asc", ignoreCase = true)) {
                aggregates.sortedWith(comparator)
            } else {
                aggregates.sortedWith(comparator.reversed())
            }

        return SpaceAnalyticsPublishersDTO(
            publishers =
                sortedAggregates.map { aggregate ->
                    SpaceAnalyticsPublisherMetricsDTO(
                        publisherId = aggregate.publisherId,
                        publisherName = aggregate.publisherName,
                        taskCount = aggregate.taskCount,
                        participantCount = aggregate.participantCount,
                        approvedParticipantCount = aggregate.approvedParticipantCount,
                        submittedParticipantCount = aggregate.submittedParticipantCount,
                        successfulParticipantCount = aggregate.successfulParticipantCount,
                        avgParticipantsPerTask = aggregate.avgParticipantsPerTask,
                        submissionConversionRate = aggregate.submissionConversionRate,
                        successRate = aggregate.successRate,
                        lastTaskCreatedAt = aggregate.lastTaskCreatedAt,
                    )
                }
        )
    }

    fun getParticipants(
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
    ): SpaceAnalyticsParticipantsDTO {
        val scope =
            loadParticipantAnalyticsScope(
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
        val approvalDistribution =
            createDistribution(
                "Participant Approval Status",
                ApproveType.entries.associate { status ->
                    status.name to
                        scope.memberships.count { membership -> membership.approved == status }
                },
            )
        val completionDistribution =
            createDistribution(
                "Participant Completion Status",
                TaskCompletionStatus.entries.associate { status ->
                    status.name to
                        scope.memberships.count { membership ->
                            membership.completionStatus == status
                        }
                },
            )
        val studentProfiles = scope.memberships.flatMap(::studentProfilesOf)

        return SpaceAnalyticsParticipantsDTO(
            summary = SpaceAnalyticsOverviewSummaryDTO(spaceId = spaceId, from = from, to = to),
            entityMetrics =
                SpaceAnalyticsParticipantEntityMetricsDTO(
                    participantCount = scope.memberships.size,
                    approvedParticipantCount =
                        scope.memberships.count { it.approved == ApproveType.APPROVED },
                    pendingParticipantCount =
                        scope.memberships.count { it.approved == ApproveType.NONE },
                    disapprovedParticipantCount =
                        scope.memberships.count { it.approved == ApproveType.DISAPPROVED },
                    submittedParticipantCount =
                        scope.memberships.count {
                            scope.submissionsByMembershipId[it.id].orEmpty().isNotEmpty()
                        },
                    successfulParticipantCount =
                        scope.memberships.count {
                            it.completionStatus == TaskCompletionStatus.SUCCESS
                        },
                ),
            studentMetrics =
                SpaceAnalyticsParticipantStudentMetricsDTO(
                    studentCount = studentProfiles.size,
                    studentsWithRealNameCount =
                        studentProfiles.count { studentProfile -> studentProfile.hasRealName },
                ),
            distributions =
                SpaceAnalyticsParticipantDistributionsDTO(
                    byApprovalStatus = approvalDistribution,
                    byCompletionStatus = completionDistribution,
                    byGrade =
                        createDistribution(
                            "Participant Grades",
                            studentProfiles
                                .mapNotNull { it.grade?.takeUnless(String::isBlank) }
                                .groupingBy { it }
                                .eachCount(),
                        ),
                    byMajor =
                        createDistribution(
                            "Participant Majors",
                            studentProfiles
                                .mapNotNull { it.major?.takeUnless(String::isBlank) }
                                .groupingBy { it }
                                .eachCount(),
                        ),
                    byClassName =
                        createDistribution(
                            "Participant Classes",
                            studentProfiles
                                .mapNotNull { it.className?.takeUnless(String::isBlank) }
                                .groupingBy { it }
                                .eachCount(),
                        ),
                    byRealNameStatus =
                        createDistribution(
                            "Participant Real Name Status",
                            mapOf(
                                "WITH_REAL_NAME" to
                                    scope.memberships.count { membership ->
                                        membership.hasRealName()
                                    },
                                "WITHOUT_REAL_NAME" to
                                    scope.memberships.count { membership ->
                                        !membership.hasRealName()
                                    },
                            ),
                        ),
                ),
            trends =
                SpaceAnalyticsParticipantTrendsDTO(
                    participantsJoined =
                        buildTimeSeries(scope.memberships.map { it.createdAt }, groupBy),
                    submissionsCreated =
                        buildTimeSeries(
                            scope.submissionsByMembershipId.values.flatten().map { it.createdAt },
                            groupBy,
                        ),
                    successesAchieved =
                        buildTimeSeries(
                            scope.memberships
                                .filter { it.completionStatus == TaskCompletionStatus.SUCCESS }
                                .map { it.updatedAt },
                            groupBy,
                        ),
                ),
        )
    }

    fun exportParticipantsCsv(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        participationApproved: String?,
        completionStatus: String?,
        realName: String,
    ): String {
        val scope =
            loadParticipantAnalyticsScope(
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
        val taskById = scope.tasks.associateBy { it.id!! }
        val rows = buildList {
            add(
                csvRow(
                    "Task ID",
                    "Task Title",
                    "Category",
                    "Task Rank",
                    "Task Creator",
                    "Created At",
                    "Deadline",
                    "Member ID",
                    "Username",
                    "Real Name",
                    "Student ID",
                    "Grade",
                    "Major",
                    "Class",
                    "Phone",
                    "Email",
                    "Apply Reason",
                    "Reject Reason",
                    "Approval Status",
                    "Completion Status",
                    "Is Team",
                    "Join Date",
                )
            )
            scope.memberships.forEach { membership ->
                val task = taskById[membership.task!!.id!!] ?: return@forEach
                val firstStudent = studentProfilesOf(membership).firstOrNull()
                add(
                    csvRow(
                        task.id,
                        task.name,
                        task.category.name,
                        task.rank ?: "",
                        task.creator.username ?: "",
                        task.createdAt,
                        task.deadline ?: "",
                        membership.memberId ?: "",
                        "",
                        firstStudent?.realName ?: "",
                        firstStudent?.studentId ?: "",
                        firstStudent?.grade ?: "",
                        firstStudent?.major ?: "",
                        firstStudent?.className ?: "",
                        membership.phone ?: "",
                        membership.email ?: "",
                        membership.applyReason ?: "",
                        membership.rejectReason ?: "",
                        membership.approved?.name ?: ApproveType.NONE.name,
                        membership.completionStatus.name,
                        membership.isTeam,
                        membership.createdAt,
                    )
                )
            }
        }
        return rows.joinToString("\n", postfix = "\n")
    }

    fun exportTasksCsv(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        hasPendingReview: Boolean?,
        hasPendingApproval: Boolean?,
    ): String {
        val analytics =
            getTasks(
                spaceId = spaceId,
                from = from,
                to = to,
                categoryId = categoryId,
                publisherId = publisherId,
                taskApproved = taskApproved,
                hasPendingReview = hasPendingReview,
                hasPendingApproval = hasPendingApproval,
                sortBy = "createdAt",
                sortOrder = "desc",
            )
        val rows = buildList {
            add(
                csvRow(
                    "Task ID",
                    "Task Title",
                    "Category",
                    "Rank",
                    "Creator",
                    "Created At",
                    "Deadline",
                    "Total Participants",
                    "Approved",
                    "Rejected",
                    "Pending",
                    "Submitted",
                    "Pending Review",
                    "Completed",
                    "Failed",
                    "Task Status",
                )
            )
            analytics.tasks.orEmpty().forEach { task ->
                add(
                    csvRow(
                        task.taskId,
                        task.taskName,
                        task.category.name,
                        "",
                        task.publisher.name,
                        toLocalDateTime(task.createdAt),
                        task.deadline?.let(::toLocalDateTime) ?: "",
                        task.participantCount,
                        task.approvedParticipantCount,
                        task.rejectedParticipantCount,
                        task.pendingParticipantApprovalCount,
                        task.submittedParticipantCount,
                        task.pendingReviewCount,
                        task.successfulParticipantCount,
                        task.failedParticipantCount,
                        task.approved.value,
                    )
                )
            }
        }
        return rows.joinToString("\n", postfix = "\n")
    }

    fun exportPublishersCsv(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        taskApproved: String?,
    ): String {
        val analytics =
            getPublishers(
                spaceId = spaceId,
                from = from,
                to = to,
                categoryId = categoryId,
                taskApproved = taskApproved,
                sortBy = "taskCount",
                sortOrder = "desc",
            )
        val rows = buildList {
            add(
                csvRow(
                    "Publisher ID",
                    "Publisher Name",
                    "Total Tasks",
                    "Total Participants",
                    "Approved Participants",
                    "Submitted Participants",
                    "Successful Participants",
                    "Average Participants Per Task",
                    "Submission Conversion Rate",
                    "Success Rate",
                    "Last Task Created At",
                )
            )
            analytics.publishers.orEmpty().forEach { publisher ->
                add(
                    csvRow(
                        publisher.publisherId,
                        publisher.publisherName,
                        publisher.taskCount,
                        publisher.participantCount,
                        publisher.approvedParticipantCount,
                        publisher.submittedParticipantCount,
                        publisher.successfulParticipantCount,
                        publisher.avgParticipantsPerTask,
                        publisher.submissionConversionRate,
                        publisher.successRate,
                        toLocalDateTime(publisher.lastTaskCreatedAt),
                    )
                )
            }
        }
        return rows.joinToString("\n", postfix = "\n")
    }

    fun getAlerts(spaceId: IdType): SpaceAnalyticsAlertsDTO {
        val tasks = filterTasks(spaceId, null, null, null, null, null)
        val membershipsByTaskId = loadMembershipsByTaskId(tasks)
        val memberships = membershipsByTaskId.values.flatten()
        val submissionsByMembershipId = loadSubmissionsByMembershipId(memberships)
        val reviewsBySubmissionId =
            loadReviewsBySubmissionId(submissionsByMembershipId.values.flatten())
        val now = LocalDateTime.now()
        val latestSubmissions =
            memberships.mapNotNull { membership ->
                latestSubmissionOf(membership, submissionsByMembershipId)
            }

        // Phase 1 uses fixed recency thresholds for governance alerts until a configurable policy
        // exists.
        return SpaceAnalyticsAlertsDTO(
            pendingTaskApprovalCount = tasks.count { it.approved == ApproveType.NONE },
            pendingParticipantApprovalCount = memberships.count { it.approved == ApproveType.NONE },
            pendingSubmissionReviewCount =
                memberships.count {
                    latestSubmissionOf(it, submissionsByMembershipId)?.let { latest ->
                        reviewsBySubmissionId[latest.id] == null
                    } == true
                },
            stalledTaskCount =
                tasks.count { task ->
                    val taskMemberships = membershipsByTaskId[task.id].orEmpty()
                    olderThan(task.createdAt, now.minusDays(STALLED_TASK_THRESHOLD_DAYS)) &&
                        taskMemberships.any { it.approved == ApproveType.APPROVED } &&
                        taskMemberships.none {
                            submissionsByMembershipId[it.id].orEmpty().isNotEmpty()
                        }
                },
            overdueUnreviewedSubmissionCount =
                latestSubmissions.count { submission ->
                    reviewsBySubmissionId[submission.id] == null &&
                        olderThan(
                            submission.createdAt,
                            now.minusDays(OVERDUE_UNREVIEWED_SUBMISSION_THRESHOLD_DAYS),
                        )
                },
            inactivePublisherCount =
                tasks
                    .groupBy { it.creator.id!!.toLong() }
                    .count { (_, publisherTasks) ->
                        olderThan(
                            publisherTasks.maxOf { task -> task.createdAt },
                            now.minusDays(INACTIVE_PUBLISHER_THRESHOLD_DAYS),
                        )
                    },
        )
    }

    private fun filterTasks(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
    ): List<Task> =
        taskRepository.findAnalyticsTasks(
            spaceId = spaceId,
            from = from?.let(::toLocalDateTime),
            to = to?.let(::toLocalDateTime),
            categoryId = categoryId,
            publisherId = publisherId,
            approved = taskApproved?.takeUnless(String::isBlank)?.let(ApproveType::valueOf),
        )

    private fun loadMembershipsByTaskId(tasks: List<Task>): Map<IdType, List<TaskMembership>> {
        val taskIds = tasks.mapNotNull { it.id }
        if (taskIds.isEmpty()) return emptyMap()
        return taskMembershipRepository.findAllByTaskIdIn(taskIds).groupBy { it.task!!.id!! }
    }

    private fun loadSubmissionsByMembershipId(
        memberships: List<TaskMembership>
    ): Map<IdType, List<org.rucca.cheese.task.TaskSubmission>> {
        val membershipIds = memberships.mapNotNull { it.id }
        if (membershipIds.isEmpty()) return emptyMap()
        return taskSubmissionRepository.findAllByMembershipIdIn(membershipIds).groupBy {
            it.membership!!.id!!
        }
    }

    private fun loadReviewsBySubmissionId(
        submissions: List<org.rucca.cheese.task.TaskSubmission>
    ): Map<IdType, TaskSubmissionReview> {
        val submissionIds = submissions.mapNotNull { it.id }
        if (submissionIds.isEmpty()) return emptyMap()
        return taskSubmissionReviewRepository.findAllBySubmissionIdIn(submissionIds).associateBy {
            it.submission!!.id!!
        }
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

    private fun studentCountOf(membership: TaskMembership): Int =
        if (membership.isTeam) membership.teamMembersRealNameInfo.size else 1

    private fun loadParticipantAnalyticsScope(
        spaceId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        publisherId: Long?,
        taskApproved: String?,
        participationApproved: String?,
        completionStatus: String?,
        realName: String,
    ): ParticipantAnalyticsScope {
        val tasks = filterTasks(spaceId, from, to, categoryId, publisherId, taskApproved)
        val memberships =
            loadMembershipsByTaskId(tasks).values.flatten().filter { membership ->
                (participationApproved.isNullOrBlank() ||
                    membership.approved?.name == participationApproved) &&
                    (completionStatus.isNullOrBlank() ||
                        membership.completionStatus.name == completionStatus) &&
                    matchesRealNameFilter(membership, realName)
            }
        return ParticipantAnalyticsScope(
            tasks = tasks,
            memberships = memberships,
            submissionsByMembershipId = loadSubmissionsByMembershipId(memberships),
        )
    }

    private fun matchesRealNameFilter(membership: TaskMembership, realName: String): Boolean =
        when (realName.lowercase()) {
            "with" -> membership.hasRealName()
            "without" -> !membership.hasRealName()
            else -> true
        }

    private fun TaskMembership.hasRealName(): Boolean =
        studentProfilesOf(this).any { studentProfile -> studentProfile.hasRealName }

    private data class StudentProfile(
        val realName: String?,
        val studentId: String?,
        val grade: String?,
        val major: String?,
        val className: String?,
    ) {
        val hasRealName: Boolean = !realName.isNullOrBlank()
    }

    private fun studentProfilesOf(membership: TaskMembership): List<StudentProfile> =
        if (membership.isTeam) {
            membership.teamMembersRealNameInfo.map { teamMember ->
                toStudentProfile(membership, teamMember)
            }
        } else {
            listOf(toStudentProfile(membership))
        }

    private fun toStudentProfile(membership: TaskMembership): StudentProfile {
        val realNameInfo = taskMembershipSnapshotService.getRealNameInfoFromMembership(membership)
        return StudentProfile(
            realName = realNameInfo?.realName,
            studentId = realNameInfo?.studentId,
            grade = realNameInfo?.grade,
            major = realNameInfo?.major,
            className = realNameInfo?.className,
        )
    }

    private fun toStudentProfile(
        membership: TaskMembership,
        teamMember: TeamMemberRealNameInfo,
    ): StudentProfile {
        val realNameInfo =
            taskMembershipSnapshotService.getRealNameInfoForTeamMemberSnapshot(
                membership,
                teamMember,
            )
        return StudentProfile(
            realName = realNameInfo.realName,
            studentId = realNameInfo.studentId,
            grade = realNameInfo.grade,
            major = realNameInfo.major,
            className = realNameInfo.className,
        )
    }

    private fun csvRow(vararg values: Any?): String = values.joinToString(",") { csvCell(it) }

    private fun csvCell(value: Any?): String {
        val raw = value?.toString() ?: ""
        if (raw.none { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            return raw
        }
        return "\"${raw.replace("\"", "\"\"")}\""
    }

    private fun safeRate(numerator: Int, denominator: Int): Double =
        if (denominator == 0) 0.0 else numerator.toDouble() / denominator.toDouble()

    private fun latestSubmissionOf(
        membership: TaskMembership,
        submissionsByMembershipId: Map<IdType, List<org.rucca.cheese.task.TaskSubmission>>,
    ): org.rucca.cheese.task.TaskSubmission? =
        submissionsByMembershipId[membership.id]
            .orEmpty()
            .maxWithOrNull(
                compareBy<org.rucca.cheese.task.TaskSubmission>({ it.version }, { it.createdAt })
            )

    private fun olderThan(timestamp: LocalDateTime, cutoff: LocalDateTime): Boolean =
        !timestamp.isAfter(cutoff)

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
        val date =
            when (groupBy.lowercase()) {
                "week" -> timestamp.toLocalDate().with(DayOfWeek.MONDAY)
                "month" -> timestamp.toLocalDate().withDayOfMonth(1)
                else -> timestamp.toLocalDate()
            }

        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun toLocalDateTime(epochMilli: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault())
}

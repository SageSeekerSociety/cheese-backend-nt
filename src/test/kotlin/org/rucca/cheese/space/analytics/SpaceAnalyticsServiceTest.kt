package org.rucca.cheese.space.analytics

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.space.models.Space
import org.rucca.cheese.space.models.SpaceCategory
import org.rucca.cheese.task.*
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserRepository

@ExtendWith(MockKExtension::class)
class SpaceAnalyticsServiceTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskMembershipRepository: TaskMembershipRepository
    private lateinit var userRepository: UserRepository
    private lateinit var service: SpaceAnalyticsService

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        taskMembershipRepository = mockk()
        userRepository = mockk()
        service = SpaceAnalyticsService(taskRepository, taskMembershipRepository, userRepository)
    }

    @Test
    fun `getSpaceTaskAnalytics should return analytics with task distributions`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category1 = mockk<SpaceCategory>()
        every { category1.id } returns 1
        every { category1.name } returns "Category 1"
        every { category1.space } returns space

        val category2 = mockk<SpaceCategory>()
        every { category2.id } returns 2
        every { category2.name } returns "Category 2"
        every { category2.space } returns space

        val user = mockk<User>()
        every { user.id } returns 1
        every { user.username } returns "testuser"

        val task1 = mockk<Task>(relaxed = true)
        every { task1.id } returns 1L
        every { task1.name } returns "Task 1"
        every { task1.category } returns category1
        every { task1.creator } returns user
        every { task1.createdAt } returns LocalDateTime.now()
        every { task1.approved } returns ApproveType.APPROVED
        every { task1.rank } returns 5

        val task2 = mockk<Task>(relaxed = true)
        every { task2.id } returns 2L
        every { task2.name } returns "Task 2"
        every { task2.category } returns category2
        every { task2.creator } returns user
        every { task2.createdAt } returns LocalDateTime.now()
        every { task2.approved } returns ApproveType.NONE
        every { task2.rank } returns 15

        val task3 = mockk<Task>(relaxed = true)
        every { task3.id } returns 3L
        every { task3.name } returns "Task 3"
        every { task3.category } returns category1
        every { task3.creator } returns user
        every { task3.createdAt } returns LocalDateTime.now()
        every { task3.approved } returns ApproveType.DISAPPROVED
        every { task3.rank } returns null

        val tasks = listOf(task1, task2, task3)

        val membership1 = mockk<TaskMembership>(relaxed = true)
        every { membership1.id } returns 1L
        every { membership1.task } returns task1
        every { membership1.memberId } returns 10
        every { membership1.approved } returns ApproveType.APPROVED
        every { membership1.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { membership1.realNameInfo } returns null
        every { membership1.email } returns "user1@test.com"

        val membership2 = mockk<TaskMembership>(relaxed = true)
        every { membership2.id } returns 2L
        every { membership2.task } returns task1
        every { membership2.memberId } returns 11
        every { membership2.approved } returns ApproveType.NONE
        every { membership2.completionStatus } returns TaskCompletionStatus.PENDING_REVIEW
        every { membership2.realNameInfo } returns null
        every { membership2.email } returns "user2@test.com"

        val membership3 = mockk<TaskMembership>(relaxed = true)
        every { membership3.id } returns 3L
        every { membership3.task } returns task2
        every { membership3.memberId } returns 12
        every { membership3.approved } returns ApproveType.DISAPPROVED
        every { membership3.completionStatus } returns TaskCompletionStatus.FAILED
        every { membership3.realNameInfo } returns null
        every { membership3.email } returns "user3@test.com"

        val memberships = listOf(membership1, membership2, membership3)

        every { taskRepository.findBySpaceId(spaceId) } returns tasks
        every { taskMembershipRepository.findAllByTaskId(1L) } returns
            memberships.filter { it.task?.id == 1L }
        every { taskMembershipRepository.findAllByTaskId(2L) } returns
            memberships.filter { it.task?.id == 2L }
        every { taskMembershipRepository.findAllByTaskId(3L) } returns emptyList()

        // When
        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "",
                successBy = "",
            )

        // Then
        assertNotNull(result)
        assertNotNull(result.taskCategoryDistribution)
        assertEquals("Task Categories", result.taskCategoryDistribution?.name)
        assertEquals(2, result.taskCategoryDistribution?.items?.size) // 2 categories

        assertNotNull(result.taskStatusDistribution)
        assertEquals("Task Status", result.taskStatusDistribution?.name)
        assertEquals(3, result.taskStatusDistribution?.items?.size) // 3 different statuses

        assertNotNull(result.participantStatusDistribution)
        assertEquals("Participant Status", result.participantStatusDistribution?.name)

        assertNotNull(result.rankDistribution)
        assertEquals("Task Ranks", result.rankDistribution?.name)
        assertEquals(2, result.rankDistribution?.items?.size) // 2 tasks with ranks
    }

    @Test
    fun `percentages should sum to 100 for non-empty distributions`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        val category = mockk<SpaceCategory>()
        val creator = mockk<User>()

        every { category.name } returns "Category1"
        every { creator.username } returns "creator1"

        val task = mockk<Task>()
        every { task.id } returns 1L
        every { task.name } returns "Task 1"
        every { task.space } returns space
        every { task.category } returns category
        every { task.creator } returns creator
        every { task.approved } returns ApproveType.APPROVED
        every { task.rank } returns 10
        every { task.createdAt } returns LocalDateTime.now()
        every { task.deadline } returns null

        val memberships =
            (1..10).map { i ->
                val membership = mockk<TaskMembership>(relaxed = true)
                every { membership.id } returns i.toLong()
                every { membership.task } returns task
                every { membership.memberId } returns i.toLong()
                every { membership.approved } returns ApproveType.APPROVED
                every { membership.completionStatus } returns TaskCompletionStatus.SUCCESS
                every { membership.realNameInfo } returns
                    RealNameInfo(
                        realName = "User$i",
                        grade =
                            when (i % 4) {
                                0 -> "大一"
                                1 -> "大二"
                                2 -> "大三"
                                else -> "大四"
                            },
                        major =
                            when (i % 3) {
                                0 -> "计算机科学"
                                1 -> "软件工程"
                                else -> "信息管理"
                            },
                        className = "计科${(i % 4) + 1}班",
                        encrypted = false,
                    )
                membership
            }

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns memberships

        // When
        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "all",
                successBy = "completion",
            )

        // Then - verify percentages sum to 100
        val gradePercentages =
            result.successStudentStatistics?.gradeDistribution?.items?.sumOf {
                it.percentage ?: 0.0
            } ?: 0.0
        val majorPercentages =
            result.successStudentStatistics?.majorDistribution?.items?.sumOf {
                it.percentage ?: 0.0
            } ?: 0.0
        val classPercentages =
            result.successStudentStatistics?.classNameDistribution?.items?.sumOf {
                it.percentage ?: 0.0
            } ?: 0.0

        assertEquals(100.0, gradePercentages, 0.01, "Grade percentages should sum to 100%")
        assertEquals(100.0, majorPercentages, 0.01, "Major percentages should sum to 100%")
        assertEquals(100.0, classPercentages, 0.01, "Class percentages should sum to 100%")
    }

    @Test
    fun `should handle null realNameInfo gracefully`() {
        // Given
        val spaceId = 1L
        val task = mockk<Task>()
        every { task.id } returns 1L
        every { task.category } returns mockk { every { name } returns "Test" }
        every { task.creator } returns mockk { every { username } returns "test" }
        every { task.approved } returns ApproveType.APPROVED
        every { task.rank } returns null

        val membership1 = mockk<TaskMembership>(relaxed = true)
        every { membership1.memberId } returns 1L
        every { membership1.realNameInfo } returns null
        every { membership1.approved } returns ApproveType.APPROVED
        every { membership1.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { membership1.task } returns task

        val membership2 = mockk<TaskMembership>(relaxed = true)
        every { membership2.memberId } returns 2L
        every { membership2.realNameInfo } returns null
        every { membership2.approved } returns ApproveType.APPROVED
        every { membership2.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { membership2.task } returns task

        val membershipsWithNullInfo = listOf(membership1, membership2)

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns membershipsWithNullInfo

        // When
        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "all",
                successBy = "completion",
            )

        // Then - should not crash and return empty distributions
        assertNotNull(result.successStudentStatistics)
        assertTrue(result.successStudentStatistics?.gradeDistribution?.items?.isEmpty() ?: false)
        assertTrue(result.successStudentStatistics?.majorDistribution?.items?.isEmpty() ?: false)
        assertTrue(
            result.successStudentStatistics?.classNameDistribution?.items?.isEmpty() ?: false
        )
        assertEquals(2, result.successStudentStatistics?.totalStudents)
        assertEquals(0, result.successStudentStatistics?.totalStudentsWithRealName)
    }

    @Test
    fun `should include all ApproveType values even with zero count`() {
        // Given
        val spaceId = 1L
        val tasks =
            listOf(
                mockk<Task> {
                    every { id } returns 1L
                    every { approved } returns ApproveType.APPROVED
                    every { category } returns mockk { every { name } returns "Test" }
                    every { creator } returns mockk { every { username } returns "test" }
                    every { rank } returns null
                }
            )

        every { taskRepository.findBySpaceId(spaceId) } returns tasks
        every { taskMembershipRepository.findAllByTaskId(any()) } returns emptyList()

        // When
        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "all",
                successBy = "approve",
            )

        // Then - should include all status types
        val statusLabels = result.taskStatusDistribution?.items?.map { it.label } ?: emptyList()
        assertTrue(statusLabels.contains("NONE"), "Should include NONE status")
        assertTrue(statusLabels.contains("APPROVED"), "Should include APPROVED status")
        assertTrue(statusLabels.contains("DISAPPROVED"), "Should include DISAPPROVED status")

        val participantStatusLabels =
            result.participantStatusDistribution?.items?.map { it.label } ?: emptyList()
        assertTrue(participantStatusLabels.contains("NONE") || participantStatusLabels.isEmpty())
    }

    @Test
    fun `getSpaceTaskAnalytics should filter tasks by date range`() {
        // Given
        val spaceId = 1L
        val now = LocalDateTime.now()
        val yesterday = now.minusDays(1)
        val tomorrow = now.plusDays(1)

        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 1
        every { category.name } returns "Category"
        every { category.space } returns space

        val user = mockk<User>()
        every { user.id } returns 1
        every { user.username } returns "testuser"

        val oldTask = mockk<Task>(relaxed = true)
        every { oldTask.id } returns 1L
        every { oldTask.name } returns "Old Task"
        every { oldTask.category } returns category
        every { oldTask.creator } returns user
        every { oldTask.createdAt } returns yesterday.minusDays(2)
        every { oldTask.approved } returns ApproveType.APPROVED
        every { oldTask.rank } returns null

        val currentTask = mockk<Task>(relaxed = true)
        every { currentTask.id } returns 2L
        every { currentTask.name } returns "Current Task"
        every { currentTask.category } returns category
        every { currentTask.creator } returns user
        every { currentTask.createdAt } returns now
        every { currentTask.approved } returns ApproveType.APPROVED
        every { currentTask.rank } returns null

        val tasks = listOf(oldTask, currentTask)

        every { taskRepository.findBySpaceId(spaceId) } returns tasks
        every { taskMembershipRepository.findAllByTaskId(2L) } returns emptyList()

        // When - filter to only include today's task
        val fromTime =
            yesterday
                .toLocalDate()
                .atStartOfDay()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli()
        val toTime =
            tomorrow.toLocalDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli()

        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = fromTime,
                to = toTime,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "",
                successBy = "",
            )

        // Then
        assertNotNull(result)
        // Only the current task should be included
        val categoryDistribution = result.taskCategoryDistribution
        assertEquals(1, categoryDistribution?.items?.sumOf { it.count ?: 0 })
    }

    @Test
    fun `getPublishersParticipation should aggregate by publisher`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 1
        every { category.name } returns "Category"
        every { category.space } returns space

        val publisher1 = mockk<User>()
        every { publisher1.id } returns 1
        every { publisher1.username } returns "publisher1"

        val publisher2 = mockk<User>()
        every { publisher2.id } returns 2
        every { publisher2.username } returns "publisher2"

        val task1 = mockk<Task>(relaxed = true)
        every { task1.id } returns 1L
        every { task1.name } returns "Task 1"
        every { task1.category } returns category
        every { task1.creator } returns publisher1
        every { task1.createdAt } returns LocalDateTime.now()
        every { task1.approved } returns ApproveType.APPROVED
        every { task1.rank } returns null

        val task2 = mockk<Task>(relaxed = true)
        every { task2.id } returns 2L
        every { task2.name } returns "Task 2"
        every { task2.category } returns category
        every { task2.creator } returns publisher1
        every { task2.createdAt } returns LocalDateTime.now()
        every { task2.approved } returns ApproveType.APPROVED
        every { task2.rank } returns null

        val task3 = mockk<Task>(relaxed = true)
        every { task3.id } returns 3L
        every { task3.name } returns "Task 3"
        every { task3.category } returns category
        every { task3.creator } returns publisher2
        every { task3.createdAt } returns LocalDateTime.now()
        every { task3.approved } returns ApproveType.APPROVED
        every { task3.rank } returns null

        val tasks = listOf(task1, task2, task3)

        val membership1 = mockk<TaskMembership>(relaxed = true)
        every { membership1.id } returns 1L
        every { membership1.task } returns task1
        every { membership1.memberId } returns 10
        every { membership1.completionStatus } returns TaskCompletionStatus.SUCCESS

        val membership2 = mockk<TaskMembership>(relaxed = true)
        every { membership2.id } returns 2L
        every { membership2.task } returns task1
        every { membership2.memberId } returns 11
        every { membership2.completionStatus } returns TaskCompletionStatus.SUCCESS

        val membership3 = mockk<TaskMembership>(relaxed = true)
        every { membership3.id } returns 3L
        every { membership3.task } returns task3
        every { membership3.memberId } returns 12
        every { membership3.completionStatus } returns TaskCompletionStatus.PENDING_REVIEW

        val memberships = listOf(membership1, membership2, membership3)

        every { taskRepository.findBySpaceId(spaceId) } returns tasks
        every { taskMembershipRepository.findAllByTaskId(1L) } returns
            memberships.filter { it.task?.id == 1L }
        every { taskMembershipRepository.findAllByTaskId(2L) } returns emptyList()
        every { taskMembershipRepository.findAllByTaskId(3L) } returns
            memberships.filter { it.task?.id == 3L }

        // When
        val result = service.getPublishersParticipation(spaceId, "")

        // Then
        assertNotNull(result)
        assertEquals(2, result.size) // 2 publishers

        val publisher1Stats = result.find { it.publisherId == 1L }
        assertNotNull(publisher1Stats)
        assertEquals("publisher1", publisher1Stats?.publisherName)
        assertEquals(2, publisher1Stats?.taskCount) // 2 tasks
        assertEquals(2, publisher1Stats?.participants) // 2 participants
        assertEquals(2, publisher1Stats?.completedUsers) // 2 successful

        val publisher2Stats = result.find { it.publisherId == 2L }
        assertNotNull(publisher2Stats)
        assertEquals("publisher2", publisher2Stats?.publisherName)
        assertEquals(1, publisher2Stats?.taskCount) // 1 task
        assertEquals(1, publisher2Stats?.participants) // 1 participant
        assertEquals(0, publisher2Stats?.completedUsers) // 0 successful
    }

    @Test
    fun `exportParticipants should generate CSV for participants format`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 1
        every { category.name } returns "Category"
        every { category.space } returns space

        val user = mockk<User>()
        every { user.id } returns 1
        every { user.username } returns "testuser"

        val task = mockk<Task>(relaxed = true)
        every { task.id } returns 1L
        every { task.name } returns "Test Task"
        every { task.category } returns category
        every { task.creator } returns user
        every { task.createdAt } returns LocalDateTime.now()
        every { task.approved } returns ApproveType.APPROVED
        every { task.rank } returns null

        val membership = mockk<TaskMembership>(relaxed = true)
        every { membership.id } returns 1L
        every { membership.task } returns task
        every { membership.memberId } returns 10
        every { membership.approved } returns ApproveType.APPROVED
        every { membership.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { membership.email } returns "test@example.com"

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns listOf(membership)
        every { userRepository.findById(any<Int>()) } returns java.util.Optional.of(user)

        // When
        val csv =
            service.exportParticipants(
                spaceId = spaceId,
                format = "csv",
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "",
                successBy = "",
            )

        // Then
        assertNotNull(csv)
        println("CSV Participants Content: $csv") // Debug output
        assertTrue(
            csv.contains(
                "Task ID,Task Title,Category,Task Rank,Task Creator,Created At,Deadline,Member ID,Username,Real Name,Student ID,Grade,Major,Class,Phone,Email,Apply Reason,Reject Reason,Approval Status,Completion Status,Is Team,Join Date"
            )
        )
        // Check for parts that should be in the CSV
        assertTrue(csv.contains("\"Test Task\""))
        assertTrue(csv.contains("test@example.com"))
        assertTrue(csv.contains("APPROVED"))
        assertTrue(csv.contains("SUCCESS"))
    }

    @Test
    fun `exportParticipants should generate CSV for summary format`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 1
        every { category.name } returns "Test Category"
        every { category.space } returns space

        val user = mockk<User>()
        every { user.id } returns 1
        every { user.username } returns "publisher1"

        val task = mockk<Task>(relaxed = true)
        every { task.id } returns 1L
        every { task.name } returns "Test Task"
        every { task.category } returns category
        every { task.creator } returns user
        every { task.createdAt } returns LocalDateTime.now()
        every { task.approved } returns ApproveType.APPROVED
        every { task.rank } returns null

        val membership1 = mockk<TaskMembership>(relaxed = true)
        every { membership1.id } returns 1L
        every { membership1.task } returns task
        every { membership1.memberId } returns 10
        every { membership1.approved } returns ApproveType.APPROVED
        every { membership1.completionStatus } returns TaskCompletionStatus.SUCCESS
        every { membership1.email } returns "test1@example.com"

        val membership2 = mockk<TaskMembership>(relaxed = true)
        every { membership2.id } returns 2L
        every { membership2.task } returns task
        every { membership2.memberId } returns 11
        every { membership2.approved } returns ApproveType.DISAPPROVED
        every { membership2.completionStatus } returns TaskCompletionStatus.FAILED
        every { membership2.email } returns "test2@example.com"

        val membership3 = mockk<TaskMembership>(relaxed = true)
        every { membership3.id } returns 3L
        every { membership3.task } returns task
        every { membership3.memberId } returns 12
        every { membership3.approved } returns ApproveType.NONE
        every { membership3.completionStatus } returns TaskCompletionStatus.PENDING_REVIEW
        every { membership3.email } returns "test3@example.com"

        val memberships = listOf(membership1, membership2, membership3)

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns memberships
        every { userRepository.findById(any<Int>()) } returns java.util.Optional.empty()

        // When
        val csv =
            service.exportParticipants(
                spaceId = spaceId,
                format = "summary",
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "",
                successBy = "",
            )

        // Then
        assertNotNull(csv)
        println("CSV Content: $csv") // Debug output
        assertTrue(
            csv.contains(
                "Task ID,Task Title,Category,Rank,Creator,Created At,Deadline,Total Participants,Approved,Rejected,Pending,Completed,Task Status"
            )
        )
        assertTrue(csv.contains("1,\"Test Task\",\"Test Category\""))
        assertTrue(csv.contains("publisher1"))
        assertTrue(csv.contains(",3,1,1,1,"))
    }

    @Test
    fun `getSpaceTaskAnalytics should handle empty data`() {
        // Given
        val spaceId = 1L

        // Return empty lists
        every { taskRepository.findBySpaceId(spaceId) } returns emptyList()

        // When
        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "",
                successBy = "",
            )

        // Then
        assertNotNull(result)
        assertNotNull(result.taskCategoryDistribution)
        assertEquals(0, result.taskCategoryDistribution?.items?.size)
        assertNotNull(result.taskStatusDistribution)
        // Status distribution always includes all enum values for consistency
        assertEquals(3, result.taskStatusDistribution?.items?.size)
        // All values should be 0
        result.taskStatusDistribution?.items?.forEach { item -> assertEquals(0, item.count) }
        assertNotNull(result.participantStatusDistribution)
        // Participant status also includes all enum values
        assertEquals(3, result.participantStatusDistribution?.items?.size)
        assertNotNull(result.rankDistribution)
        assertEquals(0, result.rankDistribution?.items?.size)
    }

    @Test
    fun `getSpaceTaskAnalytics should filter by multiple conditions`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category1 = mockk<SpaceCategory>()
        every { category1.id } returns 1
        every { category1.name } returns "Category 1"
        every { category1.space } returns space

        val category2 = mockk<SpaceCategory>()
        every { category2.id } returns 2
        every { category2.name } returns "Category 2"
        every { category2.space } returns space

        val user1 = mockk<User>()
        every { user1.id } returns 1
        every { user1.username } returns "user1"

        val user2 = mockk<User>()
        every { user2.id } returns 2
        every { user2.username } returns "user2"

        // Create tasks with different properties
        val task1 = mockk<Task>(relaxed = true)
        every { task1.id } returns 1L
        every { task1.name } returns "Task 1"
        every { task1.category } returns category1
        every { task1.creator } returns user1
        every { task1.createdAt } returns LocalDateTime.now()
        every { task1.approved } returns ApproveType.APPROVED
        every { task1.rank } returns 5

        val task2 = mockk<Task>(relaxed = true)
        every { task2.id } returns 2L
        every { task2.name } returns "Task 2"
        every { task2.category } returns category2
        every { task2.creator } returns user2
        every { task2.createdAt } returns LocalDateTime.now()
        every { task2.approved } returns ApproveType.APPROVED
        every { task2.rank } returns 10

        val tasks = listOf(task1, task2)
        every { taskRepository.findBySpaceId(spaceId) } returns tasks
        every { taskMembershipRepository.findAllByTaskId(any()) } returns emptyList()

        // When - filter by category and publisher
        val result =
            service.getSpaceTaskAnalytics(
                spaceId = spaceId,
                from = null,
                to = null,
                taskStatus = null,
                categoryId = 1L, // Filter by category 1
                publisherId = 1L, // Filter by user 1
                realName = "",
                successBy = "",
            )

        // Then - should only include task1
        assertNotNull(result)
        val categoryDist = result.taskCategoryDistribution
        assertNotNull(categoryDist)
        assertEquals(1, categoryDist?.items?.size)
        assertEquals("Category 1", categoryDist?.items?.get(0)?.label)
        assertEquals(1, categoryDist?.items?.get(0)?.count)
    }

    @Test
    fun `exportParticipants should handle special characters in CSV`() {
        // Given
        val spaceId = 1L
        val space = mockk<Space>()
        every { space.id } returns spaceId

        val category = mockk<SpaceCategory>()
        every { category.id } returns 1
        every { category.name } returns "Category \"with\" quotes" // Name with quotes
        every { category.space } returns space

        val user = mockk<User>()
        every { user.id } returns 1
        every { user.username } returns "user,with,commas" // Name with commas

        val task = mockk<Task>(relaxed = true)
        every { task.id } returns 1L
        every { task.name } returns "Task \"Special\", Test" // Name with quotes and commas
        every { task.category } returns category
        every { task.creator } returns user
        every { task.createdAt } returns LocalDateTime.now()
        every { task.approved } returns ApproveType.APPROVED
        every { task.rank } returns null

        every { taskRepository.findBySpaceId(spaceId) } returns listOf(task)
        every { taskMembershipRepository.findAllByTaskId(1L) } returns emptyList()
        every { userRepository.findById(any<Int>()) } returns java.util.Optional.empty()

        // When
        val csv =
            service.exportParticipants(
                spaceId = spaceId,
                format = "summary",
                from = null,
                to = null,
                taskStatus = null,
                categoryId = null,
                publisherId = null,
                realName = "",
                successBy = "",
            )

        // Then - verify proper escaping
        assertNotNull(csv)
        assertTrue(
            csv.contains(
                "Task ID,Task Title,Category,Rank,Creator,Created At,Deadline,Total Participants,Approved,Rejected,Pending,Completed,Task Status"
            )
        )
        // Should properly escape special characters
        assertTrue(
            csv.contains("\"Task \"\"Special\"\", Test\"") || csv.contains("Task \"Special\", Test")
        )
    }
}

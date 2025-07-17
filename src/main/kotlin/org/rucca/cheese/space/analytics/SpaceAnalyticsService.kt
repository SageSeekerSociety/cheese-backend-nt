/*
 *  Description: This file implements the SpaceAnalyticsService class.
 *               It provides data insights and analytics for specific Space.
 *
 *  Author(s):
 *      Claude Code
 *
 */

package org.rucca.cheese.space.analytics

import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.space.repositories.SpaceRepository
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.user.UserRepository
import org.rucca.cheese.user.services.UserRealNameService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SpaceAnalyticsService(
    private val spaceRepository: SpaceRepository,
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
    private val userRepository: UserRepository,
    private val userRealNameService: UserRealNameService,
) {
    fun getSpaceTaskStatistics(spaceId: IdType): SpaceTaskStatisticsDTO {
        // 获取空间下的所有任务
        val tasks = taskRepository.findBySpaceId(spaceId)

        // 1. 任务分类分布
        val taskCategoryDistribution = generateTaskCategoryDistribution(tasks)

        // 2. 任务状态分布 (unapproved, open, ongoing, completed)
        val taskStatusDistribution = generateTaskStatusDistribution(tasks)

        // 3. 参与者状态分布
        val participantStatusDistribution = generateParticipantStatusDistribution(spaceId)

        // 4. 排名分布
        val rankDistribution = generateRankDistribution(tasks)

        // 5. 成功学生统计信息
        val successStudentStatistics = generateSuccessStudentStatistics(spaceId)

        // 6. 失败学生统计信息
        val unsuccessStudentStatistics = generateUnsuccessStudentStatistics(spaceId)

        return SpaceTaskStatisticsDTO(
            taskCategoryDistribution = taskCategoryDistribution.toDistributionDTO(),
            taskStatusDistribution = taskStatusDistribution.toDistributionDTO(),
            participantStatusDistribution = participantStatusDistribution.toDistributionDTO(),
            rankDistribution = rankDistribution?.toDistributionDTO(),
            successStudentStatistics = successStudentStatistics,
            unsuccessStudentStatistics = unsuccessStudentStatistics,
        )
    }

    private fun generateTaskCategoryDistribution(
        tasks: List<org.rucca.cheese.task.Task>
    ): DistributionOneOfDTO {
        val categoryCount = tasks.groupingBy { it.category.name }.eachCount()
        val total = tasks.size

        val items =
            categoryCount.map { (categoryName, count) ->
                DiscreteDistributionItemDTO(
                    label = categoryName,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionOneOfDTO(
            name = "Task Category Distribution",
            type = DistributionOneOfDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateTaskStatusDistribution(
        tasks: List<org.rucca.cheese.task.Task>
    ): DistributionOneOfDTO {
        val statusCount =
            tasks
                .groupingBy { task ->
                    when (task.approved) {
                        org.rucca.cheese.common.persistent.ApproveType.APPROVED -> "open"
                        org.rucca.cheese.common.persistent.ApproveType.DISAPPROVED -> "unapproved"
                        org.rucca.cheese.common.persistent.ApproveType.NONE -> "unapproved"
                    }
                }
                .eachCount()

        val total = tasks.size

        val items =
            statusCount.map { (status, count) ->
                DiscreteDistributionItemDTO(
                    label = status,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionOneOfDTO(
            name = "Task Status Distribution",
            type = DistributionOneOfDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateParticipantStatusDistribution(spaceId: IdType): DistributionOneOfDTO {
        val tasks = taskRepository.findBySpaceId(spaceId)
        val taskIds = tasks.mapNotNull { it.id }

        val allMemberships =
            taskIds.flatMap { taskId -> taskMembershipRepository.findAllByTaskId(taskId) }

        val statusCount = allMemberships.groupingBy { it.completionStatus.name }.eachCount()
        val total = allMemberships.size

        val items =
            statusCount.map { (status, count) ->
                DiscreteDistributionItemDTO(
                    label = status,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionOneOfDTO(
            name = "Participant Status Distribution",
            type = DistributionOneOfDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateRankDistribution(
        tasks: List<org.rucca.cheese.task.Task>
    ): DistributionOneOfDTO {
        val rankCount = tasks.filter { it.rank != null }.groupingBy { it.rank!! }.eachCount()
        val total = tasks.count { it.rank != null }

        val items =
            rankCount.map { (rank, count) ->
                DiscreteDistributionItemDTO(
                    label = rank.toString(),
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionOneOfDTO(
            name = "Rank Distribution",
            type = DistributionOneOfDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateSuccessStudentStatistics(
        spaceId: IdType
    ): StudentRealNameInfoStatisticsDTO {
        val tasks = taskRepository.findBySpaceId(spaceId)
        val taskIds = tasks.mapNotNull { it.id }

        val successfulMemberships =
            taskIds
                .flatMap { taskId -> taskMembershipRepository.findAllByTaskId(taskId) }
                .filter { it.completionStatus == TaskCompletionStatus.SUCCESS && !it.isTeam }

        val successfulUserIds = successfulMemberships.mapNotNull { it.memberId }

        return generateStudentStatistics(successfulUserIds, "Successful Students")
    }

    private fun generateUnsuccessStudentStatistics(
        spaceId: IdType
    ): StudentRealNameInfoStatisticsDTO {
        val tasks = taskRepository.findBySpaceId(spaceId)
        val taskIds = tasks.mapNotNull { it.id }

        val unsuccessfulMemberships =
            taskIds
                .flatMap { taskId -> taskMembershipRepository.findAllByTaskId(taskId) }
                .filter { it.completionStatus != TaskCompletionStatus.SUCCESS && !it.isTeam }

        val unsuccessfulUserIds = unsuccessfulMemberships.mapNotNull { it.memberId }

        return generateStudentStatistics(unsuccessfulUserIds, "Unsuccessful Students")
    }

    private fun generateStudentStatistics(
        userIds: List<IdType>,
        name: String,
    ): StudentRealNameInfoStatisticsDTO {
        val totalStudents = userIds.size

        // 获取有真实姓名信息的学生
        val studentsWithRealName =
            userIds.mapNotNull { userId ->
                try {
                    userRealNameService.getUserIdentity(userId)
                } catch (e: Exception) {
                    null
                }
            }

        val totalStudentsWithRealName = studentsWithRealName.size

        // 生成年级分布
        val gradeDistribution = generateGradeDistribution(studentsWithRealName)

        // 生成专业分布
        val majorDistribution = generateMajorDistribution(studentsWithRealName)

        // 生成班级分布
        val classNameDistribution = generateClassNameDistribution(studentsWithRealName)

        return StudentRealNameInfoStatisticsDTO(
            totalStudents = totalStudents,
            totalStudentsWithRealName = totalStudentsWithRealName,
            gradeDistribution = gradeDistribution.toDistributionDTO(),
            majorDistribution = majorDistribution.toDistributionDTO(),
            classNameDistribution = classNameDistribution.toDistributionDTO(),
        )
    }

    private fun generateGradeDistribution(students: List<UserIdentityDTO>): DistributionOneOfDTO {
        val gradeCount = students.groupingBy { it.grade }.eachCount()
        val total = students.size

        val items =
            gradeCount.map { (grade, count) ->
                DiscreteDistributionItemDTO(
                    label = grade,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionOneOfDTO(
            name = "Grade Distribution",
            type = DistributionOneOfDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateMajorDistribution(students: List<UserIdentityDTO>): DistributionOneOfDTO {
        val majorCount = students.groupingBy { it.major }.eachCount()
        val total = students.size

        val items =
            majorCount.map { (major, count) ->
                DiscreteDistributionItemDTO(
                    label = major,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionOneOfDTO(
            name = "Major Distribution",
            type = DistributionOneOfDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateClassNameDistribution(
        students: List<UserIdentityDTO>
    ): DistributionOneOfDTO {
        val classNameCount = students.groupingBy { it.className }.eachCount()
        val total = students.size

        val items =
            classNameCount.map { (className, count) ->
                DiscreteDistributionItemDTO(
                    label = className,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionOneOfDTO(
            name = "Class Name Distribution",
            type = DistributionOneOfDTO.Type.DISCRETE,
            items = items,
        )
    }

    // Temporary conversion function until API definition is fixed
    private fun DistributionOneOfDTO.toDistributionDTO(): DistributionDTO {
        // This is a workaround - the generated DistributionDTO expects
        // ContinuousDistributionItemDTO
        // but we need to convert discrete items to continuous items
        // This is not ideal but necessary due to the API generation issue
        val continuousItems =
            this.items.map { discreteItem ->
                ContinuousDistributionItemDTO(
                    rangeStart = 0.0.toBigDecimal(), // Placeholder values
                    rangeEnd = 1.0.toBigDecimal(), // Placeholder values
                    count = discreteItem.count,
                    percentage = discreteItem.percentage,
                )
            }

        return DistributionDTO(
            name = this.name,
            type =
                when (this.type) {
                    DistributionOneOfDTO.Type.DISCRETE -> DistributionDTO.Type.DISCRETE
                },
            items = continuousItems,
        )
    }
}

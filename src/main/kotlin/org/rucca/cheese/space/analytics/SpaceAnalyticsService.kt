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
            taskCategoryDistribution = taskCategoryDistribution,
            taskStatusDistribution = taskStatusDistribution,
            participantStatusDistribution = participantStatusDistribution,
            rankDistribution = rankDistribution,
            successStudentStatistics = successStudentStatistics,
            unsuccessStudentStatistics = unsuccessStudentStatistics,
        )
    }

    private fun generateTaskCategoryDistribution(
        tasks: List<org.rucca.cheese.task.Task>
    ): DistributionDTO {
        val categoryCount = tasks.groupingBy { it.category.name }.eachCount()
        val total = tasks.size

        val items =
            categoryCount.map { (categoryName, count) ->
                DistributionItemDTO(
                    label = categoryName,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionDTO(
            name = "Task Category Distribution",
            type = DistributionDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateTaskStatusDistribution(
        tasks: List<org.rucca.cheese.task.Task>
    ): DistributionDTO {
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
                DistributionItemDTO(
                    label = status,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionDTO(
            name = "Task Status Distribution",
            type = DistributionDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateParticipantStatusDistribution(spaceId: IdType): DistributionDTO {
        val tasks = taskRepository.findBySpaceId(spaceId)
        val taskIds = tasks.mapNotNull { it.id }

        val allMemberships =
            taskIds.flatMap { taskId -> taskMembershipRepository.findAllByTaskId(taskId) }

        val statusCount = allMemberships.groupingBy { it.completionStatus.name }.eachCount()
        val total = allMemberships.size

        val items =
            statusCount.map { (status, count) ->
                DistributionItemDTO(
                    label = status,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionDTO(
            name = "Participant Status Distribution",
            type = DistributionDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateRankDistribution(tasks: List<org.rucca.cheese.task.Task>): DistributionDTO {
        val rankCount = tasks.filter { it.rank != null }.groupingBy { it.rank!! }.eachCount()
        val total = tasks.count { it.rank != null }

        val items =
            rankCount.map { (rank, count) ->
                DistributionItemDTO(
                    label = rank.toString(),
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionDTO(
            name = "Rank Distribution",
            type = DistributionDTO.Type.DISCRETE,
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
            gradeDistribution = gradeDistribution,
            majorDistribution = majorDistribution,
            classNameDistribution = classNameDistribution,
        )
    }

    private fun generateGradeDistribution(students: List<UserIdentityDTO>): DistributionDTO {
        val gradeCount = students.groupingBy { it.grade }.eachCount()
        val total = students.size

        val items =
            gradeCount.map { (grade, count) ->
                DistributionItemDTO(
                    label = grade,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionDTO(
            name = "Grade Distribution",
            type = DistributionDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateMajorDistribution(students: List<UserIdentityDTO>): DistributionDTO {
        val majorCount = students.groupingBy { it.major }.eachCount()
        val total = students.size

        val items =
            majorCount.map { (major, count) ->
                DistributionItemDTO(
                    label = major,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionDTO(
            name = "Major Distribution",
            type = DistributionDTO.Type.DISCRETE,
            items = items,
        )
    }

    private fun generateClassNameDistribution(students: List<UserIdentityDTO>): DistributionDTO {
        val classNameCount = students.groupingBy { it.className }.eachCount()
        val total = students.size

        val items =
            classNameCount.map { (className, count) ->
                DistributionItemDTO(
                    label = className,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionDTO(
            name = "Class Name Distribution",
            type = DistributionDTO.Type.DISCRETE,
            items = items,
        )
    }
}

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
        val participantStatusDistribution = generateParticipantStatusDistribution(tasks)

        // 4. 排名分布
        val rankDistribution = generateRankDistribution(tasks)

        // 5. 成功学生统计信息
        val successStudentStatistics = generateSuccessStudentStatistics(tasks)

        // 6. 失败学生统计信息
        val unsuccessStudentStatistics = generateUnsuccessStudentStatistics(tasks)

        return SpaceTaskStatisticsDTO(
            taskCategoryDistribution = taskCategoryDistribution.toDistributionDTO(),
            taskStatusDistribution = taskStatusDistribution.toDistributionDTO(),
            participantStatusDistribution = participantStatusDistribution.toDistributionDTO(),
            rankDistribution = rankDistribution?.toDistributionDTO(),
            successStudentStatistics = successStudentStatistics,
            unsuccessStudentStatistics = unsuccessStudentStatistics,
        )
    }

    private fun <T> generateDistribution(
        items: List<T>,
        name: String,
        keySelector: (T) -> String,
    ): DistributionOneOfDTO {
        val itemCount = items.groupingBy(keySelector).eachCount()
        val total = items.size

        val distributionItems =
            itemCount.map { (key, count) ->
                DiscreteDistributionItemDTO(
                    label = key,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total * 100) else 0.0,
                )
            }

        return DistributionOneOfDTO(
            name = name,
            type = DistributionOneOfDTO.Type.DISCRETE,
            items = distributionItems,
        )
    }

    private fun generateTaskCategoryDistribution(
        tasks: List<org.rucca.cheese.task.Task>
    ): DistributionOneOfDTO {
        return generateDistribution(tasks, "Task Category Distribution") { it.category.name }
    }

    private fun generateTaskStatusDistribution(
        tasks: List<org.rucca.cheese.task.Task>
    ): DistributionOneOfDTO {
        val transformedTasks =
            tasks.map { task ->
                when (task.approved) {
                    org.rucca.cheese.common.persistent.ApproveType.APPROVED -> "open"
                    org.rucca.cheese.common.persistent.ApproveType.DISAPPROVED -> "unapproved"
                    org.rucca.cheese.common.persistent.ApproveType.NONE -> "unapproved"
                }
            }

        return generateDistribution(transformedTasks, "Task Status Distribution") { it }
    }

    private fun generateParticipantStatusDistribution(
        tasks: List<org.rucca.cheese.task.Task>
    ): DistributionOneOfDTO {
        val taskIds = tasks.mapNotNull { it.id }
        val allMemberships =
            taskIds.flatMap { taskId -> taskMembershipRepository.findAllByTaskId(taskId) }

        return generateDistribution(allMemberships, "Participant Status Distribution") {
            it.completionStatus.name
        }
    }

    private fun generateRankDistribution(
        tasks: List<org.rucca.cheese.task.Task>
    ): DistributionOneOfDTO {
        val rankedTasks = tasks.filter { it.rank != null }

        return generateDistribution(rankedTasks, "Rank Distribution") { it.rank!!.toString() }
    }

    private fun generateSuccessStudentStatistics(
        tasks: List<org.rucca.cheese.task.Task>
    ): StudentRealNameInfoStatisticsDTO {
        val taskIds = tasks.mapNotNull { it.id }

        val successfulMemberships =
            taskIds
                .flatMap { taskId -> taskMembershipRepository.findAllByTaskId(taskId) }
                .filter { it.completionStatus == TaskCompletionStatus.SUCCESS && !it.isTeam }

        val successfulUserIds = successfulMemberships.mapNotNull { it.memberId }

        return generateStudentStatistics(successfulUserIds, "Successful Students")
    }

    private fun generateUnsuccessStudentStatistics(
        tasks: List<org.rucca.cheese.task.Task>
    ): StudentRealNameInfoStatisticsDTO {
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
        return generateDistribution(students, "Grade Distribution") { it.grade }
    }

    private fun generateMajorDistribution(students: List<UserIdentityDTO>): DistributionOneOfDTO {
        return generateDistribution(students, "Major Distribution") { it.major }
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

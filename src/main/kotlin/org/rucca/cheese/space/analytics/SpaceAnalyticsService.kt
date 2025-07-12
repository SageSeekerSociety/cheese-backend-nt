/*
 *  Description: This file implements the SpaceAnalyticsService class.
 *               It provides data insights and analytics for specific Space.
 *
 *  Author(s):
 *      Claude Code
 *
 */

package org.rucca.cheese.space.analytics

import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.space.repositories.SpaceRepository
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.task.TaskSubmitterType
import org.rucca.cheese.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SpaceAnalyticsService(
    private val spaceRepository: SpaceRepository,
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
    private val userRepository: UserRepository,
) {

    /** 获取特定 Space 的活跃度统计 */
    fun getSpaceActivityStatistics(spaceId: IdType): SpaceActivityStatisticsDTO {
        val space =
            spaceRepository.findById(spaceId).orElseThrow { RuntimeException("Space not found") }

        val tasks = taskRepository.findBySpaceId(spaceId)
        val totalTaskCount = tasks.size

        // 统计有正在进行参与的任务数量
        val activeTaskCount =
            tasks.count { task ->
                val memberships =
                    taskMembershipRepository.findAllByTaskIdAndApproved(
                        task.id!!,
                        ApproveType.APPROVED,
                    )
                memberships.any { membership ->
                    membership.completionStatus in
                        listOf(
                            TaskCompletionStatus.NOT_SUBMITTED,
                            TaskCompletionStatus.PENDING_REVIEW,
                            TaskCompletionStatus.REJECTED_RESUBMITTABLE,
                        )
                }
            }

        // 总参与人数（已批准）
        val totalParticipantCount =
            tasks.sumOf { task ->
                taskMembershipRepository.countByTaskIdAndApproved(task.id!!, ApproveType.APPROVED)
            }

        // 按完成状态分布统计任务
        val taskCompletionDistribution =
            tasks
                .flatMap { task ->
                    taskMembershipRepository.findAllByTaskIdAndApproved(
                        task.id!!,
                        ApproveType.APPROVED,
                    )
                }
                .groupBy { it.completionStatus }
                .mapKeys { it.key.name }
                .mapValues { it.value.size }

        // 按分类统计任务分布
        val categoryTaskDistribution =
            tasks.groupBy { it.category.name!! }.mapValues { it.value.size }

        return SpaceActivityStatisticsDTO(
            totalTaskCount = totalTaskCount,
            activeTaskCount = activeTaskCount,
            totalParticipantCount = totalParticipantCount,
            taskCompletionDistribution = taskCompletionDistribution,
            categoryTaskDistribution = categoryTaskDistribution,
        )
    }

    /** 获取特定 Space 的参与情况分析 */
    fun getParticipationAnalysis(spaceId: IdType): ParticipationAnalysisDTO {
        val tasks = taskRepository.findBySpaceId(spaceId)

        // 总参与者和已批准参与者统计
        val totalParticipants =
            tasks.sumOf { task -> taskMembershipRepository.findAllByTaskId(task.id!!).size }
        val approvedParticipants =
            tasks.sumOf { task ->
                taskMembershipRepository.countByTaskIdAndApproved(task.id!!, ApproveType.APPROVED)
            }
        val participationRate =
            if (totalParticipants > 0) {
                (approvedParticipants.toDouble() / totalParticipants * 100)
            } else 0.0

        // 团队 vs 个人参与统计
        val teamTasks = tasks.filter { it.submitterType == TaskSubmitterType.TEAM }
        val userTasks = tasks.filter { it.submitterType == TaskSubmitterType.USER }

        val teamParticipants =
            teamTasks.sumOf { task ->
                taskMembershipRepository.countByTaskIdAndApproved(task.id!!, ApproveType.APPROVED)
            }
        val userParticipants =
            userTasks.sumOf { task ->
                taskMembershipRepository.countByTaskIdAndApproved(task.id!!, ApproveType.APPROVED)
            }

        val totalApproved = teamParticipants + userParticipants
        val teamRatio =
            if (totalApproved > 0) {
                (teamParticipants.toDouble() / totalApproved * 100)
            } else 0.0

        val teamVsUserParticipation =
            ParticipationAnalysisTeamVsUserParticipationDTO(
                teamParticipants = teamParticipants,
                userParticipants = userParticipants,
                teamRatio = teamRatio,
            )

        // 用户活跃度排名
        val userActivityMap =
            mutableMapOf<IdType, Pair<Int, Int>>() // userId -> (participated, successful)

        tasks.forEach { task ->
            val memberships =
                taskMembershipRepository.findAllByTaskIdAndApproved(task.id!!, ApproveType.APPROVED)
            memberships.forEach { membership ->
                val userId = membership.memberId!!
                val current = userActivityMap.getOrDefault(userId, Pair(0, 0))
                val participated = current.first + 1
                val successful =
                    current.second +
                        if (membership.completionStatus == TaskCompletionStatus.SUCCESS) 1 else 0
                userActivityMap[userId] = Pair(participated, successful)
            }
        }

        val userActivityRanking =
            userActivityMap.entries
                .sortedByDescending { it.value.first }
                .take(10) // 取前10名
                .mapNotNull { (userId, stats) ->
                    val user = userRepository.findById(userId.toInt()).orElse(null)
                    user?.let {
                        ParticipationAnalysisUserActivityRankingInnerDTO(
                            userId = userId,
                            userName = user.username!!,
                            participatedTaskCount = stats.first,
                            successfulTaskCount = stats.second,
                        )
                    }
                }

        return ParticipationAnalysisDTO(
            totalParticipants = totalParticipants,
            approvedParticipants = approvedParticipants,
            participationRate = participationRate,
            teamVsUserParticipation = teamVsUserParticipation,
            userActivityRanking = userActivityRanking,
        )
    }

    /** 获取特定 Space 的提交和完成情况分析 */
    fun getSubmissionCompletionAnalysis(spaceId: IdType): SubmissionCompletionAnalysisDTO {
        val tasks = taskRepository.findBySpaceId(spaceId)

        // 总体完成率
        val allMemberships =
            tasks.flatMap { task ->
                taskMembershipRepository.findAllByTaskIdAndApproved(task.id!!, ApproveType.APPROVED)
            }
        val overallCompletionRate =
            if (allMemberships.isNotEmpty()) {
                val successfulCount =
                    allMemberships.count { it.completionStatus == TaskCompletionStatus.SUCCESS }
                (successfulCount.toDouble() / allMemberships.size * 100)
            } else 0.0

        // 各任务完成率
        val taskCompletionRates =
            tasks.map { task ->
                val memberships =
                    taskMembershipRepository.findAllByTaskIdAndApproved(
                        task.id!!,
                        ApproveType.APPROVED,
                    )
                val successfulCount =
                    memberships.count { it.completionStatus == TaskCompletionStatus.SUCCESS }
                val completionRate =
                    if (memberships.isNotEmpty()) {
                        (successfulCount.toDouble() / memberships.size * 100)
                    } else 0.0

                SubmissionCompletionAnalysisTaskCompletionRatesInnerDTO(
                    taskId = task.id!!,
                    taskName = task.name,
                    completionRate = completionRate,
                    totalParticipants = memberships.size,
                    successfulParticipants = successfulCount,
                )
            }

        // 提交状态分布
        val submissionStatusDistribution =
            allMemberships.groupBy { it.completionStatus.name }.mapValues { it.value.size }

        // 提交趋势分析（简化版本）
        val submissionTrendAnalysis =
            SubmissionCompletionAnalysisSubmissionTrendAnalysisDTO(
                averageSubmissionTime = 0.0, // 需要更复杂的计算，先设为0
                resubmissionRate = 0.0, // 需要访问提交历史，先设为0
            )

        return SubmissionCompletionAnalysisDTO(
            overallCompletionRate = overallCompletionRate,
            taskCompletionRates = taskCompletionRates,
            submissionStatusDistribution = submissionStatusDistribution,
            submissionTrendAnalysis = submissionTrendAnalysis,
        )
    }
}

package org.rucca.cheese.space.analytics

import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.task.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SpaceAnalyticsService(private val taskRepository: TaskRepository) {
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
        // Simple mock implementation - return empty data for now
        val emptyDistribution =
            DistributionDTO(
                name = "Empty Distribution",
                type = DistributionDTO.Type.DISCRETE,
                items = listOf(),
            )

        val emptyStats =
            StudentStatisticsDTO(
                totalStudents = 0,
                totalStudentsWithRealName = 0,
                gradeDistribution = emptyDistribution,
                majorDistribution = emptyDistribution,
                classNameDistribution = emptyDistribution,
            )

        return SpaceTaskAnalyticsDTO(
            taskCategoryDistribution = emptyDistribution,
            taskStatusDistribution = emptyDistribution,
            participantStatusDistribution = emptyDistribution,
            rankDistribution = emptyDistribution,
            successStudentStatistics = emptyStats,
            unsuccessStudentStatistics = emptyStats,
        )
    }

    fun getPublishersParticipation(
        spaceId: IdType,
        successBy: String,
    ): List<PublisherParticipationDTO> {
        // Return empty list for now
        return listOf()
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
        // Return empty CSV for now
        return "publisherId,publisherName,participants,completedUsers,taskCount\n"
    }
}

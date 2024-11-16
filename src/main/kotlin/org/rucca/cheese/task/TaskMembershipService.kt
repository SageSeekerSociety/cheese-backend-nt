package org.rucca.cheese.task

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import java.time.LocalDateTime
import org.rucca.cheese.auth.error.PermissionDeniedError
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.model.TaskMembershipDTO
import org.rucca.cheese.model.TaskParticipantRealNameInfoDTO
import org.rucca.cheese.model.TaskParticipantSummaryDTO
import org.rucca.cheese.model.TaskSubmitterTypeDTO.TEAM
import org.rucca.cheese.model.TaskSubmitterTypeDTO.USER
import org.rucca.cheese.model.TeamSummaryDTO
import org.rucca.cheese.space.SpaceUserRankService
import org.rucca.cheese.task.error.*
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.UserService
import org.springframework.stereotype.Service

val DefaultTaskMembershipRealNameInfo =
    TaskMembershipRealNameInfo(
        realName = "",
        studentId = "",
        grade = "",
        major = "",
        className = "",
        email = "",
        phone = "",
        applyReason = "",
        personalAdvantage = "",
        remark = "",
    )

fun TaskMembershipRealNameInfo.convert(): TaskParticipantRealNameInfoDTO {
    return TaskParticipantRealNameInfoDTO(
        realName = realName!!,
        studentId = studentId!!,
        grade = grade!!,
        major = major!!,
        className = className!!,
        email = email!!,
        phone = phone!!,
        applyReason = applyReason!!,
        personalAdvantage = personalAdvantage!!,
        remark = remark!!,
    )
}

fun TaskParticipantRealNameInfoDTO.convert(): TaskMembershipRealNameInfo {
    return TaskMembershipRealNameInfo(
        realName = realName,
        studentId = studentId,
        grade = grade,
        major = major,
        className = className,
        email = email,
        phone = phone,
        applyReason = applyReason,
        personalAdvantage = personalAdvantage,
        remark = remark,
    )
}

@Service
class TaskMembershipService(
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskRepository: TaskRepository,
    private val userService: UserService,
    private val teamService: TeamService,
    private val applicationConfig: ApplicationConfig,
    private val spaceUserRankService: SpaceUserRankService,
    private val entityManager: EntityManager,
) {
    private fun getTask(taskId: IdType): Task {
        return taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
    }

    private fun getTaskMembership(taskId: IdType, memberId: IdType): TaskMembership {
        return taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
            NotTaskParticipantYetError(taskId, memberId)
        }
    }

    fun getTaskMembershipDTO(
        taskId: IdType,
        memberId: IdType,
        queryRealNameInfo: Boolean = false,
    ): TaskMembershipDTO {
        val task = getTask(taskId)
        val membership = getTaskMembership(taskId, memberId)
        val taskParticipantSummaryDto =
            when (task.submitterType!!) {
                TaskSubmitterType.USER -> {
                    val user = userService.getUserDto(memberId)
                    TaskParticipantSummaryDTO(
                        id = user.id,
                        intro = user.intro,
                        name = user.username,
                        avatarId = user.avatarId,
                    )
                }
                TaskSubmitterType.TEAM -> {
                    val team = teamService.getTeamDto(memberId)
                    TaskParticipantSummaryDTO(
                        id = team.id,
                        intro = team.intro,
                        name = team.name,
                        avatarId = team.avatarId,
                    )
                }
            }
        val realNameInfo = if (queryRealNameInfo) membership.realNameInfo!!.convert() else null
        return TaskMembershipDTO(
            id = membership.id!!,
            member = taskParticipantSummaryDto,
            createdAt = membership.createdAt!!.toEpochMilli(),
            updatedAt = membership.updatedAt!!.toEpochMilli(),
            deadline = membership.deadline?.toEpochMilli(),
            approved = membership.approved!!.convert(),
            realNameInfo = realNameInfo,
        )
    }

    fun getTaskMembershipDTOs(
        taskId: IdType,
        approveType: ApproveType?,
        queryRealNameInfo: Boolean = false,
    ): List<TaskMembershipDTO> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(TaskMembership::class.java)
        val root = cq.from(TaskMembership::class.java)
        val predicates = mutableListOf<Predicate>()
        predicates.add(cb.equal(root.get<Task>("task").get<IdType>("id"), taskId))
        if (approveType != null) {
            predicates.add(cb.equal(root.get<ApproveType>("approved"), approveType))
        }
        cq.where(*predicates.toTypedArray())
        val query = entityManager.createQuery(cq)
        val participants = query.resultList
        return participants.map { getTaskMembershipDTO(taskId, it.memberId!!, queryRealNameInfo) }
    }

    fun addTaskParticipant(
        taskId: IdType,
        memberId: IdType,
        deadline: LocalDateTime?,
        approved: ApproveType,
        realNameInfo: TaskParticipantRealNameInfoDTO?,
    ) {
        ensureTaskParticipantNotReachedLimit(taskId)
        val errorOpt = isTaskJoinable(getTask(taskId), memberId)
        if (errorOpt != null) throw errorOpt
        taskMembershipRepository.save(
            TaskMembership(
                task = Task().apply { id = taskId },
                memberId = memberId,
                deadline = deadline,
                approved = approved,
                realNameInfo = realNameInfo?.convert() ?: DefaultTaskMembershipRealNameInfo
            )
        )
        autoRejectParticipantAfterReachesLimit(taskId)
    }

    fun updateTaskMembershipDeadline(
        taskId: IdType,
        memberId: IdType,
        deadline: Long,
    ) {
        val participant = getTaskMembership(taskId, memberId)
        if (participant.approved == ApproveType.APPROVED) {
            participant.deadline = deadline.toLocalDateTime()
        } else {
            throw TaskParticipantNotApprovedError(taskId, memberId)
        }
        taskMembershipRepository.save(participant)
    }

    fun ensureTaskParticipantNotReachedLimit(taskId: IdType) {
        if (applicationConfig.enforceTaskParticipantLimitCheck) {
            val task = getTask(taskId)
            if (task.participantLimit != null) {
                val actual =
                    taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
                if (actual >= task.participantLimit!!)
                    throw TaskParticipantsReachedLimitError(taskId, task.participantLimit!!, actual)
            }
        }
    }

    fun autoRejectParticipantAfterReachesLimit(taskId: IdType) {
        if (applicationConfig.autoRejectParticipantAfterReachesLimit) {
            val task = getTask(taskId)
            if (task.participantLimit != null) {
                val actual =
                    taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
                if (actual >= task.participantLimit!!) {
                    val participants =
                        taskMembershipRepository.findAllByTaskIdAndApproved(
                            taskId,
                            ApproveType.NONE
                        )
                    participants.forEach {
                        it.approved = ApproveType.DISAPPROVED
                        taskMembershipRepository.save(it)
                    }
                }
            }
        }
    }

    fun updateTaskMembershipApproved(taskId: IdType, memberId: IdType, approved: ApproveType) {
        if (approved == ApproveType.APPROVED) ensureTaskParticipantNotReachedLimit(taskId)
        val participant = getTaskMembership(taskId, memberId)
        participant.approved = approved
        taskMembershipRepository.save(participant)
        autoRejectParticipantAfterReachesLimit(taskId)
    }

    fun updateTaskMembershipRealNameInfo(
        taskId: IdType,
        memberId: IdType,
        realNameInfo: TaskParticipantRealNameInfoDTO?,
    ) {
        val participant = getTaskMembership(taskId, memberId)
        participant.realNameInfo = realNameInfo?.convert() ?: DefaultTaskMembershipRealNameInfo
        taskMembershipRepository.save(participant)
    }

    fun removeTaskParticipant(taskId: IdType, memberId: IdType) {
        val participant =
            taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
                NotTaskParticipantYetError(taskId, memberId)
            }
        participant.deletedAt = LocalDateTime.now()
        taskMembershipRepository.save(participant)
    }

    fun isTaskJoinable(task: Task, memberId: IdType): BaseError? {
        // Task is approved
        if (task.approved != ApproveType.APPROVED)
            return PermissionDeniedError("add-participant", "task", task.id, null)

        // Ensure member exists
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                if (!userService.existsUser(memberId)) return NotFoundError("user", memberId)
            TaskSubmitterType.TEAM ->
                if (!teamService.existsTeam(memberId)) return NotFoundError("team", memberId)
        }

        // Has not joined yet
        if (taskMembershipRepository.existsByTaskIdAndMemberId(task.id!!, memberId))
            return AlreadyBeTaskParticipantError(task.id!!, memberId)

        // Have enough rank
        val needToCheckRank =
            applicationConfig.rankCheckEnforced &&
                task.submitterType == TaskSubmitterType.USER &&
                task.space != null &&
                task.space.enableRank!! &&
                task.rank != null
        if (needToCheckRank) {
            val requiredRank = task.rank!! - applicationConfig.rankJump
            val acturalRank = spaceUserRankService.getRank(task.space!!.id!!, memberId)
            if (acturalRank < requiredRank)
                return YourRankIsNotHighEnoughError(acturalRank, requiredRank)
        }

        return null
    }

    fun getJoinability(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType!!) {
            TaskSubmitterType.USER -> return Pair(isTaskJoinable(task, userId) == null, null)
            TaskSubmitterType.TEAM -> {
                val teams =
                    teamService.getTeamsThatUserCanUseToJoinTask(task.id!!, userId).filter {
                        isTaskJoinable(task, it.id) == null
                    }
                return Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    fun getSubmittability(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                return Pair(
                    taskMembershipRepository.existsByTaskIdAndMemberIdAndApproved(
                        task.id!!,
                        userId,
                        ApproveType.APPROVED
                    ),
                    null
                )
            TaskSubmitterType.TEAM -> {
                val teams = teamService.getTeamsThatUserCanUseToSubmitTask(task.id!!, userId)
                return Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    fun getJoined(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                return Pair(
                    taskMembershipRepository.existsByTaskIdAndMemberId(
                        task.id!!,
                        userId,
                    ),
                    null
                )
            TaskSubmitterType.TEAM -> {
                val teams = teamService.getTeamsThatUserJoinedTaskAs(task.id!!, userId)
                return Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    fun getJoinedWithApproveType(
        task: Task,
        userId: IdType,
        approveType: ApproveType
    ): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                return Pair(
                    taskMembershipRepository.existsByTaskIdAndMemberIdAndApproved(
                        task.id!!,
                        userId,
                        approveType,
                    ),
                    null
                )
            TaskSubmitterType.TEAM -> {
                val teams =
                    teamService.getTeamsThatUserJoinedTaskAsWithApprovedType(
                        task.id!!,
                        userId,
                        approveType
                    )
                return Pair(teams.isNotEmpty(), teams)
            }
        }
    }
}

package org.rucca.cheese.user

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rucca.cheese.api.UsersApi
import org.rucca.cheese.auth.annotation.UseNewAuth
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.auth.spring.Auth
import org.rucca.cheese.auth.spring.AuthUser
import org.rucca.cheese.auth.spring.ResourceId
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.model.*
import org.rucca.cheese.team.TeamMembershipService
import org.rucca.cheese.team.models.toEnum
import org.rucca.cheese.user.models.AccessType
import org.rucca.cheese.user.services.UserRealNameService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseNewAuth
class UserController(
    private val userRealNameService: UserRealNameService,
    private val teamMembershipService: TeamMembershipService,
) : UsersApi {
    @Auth
    override suspend fun acceptTeamInvitation(
        @AuthUser userInfo: AuthUserInfo?,
        invitationId: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) {
            teamMembershipService.acceptTeamInvitation(userInfo!!.userId, invitationId)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth
    override suspend fun cancelMyJoinRequest(
        @AuthUser userInfo: AuthUserInfo?,
        requestId: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) {
            teamMembershipService.cancelMyJoinRequest(userInfo!!.userId, requestId)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth
    override suspend fun declineTeamInvitation(
        @AuthUser userInfo: AuthUserInfo?,
        invitationId: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) {
            teamMembershipService.declineTeamInvitation(userInfo!!.userId, invitationId)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth
    override suspend fun listMyInvitations(
        @AuthUser userInfo: AuthUserInfo?,
        status: ApplicationStatusDTO?,
        pageStart: Long?,
        pageSize: Long,
    ): ResponseEntity<ListMyInvitations200ResponseDTO> {
        val (invitations, pageDto) =
            teamMembershipService.listMyInvitations(
                userId = userInfo!!.userId,
                status = status?.toEnum(),
                cursorId = pageStart,
                pageSize = pageSize.toInt().coerceIn(1, 100),
            )
        return ResponseEntity.ok(
            ListMyInvitations200ResponseDTO(
                code = 200,
                data = ListMyInvitations200ResponseDataDTO(invitations, pageDto),
                message = "Success",
            )
        )
    }

    @Auth
    override suspend fun listMyJoinRequests(
        @AuthUser userInfo: AuthUserInfo?,
        status: ApplicationStatusDTO?,
        pageStart: Long?,
        pageSize: Long,
    ): ResponseEntity<ListMyJoinRequests200ResponseDTO> {
        val (requests, pageDto) =
            teamMembershipService.listMyJoinRequests(
                userId = userInfo!!.userId,
                status = status?.toEnum(),
                cursorId = pageStart,
                pageSize = pageSize.toInt().coerceIn(1, 100),
            )
        return ResponseEntity.ok(
            ListMyJoinRequests200ResponseDTO(
                code = 200,
                data = ListMyJoinRequests200ResponseDataDTO(requests, pageDto),
                message = "Success",
            )
        )
    }

    /** Implementation for getUserIdentity endpoint GET /users/{userId}/identity */
    @Auth("user:view:identity")
    override suspend fun getUserIdentity(
        @AuthUser userInfo: AuthUserInfo?,
        @ResourceId userId: Long,
        precise: Boolean,
    ): ResponseEntity<GetUserIdentity200ResponseDTO> {
        // Current user is accessing the real name information
        val currentUserId = userInfo!!.userId

        // Get identity based on precise parameter
        val identity =
            runCatching {
                    if (precise) {
                        // Get precise identity
                        userRealNameService.getUserIdentity(userId).also {
                            // Log this access for precise view since we're returning sensitive data
                            userRealNameService.logAccess(
                                accessorId = currentUserId,
                                targetId = userId,
                                accessReason = "View precise real name information",
                                accessType = AccessType.VIEW,
                            )
                        }
                    } else {
                        // For fuzzy view, we don't log access since we're masking sensitive data
                        userRealNameService.getFuzzyUserIdentity(userId)
                    }
                }
                .getOrElse { if (it is NotFoundError) null else throw it }

        // Return response
        val response =
            GetUserIdentity200ResponseDTO(
                code = 200,
                data =
                    GetUserIdentity200ResponseDataDTO(
                        hasIdentity = identity != null,
                        identity = identity,
                    ),
                message = "Success",
            )

        return ResponseEntity.ok(response)
    }

    /**
     * Implementation for getUserIdentityAccessLogs endpoint GET
     * /users/{userId}/identity/access-logs
     */
    @Auth("user:view:access_log")
    override suspend fun getUserIdentityAccessLogs(
        @AuthUser userInfo: AuthUserInfo?,
        @ResourceId userId: Long,
        pageSize: Long?,
        pageStart: Long?,
    ): ResponseEntity<GetUserIdentityAccessLogs200ResponseDTO> {
        // Get paginated access logs
        val size = pageSize?.toInt() ?: 20

        val (logDTOs, pageDTO) =
            withContext(Dispatchers.IO) {
                userRealNameService.getAccessLogs(userId, size, pageStart)
            }

        // Create response DTO
        val response =
            GetUserIdentityAccessLogs200ResponseDTO(
                code = 200,
                data = GetUserIdentityAccessLogs200ResponseDataDTO(logs = logDTOs, page = pageDTO),
                message = "Success",
            )

        return ResponseEntity.ok(response)
    }

    /** Implementation for putUserIdentity endpoint PUT /users/{userId}/identity */
    @Auth("user:update:identity")
    override suspend fun putUserIdentity(
        @AuthUser userInfo: AuthUserInfo?,
        @ResourceId userId: Long,
        putUserIdentityRequestDTO: PutUserIdentityRequestDTO,
    ): ResponseEntity<PutUserIdentity200ResponseDTO> {
        // Extract data from request
        val realName = putUserIdentityRequestDTO.realName ?: ""
        val studentId = putUserIdentityRequestDTO.studentId ?: ""
        val grade = putUserIdentityRequestDTO.grade ?: ""
        val major = putUserIdentityRequestDTO.major ?: ""
        val className = putUserIdentityRequestDTO.className ?: ""

        // Create or update identity
        val identity =
            withContext(Dispatchers.IO) {
                userRealNameService.createOrUpdateUserIdentity(
                    userId = userId,
                    realName = realName,
                    studentId = studentId,
                    grade = grade,
                    major = major,
                    className = className,
                    shouldEncrypt = true, // Always encrypt sensitive data
                )
            }

        // Return response
        val response =
            PutUserIdentity200ResponseDTO(
                code = 200,
                data = PutUserIdentity200ResponseDataDTO(identity = identity),
                message = "Success",
            )

        return ResponseEntity.ok(response)
    }

    /** Implementation for patchUserIdentity endpoint PATCH /users/{userId}/identity */
    @Auth("user:update:identity")
    override suspend fun patchUserIdentity(
        @AuthUser userInfo: AuthUserInfo?,
        @ResourceId userId: Long,
        putUserIdentityRequestDTO: PutUserIdentityRequestDTO,
    ): ResponseEntity<PutUserIdentity200ResponseDTO> {
        // Check if all fields are provided - if yes, treat like PUT
        val isFullUpdate =
            !putUserIdentityRequestDTO.realName.isNullOrBlank() &&
                !putUserIdentityRequestDTO.studentId.isNullOrBlank() &&
                !putUserIdentityRequestDTO.grade.isNullOrBlank() &&
                !putUserIdentityRequestDTO.major.isNullOrBlank() &&
                !putUserIdentityRequestDTO.className.isNullOrBlank()

        if (isFullUpdate) {
            // If all fields are provided, behave like PUT
            return putUserIdentity(userInfo, userId, putUserIdentityRequestDTO)
        }

        // Check if the user has existing identity data
        try {
            val existingIdentity =
                withContext(Dispatchers.IO) { userRealNameService.getUserIdentity(userId) }

            // Update only the fields that are provided
            val updatedRealName = putUserIdentityRequestDTO.realName ?: existingIdentity.realName
            val updatedStudentId = putUserIdentityRequestDTO.studentId ?: existingIdentity.studentId
            val updatedGrade = putUserIdentityRequestDTO.grade ?: existingIdentity.grade
            val updatedMajor = putUserIdentityRequestDTO.major ?: existingIdentity.major
            val updatedClassName = putUserIdentityRequestDTO.className ?: existingIdentity.className

            // Update with the merged data
            val identity =
                withContext(Dispatchers.IO) {
                    userRealNameService.createOrUpdateUserIdentity(
                        userId = userId,
                        realName = updatedRealName,
                        studentId = updatedStudentId,
                        grade = updatedGrade,
                        major = updatedMajor,
                        className = updatedClassName,
                        shouldEncrypt = true, // Always encrypt sensitive data
                    )
                }

            // Return response
            val response =
                PutUserIdentity200ResponseDTO(
                    code = 200,
                    data = PutUserIdentity200ResponseDataDTO(identity = identity),
                    message = "Success",
                )

            return ResponseEntity.ok(response)
        } catch (e: NotFoundError) {
            // If user doesn't have existing identity, they must provide all fields
            throw BadRequestError(
                "User has no existing identity data. Please provide all fields for first-time setup."
            )
        }
    }
}

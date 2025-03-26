package org.rucca.cheese.discussion

import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.DiscussionReactionSummaryDTO
import org.rucca.cheese.model.ReactionTypeDTO
import org.rucca.cheese.user.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Manages reactions for discussable entities */
@Service
class DiscussionReactionService(
    private val reactionRepository: DiscussionReactionRepository,
    private val discussionRepository: DiscussionRepository,
    private val userRepository: UserRepository,
    private val reactionTypeRepository: ReactionTypeRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    /**
     * Toggles a reaction (adds or removes) for a discussion
     *
     * @return updated count for the specified reaction type
     */
    @Transactional
    fun toggleReaction(
        discussionId: IdType,
        userId: IdType,
        reactionTypeId: IdType,
    ): DiscussionReactionSummaryDTO {
        val reacted =
            reactionRepository.existsByDiscussionIdAndUserIdAndReactionTypeId(
                discussionId,
                userId,
                reactionTypeId,
            )

        if (reacted) {
            reactionRepository.deleteReaction(discussionId, userId, reactionTypeId)
            //            applicationEventPublisher.publishEvent(
            //                ReactionRemovedEvent(discussion.id!!, userId, reactionTypeId)
            //            )
        } else {
            val reactionType = reactionTypeRepository.getReferenceById(reactionTypeId)

            DiscussionReaction(
                    discussion = discussionRepository.getReferenceById(discussionId),
                    user = userRepository.getReferenceById(userId.toInt()),
                    reactionType = reactionType,
                )
                .let { reactionRepository.save(it) }
            //            applicationEventPublisher.publishEvent(
            //                ReactionAddedEvent(discussion.id!!, userId, reactionTypeId)
            //            )
        }

        val currentCount =
            reactionRepository.countReactionByDiscussionIdAndReactionTypeId(
                discussionId,
                reactionTypeId,
            )
        val reactionType = reactionTypeRepository.getReferenceById(reactionTypeId)

        return DiscussionReactionSummaryDTO(
            reactionType = mapToReactionTypeDTO(reactionType),
            count = currentCount,
            hasReacted = !reacted,
        )
    }

    /** Returns reaction statistics for a discussion with user's reaction status */
    fun getReactionSummaries(
        discussionId: IdType,
        currentUserId: IdType? = null,
    ): List<DiscussionReactionSummaryDTO> {
        val counts = reactionRepository.countReactionsByDiscussionId(discussionId)

        return counts.map { typeCount ->
            val hasReacted =
                currentUserId?.let {
                    reactionRepository.existsByDiscussionIdAndUserIdAndReactionTypeId(
                        discussionId,
                        it,
                        typeCount.reactionType.id!!,
                    )
                } ?: false

            DiscussionReactionSummaryDTO(
                reactionType = mapToReactionTypeDTO(typeCount.reactionType),
                count = typeCount.count,
                hasReacted = hasReacted,
            )
        }
    }

    /** Get all available reaction types */
    fun getAllReactionTypes(): List<ReactionTypeDTO> {
        return reactionTypeRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc().map {
            mapToReactionTypeDTO(it)
        }
    }

    /** Maps ReactionType entity to DTO */
    private fun mapToReactionTypeDTO(reactionType: ReactionType): ReactionTypeDTO {
        return ReactionTypeDTO(
            id = reactionType.id!!,
            code = reactionType.code!!,
            name = reactionType.name!!,
            description = reactionType.description,
            displayOrder = reactionType.displayOrder,
            isActive = reactionType.isActive,
        )
    }
}

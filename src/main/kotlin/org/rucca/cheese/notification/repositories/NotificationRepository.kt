package org.rucca.cheese.notification.repositories

import java.time.Instant
import java.util.Optional
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository // Your custom repo
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.notification.models.Notification
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository :
    CursorPagingRepository<Notification, IdType> { // Extend your cursor repo

    // --- Standard Queries (using receiverId) ---
    fun findByIdAndReceiverId(id: IdType, receiverId: IdType): Optional<Notification>

    // Note: findAllByReceiverId is usually handled by the cursor repo now, but keep if needed
    // elsewhere
    // fun findAllByReceiverId(receiverId: Long): List<Notification> // Use cursor pagination
    // instead

    fun countByReceiverIdAndReadIsFalse(receiverId: IdType): Long // Changed return type to Long

    // --- Aggregation Queries ---
    @Query(
        """
        SELECT n FROM Notification n
        WHERE n.receiverId = :receiverId
          AND n.aggregationKey = :aggregationKey
          AND n.isAggregatable = true
          AND n.aggregateUntil > :now
          AND n.finalized = false
    """
    )
    fun findActiveAggregation(
        @Param("recipientId") recipientId: IdType,
        @Param("aggregationKey") aggregationKey: String,
        @Param("now") now: Instant,
    ): Notification?

    @Query(
        """
        SELECT n FROM Notification n
        WHERE n.isAggregatable = true
          AND n.finalized = false
          AND n.aggregateUntil <= :now
        ORDER BY n.aggregateUntil ASC
    """
    ) // Order to process oldest first
    fun findExpiredAggregations(@Param("now") now: Instant): List<Notification>

    // --- Modification Queries ---
    @Modifying
    @Query(
        "UPDATE Notification n SET n.read = true WHERE n.id = :id AND n.receiverId = :receiverId"
    )
    fun markAsReadByIdAndReceiverId(
        @Param("id") id: IdType,
        @Param("receiverId") receiverId: IdType,
    ): Int

    @Modifying
    @Query(
        "UPDATE Notification n SET n.read = true WHERE n.receiverId = :receiverId AND n.read = false"
    )
    fun markAllAsReadForReceiver(@Param("receiverId") receiverId: IdType): Int

    // Delete methods (if using soft delete via BaseEntity, ensure its mechanism is used)
    // Or define hard delete if needed:
    // @Modifying
    // @Query("DELETE Notification n WHERE n.id = :id AND n.receiverId = :receiverId")
    // fun deleteByIdAndReceiverId(@Param("id") id: Long, @Param("receiverId") receiverId: Long):
    // Int
}

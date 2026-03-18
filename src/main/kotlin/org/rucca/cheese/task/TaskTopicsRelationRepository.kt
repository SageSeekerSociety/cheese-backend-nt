package org.rucca.cheese.task

import java.util.*
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.topic.Topic
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TaskTopicsRelationRepository : JpaRepository<TaskTopicsRelation, IdType> {
    fun findByTaskIdAndTopicId(taskId: IdType, topicId: IdType): Optional<TaskTopicsRelation>

    fun findAllByTaskId(taskId: IdType): List<TaskTopicsRelation>

    /**
     * Retrieve the most popular Topics in a given Space.
     *
     * Logic:
     * 1. Join TaskTopicsRelation with Task.
     * 2. Filter tasks to those whose `space.id` equals the provided `spaceId`.
     * 3. Exclude deleted tasks (`t.deletedAt IS NULL`).
     * 4. Group by Topic and order by the number of relations in descending order.
     *
     * @param spaceId the id of the Space to search.
     * @param pageable pagination and limit controls (used to return the top N topics).
     * @return A list of `Topic` ordered by popularity (most referenced by non-deleted tasks first).
     */
    @Query(
        """
        SELECT r.topic
        FROM TaskTopicsRelation r
        JOIN r.task t
        WHERE t.space.id = :spaceId
        AND t.deletedAt IS NULL
        GROUP BY r.topic
        ORDER BY COUNT(r) DESC
    """
    )
    fun findHotTopicsBySpaceId(@Param("spaceId") spaceId: IdType, pageable: Pageable): List<Topic>

    /**
     * Retrieves the most popular topics within a specific space.
     *
     * This query performs the following steps:
     * 1. Joins the `TaskTopicsRelation` table with the `Task` table.
     * 2. Filters tasks to include only those where the `space.id` matches the provided `spaceId`.
     * 3. Excludes tasks that have been marked as deleted (`t.deletedAt IS NULL`).
     * 4. Groups the results by `Topic` and orders them by the count of relations in descending
     *    order.
     *
     * @param spaceId The ID of the space for which to retrieve the most popular topics.
     * @param pageable Pagination information to limit the number of results returned.
     * @return A list of `Topic` objects ordered by their popularity (most referenced first).
     */
    @Query(
        """
        SELECT tp
        FROM Topic tp
        WHERE LOWER(tp.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        AND EXISTS (
            SELECT 1 
            FROM TaskTopicsRelation r
            JOIN r.task t
            WHERE r.topic = tp
            AND t.space.id = :spaceId
            AND t.deletedAt IS NULL
        )
        ORDER BY LENGTH(tp.name) ASC, tp.name ASC
    """
    )
    fun searchTopicsInSpace(
        @Param("spaceId") spaceId: IdType,
        @Param("keyword") keyword: String,
        pageable: Pageable,
    ): List<Topic>
}

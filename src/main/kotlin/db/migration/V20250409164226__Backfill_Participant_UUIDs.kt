package db.migration

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import kotlin.system.measureTimeMillis
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.slf4j.LoggerFactory

/**
 * Flyway Java migration implemented in Kotlin to backfill participant UUIDs. IMPORTANT: Accessing
 * Spring beans might require specific configuration depending on how Flyway is integrated and when
 * it runs relative to Spring context initialization.
 */
class V20250409164226__Backfill_Participant_UUIDs : BaseJavaMigration() {

    companion object {
        private val logger =
            LoggerFactory.getLogger(V20250409164226__Backfill_Participant_UUIDs::class.java)
        private const val BATCH_SIZE = 100 // Adjust batch size as needed
        private const val TASK_MEMBERSHIP_TABLE = "task_membership"
        private const val TASK_MEMBERSHIP_PK_COL = "id" // Primary key of task_membership
        private const val TASK_MEMBERSHIP_UUID_COL = "participant_uuid"

        private const val TEAM_MEMBER_TABLE =
            "task_membership_team_members" // The table for the @ElementCollection

        // --- Choose ONE way to identify rows in TEAM_MEMBER_TABLE for update ---
        // Option A: If it has its own simple primary key (e.g., 'id')
        private const val TEAM_MEMBER_PK_COL = "id" // Use the actual PK column name

        // Option B: If identified by owning entity ID + member ID (assuming unique combo)
        private const val TEAM_MEMBER_FK_COL = "task_membership_id" // FK back to task_membership
        private const val TEAM_MEMBER_USER_ID_COL = "member_id" // The actual user ID column

        // --- End of Identification Options ---
        private const val TEAM_MEMBER_UUID_COL = "participant_member_uuid"
    }

    override fun migrate(context: Context) {
        logger.info("Starting TaskMembership UUID backfill JDBC migration...")
        val connection = context.connection // Get the JDBC connection from Flyway
        var originalAutoCommitState: Boolean? = null // Store original state just in case

        val totalTime = measureTimeMillis {
            try {
                originalAutoCommitState = connection.autoCommit
                // Disable auto-commit for batching and transaction control within this script
                if (originalAutoCommitState == true) {
                    connection.autoCommit = false
                    logger.info("AutoCommit disabled for migration script execution.")
                }

                // 1. Backfill task_membership table
                val updatedParents = backfillTaskMembership(connection)

                // 2. Backfill team members element collection table
                val updatedChildren = backfillTeamMembers(connection)

                // Commit the work done within this script
                connection.commit()
                logger.info("Internal migration work committed successfully.")
                logger.info(
                    "Backfill summary: TaskMemberships updated = {}, TeamMemberInfos updated = {}",
                    updatedParents,
                    updatedChildren,
                )
            } catch (e: Exception) {
                logger.error(
                    "Error during UUID backfill migration, attempting internal rollback.",
                    e,
                )
                try {
                    // Rollback the changes made *within this script* if autoCommit was false
                    if (
                        originalAutoCommitState == true
                    ) { // Only rollback if we disabled autoCommit
                        connection.rollback()
                        logger.warn("Internal migration work rolled back successfully.")
                    } else {
                        logger.warn("AutoCommit was originally false, skipping internal rollback.")
                    }
                } catch (rollbackEx: SQLException) {
                    logger.error("FATAL: Failed to rollback internal migration work.", rollbackEx)
                    // Log original exception as well if possible
                    e.addSuppressed(rollbackEx)
                }
                // Re-throw the original exception to make Flyway aware of the failure
                throw RuntimeException("UUID backfill migration failed.", e)
            } finally {
                // --- CRITICAL CHANGE: DO NOT restore autoCommit here ---
                // Flyway's TransactionalExecutionTemplate will handle the final
                // commit/rollback of the overall migration transaction and restore
                // the connection state. Explicitly restoring it here causes conflicts.
                // logger.info("Not restoring autoCommit state; letting Flyway manage it.")

                // Still close Statements and ResultSets if they were opened in this scope
                // (Though the helper functions handle their own resources)
            }
        }
        logger.info("Total JDBC backfill time: {} ms", totalTime)
    }

    /** Backfills the main task_membership table. Manages its own resources. */
    private fun backfillTaskMembership(connection: Connection): Long {
        logger.info("Starting backfill for {} table...", TASK_MEMBERSHIP_TABLE)
        var totalUpdated: Long = 0
        val selectSql =
            "SELECT $TASK_MEMBERSHIP_PK_COL FROM $TASK_MEMBERSHIP_TABLE WHERE $TASK_MEMBERSHIP_UUID_COL IS NULL FOR UPDATE" // Add FOR UPDATE for safety if needed
        val updateSql =
            "UPDATE $TASK_MEMBERSHIP_TABLE SET $TASK_MEMBERSHIP_UUID_COL = ? WHERE $TASK_MEMBERSHIP_PK_COL = ?"

        // Use try-with-resources for automatic resource management
        try {
            connection.prepareStatement(selectSql).use { selectStmt ->
                connection.prepareStatement(updateSql).use { updateStmt ->
                    selectStmt.executeQuery().use { rs
                        -> // Also use try-with-resources for ResultSet
                        var batchCount = 0
                        while (rs.next()) {
                            val id = rs.getLong(TASK_MEMBERSHIP_PK_COL)
                            val newUuid = UUID.randomUUID()

                            try {
                                updateStmt.setObject(1, newUuid)
                            } catch (e: SQLException) {
                                logger.warn(
                                    "JDBC driver might not support setObject for UUID. Falling back to String.",
                                    e,
                                )
                                updateStmt.setString(1, newUuid.toString())
                            }
                            updateStmt.setLong(2, id)

                            updateStmt.addBatch()
                            batchCount++
                            totalUpdated++

                            if (batchCount >= BATCH_SIZE) {
                                executeBatch(updateStmt, TASK_MEMBERSHIP_TABLE, batchCount)
                                batchCount = 0
                            }
                        }

                        if (batchCount > 0) {
                            executeBatch(
                                updateStmt,
                                TASK_MEMBERSHIP_TABLE,
                                batchCount,
                                isFinal = true,
                            )
                        }
                    } // ResultSet closed
                } // Update PreparedStatement closed
            } // Select PreparedStatement closed
            logger.info(
                "Backfill for {} table completed. Rows updated: {}",
                TASK_MEMBERSHIP_TABLE,
                totalUpdated,
            )
        } catch (e: SQLException) {
            logger.error("SQL Error during backfill for {}: {}", TASK_MEMBERSHIP_TABLE, e.message)
            throw e // Re-throw to be caught by the main try-catch
        }
        return totalUpdated
    }

    /** Backfills the team members element collection table. Manages its own resources. */
    private fun backfillTeamMembers(connection: Connection): Long {
        logger.info("Starting backfill for {} table...", TEAM_MEMBER_TABLE)
        var totalUpdated: Long = 0

        // Using FK + memberId identification (Adjust SQL if using a different PK)
        val selectSql =
            "SELECT $TEAM_MEMBER_FK_COL, $TEAM_MEMBER_USER_ID_COL FROM $TEAM_MEMBER_TABLE WHERE $TEAM_MEMBER_UUID_COL IS NULL FOR UPDATE" // Add FOR UPDATE if needed
        val updateSql =
            "UPDATE $TEAM_MEMBER_TABLE SET $TEAM_MEMBER_UUID_COL = ? WHERE $TEAM_MEMBER_FK_COL = ? AND $TEAM_MEMBER_USER_ID_COL = ?"

        try {
            connection.prepareStatement(selectSql).use { selectStmt ->
                connection.prepareStatement(updateSql).use { updateStmt ->
                    selectStmt.executeQuery().use { rs -> // Use try-with-resources
                        var batchCount = 0
                        while (rs.next()) {
                            val fkId = rs.getLong(TEAM_MEMBER_FK_COL)
                            val memberUserId = rs.getLong(TEAM_MEMBER_USER_ID_COL)
                            val newUuid = UUID.randomUUID()

                            try {
                                updateStmt.setObject(1, newUuid)
                            } catch (e: SQLException) {
                                logger.warn(
                                    "JDBC driver might not support setObject for UUID. Falling back to String.",
                                    e,
                                )
                                updateStmt.setString(1, newUuid.toString())
                            }
                            updateStmt.setLong(2, fkId)
                            updateStmt.setLong(3, memberUserId)

                            updateStmt.addBatch()
                            batchCount++
                            totalUpdated++

                            if (batchCount >= BATCH_SIZE) {
                                executeBatch(updateStmt, TEAM_MEMBER_TABLE, batchCount)
                                batchCount = 0
                            }
                        }

                        if (batchCount > 0) {
                            executeBatch(updateStmt, TEAM_MEMBER_TABLE, batchCount, isFinal = true)
                        }
                    } // ResultSet closed
                } // Update PreparedStatement closed
            } // Select PreparedStatement closed
            logger.info(
                "Backfill for {} table completed. Rows updated: {}",
                TEAM_MEMBER_TABLE,
                totalUpdated,
            )
        } catch (e: SQLException) {
            logger.error("SQL Error during backfill for {}: {}", TEAM_MEMBER_TABLE, e.message)
            throw e // Re-throw
        }
        return totalUpdated
    }

    /** Helper function to execute and log batch updates. */
    private fun executeBatch(
        stmt: PreparedStatement,
        tableName: String,
        batchCount: Int,
        isFinal: Boolean = false,
    ) {
        try {
            val prefix = if (isFinal) "final " else ""
            logger.info(
                "Executing {}batch update for {} ({} rows)...",
                prefix,
                tableName,
                batchCount,
            )
            stmt.executeBatch()
            logger.info("Batch executed.")
            // Removed optional intermediate commit here - commit happens once in migrate()
        } catch (e: SQLException) {
            logger.error("Error executing batch update for table {}: {}", tableName, e.message)
            throw e // Re-throw to ensure transaction failure
        }
    }
}

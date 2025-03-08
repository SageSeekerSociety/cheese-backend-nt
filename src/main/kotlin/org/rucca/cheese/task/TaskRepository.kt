package org.rucca.cheese.task

import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.IdType

interface TaskRepository : CursorPagingRepository<Task, IdType>

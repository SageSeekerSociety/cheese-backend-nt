package org.rucca.cheese.task

import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.repository.CursorPagingRepository

interface TaskRepository : CursorPagingRepository<Task, IdType>

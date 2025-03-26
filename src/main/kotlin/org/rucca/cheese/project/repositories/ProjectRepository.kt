package org.rucca.cheese.project.repositories

import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.project.models.Project

interface ProjectRepository : CursorPagingRepository<Project, IdType>

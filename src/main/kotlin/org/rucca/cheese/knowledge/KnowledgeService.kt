package org.rucca.cheese.knowledge

import org.rucca.cheese.project.KnowledgeRepository
import org.springframework.stereotype.Service

@Service
class KnowledgeService(
    private val knowledgeRepository: KnowledgeRepository,
    private val knowledgeLabelRepository: KnowledgeLabelRepository,
) {
    // TODO: Implement
}

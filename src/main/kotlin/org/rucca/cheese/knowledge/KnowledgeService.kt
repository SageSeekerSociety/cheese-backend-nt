package org.rucca.cheese.knowledge

import org.rucca.cheese.project.KnowledgeRepository
import org.springframework.stereotype.Service

@Service
@Transactional
class KnowledgeService(
    private val knowledgeRepository: KnowledgeRepository,
    private val knowledgeLabelRepository: KnowledgeLabelRepository,
) {
    fun deleteknowledge(kid:IdType)//override？
    {
        val knowledge=KnowledgeRepository.findById(kid).orElseThrow { NotFoundError("knowledge", kid) }
        knowledge.deletedAt=LocalDateTime.now()//?deleteat 
        knowledgeRepository.save(knowledge)
    }
    fun getKnowledge(kid:IdType):Knowledge
    {
        return knowledgeRepository.findById(kid).orElseThrow { NotFoundError("knowledge", kid) }
    }
    fun createKnowledge(
        name :string,
        type :KnowledgePostRequestDTO.Type, //按照knowledgepostrequestDTO 填？
        content :String,
        description:string,
        projectIds :List<Long>?=null,
        labels:List<string>?=null,
    ):IdType
    {
        val knowledge=knowledgeRepository.save
        (knowledge(
            name=name,
            type=type,
            content=content
            description=description
            projectIds=projectIds
            labels=labels
        )//?knowledge 是dto那里的嘛？id等其他信息怎么填
        )
        return knowledge.id!!//id自动生成且！！为判断非空，空了就不行
    }
    fun getKnowledgeDTO(
        knowledgeId:IdType,
    ): KnowledgeDTO
    {
        return getKnowledge(knowledgeId).toKnowledgeDTO()//??
    }
    fun knowledge.toKnowledgeDTO():KnowledgeDTO///???
    {
        return KnowledgeDTO(
            id = this.id!!,
            name=this.name!!,
            type=KnowledgeDTO.Type.forValue(this.type!!.name)，//??看Knowledgedto，
            content= this.content!!,
            description =this.description,
            materialId=this.material?.id,//????material     @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var material: Material? = null,//knowledge   val materialId: kotlin.Long? = null, 
            projectIds=this.projectIds.toList(),
            creator=this.createdBy?.toUserDTO(),//?????User？？？
            createdAt =this.createdAt!!.toEpochMilli(),
            updatedAt = this.updatedAt!!.toEpochMilli()
        )
    }
    fun updateKnowledgename(kid:IdType,name:string)
    {
       val kd=getKnowledge(kid)
       kd.name=name
       knowledgeRepository.save(kd)
    }
    fun updateKnowledgetype(kid:IdType,type:knowledgePostRequestDTO.Type)
    {
        val kd=getKnowledge(kid)
        kd.type=type
        knowledgeRepository.save(kd)
    }
    fun updateKnowledgecontent(kid:IdType,content:string)
    {
        val kd=getKnowledge(kid)
        kd.content=content
        knowledgeRepository.save(kd)
    }
    fun updateKnowledgedescription(kid:IdType,description:string)
    {
        val kd=getKnowledge(kid)
        kd.description=description
        knowledgeRepository.save(kd)
    }
    fun updateKnowledgeprojectIds(kid:IdType,projectIds:List<Long>)
    {
        val kd=getKnowledge(kid)
        kd.projectIds=projectIds
        knowledgeRepository.save(kd)
    }
}

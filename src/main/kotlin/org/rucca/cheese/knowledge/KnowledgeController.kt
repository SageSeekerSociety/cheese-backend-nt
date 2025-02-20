package org.rucca.cheese.knowledge

import org.rucca.cheese.api.KnowledgeApi
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.model.KnowledgeGet200ResponseDTO
import org.rucca.cheese.model.KnowledgePost200ResponseDTO
import org.rucca.cheese.model.KnowledgePostRequestDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class KnowledgeController(private val knowledgeService: KnowledgeService) : KnowledgeApi {
   @PostConstruct
   fun initialize()
   {
        authenticationService.ownerIds.register("knowledge",KnowledgeService:getKnowledgeOwner)//?功能
        authenticationService.customAuthLogics.register("is-Knowledge-admin")
        {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any?>?,
            _: IdGetter?,
            _: Any? ->
            KnowledgeService.isKnowledgeAdmin(
                resourceId ?: throw IllegalArgumentException("resourceId is null"),
                userId,
            )
        }
   }
    @Guard("create", "knowledge")
    override fun knowledgePost(
        knowledgePostRequestDTO: KnowledgePostRequestDTO
    ): ResponseEntity<KnowledgePost200ResponseDTO> {
        val Kid=KnowledgeService.createKnowledge(
        name = knowledgePostRequestDTO.name,
        type = knowledgePostRequestDTO.type, // 传递类型?按照knowledgePostRequestDTO填，space里好像不是这样
        content = knowledgePostRequestDTO.content,
        description = knowledgePostRequestDTO.description,
        projectIds = knowledgePostRequestDTO.projectIds,
        labels = knowledgePostRequestDTO.labels
    )
        val KnowledgeDTO = KnowledgeService.getKnowledgeDto(kid)
        return ResponseEntity.ok(KnowledgeGet200ResponseDTO(200,KnowledgeGet200ResponseDTO(KnowledgeDTO)),"OK")//?
    }

    @Guard("query", "knowledge")
    override fun knowledgeGet(
        @ResourceId projectIds: List<Long>?,
        type: String?,
        labels: List<String>?,
        query: String?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<KnowledgeGet200ResponseDTO> {
        val kdto=knowledgeService.getKnowledgeDTO(projectIds);//val queryOptions = SpaceQueryOptions(queryMyRank = queryMyRank)?
        return ResponseEntity.ok(KnowledgeGet200ResponseDTO(200,KnowledgeGet200ResponseDTO(kdto),"Ok"))
    }


    @Guard("delete","knowledge")
    override fun knowledgedelete(@ResourceId kid:long):ResponseEntity<DeleteKnowledge200DTO>
    {
        KnowledgeService.deleteknowledge(kid)
        return ResponseEntity.ok(DeleteKnowledge200DTO(200,"OK"))
    }



    @Guard("remove-admin", "knowledge")
    override fun deleteknowledgeAdmin(
        @ResourceId knowledgeId: Long,
        userId: Long,
    ): ResponseEntity<DeleteKnowledge200DTO> {
        KnowledgeService.removeAdmin(knowledgeId, userId)
        return ResponseEntity.ok(DeleteKnowledge200DTO(200, "OK"))
    }



    @Guard("update", "knowledge")
    @PutMapping("/knowledge/{id}")
    fun knowledgeUp(
        @PathVariable id:Long,//resoucesid???
      knowledgePostRequestDTO: KnowledgePostRequestDTO
    ): ResponseEntity<KnowledgePost200ResponseDTO> {

        val DTOProperties = knowledgePostRequestDTO::class.memberProperties
        DTOProperties.forEach{ property ->
            val value =property.get(knowledgePostRequestDTO)
            if( value !=null)
            {
                val updateMethod = knowledgeService::class.declaredFunctions
                    .firstOrNull{it.name =="updateKnowledge${property.name.capitalize()}"}
                updateMethod?.call(knowledgeService,id,value) 
            }
        }
    val UpdateKnowledgeDTO= knowledgeService.getKnowledgeDTO(id)//postSpace?
    return ResponseEntity.ok(
        KnowledgePost200ResponseDTO(200, KnowledgePost200ResponseDataDTO(UpdateKnowledgeDTO), "OK")
        )
    }
}

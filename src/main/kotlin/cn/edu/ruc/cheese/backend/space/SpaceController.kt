package cn.edu.ruc.cheese.backend.space

import cn.edu.ruc.cheese.backend.api.SpaceApi
import cn.edu.ruc.cheese.backend.model.DeleteSpace200Response
import cn.edu.ruc.cheese.backend.model.GetSpace200Response
import cn.edu.ruc.cheese.backend.model.PatchSpaceRequest
import cn.edu.ruc.cheese.backend.model.PostSpaceRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SpaceController : SpaceApi {
    override fun deleteSpace(space: Int): ResponseEntity<DeleteSpace200Response> {
        return super.deleteSpace(space)
    }

    override fun getSpace(space: Int): ResponseEntity<GetSpace200Response> {
        return super.getSpace(space)
    }

    override fun patchSpace(space: Int, patchSpaceRequest: PatchSpaceRequest): ResponseEntity<GetSpace200Response> {
        return super.patchSpace(space, patchSpaceRequest)
    }

    override fun postSpace(postSpaceRequest: PostSpaceRequest): ResponseEntity<GetSpace200Response> {
        return super.postSpace(postSpaceRequest)
    }
}

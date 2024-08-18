package cn.edu.ruc.cheese.backend.space

import cn.edu.ruc.cheese.backend.api.SpaceApi
import cn.edu.ruc.cheese.backend.model.InlineResponse200
import cn.edu.ruc.cheese.backend.model.InlineResponse2001
import cn.edu.ruc.cheese.backend.model.SpaceBody
import cn.edu.ruc.cheese.backend.model.SpaceBody1
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SpaceController : SpaceApi {
    override fun deleteSpace(space: Int?): ResponseEntity<InlineResponse2001> {
        return super.deleteSpace(space)
    }

    override fun getSpace(space: Int?): ResponseEntity<InlineResponse200> {
        return super.getSpace(space)
    }

    override fun patchSpace(space: Int?, body: SpaceBody1?): ResponseEntity<InlineResponse200> {
        return super.patchSpace(space, body)
    }

    override fun postSpace(body: SpaceBody?): ResponseEntity<InlineResponse200> {
        return super.postSpace(body)
    }
}

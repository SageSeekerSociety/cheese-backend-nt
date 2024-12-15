package org.rucca.cheese.project

import org.rucca.cheese.model.PostProjectRequestDTO

interface ProjectService {


    fun createProject(postProjectRequestDTO: PostProjectRequestDTO) {

    }

    fun enumerateSpaces(parentId: Long?, leaderId: Long?, memberId: Long?, status: Int?, pageSize: Int?, pageStart: Long?) {

    }

}

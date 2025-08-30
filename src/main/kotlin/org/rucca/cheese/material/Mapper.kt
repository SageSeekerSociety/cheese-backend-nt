package org.rucca.cheese.material

import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.model.AttachmentMetaDTO
import org.rucca.cheese.model.MaterialDTO

/** Extracts AttachmentMetaDTO from material meta map */
@Suppress("UNCHECKED_CAST")
private fun extractAttachmentMeta(meta: Map<String, Any>): AttachmentMetaDTO {
    return AttachmentMetaDTO(
        propertySize = (meta["size"] as? Number)?.toLong(),
        name = meta["name"] as? String,
        mime = meta["mime"] as? String,
        hash = meta["hash"] as? String,
        height = (meta["height"] as? Number)?.toInt(),
        width = (meta["width"] as? Number)?.toInt(),
        thumbnail = meta["thumbnail"] as? String,
        duration = (meta["duration"] as? Number)?.toLong(),
    )
}

/** Converts a Material entity to a MaterialDTO */
fun Material.toMaterialDTO(): MaterialDTO {
    return MaterialDTO(
        id = this.id?.toLong(),
        type =
            when (this.type) {
                MaterialType.IMAGE -> MaterialDTO.Type.image
                MaterialType.AUDIO -> MaterialDTO.Type.audio
                MaterialType.VIDEO -> MaterialDTO.Type.video
                MaterialType.FILE -> MaterialDTO.Type.file
                null -> null
            },
        uploader = null,
        createdAt = this.createdAt?.toEpochMilli(),
        expires = this.expires?.toLong(),
        downloadCount = this.downloadCount?.toLong(),
        url = this.url,
        meta = this.meta?.let { extractAttachmentMeta(it) },
    )
}

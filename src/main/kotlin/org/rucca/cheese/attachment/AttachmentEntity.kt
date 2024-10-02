/*
 * DO NOT MODIFY THIS FILE
 *
 * Since cheese-backend is migrating from NestJS to Spring Boot,
 * some modules are still implemented in https://github.com/SageSeekerSociety/cheese-backend
 *
 * However, some tables are shared between the two implementations.
 * This file is one of them.
 *
 * The original project has an independent database schema, so if you modify this file,
 * the original project may not work properly.
 *
 */

/*
 * For the same reason, we recommend you take these tables as read-only,
 * that means, do not do any write operations (INSERT, UPDATE, DELETE) to these tables.
 *
 * We expect these tables to be maintained by the original project,
 * until we decide to fully migrate to Spring Boot.
 *
 */

package org.rucca.cheese.attachment

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO,
    FILE;

    override fun toString(): String {
        return name.lowercase()
    }

    companion object {
        fun fromString(value: String): AttachmentType {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                    ?: throw IllegalArgumentException("无效的 AttachmentType: $value")
        }
    }
}

@Converter(autoApply = true)
class AttachmentTypeConverter : AttributeConverter<AttachmentType, String> {
    override fun convertToDatabaseColumn(attribute: AttachmentType?): String? {
        return attribute?.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): AttachmentType? {
        return dbData?.let { AttachmentType.fromString(it) }
    }
}

@Entity
@Table(name = "attachment")
open class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attachment_id_gen")
    @SequenceGenerator(name = "attachment_id_gen", sequenceName = "attachment_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "url", nullable = false, length = Integer.MAX_VALUE) open var url: String? = null

    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "meta", nullable = false) open var meta: MutableMap<String, Any>? = null

    @Column(name = "type", nullable = false)
    @Convert(converter = AttachmentTypeConverter::class)
    open var type: AttachmentType? = null
}

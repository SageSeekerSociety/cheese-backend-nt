package org.rucca.cheese.common.helper

import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import org.springframework.stereotype.Component

/**
 * 类型转换工具类 提供两种类型转换方法：
 * 1. convert(): 接收KType，用于反射场景
 * 2. convertTo<T>(): 使用泛型，用于类型安全的DSL场景
 */
object TypeConverter {
    /** 将值转换为指定的Kotlin类型 */
    @Suppress("UNCHECKED_CAST")
    fun convert(value: Any?, type: KType): Any? {
        if (value == null) return null

        val classifier = type.classifier as? KClass<*> ?: return value

        return when {
            // 值已经是目标类型
            classifier.java.isAssignableFrom(value::class.java) -> value

            // 基本类型转换
            classifier == String::class -> value.toString()
            classifier == Int::class || classifier == java.lang.Integer::class ->
                (value as? Number)?.toInt() ?: value.toString().toIntOrNull() ?: value
            classifier == Long::class || classifier == java.lang.Long::class ->
                (value as? Number)?.toLong() ?: value.toString().toLongOrNull() ?: value
            classifier == Double::class || classifier == java.lang.Double::class ->
                (value as? Number)?.toDouble() ?: value.toString().toDoubleOrNull() ?: value
            classifier == Float::class || classifier == java.lang.Float::class ->
                (value as? Number)?.toFloat() ?: value.toString().toFloatOrNull() ?: value
            classifier == Boolean::class || classifier == java.lang.Boolean::class ->
                when (value) {
                    is Boolean -> value
                    is String -> value.lowercase(Locale.getDefault()) == "true" || value == "1"
                    is Number -> value.toInt() != 0
                    else -> value
                }

            // 集合类型转换
            classifier == List::class && value is Collection<*> -> value.toList()
            classifier == Set::class && value is Collection<*> -> value.toSet()
            classifier == Map::class && value is Map<*, *> -> value

            // 默认返回原值，由调用者决定处理方式
            else -> value
        }
    }

    /** 将值转换为指定的实体类型 (用于DSL中的类型转换) */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> convertTo(value: Any?): T {
        if (value == null) {
            // 如果T是可空类型，返回null，否则抛出异常
            if (null is T) return null as T
            throw IllegalArgumentException(
                "Cannot convert null to non-nullable ${T::class.simpleName}"
            )
        }

        return when {
            // 值已经是目标类型
            value is T -> value

            // 基本类型转换
            T::class == String::class -> value.toString() as T
            T::class == Int::class && value is Number -> value.toInt() as T
            T::class == Int::class && value is String ->
                value.toIntOrNull() as? T
                    ?: throw IllegalArgumentException("Cannot convert '$value' to Int")
            T::class == Long::class && value is Number -> value.toLong() as T
            T::class == Long::class && value is String ->
                value.toLongOrNull() as? T
                    ?: throw IllegalArgumentException("Cannot convert '$value' to Long")
            T::class == Double::class && value is Number -> value.toDouble() as T
            T::class == Double::class && value is String ->
                value.toDoubleOrNull() as? T
                    ?: throw IllegalArgumentException("Cannot convert '$value' to Double")
            T::class == Float::class && value is Number -> value.toFloat() as T
            T::class == Float::class && value is String ->
                value.toFloatOrNull() as? T
                    ?: throw IllegalArgumentException("Cannot convert '$value' to Float")
            T::class == Boolean::class ->
                when (value) {
                    is Boolean -> value as T
                    is String ->
                        (value.lowercase(Locale.getDefault()) == "true" || value == "1") as T
                    is Number -> (value.toInt() != 0) as T
                    else ->
                        throw IllegalArgumentException(
                            "Cannot convert ${value::class.simpleName} to Boolean"
                        )
                }

            // 集合类型转换
            // 注意：这些转换不考虑元素类型，可能需要进一步处理
            T::class == List::class && value is Collection<*> -> value.toList() as T
            T::class == Set::class && value is Collection<*> -> value.toSet() as T
            T::class == Map::class && value is Map<*, *> -> value as T

            // 无法转换时抛出异常
            else ->
                throw IllegalArgumentException(
                    "Cannot convert ${value::class.simpleName} to ${T::class.simpleName}"
                )
        }
    }
}

/** 字段处理器函数类型 */
typealias FieldHandler<T> = (T, Any) -> Unit

/** 补丁处理器DSL */
class PatchHandlerDsl<T : Any> {
    val handlers = mutableMapOf<String, FieldHandler<T>>()

    /** 为指定字段定义类型安全的处理函数 */
    inline fun <reified V> handle(field: String, noinline handler: (entity: T, value: V) -> Unit) {
        handlers[field] = { entity, anyValue ->
            try {
                // 使用统一的类型转换工具
                val convertedValue = TypeConverter.convertTo<V>(anyValue)
                handler(entity, convertedValue)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Error processing field '$field': ${e.message}", e)
            }
        }
    }
}

/** 通用实体补丁服务，用于处理PATCH请求 只负责实体对象的属性更新，不负责数据库持久化 */
@Component
class EntityPatcher {
    /**
     * 使用DSL风格定义补丁操作
     *
     * @param entity 要更新的实体
     * @param patchData 补丁数据（非空字段）
     * @param configure 自定义处理器配置
     * @return 更新后的实体（注意：该实体未持久化到数据库，需要调用方保存）
     */
    fun <T : Any> patch(
        entity: T,
        patchData: Any,
        configure: PatchHandlerDsl<T>.() -> Unit = {},
    ): T {
        // 将patchData转换为Map
        val patchMap = convertToMap(patchData)

        // 配置自定义处理器
        val dsl = PatchHandlerDsl<T>().apply(configure)

        // 应用补丁并返回更新后的实体（由调用方负责保存）
        return applyPatch(entity, patchMap, dsl.handlers)
    }

    /** 应用补丁到实体 */
    private fun <T : Any> applyPatch(
        entity: T,
        patchMap: Map<String, Any?>,
        handlers: Map<String, FieldHandler<T>>,
    ): T {
        val entityClass = entity::class

        // 获取所有可修改属性
        val properties = entityClass.memberProperties.filterIsInstance<KMutableProperty1<T, Any?>>()

        // 处理每个非空字段
        patchMap.forEach { (fieldName, value) ->
            if (value != null) {
                // 优先使用自定义处理器
                if (handlers.containsKey(fieldName)) {
                    handlers[fieldName]?.invoke(entity, value)
                } else {
                    // 查找匹配的属性
                    val property = properties.find { it.name == fieldName }
                    if (property != null) {
                        property.isAccessible = true

                        // 转换值类型并设置
                        val convertedValue = TypeConverter.convert(value, property.returnType)
                        property.set(entity, convertedValue)
                    }
                }
            }
        }

        return entity
    }

    /** 将任何对象转换为Map，保留非空值 */
    private fun convertToMap(obj: Any): Map<String, Any?> {
        return when (obj) {
            is Map<*, *> -> obj.filterKeys { it is String }.mapKeys { it.key as String }
            else -> {
                obj::class
                    .memberProperties
                    .associate { prop -> prop.name to prop.getter.call(obj) }
                    .filterValues { it != null }
            }
        }
    }
}

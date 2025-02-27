package org.rucca.cheese.llm.model

/** AI资源类型枚举 */
enum class AIResourceType(val baseSeuCost: Int, val dynamicCost: Boolean) {
    LIGHTWEIGHT(1, false),
    STANDARD(1, true),
    ADVANCED(0, true);

    override fun toString(): String {
        return name.lowercase()
    }
}

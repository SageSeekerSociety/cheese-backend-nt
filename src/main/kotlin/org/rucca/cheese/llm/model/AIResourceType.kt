package org.rucca.cheese.llm.model

/** AI资源类型枚举 */
enum class AIResourceType(val description: String, val baseSeuCost: Int, val dynamicCost: Boolean) {
    LIGHTWEIGHT("轻量级交互", 1, false),
    STANDARD("标准级处理", 2, true),
    ADVANCED("深度级运算", 0, true),
}

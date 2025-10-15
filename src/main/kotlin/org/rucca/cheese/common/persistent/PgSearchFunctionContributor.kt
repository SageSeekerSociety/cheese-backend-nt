package org.rucca.cheese.common.persistent

import org.hibernate.boot.model.FunctionContributions
import org.hibernate.boot.model.FunctionContributor
import org.hibernate.type.StandardBasicTypes

class PgSearchFunctionContributor : FunctionContributor {
    override fun contributeFunctions(fc: FunctionContributions) {
        val types = fc.typeConfiguration.basicTypeRegistry
        val BOOL = types.resolve(StandardBasicTypes.BOOLEAN)
        val DOUBLE = types.resolve(StandardBasicTypes.DOUBLE)
        val STRING = types.resolve(StandardBasicTypes.STRING)
        val OBJ = types.resolve(StandardBasicTypes.OBJECT_TYPE)

        val reg = fc.functionRegistry

        // ① 把 @@@ 中缀注册成模板函数（返回 boolean）
        // 用法：pg_search(<lhs-expr>, <rhs-expr>) -> 渲染为 (<lhs> @@@ <rhs>)
        reg.registerPattern("pg_search", "(?1 @@@ ?2)", BOOL)

        // ② 常用 Query Builder 函数（用在 @@@ 的 RHS）
        reg.registerPattern("pdb_match", "paradedb.match(?1, ?2)", OBJ)
        reg.registerPattern("pdb_parse", "paradedb.parse(?1)", OBJ)
        reg.registerPattern("pdb_phrase_prefix", "paradedb.phrase_prefix(?1, ?2)", OBJ)
        reg.registerPattern("pdb_term", "paradedb.term(?1, ?2)", OBJ)
        reg.registerPattern(
            "pdb_boolean",
            "paradedb.boolean(must=>?1, should=>?2, must_not=>?3)",
            OBJ,
        )

        // ③ 评分与高亮（可投影/排序）
        reg.registerPattern("pdb_score", "paradedb.score(?1)", DOUBLE)
        reg.registerPattern(
            "pdb_snippet",
            "paradedb.snippet(?1, start_tag=>?2, end_tag=>?3)",
            STRING,
        )

        reg.registerPattern("text_array2", "ARRAY[?1, ?2]", OBJ)
        reg.registerPattern("text_array3", "ARRAY[?1, ?2, ?3]", OBJ)
        reg.registerPattern("to_jsonb", "?1::jsonb", OBJ)
    }
}

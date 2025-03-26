package org.rucca.cheese.project.models

enum class ProjectMemberRole(private val level: Int) {
    LEADER(2), // 项目负责人
    MEMBER(1), // 普通团队成员
    EXTERNAL(0); // 外部协作者

    fun isAtLeast(role: ProjectMemberRole) = this.level >= role.level
}

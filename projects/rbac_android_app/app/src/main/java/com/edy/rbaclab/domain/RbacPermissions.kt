package com.edy.rbaclab.domain

import com.edy.rbaclab.data.Project
import com.edy.rbaclab.data.User
import com.edy.rbaclab.data.UserRole

object RbacPermissions {
    fun canCreateProject(user: User): Boolean =
        user.role == UserRole.ADMIN || user.role == UserRole.MANAGER

    fun canReadUsers(user: User): Boolean =
        user.role == UserRole.ADMIN || user.role == UserRole.MANAGER

    fun canCreateUser(user: User): Boolean = user.role == UserRole.ADMIN

    fun canManageProject(user: User, project: Project): Boolean =
        user.role == UserRole.ADMIN || project.ownerId == user.id

    fun roleCapabilities(role: UserRole): List<String> = when (role) {
        UserRole.ADMIN -> listOf(
            "查看所有项目",
            "创建和管理任意项目",
            "创建用户并指定角色",
            "查看全部用户与项目成员",
        )

        UserRole.MANAGER -> listOf(
            "创建项目",
            "管理自己创建的项目",
            "查看用户列表",
            "查看自己拥有或加入的项目",
        )

        UserRole.MEMBER -> listOf(
            "查看自己加入的项目",
            "查看可见项目的成员",
            "无权创建项目或用户",
            "无权修改项目或添加成员",
        )
    }
}

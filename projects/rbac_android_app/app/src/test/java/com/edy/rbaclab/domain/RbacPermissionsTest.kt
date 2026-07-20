package com.edy.rbaclab.domain

import com.edy.rbaclab.data.Project
import com.edy.rbaclab.data.ProjectStatus
import com.edy.rbaclab.data.User
import com.edy.rbaclab.data.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RbacPermissionsTest {
    private val admin = user(id = 1, role = UserRole.ADMIN)
    private val manager = user(id = 2, role = UserRole.MANAGER)
    private val member = user(id = 3, role = UserRole.MEMBER)
    private val managerOwnedProject = project(ownerId = manager.id)

    @Test
    fun `admin receives every management capability`() {
        assertTrue(RbacPermissions.canCreateProject(admin))
        assertTrue(RbacPermissions.canReadUsers(admin))
        assertTrue(RbacPermissions.canCreateUser(admin))
        assertTrue(RbacPermissions.canManageProject(admin, managerOwnedProject))
    }

    @Test
    fun `manager can create projects and manage only owned project`() {
        assertTrue(RbacPermissions.canCreateProject(manager))
        assertTrue(RbacPermissions.canReadUsers(manager))
        assertFalse(RbacPermissions.canCreateUser(manager))
        assertTrue(RbacPermissions.canManageProject(manager, managerOwnedProject))
        assertFalse(RbacPermissions.canManageProject(manager, project(ownerId = admin.id)))
    }

    @Test
    fun `member receives read only capability`() {
        assertFalse(RbacPermissions.canCreateProject(member))
        assertFalse(RbacPermissions.canReadUsers(member))
        assertFalse(RbacPermissions.canCreateUser(member))
        assertFalse(RbacPermissions.canManageProject(member, managerOwnedProject))
    }

    private fun user(id: Int, role: UserRole): User = User(
        id = id,
        username = "user$id",
        role = role,
        isActive = true,
    )

    private fun project(ownerId: Int): Project = Project(
        id = 10,
        name = "RBAC Lab",
        description = null,
        status = ProjectStatus.ACTIVE,
        ownerId = ownerId,
    )
}

package com.edy.rbaclab.data

enum class UserRole(val wireValue: String, val label: String) {
    ADMIN("admin", "管理员"),
    MANAGER("manager", "经理"),
    MEMBER("member", "成员");

    companion object {
        fun fromWire(value: String): UserRole = entries.firstOrNull {
            it.wireValue == value
        } ?: MEMBER
    }
}

enum class ProjectStatus(val wireValue: String, val label: String) {
    PLANNING("planning", "规划中"),
    ACTIVE("active", "进行中"),
    ARCHIVED("archived", "已归档");

    companion object {
        fun fromWire(value: String): ProjectStatus = entries.firstOrNull {
            it.wireValue == value
        } ?: PLANNING
    }
}

data class User(
    val id: Int,
    val username: String,
    val role: UserRole,
    val isActive: Boolean,
)

data class Project(
    val id: Int,
    val name: String,
    val description: String?,
    val status: ProjectStatus,
    val ownerId: Int,
)

data class Token(
    val accessToken: String,
    val tokenType: String,
)

data class ApiLogEntry(
    val method: String,
    val path: String,
    val statusCode: Int,
    val detail: String,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
}

class ApiException(
    val statusCode: Int,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

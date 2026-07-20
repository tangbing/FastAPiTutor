package com.edy.rbaclab.data

class RbacRepository(
    private val store: SessionStore,
    onCall: (ApiLogEntry) -> Unit,
) {
    private val api = RbacApiClient(store.baseUrl, onCall)

    val savedToken: String? get() = store.accessToken
    val savedBaseUrl: String get() = store.baseUrl

    fun updateBaseUrl(value: String) {
        val normalized = RbacApiClient.normalizeBaseUrl(value)
        store.baseUrl = normalized
        api.baseUrl = normalized
    }

    suspend fun health(): String = api.health()

    suspend fun bootstrap(username: String, password: String): User =
        api.bootstrap(username, password)

    suspend fun login(username: String, password: String): User {
        val token = api.login(username, password)
        store.accessToken = token.accessToken
        return try {
            api.me(token.accessToken)
        } catch (error: Throwable) {
            store.accessToken = null
            throw error
        }
    }

    suspend fun restoreSession(): User? {
        val token = store.accessToken ?: return null
        return try {
            api.me(token)
        } catch (error: ApiException) {
            if (error.statusCode == 401) store.accessToken = null
            null
        }
    }

    fun logout() {
        store.accessToken = null
    }

    suspend fun me(): User = api.me(requireToken())

    suspend fun listUsers(): List<User> = api.listUsers(requireToken())

    suspend fun createUser(username: String, password: String, role: UserRole): User =
        api.createUser(requireToken(), username, password, role)

    suspend fun listProjects(): List<Project> = api.listProjects(requireToken())

    suspend fun getProject(projectId: Int): Project = api.getProject(requireToken(), projectId)

    suspend fun createProject(
        name: String,
        description: String?,
        status: ProjectStatus,
    ): Project = api.createProject(requireToken(), name, description, status)

    suspend fun updateProject(
        projectId: Int,
        name: String,
        description: String?,
        status: ProjectStatus,
    ): Project = api.updateProject(requireToken(), projectId, name, description, status)

    suspend fun listMembers(projectId: Int): List<User> =
        api.listMembers(requireToken(), projectId)

    suspend fun addMember(projectId: Int, userId: Int): User =
        api.addMember(requireToken(), projectId, userId)

    private fun requireToken(): String = store.accessToken
        ?: throw ApiException(401, "请先登录")
}

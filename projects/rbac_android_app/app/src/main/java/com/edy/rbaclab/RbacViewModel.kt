package com.edy.rbaclab

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.edy.rbaclab.data.ApiException
import com.edy.rbaclab.data.ApiLogEntry
import com.edy.rbaclab.data.Project
import com.edy.rbaclab.data.ProjectStatus
import com.edy.rbaclab.data.RbacRepository
import com.edy.rbaclab.data.SessionStore
import com.edy.rbaclab.data.User
import com.edy.rbaclab.data.UserRole
import com.edy.rbaclab.domain.RbacPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    LOGIN,
    PROJECTS,
    USERS,
    PROFILE,
    PROJECT_DETAIL,
}

enum class HealthState {
    CHECKING,
    ONLINE,
    OFFLINE,
}

data class RbacUiState(
    val baseUrl: String,
    val healthState: HealthState = HealthState.CHECKING,
    val isRestoringSession: Boolean = true,
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val currentScreen: AppScreen = AppScreen.LOGIN,
    val projects: List<Project> = emptyList(),
    val users: List<User> = emptyList(),
    val selectedProject: Project? = null,
    val members: List<User> = emptyList(),
    val notice: String? = null,
    val noticeCode: Int? = null,
    val apiLog: List<ApiLogEntry> = emptyList(),
)

class RbacViewModel(context: Context) : ViewModel() {
    private val store = SessionStore(context.applicationContext)
    private val _uiState = MutableStateFlow(RbacUiState(baseUrl = store.baseUrl))
    val uiState: StateFlow<RbacUiState> = _uiState.asStateFlow()

    private val repository = RbacRepository(store) { entry ->
        _uiState.update { state ->
            state.copy(apiLog = (listOf(entry) + state.apiLog).take(30))
        }
    }

    init {
        checkHealth()
        restoreSession()
    }

    fun updateBaseUrl(value: String) {
        repository.updateBaseUrl(value)
        _uiState.update {
            it.copy(baseUrl = repository.savedBaseUrl, healthState = HealthState.CHECKING)
        }
        checkHealth()
    }

    fun checkHealth() {
        _uiState.update { it.copy(healthState = HealthState.CHECKING) }
        viewModelScope.launch {
            val online = runCatching { repository.health() == "ok" }.getOrDefault(false)
            _uiState.update {
                it.copy(healthState = if (online) HealthState.ONLINE else HealthState.OFFLINE)
            }
        }
    }

    fun bootstrap(username: String, password: String, onSuccess: () -> Unit = {}) {
        launchAction {
            repository.bootstrap(username.trim(), password)
            _uiState.update {
                it.copy(
                    notice = "管理员初始化成功，请使用新账号登录",
                    noticeCode = 201,
                )
            }
            onSuccess()
        }
    }

    fun login(username: String, password: String) {
        launchAction {
            val user = repository.login(username.trim(), password)
            val projects = repository.listProjects()
            val users = if (RbacPermissions.canReadUsers(user)) repository.listUsers() else emptyList()
            _uiState.update {
                it.copy(
                    currentUser = user,
                    currentScreen = AppScreen.PROJECTS,
                    projects = projects,
                    users = users,
                    notice = "欢迎回来，${user.username}",
                    noticeCode = 200,
                )
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.update {
            it.copy(
                currentUser = null,
                currentScreen = AppScreen.LOGIN,
                projects = emptyList(),
                users = emptyList(),
                selectedProject = null,
                members = emptyList(),
                notice = "已安全退出",
                noticeCode = null,
            )
        }
    }

    fun navigate(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, notice = null, noticeCode = null) }
        when (screen) {
            AppScreen.PROJECTS -> refreshProjects()
            AppScreen.USERS -> refreshUsers()
            AppScreen.PROFILE -> refreshMe()
            else -> Unit
        }
    }

    fun refreshProjects() {
        launchAction(showLoading = false) {
            _uiState.update { it.copy(projects = repository.listProjects()) }
        }
    }

    fun refreshUsers() {
        val user = _uiState.value.currentUser ?: return
        if (!RbacPermissions.canReadUsers(user)) {
            _uiState.update {
                it.copy(notice = "当前角色无权读取用户列表", noticeCode = 403)
            }
            return
        }
        launchAction(showLoading = false) {
            _uiState.update { it.copy(users = repository.listUsers()) }
        }
    }

    fun refreshMe() {
        launchAction(showLoading = false) {
            _uiState.update { it.copy(currentUser = repository.me()) }
        }
    }

    fun openProject(projectId: Int) {
        _uiState.update { it.copy(currentScreen = AppScreen.PROJECT_DETAIL) }
        launchAction {
            val project = repository.getProject(projectId)
            val members = repository.listMembers(projectId)
            _uiState.update { it.copy(selectedProject = project, members = members) }
        }
    }

    fun refreshSelectedProject() {
        val id = _uiState.value.selectedProject?.id ?: return
        openProject(id)
    }

    fun createProject(
        name: String,
        description: String?,
        status: ProjectStatus,
        onSuccess: () -> Unit = {},
    ) {
        launchAction {
            val project = repository.createProject(name.trim(), description.cleanNullable(), status)
            val projects = repository.listProjects()
            _uiState.update {
                it.copy(
                    projects = projects,
                    notice = "项目“${project.name}”创建成功",
                    noticeCode = 201,
                )
            }
            onSuccess()
        }
    }

    fun updateProject(
        name: String,
        description: String?,
        status: ProjectStatus,
        onSuccess: () -> Unit = {},
    ) {
        val projectId = _uiState.value.selectedProject?.id ?: return
        launchAction {
            val updated = repository.updateProject(
                projectId = projectId,
                name = name.trim(),
                description = description.cleanNullable(),
                status = status,
            )
            _uiState.update { state ->
                state.copy(
                    selectedProject = updated,
                    projects = state.projects.map { if (it.id == updated.id) updated else it },
                    notice = "项目已更新",
                    noticeCode = 200,
                )
            }
            onSuccess()
        }
    }

    fun addMember(userId: Int, onSuccess: () -> Unit = {}) {
        val projectId = _uiState.value.selectedProject?.id ?: return
        launchAction {
            val addedUser = repository.addMember(projectId, userId)
            val members = repository.listMembers(projectId)
            _uiState.update {
                it.copy(
                    members = members,
                    notice = "${addedUser.username} 已加入项目",
                    noticeCode = 201,
                )
            }
            onSuccess()
        }
    }

    fun createUser(
        username: String,
        password: String,
        role: UserRole,
        onSuccess: () -> Unit = {},
    ) {
        launchAction {
            val created = repository.createUser(username.trim(), password, role)
            val users = repository.listUsers()
            _uiState.update {
                it.copy(
                    users = users,
                    notice = "用户 ${created.username} 创建成功",
                    noticeCode = 201,
                )
            }
            onSuccess()
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null, noticeCode = null) }
    }

    fun clearApiLog() {
        _uiState.update { it.copy(apiLog = emptyList()) }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val user = repository.restoreSession()
            if (user == null) {
                _uiState.update { it.copy(isRestoringSession = false) }
                return@launch
            }
            runCatching {
                val projects = repository.listProjects()
                val users = if (RbacPermissions.canReadUsers(user)) repository.listUsers() else emptyList()
                _uiState.update {
                    it.copy(
                        isRestoringSession = false,
                        currentUser = user,
                        currentScreen = AppScreen.PROJECTS,
                        projects = projects,
                        users = users,
                    )
                }
            }.onFailure {
                repository.logout()
                _uiState.update { state ->
                    state.copy(isRestoringSession = false, currentScreen = AppScreen.LOGIN)
                }
            }
        }
    }

    private fun launchAction(
        showLoading: Boolean = true,
        action: suspend () -> Unit,
    ) {
        if (showLoading) _uiState.update { it.copy(isLoading = true, notice = null, noticeCode = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (error: ApiException) {
                handleApiError(error)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(notice = error.message ?: "发生未知错误", noticeCode = 0)
                }
            } finally {
                if (showLoading) _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleApiError(error: ApiException) {
        if (error.statusCode == 401) {
            repository.logout()
            _uiState.update {
                it.copy(
                    currentUser = null,
                    currentScreen = AppScreen.LOGIN,
                    projects = emptyList(),
                    users = emptyList(),
                    selectedProject = null,
                    members = emptyList(),
                    notice = "登录已失效：${error.message}",
                    noticeCode = 401,
                )
            }
            return
        }

        val prefix = when (error.statusCode) {
            0 -> "网络不可用"
            403 -> "权限不足"
            404 -> "资源不可见或不存在"
            409 -> "操作冲突"
            422 -> "输入校验失败"
            else -> "请求失败"
        }
        _uiState.update {
            it.copy(notice = "$prefix：${error.message}", noticeCode = error.statusCode)
        }
    }

    private fun String?.cleanNullable(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RbacViewModel(context) as T
            }
    }
}

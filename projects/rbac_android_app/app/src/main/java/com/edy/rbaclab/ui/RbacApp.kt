package com.edy.rbaclab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edy.rbaclab.AppScreen
import com.edy.rbaclab.RbacUiState
import com.edy.rbaclab.RbacViewModel
import com.edy.rbaclab.domain.RbacPermissions
import com.edy.rbaclab.ui.components.LoadingOverlay
import com.edy.rbaclab.ui.components.NoticeBanner
import com.edy.rbaclab.ui.components.RoleBadge
import com.edy.rbaclab.ui.screens.LoginScreen
import com.edy.rbaclab.ui.screens.ProfileScreen
import com.edy.rbaclab.ui.screens.ProjectDetailScreen
import com.edy.rbaclab.ui.screens.ProjectsScreen
import com.edy.rbaclab.ui.screens.UsersScreen

@Composable
fun RbacApp(state: RbacUiState, viewModel: RbacViewModel) {
    if (state.isRestoringSession) {
        SplashScreen()
        return
    }

    val currentUser = state.currentUser
    if (currentUser == null) {
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.fillMaxSize()) {
                    NoticeBanner(
                        message = state.notice,
                        statusCode = state.noticeCode,
                        onDismiss = viewModel::clearNotice,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    LoginScreen(
                        baseUrl = state.baseUrl,
                        healthState = state.healthState,
                        isLoading = state.isLoading,
                        onBaseUrlChange = viewModel::updateBaseUrl,
                        onCheckHealth = viewModel::checkHealth,
                        onLogin = viewModel::login,
                        onBootstrap = viewModel::bootstrap,
                        modifier = Modifier.weight(1f),
                    )
                }
                LoadingOverlay(state.isLoading)
            }
        }
        return
    }

    BackHandler(enabled = state.currentScreen == AppScreen.PROJECT_DETAIL) {
        viewModel.navigate(AppScreen.PROJECTS)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                username = currentUser.username,
                roleLabel = currentUser.role.label,
                showBack = state.currentScreen == AppScreen.PROJECT_DETAIL,
                onBack = { viewModel.navigate(AppScreen.PROJECTS) },
            )
        },
        bottomBar = {
            if (state.currentScreen != AppScreen.PROJECT_DETAIL) {
                AppNavigation(
                    selected = state.currentScreen,
                    canReadUsers = RbacPermissions.canReadUsers(currentUser),
                    onNavigate = viewModel::navigate,
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                NoticeBanner(
                    message = state.notice,
                    statusCode = state.noticeCode,
                    onDismiss = viewModel::clearNotice,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                when (state.currentScreen) {
                    AppScreen.PROJECTS -> ProjectsScreen(
                        currentUser = currentUser,
                        projects = state.projects,
                        onRefresh = viewModel::refreshProjects,
                        onOpenProject = viewModel::openProject,
                        onCreateProject = viewModel::createProject,
                        modifier = Modifier.fillMaxSize(),
                    )

                    AppScreen.USERS -> UsersScreen(
                        currentUser = currentUser,
                        users = state.users,
                        onRefresh = viewModel::refreshUsers,
                        onCreateUser = viewModel::createUser,
                        modifier = Modifier.fillMaxSize(),
                    )

                    AppScreen.PROFILE -> ProfileScreen(
                        currentUser = currentUser,
                        baseUrl = state.baseUrl,
                        apiLog = state.apiLog,
                        onRefresh = viewModel::refreshMe,
                        onClearLog = viewModel::clearApiLog,
                        onLogout = viewModel::logout,
                        modifier = Modifier.fillMaxSize(),
                    )

                    AppScreen.PROJECT_DETAIL -> ProjectDetailScreen(
                        project = state.selectedProject,
                        currentUser = currentUser,
                        members = state.members,
                        allUsers = state.users,
                        onRefresh = viewModel::refreshSelectedProject,
                        onUpdate = viewModel::updateProject,
                        onAddMember = viewModel::addMember,
                        modifier = Modifier.fillMaxSize(),
                    )

                    AppScreen.LOGIN -> Unit
                }
            }
            LoadingOverlay(state.isLoading)
        }
    }
}

@Composable
private fun AppTopBar(
    username: String,
    roleLabel: String,
    showBack: Boolean,
    onBack: () -> Unit,
) {
    Surface(shadowElevation = 1.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showBack) {
                TextButton(onClick = onBack) { Text("← 返回") }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        "R",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("RBAC Lab", fontWeight = FontWeight.Black)
                Text(
                    "$username · $roleLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppNavigation(
    selected: AppScreen,
    canReadUsers: Boolean,
    onNavigate: (AppScreen) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == AppScreen.PROJECTS,
            onClick = { onNavigate(AppScreen.PROJECTS) },
            icon = { Text("P", fontWeight = FontWeight.Black) },
            label = { Text("项目") },
        )
        if (canReadUsers) {
            NavigationBarItem(
                selected = selected == AppScreen.USERS,
                onClick = { onNavigate(AppScreen.USERS) },
                icon = { Text("U", fontWeight = FontWeight.Black) },
                label = { Text("团队") },
            )
        }
        NavigationBarItem(
            selected = selected == AppScreen.PROFILE,
            onClick = { onNavigate(AppScreen.PROFILE) },
            icon = { Text("ME", fontWeight = FontWeight.Black) },
            label = { Text("权限") },
        )
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator()
            Text("正在恢复 RBAC 会话", fontWeight = FontWeight.Bold)
        }
    }
}

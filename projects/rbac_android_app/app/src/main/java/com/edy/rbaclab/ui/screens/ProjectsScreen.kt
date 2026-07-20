package com.edy.rbaclab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edy.rbaclab.data.Project
import com.edy.rbaclab.data.ProjectStatus
import com.edy.rbaclab.data.User
import com.edy.rbaclab.domain.RbacPermissions
import com.edy.rbaclab.ui.components.EmptyState
import com.edy.rbaclab.ui.components.PermissionLockedCard
import com.edy.rbaclab.ui.components.ProjectStatusBadge
import com.edy.rbaclab.ui.components.ScreenHeader

@Composable
fun ProjectsScreen(
    currentUser: User,
    projects: List<Project>,
    onRefresh: () -> Unit,
    onOpenProject: (Int) -> Unit,
    onCreateProject: (String, String?, ProjectStatus, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedStatus by rememberSaveable { mutableStateOf<ProjectStatus?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    val canCreate = RbacPermissions.canCreateProject(currentUser)
    val visibleProjects = projects.filter { selectedStatus == null || it.status == selectedStatus }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "GET /projects",
                title = "项目工作台",
                subtitle = if (currentUser.role.name == "ADMIN") {
                    "管理员视角：当前返回全部项目。"
                } else {
                    "当前仅展示你拥有或已经加入的项目。"
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                actions = {
                    OutlinedButton(onClick = onRefresh) { Text("刷新") }
                },
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    label = { Text("全部 ${projects.size}") },
                )
                ProjectStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status.label) },
                    )
                }
            }
        }

        item {
            if (canCreate) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("创建一个新项目", fontWeight = FontWeight.Bold)
                            Text(
                                "管理员和经理可调用 POST /projects",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(onClick = { showCreateDialog = true }) { Text("新建") }
                    }
                }
            } else {
                PermissionLockedCard(
                    title = "创建项目已锁定",
                    description = "成员角色没有 POST /projects 权限，但仍可查看自己加入的项目。",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        if (visibleProjects.isEmpty()) {
            item {
                EmptyState(
                    title = "没有符合条件的项目",
                    description = if (canCreate) "创建项目来测试所有者权限。" else "请让项目所有者把你加入成员列表。",
                    actionLabel = if (canCreate) "创建项目" else null,
                    onAction = if (canCreate) ({ showCreateDialog = true }) else null,
                )
            }
        } else {
            items(visibleProjects, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    currentUser = currentUser,
                    onClick = { onOpenProject(project.id) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }

    if (showCreateDialog) {
        ProjectEditorDialog(
            title = "创建项目",
            confirmLabel = "创建",
            initialProject = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description, status ->
                onCreateProject(name, description, status) { showCreateDialog = false }
            },
        )
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    currentUser: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOwner = project.ownerId == currentUser.id
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProjectStatusBadge(project.status)
                Text(
                    if (isOwner) "你是所有者" else "Owner #${project.ownerId}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isOwner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    fontWeight = if (isOwner) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Text(project.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                project.description ?: "暂无项目描述",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "查看详情与成员  →",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun ProjectEditorDialog(
    title: String,
    confirmLabel: String,
    initialProject: Project?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, ProjectStatus) -> Unit,
) {
    var name by rememberSaveable(initialProject?.id) { mutableStateOf(initialProject?.name.orEmpty()) }
    var description by rememberSaveable(initialProject?.id) {
        mutableStateOf(initialProject?.description.orEmpty())
    }
    var status by rememberSaveable(initialProject?.id) {
        mutableStateOf(initialProject?.status ?: ProjectStatus.PLANNING)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (initialProject == null) "调用 POST /projects" else "调用 PATCH /projects/${initialProject.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("项目描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
                Text("状态", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProjectStatus.entries.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { Text(option.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, status) },
                enabled = name.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

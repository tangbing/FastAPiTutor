package com.edy.rbaclab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.edy.rbaclab.data.Project
import com.edy.rbaclab.data.ProjectStatus
import com.edy.rbaclab.data.User
import com.edy.rbaclab.domain.RbacPermissions
import com.edy.rbaclab.ui.components.EmptyState
import com.edy.rbaclab.ui.components.PermissionLockedCard
import com.edy.rbaclab.ui.components.ProjectStatusBadge
import com.edy.rbaclab.ui.components.RoleBadge
import com.edy.rbaclab.ui.components.ScreenHeader

@Composable
fun ProjectDetailScreen(
    project: Project?,
    currentUser: User,
    members: List<User>,
    allUsers: List<User>,
    onRefresh: () -> Unit,
    onUpdate: (String, String?, ProjectStatus, () -> Unit) -> Unit,
    onAddMember: (Int, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (project == null) {
        EmptyState(
            title = "正在读取项目",
            description = "调用 GET /projects/{project_id} 验证当前用户的可见权限。",
            modifier = modifier,
        )
        return
    }

    var showEditDialog by rememberSaveable(project.id) { mutableStateOf(false) }
    var showMemberDialog by rememberSaveable(project.id) { mutableStateOf(false) }
    val canManage = RbacPermissions.canManageProject(currentUser, project)
    val owner = allUsers.firstOrNull { it.id == project.ownerId }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "GET /projects/${project.id}",
                title = project.name,
                subtitle = project.description ?: "这个项目还没有描述。",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                actions = { OutlinedButton(onClick = onRefresh) { Text("刷新") } },
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProjectStatusBadge(project.status)
                        Text(
                            "项目 #${project.id}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("所有者", style = MaterialTheme.typography.labelMedium)
                        Text(
                            owner?.username ?: "User #${project.ownerId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (canManage) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { showEditDialog = true }) {
                                Text("编辑项目")
                            }
                            OutlinedButton(onClick = { showMemberDialog = true }) {
                                Text("添加成员")
                            }
                        }
                    }
                }
            }
        }

        item {
            if (canManage) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("管理权限已解锁", fontWeight = FontWeight.Bold)
                        Text(
                            if (currentUser.role.name == "ADMIN") {
                                "管理员可以管理任意项目。"
                            } else {
                                "你是项目所有者，可以 PATCH 项目并 POST 新成员。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            } else {
                PermissionLockedCard(
                    title = "当前为只读模式",
                    description = "项目成员可以查看详情和成员，但 PATCH 与添加成员会返回 HTTP 403。",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("项目成员", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "GET /projects/${project.id}/members",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        members.size.toString(),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (members.isEmpty()) {
            item {
                EmptyState(
                    title = "还没有项目成员",
                    description = "所有者不自动出现在成员表中；添加用户后会显示在这里。",
                    actionLabel = if (canManage) "添加第一个成员" else null,
                    onAction = if (canManage) ({ showMemberDialog = true }) else null,
                )
            }
        } else {
            items(members, key = { it.id }) { member ->
                MemberRow(member = member, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }

    if (showEditDialog) {
        ProjectEditorDialog(
            title = "编辑项目",
            confirmLabel = "保存修改",
            initialProject = project,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, description, status ->
                onUpdate(name, description, status) { showEditDialog = false }
            },
        )
    }

    if (showMemberDialog) {
        AddMemberDialog(
            users = allUsers.filter { candidate ->
                candidate.isActive && members.none { it.id == candidate.id }
            },
            onDismiss = { showMemberDialog = false },
            onConfirm = { userId -> onAddMember(userId) { showMemberDialog = false } },
        )
    }
}

@Composable
private fun MemberRow(member: User, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(member.username, fontWeight = FontWeight.Bold)
                Text(
                    "User #${member.id} · ${if (member.isActive) "账号启用" else "账号停用"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RoleBadge(member.role)
        }
    }
}

@Composable
private fun AddMemberDialog(
    users: List<User>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var selectedUserId by rememberSaveable { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加项目成员") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "调用 POST /projects/{project_id}/members。重复添加时会看到 HTTP 409。",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (users.isEmpty()) {
                    Text("没有可添加的活跃用户。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(users, key = { it.id }) { user ->
                            val selected = selectedUserId == user.id
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { selectedUserId = user.id },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(user.username, fontWeight = FontWeight.SemiBold)
                                    RoleBadge(user.role)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedUserId?.let(onConfirm) },
                enabled = selectedUserId != null,
            ) {
                Text("添加成员")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

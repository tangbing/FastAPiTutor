package com.edy.rbaclab.ui.screens

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.edy.rbaclab.data.User
import com.edy.rbaclab.data.UserRole
import com.edy.rbaclab.domain.RbacPermissions
import com.edy.rbaclab.ui.components.EmptyState
import com.edy.rbaclab.ui.components.PermissionLockedCard
import com.edy.rbaclab.ui.components.RoleBadge
import com.edy.rbaclab.ui.components.ScreenHeader

@Composable
fun UsersScreen(
    currentUser: User,
    users: List<User>,
    onRefresh: () -> Unit,
    onCreateUser: (String, String, UserRole, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    val canCreate = RbacPermissions.canCreateUser(currentUser)
    val filtered = users.filter { it.username.contains(search, ignoreCase = true) }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader(
                eyebrow = "GET /users",
                title = "团队与角色",
                subtitle = "管理员和经理可以读取用户；只有管理员可以创建新账号并指定角色。",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                actions = { OutlinedButton(onClick = onRefresh) { Text("刷新") } },
            )
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("搜索用户名") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                singleLine = true,
            )
        }
        item {
            if (canCreate) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("管理员操作", fontWeight = FontWeight.Bold)
                            Text("POST /users 创建角色账号", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { showCreateDialog = true }) { Text("新建用户") }
                    }
                }
            } else {
                PermissionLockedCard(
                    title = "创建用户已锁定",
                    description = "经理可以 GET /users，但 POST /users 仅允许管理员。",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyState("没有用户", "尝试清空搜索条件或由管理员创建账号。") }
        } else {
            items(filtered, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    isCurrentUser = user.id == currentUser.id,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { username, password, role ->
                onCreateUser(username, password, role) { showCreateDialog = false }
            },
        )
    }
}

@Composable
private fun UserCard(user: User, isCurrentUser: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(user.username, fontWeight = FontWeight.Bold)
                    if (isCurrentUser) {
                        Text("你", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "User #${user.id} · ${if (user.isActive) "Active" else "Disabled"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RoleBadge(user.role)
        }
    }
}

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, UserRole) -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(UserRole.MEMBER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建用户") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "用户名重复会触发 HTTP 409；密码少于 8 位会触发 HTTP 422。",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("初始密码") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Text("角色", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UserRole.entries.forEach { option ->
                        FilterChip(
                            selected = role == option,
                            onClick = { role = option },
                            label = { Text(option.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username, password, role) },
                enabled = username.length >= 3 && password.length >= 8,
            ) {
                Text("创建")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

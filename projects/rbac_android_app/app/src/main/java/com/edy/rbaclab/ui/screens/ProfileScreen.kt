package com.edy.rbaclab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edy.rbaclab.data.ApiLogEntry
import com.edy.rbaclab.data.User
import com.edy.rbaclab.data.UserRole
import com.edy.rbaclab.domain.RbacPermissions
import com.edy.rbaclab.ui.components.RoleBadge
import com.edy.rbaclab.ui.components.ScreenHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    currentUser: User,
    baseUrl: String,
    apiLog: List<ApiLogEntry>,
    onRefresh: () -> Unit,
    onClearLog: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHeader(
                eyebrow = "GET /auth/me",
                title = currentUser.username,
                subtitle = "当前身份由 JWT 的 sub 解析，再从数据库读取最新角色与启用状态。",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                actions = { RoleBadge(currentUser.role) },
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("当前角色能力", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    RbacPermissions.roleCapabilities(currentUser.role).forEach { capability ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                            Text(capability, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider()
                    Text("API 地址", style = MaterialTheme.typography.labelMedium)
                    Text(baseUrl, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onRefresh) { Text("刷新身份") }
                        Button(onClick = onLogout) { Text("退出登录") }
                    }
                }
            }
        }
        item {
            PermissionMatrix(modifier = Modifier.padding(horizontal = 20.dp))
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("接口活动", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "实时观察 2xx / 401 / 403 / 404 / 409 / 422",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onClearLog) { Text("清空") }
            }
        }
        if (apiLog.isEmpty()) {
            item {
                Text(
                    "操作任意页面后，接口记录会显示在这里。",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(apiLog, key = { it.timestampMillis.toString() + it.path + it.method }) { entry ->
                ApiLogRow(entry = entry, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

@Composable
private fun PermissionMatrix(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("权限矩阵", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "角色        项目创建   用户创建   全部项目",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium,
            )
            UserRole.entries.forEach { role ->
                val createProject = role != UserRole.MEMBER
                val createUser = role == UserRole.ADMIN
                val allProjects = role == UserRole.ADMIN
                Text(
                    "${role.wireValue.padEnd(10)}  ${mark(createProject).padEnd(8)}  ${mark(createUser).padEnd(8)}  ${mark(allProjects)}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun mark(allowed: Boolean): String = if (allowed) "YES" else "LOCK"

@Composable
private fun ApiLogRow(entry: ApiLogEntry, modifier: Modifier = Modifier) {
    val statusColor = when {
        entry.statusCode in 200..299 -> Color(0xFF17805F)
        entry.statusCode == 403 -> Color(0xFF9A6200)
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = statusColor.copy(alpha = 0.12f),
                contentColor = statusColor,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    if (entry.statusCode == 0) "NET" else entry.statusCode.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${entry.method} ${entry.path}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    entry.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Text(
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestampMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

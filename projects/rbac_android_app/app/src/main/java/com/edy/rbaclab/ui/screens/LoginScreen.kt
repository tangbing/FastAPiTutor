package com.edy.rbaclab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.edy.rbaclab.HealthState
import com.edy.rbaclab.ui.components.HealthPill
import com.edy.rbaclab.ui.components.ScreenHeader

@Composable
fun LoginScreen(
    baseUrl: String,
    healthState: HealthState,
    isLoading: Boolean,
    onBaseUrlChange: (String) -> Unit,
    onCheckHealth: () -> Unit,
    onLogin: (String, String) -> Unit,
    onBootstrap: (String, String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var baseUrlInput by rememberSaveable(baseUrl) { mutableStateOf(baseUrl) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showBootstrap by rememberSaveable { mutableStateOf(false) }
    val canSubmit = username.length >= 3 && password.length >= 8 && !isLoading

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "RBAC / LAB",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            HealthPill(healthState)
        }

        ScreenHeader(
            eyebrow = "Android × FastAPI",
            title = "把权限规则，做成看得见的体验",
            subtitle = "登录不同角色，观察同一套接口如何改变项目可见性、操作入口和 HTTP 状态反馈。",
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("登录工作区", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("用户名") },
                    supportingText = { Text("至少 3 个字符") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    supportingText = { Text("至少 8 个字符") },
                    singleLine = true,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) "隐藏" else "显示")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (canSubmit) onLogin(username, password) },
                    ),
                )
                Button(
                    onClick = { onLogin(username, password) },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("登录并读取 /auth/me", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("第一次运行？", fontWeight = FontWeight.SemiBold)
                        Text(
                            "仅数据库中没有用户时可初始化管理员",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showBootstrap = true }) {
                        Text("初始化管理员")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("API 连接", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = { baseUrlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    supportingText = {
                        Text("模拟器用 10.0.2.2；真机请填写电脑的局域网 IP")
                    },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onBaseUrlChange(baseUrlInput) }) {
                        Text("保存并检测")
                    }
                    OutlinedButton(onClick = onCheckHealth) {
                        Text("重新检测")
                    }
                }
            }
        }

        Text(
            text = "角色预览  ·  管理员：全局管理  /  经理：创建并管理自己的项目  /  成员：只读已加入项目",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
    }

    if (showBootstrap) {
        BootstrapDialog(
            isLoading = isLoading,
            onDismiss = { showBootstrap = false },
            onConfirm = { adminName, adminPassword ->
                onBootstrap(adminName, adminPassword) { showBootstrap = false }
            },
        )
    }
}

@Composable
private fun BootstrapDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("admin") }
    var password by rememberSaveable { mutableStateOf("") }
    val enabled = username.length >= 3 && password.length >= 8 && !isLoading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("初始化第一个管理员") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "调用 POST /auth/bootstrap。已有用户时服务器会返回 HTTP 409。",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("管理员用户名") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("管理员密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(username, password) }, enabled = enabled) {
                Text("创建管理员")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

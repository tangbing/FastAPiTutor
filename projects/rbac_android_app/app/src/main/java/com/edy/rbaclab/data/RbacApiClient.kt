package com.edy.rbaclab.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class RbacApiClient(
    baseUrl: String,
    private val onCall: (ApiLogEntry) -> Unit,
) {
    @Volatile
    var baseUrl: String = normalizeBaseUrl(baseUrl)

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun health(): String {
        val body = execute(
            Request.Builder()
                .url(url("/health"))
                .get()
                .build(),
        )
        return JSONObject(body).optString("status", "unknown")
    }

    suspend fun bootstrap(username: String, password: String): User {
        val body = execute(
            jsonRequest(
                path = "/auth/bootstrap",
                method = "POST",
                payload = JSONObject()
                    .put("username", username)
                    .put("password", password),
            ),
        )
        return parseUser(JSONObject(body))
    }

    suspend fun login(username: String, password: String): Token {
        val form = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()
        val body = execute(
            Request.Builder()
                .url(url("/auth/token"))
                .post(form)
                .build(),
        )
        val json = JSONObject(body)
        return Token(
            accessToken = json.getString("access_token"),
            tokenType = json.optString("token_type", "bearer"),
        )
    }

    suspend fun me(token: String): User = parseUser(
        JSONObject(execute(authGet("/auth/me", token))),
    )

    suspend fun listUsers(token: String, offset: Int = 0, limit: Int = 100): List<User> {
        val body = execute(authGet("/users?offset=$offset&limit=$limit", token))
        return parseArray(body, ::parseUser)
    }

    suspend fun createUser(
        token: String,
        username: String,
        password: String,
        role: UserRole,
    ): User {
        val payload = JSONObject()
            .put("username", username)
            .put("password", password)
            .put("role", role.wireValue)
        return parseUser(
            JSONObject(execute(jsonRequest("/users", "POST", payload, token))),
        )
    }

    suspend fun listProjects(token: String, offset: Int = 0, limit: Int = 100): List<Project> {
        val body = execute(authGet("/projects?offset=$offset&limit=$limit", token))
        return parseArray(body, ::parseProject)
    }

    suspend fun getProject(token: String, projectId: Int): Project = parseProject(
        JSONObject(execute(authGet("/projects/$projectId", token))),
    )

    suspend fun createProject(
        token: String,
        name: String,
        description: String?,
        status: ProjectStatus,
    ): Project {
        val payload = JSONObject()
            .put("name", name)
            .put("description", description ?: JSONObject.NULL)
            .put("status", status.wireValue)
        return parseProject(
            JSONObject(execute(jsonRequest("/projects", "POST", payload, token))),
        )
    }

    suspend fun updateProject(
        token: String,
        projectId: Int,
        name: String,
        description: String?,
        status: ProjectStatus,
    ): Project {
        val payload = JSONObject()
            .put("name", name)
            .put("description", description ?: JSONObject.NULL)
            .put("status", status.wireValue)
        return parseProject(
            JSONObject(
                execute(jsonRequest("/projects/$projectId", "PATCH", payload, token)),
            ),
        )
    }

    suspend fun listMembers(token: String, projectId: Int): List<User> {
        val body = execute(authGet("/projects/$projectId/members", token))
        return parseArray(body, ::parseUser)
    }

    suspend fun addMember(token: String, projectId: Int, userId: Int): User {
        val payload = JSONObject().put("user_id", userId)
        return parseUser(
            JSONObject(
                execute(
                    jsonRequest(
                        path = "/projects/$projectId/members",
                        method = "POST",
                        payload = payload,
                        token = token,
                    ),
                ),
            ),
        )
    }

    private fun authGet(path: String, token: String): Request = Request.Builder()
        .url(url(path))
        .header("Authorization", "Bearer $token")
        .get()
        .build()

    private fun jsonRequest(
        path: String,
        method: String,
        payload: JSONObject,
        token: String? = null,
    ): Request {
        val builder = Request.Builder()
            .url(url(path))
            .method(method, payload.toString().toRequestBody(JSON_MEDIA_TYPE))
        if (token != null) builder.header("Authorization", "Bearer $token")
        return builder.build()
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val path = request.url.encodedPath
                val detail = if (response.isSuccessful) {
                    "请求成功"
                } else {
                    parseErrorDetail(responseBody)
                }
                onCall(
                    ApiLogEntry(
                        method = request.method,
                        path = path,
                        statusCode = response.code,
                        detail = detail,
                    ),
                )
                if (!response.isSuccessful) {
                    throw ApiException(response.code, detail)
                }
                responseBody
            }
        } catch (error: ApiException) {
            throw error
        } catch (error: IOException) {
            val detail = error.message ?: "无法连接服务器"
            onCall(
                ApiLogEntry(
                    method = request.method,
                    path = request.url.encodedPath,
                    statusCode = 0,
                    detail = detail,
                ),
            )
            throw ApiException(0, detail, error)
        }
    }

    private fun url(path: String): String = "$baseUrl${if (path.startsWith('/')) path else "/$path"}"

    private fun parseErrorDetail(body: String): String = runCatching {
        val detail = JSONObject(body).opt("detail")
        when (detail) {
            is String -> detail
            is JSONArray -> detail.toString()
            else -> "服务器返回错误"
        }
    }.getOrDefault("服务器返回错误")

    private fun parseUser(json: JSONObject): User = User(
        id = json.getInt("id"),
        username = json.getString("username"),
        role = UserRole.fromWire(json.optString("role", "member")),
        isActive = json.optBoolean("is_active", true),
    )

    private fun parseProject(json: JSONObject): Project = Project(
        id = json.getInt("id"),
        name = json.getString("name"),
        description = if (json.isNull("description")) null else json.optString("description"),
        status = ProjectStatus.fromWire(json.optString("status", "planning")),
        ownerId = json.getInt("owner_id"),
    )

    private fun <T> parseArray(body: String, parser: (JSONObject) -> T): List<T> {
        val array = JSONArray(body)
        return buildList {
            for (index in 0 until array.length()) add(parser(array.getJSONObject(index)))
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun normalizeBaseUrl(value: String): String {
            val trimmed = value.trim().trimEnd('/')
            return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "http://$trimmed"
            }
        }
    }
}

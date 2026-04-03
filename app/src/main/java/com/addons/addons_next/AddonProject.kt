package com.addons.addons_next

import org.json.JSONObject

data class AddonProject(
    val id: String,
    val name: String,
    val namespace: String,
    val authorId: String,
    val description: String,
    val packVersion: String,
    val minEngineVersion: String,
    val createdAt: Long,
    val updatedAt: Long,
    val rootPath: String,
    val behaviorPackPath: String?,
    val resourcePackPath: String?
) {
    val hasBehaviorPack: Boolean
        get() = !behaviorPackPath.isNullOrBlank()

    val hasResourcePack: Boolean
        get() = !resourcePackPath.isNullOrBlank()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("namespace", namespace)
        put("authorId", authorId)
        put("description", description)
        put("packVersion", packVersion)
        put("minEngineVersion", minEngineVersion)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("rootPath", rootPath)
        put("behaviorPackPath", behaviorPackPath)
        put("resourcePackPath", resourcePackPath)
    }

    companion object {
        fun fromJson(jsonObject: JSONObject): AddonProject = AddonProject(
            id = jsonObject.getString("id"),
            name = jsonObject.getString("name"),
            namespace = jsonObject.getString("namespace"),
            authorId = jsonObject.optString("authorId"),
            description = jsonObject.optString("description"),
            packVersion = jsonObject.optString("packVersion", "1.0.0"),
            minEngineVersion = jsonObject.optString("minEngineVersion", AddonProjectStorage.DEFAULT_ENGINE_VERSION),
            createdAt = jsonObject.optLong("createdAt"),
            updatedAt = jsonObject.optLong("updatedAt"),
            rootPath = jsonObject.optString("rootPath"),
            behaviorPackPath = jsonObject.optString("behaviorPackPath").ifBlank { null },
            resourcePackPath = jsonObject.optString("resourcePackPath").ifBlank { null }
        )
    }
}

data class CreateProjectRequest(
    val name: String,
    val namespace: String,
    val authorId: String,
    val description: String,
    val minEngineVersion: String,
    val includeBehaviorPack: Boolean,
    val includeResourcePack: Boolean
)

data class UserSettings(
    val authorId: String = "studio.author",
    val minEngineVersion: String = AddonProjectStorage.DEFAULT_ENGINE_VERSION,
    val defaultDescription: String = "Generated with Addons IDE Next",
    val includeResourcePackByDefault: Boolean = true
)

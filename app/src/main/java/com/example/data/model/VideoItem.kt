package com.example.data.model

data class VideoItem(
    val id: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val category: String = "General",
    val description: String = "",
    val duration: String = "00:00",
    val views: String = "1.2K",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "videoUrl" to videoUrl,
            "thumbnailUrl" to thumbnailUrl,
            "category" to category,
            "description" to description,
            "duration" to duration,
            "views" to views,
            "timestamp" to timestamp
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): VideoItem {
            return VideoItem(
                id = id,
                title = map["title"] as? String ?: "Untitled Video",
                videoUrl = map["videoUrl"] as? String ?: "",
                thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                category = map["category"] as? String ?: "General",
                description = map["description"] as? String ?: "",
                duration = map["duration"] as? String ?: "03:45",
                views = map["views"] as? String ?: "1.5K",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.VideoItem
import com.example.service.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VideoRepository(private val context: Context) {

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore not initialized or unavailable: ${e.message}")
            null
        }
    }

    // Curated high quality working stream list (Public Domain / Creative Commons Blender Foundation & Google open streams)
    private val defaultVideos = listOf(
        VideoItem(
            id = "vid_1",
            title = "Big Buck Bunny - Forest Adventure",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
            category = "Animation",
            description = "A large and lovable rabbit takes on bullying forest pests in this classic high-definition animated open movie.",
            duration = "09:56",
            views = "45.2K",
            timestamp = System.currentTimeMillis() - 86400000L * 2
        ),
        VideoItem(
            id = "vid_2",
            title = "Tears of Steel - Sci-Fi VFX Showcase",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            category = "Sci-Fi",
            description = "Set in a dystopian future in Amsterdam, a group of warriors and scientists battle rogue robotics with cutting-edge visual effects.",
            duration = "12:14",
            views = "32.8K",
            timestamp = System.currentTimeMillis() - 86400000L * 5
        ),
        VideoItem(
            id = "vid_3",
            title = "Elephants Dream - Open Source Cinema",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            category = "Action",
            description = "The strange journey of two men through the mechanical intestines of a gigantic machine world.",
            duration = "10:53",
            views = "28.4K",
            timestamp = System.currentTimeMillis() - 86400000L * 7
        ),
        VideoItem(
            id = "vid_4",
            title = "For Bigger Blazes - Extreme Action",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=600&auto=format&fit=crop&q=80",
            category = "Trailers",
            description = "High octane action cinematic trailer with ultra HD visuals and thrilling sound design.",
            duration = "00:15",
            views = "19.5K",
            timestamp = System.currentTimeMillis() - 86400000L * 10
        ),
        VideoItem(
            id = "vid_5",
            title = "For Bigger Escapes - Nature & Drone",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=600&auto=format&fit=crop&q=80",
            category = "Documentary",
            description = "Escape to the most breathtaking landscapes around the globe captured with majestic aerial cinematography.",
            duration = "00:15",
            views = "14.1K",
            timestamp = System.currentTimeMillis() - 86400000L * 12
        )
    )

    private val _videosState = MutableStateFlow<List<VideoItem>>(defaultVideos)
    val videosState = _videosState.asStateFlow()

    private val unlockedVideoIds = mutableSetOf<String>()

    init {
        listenToFirestore()
    }

    private fun listenToFirestore() {
        val fs = firestore ?: return
        try {
            fs.collection(COLLECTION_VIDEOS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore listen failed: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val firestoreList = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { VideoItem.fromMap(doc.id, it) }
                        }
                        // Combine Firestore videos with default base videos (filtering out duplicates)
                        val firestoreIds = firestoreList.map { it.id }.toSet()
                        val combined = firestoreList + defaultVideos.filter { it.id !in firestoreIds }
                        _videosState.value = combined
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Firestore listener", e)
        }
    }

    suspend fun addVideo(
        title: String,
        videoUrl: String,
        category: String,
        description: String,
        thumbnailUrl: String,
        sendPushNotification: Boolean = true
    ): Result<VideoItem> {
        return try {
            val id = "vid_${UUID.randomUUID().toString().substring(0, 8)}"
            val thumb = if (thumbnailUrl.isNotBlank()) thumbnailUrl else {
                "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=600&auto=format&fit=crop&q=80"
            }

            val newVideo = VideoItem(
                id = id,
                title = title.trim(),
                videoUrl = videoUrl.trim(),
                thumbnailUrl = thumb.trim(),
                category = category.trim().ifEmpty { "General" },
                description = description.trim().ifEmpty { "Added by Admin" },
                duration = "04:30",
                views = "1",
                timestamp = System.currentTimeMillis()
            )

            // Save to Firestore if available
            firestore?.let { fs ->
                try {
                    fs.collection(COLLECTION_VIDEOS)
                        .document(id)
                        .set(newVideo.toMap())
                        .await()
                    Log.d(TAG, "Video added to Firestore successfully: $id")
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore write failed, persisting locally: ${e.message}")
                }
            }

            // Also prepend to local state immediately for instant responsive UI
            val currentList = _videosState.value.toMutableList()
            currentList.add(0, newVideo)
            _videosState.value = currentList

            // Trigger Push Notification if enabled
            if (sendPushNotification) {
                NotificationHelper.showNewVideoNotification(
                    context = context,
                    videoTitle = newVideo.title,
                    description = newVideo.description
                )
            }

            Result.success(newVideo)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding video", e)
            Result.failure(e)
        }
    }

    suspend fun deleteVideo(videoId: String): Result<Unit> {
        return try {
            firestore?.let { fs ->
                try {
                    fs.collection(COLLECTION_VIDEOS).document(videoId).delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore delete failed: ${e.message}")
                }
            }
            _videosState.value = _videosState.value.filter { it.id != videoId }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isVideoUnlocked(videoId: String): Boolean {
        return unlockedVideoIds.contains(videoId)
    }

    fun unlockVideo(videoId: String) {
        unlockedVideoIds.add(videoId)
    }

    companion object {
        private const val TAG = "VideoRepository"
        private const val COLLECTION_VIDEOS = "videos"
    }
}

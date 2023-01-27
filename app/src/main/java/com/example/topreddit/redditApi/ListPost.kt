package com.example.topreddit.redditApi

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.http.*
import java.util.*


class ListPostResponse {
    @SerializedName("data")
    @Expose
    var data: ListInfo? = null
}

class ListInfo {
    @SerializedName("children")
    @Expose
    var postsInfo: List<PostsInfo>? = null
}

class PostsInfo {
    @SerializedName("data")
    @Expose
    var post: Post? = null
}

@Entity(tableName = "posts")
class Post {
    @PrimaryKey
    var postId: Long? = null

    @field:SerializedName("name")
    @Expose
    var name: String? = null

    @field:SerializedName("id")
    @Expose
    var id: String? = null

    @field:SerializedName("subreddit")
    @Expose
    var subreddit: String? = null

    @field:SerializedName("selftext")
    @Expose
    var description: String? = null

    @field:SerializedName("title")
    @Expose
    var title: String? = null

    @field:SerializedName("url_overridden_by_dest")
    @Expose
    var simple_medial_url: String? = null

    @field:SerializedName("created_utc")
    @Expose
    var created_utc: Long? = null

    @field:SerializedName("thumbnail")
    @Expose
    var thumbnail: String? = null

    @field:SerializedName("url")
    @Expose
    var url: String? = null

    @field:SerializedName("num_comments")
    @Expose
    var num_comments: Long? = null

    fun getTimeAgoString(): String {
        val SECOND_MILLIS: Int = 1000;
        val MINUTE_MILLIS: Int = 60 * SECOND_MILLIS;
        val HOUR_MILLIS: Int = 60 * MINUTE_MILLIS;
        val DAY_MILLIS: Int = 24 * HOUR_MILLIS;

        var time: Long = created_utc ?: return "Unknown"

        if (time < 1000000000000L) {
            time *= 1000
        }

        val now = System.currentTimeMillis()
        if (time > now || time <= 0) {
            return "Unknown"
        }

        val diff: Long = now - time
        return if (diff < MINUTE_MILLIS) {
            "just now"
        } else if (diff < 2 * MINUTE_MILLIS) {
            "a minute ago"
        } else if (diff < 50 * MINUTE_MILLIS) {
            (diff / MINUTE_MILLIS).toString() + " minutes ago"
        } else if (diff < 90 * MINUTE_MILLIS) {
            "an hour ago"
        } else if (diff < 24 * HOUR_MILLIS) {
            (diff / HOUR_MILLIS).toString() + " hours ago"
        } else if (diff < 48 * HOUR_MILLIS) {
            "yesterday"
        } else {
            (diff / DAY_MILLIS).toString() + " days ago"
        }
    }
}

/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.topreddit

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.topreddit.redditApi.Post


/**
 * View Holder for a [Post] RecyclerView list item.
 */
class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val imageViewThumbnail: ImageView = view.findViewById(R.id.imageViewThumbnail)
    private val textViewAuthor: TextView = view.findViewById(R.id.textViewAuthor)
    private val textViewTimeAgo: TextView = view.findViewById(R.id.textViewTimeAgo)
    private val textViewCommentsCount: TextView = view.findViewById(R.id.textViewCommentsCount)
    private val textViewTitle: TextView = view.findViewById(R.id.textViewTitle)

    private var post: Post? = null

    init {
        view.setOnClickListener {
            Log.d(TAG, "Post[" + post?.id.toString() + "] clicked")
            post?.url?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view.context.startActivity(intent)
            }
        }
    }

    fun bind(post: Post?) {
        if (post == null) {
            val resources = itemView.resources
            textViewTitle.text = resources.getString(R.string.loading)
        } else {
            showPostData(post)
        }
    }

    private fun showPostData(post: Post) {
        this.post = post

        Glide.with(itemView)
            .load(post.thumbnail)
            .error(R.drawable.ic_broken_image)
            .into(imageViewThumbnail);

        textViewAuthor.text = post.subreddit
        textViewTimeAgo.text = post.getTimeAgoString()
        textViewTitle.text = post.title
        textViewCommentsCount.text = post.num_comments.toString()
    }

    companion object {
        fun create(parent: ViewGroup): PostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.post_view_item, parent, false)
            return PostViewHolder(view)
        }

        private const val TAG = "PostAdapter"
    }
}

package com.example.topreddit.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.topreddit.db.PostDatabase
import com.example.topreddit.redditApi.Post
import com.example.topreddit.redditApi.RedditService
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that works with local and remote data sources.
 */
class RedditRepository(
    private val service: RedditService,
    private val postDatabase: PostDatabase
) {

    @OptIn(ExperimentalPagingApi::class)
    fun getPosts(): Flow<PagingData<Post>> {
        val pagingSourceFactory = { postDatabase.postsDao().getAll() }

        return Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = RedditRemoteMediator(
                service,
                postDatabase
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 20
        private const val TAG = "RedditRepository"
    }
}

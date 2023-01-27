package com.example.topreddit.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.topreddit.data.RedditRepository.Companion.NETWORK_PAGE_SIZE
import com.example.topreddit.db.PostDatabase
import com.example.topreddit.redditApi.Post
import com.example.topreddit.redditApi.RedditService
import retrofit2.HttpException
import java.io.IOException

private const val REDDIT_STARTING_SLICE_ANCHOR = "null"

@OptIn(ExperimentalPagingApi::class)
class RedditRemoteMediator(
    private val service: RedditService,
    private val postDatabase: PostDatabase
) : RemoteMediator<Int, Post>() {

    override suspend fun initialize(): InitializeAction {
        // Launch remote refresh as soon as paging starts and do not trigger remote prepend or
        // append until refresh has succeeded.
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, Post>): MediatorResult {

        val after = when (loadType) {
            LoadType.REFRESH -> {
                REDDIT_STARTING_SLICE_ANCHOR
            }
            LoadType.PREPEND -> {
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {

                val after =
                    state.pages.lastOrNull() { it.data.isNotEmpty() }?.data?.lastOrNull()?.name

                if (after == null) {
                    return MediatorResult.Success(endOfPaginationReached = false)
                }

                after
            }
        }

        try {
            val apiResponse = service.getPosts(
                "android:com.example.topreddit:v1.0 (by /u/RedditechTester)",
                "top",
                after,
                NETWORK_PAGE_SIZE
            )

            val posts = apiResponse.data?.postsInfo!!.map { it.post!! }

            val endOfPaginationReached = posts.isEmpty()
            postDatabase.withTransaction {
                // clear all tables in the database
                if (loadType == LoadType.REFRESH) {
                    postDatabase.postsDao().clearPosts()
                }

                postDatabase.postsDao().insertAll(posts)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    companion object {
        private const val TAG = "RedditRemoteMediator"
    }
}

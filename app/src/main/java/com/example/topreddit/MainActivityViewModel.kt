package com.example.topreddit

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.example.topreddit.data.RedditRepository
import com.example.topreddit.redditApi.Post
import kotlinx.coroutines.flow.Flow

class MainViewModel(
    private val repository: RedditRepository
) : ViewModel() {

    val pagingDataFlow: Flow<PagingData<Post>> by lazy { getPosts() }

    private fun getPosts(): Flow<PagingData<Post>> =
        repository.getPosts()

}

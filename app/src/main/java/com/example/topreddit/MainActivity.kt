package com.example.topreddit

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.topreddit.databinding.ActivityMainBinding
import com.example.topreddit.redditApi.Post
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get the view model
        val viewModel = ViewModelProvider(
            this, Injection.provideViewModelFactory(
                context = this,
                owner = this
            )
        )
            .get(MainViewModel::class.java)

        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.recycleViewPostList.addItemDecoration(decoration)

        val postAdapter = PostAdapter()
        val header = PostsLoadStateAdapter { postAdapter.retry() }
        binding.recycleViewPostList.adapter = postAdapter.withLoadStateHeaderAndFooter(
            header = header,
            footer = PostsLoadStateAdapter { postAdapter.retry() }
        )

        binding.bindList(
            header = header,
            postAdapter = postAdapter,
            pagingData = viewModel.pagingDataFlow
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_refresh -> {
            val postAdapter =
                (binding.recycleViewPostList.adapter as ConcatAdapter).adapters[1] as PostAdapter
            binding.recycleViewPostList.smoothScrollToPosition(0)
            postAdapter.refresh()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }


    private fun ActivityMainBinding.bindList(
        header: PostsLoadStateAdapter,
        postAdapter: PostAdapter,
        pagingData: Flow<PagingData<Post>>
    ) {
        retryButton.setOnClickListener { postAdapter.retry() }

        lifecycleScope.launch {
            pagingData.collectLatest(postAdapter::submitData)
        }

        lifecycleScope.launch {
            postAdapter.loadStateFlow.collect { loadState ->
                // Show a retry header if there was an error refreshing, and items were previously
                // cached OR default to the default prepend state

                header.loadState = loadState.mediator
                    ?.refresh
                    ?.takeIf { it is LoadState.Error && postAdapter.itemCount > 0 }
                    ?: loadState.prepend

                val isErrorLoad =
                    loadState.mediator?.refresh is LoadState.Error && postAdapter.itemCount == 0
                // show empty recycleViewPostList
                textViewErrorMessage.isVisible = isErrorLoad
                // Only show the recycleViewPostList if refresh succeeds, either from the the local db or the remote.
                recycleViewPostList.isVisible =
                    loadState.source.refresh is LoadState.NotLoading || loadState.mediator?.refresh is LoadState.NotLoading
                // Show loading spinner during initial load or refresh.
                progressBar.isVisible = loadState.mediator?.refresh is LoadState.Loading
                // Show the retry state if initial load or refresh fails.
                retryButton.isVisible = isErrorLoad
                // Toast on any error, regardless of whether it came from RemoteMediator or PagingSource
                val errorState = loadState.source.append as? LoadState.Error
                    ?: loadState.source.prepend as? LoadState.Error
                    ?: loadState.append as? LoadState.Error
                    ?: loadState.prepend as? LoadState.Error

                errorState?.let {
                    Log.e(Companion.TAG, it.error.toString())
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
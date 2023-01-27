package com.example.topreddit

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.topreddit.databinding.ActivityMainBinding
import com.example.topreddit.redditApi.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    //ImageLoader instance
    private lateinit var imageLoader: ImageLoader

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


        //getting imageloader instance
        imageLoader = Coil.imageLoader(this)

        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.recycleViewPostList.addItemDecoration(decoration)

        val postAdapter = PostAdapter(::saveImageByUrl)
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

    private fun saveImageByUrl(url: String?): Unit {
        getBitmapFromUrl(url!!)
    }

    private fun getBitmapFromUrl(bitmapURL: String) = lifecycleScope.launch {
        val request = ImageRequest.Builder(this@MainActivity)
            .data(bitmapURL)
            .build()
        try {
            val downloadedBitmap = (imageLoader.execute(request).drawable as BitmapDrawable).bitmap
            saveMediaToStorage(downloadedBitmap)
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            Toast.makeText(this@MainActivity, "Image save error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveMediaToStorage(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(this, "Saved to Photos", Toast.LENGTH_SHORT).show()
        }
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
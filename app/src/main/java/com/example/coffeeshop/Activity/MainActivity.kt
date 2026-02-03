package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.coffeeshop.Adapter.CategoryAdapter
import com.example.coffeeshop.Adapter.PopularAdapter
import com.example.coffeeshop.ViewModel.MainViewModel
import com.example.coffeeshop.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val viewModel= MainViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)

        // This line connects the activity to the layout file.
        setContentView(binding.root)

        initBanner()
        initCategory()
        initPopular()
        initBottomMenu()
    }

    private fun initBottomMenu() {
        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, CartActivity::class.java))
        }
    }

    private fun initBanner() {
        binding.progressBarBanner.visibility= View.VISIBLE;
        viewModel.loadBanner().observeForever{
            Glide.with(this@MainActivity)
                .load(it[0].url)
                .into(binding.banner)
            binding.progressBarBanner.visibility= View.GONE
        }

    }
    private fun initCategory() {
        binding.progressBarCategory.visibility= View.VISIBLE;
        viewModel.loadCategory().observeForever{
            binding.recyclerViewCat.layoutManager= LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false

            )
            binding.recyclerViewCat.adapter= CategoryAdapter(it)
            binding.progressBarCategory.visibility= View.GONE
        }
    }

    private fun initPopular() {
        binding.progressBarPopular.visibility= View.VISIBLE;
        viewModel.loadPopular().observeForever{
            binding.recyclerViewPopular.layoutManager= GridLayoutManager(this,2)
            binding.recyclerViewPopular.adapter= PopularAdapter(it)
            binding.progressBarPopular.visibility= View.GONE
        }
    }
}
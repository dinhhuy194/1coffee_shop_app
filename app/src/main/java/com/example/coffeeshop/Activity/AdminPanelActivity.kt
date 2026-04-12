package com.example.coffeeshop.Activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.coffeeshop.Fragment.AdminBannerFragment
import com.example.coffeeshop.Fragment.AdminCategoryFragment
import com.example.coffeeshop.Fragment.AdminItemsFragment
import com.example.coffeeshop.Fragment.AdminOrderFragment
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.ActivityAdminPanelBinding

class AdminPanelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminPanelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AdminCategoryFragment())
                .commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_categories -> AdminCategoryFragment()
                R.id.nav_items -> AdminItemsFragment()
                R.id.nav_banners -> AdminBannerFragment()
                R.id.nav_orders -> AdminOrderFragment()
                else -> AdminCategoryFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
            true
        }
    }
}

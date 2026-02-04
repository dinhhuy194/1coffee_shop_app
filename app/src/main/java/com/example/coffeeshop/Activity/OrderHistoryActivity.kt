package com.example.coffeeshop.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.coffeeshop.Fragment.OrderHistoryFragment
import com.example.coffeeshop.databinding.ActivityOrderHistoryBinding

class OrderHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderHistoryBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Load OrderHistoryFragment if not already loaded
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, OrderHistoryFragment())
                .commit()
        }
    }
}

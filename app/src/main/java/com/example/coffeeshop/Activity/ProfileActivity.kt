package com.example.coffeeshop.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.coffeeshop.Fragment.ProfileFragment
import com.example.coffeeshop.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Load ProfileFragment if not already loaded
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, ProfileFragment())
                .commit()
        }
    }
}

package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.coffeeshop.Activity.MainActivity
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.ActivitySplashBinding
import kotlin.jvm.java

class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.StartBtn.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }

    }
}
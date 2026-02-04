package com.example.coffeeshop.Activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Adapter.CartAdapter
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.ActivityCartBinding
import com.example.project1762.Helper.ManagmentCart
import com.uilover.project195.Helper.ChangeNumberItemsListener

class CartActivity : AppCompatActivity() {
    lateinit var binding: ActivityCartBinding
    lateinit var managmentCart: ManagmentCart
    private var tax: Double = 0.0
    private var delivery: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cart)

        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        managmentCart = ManagmentCart(this)

        calculateCart()
        setVariable()
        initCartList()
    }

    private fun initCartList() {
        binding.apply {
            cartView.layoutManager = LinearLayoutManager(
                this@CartActivity,
                LinearLayoutManager.VERTICAL, false
            )
            cartView.adapter = CartAdapter(managmentCart.getListCart(), this@CartActivity,
                object : ChangeNumberItemsListener {
                    override fun onChanged() {
                        calculateCart()
                    }
                })
        }
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun calculateCart() {
        val percentTax = 0.02  // 2% tax
        val deliveryFee = 15.0
        
        val subtotal = Math.round(managmentCart.getTotalFee() * 100) / 100.0
        tax = Math.round(subtotal * percentTax * 100) / 100.0
        val total = Math.round((subtotal + tax + deliveryFee) * 100) / 100.0
        
        binding.apply {
            totalFeeTxt.text = "$${String.format("%.2f", subtotal)}"
            taxTxt.text = "$${String.format("%.2f", tax)}"
            deliveryTxt.text = "$${String.format("%.2f", deliveryFee)}"
            totalTxt.text = "$${String.format("%.2f", total)}"
        }

    }
}
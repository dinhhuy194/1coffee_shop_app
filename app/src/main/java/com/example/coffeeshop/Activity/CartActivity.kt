package com.example.coffeeshop.Activity

import android.os.Bundle
import android.view.View
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
                        updateEmptyState()
                    }
                })
        }
        
        // Initial empty state check
        updateEmptyState()
    }
    
    private fun updateEmptyState() {
        val isEmpty = managmentCart.getListCart().isEmpty()
        binding.emptyCartLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.cartView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener {
            finish()
        }
        
        // Proceed to Checkout button
        binding.button3.setOnClickListener {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            
            if (currentUser == null) {
                // Show login dialog
                android.app.AlertDialog.Builder(this)
                    .setTitle("Login Required")
                    .setMessage("Please login to proceed with checkout")
                    .setPositiveButton("Login") { _, _ ->
                        startActivity(android.content.Intent(this, LoginActivity::class.java))
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                // Get calculated values
                val percentTax = 0.02
                val deliveryFee = 15.0
                val subtotal = Math.round(managmentCart.getTotalFee() * 100) / 100.0
                val tax = Math.round(subtotal * percentTax * 100) / 100.0
                val total = Math.round((subtotal + tax + deliveryFee) * 100) / 100.0
                
                // Go to checkout
                val intent = android.content.Intent(this, CheckoutActivity::class.java)
                intent.putExtra("subtotal", subtotal)
                intent.putExtra("tax", tax)
                intent.putExtra("delivery", deliveryFee)
                intent.putExtra("total", total)
                startActivity(intent)
            }
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
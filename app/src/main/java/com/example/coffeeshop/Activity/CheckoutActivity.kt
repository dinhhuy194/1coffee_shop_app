package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.coffeeshop.ViewModel.CheckoutViewModel
import com.example.coffeeshop.databinding.ActivityCheckoutBinding
import com.example.project1762.Helper.ManagmentCart
import com.google.firebase.auth.FirebaseAuth

class CheckoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCheckoutBinding
    private val viewModel: CheckoutViewModel by viewModels()
    private lateinit var managmentCart: ManagmentCart
    
    private var subtotal = 0.0
    private var tax = 0.0
    private var delivery = 0.0
    private var total = 0.0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        managmentCart = ManagmentCart(this)
        
        // Get data from intent
        subtotal = intent.getDoubleExtra("subtotal", 0.0)
        tax = intent.getDoubleExtra("tax", 0.0)
        delivery = intent.getDoubleExtra("delivery", 0.0)
        total = intent.getDoubleExtra("total", 0.0)
        
        displayOrderSummary()
        setupListeners()
        observeCheckoutState()
    }
    
    private fun displayOrderSummary() {
        binding.apply {
            subtotalTxt.text = "$${String.format("%.2f", subtotal)}"
            taxTxt.text = "$${String.format("%.2f", tax)}"
            deliveryTxt.text = "$${String.format("%.2f", delivery)}"
            totalTxt.text = "$${String.format("%.2f", total)}"
        }
    }
    
    private fun setupListeners() {
        binding.placeOrderBtn.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            
            if (currentUser == null) {
                Toast.makeText(this, "You must be logged in to place an order", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val cartItems = managmentCart.getListCart()
            
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }
            
            // Place order
            viewModel.placeOrder(
                userId = currentUser.uid,
                items = cartItems,
                subtotal = subtotal,
                tax = tax,
                delivery = delivery,
                totalAmount = total
            )
        }
    }
    
    private fun observeCheckoutState() {
        viewModel.checkoutState.observe(this) { state ->
            when (state) {
                is CheckoutViewModel.CheckoutState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
                is CheckoutViewModel.CheckoutState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.placeOrderBtn.isEnabled = false
                }
                is CheckoutViewModel.CheckoutState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.placeOrderBtn.isEnabled = true
                    
                    Toast.makeText(this, "Order placed successfully! Order ID: ${state.orderId}", Toast.LENGTH_LONG).show()
                    
                    // Clear cart
                    managmentCart.clearCart()
                    
                    // Go back to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }
                is CheckoutViewModel.CheckoutState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.placeOrderBtn.isEnabled = true
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

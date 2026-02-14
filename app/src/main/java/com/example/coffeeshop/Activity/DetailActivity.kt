package com.example.coffeeshop.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Helper.PriceCalculator
import com.example.coffeeshop.R
import com.example.coffeeshop.Repository.FavoriteRepository
import com.example.coffeeshop.databinding.ActivityDetailBinding
import com.example.project1762.Helper.ManagmentCart
import com.google.firebase.auth.FirebaseAuth

class DetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding
    private lateinit var item: ItemsModel
    private lateinit var managementCart: ManagmentCart
    private val favoriteRepository = FavoriteRepository()
    private val auth = FirebaseAuth.getInstance()
    
    // Variables for price calculation
    private var selectedSize = "Medium"
    private var selectedIce = "Đá chung"
    private var selectedSugar = "Bình thường"
    private var basePrice = 0.0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managementCart = ManagmentCart(this)

        bundle()
        initSizeList()
        initIceOptions()
        initSugarOptions()
        initFavoriteButton()
    }
    
    private fun initFavoriteButton() {
        // Check favorite status from Firebase
        val userId = auth.currentUser?.uid
        if (userId != null) {
            favoriteRepository.isFavorite(userId, item.title) { isFavorite ->
                item.isFavorite = isFavorite
                updateFavoriteIcon()
            }
        } else {
            updateFavoriteIcon()
        }
        
        // Set click listener
        binding.favBtn.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                favoriteRepository.toggleFavorite(userId, item) { isFavorite ->
                    item.isFavorite = isFavorite
                    updateFavoriteIcon()
                    
                    val message = if (isFavorite) "Đã thêm vào yêu thích" else "Đã xóa khỏi yêu thích"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateFavoriteIcon() {
        val iconRes = if (item.isFavorite) {
            R.drawable.ic_favorite_filled
        } else {
            R.drawable.ic_favorite_outline
        }
        binding.favBtn.setImageResource(iconRes)
    }

    private fun initSizeList() {
        binding.apply {
            smallBtn.setOnClickListener {
                selectedSize = "Small"
                updateSizeSelection()
                updateTotalPrice()
            }
            mediumBtn.setOnClickListener {
                selectedSize = "Medium"
                updateSizeSelection()
                updateTotalPrice()
            }
            largeBtn.setOnClickListener {
                selectedSize = "Large"
                updateSizeSelection()
                updateTotalPrice()
            }
        }
    }

    private fun updateSizeSelection() {
        binding.apply {
            smallBtn.setBackgroundResource(if (selectedSize == "Small") R.drawable.stroke_brown_bg else 0)
            mediumBtn.setBackgroundResource(if (selectedSize == "Medium") R.drawable.stroke_brown_bg else 0)
            largeBtn.setBackgroundResource(if (selectedSize == "Large") R.drawable.stroke_brown_bg else 0)
        }
    }

    private fun initIceOptions() {
        binding.apply {
            hotBtn.setOnClickListener {
                selectedIce = "Nóng"
                updateIceSelection()
                updateTotalPrice()
            }
            iceSharedBtn.setOnClickListener {
                selectedIce = "Đá chung"
                updateIceSelection()
                updateTotalPrice()
            }
            iceSeparateBtn.setOnClickListener {
                selectedIce = "Đá riêng"
                updateIceSelection()
                updateTotalPrice()
            }
        }
    }

    private fun updateIceSelection() {
        binding.apply {
            hotBtn.setBackgroundResource(if (selectedIce == "Nóng") R.drawable.stroke_brown_bg else 0)
            iceSharedBtn.setBackgroundResource(if (selectedIce == "Đá chung") R.drawable.stroke_brown_bg else 0)
            iceSeparateBtn.setBackgroundResource(if (selectedIce == "Đá riêng") R.drawable.stroke_brown_bg else 0)
        }
    }

    private fun initSugarOptions() {
        binding.apply {
            noSugarBtn.setOnClickListener {
                selectedSugar = "Không"
                updateSugarSelection()
                updateTotalPrice()
            }
            lessSugarBtn.setOnClickListener {
                selectedSugar = "Ít"
                updateSugarSelection()
                updateTotalPrice()
            }
            normalSugarBtn.setOnClickListener {
                selectedSugar = "Bình thường"
                updateSugarSelection()
                updateTotalPrice()
            }
        }
    }

    private fun updateSugarSelection() {
        binding.apply {
            noSugarBtn.setBackgroundResource(if (selectedSugar == "Không") R.drawable.stroke_brown_bg else 0)
            lessSugarBtn.setBackgroundResource(if (selectedSugar == "Ít") R.drawable.stroke_brown_bg else 0)
            normalSugarBtn.setBackgroundResource(if (selectedSugar == "Bình thường") R.drawable.stroke_brown_bg else 0)
        }
    }

    private fun updateTotalPrice() {
        val quantity = item.numberInCart
        val totalPrice = PriceCalculator.calculateTotalPrice(
            basePrice,
            selectedSize,
            selectedIce,
            selectedSugar,
            quantity
        )
        binding.priceTxt.text = "$${String.format("%.2f", totalPrice)}"
    }

    private fun bundle() {
        binding.apply {
            item=intent.getSerializableExtra("object") as ItemsModel
            
            // Initialize base price and quantity
            basePrice = item.price
            item.numberInCart = 1
            numberItemTxt.text = "1"

            Glide.with(this@DetailActivity)
                .load(item.picUrl[0])
                .into(binding.picMain)

            titleTxt.text=item.title
            descriptionTxt.text=item.description
            ratingTxt.text=item.rating.toString()
            
            // Initialize UI selections
            updateSizeSelection()
            updateIceSelection()
            updateSugarSelection()
            updateTotalPrice()

            addToCartBtn.setOnClickListener {
                item.numberInCart=Integer.valueOf(
                    numberItemTxt.text.toString()
                )
                // Save selected options to item
                item.selectedSize = selectedSize
                item.iceOption = selectedIce
                item.sugarOption = selectedSugar
                
                managementCart.insertItems(item)
                
                // Finish activity to return to previous screen
                finish()
            }
            backBtn.setOnClickListener {
                finish()
            }
            plusCart.setOnClickListener {
                item.numberInCart++
                numberItemTxt.text = item.numberInCart.toString()
                updateTotalPrice()
            }
            minusBtn.setOnClickListener {
                if (item.numberInCart > 1){
                    item.numberInCart--
                    numberItemTxt.text = item.numberInCart.toString()
                    updateTotalPrice()
                }
            }
        }
    }
}
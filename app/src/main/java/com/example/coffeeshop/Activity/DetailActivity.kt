package com.example.coffeeshop.Activity

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.Helper.PriceCalculator
import com.example.coffeeshop.R
import com.example.coffeeshop.Repository.FavoriteRepository
import com.example.coffeeshop.ViewModel.ReviewUiState
import com.example.coffeeshop.ViewModel.ReviewViewModel
import com.example.coffeeshop.databinding.ActivityDetailBinding
import com.example.coffeeshop.ui.compose.CartBubbleData
import com.example.coffeeshop.ui.compose.CartBubbleState
import com.example.project1762.Helper.ManagmentCart
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class DetailActivity : AppCompatActivity() {

    lateinit var binding: ActivityDetailBinding
    private lateinit var item: ItemsModel
    private lateinit var managementCart: ManagmentCart
    private val favoriteRepository = FavoriteRepository()
    private val auth = FirebaseAuth.getInstance()
    private val reviewViewModel: ReviewViewModel by viewModels()

    private var selectedSize = "Medium"
    private var selectedIce = "Đá chung"
    private var selectedSugar = "Bình thường"
    private var basePrice = 0.0

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managementCart = ManagmentCart(this)

        bundle()
        initSizeList()
        initIceOptions()
        initSugarOptions()
        initFavoriteButton()
        initReviewButton()
        observeReviewState()
    }

    // ─── Review ──────────────────────────────────────────────────────────────

    private fun initReviewButton() {
        binding.reviewBtn.setOnClickListener {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showReviewDialog(user.uid, user.displayName ?: "Ẩn danh")
        }
    }

    private fun showReviewDialog(userId: String, userName: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null)

        dialogView.findViewById<TextView>(R.id.dialogItemName).text = item.title

        val stars = listOf(
            dialogView.findViewById<ImageView>(R.id.star1),
            dialogView.findViewById<ImageView>(R.id.star2),
            dialogView.findViewById<ImageView>(R.id.star3),
            dialogView.findViewById<ImageView>(R.id.star4),
            dialogView.findViewById<ImageView>(R.id.star5)
        )
        val hintTxt     = dialogView.findViewById<TextView>(R.id.ratingHintTxt)
        val commentInput = dialogView.findViewById<TextInputEditText>(R.id.commentInput)

        var selectedRating = 0
        val hints = listOf("", "Tệ 😞", "Không hài lòng 😐", "Bình thường 🙂", "Tốt 😊", "Tuyệt vời! 🤩")

        fun updateStars(rating: Int) {
            selectedRating = rating
            stars.forEachIndexed { index, iv ->
                iv.clearColorFilter()
                iv.setColorFilter(if (index < rating) 0xFFFFC107.toInt() else 0xFFD0C4BB.toInt())
            }
            hintTxt.text = hints.getOrElse(rating) { "" }
        }

        stars.forEachIndexed { index, iv -> iv.setOnClickListener { updateStars(index + 1) } }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<android.widget.Button>(R.id.cancelReviewBtn).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.submitReviewBtn).setOnClickListener {
            val comment = commentInput.text?.toString()?.trim() ?: ""
            reviewViewModel.submitReview(
                itemId   = item.title,
                userId   = userId,
                userName = userName,
                rating   = selectedRating,
                comment  = comment
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeReviewState() {
        reviewViewModel.uiState.observe(this) { state ->
            when (state) {
                is ReviewUiState.Loading         -> Unit
                is ReviewUiState.Success         -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    reviewViewModel.resetState()
                }
                is ReviewUiState.AlreadyReviewed -> {
                    Toast.makeText(this, "Bạn đã đánh giá món này rồi!", Toast.LENGTH_SHORT).show()
                    reviewViewModel.resetState()
                }
                is ReviewUiState.Error           -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    reviewViewModel.resetState()
                }
                else -> Unit
            }
        }
    }

    // ─── Favourite ───────────────────────────────────────────────────────────

    private fun initFavoriteButton() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            favoriteRepository.isFavorite(userId, item.title) { isFav ->
                item.isFavorite = isFav
                updateFavoriteIcon()
            }
        } else {
            updateFavoriteIcon()
        }

        binding.favBtn.setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                favoriteRepository.toggleFavorite(uid, item) { isFav ->
                    item.isFavorite = isFav
                    updateFavoriteIcon()
                    val msg = if (isFav) "Đã thêm vào yêu thích" else "Đã xóa khỏi yêu thích"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFavoriteIcon() {
        binding.favBtn.setImageResource(
            if (item.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
        )
    }

    // ─── Size ────────────────────────────────────────────────────────────────

    private fun initSizeList() {
        binding.apply {
            smallBtn.setOnClickListener  { selectedSize = "Small";  updateSizeSelection(); updateTotalPrice() }
            mediumBtn.setOnClickListener { selectedSize = "Medium"; updateSizeSelection(); updateTotalPrice() }
            largeBtn.setOnClickListener  { selectedSize = "Large";  updateSizeSelection(); updateTotalPrice() }
        }
    }

    private fun updateSizeSelection() {
        binding.apply {
            smallBtn.setBackgroundResource(if (selectedSize == "Small")  R.drawable.stroke_brown_bg else 0)
            mediumBtn.setBackgroundResource(if (selectedSize == "Medium") R.drawable.stroke_brown_bg else 0)
            largeBtn.setBackgroundResource(if (selectedSize == "Large")  R.drawable.stroke_brown_bg else 0)
        }
    }

    // ─── Ice ─────────────────────────────────────────────────────────────────

    private fun initIceOptions() {
        binding.apply {
            hotBtn.setOnClickListener         { selectedIce = "Nóng";     updateIceSelection(); updateTotalPrice() }
            iceSharedBtn.setOnClickListener   { selectedIce = "Đá chung"; updateIceSelection(); updateTotalPrice() }
            iceSeparateBtn.setOnClickListener { selectedIce = "Đá riêng"; updateIceSelection(); updateTotalPrice() }
        }
    }

    private fun updateIceSelection() {
        binding.apply {
            hotBtn.setBackgroundResource(if (selectedIce == "Nóng")      R.drawable.stroke_brown_bg else 0)
            iceSharedBtn.setBackgroundResource(if (selectedIce == "Đá chung")  R.drawable.stroke_brown_bg else 0)
            iceSeparateBtn.setBackgroundResource(if (selectedIce == "Đá riêng") R.drawable.stroke_brown_bg else 0)
        }
    }

    // ─── Sugar ───────────────────────────────────────────────────────────────

    private fun initSugarOptions() {
        binding.apply {
            noSugarBtn.setOnClickListener     { selectedSugar = "Không";       updateSugarSelection(); updateTotalPrice() }
            lessSugarBtn.setOnClickListener   { selectedSugar = "Ít";          updateSugarSelection(); updateTotalPrice() }
            normalSugarBtn.setOnClickListener { selectedSugar = "Bình thường"; updateSugarSelection(); updateTotalPrice() }
        }
    }

    private fun updateSugarSelection() {
        binding.apply {
            noSugarBtn.setBackgroundResource(if (selectedSugar == "Không")        R.drawable.stroke_brown_bg else 0)
            lessSugarBtn.setBackgroundResource(if (selectedSugar == "Ít")         R.drawable.stroke_brown_bg else 0)
            normalSugarBtn.setBackgroundResource(if (selectedSugar == "Bình thường") R.drawable.stroke_brown_bg else 0)
        }
    }

    // ─── Price ───────────────────────────────────────────────────────────────

    private fun updateTotalPrice() {
        val total = PriceCalculator.calculateTotalPrice(
            basePrice, selectedSize, selectedIce, selectedSugar, item.numberInCart
        )
        binding.priceTxt.text = "$${String.format("%.2f", total)}"
    }

    // ─── Bundle ──────────────────────────────────────────────────────────────

    private fun bundle() {
        binding.apply {
            item = intent.getSerializableExtra("object") as ItemsModel

            basePrice = item.price
            item.numberInCart = 1
            numberItemTxt.text = "1"

            Glide.with(this@DetailActivity).load(item.picUrl[0]).into(binding.picMain)

            titleTxt.text       = item.title
            descriptionTxt.text = item.description
            ratingTxt.text      = item.rating.toString()

            updateSizeSelection()
            updateIceSelection()
            updateSugarSelection()
            updateTotalPrice()

            addToCartBtn.setOnClickListener {
                item.numberInCart = Integer.valueOf(numberItemTxt.text.toString())
                item.selectedSize = selectedSize
                item.iceOption    = selectedIce
                item.sugarOption  = selectedSugar
                managementCart.insertItems(item)

                val thumb = if (item.picUrl.isNotEmpty()) item.picUrl[0] else ""
                CartBubbleState.setPending(CartBubbleData(drinkName = item.title, thumbnailUrl = thumb))
                finish()
            }
            backBtn.setOnClickListener { finish() }
            plusCart.setOnClickListener {
                item.numberInCart++
                numberItemTxt.text = item.numberInCart.toString()
                updateTotalPrice()
            }
            minusBtn.setOnClickListener {
                if (item.numberInCart > 1) {
                    item.numberInCart--
                    numberItemTxt.text = item.numberInCart.toString()
                    updateTotalPrice()
                }
            }
        }
    }
}
package com.example.coffeeshop.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.Domain.FilterOptions
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.FragmentFilterBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * FilterBottomSheet – BottomSheetDialogFragment cho phép user chọn:
 *  - Sắp xếp theo giá / rating / mặc định
 *  - Lọc theo loại (category)
 *  - Khoảng giá (RangeSlider)
 *
 * Sử dụng:
 *   FilterBottomSheet.newInstance(currentOptions, categories) { options ->
 *       viewModel.applyFilter(options)
 *   }
 */
class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var currentOptions: FilterOptions = FilterOptions()
    private var categories: List<CategoryModel> = emptyList()
    private var onApply: ((FilterOptions) -> Unit)? = null

    // State tạm
    private var selectedCategoryId: String = ""

    companion object {
        fun newInstance(
            current: FilterOptions,
            categories: List<CategoryModel>,
            onApply: (FilterOptions) -> Unit
        ): FilterBottomSheet {
            return FilterBottomSheet().also {
                it.currentOptions  = current
                it.categories      = categories
                it.onApply         = onApply
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedCategoryId = currentOptions.categoryId

        restoreSortState()
        buildCategoryChips()
        restorePriceRange()
        setupListeners()
    }

    // ─────────────────────────────────────────────
    // Restore UI từ currentOptions
    // ─────────────────────────────────────────────

    private fun restoreSortState() {
        val radioId = when (currentOptions.sortBy) {
            FilterOptions.SortBy.PRICE_ASC  -> R.id.rbPriceAsc
            FilterOptions.SortBy.PRICE_DESC -> R.id.rbPriceDesc
            FilterOptions.SortBy.RATING_DESC -> R.id.rbRatingDesc
            else                             -> R.id.rbDefault
        }
        binding.rgSortBy.check(radioId)
    }

    private fun buildCategoryChips() {
        val container = binding.categoryChipGroup as LinearLayout

        // "Tất cả" chip
        container.addView(createChip("Tất cả", ""))

        categories.forEach { cat ->
            container.addView(createChip(cat.title, cat.id.toString()))
        }
    }

    private fun createChip(label: String, catId: String): TextView {
        val chip = layoutInflater.inflate(
            R.layout.chip_filter_item, binding.categoryChipGroup as LinearLayout, false
        ) as TextView

        chip.text = label
        chip.tag  = catId
        chip.setOnClickListener { v ->
            selectedCategoryId = v.tag as String
            // Update visual: highlight selected
            val parent = binding.categoryChipGroup as LinearLayout
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i) as? TextView ?: continue
                val isSelected = child.tag == selectedCategoryId
                child.isSelected = isSelected
                child.setTextColor(
                    if (isSelected)
                        resources.getColor(R.color.white, null)
                    else
                        resources.getColor(R.color.darkBrown, null)
                )
            }
        }

        // Initial selection state
        val isSelected = catId == selectedCategoryId
        chip.isSelected = isSelected
        chip.setTextColor(
            if (isSelected)
                resources.getColor(R.color.white, null)
            else
                resources.getColor(R.color.darkBrown, null)
        )

        return chip
    }

    private fun restorePriceRange() {
        val min = currentOptions.minPrice.toFloat().coerceIn(0f, 50f)
        val max = if (currentOptions.maxPrice == Double.MAX_VALUE) 50f
                  else currentOptions.maxPrice.toFloat().coerceIn(min, 50f)
        binding.priceRangeSlider.setValues(min, max)
        updatePriceLabel(min, max)
    }

    // ─────────────────────────────────────────────
    // Listeners
    // ─────────────────────────────────────────────

    private fun setupListeners() {
        binding.priceRangeSlider.addOnChangeListener { slider, _, _ ->
            val vals = slider.values
            updatePriceLabel(vals[0], vals[1])
        }

        binding.btnReset.setOnClickListener {
            resetAll()
        }

        binding.btnApplyFilter.setOnClickListener {
            val options = buildFilterOptions()
            onApply?.invoke(options)
            dismiss()
        }
    }

    private fun updatePriceLabel(min: Float, max: Float) {
        val maxLabel = if (max >= 50f) "$50+" else "$${"%.0f".format(max)}"
        binding.tvPriceRange.text = "$${"%.0f".format(min)} – $maxLabel"
    }

    private fun resetAll() {
        binding.rgSortBy.check(R.id.rbDefault)
        selectedCategoryId = ""
        buildCategoryChips()  // rebuild để clear selection
        binding.priceRangeSlider.setValues(0f, 50f)
        updatePriceLabel(0f, 50f)
    }

    // ─────────────────────────────────────────────
    // Build FilterOptions từ UI
    // ─────────────────────────────────────────────

    private fun buildFilterOptions(): FilterOptions {
        val sortBy = when (binding.rgSortBy.checkedRadioButtonId) {
            R.id.rbPriceAsc  -> FilterOptions.SortBy.PRICE_ASC
            R.id.rbPriceDesc -> FilterOptions.SortBy.PRICE_DESC
            R.id.rbRatingDesc -> FilterOptions.SortBy.RATING_DESC
            else              -> FilterOptions.SortBy.DEFAULT
        }

        val sliderVals = binding.priceRangeSlider.values
        val minPrice = sliderVals[0].toDouble()
        val maxPrice = if (sliderVals[1] >= 50f) Double.MAX_VALUE else sliderVals[1].toDouble()

        return FilterOptions(
            sortBy     = sortBy,
            categoryId = selectedCategoryId,
            minPrice   = minPrice,
            maxPrice   = maxPrice
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

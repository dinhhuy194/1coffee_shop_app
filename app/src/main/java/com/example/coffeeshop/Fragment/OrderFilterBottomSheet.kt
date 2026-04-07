package com.example.coffeeshop.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.BottomSheetOrderFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

enum class OrderSortType {
    DATE_NEWEST,      // Mới nhất
    DATE_OLDEST,      // Cũ nhất  
    PRICE_HIGH_LOW,   // Giá cao → thấp
    PRICE_LOW_HIGH    // Giá thấp → cao
}

enum class DateRangeFilter {
    ALL_TIME,         // Tất cả
    TODAY,            // Hôm nay
    LAST_7_DAYS,      // 7 ngày qua
    LAST_30_DAYS      // 30 ngày qua
}

data class OrderFilterOptions(
    val sortType: OrderSortType = OrderSortType.DATE_NEWEST,
    val dateRange: DateRangeFilter = DateRangeFilter.ALL_TIME
)

class OrderFilterBottomSheet(
    private val currentOptions: OrderFilterOptions = OrderFilterOptions(),
    private val onFilterSelected: (OrderFilterOptions) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOrderFilterBinding? = null
    private val binding get() = _binding!!
    
    private var selectedSortType: OrderSortType = currentOptions.sortType
    private var selectedDateRange: DateRangeFilter = currentOptions.dateRange

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetOrderFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        // Set initial sort selection
        binding.sortRadioGroup.check(
            when (currentOptions.sortType) {
                OrderSortType.DATE_NEWEST -> R.id.radioNewest
                OrderSortType.DATE_OLDEST -> R.id.radioOldest
                OrderSortType.PRICE_HIGH_LOW -> R.id.radioPriceHighLow
                OrderSortType.PRICE_LOW_HIGH -> R.id.radioPriceLowHigh
            }
        )
        
        // Set initial date range selection
        binding.dateRangeRadioGroup.check(
            when (currentOptions.dateRange) {
                DateRangeFilter.ALL_TIME -> R.id.radioAllTime
                DateRangeFilter.TODAY -> R.id.radioToday
                DateRangeFilter.LAST_7_DAYS -> R.id.radioLast7Days
                DateRangeFilter.LAST_30_DAYS -> R.id.radioLast30Days
            }
        )
    }

    private fun setupListeners() {
        // Sort selection
        binding.sortRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedSortType = when (checkedId) {
                R.id.radioNewest -> OrderSortType.DATE_NEWEST
                R.id.radioOldest -> OrderSortType.DATE_OLDEST
                R.id.radioPriceHighLow -> OrderSortType.PRICE_HIGH_LOW
                R.id.radioPriceLowHigh -> OrderSortType.PRICE_LOW_HIGH
                else -> OrderSortType.DATE_NEWEST
            }
        }
        
        // Date range selection
        binding.dateRangeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedDateRange = when (checkedId) {
                R.id.radioAllTime -> DateRangeFilter.ALL_TIME
                R.id.radioToday -> DateRangeFilter.TODAY
                R.id.radioLast7Days -> DateRangeFilter.LAST_7_DAYS
                R.id.radioLast30Days -> DateRangeFilter.LAST_30_DAYS
                else -> DateRangeFilter.ALL_TIME
            }
        }

        // Apply button
        binding.applyBtn.setOnClickListener {
            onFilterSelected(OrderFilterOptions(selectedSortType, selectedDateRange))
            dismiss()
        }

        // Reset button
        binding.resetBtn.setOnClickListener {
            onFilterSelected(OrderFilterOptions())
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

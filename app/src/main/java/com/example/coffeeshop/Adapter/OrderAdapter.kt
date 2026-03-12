package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.R
import com.example.coffeeshop.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter hiển thị danh sách đơn hàng trong Order History.
 *
 * Mỗi đơn hàng hiển thị:
 * - Header: orderId, ngày, số lượng items, tổng tiền, trạng thái đơn
 * - Expanded: danh sách items, trạng thái thanh toán, nút "Thanh toán ngay" (nếu unpaid)
 *
 * @param orders         Danh sách đơn hàng
 * @param onPayNowClick  Callback khi bấm nút "Thanh toán ngay" (truyền Order)
 */
class OrderAdapter(
    private val orders: List<Order>,
    private val onPayNowClick: ((Order) -> Unit)? = null
) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {
    
    // Theo dõi trạng thái expand/collapse cho mỗi đơn hàng
    private val expandedStates = mutableMapOf<String, Boolean>()
    
    inner class ViewHolder(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        val isExpanded = expandedStates[order.orderId] ?: false
        
        holder.binding.apply {
            // ===== HEADER: Thông tin cơ bản =====
            orderIdTxt.text = order.orderId
            totalAmountTxt.text = "$${String.format("%.2f", order.totalAmount)}"
            
            // Định dạng ngày giờ
            val date = Date(order.timestamp)
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            dateTxt.text = dateFormat.format(date)
            
            // Số lượng items
            val itemCount = order.items.sumOf { it.quantity }
            val itemText = if (itemCount == 1) "1 item" else "$itemCount items"
            itemCountTxt.text = itemText
            
            // ===== TRẠNG THÁI ĐƠN HÀNG (orderStatus) =====
            // Hiển thị trạng thái tổng hợp dựa trên cả orderStatus và paymentStatus
            val displayStatus = getDisplayStatus(order)
            statusTxt.text = displayStatus.first
            statusTxt.setBackgroundResource(displayStatus.second)
            
            // ===== EXPAND / COLLAPSE =====
            val rotation = if (isExpanded) 180f else 0f
            expandIcon.animate()
                .rotation(rotation)
                .setDuration(200)
                .start()
            
            expandIcon.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less 
                else R.drawable.ic_expand_more
            )
            
            // Hiện/ẩn phần chi tiết khi expand
            if (isExpanded) {
                divider.visibility = View.VISIBLE
                itemsRecyclerView.visibility = View.VISIBLE
                paymentInfoLayout.visibility = View.VISIBLE
                
                // Setup nested RecyclerView cho danh sách items
                itemsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
                itemsRecyclerView.adapter = OrderItemAdapter(order.items)
                
                // ===== THÔNG TIN THANH TOÁN =====
                // Hiển thị phương thức thanh toán
                val methodIcon = if (order.paymentMethod == "VNPAY") "🏦" else "💵"
                paymentMethodTxt.text = "$methodIcon ${order.paymentMethod}"
                
                // Hiển thị trạng thái thanh toán (paymentStatus)
                setupPaymentStatus(this, order)
            } else {
                divider.visibility = View.GONE
                itemsRecyclerView.visibility = View.GONE
                paymentInfoLayout.visibility = View.GONE
            }
            
            // Click listener cho expand/collapse
            orderHeaderLayout.setOnClickListener {
                expandedStates[order.orderId] = !isExpanded
                notifyItemChanged(position)
            }
        }
    }

    /**
     * Xác định trạng thái hiển thị cho badge dựa trên orderStatus + paymentStatus.
     *
     * Logic:
     * - pending + unpaid → "CHỜ THANH TOÁN" (vàng)
     * - pending + paid → "ĐÃ THANH TOÁN" (xanh dương)
     * - completed → "HOÀN THÀNH" (xanh lá)
     * - cancelled → "ĐÃ HỦY" (đỏ)
     *
     * @return Pair<String, Int> (text hiển thị, drawable background resource)
     */
    private fun getDisplayStatus(order: Order): Pair<String, Int> {
        return when {
            order.orderStatus == "completed" -> 
                "HOÀN THÀNH" to R.drawable.bg_status_completed
            order.orderStatus == "cancelled" -> 
                "ĐÃ HỦY" to R.drawable.bg_status_cancelled
            order.paymentStatus == "paid" -> 
                "ĐÃ THANH TOÁN" to R.drawable.bg_status_paid
            order.paymentStatus == "failed" -> 
                "THANH TOÁN LỖI" to R.drawable.bg_status_cancelled
            else -> 
                "CHỜ THANH TOÁN" to R.drawable.bg_status_pending
        }
    }

    /**
     * Thiết lập hiển thị trạng thái thanh toán và nút "Thanh toán ngay".
     *
     * - unpaid + VNPAY → hiện nút "Thanh toán ngay"
     * - paid → hiện badge "PAID" xanh
     * - failed → hiện badge "FAILED" đỏ
     */
    private fun setupPaymentStatus(binding: ItemOrderBinding, order: Order) {
        binding.apply {
            when (order.paymentStatus) {
                "paid" -> {
                    paymentStatusTxt.text = "PAID"
                    paymentStatusTxt.setBackgroundResource(R.drawable.bg_status_paid)
                    payNowBtn.visibility = View.GONE
                }
                "failed" -> {
                    paymentStatusTxt.text = "FAILED"
                    paymentStatusTxt.setBackgroundResource(R.drawable.bg_status_cancelled)
                    // Cho phép thanh toán lại
                    payNowBtn.visibility = View.VISIBLE
                    payNowBtn.text = "Thử lại"
                    payNowBtn.setOnClickListener {
                        onPayNowClick?.invoke(order)
                    }
                }
                else -> { // unpaid
                    paymentStatusTxt.text = "UNPAID"
                    paymentStatusTxt.setBackgroundResource(R.drawable.bg_status_pending)
                    
                    // Chỉ hiện nút "Thanh toán ngay" cho đơn VNPAY chưa thanh toán
                    if (order.paymentMethod == "VNPAY" && order.orderStatus == "pending") {
                        payNowBtn.visibility = View.VISIBLE
                        payNowBtn.text = "Thanh toán ngay"
                        payNowBtn.setOnClickListener {
                            onPayNowClick?.invoke(order)
                        }
                    } else {
                        payNowBtn.visibility = View.GONE
                    }
                }
            }
        }
    }
    
    override fun getItemCount() = orders.size
}

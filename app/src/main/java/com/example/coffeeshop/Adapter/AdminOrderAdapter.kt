package com.example.coffeeshop.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.AdapterView
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeeshop.Model.Order
import com.example.coffeeshop.R
import java.text.SimpleDateFormat
import java.util.*

class AdminOrderAdapter(
    private var items: List<Order> = emptyList(),
    private val onStatusChange: (String, String) -> Unit // orderId, newStatus
) : RecyclerView.Adapter<AdminOrderAdapter.ViewHolder>() {

    fun updateData(newItems: List<Order>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = items[position]
        val ctx = holder.itemView.context

        holder.orderIdTxt.text = "#${order.orderId.takeLast(6).uppercase()}"
        holder.customerTxt.text = "👤 ${order.userName.ifBlank { order.userId.take(8) }}"
        holder.amountTxt.text = "$${order.totalAmount}"

        // Format timestamp
        val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        holder.timeTxt.text = "🕐 ${dateFormat.format(Date(order.timestamp))}"

        // Status chip color
        val statusBg = when (order.orderStatus) {
            Order.STATUS_PENDING -> R.drawable.bg_status_pending
            Order.STATUS_PREPARING -> R.drawable.bg_status_preparing
            Order.STATUS_READY -> R.drawable.bg_status_ready
            Order.STATUS_COMPLETED -> R.drawable.bg_status_completed
            Order.STATUS_CANCELLED -> R.drawable.bg_status_cancelled
            else -> R.drawable.bg_status_pending
        }
        holder.statusTxt.text = Order.getStatusLabel(order.orderStatus)
        holder.statusTxt.setBackgroundResource(statusBg)

        // Status spinner
        val statusLabels = Order.ALL_STATUSES.map { Order.getStatusLabel(it) }
        val spinnerAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, statusLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.statusSpinner.adapter = spinnerAdapter

        // Set current selection without triggering listener
        val currentIndex = Order.ALL_STATUSES.indexOf(order.orderStatus)
        holder.statusSpinner.setSelection(currentIndex.coerceAtLeast(0))

        holder.statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val newStatus = Order.ALL_STATUSES[pos]
                if (newStatus != order.orderStatus) {
                    onStatusChange(order.orderId, newStatus)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderIdTxt: TextView = view.findViewById(R.id.orderIdTxt)
        val statusTxt: TextView = view.findViewById(R.id.statusTxt)
        val customerTxt: TextView = view.findViewById(R.id.customerTxt)
        val amountTxt: TextView = view.findViewById(R.id.amountTxt)
        val timeTxt: TextView = view.findViewById(R.id.timeTxt)
        val statusSpinner: Spinner = view.findViewById(R.id.statusSpinner)
    }
}

package com.example.coffeeshop.Activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Adapter.RedeemedVoucherAdapter
import com.example.coffeeshop.ViewModel.MyVouchersViewModel
import com.example.coffeeshop.databinding.ActivityMyVouchersBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class MyVouchersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyVouchersBinding
    private val viewModel: MyVouchersViewModel by viewModels()
    private lateinit var adapter: RedeemedVoucherAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyVouchersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeData()
        loadData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = RedeemedVoucherAdapter(emptyList()) { voucher ->
            // Xác nhận sử dụng voucher
            MaterialAlertDialogBuilder(this)
                .setTitle("Sử dụng voucher")
                .setMessage("Bạn muốn sử dụng \"${voucher.name}\"?")
                .setPositiveButton("Sử dụng") { _, _ ->
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton
                    viewModel.markAsUsed(userId, voucher.id)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        binding.voucherRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.voucherRecyclerView.adapter = adapter
    }

    private fun observeData() {
        viewModel.vouchers.observe(this) { vouchers ->
            adapter.updateList(vouchers)

            if (vouchers.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.voucherRecyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.voucherRecyclerView.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.actionState.observe(this) { state ->
            when (state) {
                is MyVouchersViewModel.ActionState.Success -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                }
                is MyVouchersViewModel.ActionState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                }
                else -> { /* Idle / Loading */ }
            }
        }
    }

    private fun loadData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            viewModel.loadVouchers(userId)
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

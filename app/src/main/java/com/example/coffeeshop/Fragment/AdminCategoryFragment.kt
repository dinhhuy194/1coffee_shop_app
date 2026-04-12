package com.example.coffeeshop.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Adapter.AdminCategoryAdapter
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.AdminViewModel
import com.example.coffeeshop.databinding.FragmentAdminCategoryBinding
import com.google.firebase.database.FirebaseDatabase

class AdminCategoryFragment : Fragment() {
    private var _binding: FragmentAdminCategoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by activityViewModels()
    private lateinit var adapter: AdminCategoryAdapter

    // Track hidden state from raw data
    private val hiddenMap = mutableMapOf<Int, Boolean>()
    private var showHiddenTab = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminCategoryAdapter(
            onEdit = { cat -> showEditDialog(cat) },
            onToggle = { cat, hide ->
                if (hide) viewModel.softDeleteCategory(cat.id)
                else viewModel.restoreCategory(cat.id)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Tab listener
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                showHiddenTab = tab?.position == 1
                refreshList()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        binding.fabAdd.setOnClickListener { showAddDialog() }

        observeViewModel()
        loadCategoriesWithHiddenState()
    }

    private fun loadCategoriesWithHiddenState() {
        // Load raw data to track hidden state
        binding.progressBar.visibility = View.VISIBLE
        FirebaseDatabase.getInstance().getReference("Category").get()
            .addOnSuccessListener { snapshot ->
                hiddenMap.clear()
                val cats = mutableListOf<CategoryModel>()
                for (child in snapshot.children) {
                    val cat = child.getValue(CategoryModel::class.java) ?: continue
                    val isHidden = child.child("isHidden").getValue(Boolean::class.java) ?: false
                    hiddenMap[cat.id] = isHidden
                    cats.add(cat)
                }
                adapter.updateData(cats, hiddenMap, showHiddenTab)
                binding.progressBar.visibility = View.GONE
                binding.emptyTxt.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
    }

    private fun refreshList() {
        loadCategoriesWithHiddenState()
    }

    private fun observeViewModel() {
        viewModel.toastMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
                loadCategoriesWithHiddenState()
            }
        }
    }

    private fun showAddDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Tên danh mục"
            setPadding(40, 20, 40, 20)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("➕ Thêm danh mục mới")
            .setView(editText)
            .setPositiveButton("Thêm") { _, _ ->
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.addCategory(title)
                } else {
                    Toast.makeText(requireContext(), "Tên không được trống", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditDialog(cat: CategoryModel) {
        val editText = EditText(requireContext()).apply {
            hint = "Tên danh mục"
            setText(cat.title)
            setPadding(40, 20, 40, 20)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("✏️ Sửa danh mục")
            .setView(editText)
            .setPositiveButton("Lưu") { _, _ ->
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.updateCategory(cat.id, title)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

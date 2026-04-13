package com.example.coffeeshop.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Adapter.AdminItemAdapter
import com.example.coffeeshop.Domain.ItemsModel
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.AdminViewModel
import com.example.coffeeshop.databinding.FragmentAdminItemsBinding
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.io.FileOutputStream

class AdminItemsFragment : Fragment() {
    private var _binding: FragmentAdminItemsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by activityViewModels()
    private lateinit var adapter: AdminItemAdapter

    private var showHiddenTab = false
    private var allItemsRaw = listOf<Pair<String, ItemsModel>>()
    private var hiddenCategoryIds = emptySet<String>()
    private var pendingImageUri: Uri? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                pendingImageUri = uri
                // Upload to Cloudinary
                val file = uriToFile(uri)
                if (file != null) {
                    viewModel.uploadImage(file)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminItemAdapter(
            onEdit = { itemKey, item -> showEditDialog(itemKey, item) },
            onToggle = { itemKey, hide ->
                val item = allItemsRaw.firstOrNull { it.first == itemKey }?.second
                val categoryHidden = item?.categoryId?.let { hiddenCategoryIds.contains(it) } == true
                if (!hide && categoryHidden) {
                    Toast.makeText(
                        requireContext(),
                        "Danh mục của sản phẩm đang bị ẩn, hãy hiện danh mục trước",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Cập nhật local state ngay lập tức để UI phản hồi nhanh
                    allItemsRaw = allItemsRaw.map { (key, it) ->
                        if (key == itemKey) key to ItemsModel(
                            title = it.title,
                            description = it.description,
                            picUrl = it.picUrl,
                            price = it.price,
                            rating = it.rating,
                            numberInCart = it.numberInCart,
                            extra = it.extra,
                            categoryId = it.categoryId,
                            selectedSize = it.selectedSize,
                            iceOption = it.iceOption,
                            sugarOption = it.sugarOption,
                            isFavorite = it.isFavorite,
                            isHidden = hide
                        )
                        else key to it
                    }
                    filterAndDisplay()

                    if (hide) viewModel.softDeleteItem(itemKey)
                    else viewModel.restoreItem(itemKey)
                }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                showHiddenTab = tab?.position == 1
                filterAndDisplay()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // Search
        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndDisplay()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.fabAdd.setOnClickListener { showAddDialog() }

        observeViewModel()
        loadItems()
    }

    private fun loadItems() {
        binding.progressBar.visibility = View.VISIBLE
        FirebaseDatabase.getInstance().getReference("Category").get()
            .addOnSuccessListener { categorySnapshot ->
                hiddenCategoryIds = categorySnapshot.children
                    .mapNotNull { child ->
                        val isHidden = child.child("isHidden").getValue(Boolean::class.java) ?: false
                        val id = child.child("id").getValue(Long::class.java)?.toInt()
                        if (isHidden && id != null) id.toString() else null
                    }
                    .toSet()

                FirebaseDatabase.getInstance().getReference("Items").get()
                    .addOnSuccessListener { snapshot ->
                        val list = mutableListOf<Pair<String, ItemsModel>>()
                        snapshot.children.forEach { child ->
                            val key = child.key ?: return@forEach
                            child.getValue(ItemsModel::class.java)?.let { item ->
                                list.add(key to item)
                            }
                        }
                        allItemsRaw = list
                        filterAndDisplay()
                        binding.progressBar.visibility = View.GONE
                    }
                    .addOnFailureListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Lỗi tải dữ liệu sản phẩm", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu danh mục", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterAndDisplay() {
        val query = binding.searchEdit.text.toString().lowercase()
        val filtered = allItemsRaw.filter { (_, item) ->
            val effectiveHidden = item.isHidden || hiddenCategoryIds.contains(item.categoryId)
            val matchHidden = effectiveHidden == showHiddenTab
            val matchSearch = query.isEmpty() || item.title.lowercase().contains(query)
            matchHidden && matchSearch
        }
        adapter.updateData(filtered)
        binding.emptyTxt.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun observeViewModel() {
        viewModel.toastMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
                loadItems()
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_item_form, null)

        val titleEdit = dialogView.findViewById<EditText>(R.id.editTitle)
        val priceEdit = dialogView.findViewById<EditText>(R.id.editPrice)
        val ratingEdit = dialogView.findViewById<EditText>(R.id.editRating)
        val descEdit = dialogView.findViewById<EditText>(R.id.editDescription)
        val extraEdit = dialogView.findViewById<EditText>(R.id.editExtra)
        val categoryEdit = dialogView.findViewById<EditText>(R.id.editCategoryId)
        val picUrlEdit = dialogView.findViewById<EditText>(R.id.editPicUrl)
        val btnPickImage = dialogView.findViewById<Button>(R.id.btnPickImage)

        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
        }

        viewModel.uploadedUrl.observe(viewLifecycleOwner) { url ->
            url?.let {
                picUrlEdit.setText(it)
                viewModel.clearUploadedUrl()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("➕ Thêm sản phẩm mới")
            .setView(dialogView)
            .setPositiveButton("Thêm") { _, _ ->
                val title = titleEdit.text.toString().trim()
                val price = priceEdit.text.toString().toDoubleOrNull() ?: 0.0
                val rating = ratingEdit.text.toString().toDoubleOrNull() ?: 0.0
                val desc = descEdit.text.toString().trim()
                val extra = extraEdit.text.toString().trim()
                val catId = categoryEdit.text.toString().trim()
                val picUrl = picUrlEdit.text.toString().trim()

                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "Tên sản phẩm không được trống", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val item = ItemsModel(
                    title = title,
                    price = price,
                    rating = rating,
                    description = desc,
                    extra = extra,
                    categoryId = catId,
                    picUrl = if (picUrl.isNotEmpty()) arrayListOf(picUrl) else arrayListOf()
                )
                viewModel.addItem(item)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditDialog(itemKey: String, item: ItemsModel) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_item_form, null)

        val titleEdit = dialogView.findViewById<EditText>(R.id.editTitle).apply { setText(item.title) }
        val priceEdit = dialogView.findViewById<EditText>(R.id.editPrice).apply { setText(item.price.toString()) }
        val ratingEdit = dialogView.findViewById<EditText>(R.id.editRating).apply { setText(item.rating.toString()) }
        val descEdit = dialogView.findViewById<EditText>(R.id.editDescription).apply { setText(item.description) }
        val extraEdit = dialogView.findViewById<EditText>(R.id.editExtra).apply { setText(item.extra) }
        val categoryEdit = dialogView.findViewById<EditText>(R.id.editCategoryId).apply { setText(item.categoryId) }
        val picUrlEdit = dialogView.findViewById<EditText>(R.id.editPicUrl).apply {
            setText(item.picUrl.firstOrNull() ?: "")
        }
        val btnPickImage = dialogView.findViewById<Button>(R.id.btnPickImage)

        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
        }

        viewModel.uploadedUrl.observe(viewLifecycleOwner) { url ->
            url?.let {
                picUrlEdit.setText(it)
                viewModel.clearUploadedUrl()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("✏️ Sửa sản phẩm")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val updatedItem = ItemsModel(
                    title = titleEdit.text.toString().trim(),
                    price = priceEdit.text.toString().toDoubleOrNull() ?: 0.0,
                    rating = ratingEdit.text.toString().toDoubleOrNull() ?: 0.0,
                    description = descEdit.text.toString().trim(),
                    extra = extraEdit.text.toString().trim(),
                    categoryId = categoryEdit.text.toString().trim(),
                    picUrl = arrayListOf(picUrlEdit.text.toString().trim()).apply {
                        removeAll { it.isEmpty() }
                    }
                )
                viewModel.updateItem(itemKey, updatedItem)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

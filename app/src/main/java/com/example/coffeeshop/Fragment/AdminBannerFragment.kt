package com.example.coffeeshop.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coffeeshop.Adapter.AdminBannerAdapter
import com.example.coffeeshop.Domain.BannerModel
import com.example.coffeeshop.R
import com.example.coffeeshop.ViewModel.AdminViewModel
import com.example.coffeeshop.databinding.FragmentAdminBannerBinding
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.io.FileOutputStream

class AdminBannerFragment : Fragment() {
    private var _binding: FragmentAdminBannerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by activityViewModels()
    private lateinit var adapter: AdminBannerAdapter

    private var showHiddenTab = false
    private var allBannersRaw = listOf<Pair<Int, BannerModel>>()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val file = uriToFile(uri)
                if (file != null) viewModel.uploadImage(file)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminBannerAdapter(
            onEdit = { index, banner -> showEditDialog(index, banner) },
            onToggle = { index, hide ->
                if (hide) viewModel.softDeleteBanner(index)
                else viewModel.restoreBanner(index)
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

        binding.fabAdd.setOnClickListener { showAddDialog() }

        observeViewModel()
        loadBanners()
    }

    private fun loadBanners() {
        binding.progressBar.visibility = View.VISIBLE
        FirebaseDatabase.getInstance().getReference("Banner").get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<Pair<Int, BannerModel>>()
                snapshot.children.forEachIndexed { index, child ->
                    child.getValue(BannerModel::class.java)?.let { banner ->
                        list.add(index to banner)
                    }
                }
                allBannersRaw = list
                filterAndDisplay()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterAndDisplay() {
        val filtered = allBannersRaw.filter { (_, banner) ->
            banner.isHidden == showHiddenTab
        }
        adapter.updateData(filtered)
        binding.emptyTxt.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun observeViewModel() {
        viewModel.toastMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
                loadBanners()
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_banner_form, null)

        val urlEdit = dialogView.findViewById<EditText>(R.id.editUrl)
        val titleEdit = dialogView.findViewById<EditText>(R.id.editTitle)
        val btnPickImage = dialogView.findViewById<Button>(R.id.btnPickImage)

        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
        }

        viewModel.uploadedUrl.observe(viewLifecycleOwner) { url ->
            url?.let {
                urlEdit.setText(it)
                viewModel.clearUploadedUrl()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("➕ Thêm banner mới")
            .setView(dialogView)
            .setPositiveButton("Thêm") { _, _ ->
                val url = urlEdit.text.toString().trim()
                val title = titleEdit.text.toString().trim()
                if (url.isEmpty()) {
                    Toast.makeText(requireContext(), "URL ảnh không được trống", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.addBanner(url, title)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditDialog(index: Int, banner: BannerModel) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_banner_form, null)

        val urlEdit = dialogView.findViewById<EditText>(R.id.editUrl).apply { setText(banner.url) }
        val titleEdit = dialogView.findViewById<EditText>(R.id.editTitle).apply { setText(banner.title) }
        val btnPickImage = dialogView.findViewById<Button>(R.id.btnPickImage)

        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
        }

        viewModel.uploadedUrl.observe(viewLifecycleOwner) { url ->
            url?.let {
                urlEdit.setText(it)
                viewModel.clearUploadedUrl()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("✏️ Sửa banner")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val url = urlEdit.text.toString().trim()
                val title = titleEdit.text.toString().trim()
                if (url.isEmpty()) {
                    Toast.makeText(requireContext(), "URL không được trống", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.updateBanner(index, url, title)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output -> inputStream.copyTo(output) }
            file
        } catch (e: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

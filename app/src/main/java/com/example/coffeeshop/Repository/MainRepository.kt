package com.example.coffeeshop.Repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.coffeeshop.Domain.BannerModel
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.Domain.ItemsModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import android.util.Log

class MainRepository {
    private val firebaseDatabase = FirebaseDatabase.getInstance();

    private fun hiddenCategoryIdsFrom(snapshot: DataSnapshot): Set<String> {
        val hiddenIds = mutableSetOf<String>()
        for (catChild in snapshot.children) {
            val isHidden = catChild.child("isHidden").getValue(Boolean::class.java) ?: false
            if (isHidden) {
                val id = catChild.child("id").getValue(Long::class.java)?.toInt()
                if (id != null) hiddenIds.add(id.toString())
            }
        }
        return hiddenIds
    }

    fun loadBanner(): MutableLiveData<MutableList<BannerModel>> {
        val listData = MutableLiveData<MutableList<BannerModel>>()
        val ref = firebaseDatabase.getReference("Banner");
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BannerModel>();
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(BannerModel::class.java)
                    // Chỉ thêm banner không bị ẩn (isHidden = false hoặc null)
                    item?.let { if (!it.isHidden) list.add(it) }
                }
                listData.value = list;
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainRepository", "loadBanner onCancelled: " + error.message)
            }

        })
        return listData;
    }

    fun loadCategory(): MutableLiveData<MutableList<CategoryModel>> {
        val listData = MutableLiveData<MutableList<CategoryModel>>()
        val ref = firebaseDatabase.getReference("Category");
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CategoryModel>();
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(CategoryModel::class.java)
                    item?.let {
                        if (!it.isHidden) list.add(it)
                    }
                }
                listData.value = list;
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainRepository", "loadCategory onCancelled: " + error.message)
            }

        })
        return listData;
    }

    fun loadPopular(): MutableLiveData<MutableList<ItemsModel>> {
        val listData = MutableLiveData<MutableList<ItemsModel>>()
        val popularRef = firebaseDatabase.getReference("Popular")
        val categoryRef = firebaseDatabase.getReference("Category")

        popularRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(popularSnapshot: DataSnapshot) {
                categoryRef.get()
                    .addOnSuccessListener { categorySnapshot ->
                        val hiddenCategoryIds = hiddenCategoryIdsFrom(categorySnapshot)
                        val list = mutableListOf<ItemsModel>()
                        for (childSnapshot in popularSnapshot.children) {
                            val item = childSnapshot.getValue(ItemsModel::class.java)
                            item?.let {
                                val categoryVisible = !hiddenCategoryIds.contains(it.categoryId)
                                if (!it.isHidden && categoryVisible) {
                                    list.add(it)
                                }
                            }
                        }
                        listData.value = list
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainRepository", "loadPopular category fetch error: ${e.message}")
                        listData.value = mutableListOf()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainRepository", "loadPopular onCancelled: " + error.message)
            }
        })
        return listData;
    }

    fun loadAllItems(): MutableLiveData<MutableList<ItemsModel>> {
        val listData = MutableLiveData<MutableList<ItemsModel>>()
        val itemsRef = firebaseDatabase.getReference("Items")
        val categoryRef = firebaseDatabase.getReference("Category")

        itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(itemsSnapshot: DataSnapshot) {
                categoryRef.get()
                    .addOnSuccessListener { categorySnapshot ->
                        val hiddenCategoryIds = hiddenCategoryIdsFrom(categorySnapshot)
                        val list = mutableListOf<ItemsModel>()
                        for (childSnapshot in itemsSnapshot.children) {
                            val item = childSnapshot.getValue(ItemsModel::class.java)
                            item?.let {
                                val categoryVisible = !hiddenCategoryIds.contains(it.categoryId)
                                if (!it.isHidden && categoryVisible) {
                                    list.add(it)
                                }
                            }
                        }
                        listData.value = list
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainRepository", "loadAllItems category fetch error: ${e.message}")
                        listData.value = mutableListOf()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainRepository", "loadAllItems onCancelled: " + error.message)
            }
        })
        return listData;
    }

    fun loadItemCategory(categoryId: String): MutableLiveData<MutableList<ItemsModel>> {
        val itemsLiveData = MutableLiveData<MutableList<ItemsModel>>()
        val categoryRef = firebaseDatabase.getReference("Category")
        val ref = firebaseDatabase.getReference("Items")
        val query: Query = ref.orderByChild("categoryId").equalTo(categoryId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryRef.get()
                    .addOnSuccessListener { categorySnapshot ->
                        val hiddenCategoryIds = hiddenCategoryIdsFrom(categorySnapshot)
                        if (hiddenCategoryIds.contains(categoryId)) {
                            itemsLiveData.value = mutableListOf()
                            return@addOnSuccessListener
                        }

                        val list = mutableListOf<ItemsModel>()
                        for (childSnapshot in snapshot.children) {
                            val item = childSnapshot.getValue(ItemsModel::class.java)
                            item?.let {
                                if (!it.isHidden) list.add(it)
                            }
                        }
                        itemsLiveData.value = list
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainRepository", "loadItemCategory category fetch error: ${e.message}")
                        itemsLiveData.value = mutableListOf()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainRepository", "loadItemCategory onCancelled: " + error.message)
            }
        })
        return itemsLiveData;

    }

    fun searchItems(query: String): MutableLiveData<MutableList<ItemsModel>> {
        val searchResultsLiveData = MutableLiveData<MutableList<ItemsModel>>()
        val itemsRef = firebaseDatabase.getReference("Items")
        val popularRef = firebaseDatabase.getReference("Popular")
        val categoriesRef = firebaseDatabase.getReference("Category")
        
        // Load categories first to map categoryId to categoryTitle
        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(categorySnapshot: DataSnapshot) {
                val categoryMap = mutableMapOf<String, String>()
                val hiddenCategoryIds = mutableSetOf<String>()
                for (catChild in categorySnapshot.children) {
                    val category = catChild.getValue(CategoryModel::class.java)
                    category?.let {
                        categoryMap[it.id.toString()] = it.title.lowercase()
                        if (it.isHidden) {
                            hiddenCategoryIds.add(it.id.toString())
                        }
                    }
                }
                
                // Load Items node
                itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(itemsSnapshot: DataSnapshot) {
                        val scored = mutableListOf<Pair<ItemsModel, Int>>() // (item, score)
                        val seen = mutableSetOf<String>() // dedup bằng title+categoryId
                        val lowerQuery = query.lowercase()
                        
                        /**
                         * Tính điểm liên quan của item với query.
                         * Trả về 0 nếu không match, >0 nếu match (càng cao càng liên quan).
                         *
                         * Thứ tự ưu tiên:
                         *  - Tên sản phẩm trùng hoàn toàn  → 100
                         *  - Tên sản phẩm bắt đầu bằng query → 50
                         *  - Tên sản phẩm chứa query        → 30
                         *  - Tên danh mục chứa query         → 15
                         *  - Mô tả chứa query                → 5
                         */
                        fun relevanceScore(item: ItemsModel): Int {
                            if (item.isHidden || hiddenCategoryIds.contains(item.categoryId)) return 0
                            var score = 0
                            val lowerTitle = item.title.lowercase()

                            // Title matching (tích lũy)
                            when {
                                lowerTitle == lowerQuery        -> score += 100  // exact match
                                lowerTitle.startsWith(lowerQuery) -> score += 50   // starts with
                                lowerTitle.contains(lowerQuery)   -> score += 30   // contains
                            }

                            // Category matching
                            val categoryTitle = categoryMap[item.categoryId] ?: ""
                            if (categoryTitle.contains(lowerQuery)) score += 15

                            // Description matching
                            if (item.description.lowercase().contains(lowerQuery)) score += 5

                            return score
                        }

                        // Helper: tính score và thêm vào danh sách
                        fun scoreAndAdd(item: ItemsModel) {
                            val key = "${item.title}||${item.categoryId}"
                            if (seen.contains(key)) return
                            val score = relevanceScore(item)
                            if (score > 0) {
                                scored.add(item to score)
                                seen.add(key)
                            }
                        }
                        
                        // Search trong Items
                        for (itemChild in itemsSnapshot.children) {
                            val item = itemChild.getValue(ItemsModel::class.java)
                            item?.let { scoreAndAdd(it) }
                        }
                        
                        // Tiếp tục search trong Popular
                        popularRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(popularSnapshot: DataSnapshot) {
                                for (popChild in popularSnapshot.children) {
                                    val item = popChild.getValue(ItemsModel::class.java)
                                    item?.let { scoreAndAdd(it) }
                                }
                                // Sắp xếp theo điểm liên quan giảm dần
                                scored.sortByDescending { it.second }
                                searchResultsLiveData.value = scored.map { it.first }.toMutableList()
                            }
                            
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("MainRepository", "searchItems popular onCancelled: " + error.message)
                                scored.sortByDescending { it.second }
                                searchResultsLiveData.value = scored.map { it.first }.toMutableList()
                            }
                        })
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MainRepository", "searchItems items onCancelled: " + error.message)
                        searchResultsLiveData.value = mutableListOf()
                    }
                })
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("MainRepository", "searchItems categories onCancelled: " + error.message)
                searchResultsLiveData.value = mutableListOf()
            }
        })
        
        return searchResultsLiveData
    }

    /**
     * Load TẤT CẢ sản phẩm visible từ cả "Items" và "Popular" nodes.
     * Dùng cho chế độ filter-only (không có search query).
     * Deduplicate bằng title + categoryId.
     */
    fun loadAllItemsAndPopular(): MutableLiveData<MutableList<ItemsModel>> {
        val resultLiveData = MutableLiveData<MutableList<ItemsModel>>()
        val itemsRef = firebaseDatabase.getReference("Items")
        val popularRef = firebaseDatabase.getReference("Popular")
        val categoryRef = firebaseDatabase.getReference("Category")

        categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(categorySnapshot: DataSnapshot) {
                val hiddenCategoryIds = hiddenCategoryIdsFrom(categorySnapshot)

                itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(itemsSnapshot: DataSnapshot) {
                        val list = mutableListOf<ItemsModel>()
                        val seen = mutableSetOf<String>()

                        fun addIfVisible(item: ItemsModel) {
                            if (item.isHidden || hiddenCategoryIds.contains(item.categoryId)) return
                            val key = "${item.title}||${item.categoryId}"
                            if (seen.contains(key)) return
                            list.add(item)
                            seen.add(key)
                        }

                        for (child in itemsSnapshot.children) {
                            child.getValue(ItemsModel::class.java)?.let { addIfVisible(it) }
                        }

                        popularRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(popularSnapshot: DataSnapshot) {
                                for (child in popularSnapshot.children) {
                                    child.getValue(ItemsModel::class.java)?.let { addIfVisible(it) }
                                }
                                resultLiveData.value = list
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("MainRepository", "loadAllItemsAndPopular popular error: " + error.message)
                                resultLiveData.value = list
                            }
                        })
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MainRepository", "loadAllItemsAndPopular items error: " + error.message)
                        resultLiveData.value = mutableListOf()
                    }
                })
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("MainRepository", "loadAllItemsAndPopular category error: " + error.message)
                resultLiveData.value = mutableListOf()
            }
        })

        return resultLiveData
    }
}

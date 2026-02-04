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

    fun loadBanner(): MutableLiveData<MutableList<BannerModel>> {
        val listData = MutableLiveData<MutableList<BannerModel>>()
        val ref = firebaseDatabase.getReference("Banner");
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BannerModel>();
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(BannerModel::class.java)
                    item?.let { list.add(it) }
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
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CategoryModel>();
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(CategoryModel::class.java)
                    item?.let { list.add(it) }
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
        val ref = firebaseDatabase.getReference("Popular");
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ItemsModel>();
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(ItemsModel::class.java)
                    item?.let { list.add(it) }
                }
                listData.value = list;
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainRepository", "loadPopular onCancelled: " + error.message)
            }

        })
        return listData;
    }

    fun loadAllItems(): MutableLiveData<MutableList<ItemsModel>> {
        val listData = MutableLiveData<MutableList<ItemsModel>>()
        val ref = firebaseDatabase.getReference("Items");
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ItemsModel>();
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(ItemsModel::class.java)
                    item?.let { list.add(it) }
                }
                listData.value = list;
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainRepository", "loadAllItems onCancelled: " + error.message)
            }
        })
        return listData;
    }

    fun loadItemCategory(categoryId: String): MutableLiveData<MutableList<ItemsModel>> {
        val itemsLiveData = MutableLiveData<MutableList<ItemsModel>>()
        val ref = firebaseDatabase.getReference("Items")
        val query: Query = ref.orderByChild("categoryId").equalTo(categoryId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ItemsModel>();
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(ItemsModel::class.java)
                    item?.let { list.add(it) }
                }
                itemsLiveData.value = list;
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
        val categoriesRef = firebaseDatabase.getReference("Category")
        
        // Load categories first to map categoryId to categoryTitle
        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(categorySnapshot: DataSnapshot) {
                val categoryMap = mutableMapOf<String, String>()
                for (catChild in categorySnapshot.children) {
                    val category = catChild.getValue(CategoryModel::class.java)
                    category?.let {
                        categoryMap[it.id.toString()] = it.title.lowercase()
                    }
                }
                
                // Now load and filter items
                itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(itemsSnapshot: DataSnapshot) {
                        val list = mutableListOf<ItemsModel>()
                        val lowerQuery = query.lowercase()
                        
                        for (itemChild in itemsSnapshot.children) {
                            val item = itemChild.getValue(ItemsModel::class.java)
                            item?.let {
                                val matchesTitle = it.title.lowercase().contains(lowerQuery)
                                val matchesDescription = it.description.lowercase().contains(lowerQuery)
                                val categoryTitle = categoryMap[it.categoryId] ?: ""
                                val matchesCategory = categoryTitle.contains(lowerQuery)
                                
                                if (matchesTitle || matchesDescription || matchesCategory) {
                                    list.add(it)
                                }
                            }
                        }
                        searchResultsLiveData.value = list
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
}

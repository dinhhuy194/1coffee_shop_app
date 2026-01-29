package com.example.coffeeshop.Repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.coffeeshop.Domain.BannerModel
import com.example.coffeeshop.Domain.CategoryModel
import com.example.coffeeshop.Domain.ItemsModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainRepository {
    private val firebaseDatabase = FirebaseDatabase.getInstance();

    fun loadBanner(): MutableLiveData<MutableList<BannerModel>>
    {
        val listData = MutableLiveData<MutableList<BannerModel>>()
        val ref = firebaseDatabase.getReference("Banner");
        ref.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BannerModel>();
                for (childSnapshot in snapshot.children){
                    val item = childSnapshot.getValue(BannerModel::class.java)
                    item?.let { list.add(it) }
                }
                listData.value = list;
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
        return listData;
    }

    fun loadCategory(): MutableLiveData<MutableList<CategoryModel>>
    {
        val listData = MutableLiveData<MutableList<CategoryModel>>()
        val ref = firebaseDatabase.getReference("Category");
        ref.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CategoryModel>();
                for (childSnapshot in snapshot.children){
                    val item = childSnapshot.getValue(CategoryModel::class.java)
                    item?.let { list.add(it) }
                }
                listData.value = list;
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
        return listData;
    }

    fun loadPopular(): MutableLiveData<MutableList<ItemsModel>>
    {
        val listData = MutableLiveData<MutableList<ItemsModel>>()
        val ref = firebaseDatabase.getReference("Popular");
        ref.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ItemsModel>();
                for (childSnapshot in snapshot.children){
                    val item = childSnapshot.getValue(ItemsModel::class.java)
                    item?.let { list.add(it) }
                }
                listData.value = list;
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
        return listData;
    }
}
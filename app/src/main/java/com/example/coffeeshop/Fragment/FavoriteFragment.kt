package com.example.coffeeshop.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.coffeeshop.Adapter.PopularAdapter
import com.example.coffeeshop.Helper.ManagmentFavorite
import com.example.coffeeshop.databinding.FragmentFavoriteBinding

class FavoriteFragment : Fragment() {
    
    private lateinit var binding: FragmentFavoriteBinding
    private lateinit var managmentFavorite: ManagmentFavorite
    private lateinit var adapter: PopularAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        managmentFavorite = ManagmentFavorite(requireContext())
        
        setupRecyclerView()
        loadFavorites()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh favorites when returning to this fragment
        loadFavorites()
    }
    
    private fun setupRecyclerView() {
        binding.favoriteRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
    }
    
    private fun loadFavorites() {
        val favoritesList = managmentFavorite.getFavoritesList()
        
        // Update count
        binding.countTxt.text = "${favoritesList.size} items"
        
        // Show/hide empty state
        if (favoritesList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.favoriteRecyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.favoriteRecyclerView.visibility = View.VISIBLE
            
            // Setup adapter with favorites
            adapter = PopularAdapter(favoritesList)
            binding.favoriteRecyclerView.adapter = adapter
        }
    }
}

package com.ldihackos.tellvport.views

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ldihackos.tellvport.adapters.PlatformsAdapter
import com.ldihackos.tellvport.databinding.FragmentPlatformBinding
import com.ldihackos.tellvport.models.PlatformData
import com.ldihackos.tellvport.utils.LanguagePref
import com.ldihackos.tellvport.utils.NetworkResult
import com.ldihackos.tellvport.utils.setLocale
import com.ldihackos.tellvport.viewmodels.ApiViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlatformFragment : Fragment() {

    private val apiViewModel by viewModels<ApiViewModel>()
    private var _binding: FragmentPlatformBinding? = null
    private val binding get() = _binding!!
    private val platformsAdapter = PlatformsAdapter()

    override fun onAttach(context: Context) {
        super.onAttach(context.setLocale(LanguagePref.getLanguage(context)))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlatformBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        bindObservers()
        setupSearch()
    }

    private fun setupRecyclerView() {
        binding.rvPlatforms.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = platformsAdapter
        }
    }

    private fun bindObservers() {
        apiViewModel.platformsLiveData.observe(viewLifecycleOwner) { result ->
            binding.loader.isVisible = result is NetworkResult.Loading
            when (result) {
                is NetworkResult.Error ->
                    Log.d("API ERROR", "DATA=${result.data} MESSAGE=${result.message}")
                is NetworkResult.Success -> {
                    Log.d("API SUCCESS", "DATA=${result.data}")
                    platformsAdapter.submitList(result.data)
                }
                else -> Unit
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val queries = text.toString().lowercase().split(" ").filter { it.isNotBlank() }
            val filteredList = apiViewModel.platformsLiveData.value?.data?.filter { platform ->
                queries.any { query ->
                    platform.platform_lastest_train_name.lowercase().contains(query) ||
                    platform.platform_lastest_train_number.contains(query)
                }
            }.orEmpty()
            platformsAdapter.submitList(filteredList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.ldihackos.tellvport.views

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.ldihackos.tellvport.R
import com.ldihackos.tellvport.adapters.PlatformsAdapter
import com.ldihackos.tellvport.databinding.FragmentPlatformBinding
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
    private lateinit var platformsAdapter: PlatformsAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val language = LanguagePref.getLanguage(context)
        val updatedContext = context.setLocale(language)
        @Suppress("DEPRECATION")
        super.onAttach(updatedContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlatformBinding.inflate(layoutInflater, container, false)
        platformsAdapter = PlatformsAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindObservers()

        binding.rvPlatforms.layoutManager = LinearLayoutManager(context)
        binding.rvPlatforms.adapter = platformsAdapter
    }

    private fun bindObservers() {
        apiViewModel.platformsLiveData.observe(viewLifecycleOwner) {
            binding.loader.isVisible = false
            when(it) {
                is NetworkResult.Error -> {
                    Log.d("THARUN (API ERROR)", "DATA=${it.data} MESSAGE=${it.message}")
                }
                is NetworkResult.Loading -> {
                    binding.loader.isVisible = true
                    Log.d("THARUN (API LOADING)", "DATA=${it.data} MESSAGE=${it.message}")
                }
                is NetworkResult.Success -> {
                    Log.d("THARUN (API SUCCESS)", "DATA=${it.data} MESSAGE=${it.message}")
                    platformsAdapter.submitList(it.data)
                }
            }
        }
    }
}
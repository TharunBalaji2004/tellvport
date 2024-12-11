package com.ldihackos.tellvport.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ldihackos.tellvport.R
import com.ldihackos.tellvport.databinding.FragmentMapBinding
import com.ldihackos.tellvport.databinding.FragmentUtilitiesBinding


class UtilitiesFragment : Fragment() {

    private var _binding: FragmentUtilitiesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUtilitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
}
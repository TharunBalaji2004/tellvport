package com.ldihackos.tellvport.views

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ldihackos.tellvport.R
import com.ldihackos.tellvport.utils.LanguagePref
import com.ldihackos.tellvport.utils.setLocale

class EmergencyFragment : Fragment() {

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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_emergency, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}
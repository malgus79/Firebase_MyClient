package com.myclient.promo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.myclient.R
import com.myclient.databinding.FragmentPromoBinding

class PromoFragment : Fragment() {
    private var binding: FragmentPromoBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPromoBinding.inflate(inflater, container, false)
        binding?.let {
            return it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configRemoteConfig()

    }

    private fun configRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val percentaje = remoteConfig.getDouble("percentaje")
                    val photoUrl = remoteConfig.getString("photoUrl")
                    val message = remoteConfig.getString("message")

                    binding?.let {
                        it.tvMessage.text = message
                        it.tvPercentaje.text = percentaje.toString()

                        Glide.with(this)
                            .load(photoUrl)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)  //cache no se encuentre disponible
                            .placeholder(R.drawable.ic_access_time)
                            .error(R.drawable.ic_local_offer)
                            .centerCrop()
                            .into(it.imgPromo)
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
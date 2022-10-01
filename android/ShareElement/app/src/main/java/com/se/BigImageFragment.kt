package com.se

import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.SharedElementCallback


class BigImageFragment : Fragment() {

    companion object {
        const val TAG = "BigImageFragment"

        @JvmStatic
        fun newInstance() = BigImageFragment()
    }

    private var button: TextView? = null
    private var bigImageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition =  TransitionInflater.from(requireContext()).inflateTransition(R.transition.share_element)
        setEnterSharedElementCallback(object : SharedElementCallback(){
            override fun onSharedElementStart(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                Log.d("IzumiSakai", "onSharedElementStart call -> sharedElementNames = $sharedElementNames, sharedElements = $sharedElements, sharedElementSnapshots = $sharedElementSnapshots")
            }

            override fun onSharedElementEnd(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                Log.d("IzumiSakai", "onSharedElementEnd call -> sharedElementNames = $sharedElementNames, sharedElements = $sharedElements, sharedElementSnapshots = $sharedElementSnapshots")
            }

            override fun onRejectSharedElements(rejectedSharedElements: MutableList<View>?) {
                Log.d("IzumiSakai", "onRejectSharedElements call -> rejectedSharedElements = $rejectedSharedElements")
            }

            override fun onMapSharedElements(
                names: MutableList<String>?,
                sharedElements: MutableMap<String, View>?
            ) {
                Log.d("IzumiSakai", "onMapSharedElements call -> names = $names, sharedElements = $sharedElements")
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_big_image, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button = view.findViewById(R.id.big_button)
        bigImageView = view.findViewById(R.id.big_image_view)

        bigImageView?.transitionName = MainActivity.TRANSITION_NAME


        button?.setOnClickListener {
            val fragmentManager = activity?.supportFragmentManager
            val fragment = fragmentManager?.findFragmentByTag(SmallViewFragment.TAG) ?: SmallViewFragment.newInstance()
            val transaction = fragmentManager?.beginTransaction()
            bigImageView?.let { transaction?.addSharedElement(it, MainActivity.TRANSITION_NAME) }

            transaction
                ?.replace(R.id.fragment_container, fragment, SmallViewFragment.TAG)
                ?.commitAllowingStateLoss()
        }
    }
}
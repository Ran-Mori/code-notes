package com.se.fragment

import android.os.Bundle
import android.transition.TransitionInflater
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.se.MainActivity
import com.se.R

class SmallViewFragment : Fragment() {

    companion object {
        const val TAG = "SmallImageFragment"

        @JvmStatic
        fun newInstance() = SmallViewFragment()
    }

    private var button: TextView? = null
    private var smallImageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition =  TransitionInflater.from(requireContext()).inflateTransition(
            R.transition.share_element
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_small_image_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button = view.findViewById(R.id.small_button)

        smallImageView = view.findViewById(R.id.small_image_view)
        smallImageView?.transitionName = MainActivity.TRANSITION_NAME

        button?.setOnClickListener {
            val fragmentManager = activity?.supportFragmentManager
            val fragment = fragmentManager?.findFragmentByTag(BigImageFragment.TAG) ?: BigImageFragment.newInstance()
            val transaction = fragmentManager?.beginTransaction()

            smallImageView?.let { transaction?.addSharedElement(it, MainActivity.TRANSITION_NAME) }

            transaction
                ?.replace(R.id.fragment_container, fragment, BigImageFragment.TAG)
                ?.commitAllowingStateLoss()
        }
    }
}
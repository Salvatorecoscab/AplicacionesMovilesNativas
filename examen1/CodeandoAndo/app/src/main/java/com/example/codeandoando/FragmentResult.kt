package com.example.codeandoando

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.codeandoando.databinding.FragmentResultBinding

class FragmentResult : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private var onPlayAgainListener: (() -> Unit)? = null
    private var onExitListener: (() -> Unit)? = null

    private var isCorrectResult: Boolean = false

    companion object {
        private const val ARG_IS_CORRECT = "isCorrect"
        private const val TAG = "FragmentResult"

        fun newInstance(isCorrect: Boolean): FragmentResult {
            val fragment = FragmentResult()
            val args = Bundle()
            args.putBoolean(ARG_IS_CORRECT, isCorrect)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isCorrectResult = arguments?.getBoolean(ARG_IS_CORRECT) ?: false

        val resultIconImageView: ImageView = binding.resultIconImageView
        val resultTextView: TextView = binding.resultTextView
        val playAgainButton: Button = binding.playAgainButton
        val exitButton: Button = binding.exitButton

        if (isCorrectResult) {
            resultIconImageView.setImageResource(R.drawable.ic_check_green) // Asegúrate de tener este drawable
            resultTextView.setText(R.string.you_won)
        } else {
            resultIconImageView.setImageResource(R.drawable.ic_cross_red) // Asegúrate de tener este drawable
            resultTextView.setText(R.string.you_lost) // O un nuevo string "you_lost"
        }

        playAgainButton.setOnClickListener {
            Log.d(TAG, "Play Again clicked")
            onPlayAgainListener?.invoke()
        }

        exitButton.setOnClickListener {
            Log.d(TAG, "Exit clicked")
            onExitListener?.invoke()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnResultInteractionListener) {
            onPlayAgainListener = context::onPlayAgain
            onExitListener = context::onExit
        } else {
            throw RuntimeException("$context must implement OnResultInteractionListener")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface OnResultInteractionListener {
        fun onPlayAgain()
        fun onExit()
    }
}
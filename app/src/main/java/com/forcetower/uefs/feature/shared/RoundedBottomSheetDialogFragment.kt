package com.forcetower.uefs.feature.shared

import android.app.Dialog
import android.os.Bundle
import com.forcetower.uefs.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class RoundedBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.AppTheme_BottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme)

}
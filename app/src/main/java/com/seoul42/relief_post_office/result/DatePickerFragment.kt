package com.seoul42.relief_post_office.result

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment

class DatePickerFragment(private val date: String) : DialogFragment(), DatePickerDialog.OnDateSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dateArray = date.split("/")
        val year = dateArray[0].toInt()
        val month = dateArray[1].toInt()
        val day = dateArray[2].toInt()

        return DatePickerDialog(requireContext(), this, year, month-1, day)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        val activity = activity as ResultActivity?
        activity?.processDatePickerResult(year, month, day)
    }
}
package com.seoul42.relief_post_office.result

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment

/**
 * 원하는 날짜를 선택하게 해주는 프레그먼트 클래스
 *
 * 안드로이드 API 입니다.
 * 고른 날짜를 기준으로 달력을 만들어 사용자가 편하게 합니다.
 */
class DatePickerFragment(private val date: String) : DialogFragment(), DatePickerDialog.OnDateSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 이미 설정된 날짜
        val dateArray = date.split("/")
        val year = dateArray[0].toInt()
        val month = dateArray[1].toInt()
        val day = dateArray[2].toInt()

        // 설정된 날짜로 다이얼로그 띄우기
        return DatePickerDialog(requireContext(), this, year, month-1, day)
    }

    /**
     * 날짜 선택기 다이얼로그에서 날짜를 선택하면 호출되는 매서드
     *
     * 선택된 날짜로 결과 액티비티의 날짜 정보를 변경합니다.
     */
    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        val activity = activity as ResultActivity?
        activity?.processDatePickerResult(year, month, day)
    }
}
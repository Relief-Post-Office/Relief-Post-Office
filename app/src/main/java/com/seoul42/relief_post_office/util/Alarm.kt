package com.seoul42.relief_post_office.util

import android.content.Context
import android.os.Build
import android.os.PowerManager

/**
 * 알람 기능에서 활용될 수 있는 커스텀한 메서드들을 관리
 */
internal object Alarm {
    /**
     * 요일을 index 별로 가져오도록 커스텀한 메서드
     */
    fun getDay(curDay : String) : Int {
        return when(curDay) {
            "월" -> 1
            "화" -> 2
            "수" -> 3
            "목" -> 4
            "금" -> 5
            "토" -> 6
            else -> 7
        }
    }
    /**
     * guardian 플래그에 따라 현재 시간과 안부 시간의 초(second) 차이를 반환
     *  - guardian = true : 현재 시간과 (안부 시간 + 30분)의 차이
     *  - guardian = false : 현재 시간과 안부 시간의 차이
     */
    fun getTimeGap(curTime : String, safetyTime : String, dayGap : Int, guardian : Boolean) : Int {
        val curHour = curTime.substring(0, 2).toInt()
        val curMin = curTime.substring(3, 5).toInt()
        val curSecond = curTime.substring(6, 8).toInt()
        val safetyHour = safetyTime.substring(0, 2).toInt()
        val safetyMin = if (guardian) {
            safetyTime.substring(3, 5).toInt() + 30
        } else {
            safetyTime.substring(3, 5).toInt()
        }

        return if (dayGap == 0) {
            if ((safetyHour * 3600 + safetyMin * 60) - (curHour * 3600 + curMin * 60 + curSecond) < 0) {
                ((safetyHour + 24 * 7) * 3600 + safetyMin * 60) - (curHour * 3600 + curMin * 60 + curSecond)
            } else {
                (safetyHour * 3600 + safetyMin * 60) - (curHour * 3600 + curMin * 60 + curSecond)
            }
        } else {
            ((safetyHour + 24 * dayGap) * 3600 + safetyMin * 60) - (curHour * 3600 + curMin * 60 + curSecond)
        }
    }

    /**
     * ex) date = "2022-07-12", time = "13:25" => 202207121325 (type : long)
     */
    fun getDateToLong(date : String, time : String) : Long {
        val dateArray = date.split("-")
        val timeArray = time.split(":")
        val dateTime = dateArray[0] + dateArray[1] + dateArray[2] +
                timeArray[0] + timeArray[1]

        return dateTime.toLong()
    }
}
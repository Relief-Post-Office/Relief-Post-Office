package com.seoul42.relief_post_office.util

/**
 * 프로젝트의 가독성을 높이기 위한 여러 상수들 선언
 */
class Constants {
    companion object {
        // FCM URL
        const val FCM_URL = "https://fcm.googleapis.com"

        // Cloud Messaging Server Key
        const val FCM_KEY =
            "AAAA3S0TaUQ:APA91bG5WuqVq_Mmu3NnOxKZmOXj9kpc-wq0r0vRaUtgRSkO2GciX8Vio3E_YdDG3NxSBQzl3RdvKwoFI9_d7N7rMsfTXrIJW6IUERVgNvqQdPq1w2g0Yj-Qstayo1Ao5osWiNyrOdHR"

        // Fragment Util
        const val TAG_MAIN = "fragment_main"
        const val TAG_SAFETY = "fragment_safety"
        const val TAG_QUESTION = "fragment_question"

        // request dialog util
        const val INVALID_PHONE_NUMBER = "invalid_phone_number"
        const val CONNECTED_GUARDIAN = "connected_guardian"
        const val NON_EXIST_GUARDIAN = "non_exist_guardian_phone_number"
        const val REGISTER_SUCCESS = "register_success"
    }
}
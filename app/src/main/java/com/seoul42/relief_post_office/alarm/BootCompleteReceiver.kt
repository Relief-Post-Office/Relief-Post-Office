package com.seoul42.relief_post_office.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.model.ResultDTO
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.model.WardDTO
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class BootCompleteReceiver : BroadcastReceiver() {

    data class RecommendDTO(
        val force: Boolean?,
        val timeGap: Int?,
        val safetyId: String?,
        val safetyDTO: SafetyDTO?,
        var resultId: String?,
        var resultDTO: ResultDTO?
    ) : Serializable

    private val recommendList = ArrayList<RecommendDTO>() /* 추천할 수 있는 모든 항목들을 담음 */
    private val resultMap : MutableMap<String, String> = mutableMapOf() /* key = safetyId, value = resultId */

    companion object {
        const val REPEAT_FORCE = "com.rightline.backgroundrepeatapp.REPEAT_FORCE"
        const val REPEAT_PUSH = "com.rightline.backgroundrepeatapp.REPEAT_PUSH"
    }

    override fun onReceive(context : Context?, intent : Intent?) {
        if (intent!!.action.equals(Intent.ACTION_BOOT_COMPLETED) && Firebase.auth.currentUser != null) {
            Log.d("Boot Complete", "check...")
            val userId = Firebase.auth.uid.toString()
            val userDB = Firebase.database.reference.child("user").child(userId)

            userDB.get().addOnSuccessListener {
                val userDTO = it.getValue(UserDTO::class.java) as UserDTO
                if (userDTO.guardian == false) {

                }
            }
        }
    }
}
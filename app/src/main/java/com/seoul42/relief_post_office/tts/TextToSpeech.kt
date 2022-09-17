package com.seoul42.relief_post_office.tts

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class TextToSpeech(view : View, private val context : Context) {
    private val auth : FirebaseAuth by lazy {
        Firebase.auth
    }

    private val dateAndTime : LocalDateTime by lazy {
        LocalDateTime.now()
    }

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    private val recordingFilePath: String by lazy {
        "${view.context.externalCacheDir?.absolutePath}/${auth.currentUser?.uid + dateAndTime.format(formatter)}.3gp"
    }

    private val params = Bundle()
    private var tts: TextToSpeech? = null

    fun initTTS() {
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, null)
        tts = TextToSpeech(context) { state ->
            if (state == TextToSpeech.SUCCESS) {
                tts!!.language = Locale.KOREAN
            } else {
                throw Exception("TTS is not available!")
            }
        }
    }

    fun synthesizeToFile(text: String) {
        tts!!.synthesizeToFile(text, HashMap(), recordingFilePath)
    }

    fun returnRecordingFile() : String {
        return recordingFilePath
    }
}
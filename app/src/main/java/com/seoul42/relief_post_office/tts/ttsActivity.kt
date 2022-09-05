package com.seoul42.relief_post_office.tts

import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import java.util.*


class TtsActivity(view: View){
    private val params = Bundle()
    private val colorSpan = BackgroundColorSpan(Color.YELLOW)
    private var tts: TextToSpeech? = null
    private var inputEditText: EditText? = null
    private var contentTextView: TextView? = null
    private var playState: PlayState = PlayState.STOP
    private var spannable: Spannable? = null
    private var standbyIndex = 0
    private var lastPlayIndex = 0

    // record 방식과 같이 view를 통해
    private val recordingFilePath: String by lazy {
        "${view.context.externalCacheDir?.absolutePath}/recording.3gp"
    }

    private fun initTTS() {
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, null)
        // TextToSpeech(context = ?)
        // context 값을 찾아야함
//        tts = TextToSpeech() { state ->
//            if (state == TextToSpeech.SUCCESS) {
//                tts!!.language = Locale.KOREAN
//            }
//        }
    }

    fun startPlay() {
        val content = inputEditText!!.text.toString()
        if (playState.isStopping && !tts!!.isSpeaking) {
            setContentFromEditText(content)
            startSpeak(content)
        } else if (playState.isWaiting) {
            standbyIndex += lastPlayIndex
            startSpeak(content.substring(standbyIndex))
        }
        playState = PlayState.PLAY
    }

    private fun setContentFromEditText(content: String) {
        contentTextView!!.setText(content, TextView.BufferType.SPANNABLE)
        spannable = contentTextView!!.text as SpannableString
    }

    private fun startSpeak(text: String) {
        tts!!.synthesizeToFile(text, HashMap(), recordingFilePath)
    }

    fun returnRecordingFile() : String {
        return recordingFilePath
    }
}
package com.seoul42.relief_post_office.record

import android.media.MediaRecorder
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class AnswerRecordActivity(view: View) {
    private val auth : FirebaseAuth by lazy {
        Firebase.auth
    }

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    private val dateAndTime : LocalDateTime by lazy {
        LocalDateTime.now()
    }

    private val recordingFilePath: String by lazy {
        "${view.context.externalCacheDir?.absolutePath}/${auth.currentUser?.uid + dateAndTime.format(formatter)}.3gp"
    }

    private var recorder: MediaRecorder? = null

    private var state : RecordState = RecordState.BEFORE_RECORDING

    /**
     * 로컬에 저장된 녹음파일 캐시주소 반환
     * - 피보호자 음성응답용
     * - AnswerActivity 전달
     * - Firebase Storage에 3pg 파일형태로 저장될 예정
     */
    fun returnRecordingFile() : String {
        return recordingFilePath
    }

    /**
     * 음성 녹음기능 시작
     * - MediaRecorder 클래스 활용
     * - 포맷 : THREE_GPP
     * - 엔코더 : AMR_NB
     * - 로컷캐시에 임의로 저장
     */
    fun startRecoding() {
        // 녹음 시작 시 초기화
        recorder = MediaRecorder()
            .apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(recordingFilePath)
                prepare()
            }
        recorder?.start()
        state = RecordState.ON_RECORDING
    }

    /**
     * 음성 녹음기능 중지
     * - '녹음 중' state 일때 버튼 누를경우, 녹음 중단 및 메모리해제
     */
    fun stopRecording() {
        if (state == RecordState.ON_RECORDING) {
            recorder?.run {
                stop()
                release()
            }
            recorder = null
        }
    }

    // 상수로 우리가 요청할 오디오 권한의 코드를 따로 정의
    companion object {
        const val REQUEST_RECORD_AUDIO_PERMISSION = 201
    }
}
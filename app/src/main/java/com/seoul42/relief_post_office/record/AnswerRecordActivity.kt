package com.seoul42.relief_post_office.record

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
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

    // 요청할 권한들을 담을 배열에 음성 녹음 관련 권한을 담아줍니다.
    private val recordingFilePath: String by lazy {
        "${view.context.externalCacheDir?.absolutePath}/${auth.currentUser?.uid + dateAndTime.format(formatter)}.3gp"
    }

    private var recorder: MediaRecorder? = null // 사용 하지 않을 때는 메모리해제 및  null 처리

    // 로컬에 저장된 녹음파일 캐시주소
    // QuestionFragments로 전달, firebase storage에 3pg 파일형태로 저장될 예정
    fun returnRecordingFile() : String {
        return recordingFilePath
    }

    private var state : RecordState = RecordState.BEFORE_RECORDING

    // 녹음 메써드
    fun startRecoding() {
        // 녹음 시작 시 초기화
        recorder = MediaRecorder()
            .apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP) // 포멧
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // 엔코더
                setOutputFile(recordingFilePath) // 우리는 저장 x 캐시에
                prepare()
            }
        recorder?.start()
        state = RecordState.ON_RECORDING
    }

    // '녹음 중'일때 버튼 누를경우, 녹음 중단 및 메모리해제
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
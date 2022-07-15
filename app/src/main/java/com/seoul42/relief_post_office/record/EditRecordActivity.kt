package com.seoul42.relief_post_office.record

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
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
class EditRecordActivity(var src: String?, view: View) {

    private val auth : FirebaseAuth by lazy {
        Firebase.auth
    }

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    private val dateAndTime : LocalDateTime by lazy {
        LocalDateTime.now()
    }

    private val recordTimeTextView: EditRecordCountTime by lazy {
        view.findViewById(R.id.question_setting_time2)
    }

    private val recordDurationTextView: EditRecordDurationTime by lazy {
        view.findViewById(R.id.question_duration_time2)
    }

    private val resetButton: Button by lazy {
        view.findViewById(R.id.question_record_settingBtn2)
    }

    private val recordButton: EditRecordButton by lazy {
        view.findViewById(R.id.question_setting_playBtn2)
    }

    // 요청할 권한들을 담을 배열에 음성 녹음 관련 권한을 담아줍니다.
    private var recordingFilePath: String? = src

    private var state = RecordState.AFTER_RECORDING
        set(value) { // setter 설정
            field = value // 실제 프로퍼티에 대입
            resetButton.isEnabled = (value == RecordState.AFTER_RECORDING || value == RecordState.ON_PLAYING)
            recordButton.updateIconWithState(value)
        }

    private var recorder: MediaRecorder? = null // 사용 하지 않을 때는 메모리해제 및  null 처리
    private var player: MediaPlayer? = null

     fun initViews() {
         recordButton.updateIconWithState(state)
         recordDurationTextView.setRecordDuration(recordingFilePath)
    }

     fun bindViews(view : View) {
        recordButton.setOnClickListener {
            when (state) {
                RecordState.BEFORE_RECORDING -> {
                    startRecoding()
                }
                RecordState.ON_RECORDING -> {
                    stopRecording()
                }
                RecordState.AFTER_RECORDING -> {
                    startPlaying()
                }
                RecordState.ON_PLAYING -> {
                    stopPlaying()
                }
            }
        }

         // 수정버튼 클릭 시, 기존 src는 null처리 및 "recordingFilePath"는 로컬 캐시주소 포인트
        resetButton.setOnClickListener {
            stopPlaying()
            recordingFilePath = "${view.context.externalCacheDir?.absolutePath}/${auth.currentUser?.uid + dateAndTime.format(formatter)}.3gp"
            // clear
            recordDurationTextView.clearCountTime()
            recordTimeTextView.clearCountTime()
            state = RecordState.BEFORE_RECORDING
        }
    }

    fun initVariables() {
        state = RecordState.AFTER_RECORDING
    }

    fun returnRecordingFile() : String? {
        return recordingFilePath
    }

    private fun startRecoding() {

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
        recordDurationTextView.startCountUp()
        recordTimeTextView.startCountUp()
        state = RecordState.ON_RECORDING
    }

    private fun stopRecording() {
        recorder?.run {
            stop()
            release()
        }
        recorder = null
        recordDurationTextView.setRecordDuration(recordingFilePath)
        recordTimeTextView.stopCountUp()
        state = RecordState.AFTER_RECORDING
    }

    private fun startPlaying() {
        // MediaPlayer
        player = MediaPlayer()
            .apply {
                setDataSource(recordingFilePath)
                prepare() // 재생 할 수 있는 상태 (큰 파일 또는 네트워크로 가져올 때는 prepareAsync() )
            }

        // 전부 재생 했을 때
        player?.setOnCompletionListener {
            stopPlaying()
            state = RecordState.AFTER_RECORDING
        }

        player?.start() // 재생
        recordDurationTextView.setRecordDuration(recordingFilePath)
        recordTimeTextView.startCountUp()
        state = RecordState.ON_PLAYING
    }

    private fun stopPlaying() {
        player?.release()
        player = null
        recordTimeTextView.stopCountUp()

        state = RecordState.AFTER_RECORDING
    }

    // 상수로 우리가 요청할 오디오 권한의 코드를 따로 정의
    companion object {
        const val REQUEST_RECORD_AUDIO_PERMISSION = 201
    }
}
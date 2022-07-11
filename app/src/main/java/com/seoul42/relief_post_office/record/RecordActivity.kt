package com.seoul42.relief_post_office.record

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.view.View
import android.widget.Button
import com.seoul42.relief_post_office.R

class RecordActivity(view: View) {

    private val recordTimeTextView: RecordCountTime by lazy {
        view.findViewById(R.id.question_setting_time)
    }

    private val resetButton: Button by lazy {
        view.findViewById(R.id.question_record_settingBtn)
    }

    private val recordButton: RecordButton by lazy {
        view.findViewById(R.id.question_setting_playBtn)
    }


    // 요청할 권한들을 담을 배열에 음성 녹음 관련 권한을 담아줍니다.

    private val recordingFilePath: String by lazy {
        "${view.context.externalCacheDir?.absolutePath}/recording.3gp"
    }
    private var state = RecordState.BEFORE_RECORDING
        set(value) { // setter 설정
            field = value // 실제 프로퍼티에 대입
            resetButton.isEnabled = (value == RecordState.AFTER_RECORDING || value == RecordState.ON_PLAYING)
            recordButton.updateIconWithState(value)
        }

    private var recorder: MediaRecorder? = null // 사용 하지 않을 때는 메모리해제 및  null 처리
    private var player: MediaPlayer? = null



     fun initViews() {
        recordButton.updateIconWithState(state)
    }

     fun bindViews() {

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

        resetButton.setOnClickListener {
            stopPlaying()
            // clear
            recordTimeTextView.clearCountTime()
            state = RecordState.BEFORE_RECORDING
        }
    }

    fun initVariables() {
        state = RecordState.BEFORE_RECORDING
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
        recordTimeTextView.startCountUp()
        state = RecordState.ON_RECORDING
    }

    private fun stopRecording() {
        recorder?.run {
            stop()
            release()
        }
        recorder = null
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
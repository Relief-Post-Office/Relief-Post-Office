package com.seoul42.relief_post_office.tts

enum class PlayState(val state: String) {
    PLAY("재생 중"), WAIT("일시정지"), STOP("멈춤");

    val isStopping: Boolean
        get() = this == STOP
    val isWaiting: Boolean
        get() = this == WAIT
    val isPlaying: Boolean
        get() = this == PLAY
}
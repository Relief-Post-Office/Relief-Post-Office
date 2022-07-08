package com.seoul42.relief_post_office.model

import java.io.Serializable

// 질문 오브젝트 타입
data class QuestionDTO(
    var key : String?,
    val body : QuestionBody?
) : Serializable {
    // 질문안의 데이터 내용
    data class QuestionBody(
        var owner: String?,      // 소유주
        var date: String?,        // 생성 날짜
        var text: String?,       // 텍스트
        var secret: Boolean,    // 비밀 옵션
        var record: Boolean,     // 녹음 옵션
        var src: String?         // 녹음 파일 주소
    ) : Serializable {
        constructor() : this(null, null, null, false, false, null)
    }
    constructor() : this(null, null)
}
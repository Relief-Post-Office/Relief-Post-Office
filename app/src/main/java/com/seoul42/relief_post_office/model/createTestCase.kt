import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

fun createTestCase() {
    addUsers() // user1..9
    addGuardians() //guardian1..4
    addWards() //wards5..9
    addRegards() //regard1..27
    addQuestions() //question1..12
    addResults() //result5..9
    addAnswers() //answers1..12
}

fun addUsers() {
    val datebase = Firebase.database
    val document = datebase.getReference("users")

    for (i: Int in 1..4) {
        val user = Users(i)
        user.userType = "보호자"
        document.child("userid-${i}").setValue(user)
    }
    for (i: Int in 5..9) {
        val user = Users(i)
        user.userType = "피보호자"
        document.child("userid-${i}").setValue(user)
    }
}

fun addGuardians() {
    val datebase = Firebase.database
    val document = datebase.getReference("guardians")

    for (i: Int in 1..4) {
        val guardian = Guardians(i)
        document.child("userid-${i}").setValue(guardian)
    }
}

fun addWards() {
    val datebase = Firebase.database
    val document = datebase.getReference("wards")

    for (i: Int in 5..9) {
        val ward = Wards(i)
        document.child("userid-${i}").setValue(ward)
    }
}

fun addRegards() {
    val datebase = Firebase.database
    val document = datebase.getReference("regards")

    for (i: Int in 1..4) {
        val regard = Regards(i)
        regard.questionList = arrayListOf("question-${1+3*i}", "question-${2+3*i}","question-${3+3*i}")
        document.child("regardid-${i}").setValue(regard)
    }
    for (i: Int in 5..9) {
        val regard = Regards(i)
        regard.questionList = arrayListOf("question-1", "question-3","question-11")
        document.child("regardid-${i}").setValue(regard)
    }
}

fun addQuestions() {
    val datebase = Firebase.database
    val document = datebase.getReference("questions")

    for (i: Int in 1..12) {
        val question = Questions(i)
        document.child("questionid-${i}").setValue(question)
    }
}

fun addResults() {
    val datebase = Firebase.database
    val document = datebase.getReference("results")

    for (i: Int in 5..9) {
        val result = Results(i)
        document.child("resultid-${i}").setValue(result)
    }
}

fun addAnswers() {
    val datebase = Firebase.database
    val document = datebase.getReference("answers")

    for (i: Int in 1..12) {
        val answer = Answers(i)
        document.child("questionid-${i}").setValue(answer)
    }
}

class Users {
    var profileAddress = "프로필 주소"
    var name  = "이름"
    var gender = "성별"
    var dataOfBirth = ""
    var phone = ""
    var address = "주소"
    var userType = ""
    var token = ""

    constructor()
    constructor(i: Int) {
        this.profileAddress = "프로필 주소 ${i}"
        this.name  = "이름 ${i}"
        this.gender = "성별 ${i}"
        this.dataOfBirth = "1999/01/0${i}"
        this.phone = "010-1234-123${i}"
        this.address = "주소 ${i}"
        this.token = "토큰 ${i}"
    }
}

class Guardians {
    lateinit var myRegardList: ArrayList<String>
    lateinit var myQuestionList: ArrayList<String>
    lateinit var connectedUserList: ArrayList<String>

    constructor()
    constructor(i: Int) {
        this.myRegardList = arrayListOf("regerdid-${1+3*(i-1)}", "regerdid-${2+3*(i-1)}", "regerdid-${3+3*(i-1)}")
        this.myQuestionList = arrayListOf("question-${1+3*(i-1)}", "question-${2+3*(i-1)}","question-${3+3*(i-1)}")
        this.connectedUserList = arrayListOf("userid-5", "userid-6", "userid-7")
    }
}

class Wards {
    lateinit var connectedRegardsList: ArrayList<String>
    lateinit var resultList: ArrayList<String>
    lateinit var requestedList: ArrayList<String>
    lateinit var connectedUserList: ArrayList<String>

    constructor()
    constructor(i: Int) {
        this.connectedRegardsList = arrayListOf("regerdid-${1+3*(i-1)}", "regerdid-${2+3*(i-1)}", "regerdid-${3+3*(i-1)}")
        this.resultList = arrayListOf("resultid-${i}")
        this.requestedList = arrayListOf("userid-4")
        this.connectedUserList = arrayListOf("userid-1", "userid-2", "userid-3")
    }
}

class Regards {
    var name = "안부 이름"
    lateinit var questionList: ArrayList<String>
    var warningTime = "5분"
    var alarmWeek = "월화수목금토일"
    var alarmTime = "13시14분"

    constructor()
    constructor(i: Int) {
        this.name = "안부 이름 ${i}"
    }
}

class Questions {
    var owner_id = ""
    var text = "질문 내용"
    var secretOn: Boolean = false
    var recordOn: Boolean = false
    var recordAddress = "녹음 질문 파일 주소"

    constructor()
    constructor(i: Int) {
        this.owner_id = "userid-${1+((i-1)/3)}"
        this.text = "질문 내용 ${i}"
        this.recordAddress = "녹음 질문 파일 주소 ${i}"
    }
}

class Results {
    var date = ""
    var regard_id = ""
    var responseTime = ""
    lateinit var answerList: ArrayList<String>

    constructor()
    constructor(i: Int) {
        this.date = "2022/07/02"
        this.regard_id = "regardid-${i}"
        this.responseTime = "01시간05분24초"
        this.answerList = arrayListOf("answerid-${1+3*(i-5)}", "answerid-${2+3*(i-5)}", "answerid-${3+3*(i-5)}")
    }
}

class Answers {
    var answer = ""
    var recordAddress = "녹음 응답 파일 주소"

    constructor()
    constructor(i: Int) {
        this.answer = "긍정"
        this.recordAddress = "녹음 응답 파일 주소 ${i}"
    }
}
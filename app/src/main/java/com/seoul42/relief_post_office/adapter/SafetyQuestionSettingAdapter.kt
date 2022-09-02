package com.seoul42.relief_post_office.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.Log
import android.util.SparseBooleanArray
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.util.remove
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.NotificationDTO
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.record.EditRecordActivity
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 안부의 질문을 설정하는 화면에서, 질문들을 RecyclerView에 듸우기 위한 adapter 클래스
 *  - context : "SafetyQuestionSettingActivity"의 context
 *  - checkedQuestions : 기존에 안부에 설정되어 있던 질문들을 담은 리스트
 *  - items : 로그인한 보호자가 소유한 질문들을 담은 리스트
 *  - firebaseViewModel : FCM을 보내는 기능을 가진 클래스
 */
class SafetyQuestionSettingAdapter(
    private val context: Context,
    val checkedQuestions : ArrayList<String>,
    private val items : ArrayList<Pair<String, QuestionDTO>>,
    private val firebaseViewModel: FirebaseViewModel
)
    : RecyclerView.Adapter<SafetyQuestionSettingAdapter.ViewHolder>() {

    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    private val owner : String = Firebase.auth.currentUser!!.uid
    val database = Firebase.database
    // 선택된 질문들의 상태를 담을 배열
    val checkboxStatus = SparseBooleanArray()
    // 선택 해제된 질문들의 상태를 담을 배열
    val deletedQuestions = arrayListOf<String>()

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        // 각 아이템마다 뷰 처리
        /**
         * 각 질문들 마다 정보를 가져와서 세팅해주는 메서드
         *  - item : <질문 id, 질문 DTO>
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun bindItems(item: Pair<String, QuestionDTO>){
            // 텍스트 세팅
            val rvText = itemView.findViewById<TextView>(R.id.safety_question_item_text)
            rvText.text = item.second.text

            // 기존 안부에 있던 질문들인지 확인하고 스위치 세팅
            val itemCheckBox = itemView.findViewById<Switch>(R.id.safety_question_item_switch)
            if (checkedQuestions.contains(item.first)){
                itemCheckBox.isChecked = true
                checkboxStatus.put(adapterPosition, true)
            }
            else
                checkboxStatus.put(adapterPosition, false)

            // 질문 눌렀을 때 질문 수정 다이얼로그 띄우기
            rvText.setOnClickListener{
                // 아이템 여러번 눌리는 것 방지
                it.isClickable = false
                // 질문 수정 다이얼로그 세팅
                setEditQuestionDialog(item)
            }

            // 체크 박스 처리
            if (checkboxStatus[adapterPosition] != null)
                itemCheckBox.isChecked = checkboxStatus[adapterPosition]
            itemCheckBox.setOnClickListener {
                if (!(itemCheckBox.isChecked)) {
                    deletedQuestions.add(item.first)
                    checkboxStatus.put(adapterPosition, false)
                    checkedQuestions.remove(item.first)
                }
                else {
                    deletedQuestions.remove(item.first)
                    checkboxStatus.put(adapterPosition, true)
                    checkedQuestions.add(item.first)
                }
                notifyItemChanged(adapterPosition)
            }

            // 재생 버튼 세팅
            setRecordPlayBtn(item.second.src!!)
        }

        /**
         * 사용자가 질문을 클릭 시 생성되는 질문 수정 다이얼로그를 세팅해주는 메서드
         */
        private fun setEditQuestionDialog(item : Pair<String, QuestionDTO>){
            // 질문 수정 다이얼로그 세팅
            val questionText = item.second.text
            val secret = item.second.secret
            val record = item.second.record
            val src = item.second.src

            val dialog = android.app.AlertDialog.Builder(context).create()
            val eDialog : LayoutInflater = LayoutInflater.from(context)
            val mView : View = eDialog.inflate(R.layout.setting_question_dialog2,null)

            dialog.setView(mView)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.create()

            dialog.findViewById<EditText>(R.id.question_text2).setText(questionText) // 텍스트 세팅
            dialog.findViewById<Switch>(R.id.secret_switch2).isChecked = secret   // 비밀 스위치 세팅
            dialog.findViewById<Switch>(R.id.record_switch2).isChecked = record   // 녹음 스위치 세팅

            // 저장되어 있는 녹음가지고 와서 세팅
            var editRecordActivity = EditRecordActivity(src.toString(), mView)
            editRecordActivity.initViews()
            editRecordActivity.bindViews(mView)
            editRecordActivity.initVariables()

            // 다이얼로그 종료 시 이벤트
            dialog.setOnDismissListener {
                editRecordActivity.stopRecording()
                editRecordActivity.stopPlaying()
                // 아이템 클릭 방지 해제
                itemView.findViewById<TextView>(R.id.safety_question_item_text).isClickable = true
            }

            // 질문 수정 다이얼로그의 "저장" 버튼을 눌렀을 때 이벤트 처리
            dialog.findViewById<Button>(R.id.save_question_btn).setOnClickListener {
                it.isClickable = false

                // 프로그레스바 처리
                dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                val progressBar = dialog.findViewById<ProgressBar>(R.id.setting_question_progressbar2)
                progressBar.visibility = View.VISIBLE

                // 녹음 중이라면 중단 후 저장
                editRecordActivity.stopRecording()
                // 재생 중이라면 재생 중단
                editRecordActivity.stopPlaying()

                // 텍스트, 비밀 옵션, 녹음 옵션
                val editedQuestionText = dialog.findViewById<EditText>(R.id.question_text2).text.toString()
                val editedSecret = dialog.findViewById<Switch>(R.id.secret_switch2).isChecked
                val editedRecord = dialog.findViewById<Switch>(R.id.record_switch2).isChecked

                // question 컬렉션에 수정된 질문 내용 수정
                val question = database.getReference("question").child(item.first)
                question.child("text").setValue(editedQuestionText)
                question.child("secret").setValue(editedSecret)
                question.child("record").setValue(editedRecord)

                // EditRecordActivity에서 받은 녹음파일 변경주소 반영
                var editRecordFile = Uri.fromFile(File(editRecordActivity.returnRecordingFile()))
                val editRecordRef =
                    storage.reference.child("questionRecord/${item.second.owner}/${item.second.owner + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}")
                var uploadEditRecord = editRecordRef.putFile(editRecordFile)

                uploadEditRecord.addOnSuccessListener {
                    editRecordRef.downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            question.child("src").setValue(task.result.toString())

                            // 로그인한 보호자의 questionList와 question 컬렉션의 수정된 질문의 최종 수정날짜 수정
                            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            question.child("date").setValue(date)
                            database.getReference("guardian").child(item.second.owner.toString())
                                .child("questionList")
                                .child(item.first).setValue(date)

                            // 질문과 연결된 안부를 가진 피보호자들에게 안부 동기화 fcm 전송
                            sendFCM(item, "SafetyWard", "안부를 동기화 합니다")
                        }
                    }
                    // 수정하였지만 녹음을 바꾸진 않은 경우
                }.addOnFailureListener{
                    // 로그인한 보호자의 questionList와 question 컬렉션의 수정된 질문의 최종 수정날짜 수정
                    val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    question.child("date").setValue(date)
                    database.getReference("guardian").child(item.second.owner.toString())
                        .child("questionList")
                        .child(item.first).setValue(date)

                    // 질문과 연결된 안부를 가진 피보호자들에게 안부 동기화 fcm 전송
                    sendFCM(item, "SafetyWard", "안부를 동기화 합니다")
                }
                // 다이얼로그 종료
                Handler().postDelayed({
                    Toast.makeText(context, "질문 수정 완료", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }, 1000)
            }

            // 질문 수정 다이얼로그의 "삭제" 버튼을 눌렀을 때 이벤트 처리
            dialog.findViewById<Button>(R.id.delete_question_btn).setOnClickListener {
                it.isClickable = false

                // 프로그레스바 처리
                dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                val progressBar = dialog.findViewById<ProgressBar>(R.id.setting_question_progressbar2)
                progressBar.visibility = View.VISIBLE

                // 만약 질문에 연결된 안부가 있다면 삭제 불가
                if (item.second.connectedSafetyList.isEmpty()) {
                    // 해당 질문 id를 통해 데이터베이스에서 삭제
                    database.getReference("question").child(item.first).setValue(null)
                    // 로그인한 보호자의 질문 목록에서 해당하는 질문id 삭제하기
                    database.getReference("guardian")
                        .child(item.second.owner.toString())
                        .child("questionList")
                        .child(item.first).setValue(null)

                    // 현재 아답터에서 가지고 있는 리스트에 해당 질문 삭제 적용
                    checkboxStatus.delete(adapterPosition)
                    checkedQuestions.remove(item.first)

                    // 해당 아이템 체크 해제
                    checkedQuestions.remove(item.first)

                    // 다이얼로그 종료
                    Toast.makeText(context, "질문 삭제 완료", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                else{
                    Toast.makeText(context, "질문이 포함된 안부가 있습니다", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            // 질문 수정 다이얼로그 띄우기
            dialog.show()
        }

        /**
         * RecyclerView에 있는 녹음 재생 버튼 동작 세팅해주는 메서드
         *  - recordSrc : 재생할 녹음 파일의 주소
         */
        private fun setRecordPlayBtn(recordSrc : String) {
            // 재생 버튼 클릭 이벤트
            var playing = false
            var player: MediaPlayer? = null
            val playerBtn = itemView.findViewById<ImageView>(R.id.safety_question_item_play_button)
            playerBtn.setOnClickListener{
                // 재생 중이면, 재생 버튼으로 이미지 변경
                if (playing){
                    player?.release()
                    player = null

                    playerBtn.setImageResource(R.drawable.playbtn)
                    playing = false
                }
                // 재생 중이 아니면, 녹음 재생 후 중지 버튼으로 이미지 변경
                else{
                    // 녹음 소스 불러와서 미디어 플레이어 세팅
                    player = MediaPlayer().apply {
                        setDataSource(recordSrc)
                        prepare()
                    }

                    player?.setOnCompletionListener {
                        player?.release()
                        player = null

                        playerBtn.setImageResource(R.drawable.playbtn)
                        playing = false
                    }
                    // 재생
                    player?.start()
                    playerBtn.setImageResource(R.drawable.stopbtn)
                    playing = true
                }
            }
        }

        /**
         *  질문 동기화 FCM을 보내는 메서드
         *  보호자가 질문 수정 시, 해당 질문을 포함한 안부를 가지고 있는 모든 피보호자에게 안부 동기화를 요청하기 위함
         *  - item : <질문 id, 질문 DTO>
         *  - title : FCM에 포함될 title
         *  - message : FCM에 포함될 message
         */
        private fun sendFCM(
            item : Pair<String, QuestionDTO>,
            title : String,
            message : String,
        ){
            val safetyListRef = database.getReference("question")
                .child(item.first).child("connectedSafetyList")
            safetyListRef.get().addOnSuccessListener {
                if (it.getValue() != null) {
                    val safetyList =
                        (it.getValue() as HashMap<String, String>).keys.toList()
                    val UserRef = database.getReference("user")
                    for (safetyId in safetyList) {
                        database.getReference("safety").child(safetyId).child("uid").get()
                            .addOnSuccessListener {
                                UserRef.child(it.getValue().toString())
                                    .child("token").get().addOnSuccessListener {
                                        val notificationData = NotificationDTO.NotificationData(title, "안심우체국", message)
                                        val notificationDTO = NotificationDTO(it.getValue().toString(), "high", notificationData)
                                        firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
                                        Log.d("test", safetyId)
                                        Log.d("test2", it.getValue().toString())
                                    }
                            }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : SafetyQuestionSettingAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.safety_question_item, parent, false)

        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: SafetyQuestionSettingAdapter.ViewHolder, position: Int) {
        if (items[position].second.owner == owner) {
            holder.bindItems(items[position])
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
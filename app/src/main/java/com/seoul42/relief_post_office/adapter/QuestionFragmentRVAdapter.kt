package com.seoul42.relief_post_office.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.guardian.QuestionFragment
import com.seoul42.relief_post_office.model.NotificationDTO
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.record.EditRecordActivity
import com.seoul42.relief_post_office.record.RecordActivity
import com.seoul42.relief_post_office.record.RecordState
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class QuestionFragmentRVAdapter(
    private val context: Context,
    private val items: ArrayList<Pair<String, QuestionDTO>>,
    private val firebaseViewModel: FirebaseViewModel,
)
    : RecyclerView.Adapter<QuestionFragmentRVAdapter.ViewHolder>() {

    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    val database = Firebase.database
    val owner = Firebase.auth.currentUser!!.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
    : QuestionFragmentRVAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.question_rv_item, parent, false)

        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: QuestionFragmentRVAdapter.ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    // 전체 리사이클러 뷰의 아이템 개수
    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){

        // 데이터 매핑 해주기
        @RequiresApi(Build.VERSION_CODES.O)
        fun bindItems(item: Pair<String, QuestionDTO>){
            // 각 질문 별 텍스트
            val rvText = itemView.findViewById<TextView>(R.id.question_rv_item_text)
            rvText.text = item.second.text

            // 아이템 눌렀을 때 이벤트
            rvText.setOnClickListener{
                // 아이템 여러번 눌리는 것 방지
                rvText.isClickable = false

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


                // 질문 수정 다이얼로그 띄우기
                dialog.show()

                // 저장되어 있는 녹음가지고 와서 세팅
                var editRecordActivity = EditRecordActivity(src.toString(), mView)
                editRecordActivity.initViews()
                editRecordActivity.bindViews(mView)
                editRecordActivity.initVariables()

                // 다이얼로그 종료 시 이벤트
                dialog.setOnDismissListener {
                    editRecordActivity.stopRecording()
                    editRecordActivity.stopPlaying()
                    // 아이템 터치 다시 가능하게 하기
                    rvText.isClickable = true
                }

                // 질문 수정 다이얼로그의 "저장" 버튼을 눌렀을 때 이벤트 처리
                dialog.findViewById<Button>(R.id.save_question_btn).setOnClickListener {

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

                                // 로그인한 보호자와 연결된 모든 피보호자에게 안부 동기화 fcm 전송
                                val wardListRef = database.getReference("guardian").child(item.second.owner.toString()).child("connectList")
                                wardListRef.get().addOnSuccessListener {
                                    val wardList = (it.getValue() as HashMap<String, String>).values.toList()
                                    val UserRef = database.getReference("user")
                                    for (wardId in wardList){
                                        UserRef.child(wardId).child("token").get().addOnSuccessListener {
                                            val notificationData = NotificationDTO.NotificationData("SafetyWard",
                                                "안심우체국", "안부를 동기화 합니다")
                                            val notificationDTO = NotificationDTO(it.getValue().toString()!!, notificationData)
                                            firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
                                        }
                                    }
                                }

                                // 다이얼로그 종료
                                Toast.makeText(context, "질문 수정 완료", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
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

                        // 로그인한 보호자와 연결된 모든 피보호자에게 안부 동기화 fcm 전송
                        val wardListRef = database.getReference("guardian").child(item.second.owner.toString()).child("connectList")
                        wardListRef.get().addOnSuccessListener {
                            if (it.getValue() != null) {
                                val wardList =
                                    (it.getValue() as HashMap<String, String>).values.toList()
                                val UserRef = database.getReference("user")
                                for (wardId in wardList) {
                                    UserRef.child(wardId).child("token").get()
                                        .addOnSuccessListener {
                                            val notificationData = NotificationDTO.NotificationData(
                                                "SafetyWard",
                                                "안심우체국", "안부를 동기화 합니다"
                                            )
                                            val notificationDTO = NotificationDTO(
                                                it.getValue().toString()!!,
                                                notificationData
                                            )
                                            firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
                                        }
                                }
                            }
                        }

                        // 다이얼로그 종료
                        Toast.makeText(context, "질문 수정 완료", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }

                // 질문 수정 다이얼로그의 "삭제" 버튼을 눌렀을 때 이벤트 처리
                dialog.findViewById<Button>(R.id.delete_question_btn).setOnClickListener {

                    // 프로그레스바 처리
                    dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    val progressBar = dialog.findViewById<ProgressBar>(R.id.setting_question_progressbar2)
                    progressBar.visibility = View.VISIBLE

                    // 만약 질문에 연결된 안부가 있다면 삭제 불가
                    if (item.second.connectedSafetyList.isEmpty()){
                        // 해당 질문 id를 통해 데이터베이스에서 삭제
                        database.getReference("question").child(item.first).setValue(null)
                        // 로그인한 보호자의 질문 목록에서 해당하는 질문id 삭제하기
                        database.getReference("guardian")
                            .child(item.second.owner.toString())
                            .child("questionList")
                            .child(item.first).setValue(null)

                        // 다이얼로그 종료
                        Toast.makeText(context, "질문 삭제 완료", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    else{
                        Toast.makeText(context, "질문이 포함된 안부가 있습니다", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }

            // 재생 버튼 클릭 이벤트
            var playing = false
            var player: MediaPlayer? = null
            val playerBtn = itemView.findViewById<ImageView>(R.id.question_rv_item_playBtn)
            playerBtn.setOnClickListener{
                // 재생 중이면 재생 버튼으로 이미지 변경
                if (playing){
                    player?.release()
                    player = null

                    playerBtn.setImageResource(R.drawable.playbtn)
                    playing = false
                }
                // 재생 중이 아니면 중지 버튼으로 이미지 변경
                else{
                    // 녹음 소스 불러와서 미디어 플레이어 세팅
                    player = MediaPlayer().apply {
                        setDataSource(item.second.src)
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
    }
}
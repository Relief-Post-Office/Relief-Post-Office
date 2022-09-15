package com.seoul42.relief_post_office.adapter

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.model.SafetyDTO

/**
 * 피보호자의 "안부 추가"화면에서 질문들을 RecyclerView에 띄우기 위한 adapter 클래스
 *  - items : 화면에 나타낼 질문들을 담은 리스트
 */
class AddWardSafetyAdapter(private val items: ArrayList<Pair<String, QuestionDTO>>)
    : RecyclerView.Adapter<AddWardSafetyAdapter.ViewHolder>(){

    // 로그인한 보호자의 uid
    private var owner = Firebase.auth.currentUser?.uid.toString()

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        /**
         * 각 질문들의 정보를 받아와서 RecyclerView에 각각 세팅해주는 메서드
         * - item : <질문 id, 질문 DTO>
         */
        fun bindItems(item: Pair<String, QuestionDTO>) {
            val rvText = itemView.findViewById<TextView>(R.id.question_rv_item_text)
            // 비밀 옵션이 켜져있는 질문이면 질문 생성자를 제외한 이들이 확인 할 수 없도록 세팅
            if (item.second.secret && item.second.owner != owner) {
                rvText.text = "비밀 편지"
            } else {
                // text 세팅
                rvText.text = item.second.text

                // 녹음 재생 버튼 세팅
                setRecordPlayBtn(item.second.src!!)
            }
        }

        /**
         * RecyclerView에 있는 녹음 재생 버튼 동작 세팅해주는 메서드
         *  - recordSrc : 재생할 녹음 파일의 주소
         */
        private fun setRecordPlayBtn(recordSrc : String) {
            var playing = false
            var player: MediaPlayer? = null
            val playerBtn = itemView.findViewById<ImageView>(R.id.question_rv_item_playBtn)
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
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddWardSafetyAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.question_rv_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddWardSafetyAdapter.ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
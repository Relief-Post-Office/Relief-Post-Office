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

class AddWardSafetyAdapter(private val items: ArrayList<Pair<String, QuestionDTO>>)
    : RecyclerView.Adapter<AddWardSafetyAdapter.ViewHolder>(){

    private var owner = Firebase.auth.currentUser?.uid.toString()

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

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        // 각 아이템마다 뷰 처리
        fun bindItems(item: Pair<String, QuestionDTO>){
            // 각 질문 별 세팅
            val rvText = itemView.findViewById<TextView>(R.id.question_rv_item_text)
            // 만약 비밀 옵션이 켜져있는 질문이면 질문의 주인을 제외하고 내용을 확인할 수 없도록 설정
            if (item.second.secret && item.second.owner != owner){
                // text 세팅
                rvText.text = "비밀 편지"
            }
            else{
                // text 세팅
                rvText.text = item.second.text

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

}
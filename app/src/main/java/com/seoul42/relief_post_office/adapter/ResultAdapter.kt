package com.seoul42.relief_post_office

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.databinding.ItemResultBinding
import com.seoul42.relief_post_office.model.ResultDTO

class ResultAdapter(private val resultList: MutableList<Pair<String, ResultDTO>>)
    : RecyclerView.Adapter<ResultAdapter.ResultHolder>() {
    inner class ResultHolder(val binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root){
        @SuppressLint("NotifyDataSetChanged")
        fun setResult(result: Pair<String, ResultDTO>) {
            val database = Firebase.database
            val regardRef = database.getReference("safety")
            regardRef.child(result.second.safetyId).child("name").get().addOnSuccessListener {
                binding.regardName.text = it.value.toString()
                notifyDataSetChanged()
            }
            regardRef.child(result.second.safetyId).child("alarmTime").get().addOnSuccessListener {
                binding.alarmTime.text = it.value.toString()
                notifyDataSetChanged()
            }
            binding.responseTime.text = result.second.responseTime.toString()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultHolder {
        val binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return ResultHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultHolder, position: Int) {
        val result = resultList.get(position)
        holder.setResult(result)
    }

    override fun getItemCount(): Int {
        return resultList.size
    }
}
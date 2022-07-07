package io.grpc.examples.helloworld.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.grpc.examples.helloworld.R
import io.grpc.examples.helloworld.dto.ChatItem

class MainAdapter(private val chatList: ArrayList<ChatItem>): RecyclerView.Adapter<MainViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return  MainViewHolder(view = view)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.onBind(chatItem = chatList[position])
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun notifyItemSet(position: Int) {
        this.notifyDataSetChanged()
    }
}
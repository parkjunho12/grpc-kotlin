package io.grpc.examples.helloworld.adapter

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.grpc.examples.helloworld.R
import io.grpc.examples.helloworld.dto.ChatItem

class MainViewHolder(private val view: View): RecyclerView.ViewHolder(view) {

    fun onBind(chatItem: ChatItem) {
        if (chatItem.isMy) {
            val myChat = view.findViewById<TextView>(R.id.my_chat)
            myChat.isVisible = true
            view.findViewById<TextView>(R.id.your_con).isVisible = true
            view.findViewById<TextView>(R.id.my_id).text = chatItem.pNumber
            myChat.text = chatItem.chat
        } else {
            val yourChat = view.findViewById<TextView>(R.id.your_chat)
            view.findViewById<TextView>(R.id.my_con).isVisible = false
            view.findViewById<TextView>(R.id.your_id).text = chatItem.pNumber
            yourChat.isVisible = true
            yourChat.text = chatItem.chat
        }

    }
}
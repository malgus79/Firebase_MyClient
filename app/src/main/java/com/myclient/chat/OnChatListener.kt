package com.myclient.chat

import com.myclient.entities.Message

interface OnChatListener {
    fun deleteMessage(message: Message)
}
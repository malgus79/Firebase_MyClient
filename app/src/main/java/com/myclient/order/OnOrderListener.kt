package com.myclient.order

import com.myclient.entities.Order

interface OnOrderListener {
    fun onTrack(order: Order)
    fun onStartChat(order: Order)
}
package com.myclient.cart

import com.myclient.entities.Product

interface OnCartListener {
    fun setQuantity(product: Product)
    fun showTotal(total: Double)
}
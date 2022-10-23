package com.myclient.product

import com.myclient.entities.Product

interface OnProductListener {
    fun onClick(product: Product)
}
package com.myclient.product

import com.google.firebase.auth.FirebaseUser
import com.myclient.entities.Product

interface MainAux {
    fun getProductsCart(): MutableList<Product>
    fun updateTotal()
    fun clearCart()

    fun getProductSelected(): Product?
    fun showButton(isVisible: Boolean)
    fun addProductToCart(product: Product)

    fun updateTitle(user: FirebaseUser)
}
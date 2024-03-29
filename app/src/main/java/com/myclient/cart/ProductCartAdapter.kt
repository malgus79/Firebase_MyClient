package com.myclient.cart

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.myclient.R
import com.myclient.databinding.ItemProductCartBinding
import com.myclient.entities.Product

class ProductCartAdapter(private val productList: MutableList<Product>,
                         private val listener: OnCartListener) :
    RecyclerView.Adapter<ProductCartAdapter.ViewHolder>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_product_cart, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        holder.setListener(product)

        holder.binding.tvName.text = product.name
        holder.binding.tvQuantity.text = product.newQuantity.toString()

        Glide.with(context)
            .load(product.imgUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_access_time)
            .error(R.drawable.ic_broken_image)
            .centerCrop()
            .circleCrop()
            .into(holder.binding.imgProduct)
    }

    override fun getItemCount(): Int = productList.size

    fun add(product: Product){
        if (!productList.contains(product)){
            productList.add(product)
            notifyItemInserted(productList.size - 1)
            calcTotal()
        } else {
            update(product)
        }
    }

    fun update(product: Product){
        val index = productList.indexOf(product)
        if (index != -1){
            productList.set(index, product)
            notifyItemChanged(index)
            calcTotal()
        }
    }

    fun delete(product: Product){
        val index = productList.indexOf(product)
        if (index != -1){
            productList.removeAt(index)
            notifyItemRemoved(index)
            calcTotal()
        }
    }

    //calcular el total del carrito
    private fun calcTotal(){
        var result = 0.0
        for (product in productList){
            result += product.totalPrice()
        }
        listener.showTotal(result)
    }

    //para recibir en la fun requestOrder() del fragment
    fun getProducts(): List<Product> = productList

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val binding = ItemProductCartBinding.bind(view)

        //incrementar cantidad
        fun setListener(product: Product){
            binding.ibSum.setOnClickListener {
                if (product.newQuantity < product.quantity) {
                    product.newQuantity += 1
                    listener.setQuantity(product)
                }
            }
            //disminuir cantidad
            binding.ibSub.setOnClickListener {
                if (product.newQuantity > 0) {
                    product.newQuantity -= 1
                    listener.setQuantity(product)
                }
            }
        }
    }
}
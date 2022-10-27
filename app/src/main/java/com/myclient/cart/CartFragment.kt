package com.myclient.cart

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.myclient.Constants
import com.myclient.R
import com.myclient.databinding.FragmentCartBinding
import com.myclient.entities.Order
import com.myclient.entities.Product
import com.myclient.entities.ProductOrder
import com.myclient.order.OrderActivity
import com.myclient.product.MainAux

class CartFragment : BottomSheetDialogFragment(), OnCartListener {

    private var binding: FragmentCartBinding? = null

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private lateinit var adapter: ProductCartAdapter

    private var totalPrice = 0.0

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentCartBinding.inflate(LayoutInflater.from(activity))
        binding?.let {
            val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
            bottomSheetDialog.setContentView(it.root)

            //instanciar las var
            bottomSheetBehavior = BottomSheetBehavior.from(it.root.parent as View)
            //manipular el comportamiento a traves del estado
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

   /*
 // para instanciarlo en un fragment
 val fragment = BottomSheetFragment()
 fragment.show(fragmentManager!!.beginTransaction(), BottomSheetFragment.TAG)
   */

            setupRecyclerView()
            setupButtons()

            getProducts()
            configAnalytics()

            return bottomSheetDialog
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun setupRecyclerView() {
        binding?.let {
            adapter = ProductCartAdapter(mutableListOf(), this)

            it.recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@CartFragment.adapter
            }

            /*(1..5).forEach {
                val product = Product(it.toString(), "Producto $it", "This product is $it",
                    "", it, 2.0*it)
                adapter.add(product)
            }*/
        }
    }

    //boton de cerrar del bottomSheet
    private fun setupButtons(){
        binding?.let {
            it.ibCancel.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            //extraer el pedido en una orden
            it.efab.setOnClickListener {
                requestOrder()
            }
        }
    }

    private fun getProducts(){
        (activity as? MainAux)?.getProductsCart()?.forEach {
            adapter.add(it)
        }
    }

    //analytics
    private fun configAnalytics(){
        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW){
            param(FirebaseAnalytics.Param.METHOD, "check_track")
        }
    }

    //cerrar el fragmento y limpiar el listado
    private fun requestOrder(){
        //extraer el usuario autenticado para usar su uid
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { myUser ->

            //desabilitar la interfaz
            enableUI(false)

            //obtener el nuevo array de ProductOrder
            val products = hashMapOf<String, ProductOrder>()
            adapter.getProducts().forEach { product ->
                products.put(product.id!!, ProductOrder(product.id!!, product.name!!, product.newQuantity))
                //products.put(product.id!!, ProductOrder(product.id!!, product.name!!, product.newQuantity, product.partnerId))
            }

            val order = Order(clientId = myUser.uid, products = products, totalPrice = totalPrice, status = 1)

            val db = FirebaseFirestore.getInstance()
            db.collection(Constants.COLL_REQUESTS)
                .add(order)
                .addOnSuccessListener {
                    //cerrar fragmento y limpiar carrito y lanzar orderAct
                    dismiss()  //quitar el fragment
                    (activity as? MainAux)?.clearCart()
                    startActivity(Intent(context, OrderActivity::class.java))

                    Toast.makeText(activity, "Compra realizada.", Toast.LENGTH_SHORT).show()
                    //analytics
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_PAYMENT_INFO){
                        val products = mutableListOf<Bundle>()
                        order.products.forEach {
                            //en caso de que se compren mas de 5 productos (mayoreo)
                            if (it.value.quantity > 5) {
                                val bundle = Bundle()
                                bundle.putString("id_product", it.key)
                                products.add(bundle)
                            }
                        }
                        param(FirebaseAnalytics.Param.QUANTITY, products.toTypedArray())
                    }
                    firebaseAnalytics.setUserProperty(Constants.USER_PROP_QUANTITY,
                        //al menos 1 producto supero el "mayoreo"
                        if (products.size > 0) "con_mayoreo" else "sin_mayoreo")
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Error al comprar.", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    //habilitar la interfaz
                    enableUI(true)
                }
        }
    }

    //sesabilitar boton de cerrar y pagar en el carrito
    private fun enableUI(enable: Boolean){
        binding?.let {
            it.ibCancel.isEnabled = enable
            it.efab.isEnabled = enable
        }
    }

    //volver a binding = null (buenas practicas)
    override fun onDestroyView() {
        (activity as? MainAux)?.updateTotal()  //para refrescar el total en la mainActivity
        super.onDestroyView()
        binding = null
    }

    //obtener las cantidades del carrito (para modif sus cantitades desde ahi)
    override fun setQuantity(product: Product) {
        adapter.update(product)
    }

    //actualizar el total del carrito en el fragment (lo detona el adapter)
    override fun showTotal(total: Double) {
        totalPrice = total
        binding?.let {
            it.tvTotal.text = getString(R.string.product_full_cart, total)
        }
    }
}
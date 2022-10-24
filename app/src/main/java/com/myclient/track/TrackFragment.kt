package com.myclient.track

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.myclient.Constants
import com.myclient.R
import com.myclient.databinding.FragmentTrackBinding
import com.myclient.entities.Order
import com.myclient.order.OrderAux

class TrackFragment : Fragment() {

    private var binding: FragmentTrackBinding? = null

    private var order: Order? = null

//    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTrackBinding.inflate(inflater, container, false)
        binding?.let {
            return  it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getOrder()
    }

    //obtener la orden
    private fun getOrder() {
        order = (activity as? OrderAux)?.getOrderSelected()

        /*en base a esta orden se va a :
         -) actualizar la interfaz
         -) lanzar la consulta para que se reflejen los cambios en tiempo real
         */


        order?.let {
            updateUI(it)  //que ckeckBox seleccionar

            getOrderInRealtime(it.id)  //obtener orden en tiempo real

//            setupActionBar()
//            configAnalytics()
        }
    }

    //que ckeckBox seleccionar
    private fun updateUI(order: Order) {
        binding?.let {
            it.progressBar.progress = order.status * (100/3) - 15

            it.cbOrdered.isChecked = order.status > 0
            it.cbPreparing.isChecked = order.status > 1
            it.cbSent.isChecked = order.status > 2
            it.cbDelivered.isChecked = order.status > 3
        }
    }

    //obtener orden en tiempo real
    private fun getOrderInRealtime(orderId: String){
        val db = FirebaseFirestore.getInstance()

        val orderRef = db.collection(Constants.COLL_REQUESTS).document(orderId)
        orderRef.addSnapshotListener { snapshot, error ->
            //si existe error
            if (error != null){
                Toast.makeText(activity, "Error al consultar esta orden.", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            //sino hubo error -> proseguir
            if (snapshot != null && snapshot.exists()){
                val order = snapshot.toObject(Order::class.java)
                order?.let {
                    it.id = snapshot.id

                    //que ckeckBox seleccionar
                    updateUI(it)
                }
            }
        }
    }

//    private fun setupActionBar(){
//        (activity as? AppCompatActivity)?.let {
//            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
//            it.supportActionBar?.title = getString(R.string.track_title)
//            setHasOptionsMenu(true)
//        }
//    }

//    private fun configAnalytics(){
//        firebaseAnalytics = Firebase.analytics
//        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW){
//            param(FirebaseAnalytics.Param.METHOD, "check_track")
//        }
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == android.R.id.home){
//            activity?.onBackPressed()
//        }
//        return super.onOptionsItemSelected(item)
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

//    override fun onDestroy() {
//        (activity as? AppCompatActivity)?.let {
//            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
//            it.supportActionBar?.title = getString(R.string.order_title)
//            setHasOptionsMenu(false)
//        }
//        super.onDestroy()
//    }
}
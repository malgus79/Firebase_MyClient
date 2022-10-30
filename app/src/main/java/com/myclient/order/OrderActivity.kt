package com.myclient.order

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.myclient.Constants
import com.myclient.R
import com.myclient.chat.ChatFragment
import com.myclient.databinding.ActivityOrderBinding
import com.myclient.entities.Order
import com.myclient.track.TrackFragment

class OrderActivity : AppCompatActivity(), OnOrderListener, OrderAux {

    private lateinit var binding: ActivityOrderBinding

    private lateinit var adapter: OrderAdaper

    private lateinit var orderSelected: Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFirestore()

        checkIntent(intent)
    }

    private fun checkIntent(intent: Intent?) {
        intent?.let {
            val actionIntent = it.getIntExtra(Constants.ACTION_INTENT, 0)
            if (actionIntent == 1) {
                val id = intent.getStringExtra(Constants.PROP_ID) ?: ""
                val status = intent.getIntExtra(Constants.PROP_STATUS, 0)
                orderSelected = Order(id = id, status = status)

                val fragment = TrackFragment()
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.containerMain, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    //config recyclerView
    private fun setupRecyclerView() {
        adapter = OrderAdaper(mutableListOf(), this)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OrderActivity)
            adapter = this@OrderActivity.adapter
        }
    }

    //consultas a firestore
    private fun setupFirestore(){
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val db = FirebaseFirestore.getInstance()

            db.collection(Constants.COLL_REQUESTS)
                //.orderBy(Constants.PROP_DATE, Query.Direction.ASCENDING)  //ordenar por fecha asc
                //.orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING)  //ordenar por fecha desc
                //.whereEqualTo(Constants.PROP_CLIENT_ID, user.uid)  //cada cliente solo vea sus ordenes
                //.whereIn(Constants.PROP_STATUS, listOf(1, 4))  //ordenes con status 1 y 4 (tiene que ser en forma de array)
                //.whereNotIn(Constants.PROP_STATUS, listOf(4))  //traer todoo lo que no coincida con el array
                //.whereGreaterThan(Constants.PROP_STATUS, 2)  //mayor que
                //.whereLessThan(Constants.PROP_STATUS, 4)  //menor que
                //.whereEqualTo(Constants.PROP_STATUS, 3)  //igual que
                //.whereGreaterThanOrEqualTo(Constants.PROP_STATUS, 2)  //mayor o igual que
                /*.whereEqualTo(Constants.PROP_CLIENT_ID, user.uid)
                .orderBy(Constants.PROP_STATUS, Query.Direction.DESCENDING)*/
                .whereEqualTo(Constants.PROP_CLIENT_ID, user.uid)
                /*.orderBy(Constants.PROP_STATUS, Query.Direction.ASCENDING)
                .whereLessThan(Constants.PROP_STATUS, 4)*/
                .orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener {
                    for (document in it){
                        val order = document.toObject(Order::class.java)
                        order.id = document.id
                        adapter.add(order)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al consultar los datos.", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    //lanzar el fragmentTrack
    override fun onTrack(order: Order) {
        //orden que envia el adaptador
        orderSelected = order

        val fragment = TrackFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()
    }

    //iniciar el fragment del chat
    override fun onStartChat(order: Order) {
        orderSelected = order

        val fragment = ChatFragment()

        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()
    }

    //obtener la orden seleccionada
   override fun getOrderSelected(): Order = orderSelected
}
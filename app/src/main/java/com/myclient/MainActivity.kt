package com.myclient

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.myclient.databinding.ActivityMainBinding
import com.myclient.entities.Product
import com.myclient.product.OnProductListener
import com.myclient.product.ProductAdapter

class MainActivity : AppCompatActivity(), OnProductListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private lateinit var adapter: ProductAdapter

    private lateinit var firestoreListener: ListenerRegistration

    //esperar el resultado
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            //aca se procesa la respuesta
            val response = IdpResponse.fromResultIntent(it.data)

            if (it.resultCode == RESULT_OK) {
                //usuario autenticado
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                }
            } else {
                //se pulso atras
                if (response == null) {
                    Toast.makeText(this, "Hasta pronto", Toast.LENGTH_SHORT).show()
                    finish()
                } else { //otro tipo de error
                    response.error?.let {
                        if (it.errorCode == ErrorCodes.NO_NETWORK) {
                            Toast.makeText(this, "Sin internet", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Código de error: ${it.errorCode}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configAuth()
        configRecyclerView()

    }

    private fun configAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            //usuario autenticado
            if (auth.currentUser != null) {
                supportActionBar?.title = auth.currentUser?.displayName
                binding.llProgress.visibility = View.GONE
                binding.nsvProducts.visibility = View.VISIBLE
            } else {
                //habilitar todos los proveedores de auth
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build())

                resultLauncher.launch(AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setIsSmartLockEnabled(false) //desactivar la muestra de las opciones de uauario
                    .build())
            }
        }
    }


    //adherir o remover los listener
    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        configFirestoreRealTime()  //se ejecuta unicamente en onResume
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        firestoreListener.remove()  //quitar el listener cada vez que se pause la app
    }

    //config recyclerView
    private fun configRecyclerView(){
        adapter = ProductAdapter(mutableListOf(), this)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3,
                GridLayoutManager.HORIZONTAL, false)
            adapter = this@MainActivity.adapter
        }
    }

    //crear el menu: cerrar sesion
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //config al hacer click en cerrar sesion
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_out -> {
                AuthUI.getInstance().signOut(this)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sesión finalizada", Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            binding.nsvProducts.visibility = View.GONE
                            binding.llProgress.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(this, "No se pudo cerrar la sesión", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //config Firestore en tiempo real
    private fun configFirestoreRealTime() {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTS)

        //listener: captar los cambios
        firestoreListener = productRef.addSnapshotListener { snapshots, error ->
            if (error != null){
                Toast.makeText(this, "Error al consultar datos.", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            //en caso de que no exista error
            for (snapshot in snapshots!!.documentChanges){
                val product = snapshot.document.toObject(Product::class.java)
                product.id = snapshot.document.id
                when(snapshot.type){
                    DocumentChange.Type.ADDED -> adapter.add(product)
                    DocumentChange.Type.MODIFIED -> adapter.update(product)
                    DocumentChange.Type.REMOVED -> adapter.delete(product)
                }
            }
        }
    }

    override fun onClick(product: Product) {

    }

}
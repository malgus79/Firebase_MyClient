package com.myclient.product

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.myclient.Constants
import com.myclient.R
import com.myclient.cart.CartFragment
import com.myclient.databinding.ActivityMainBinding
import com.myclient.detail.DetailFragment
import com.myclient.entities.Product
import com.myclient.order.OrderActivity
import com.myclient.profile.ProfileFragment
import java.security.MessageDigest

class MainActivity : AppCompatActivity(), OnProductListener, MainAux {

    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private lateinit var adapter: ProductAdapter

    private lateinit var firestoreListener: ListenerRegistration

    private var productSelected: Product? = null
    private val productCartList = mutableListOf<Product>()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

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

                    //extraer el token desde preferences
                    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                    val token = preferences.getString(Constants.PROP_TOKEN, null)

                    //si le token no es null
                    token?.let {
                        val db = FirebaseFirestore.getInstance()
                        val tokenMap = hashMapOf(Pair(Constants.PROP_TOKEN, token))

                        //crear una nueva coleccion
                        db.collection(Constants.COLL_USERS)
                                //idi del usuario
                            .document(user.uid)
                                //1 solo usuario puede tener la app en mas de 1 dispositivo -> notificamos a todos los dispositivos
                                //por eso se crea otra colleccion correspondientes a los dispositivos del mismo usuario
                            .collection(Constants.COLL_TOKENS)
                            .add(tokenMap)
                            .addOnSuccessListener {
                                Log.i("registered token", token)
                                //limpiar las preferencias
                                preferences.edit {
                                    putString(Constants.PROP_TOKEN, null)
                                        .apply()
                                }
                            }
                            .addOnFailureListener {
                                Log.i("no registered token", token)
                            }
                    }
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
        configButtons()
        configAnalytics()


/**

        //fcm
        //extraer el token de forma manual y enviarlo al servidor
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful){
                val token = task.result
                val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                preferences.edit {
                    putString(Constants.PROP_TOKEN, token)
                        .apply()
                }
                Log.i("get token", token.toString())
            } else {
                Log.i("get token fail", task.exception.toString())
            }
        }

*/

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
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                    AuthUI.IdpConfig.FacebookBuilder().build(),
                    AuthUI.IdpConfig.PhoneBuilder().build())

                //vista personalizada
                val loginView = AuthMethodPickerLayout
                    .Builder(R.layout.view_login)
                    .setEmailButtonId(R.id.btnEmail)
                    .setGoogleButtonId(R.id.btnGoogle)
                    .setFacebookButtonId(R.id.btnFacebook)
                    .setPhoneButtonId(R.id.btnPhone)
                    .setTosAndPrivacyPolicyId(R.id.tvPolicy)
                    .build()

                resultLauncher.launch(AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setIsSmartLockEnabled(false) //desactivar la muestra de las opciones de uauario
                    .setTosAndPrivacyPolicyUrls("https://www.chess.com/es",
                        "https://www.chess.com/es")
                    .setAuthMethodPickerLayout(loginView)  //vista login personalizada
                    .setTheme(R.style.LoginTheme)  //theme personalizado
                    .build())
            }
        }
/*

        //para facebook login
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = getPackageManager().getPackageInfo(
                    "com.myclient",
                    PackageManager.GET_SIGNING_CERTIFICATES)
                for (signature in info.signingInfo.apkContentsSigners) {
                    val md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    Log.d("API >= 28 KeyHash:",
                        Base64.encodeToString(md.digest(), Base64.DEFAULT));
                }
            } else {
                val info = getPackageManager().getPackageInfo(
                    "com.myclient",
                    PackageManager.GET_SIGNATURES);
                for (signature in info.signatures) {
                    val md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    Log.d("API < 28 KeyHash:",
                        Base64.encodeToString(md.digest(), Base64.DEFAULT));
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
*/
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

    //config btn carrito
    private fun configButtons(){
        binding.btnViewCart.setOnClickListener {
            val fragment = CartFragment()
            fragment.show(supportFragmentManager.beginTransaction(), CartFragment::class.java.simpleName)
        }
    }

    //config analytics
    private fun configAnalytics(){
        firebaseAnalytics = Firebase.analytics
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
            //ver el historial de compras
            R.id.action_order_history -> startActivity(Intent(this, OrderActivity::class.java))

            //menu de editat perfil
            R.id.action_profile -> {
                val fragment = ProfileFragment()
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.containerMain, fragment)
                    .addToBackStack(null)
                    .commit()

//                showButton(false)
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
        //primero validar si existe ese producto en el carrito actual
        val index = productCartList.indexOf(product)
        if (index != -1){  //significa que hay que actualizar -> porque si existe un index
            productSelected = productCartList[index]
        } else {
            productSelected = product  //sino -> agregarlo por primera vez
        }

        //instanciar el detail fragment
        val fragment = DetailFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()

        showButton(false)
        //analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM){
            param(FirebaseAnalytics.Param.ITEM_ID, product.id!!)
            param(FirebaseAnalytics.Param.ITEM_NAME, product.name!!)
        }
    }

    //obtener el carrito desde la main
    override fun getProductsCart(): MutableList<Product> = productCartList

    override fun getProductSelected(): Product? = productSelected

    //btn de ver carrito
    override fun showButton(isVisible: Boolean) {
        binding.btnViewCart.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    //agregar producto al carrito
    override fun addProductToCart(product: Product) {
        //primero validar si existe ese producto en el carrito actual
        val index = productCartList.indexOf(product)
        if (index != -1){  //significa que hay que actualizar -> porque si existe un index
            productCartList.set(index, product)
        } else {
            productCartList.add(product)  //sino -> agregarlo por primera vez
        }
        //depsues de actualizar el listado -> actualizar total
        updateTotal()
    }

    //actualizar el total del carrito
    override fun updateTotal() {
        var total = 0.0
        productCartList.forEach { product ->
            total += product.totalPrice()
        }

        if (total == 0.0){
            binding.tvTotal.text = getString(R.string.product_empty_cart)
        } else {
            binding.tvTotal.text = getString(R.string.product_full_cart, total)
        }
    }

    //limpiar carrito
    override fun clearCart() {
        productCartList.clear()
    }
}
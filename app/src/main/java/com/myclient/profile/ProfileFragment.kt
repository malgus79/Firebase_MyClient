package com.myclient.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.myclient.Constants
import com.myclient.R
import com.myclient.databinding.FragmentProfileBinding
import com.myclient.product.MainAux
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
//import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null

    private var photoSelectedUri: Uri? = null

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            photoSelectedUri = it.data?.data

            binding?.let {
                Glide.with(this)
                    .load(photoSelectedUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_access_time)
                    .error(R.drawable.ic_broken_image)
                    .centerCrop()
                    .circleCrop()
                    .into(it.ibProfile)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding?.let {
            return  it.root
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getUser()
        configButtons()
    }

    //obtener el usuario
    private fun getUser() {
        binding?.let { binding ->
            //obtener el usuario logieado (no null)
            FirebaseAuth.getInstance().currentUser?.let { user ->
                binding.etFullName.setText(user.displayName)
                //binding.etPhotoUrl.setText(user.photoUrl.toString())

                Glide.with(this)
                    .load(user.photoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_access_time)
                    .error(R.drawable.ic_broken_image)
                    .centerCrop()
                    .circleCrop()
                    .into(binding.ibProfile)

                setupActionBar()
            }
        }
    }

    //config title
    private fun setupActionBar(){
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.title = getString(R.string.profile_title)
            setHasOptionsMenu(true)
        }
    }

    private fun configButtons() {
        binding?.let { binding ->
            binding.ibProfile.setOnClickListener {
                openGallery()
            }
            binding.btnUpdate.setOnClickListener {
                binding.etFullName.clearFocus()
                //binding.etPhotoUrl.clearFocus()
                FirebaseAuth.getInstance().currentUser?.let { user ->
                    //si solo se modifico el text
                    if (photoSelectedUri == null) {
                        //updateUserProfile(binding, user, Uri.parse(""))
                        updateUserProfile(binding, user, Uri.parse(""))
                    } else {
                        //caso contrario el usuario eligio una imagen de la geleria
                        uploadReducedImage(user)
                    }
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    //actualizar perfil
    private fun updateUserProfile(binding: FragmentProfileBinding, user: FirebaseUser, uri: Uri) {
//    private fun updateUserProfile(binding: FragmentProfileBinding, user: FirebaseUser, uri: Uri) {

        //para poder editar tanto el nombre como la foto de perfil
        val profileUpdated = UserProfileChangeRequest.Builder()
            .setDisplayName(binding.etFullName.text.toString().trim())
            .setPhotoUri(uri)
            .build()

//        val profileUpdated = UserProfileChangeRequest.Builder()
//            .setDisplayName(binding.etFullName.text.toString().trim())
//            .setPhotoUri(uri)
//            .build()

        user.updateProfile(profileUpdated)
            .addOnSuccessListener {
                Toast.makeText(activity, "Usuario actualizado.", Toast.LENGTH_SHORT).show()
                (activity as? MainAux)?.updateTitle(user)
                activity?.onBackPressed()  //para cerrar el fragmento
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Error al actualizar el usuario.", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    //subir image BITMAP
    private fun uploadReducedImage(user: FirebaseUser){
        //identificar el id del usuario, asi se podra guardar las imagenes por usuario
        val profileRef = FirebaseStorage.getInstance().reference.child(user.uid)
            .child(Constants.PATH_PROFIlE).child(Constants.MY_PHOTO)

        photoSelectedUri?.let { uri ->
            binding?.let { binding ->
                getBitmapFromUri(uri)?.let { bitmap ->
                    binding.progressBar.visibility = View.VISIBLE

                    //para configurar bitmap (formato y calidad)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)

                    profileRef.putBytes(baos.toByteArray())
                        .addOnProgressListener {
                            val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                            it.run {
                                binding.progressBar.progress = progress
                                binding.tvProgress.text = String.format("%s%%", progress)
                            }
                        }
                        .addOnCompleteListener {
                            binding.progressBar.visibility = View.INVISIBLE
                            binding.tvProgress.text = ""
                        }
                        .addOnSuccessListener {
                            it.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                                Log.i("URL", downloadUrl.toString())
                                updateUserProfile(binding, user, downloadUrl)
                            }
                        }
                        .addOnFailureListener{
                            Toast.makeText(activity, "Error al subir imagen.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    //obtener imagen bitmap desde una uri
    private fun getBitmapFromUri(uri: Uri): Bitmap?{
        activity?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(it.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(it.contentResolver, uri)
            }
            return getResizedImage(bitmap, 150)
        }
        return null
    }

    //cambiar dimensiones de las imagenes
    private fun getResizedImage(image: Bitmap, maxSize: Int): Bitmap{
        var width = image.width
        var height = image.height
        //ancho y alto es menor al maximo de la imagen -> no se hace nada
        if (width <= maxSize && height <= maxSize) return image

        //imagen tiene una dimension mas grande que el tamaño maximo (alto o ancho)
        // objetivo: mantener el ratio de ese bitmap
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1){
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height / bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    //accion del retroceso (flecha atras en el titulo)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home){
            //bug: título al volver atras, sin guardar los cambios
            FirebaseAuth.getInstance().currentUser?.let { user ->
                (activity as? MainAux)?.updateTitle(user)
            }
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    //para liberar recursos anulamos a binding
    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }

    //para resetear el titulo
    override fun onDestroy() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            setHasOptionsMenu(false)
        }
        super.onDestroy()
    }
}
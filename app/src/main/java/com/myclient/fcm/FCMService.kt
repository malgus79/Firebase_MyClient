package com.myclient.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.myclient.Constants
import com.myclient.R
import com.myclient.order.OrderActivity
import com.myclient.product.MainActivity

class FCMService : FirebaseMessagingService() {
    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)

        registerNewTokenLocal(newToken)
    }

    //guardar el token en las preferencias de android
    private fun registerNewTokenLocal(newToken: String){
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        preferences.edit {
            putString(Constants.PROP_TOKEN, newToken)
                .apply()
        }

        Log.i("new token", newToken)
    }

    //recibir notificacion en primer plano
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

//        if (remoteMessage.data.isNotEmpty()){
//            sendNotificationByData(remoteMessage.data)
//        }

        //verificar que la notificacion no sea nula
        remoteMessage.notification?.let {
            val imgUrl = it.imageUrl//"https://www.maxpixel.net/static/photo/1x/Marvel-Super-Hero-Flash-4281077.png"
            if (imgUrl == null){
                sendNotification(it)
            } else {
                Glide.with(applicationContext)
                    .asBitmap()
                    .load(imgUrl)
                    //implementacion de la interfaz
                    .into(object : CustomTarget<Bitmap?>(){
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap?>?
                        ) {
                            sendNotification(it, resource)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
    }

    //procesar el mensaje recibido
    private fun sendNotification(notification: RemoteMessage.Notification, bitmap: Bitmap? = null){
        //construir una notifcacion con kotlin
        //val intent = Intent(this, MainActivity::class.java)
        val intent = Intent(this, OrderActivity::class.java)  //definir a que activity se lleva al clickear en la notificacion
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)  //recomendacion de firebase
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            //PendingIntent.FLAG_ONE_SHOT)
            PendingIntent.FLAG_MUTABLE)

        val channelId = getString(R.string.notification_channel_id_default)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)  //eliminar la notificacion luego de darle click
            .setSound(defaultSoundUri)
            .setColor(ContextCompat.getColor(this, R.color.amber_500_dark))
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(notification.body))  //hacer expandible el texto

        bitmap?.let {
            notificationBuilder
                .setLargeIcon(bitmap)
                .setStyle(NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null))
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //canal para notificacion en primer plano
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId,
                getString(R.string.notification_channel_name_default),
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

//    private fun sendNotificationByData(data: Map<String, String>){
//        val intent = Intent(this, OrderActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
//            PendingIntent.FLAG_ONE_SHOT)
//
//        val channelId = getString(R.string.notification_channel_id_default)
//        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//        val notificationBuilder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.ic_stat_name)
//            .setContentTitle(data.get("title"))
//            .setContentText(data.get("body"))
//            .setAutoCancel(true)
//            .setSound(defaultSoundUri)
//            .setColor(ContextCompat.getColor(this, R.color.yellow_a400))
//            .setContentIntent(pendingIntent)
//            .setStyle(NotificationCompat.BigTextStyle()
//                .bigText(data.get("body")))
//
//        val actionIntent = data.get(Constants.ACTION_INTENT)?.toInt()
//        val orderId = data.get(Constants.PROP_ID)
//        val status = data.get(Constants.PROP_STATUS)?.toInt()
//        val trackIntent = Intent(this, OrderActivity::class.java).apply {
//            putExtra(Constants.ACTION_INTENT, actionIntent) //1 = track
//            putExtra(Constants.PROP_ID, orderId)
//            putExtra(Constants.PROP_STATUS, status)
//        }
//        val trackPendingIntent = PendingIntent.getActivity(this, System.currentTimeMillis().toInt(),
//            trackIntent, 0)
//        val action = NotificationCompat.Action.Builder(R.drawable.ic_local_shipping, "Rastrear ahora",
//            trackPendingIntent).build()
//
//        notificationBuilder.addAction(action)
//
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//            val channel = NotificationChannel(channelId,
//                getString(R.string.notification_channel_name_default),
//                NotificationManager.IMPORTANCE_DEFAULT)
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        notificationManager.notify(0, notificationBuilder.build())
//    }
}
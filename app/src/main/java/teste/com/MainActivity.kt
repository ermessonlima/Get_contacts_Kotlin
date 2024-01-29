package teste.com


import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat


class MainActivity : AppCompatActivity() {

    companion object {
        const val PERMISSIONS_REQUEST_READ_CONTACTS = 100
        const val PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 101

    }
    private val CHANNEL_ID = "contacts_channel_id"

    private val NOTIFICATION_ID: Int = 9999


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onStdddddart 1")
        createNotificationChannel()
        loadContacts()


    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSIONS_REQUEST_POST_NOTIFICATIONS)
            }
        }
    }
    private fun showContactCountNotification(count: Int) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val prevIntent = Intent(this, PrevActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val pauseIntent = Intent(this, PauseActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }


        val remoteViews = RemoteViews(packageName, R.layout.notification_layout)
        remoteViews.setImageViewResource(R.id.background_image, R.drawable.teste)

        val notificationLayoutExpanded = RemoteViews(packageName, R.layout.notification_large)
        remoteViews.setTextViewText(R.id.notification_title, "Contatos Atualizados")
        remoteViews.setTextViewText(R.id.notification_content, "Você tem $count contatos.")

        notificationLayoutExpanded.setTextViewText(R.id.notification_title, "Contatos Atualizados")
        notificationLayoutExpanded.setTextViewText(R.id.notification_content, "Você tem $count contatos.")

        val notificationLayout = RemoteViews(packageName, R.layout.notification_buttons_layout)


        notificationLayout.setOnClickPendingIntent(R.id.button_prev, prevIntent)
        notificationLayout.setOnClickPendingIntent(R.id.button_pause, pauseIntent)

        val background = BitmapFactory.decodeResource(resources, R.drawable.notification_background)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pause)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle("Updated contacts")
            .setContentText("You have  $count contacts.")
            .setLargeIcon(background)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1)
            )
            .setCustomContentView(notificationLayout)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }


    private fun loadContacts() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
            Log.d("MainActivity", "onStart 1")
        } else {
            Log.d("MainActivity", "onStart 2")
            setupContactsListView()
        }
    }

    private fun setupContactsListView() {
        val contacts = readContacts()
        val recyclerView = findViewById<RecyclerView>(R.id.contactsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ContactsAdapter(contacts)

        showContactCountNotification(contacts.size)
    }

    private fun readContacts(): List<String> {
        val contacts = mutableListOf<String>()
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, null, null, null, null
        )

        try {
            if (cursor != null) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIndex)
                        if (name.startsWith("S", ignoreCase = true) || name.startsWith("E", ignoreCase = true)) {
                            contacts.add(name)
                        }
                    }
                }
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.d("ContactCount", "Número de contatos filtrados: ${contacts.size}")
        return contacts
    }



    private val contactsObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            setupContactsListView()
        }
    }
    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsObserver)
    }

    override fun onStop() {
        super.onStop()
        contentResolver.unregisterContentObserver(contactsObserver)
    }

}
    class ContactsAdapter(private val contacts: List<String>) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var nameTextView: TextView = itemView as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contact = contacts[position]
            holder.nameTextView.text = contact
        }

        override fun getItemCount() = contacts.size



}
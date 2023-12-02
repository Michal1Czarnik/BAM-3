package com.example.lab3;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("NetworkReceiver", "Received network change intent")
            checkNetworkStatus()
            sendApiRequest()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            sendApiRequest()
        }

        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        val btnReadContacts: Button = findViewById(R.id.btnReadContacts)
        btnReadContacts.setOnClickListener {
            checkContactsPermissionAndRead()
        }
    }

    fun onReadContactsButtonClick(view: android.view.View) {
        checkContactsPermissionAndRead()
    }


    private fun checkContactsPermissionAndRead() {
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
        } else {
            readContacts()
        }
    }

    private fun readContacts() {
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.let {
            while (it.moveToNext()) {
                val contactName =
                    it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                Log.d("Contacts", "Contact: $contactName")
            }
            it.close()
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    }


    private fun sendApiRequest() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPosts().execute()
                val posts = response.body()
                Log.d("API_RESPONSE",  posts.toString())
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("API_EXCEPTION", "Exception: ${e.message}")
            }
        }
    }

    private fun checkNetworkStatus() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        Log.d("NetworkStatus", "Connected to the internet")
    }

    override fun onDestroy() {
        unregisterReceiver(networkReceiver)
        super.onDestroy()
    }
}

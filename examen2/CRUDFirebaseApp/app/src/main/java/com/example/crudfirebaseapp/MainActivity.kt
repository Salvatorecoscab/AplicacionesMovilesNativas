package com.example.crudfirebaseapp

import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.crudfirebaseapp.fragments.AdminFragment
import com.example.crudfirebaseapp.fragments.LoginFragment
import com.example.crudfirebaseapp.fragments.ProfileFragment
import com.example.crudfirebaseapp.fragments.RegisterFragment
import com.example.crudfirebaseapp.utils.SessionManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest // <<--- ESTE IMPORT ES ESENCIAL
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.FirebaseApp

private const val REQUEST_CODE_POST_NOTIFICATIONS = 101

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var sessionManager: SessionManager
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        // Ponlo en cualquier parte que ejecutes una sola vez
        val options = FirebaseApp.getInstance().options
        Log.i("FIREBASE_INFO",
            "ProjectId=${options.projectId}, AppId=${options.applicationId}")
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val idToken = task.result?.token
                Log.d("MY_APP_TOKEN", "Firebase ID Token: $idToken")
                // Ahora busca este log en Logcat. Puedes copiar el token desde allí.
            } else {
                Log.e("MY_APP_TOKEN", "Error obteniendo ID Token", task.exception)
            }
        }
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU es API 33
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
            }
        }

        // Habilitar persistencia offline de Firebase (solo se puede llamar una vez)
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al habilitar persistencia (probablemente ya habilitada): ${e.message}")
        }

        // Inicializar
        bottomNavigation = findViewById(R.id.bottom_navigation)
        sessionManager = SessionManager(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Configurar navegación
        setupBottomNavigation()

        // Verificar estado de autenticación
        val currentUser = auth.currentUser
        if (currentUser != null && sessionManager.isLoggedIn()) {
            Log.d("MainActivity", "Usuario autenticado: ${currentUser.uid}")
            FirebaseUtils.updateFCMTokenForCurrentUser()

            // Primero usar datos de sesión local para UI inmediata
            val isAdmin = sessionManager.isAdmin()
            updateUIBasedOnRole(isAdmin)

            // Cargar fragmento inicial según rol guardado localmente
            if (savedInstanceState == null) {
                if (isAdmin) {
                    loadFragment(AdminFragment())
                } else {
                    loadFragment(ProfileFragment())
                }
            }

            // Luego verificar con Firebase (por si el rol cambió remotamente)
            verifyAdminRole(currentUser.uid)
        } else {
            // No hay sesión, mostrar pantalla de login
            Log.d("MainActivity", "Sin sesión activa, mostrando login")
            if (savedInstanceState == null) {
                loadFragment(LoginFragment())
                updateUIForLoggedOut()
            }
        }

        // Escuchar cambios de autenticación
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null && sessionManager.isLoggedIn()) {
                // Usuario cerró sesión en otro lugar
                Log.d("MainActivity", "Sesión cerrada remotamente")
                sessionManager.logoutUser()
                updateUIForLoggedOut()
                loadFragment(LoginFragment())
                Toast.makeText(this, "Tu sesión ha finalizado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyAdminRole(userId: String) {
        val userRef = database.getReference("users").child(userId)
        userRef.keepSynced(true) // Mantener datos disponibles offline

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val isAdmin = snapshot.child("isAdmin").getValue(Boolean::class.java) ?: false
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""

                    Log.d("MainActivity", "Usuario verificado en DB: isAdmin=$isAdmin")

                    // Actualizar sesión solo si hay cambios
                    if (isAdmin != sessionManager.isAdmin()) {
                        Log.d("MainActivity", "Actualizando rol de usuario: $isAdmin")
                        sessionManager.createLoginSession(userId, email, isAdmin, name)
                        updateUIBasedOnRole(isAdmin)

                        // Si estaba en una sección no permitida, redirigir
                        if (!isAdmin && bottomNavigation.selectedItemId == R.id.navigation_admin) {
                            bottomNavigation.selectedItemId = R.id.navigation_profile
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Error al verificar rol: ${error.message}")
                // Conservar el rol actual en caso de error de red
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_login -> {
                    if (!sessionManager.isLoggedIn()) {
                        loadFragment(LoginFragment())
                        true
                    } else {
                        false
                    }
                }
                R.id.navigation_register -> {
                    if (!sessionManager.isLoggedIn()) {
                        loadFragment(RegisterFragment())
                        true
                    } else {
                        false
                    }
                }
                R.id.navigation_admin -> {
                    if (sessionManager.isLoggedIn() && sessionManager.isAdmin()) {
                        loadFragment(AdminFragment())
                        true
                    } else {
                        Toast.makeText(this, "Acceso denegado. Debes ser administrador para acceder a esta sección.", Toast.LENGTH_SHORT).show()
                        if (sessionManager.isLoggedIn()) {
                            bottomNavigation.selectedItemId = R.id.navigation_profile
                        } else {
                            bottomNavigation.selectedItemId = R.id.navigation_login
                        }
                        false
                    }
                }
                R.id.navigation_profile -> {
                    if (sessionManager.isLoggedIn()) {
                        loadFragment(ProfileFragment())
                        true
                    } else {
                        Toast.makeText(this, "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show()
                        loadFragment(LoginFragment())
                        bottomNavigation.selectedItemId = R.id.navigation_login
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

    private fun updateUIBasedOnRole(isAdmin: Boolean) {
        // Mostrar/ocultar elementos del menú según el rol
        bottomNavigation.menu.findItem(R.id.navigation_admin).isVisible = isAdmin
        bottomNavigation.menu.findItem(R.id.navigation_login).isVisible = false
        bottomNavigation.menu.findItem(R.id.navigation_register).isVisible = false
        bottomNavigation.menu.findItem(R.id.navigation_profile).isVisible = true
    }

    private fun updateUIForLoggedOut() {
        bottomNavigation.menu.findItem(R.id.navigation_login).isVisible = true
        bottomNavigation.menu.findItem(R.id.navigation_register).isVisible = true
        bottomNavigation.menu.findItem(R.id.navigation_admin).isVisible = false
        bottomNavigation.menu.findItem(R.id.navigation_profile).isVisible = false
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


}
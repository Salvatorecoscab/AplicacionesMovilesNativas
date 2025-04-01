package com.example.crudfirebaseapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.crudfirebaseapp.fragments.AdminFragment
import com.example.crudfirebaseapp.fragments.LoginFragment
import com.example.crudfirebaseapp.fragments.ProfileFragment
import com.example.crudfirebaseapp.fragments.RegisterFragment
import com.example.crudfirebaseapp.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var sessionManager: SessionManager
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
}
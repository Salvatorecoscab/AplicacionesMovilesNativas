package com.example.crudfirebaseapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.crudfirebaseapp.R
import com.example.crudfirebaseapp.dialogs.EditProfileDialog
import com.example.crudfirebaseapp.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {
    private lateinit var logoutButton: Button
    private lateinit var editProfileButton: FloatingActionButton
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var userRoleTextView: TextView
    private lateinit var profileImageView: CircleImageView
    private lateinit var sessionManager: SessionManager

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private var profileImageUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Solo inicializar variables que no dependan de la vista
        sessionManager = SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas ahora que view está disponible
        logoutButton = view.findViewById(R.id.button_logout)
        editProfileButton = view.findViewById(R.id.button_edit_profile)
        userNameTextView = view.findViewById(R.id.text_user_name)
        userEmailTextView = view.findViewById(R.id.text_user_email)
        userRoleTextView = view.findViewById(R.id.text_user_role)
        profileImageView = view.findViewById(R.id.profile_image)

        // Mostrar información básica del usuario mientras cargamos los datos completos
        userNameTextView.text = sessionManager.getUserName() ?: "Usuario"
        userEmailTextView.text = sessionManager.getUserEmail() ?: "correo@ejemplo.com"
        userRoleTextView.text = if (sessionManager.isAdmin()) "Administrador" else "Usuario"

        // Cargar datos completos del perfil
        loadUserProfile()

        // Configurar listener para editar perfil
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        // Configurar listener para el botón de cierre de sesión
        logoutButton.setOnClickListener {
            // Cerrar sesión
            sessionManager.logoutUser()

            // Cerrar sesión en Firebase Auth
            auth.signOut()

            // Actualizar UI
            val navView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            navView.menu.findItem(R.id.navigation_login).isVisible = true
            navView.menu.findItem(R.id.navigation_register).isVisible = true
            navView.menu.findItem(R.id.navigation_admin).isVisible = false
            navView.menu.findItem(R.id.navigation_profile).isVisible = false

            // Navegar a login
            navView.selectedItemId = R.id.navigation_login

            Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        val userId = sessionManager.getUserId()

        database.getReference("users").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return // Verificar si el fragmento sigue adjunto

                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""
                        val isAdmin = snapshot.child("isAdmin").getValue(Boolean::class.java) ?: false
                        profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                        userNameTextView.text = name
                        userEmailTextView.text = email
                        userRoleTextView.text = if (isAdmin) "Administrador" else "Usuario"

                        // Actualizar SessionManager con datos actualizados
                        sessionManager.createLoginSession(userId, email, isAdmin, name)

                        // Cargar imagen de perfil
                        if (profileImageUrl.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load(profileImageUrl)
                                .placeholder(R.drawable.default_profile)
                                .into(profileImageView)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (!isAdded) return

                    Toast.makeText(requireContext(), "Error al cargar perfil: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showEditProfileDialog() {
        val dialog = EditProfileDialog(
            userId = sessionManager.getUserId(),
            currentName = userNameTextView.text.toString(),
            currentProfileImageUrl = profileImageUrl
        ) {
            // Este callback se ejecutará cuando el perfil se actualice correctamente
            // No necesitamos hacer nada aquí porque loadUserProfile se llamará automáticamente
            // cuando los datos cambien gracias al ValueEventListener
        }

        dialog.show(childFragmentManager, "EditProfileDialog")
    }
}
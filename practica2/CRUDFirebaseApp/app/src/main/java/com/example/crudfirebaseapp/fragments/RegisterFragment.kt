package com.example.crudfirebaseapp.fragments

import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.Toast
import com.example.crudfirebaseapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.crudfirebaseapp.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var isAdminCheckBox: CheckBox
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas con los IDs CORRECTOS del XML
        nameEditText = view.findViewById(R.id.edit_text_name)
        emailEditText = view.findViewById(R.id.email_edit)         // Corregido
        passwordEditText = view.findViewById(R.id.password_edit)   // Corregido

        // Usar RadioButton en lugar de CheckBox
        val radioAdmin = view.findViewById<RadioButton>(R.id.radio_admin)
        registerButton = view.findViewById(R.id.button_register)

        // Resto del código como lo tenías...
        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = view.findViewById<TextInputEditText>(R.id.confirm_password_edit).text.toString().trim()

            // Verificar campos
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Usar el RadioButton para determinar si es admin
            val isAdmin = radioAdmin.isChecked

            // Deshabilitar botón
            registerButton.isEnabled = false

            // Registrar usuario
            registerUser(name, email, password, isAdmin)
        }
    }

    private fun registerUser(name: String, email: String, password: String, isAdmin: Boolean) {
        // Añadir logging para diagnóstico
        Log.d("RegisterFragment", "Iniciando registro de usuario: $email, isAdmin: $isAdmin")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!isAdded) return@addOnCompleteListener // Verificar si el fragmento sigue adjunto

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        Log.d("RegisterFragment", "Usuario creado en Authentication. UID: ${user.uid}")

                        // Crear documento en Firestore
                        createUserDocumentInFirestore(user.uid, name, email, isAdmin)
                    } else {
                        hideProgressAndEnableButton()
                        Toast.makeText(requireContext(), "Error: Usuario nulo después de registro", Toast.LENGTH_SHORT).show()
                        Log.e("RegisterFragment", "Usuario nulo después de registro exitoso")
                    }
                } else {
                    hideProgressAndEnableButton()
                    val errorMessage = task.exception?.message ?: "Error de registro"
                    Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterFragment", "Error de registro", task.exception)
                }
            }
    }

    private fun createUserDocumentInFirestore(userId: String, name: String, email: String, isAdmin: Boolean) {
        // Cambiar a Realtime Database
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        val userData = HashMap<String, Any>()
        userData["uid"] = userId
        userData["name"] = name
        userData["email"] = email
        userData["isAdmin"] = isAdmin
        userData["profileImageUrl"] = ""
        userData["createdAt"] = ServerValue.TIMESTAMP

        // Añadir timeout
        val timeout = 30000L // 30 segundos
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (isAdded) {
                hideProgressAndEnableButton()
                Toast.makeText(requireContext(), "La operación ha tardado demasiado. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                Log.e("RegisterFragment", "Timeout al crear usuario en Database")
            }
        }

        handler.postDelayed(timeoutRunnable, timeout)

        // Guardar datos en la ruta users/userId
        usersRef.child(userId).setValue(userData)
            .addOnSuccessListener {
                handler.removeCallbacks(timeoutRunnable)

                if (!isAdded) return@addOnSuccessListener

                Log.d("RegisterFragment", "Usuario creado con éxito en Realtime Database")

                // Crear sesión
                val sessionManager = SessionManager(requireContext())
                sessionManager.createLoginSession(userId, email, isAdmin, name)

                // Habilitar botón
                hideProgressAndEnableButton()

                // Navegación
                navigateBasedOnRole(isAdmin)
            }
            .addOnFailureListener { e ->
                handler.removeCallbacks(timeoutRunnable)

                if (!isAdded) return@addOnFailureListener

                hideProgressAndEnableButton()
                Log.e("RegisterFragment", "Error al crear usuario en Realtime Database", e)
                Toast.makeText(requireContext(), "Error al crear perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateBasedOnRole(isAdmin: Boolean) {
        // Actualizar navegación
        val navView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Ocultar/mostrar elementos del menú según el rol
        navView.menu.findItem(R.id.navigation_login).isVisible = false
        navView.menu.findItem(R.id.navigation_register).isVisible = false
        navView.menu.findItem(R.id.navigation_admin).isVisible = isAdmin
        navView.menu.findItem(R.id.navigation_profile).isVisible = true

        // Navegar al fragmento correspondiente
        val destinationId = if (isAdmin) R.id.navigation_admin else R.id.navigation_profile
        navView.selectedItemId = destinationId

        Toast.makeText(
            requireContext(),
            "Registro exitoso como ${if (isAdmin) "administrador" else "usuario"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun hideProgressAndEnableButton() {
        registerButton.isEnabled = true
    }
}
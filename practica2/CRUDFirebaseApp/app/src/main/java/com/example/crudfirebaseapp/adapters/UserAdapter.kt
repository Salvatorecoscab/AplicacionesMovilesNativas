package com.example.crudfirebaseapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.crudfirebaseapp.R
import com.example.crudfirebaseapp.models.User
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter(
    private val users: MutableList<User>,
    private val listener: OnUserClickListener
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    interface OnUserClickListener {
        fun onUserClick(user: User)
        fun onUserEditClick(user: User)
        fun onUserDeleteClick(user: User)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.text_user_name)
        private val emailTextView: TextView = itemView.findViewById(R.id.text_user_email)
        private val adminContainer: LinearLayout = itemView.findViewById(R.id.admin_container)

        private val profileImageView: CircleImageView = itemView.findViewById(R.id.profile_image)  // Añadir esta línea
        private val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)

        fun bind(user: User) {
            nameTextView.text = user.name
            emailTextView.text = user.email
            adminContainer.visibility = if (user.isAdmin) View.VISIBLE else View.GONE

            // Cargar imagen de perfil con Glide
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.default_profile)
                    .into(profileImageView)
            } else {
                // Usar imagen por defecto
                profileImageView.setImageResource(R.drawable.default_profile)
            }

            // Configurar listeners
            itemView.setOnClickListener {
                listener.onUserClick(user)
            }

            editButton.setOnClickListener {
                listener.onUserEditClick(user)
            }

            deleteButton.setOnClickListener {
                listener.onUserDeleteClick(user)
            }
        }
    }
}
package zamora.jorge.taskfam

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.databinding.ActivityJoinHouseBinding

private lateinit var binding: ActivityJoinHouseBinding

class JoinHouse : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = Color.BLACK

        binding = ActivityJoinHouseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener() {
            startActivity(Intent(this, CreateJoinHome::class.java))
        }

        binding.btnAdd.setOnClickListener() {
            joinHouse()
        }
    }

    fun joinHouse() {
        // Obtener datos
        val codigo = binding.inputCode.text.toString().trim()

        // Validar campos vacíos
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Ingrese el código del hogar", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance().reference.child("homes")

        database.orderByChild("code").equalTo(codigo)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (homeSnapshot in snapshot.children) {
                            val homeId = homeSnapshot.key
                            val userId = FirebaseAuth.getInstance().currentUser?.uid

                            if (homeId != null && userId != null) {
                                val membersRef = database.child(homeId).child("members")

                                //Obtiene la lista de miembros que tiene la casa ctualmente
                                membersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(membersSnapshot: DataSnapshot) {
                                        val membersList = mutableListOf<String>()

                                        // Se añaden los miembros que ya esttan agregados en la casa
                                        for (member in membersSnapshot.children) {
                                            member.getValue(String::class.java)?.let {
                                                membersList.add(it)
                                            }
                                        }

                                        // Se agrega al nuevo miembro si no esta en la lista
                                        if (!membersList.contains(userId)) {
                                            membersList.add(userId)
                                            //Modifica la lista de miembros con el nuevo usuario
                                            membersRef.setValue(membersList).addOnCompleteListener { task ->

                                                if (task.isSuccessful) {
                                                    val editable = homeSnapshot.child("editable").getValue(Boolean::class.java) ?: false

                                                    if (editable) {
                                                        val adminsRef = database.child(homeId).child("adminsId")
                                                        adminsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                                            override fun onDataChange(adminsSnapshot: DataSnapshot) {
                                                                val adminsList = mutableListOf<String>()

                                                                for (admin in adminsSnapshot.children) {
                                                                    admin.getValue(String::class.java)?.let {
                                                                        adminsList.add(it)
                                                                    }
                                                                }

                                                                if (!adminsList.contains(userId)) {
                                                                    adminsList.add(userId)
                                                                    adminsRef.setValue(adminsList)
                                                                }
                                                            }

                                                            override fun onCancelled(error: DatabaseError) {
                                                                Toast.makeText(
                                                                    this@JoinHouse,
                                                                    "Error al obtener miembros: ${error.message}",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        })
                                                    }

                                                    Toast.makeText(
                                                        this@JoinHouse,
                                                        "Te has unido al hogar correctamente",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    cargarHogar(homeId)
                                                } else {
                                                    Toast.makeText(
                                                        this@JoinHouse,
                                                        "Error al unirse al hogar",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(
                                                this@JoinHouse,
                                                "Ya eres miembro de este hogar",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(
                                            this@JoinHouse,
                                            "Error al obtener miembros: ${error.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                            }
                            return
                        }
                    } else {
                        Toast.makeText(
                            this@JoinHouse,
                            "Código de hogar no encontrado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@JoinHouse,
                        "Error en la consulta: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

    }

    fun cargarHogar(homeId: String){
        val database = FirebaseDatabase.getInstance().reference

        database.child("homes").child(homeId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val home = snapshot.getValue(Home::class.java)
                if (home != null) {
                    Toast.makeText(
                        this@JoinHouse,
                        "Te has unido al hogar correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@JoinHouse, MainActivity::class.java)
                    intent.putExtra("HOME", home)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@JoinHouse, "No se pudo cargar la información del hogar", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@JoinHouse, "El hogar no existe", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this@JoinHouse, "Error al obtener el hogar", Toast.LENGTH_SHORT).show()
        }
    }


}
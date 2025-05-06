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

        // Listener para el boton de regreso
        binding.btnBack.setOnClickListener() {
            startActivity(Intent(this, CreateJoinHome::class.java))
        }

        // Listener para la logica de ingresar a una casa
        binding.btnAdd.setOnClickListener() {
            joinHouse()
        }
    }

    /**
     * Intenta unir al usuario actual a un hogar existente utilizando el código ingresado.
     * Valida que el código no esté vacío, busca el hogar en la base de datos,
     * y si lo encuentra, agrega al usuario como miembro.
     */
    fun joinHouse() {
        // Obtener datos
        val codigo = binding.inputCode.text.toString().trim()

        // Validar campos vacíos
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Ingrese el código del hogar", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener una referencia al nodo "homes" en la base de datos de Firebase
        val database = FirebaseDatabase.getInstance().reference.child("homes")

        // Buscar un hogar que tenga el código ingresado
        database.orderByChild("code").equalTo(codigo)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                /**
                 * Se llama una vez con el valor en la ubicación inicial y nuevamente cada vez que los datos en esa ubicación cambian.
                 * Este método se utiliza para procesar los resultados de la búsqueda del hogar por código.
                 * @param snapshot Contiene los datos de los hogares que coinciden con el código.
                 */
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Se verefica si existe la snapshot
                    if (snapshot.exists()) {
                        for (homeSnapshot in snapshot.children) {
                            // Obtenemos el id del hogar y el usuario
                            val homeId = homeSnapshot.key
                            val userId = FirebaseAuth.getInstance().currentUser?.uid

                            // Verificamos que no sean nullos
                            if (homeId != null && userId != null) {
                                // Obtenemos la referencia de miembros del hogar encontrado
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

                                                // Verificamos si la operacion fue exitosa
                                                if (task.isSuccessful) {
                                                    // Ontenemos el estado del editable del hogar
                                                    val editable = homeSnapshot.child("editable").getValue(Boolean::class.java) ?: false

                                                    // Si lo es, agregamos al usuario a admins
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

                /**
                 * Se llama si la operación de lectura de datos es cancelada.
                 * @param error Contiene información sobre el error ocurrido.
                 */
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@JoinHouse,
                        "Error en la consulta: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

    }

    /**
     * Carga la información completa del hogar desde la base de datos utilizando su ID
     * y navega a la actividad MainActivity, pasando la información del hogar como un extra.
     * @param homeId El ID único del hogar a cargar.
     */
    fun cargarHogar(homeId: String){
        // Creamos una referencia de la bd
        val database = FirebaseDatabase.getInstance().reference

        // Si se selecciona un hogar cargamos toda su informacion a MainActivity
        database.child("homes").child(homeId).get().addOnSuccessListener { snapshot ->
            // Verificamos que exista
            if (snapshot.exists()) {
                // Obtenemos el hogar de la snapshot
                val home = snapshot.getValue(Home::class.java)
                if (home != null) {
                    // Notificamos al usuario
                    Toast.makeText(
                        this@JoinHouse,
                        "Te has unido al hogar correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Iniciamos la actividad siguiente
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
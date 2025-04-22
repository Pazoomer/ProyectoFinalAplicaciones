package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.data.Member
import zamora.jorge.taskfam.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var home: Home? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.BLACK

        home = intent.getParcelableExtra<Home>("HOME")

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBackArrow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("HOME", home)
            startActivity(intent)
        }

        binding.tvBorrarHogar.setOnClickListener {
            borrarCasa()
        }

        auth = FirebaseAuth.getInstance()

        database = FirebaseDatabase.getInstance().reference

        getMembers()

        binding.tvSubtitulo.text = "Codigo del hogar: ${home!!.code}"

        home?.let {
            binding.tvSubtitulo.text = "Código del hogar: ${it.code}"
            binding.etNombreHogar.setText(it.nombre)

            // Seleccionar radio según el valor de editable
            if (it.editable == true) {
                binding.rbEdit.isChecked = true
            } else {
                binding.rbNoEdit.isChecked = true
            }

        } ?: run {
            binding.tvSubtitulo.text = "Error: No se encontró el hogar"
        }

        binding.etNombreHogar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No es necesario implementar este método
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No es necesario implementar este método
            }

            override fun afterTextChanged(s: Editable?) {
                guardarNombre(s.toString())
            }
        })

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbEdit -> {
                    home?.editable = true
                    guardarEditable(true)
                }
                R.id.rbNoEdit -> {
                    home?.editable = false
                    guardarEditable(false)
                }
            }
        }
    }

    fun getMembers() {

        if(home==null){
            return
        }else if(home?.members==null){
            return
        }

        val membersIds = home?.members ?: emptyList()

        if (membersIds.isEmpty()) {
            Log.d("Firebase", "No hay miembros en este hogar.")
            return
        }

        val membersHome = mutableListOf<Member>()

        for (memberId in membersIds) {
            database.child("members").child(memberId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val member = snapshot.getValue(Member::class.java)
                        member?.let { membersHome.add(it) }
                    }

                    if (membersHome.size == membersIds.size) {
                        val listView = findViewById<ListView>(R.id.listViewMiembros)
                        val adapter = MiembroAdapter(this@Settings, membersHome, home!!, database, auth)
                        listView.adapter = adapter
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error al obtener miembro: ${error.message}")
                }
            })
        }
    }

    fun guardarNombre(texto: String) {
        //Verificar que no este vacio
        if (texto.isEmpty()) {
            showError("Por favor ingrese un nombre")
            return
        }

        //Usuario autentificado
        val userId = auth.currentUser?.uid ?: run {
            showError("Error: Usuario no autenticado")
            return
        }

        //Veririfcar que el hogar existe
        val homeId = home?.id ?: run {
            showError("Error: No se encontró el ID del hogar")
            return
        }

        //Cambiarle el nombre al hogar
        database.child("homes").child(homeId).child("nombre").setValue(texto)
            .addOnSuccessListener {
                Toast.makeText(this, "Nombre actualizado exitosamente", Toast.LENGTH_SHORT).show()
                home!!.nombre = texto
            }
            .addOnFailureListener { e ->
                showError("Error al actualizar el nombre: ${e.message}")
            }
    }

    fun guardarEditable(editable: Boolean) {
        val userId = auth.currentUser?.uid ?: run {
            showError("Error: Usuario no autenticado")
            return
        }

        val homeId = home?.id ?: run {
            showError("Error: No se encontró el ID del hogar")
            return
        }

        database.child("homes").child(homeId).child("editable").setValue(editable)
            .addOnSuccessListener {
                Toast.makeText(this, "Permiso de edición actualizado", Toast.LENGTH_SHORT).show()
                home!!.editable = editable
            }
            .addOnFailureListener { e ->
                showError("Error al actualizar editable: ${e.message}")
            }
    }

    private fun borrarCasa(){
        //Usuario autentificado
        val userId = auth.currentUser?.uid ?: run {
            showError("Error: Usuario no autenticado")
            return
        }

        //Veririfcar que el hogar existe
        val homeId = home?.id ?: run {
            showError("Error: No se encontró el ID del hogar")
            return
        }

        // Eliminar las tareas con ese homeId
        database.child("tasks").orderByChild("homeId").equalTo(homeId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (taskSnapshot in snapshot.children) {
                        taskSnapshot.ref.removeValue()
                    }

                    // Después de borrar las tasks, borrar la casa
                    database.child("homes").child(homeId).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this@Settings, "Hogar y tareas eliminados", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@Settings, CreateJoinHome::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            showError("Error al eliminar el hogar: ${e.message}")
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Error al borrar las tareas: ${error.message}")
                }
            })
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        println("Error: $message")
    }

    class MiembroAdapter(private val context: Context,
                         private var data: MutableList<Member>,
                         private val home: Home,
                         private val database: DatabaseReference,
                         private val auth: FirebaseAuth) : BaseAdapter() {

        override fun getCount(): Int = data.size

        override fun getItem(position: Int): Any = data[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.settings_member, parent, false)

            val tvNombre = view.findViewById<TextView>(R.id.tvNombre)
            val btnEliminar = view.findViewById<ImageView>(R.id.btnEliminar)
            val miembro = data[position]

            tvNombre.text = miembro.name

            if(miembro.id==home.adminId){
                btnEliminar.visibility = View.GONE
            }

            btnEliminar.setOnClickListener {
                eliminarMiembroCompletoAtomic(miembro.id, home.id) { exito ->
                    if (exito) {
                        data.removeAt(position)
                        notifyDataSetChanged()
                        Toast.makeText(context, "Miembro y tareas eliminadas", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return view
        }

        fun eliminarMiembroCompletoAtomic(
            miembroId: String,
            homeId: String,
            onResult: (Boolean) -> Unit
        ) {
            val homeRef = database.child("homes").child(homeId)
            val tasksRef = database.child("tasks")
            val memberTasksRef = database.child("members").child(miembroId).child("tasks")

            // 1. Leer datos del hogar (adminId, lista de tareas, miembros y admins)
            homeRef.get().addOnSuccessListener { homeSnap ->
                val adminId = homeSnap.child("adminId").getValue(String::class.java)
                if (adminId == miembroId) {
                    onResult(false) // no puede eliminar al admin
                    return@addOnSuccessListener
                }
                val homeTasks = homeSnap.child("tasks").children.mapNotNull { it.getValue(String::class.java) }
                val homeMembers = homeSnap.child("members").children.mapNotNull { it.getValue(String::class.java) }
                val homeAdmins = homeSnap.child("adminsId").children.mapNotNull { it.getValue(String::class.java) }

                // 2. Leer las tareas del miembro
                memberTasksRef.get().addOnSuccessListener { memberTasksSnap ->
                    val memberTasks = memberTasksSnap.children.mapNotNull { it.getValue(String::class.java) }

                    // Intersección de tareas hogar ∧ tareas miembro
                    val commonTasks = homeTasks.intersect(memberTasks)

                    // 3. Leer asignaciones de todas las tareas
                    tasksRef.get().addOnSuccessListener { tasksSnap ->
                        val updates = mutableMapOf<String, Any?>()
                        val tasksToDelete = mutableListOf<String>()

                        commonTasks.forEach { taskId ->
                            val assignmentSnap = tasksSnap.child(taskId).child("assignments")
                            val assignedMembers = assignmentSnap.children.mapNotNull { it.key }

                            if (assignedMembers.size == 1 && assignedMembers.first() == miembroId) {
                                // Solo él: borrar tarea
                                tasksToDelete.add(taskId)
                                updates["tasks/$taskId"] = null
                            } else {
                                // Quitar solo su asignación
                                updates["tasks/$taskId/assignments/$miembroId"] = null
                            }
                            // Siempre quitar de member->tasks
                            updates["members/$miembroId/tasks/$taskId"] = null
                        }

                        // 4. Actualizar lista de tareas del hogar sin las borradas
                        val updatedHomeTasks = homeTasks.filterNot { tasksToDelete.contains(it) }
                        updates["homes/$homeId/tasks"] = updatedHomeTasks

                        // 5. Quitar miembro de members y adminsId del hogar
                        val updatedHomeMembers = homeMembers.filterNot { it == miembroId }
                        updates["homes/$homeId/members"] = updatedHomeMembers
                        val updatedHomeAdmins = homeAdmins.filterNot { it == miembroId }
                        updates["homes/$homeId/adminsId"] = updatedHomeAdmins

                        // 6. Ejecutar todo atómicamente
                        database.updateChildren(updates)
                            .addOnSuccessListener { onResult(true) }
                            .addOnFailureListener { onResult(false) }
                    }.addOnFailureListener { onResult(false) }
                }.addOnFailureListener { onResult(false) }
            }.addOnFailureListener { onResult(false) }
        }
    }
}

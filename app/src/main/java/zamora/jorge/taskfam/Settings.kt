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

        // Obtiene el objeto Home que fue pasado como extra desde la actividad anterior (MainActivity)
        home = intent.getParcelableExtra<Home>("HOME")

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Listener en la flecha volver atras, que nos regres a MainActivity, con la cas actual
        binding.ivBackArrow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("HOME", home)
            startActivity(intent)
        }

        // Listener el cual llama al metodo borrarCasa
        binding.tvBorrarHogar.setOnClickListener {
            borrarCasa()
        }

        // Obtenemos la instancia de autenticacion de Firebase
        auth = FirebaseAuth.getInstance()

        //Obtenemos la referencia de la base de datos
        database = FirebaseDatabase.getInstance().reference

        // Cargamos la lista de los miembros del hogar
        getMembers()

        // Mostramos el codigo del hogar
        binding.tvSubtitulo.text = "Codigo del hogar: ${home!!.code}"

        // Si el objeto Home no es nulo, muestra el nombre del hogar y configura el RadioButton
        // según si la edición de tareas está habilitada o no
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
            // Si el objeto Home es nulo, muestra un mensaje de error
            binding.tvSubtitulo.text = "Error: No se encontró el hogar"
        }

        // Listener para el EditText que permite cambiar el nombre del hogar
        binding.etNombreHogar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No es necesario implementar este método
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No es necesario implementar este método
            }

            override fun afterTextChanged(s: Editable?) {
                // Guarda el nuevo nombre del hogar en la base de datos.
                guardarNombre(s.toString())
            }
        })

        // Listener para el RadioGroup que permite habilitar o deshabilitar la edición de tareas.
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

    /**
     * Obtiene la lista de miembros del hogar actual desde la base de datos de Firebase.
     * Los IDs de los miembros se obtienen de la lista 'members' dentro del nodo del hogar.
     * Luego, se consulta la información detallada de cada miembro utilizando su ID.
     * Finalmente, se muestra la lista de miembros en un ListView utilizando un adaptador personalizado.
     */
    fun getMembers() {

        // Verificamos si el hogar no es nullo
        if(home==null){
            return
        }else if(home?.members==null){
            return
        }

        // Obtenemos la lista de ids de los miembros del hogar
        val membersIds = home?.members ?: emptyList()

        // Mensaje si no hay miembros
        if (membersIds.isEmpty()) {
            Log.d("Firebase", "No hay miembros en este hogar.")
            return
        }

        // Lista mutable para almacenar los miembros del hogar
        val membersHome = mutableListOf<Member>()

        // Itera sobre la lista de IDs de los miembros para obtener su información
        for (memberId in membersIds) {
            database.child("members").child(memberId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Si el snapshot existe, convierte los datos a un objeto Member
                    if (snapshot.exists()) {
                        val member = snapshot.getValue(Member::class.java)
                        member?.let { membersHome.add(it) }
                    }

                    // Si se han obtenido todos los miembros, crea y asigna el adaptador al ListView.
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

    /**
     * Guarda el nuevo nombre del hogar en la base de datos de Firebase.
     * Realiza validaciones para asegurar que el nombre no esté vacío y que el usuario esté autenticado.
     * @param texto El nuevo nombre del hogar a guardar.
     */
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
                home!!.nombre = texto //Actualizamos localmente tambien
            }
            .addOnFailureListener { e ->
                showError("Error al actualizar el nombre: ${e.message}")
            }
    }

    /**
     * Guarda el estado de edición del hogar (habilitado o deshabilitado) en la base de datos de Firebase.
     * Realiza validaciones para asegurar que el usuario esté autenticado y que el objeto Home no sea nulo.
     * @param editable Booleano que indica si la edición de tareas está habilitada (true) o no (false).
     */
    fun guardarEditable(editable: Boolean) {
        // Obtenemos el ID del usuario autenticado
        val userId = auth.currentUser?.uid ?: run {
            showError("Error: Usuario no autenticado")
            return
        }

        // Verificamo que el objeto Home y su ID no sean nulos
        val homeId = home?.id ?: run {
            showError("Error: No se encontró el ID del hogar")
            return
        }

        // Actualizar el estado de edición del hogar en la base de datos
        database.child("homes").child(homeId).child("editable").setValue(editable)
            .addOnSuccessListener {
                Toast.makeText(this, "Permiso de edición actualizado", Toast.LENGTH_SHORT).show()
                home!!.editable = editable //Actualizamos localmente tambien
            }
            .addOnFailureListener { e ->
                showError("Error al actualizar editable: ${e.message}")
            }
    }

    /**
     * Borra el hogar actual y todas sus tareas asociadas de la base de datos de Firebase.
     * Solo el administrador del hogar debería poder realizar esta acción.
     * Realiza validaciones para asegurar que el usuario esté autenticado y que el objeto Home no sea nulo.
     */
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
                    // Itera sobre todas las tareas encontradas con el homeId actual y las elimina
                    for (taskSnapshot in snapshot.children) {
                        taskSnapshot.ref.removeValue()
                    }

                    // Después de borrar las tasks, borrar la casa
                    database.child("homes").child(homeId).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this@Settings, "Hogar y tareas eliminados", Toast.LENGTH_SHORT).show()
                            // Redirige al usuario a la actividad de creación/unión de hogares
                            startActivity(Intent(this@Settings, CreateJoinHome::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            showError("Error al eliminar el hogar: ${e.message}")
                        }
                }

                // En caso de un error al borrar
                override fun onCancelled(error: DatabaseError) {
                    showError("Error al borrar las tareas: ${error.message}")
                }
            })
    }

    /**
     * Muestra un Toast con el mensaje de error proporcionado y loguea el error en la consola.
     * @param message El mensaje de error a mostrar.
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        println("Error: $message")
    }

    /**
     * Adaptador personalizado para mostrar la lista de miembros del hogar en un ListView.
     * Permite eliminar miembros del hogar (excepto al administrador).
     * @param context El contexto de la actividad.
     * @param data La lista mutable de objetos [Member] a mostrar.
     * @param home El objeto [Home] actual.
     * @param database Referencia a la base de datos de Firebase.
     * @param auth Instancia del servicio de autenticación de Firebase.
     */
    class MiembroAdapter(private val context: Context,
                         private var data: MutableList<Member>,
                         private val home: Home,
                         private val database: DatabaseReference,
                         private val auth: FirebaseAuth) : BaseAdapter() {

        override fun getCount(): Int = data.size

        override fun getItem(position: Int): Any = data[position]

        override fun getItemId(position: Int): Long = position.toLong()

        /**
         * Proporciona una vista para cada elemento en el ListView de miembros.
         * @param position La posición del miembro en la lista.
         * @param convertView La vista antigua para reutilizar, si está disponible.
         * @param parent El ViewGroup al que se adjuntará la vista.
         * @return La vista que muestra el nombre del miembro y el botón de eliminar.
         */
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.settings_member, parent, false)

            // Obtiene las referencias a las vistas dentro del layout del elemento del miembro
            val tvNombre = view.findViewById<TextView>(R.id.tvNombre)
            val btnEliminar = view.findViewById<ImageView>(R.id.btnEliminar)
            // Miembro actual
            val miembro = data[position]

            // Muestra el nombre del miembro en el TextView
            tvNombre.text = miembro.name

            // Oculta el botón de eliminar si el miembro actual es el administrador del hogar
            if(miembro.id==home.adminId){
                btnEliminar.visibility = View.GONE
            }

            // Listener para el botón de eliminar miembro.
            btnEliminar.setOnClickListener {
                // Llama a la función para eliminar el miembro de forma atómica (incluyendo sus tareas asignadas)
                eliminarMiembroCompletoAtomic(miembro.id, home.id) { exito ->
                    if (exito) {
                        // Si la eliminación fue exitosa, remueve el miembro de la lista local y notifica al adaptador
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

        /**
         * Elimina un miembro del hogar de forma atómica, incluyendo la eliminación de las tareas
         * que solo estaban asignadas a ese miembro y la actualización de las tareas compartidas.
         * @param miembroId El ID del miembro a eliminar.
         * @param homeId El ID del hogar al que pertenece el miembro.
         * @param onResult Función lambda que recibe un booleano indicando si la operación fue exitosa.
         */
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

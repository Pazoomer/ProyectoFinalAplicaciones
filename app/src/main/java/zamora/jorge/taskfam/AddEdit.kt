package zamora.jorge.taskfam

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.data.Member
import zamora.jorge.taskfam.data.MembersDay
import zamora.jorge.taskfam.data.Task
import zamora.jorge.taskfam.databinding.ActivityAddEditBinding

class AddEdit : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private var miembrosDisponibles = mutableListOf<Member>()
    private var miembrosAsignados = mutableListOf<MembersDay>()
    private lateinit var adapter: MiembroAdapter
    private var accion: String? = ""
    private var home: Home? = null
    private var tarea: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK

        // Obtenemos datos del intent
        val bundle = intent.extras

        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificamos si se recibieron extras
        if (bundle != null) {
            // Recuperamos los extras
            accion = bundle.getString("Accion")
            home = bundle.get("Home") as Home?
            tarea = bundle.get("TASK") as Task?

            if (tarea == null) {
                // Obtiene lista de miembros actual y actualiza el adapter
                obtenerMiembrosDeHome { listaMiembros ->
                    miembrosDisponibles = listaMiembros.toMutableList()
                    actualizarAdapter()
                    //Actualiza el estado del botón después de cargar a miembros
                    actualizarEstadoBotonAgregarHabitante()
                }
            }else {
                obtenerMiembrosDisponibles(tarea!!) { listaMiembrosDisponibles ->
                    miembrosDisponibles = listaMiembrosDisponibles.toMutableList()
                    actualizarAdapter()
                    // Actualiza el estado del botón basándose en la lista de disponibles
                    actualizarEstadoBotonAgregarHabitante()}
            }


            // Mostramos los textos adecuados a la accion a realizar
            binding.tvAgregarEditar.text = accion
            binding.btnAgregarEditar.text = accion

            if (accion == "EDITAR") {
                binding.etNombreTarea.setText(tarea?.titulo)
                binding.etDescripcion.setText(tarea?.descripcion)

                binding.btnAgregarEditar.text = "Guardar"


                // Obtiene las asignaciones de la tarea (miembros y días asignados)
                val assignments = tarea?.assignments ?: emptyMap()

                // Obtiene la información de los miembros asignados a la tarea
                obtenerMiembrosPorIds(assignments.keys.toList()) { miembros ->
                    val miembrosDeTarea = mutableListOf<MembersDay>()

                    for (miembro in miembros) {
                        // Recuperamos los dias seleccionados por miembro y creamos un mapa en el cual se marcaran como seleccionados
                        val diasAsignados = assignments[miembro.id] ?: emptyMap()
                        val diasSeleccionados =
                            diasAsignados.keys.associateWith { true }.toMutableMap()
                        miembrosDeTarea.add(MembersDay(miembro, diasSeleccionados))
                    }

                    // Limpia la lista de miembros asignados y agrega los miembros de la tarea
                    miembrosAsignados.clear()
                    miembrosAsignados.addAll(miembrosDeTarea)

                    // Remueve los miembros de la tarea de la lista de miembros disponibles
                    miembrosDisponibles.removeAll(miembros.map { it })

                    // Mostramos miembros y dias seleccionados en el adapter
                    actualizarAdapter()
                }


            }
        }

        binding.btnAgregarHabitante.setOnClickListener {
            // Usamos el metodo para agregar habitante a la tarea
            agregarHabitante()
        }

        binding.btnAgregarEditar.setOnClickListener {
            // Verificamos si la accion recuperada en extras es para agregar tarea
            if (accion.equals("AGREGAR")) {
                // Usamos el metodo para agregar tarea
                agregarTarea()
            } else if (accion.equals("EDITAR")) {
                // Usamos el metodo para actualizar tarea
                actualizarTarea()
            }
        }

        binding.tvEliminar.setOnClickListener {
            // Usamos el metodo para eliminar tarea
            eliminarTarea()
        }

        binding.ivBackArrow.setOnClickListener {
            // Creamos un intent a MainActivity y enviamos el hogar actual
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("HOME", home)
            startActivity(intent)
        }
    }

    /**
     * Elimina la tarea actual de la base de datos de Firebase.
     * Después de eliminar la tarea, regresa a la actividad principal.
     */
    private fun eliminarTarea(){
        // Obtenemos una referencia de la bd eb firebase
        val database = FirebaseDatabase.getInstance().reference
        // Elimina la tarea de la bd usando el id de la misma
        database.child("tasks").child(tarea?.id ?: "").removeValue()

        // Creamos un intent a MainActivity y enviamos el hogar actual
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("HOME", home)
        startActivity(intent)
        // Cerramos la actividad actual
        finish()
    }


    /**
     * Obtiene la lista de miembros pertenecientes al hogar actual desde la base de datos de Firebase.
     * @param callback Función lambda que se llamará con la lista de [Member] del hogar.
     */
    private fun obtenerMiembrosDeHome(callback: (List<Member>) -> Unit) {
        // Obtenemos una referencia de la bd eb firebase
        val database = FirebaseDatabase.getInstance().reference
        // Obtenemos una referencia de la casa acutal
        val homeId = home?.id

        // Si el ID es null o vacio, arrojamos un error
        if (homeId.isNullOrEmpty()) {
            Log.e("Firebase", "Error: homeId es nulo o vacío")
            callback(emptyList())
            return
        }

        // Accedemos a los miembros de la casa en especifico
        database.child("homes").child(homeId).child("members")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                /**
                 * Se llama cuando se obtienen los datos de los miembros del hogar.
                 * @param snapshot Contiene los IDs de los usuarios que son miembros del hogar.
                 */
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Mapea los ID de los usuarios de la casa a Strings
                    val userIds = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                    // Obtenemos los miembros por sus ids.
                    obtenerMiembrosPorIds(userIds, callback)
                }

                /**
                 * Se llama si la operación de lectura de datos es cancelada.
                 * @param error Contiene información sobre el error ocurrido.
                 */
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error al obtener IDs de miembros: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    private fun obtenerMiembrosDisponibles(task: Task, callback: (List<Member>) -> Unit) {
        // Obtiene los IDs de los miembros que ya están asignados a la tarea actual

        val assignedMemberIds = task.assignments.keys.toSet() // Convertimos a Set para una búsqueda eficiente

        // Obtiene la lista completa de TODOS los miembros del hogar de esta tarea.
        // Reutilizamos tu función existente obtenerMiembrosDeHome.
        // Asumimos que la variable 'home' en esta actividad está correctamente establecida al hogar de la tarea.
        obtenerMiembrosDeHome { allHomeMembers ->
            // Una vez que tenemos todos los miembros del hogar, filtramos aquellos
            // cuyo ID NO está en el conjunto de IDs de miembros asignados.
            val availableMembers = allHomeMembers.filter { member ->
                !assignedMemberIds.contains(member.id)
            }

            // Devolvemos la lista filtrada (los miembros disponibles) a través del callback
            callback(availableMembers)
        }
    }

    /**
     * Obtiene la información detallada de una lista de miembros utilizando sus IDs desde la base de datos de Firebase.
     * @param userIds Lista de IDs de los miembros a obtener.
     * @param callback Función lambda que se llamará con la lista de objetos [Member] obtenidos.
     */
    private fun obtenerMiembrosPorIds(userIds: List<String>, callback: (List<Member>) -> Unit) {
        // Obtenemos una referencia de la bd eb firebase
        val database = FirebaseDatabase.getInstance().reference
        // Lista mutable de miemrbos obtenidos
        val miembros = mutableListOf<Member>()
        // Contador para verificar que se hayan obtenido todos los miembros
        var pendientes = userIds.size

        // Si esta vacia enviamos una lista vacia
        if (userIds.isEmpty()) {
            callback(emptyList())
            return
        }

        // Iteramos por las ids de los usuarios
        for (userId in userIds) {
            // Accedemos a miembros
            database.child("members").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    /**
                     * Se llama cuando se obtienen los datos de un miembro.
                     * @param userSnapshot Contiene la información del miembro.
                     */
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        // Obtenemos el nombre e email del miembro
                        val name = userSnapshot.child("name").getValue(String::class.java)
                        val email = userSnapshot.child("email").getValue(String::class.java)

                        // Construimos un objeto con la informacion
                        val miembro = Member(
                            id = userId,
                            name = name ?: "Sin nombre",
                            email = email ?: "Sin correo"
                        )
                        // Lo agregamos a la lista
                        miembros.add(miembro)
                        // Restamos los pendientes
                        pendientes--

                        if (pendientes == 0) {
                            // Cuando se recorra toda la lista se devuelve la lista de miembros con el callback
                            callback(miembros)
                        }
                    }

                    /**
                     * Se llama si la operación de lectura de datos es cancelada para un miembro específico.
                     * @param error Contiene información sobre el error ocurrido.
                     */
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Error al obtener miembro $userId: ${error.message}")
                        pendientes--
                        if (pendientes == 0) {
                            callback(miembros)
                        }
                    }
                })
        }
    }


    /**
     * Agrega el primer miembro disponible a la lista de miembros asignados a la tarea.
     * Si no hay miembros disponibles, muestra un Toast indicando la situación.
     * Después de agregar un miembro, actualiza el adaptador de la lista.
     */
    private fun agregarHabitante() {
        // Verificamos si hay miembros disponibles
        if (miembrosDisponibles.isNotEmpty()) {
            // Obtiene el primer miembro de la lista de disponibles
            var primerDisponible = miembrosDisponibles.first()
            if (miembrosAsignados.any { it.member.id == primerDisponible.id }) {
                miembrosDisponibles.remove(primerDisponible)
                primerDisponible = miembrosDisponibles.first()
            }else {
                miembrosDisponibles.remove(primerDisponible)
            }

            // Agrega el miembro a la lista de asignados
            miembrosAsignados.add(MembersDay(primerDisponible))
            // Actualiza el adaptador para reflejar el cambio
            actualizarAdapter()
            //Actualiza el estado del botón después de cargar a miembros
            actualizarEstadoBotonAgregarHabitante()
        } else {
            // Muestra mensaje de error si no hay miembros a agregar
            Toast.makeText(this, "No hay más miembros disponibles", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Crea o actualiza el adaptador para la lista de miembros asignados.
     * El adaptador se encarga de mostrar cada miembro y permitir la selección de los días asignados.
     */
    private fun actualizarAdapter() {
        // Crea una nueva instancia del adaptador con la lista actual de miembros asignados y disponibles,
        // y una lambda que se llama cuando la lista de miembros asignados cambia
        adapter =
            MiembroAdapter(this, miembrosAsignados, miembrosDisponibles) {
                actualizarAdapter()
                actualizarEstadoBotonAgregarHabitante() }
        // Asigna el adapter al ListView de miembros
        binding.lvMiembros.adapter = adapter
        actualizarEstadoBotonAgregarHabitante()
    }

    /**
     * Agrega una nueva tarea a la base de datos de Firebase.
     * Recopila el nombre, la descripción y los miembros asignados con sus respectivos días.
     * Valida que se hayan ingresado todos los datos y que al menos un miembro esté asignado con algún día.
     * Después de agregar la tarea, regresa a la actividad principal.
     */
    private fun agregarTarea() {
        // Obtenemos el nombre y descripcion de los EditText
        val nombre = binding.etNombreTarea.text.toString()
        val descripcion = binding.etDescripcion.text.toString()

        // Verificamos que no esten vacios
        if (nombre.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese todos los datos", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificamos que haya miembros en la tarea
        if (miembrosAsignados.isEmpty()) {
            Toast.makeText(this, "Por favor agregue al menos un miembro", Toast.LENGTH_SHORT).show()
            return
        }

        // Creamos una lista mutable de tareas
        val asignaciones = mutableMapOf<String, Map<String, Boolean>>()
        // Genera una ID para la tarea en Firebase
        val taskId = FirebaseDatabase.getInstance().reference.child("tasks").push().key ?: return
        // Obtiene el ID del hogar actual
        val homeId = home?.id ?: ""

        var diasTareas = false

        // Itera sobre cada miembro asignado para recopilar los días seleccionados
        for (miembro in miembrosAsignados) {
            val diasSeleccionados = mutableMapOf<String, Boolean>()

            // Solo agregamos los días seleccionados
            if (miembro.diasSeleccionados.containsKey("Lunes")) {
                diasSeleccionados["Lunes"] = false
            }
            if (miembro.diasSeleccionados.containsKey("Martes")) {
                diasSeleccionados["Martes"] = false
            }
            if (miembro.diasSeleccionados.containsKey("Miércoles")) {
                diasSeleccionados["Miércoles"] = false
            }
            if (miembro.diasSeleccionados.containsKey("Jueves")) {
                diasSeleccionados["Jueves"] = false
            }
            if (miembro.diasSeleccionados.containsKey("Viernes")) {
                diasSeleccionados["Viernes"] = false
            }
            if (miembro.diasSeleccionados.containsKey("Sábado")) {
                diasSeleccionados["Sábado"] = false
            }
            if (miembro.diasSeleccionados.containsKey("Domingo")) {
                diasSeleccionados["Domingo"] = false
            }

            // Verifica si el miembro tiene al menos un día seleccionado
            if (diasSeleccionados.isNotEmpty()) {
                diasTareas = true
                // Agrega las asignaciones del miembro al mapa principal de asignaciones

                asignaciones[miembro.member.id] = diasSeleccionados
            } else{
                diasTareas = false
                Toast.makeText(this, "Todos los miembros de la tarea deben tener al menos un día", Toast.LENGTH_SHORT).show()
                return
            }



            // Actualizamos el miembro con el nuevo ID de tarea
            val miembroRef =
                FirebaseDatabase.getInstance().reference.child("members").child(miembro.member.id)
            miembroRef.child("tasks").push().setValue(taskId)
        }

        // Si ningún miembro tiene días asignados, detiene la creación de la tarea
        if (!diasTareas){
            return
        }

        // Crea un objeto Task con la información recopilada
        val tarea = Task(
            id = taskId,
            titulo = nombre,
            descripcion = descripcion,
            homeId = homeId,
            assignments = asignaciones
        )

        // Guardamos la tarea en la base de datos
        val database = FirebaseDatabase.getInstance().reference
        database.child("tasks").child(taskId).setValue(tarea)
            .addOnSuccessListener {
                Toast.makeText(this, "Tarea creada exitosamente", Toast.LENGTH_SHORT).show()

                // Actualizamos el hogar con el nuevo ID de tarea
                val homeRef = FirebaseDatabase.getInstance().reference.child("homes").child(homeId)
                homeRef.child("tasks").push().setValue(taskId)

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("HOME", home)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear la tarea", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Actualiza la información de una tarea existente en la base de datos de Firebase.
     * Recopila el nuevo nombre, la descripción y las asignaciones actualizadas de los miembros.
     * Valida que se hayan ingresado todos los datos y que al menos un miembro esté asignado con algún día.
     * Después de actualizar la tarea, regresa a la actividad principal.
     */
    private fun actualizarTarea() {
        // Obtenemos el nombre y descripcion de los EditText
        val nombre = binding.etNombreTarea.text.toString()
        val descripcion = binding.etDescripcion.text.toString()

        // Validaciones iniciales
        if (nombre.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese todos los datos", Toast.LENGTH_SHORT).show()
            return
        }

        if (miembrosAsignados.isEmpty()) {
            Toast.makeText(this, "Por favor agregue al menos un miembro", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtiene IDs necesarios, saliendo si son nulos (esto ya lo hacías, mejorarlo un poco)
        val taskId = tarea?.id ?: run {
            Toast.makeText(this, "Error: ID de tarea no encontrado.", Toast.LENGTH_SHORT).show()
            return
        }
        val homeId = home?.id ?: run {
            Toast.makeText(this, "Error: ID de hogar no encontrado.", Toast.LENGTH_SHORT).show()
            return
        }


        // **** CAMBIO CLAVE: Leer la tarea actual desde Firebase primero ****
        val taskRef = FirebaseDatabase.getInstance().reference.child("tasks").child(taskId)

        taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Obtenemos la tarea original para acceder a sus asignaciones y estados de completado
                val originalTask = snapshot.getValue(Task::class.java)
                // Obtenemos el mapa de asignaciones originales, o un mapa vacío si no existe
                val originalAssignments = originalTask?.assignments ?: emptyMap()

                // Mapa para construir las NUEVAS asignaciones con estados de completado preservados
                val nuevasAsignaciones = mutableMapOf<String, MutableMap<String, Boolean>>() // Usamos MutableMap para los días internos

                var alMenosUnDiaSeleccionadoEnTotal = false // Bandera para verificar si al menos un día globalmente está seleccionado

                // Itera sobre cada miembro que está AHORA asignado en la UI (miembrosAsignados)
                for (miembroAsignadoUI in miembrosAsignados) {
                    val miembroId = miembroAsignadoUI.member.id
                    // Días seleccionados en la UI para este miembro
                    val diasSeleccionadosUI = miembroAsignadoUI.diasSeleccionados // Esto es un Map<String, Boolean> donde Boolean indica si está marcado en la UI

                    val diasParaEsteMiembro = mutableMapOf<String, Boolean>() // Mapa para los días de este miembro en la NUEVA asignación

                    // Itera sobre los días que están marcados como seleccionados en la UI para ESTE miembro
                    for ((diaNombre, isSelectedInUI) in diasSeleccionadosUI) {
                        if (isSelectedInUI) { // Si el checkbox para este día está marcado en la UI
                            alMenosUnDiaSeleccionadoEnTotal = true // Confirmamos que al menos un día está seleccionado globalmente

                            // **** Lógica para PRESERVAR el estado de completado ****
                            // Buscamos este día y miembro en las asignaciones ORIGINALES de la tarea
                            val originalStatus = originalAssignments[miembroId]?.get(diaNombre)

                            // Añadimos el día al mapa de días para este miembro
                            // Si el día YA existía en las asignaciones originales, usamos su estado (true/false).
                            // Si el día NO existía en las asignaciones originales (es un día recién marcado o un miembro nuevo),
                            // lo añadimos con el estado inicial 'false'.
                            diasParaEsteMiembro[diaNombre] = originalStatus ?: false
                        }
                        // Si isSelectedInUI es false, simplemente no añadimos el día al mapa 'diasParaEsteMiembro',
                        // lo que efectivamente lo elimina de las asignaciones si estaba antes.
                    }

                    // Si este miembro tiene al menos un día seleccionado en la UI, lo añadimos a las nuevas asignaciones
                    if (diasParaEsteMiembro.isNotEmpty()) {
                        nuevasAsignaciones[miembroId] = diasParaEsteMiembro
                    }
                    // Si diasParaEsteMiembro está vacío, este miembro no se incluirá en las nuevas asignaciones,
                    // eliminándolo de la tarea si estaba previamente asignado.
                }

                // Validación final: Asegurarse de que al menos un día esté seleccionado en TODA la tarea
                if (!alMenosUnDiaSeleccionadoEnTotal) {
                    Toast.makeText(this@AddEdit, "La tarea debe tener al menos un día asignado en total.", Toast.LENGTH_SHORT).show()
                    return // Detiene la actualización
                }


                // Crea el objeto Task actualizado usando las NUEVAS asignaciones (con estados preservados)
                val tareaActualizada = Task(
                    id = taskId,
                    titulo = nombre,
                    descripcion = descripcion,
                    homeId = homeId,
                    assignments = nuevasAsignaciones // Usa el mapa de asignaciones construido que mantiene 'true'
                )

                // **** Guarda el objeto actualizado en Firebase (reemplazando la versión anterior) ****
                // Como reconstruimos el mapa de asignaciones con los estados correctos, setValue ahora es seguro.
                taskRef.setValue(tareaActualizada)
                    .addOnSuccessListener {
                        Toast.makeText(this@AddEdit, "Tarea actualizada exitosamente", Toast.LENGTH_SHORT).show()


                        val homeRef = FirebaseDatabase.getInstance().reference.child("homes").child(homeId)
                        homeRef.child("tasks").push().setValue(taskId)


                        // Navega de vuelta a MainActivity
                        val intent = Intent(this@AddEdit, MainActivity::class.java)
                        intent.putExtra("HOME", home)
                        startActivity(intent)
                        finish() // Finaliza la actividad AddEdit
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@AddEdit, "Error al actualizar la tarea: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar el error de lectura de la tarea original
                Toast.makeText(this@AddEdit, "Error al leer la tarea original: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }) // Fin del listener de ValueEventListener
    }

    /**
     * Actualiza el estado habilitado del botón "Agregar Habitante"
     * basándose en si hay miembros disponibles para agregar.
     */
    private fun actualizarEstadoBotonAgregarHabitante() {
        binding.btnAgregarHabitante.isEnabled = miembrosDisponibles.isNotEmpty()
    }

}

/**
 * Adaptador personalizado para mostrar la lista de miembros asignados a una tarea
 * y permitir la selección de los días de la semana para cada miembro.
 * @param context El contexto de la actividad.
 * @param miembrosAsignados La lista mutable de objetos [MembersDay] que representan
 * los miembros asignados a la tarea junto con sus días seleccionados.
 * @param miembrosDisponibles La lista mutable de objetos [Member] que aún no han sido
 * asignados a la tarea y están disponibles para ser agregados.
 * @param onListUpdate Una función lambda que se llama cuando la lista de miembros asignados
 * es modificada (se agrega o elimina un miembro). Se utiliza para notificar a la actividad
 * que debe actualizar su UI.
 */
class MiembroAdapter(
    private val context: Context,
    private val miembrosAsignados: MutableList<MembersDay>,
    private val miembrosDisponibles: MutableList<Member>,
    private val onListUpdate: () -> Unit
) : ArrayAdapter<MembersDay>(context, 0, miembrosAsignados) {

    /**
     * Proporciona una vista para cada elemento en el [android.widget.ListView] de miembros asignados.
     * @param position La posición del elemento dentro del adaptador.
     * @param convertView La vista antigua para reutilizar, si está disponible.
     * @param parent El ViewGroup al que se adjuntará la vista.
     * @return La vista que muestra la información del miembro y los controles para los días.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.item_member, parent, false)
        val miembroActual = miembrosAsignados[position]

        // Establecemos instancia de la vista
        val spinner = view.findViewById<Spinner>(R.id.sSeleccionMiembro)
        val btnEliminar = view.findViewById<TextView>(R.id.tvEliminar)

        val chbLunes = view.findViewById<CheckBox>(R.id.cbLunes)
        val chbMartes = view.findViewById<CheckBox>(R.id.cbMartes)
        val chbMiercoles = view.findViewById<CheckBox>(R.id.cbMiercoles)
        val chbJueves = view.findViewById<CheckBox>(R.id.cbJueves)
        val chbViernes = view.findViewById<CheckBox>(R.id.cbViernes)
        val chbSabado = view.findViewById<CheckBox>(R.id.cbSabado)
        val chbDomingo = view.findViewById<CheckBox>(R.id.cbDomingo)
        val cbTodosDias = view.findViewById<CheckBox>(R.id.cbTodos)

        val mapaDias = miembroActual.diasSeleccionados

        // Listener para el checkbox "Todos los días"
        val todosDiasListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            val dias =
                listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
            if (isChecked) {
                dias.forEach { mapaDias[it] = true } // Marca todos los días
            } else {
                dias.forEach { mapaDias.remove(it) } // Quita todos los días
            }
            notifyDataSetChanged() // Redibuja la vista
        }

        // Función para generar listener para los días individuales
        fun listenerCheck(dia: String): CompoundButton.OnCheckedChangeListener {
            return CompoundButton.OnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    mapaDias[dia] = true
                } else {
                    mapaDias.remove(dia)
                }

                // Actualiza el estado del checkbox "Todos"
                cbTodosDias.setOnCheckedChangeListener(null)
                cbTodosDias.isChecked = mapaDias.keys.containsAll(
                    listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
                )
                cbTodosDias.setOnCheckedChangeListener(todosDiasListener)
            }
        }

        // Configuración del Spinner con el miembro actual + los disponibles
        val opciones = mutableListOf<Member>()
        opciones.add(miembroActual.member)
        opciones.addAll(miembrosDisponibles)

        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, opciones)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter
        spinner.setSelection(0) // Selecciona el miembro actual por defecto.

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                selectedIndex: Int,
                id: Long
            ) {
                val seleccionado = opciones[selectedIndex]
                if (seleccionado != miembroActual.member) {
                    miembrosDisponibles.add(miembroActual.member)
                    miembrosAsignados[position] = MembersDay(seleccionado)
                    miembrosDisponibles.remove(seleccionado)
                    onListUpdate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnEliminar.setOnClickListener {
            miembrosDisponibles.add(miembroActual.member)
            miembrosAsignados.removeAt(position)
            onListUpdate()
        }

        // Actualiza el estado de los checkboxes de los días según el mapa de días seleccionados.
        actualizarChecks(
            mapaDias,
            chbLunes,
            chbMartes,
            chbMiercoles,
            chbJueves,
            chbViernes,
            chbSabado,
            chbDomingo,
            cbTodosDias,
            todosDiasListener,
            ::listenerCheck
        )
        return view
    }

    /**
     * Actualiza el estado de los checkboxes de los días y el checkbox "Todos los días"
     * según el mapa de días seleccionados para un miembro.
     * @param mapaDias El mapa que contiene los días seleccionados para el miembro actual.
     * @param chbLunes Checkbox para el día Lunes.
     * @param chbMartes Checkbox para el día Martes.
     * @param chbMiercoles Checkbox para el día Miércoles.
     * @param chbJueves Checkbox para el día Jueves.
     * @param chbViernes Checkbox para el día Viernes.
     * @param chbSabado Checkbox para el día Sábado.
     * @param chbDomingo Checkbox para el día Domingo.
     * @param cbTodosDias Checkbox para seleccionar/deseleccionar todos los días.
     * @param todosDiasListener El listener para el checkbox "Todos los días".
     * @param listenerDia Una función que genera un listener para cada checkbox de día individual.
     */
    private fun actualizarChecks(
        mapaDias: MutableMap<String, Boolean>,
        chbLunes: CheckBox,
        chbMartes: CheckBox,
        chbMiercoles: CheckBox,
        chbJueves: CheckBox,
        chbViernes: CheckBox,
        chbSabado: CheckBox,
        chbDomingo: CheckBox,
        cbTodosDias: CheckBox,
        todosDiasListener: CompoundButton.OnCheckedChangeListener,
        listenerDia: (String) -> CompoundButton.OnCheckedChangeListener
    ) {
        val dias = listOf(
            "Lunes" to chbLunes,
            "Martes" to chbMartes,
            "Miércoles" to chbMiercoles,
            "Jueves" to chbJueves,
            "Viernes" to chbViernes,
            "Sábado" to chbSabado,
            "Domingo" to chbDomingo
        )

        // Limpia listeners y setea estado inicial según el mapa
        dias.forEach { (dia, checkbox) ->
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = mapaDias.containsKey(dia)
        }

        // Limpia el listener del checkbox "Todos los días" y establece su estado inicial
        // según si todos los días de la semana están presentes en el mapa
        cbTodosDias.setOnCheckedChangeListener(null)
        cbTodosDias.isChecked = mapaDias.keys.containsAll(
            dias.map { it.first }
        )

        // Vuelve a asignar los listeners a los checkboxes de los días individuales
        dias.forEach { (dia, checkbox) ->
            checkbox.setOnCheckedChangeListener(listenerDia(dia))
        }

        // Vuelve a asignar el listener al checkbox en todos los dias
        cbTodosDias.setOnCheckedChangeListener(todosDiasListener)
    }


}
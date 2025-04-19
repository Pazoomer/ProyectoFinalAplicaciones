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

        val bundle = intent.extras

        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (bundle != null) {
            accion = bundle.getString("Accion")
            home = bundle.get("Home") as Home?
            tarea = bundle.get("TASK") as Task?

            obtenerMiembrosDeHome { listaMiembros ->
                miembrosDisponibles = listaMiembros.toMutableList()
                actualizarAdapter()
            }
            binding.tvAgregarEditar.text = accion
            binding.btnAgregarEditar.text = accion

            if (accion == "EDITAR") {
                binding.etNombreTarea.setText(tarea?.titulo)
                binding.etDescripcion.setText(tarea?.descripcion)

                val assignments = tarea?.assignments ?: emptyMap()

                obtenerMiembrosPorIds(assignments.keys.toList()) { miembros ->
                    val miembrosDeTarea = mutableListOf<MembersDay>()

                    for (miembro in miembros) {
                        val diasAsignados = assignments[miembro.id] ?: emptyMap()
                        val diasSeleccionados =
                            diasAsignados.keys.associateWith { true }.toMutableMap()
                        miembrosDeTarea.add(MembersDay(miembro, diasSeleccionados))
                    }

                    miembrosAsignados.clear()
                    miembrosAsignados.addAll(miembrosDeTarea)

                    miembrosDisponibles.removeAll(miembros.map { it })

                    actualizarAdapter()
                }


            }
        }

        binding.btnAgregarHabitante.setOnClickListener {
            agregarHabitante()
        }

        binding.btnAgregarEditar.setOnClickListener {
            if (accion.equals("AGREGAR")) {
                agregarTarea()
            } else if (accion.equals("EDITAR")) {
                actualizarTarea()
            }
        }

        binding.tvEliminar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.ivBackArrow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }


    private fun obtenerMiembrosDeHome(callback: (List<Member>) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference
        val homeId = home?.id

        if (homeId.isNullOrEmpty()) {
            Log.e("Firebase", "Error: homeId es nulo o vacío")
            callback(emptyList())
            return
        }

        database.child("homes").child(homeId).child("members")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userIds = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                    obtenerMiembrosPorIds(userIds, callback)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error al obtener IDs de miembros: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    private fun obtenerMiembrosPorIds(userIds: List<String>, callback: (List<Member>) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference
        val miembros = mutableListOf<Member>()
        var pendientes = userIds.size

        if (userIds.isEmpty()) {
            callback(emptyList())
            return
        }

        for (userId in userIds) {
            database.child("members").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        val name = userSnapshot.child("name").getValue(String::class.java)
                        val email = userSnapshot.child("email").getValue(String::class.java)

                        val miembro = Member(
                            id = userId,
                            name = name ?: "Sin nombre",
                            email = email ?: "Sin correo"
                        )
                        miembros.add(miembro)
                        pendientes--

                        if (pendientes == 0) {
                            callback(miembros)
                        }
                    }

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


    private fun agregarHabitante() {
        if (miembrosDisponibles.isNotEmpty()) {
            val primerDisponible = miembrosDisponibles.first()
            miembrosDisponibles.remove(primerDisponible)
            miembrosAsignados.add(MembersDay(primerDisponible))
            actualizarAdapter()
        } else {
            Toast.makeText(this, "No hay más miembros disponibles", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarAdapter() {
        adapter =
            MiembroAdapter(this, miembrosAsignados, miembrosDisponibles) { actualizarAdapter() }
        binding.lvMiembros.adapter = adapter
    }

    private fun agregarTarea() {
        val nombre = binding.etNombreTarea.text.toString()
        val descripcion = binding.etDescripcion.text.toString()

        if (nombre.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese todos los datos", Toast.LENGTH_SHORT).show()
            return
        }

        if (miembrosAsignados.isEmpty()) {
            Toast.makeText(this, "Por favor agregue al menos un miembro", Toast.LENGTH_SHORT).show()
            return
        }

        val asignaciones = mutableMapOf<String, Map<String, Boolean>>()
        val taskId = FirebaseDatabase.getInstance().reference.child("tasks").push().key ?: return
        val homeId = home?.id ?: ""

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

            if (diasSeleccionados.isNotEmpty()) {
                asignaciones[miembro.member.id] = diasSeleccionados
            }

            // Actualizamos el miembro con el nuevo ID de tarea
            val miembroRef =
                FirebaseDatabase.getInstance().reference.child("members").child(miembro.member.id)
            miembroRef.child("tasks").push().setValue(taskId)
        }

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

    private fun actualizarTarea() {
        val nombre = binding.etNombreTarea.text.toString()
        val descripcion = binding.etDescripcion.text.toString()

        if (nombre.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese todos los datos", Toast.LENGTH_SHORT).show()
            return
        }

        if (miembrosAsignados.isEmpty()) {
            Toast.makeText(this, "Por favor agregue al menos un miembro", Toast.LENGTH_SHORT).show()
            return
        }

        val asignaciones = mutableMapOf<String, Map<String, Boolean>>()

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

            if (diasSeleccionados.isNotEmpty()) {
                asignaciones[miembro.member.id] = diasSeleccionados
            }
        }

        val tareaActualizada = Task(
            id = tarea?.id ?: return,
            titulo = nombre,
            descripcion = descripcion,
            homeId = home?.id ?: "",
            assignments = asignaciones
        )

        val database = FirebaseDatabase.getInstance().reference
        database.child("tasks").child(tarea?.id ?: "").setValue(tareaActualizada)
            .addOnSuccessListener {
                Toast.makeText(this, "Tarea actualizada exitosamente", Toast.LENGTH_SHORT).show()

                val homeRef =
                    FirebaseDatabase.getInstance().reference.child("homes").child(home?.id ?: "")
                homeRef.child("tasks").push().setValue(tarea?.id)

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("HOME", home)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar la tarea", Toast.LENGTH_SHORT).show()
            }
    }

}

class MiembroAdapter(
    private val context: Context,
    private val miembrosAsignados: MutableList<MembersDay>,
    private val miembrosDisponibles: MutableList<Member>,
    private val onListUpdate: () -> Unit
) : ArrayAdapter<MembersDay>(context, 0, miembrosAsignados) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.item_member, parent, false)
        val miembroActual = miembrosAsignados[position]

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
        spinner.setSelection(0)

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

        cbTodosDias.setOnCheckedChangeListener(null)
        cbTodosDias.isChecked = mapaDias.keys.containsAll(
            dias.map { it.first }
        )

        // Vuelve a setear listeners
        dias.forEach { (dia, checkbox) ->
            checkbox.setOnCheckedChangeListener(listenerDia(dia))
        }

        cbTodosDias.setOnCheckedChangeListener(todosDiasListener)
    }


}

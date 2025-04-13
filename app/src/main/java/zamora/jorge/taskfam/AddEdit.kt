package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.data.Member
import zamora.jorge.taskfam.databinding.ActivityAddEditBinding

class AddEdit : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private var miembrosDisponibles = mutableListOf<Member>()
    private var miembrosAsignados = mutableListOf<Member>()
    private lateinit var adapter: MiembroAdapter
    private var accion: String?=""
    private var home: Home?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK

        val bundle = intent.extras




        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (bundle!=null){
            accion = bundle.getString("Accion")
            home = bundle.get("Home") as Home?
            obtenerMiembrosDeHome { listaMiembros ->
                miembrosDisponibles = listaMiembros.toMutableList()
                actualizarAdapter()
            }
            binding.tvAgregarEditar.text = accion
        }



        binding.btnAgregarHabitante.setOnClickListener {
            agregarHabitante()
        }

        binding.btnAgregarEditar.setOnClickListener {
            agregarEditar()
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
            miembrosAsignados.add(primerDisponible)
            actualizarAdapter()
        } else {
            Toast.makeText(this, "No hay más miembros disponibles", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarAdapter() {
        adapter = MiembroAdapter(this, miembrosAsignados, miembrosDisponibles) { actualizarAdapter() }
        binding.lvMiembros.adapter = adapter
    }

    private fun agregarEditar() {
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

        // TODO: Agregar tarea en la base de datos

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}

class MiembroAdapter(
    private val context: Context,
    private val miembrosAsignados: MutableList<Member>,
    private val miembrosDisponibles: MutableList<Member>,
    private val onListUpdate: () -> Unit
) : ArrayAdapter<Member>(context, 0, miembrosAsignados) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_member, parent, false)
        val miembroActual = miembrosAsignados[position]

        val spinner = view.findViewById<Spinner>(R.id.sSeleccionMiembro)
        val btnEliminar = view.findViewById<TextView>(R.id.tvEliminar)

        // Combina el miembro actual con los disponibles para llenar el spinner
        val opciones = mutableListOf<Member>()
        opciones.add(miembroActual)
        opciones.addAll(miembrosDisponibles)

        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, opciones)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter
        spinner.setSelection(0)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, selectedIndex: Int, id: Long) {
                val seleccionado = opciones[selectedIndex]
                if (seleccionado != miembroActual) {
                    miembrosDisponibles.add(miembroActual)
                    miembrosAsignados[position] = seleccionado
                    miembrosDisponibles.remove(seleccionado)
                    onListUpdate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnEliminar.setOnClickListener {
            miembrosDisponibles.add(miembroActual)
            miembrosAsignados.removeAt(position)
            onListUpdate()
        }

        return view
    }
}
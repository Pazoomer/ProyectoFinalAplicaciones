package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import zamora.jorge.taskfam.data.Member
import zamora.jorge.taskfam.databinding.ActivityAddEditBinding

class AddEdit : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private val listaMiembros = mutableListOf<Member>()
    private lateinit var adapter: MiembroAdapter
    private val listaNombres = listOf("Beto", "Chuy", "Abel", "Jorge", "Alma", "El Novaaaaaaak")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK

        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar el ListView con el adapter
        adapter = MiembroAdapter(this, listaMiembros, listaNombres)
        binding.lvMiembros.adapter = adapter

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

    private fun agregarHabitante() {
        // Agregar un nuevo miembro vacío a la lista
        val nuevoMiembro = Member("Selecciona un nombre")
        listaMiembros.add(nuevoMiembro)
        adapter.notifyDataSetChanged() // Refrescar la lista
    }

    private fun agregarEditar() {
        val nombre = binding.etNombreTarea.text.toString()
        val descripcion = binding.etDescripcion.text.toString()

        if (nombre.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese todos los datos", Toast.LENGTH_SHORT).show()
            return
        }

        if (listaMiembros.isEmpty()) {
            Toast.makeText(this, "Por favor agregue al menos un miembro", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Agregar tarea en la base de datos

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}

class MiembroAdapter(
    context: Context,
    private val miembros: MutableList<Member>,
    private val listaNombres: List<String>
) : ArrayAdapter<Member>(context, 0, miembros) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_member, parent, false)

        val miembro = miembros[position]
        val spinner = view.findViewById<Spinner>(R.id.sSeleccionMiembro)
        val btnEliminar = view.findViewById<TextView>(R.id.tvEliminar)

        btnEliminar.setOnClickListener {
            miembros.removeAt(position)
            notifyDataSetChanged()
        }

        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, listaNombres)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        // Establecer el miembro seleccionado si ya tiene un nombre válido
        val index = listaNombres.indexOf(miembro.name)
        if (index != -1) {
            spinner.setSelection(index)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, selectedIndex: Int, id: Long) {
                // Asegurar que no haya IndexOutOfBoundsException
                if (position < miembros.size) {
                    miembros[position].name = listaNombres[selectedIndex]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return view
    }
}
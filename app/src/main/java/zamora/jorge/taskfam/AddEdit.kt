package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import zamora.jorge.taskfam.databinding.ActivityAddEditBinding

class AddEdit : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private val listaMiembros = mutableListOf<Member>()
    private lateinit var adapter: MiembroAdapter
    private val listaNombres = listOf("Beto", "Chuy", "Abel", "Jorge", "Alma", "El Novaaaaaaak")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar el Spinner con la lista de nombres
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaNombres)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sSeleccionMiembro.adapter = spinnerAdapter

        // Configurar el ListView con el adapter
        adapter = MiembroAdapter(this, listaMiembros)
        binding.lvMiembros.adapter = adapter

        binding.btnAgregarHabitante.setOnClickListener {
            val nombreSeleccionado = binding.sSeleccionMiembro.selectedItem.toString()

            if (nombreSeleccionado.isNotEmpty()) {
                val nuevoMiembro = Member(nombreSeleccionado)
                listaMiembros.add(nuevoMiembro)
                adapter.notifyDataSetChanged() // Refrescar la lista
            }
        }

        binding.btnAgregarEditar.setOnClickListener(){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.tvEliminar.setOnClickListener(){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.ivBackArrow.setOnClickListener(){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}

class MiembroAdapter(context: Context, private val miembros: List<Member>) :
    ArrayAdapter<Member>(context, 0, miembros) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_member, parent, false)

        val miembro = miembros[position]
        val tvAsignarDiasA = view.findViewById<TextView>(R.id.tvAsignarDiasA)

        tvAsignarDiasA.text = "Asignar a ${miembro.name} los días:"

        return view
    }
}
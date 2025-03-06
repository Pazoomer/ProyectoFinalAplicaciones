package zamora.jorge.taskfam

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import zamora.jorge.taskfam.data.Day
import zamora.jorge.taskfam.data.Task
import zamora.jorge.taskfam.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private val listaDias = mutableListOf<Day>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        llenarListaDias()
        val dayAdapter = DayAdapter(this, listaDias)
        binding.lvDias.adapter = dayAdapter

    }

    private fun llenarListaDias() {
        listaDias.add(Day("Lunes", listOf(
            Task("Limpiar baño", "Tienes que limpiar bien", "Chuy"),
            Task("Limpiar baño", "Tienes que limpiar bien", "Chuy"),
            Task("Limpiar baño", "Tienes que limpiar bien", "Chuy"),
            Task("Lavar platos", "No olvides los vasos", "Abel"),
            Task("sip", "No olvides los vasos", "Juanito")
        ), false))
        listaDias.add(Day("Martes", listOf(
            Task("Sacar la basura", "Hoy es día de recolección", "Maria")
        ), false))
    }

    class DayAdapter(private val context: Context, private val dias: List<Day>) : BaseAdapter() {
        override fun getCount(): Int = dias.size
        override fun getItem(position: Int): Any = dias[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_day, parent, false)

            val tvDia: TextView = view.findViewById(R.id.tv_dia)
            val progressBar: ProgressBar = view.findViewById(R.id.progressBar)

            val dia = dias[position]
            tvDia.text = dia.nombre

            progressBar.max = dia.tareas.size
            progressBar.progress = dia.tareas.count { /* Lógica para determinar si está completada */ false }

            val tareaAdapter = TaskAdapter(context, dia.tareas)
            val listViewTareas: ListView = view.findViewById(R.id.lv_tareas)
            listViewTareas.adapter = tareaAdapter

            return view
        }
    }

    class TaskAdapter(private val context: Context, private val tareas: List<Task>) : BaseAdapter() {
        override fun getCount(): Int = tareas.size
        override fun getItem(position: Int): Any = tareas[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_task_list, parent, false)

            val tvMiembro: TextView = view.findViewById<TextView>(R.id.tv_miembro)
            val tvTitulo: TextView= view.findViewById<TextView>(R.id.tv_titulotarea)
            val tvDescripcion: TextView = view.findViewById<TextView>(R.id.tv_descripciontarea)

            val tarea = tareas[position]
            tvMiembro.text = tarea.miembro
            tvTitulo.text = tarea.title
            tvDescripcion.text = tarea.description

            return view
        }
    }
}
package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private val listaDias = mutableListOf<Day>()
    private var mostrarSoloHoy = false

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


        binding.switchOnOff.setOnCheckedChangeListener { _, isChecked ->
            mostrarSoloHoy = isChecked
            dayAdapter.updateList(getDiasMostrados())
            if (isChecked) {
                binding.tvSwitchSemanal.setTextColor(resources.getColor(android.R.color.black))
                binding.tvSwitchHoy.setTextColor(resources.getColor(android.R.color.white))
            } else {
                binding.tvSwitchHoy.setTextColor(resources.getColor(android.R.color.black))
                binding.tvSwitchSemanal.setTextColor(resources.getColor(android.R.color.white))
            }
        }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, CreateJoinHome::class.java)
            startActivity(intent)
        }

        binding.addTask.setOnClickListener {
            val intent = Intent(this, AddEdit::class.java)
            startActivity(intent)
        }

        binding.ivSettings.setOnClickListener {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
        }


    }

    private fun llenarListaDias() {
        listaDias.add(Day("Lunes", listOf(
            Task("Cocinar", "Tienes que limpiar bien", "Chuy"),
            Task("Cocinar2", "Tienes que limpiar bien", "Chuy"),
            Task("Limpiar baño", "Tienes que limpiar bien", "Chuy"),

        ), false))

        listaDias.add(Day("Martes", listOf(
            Task("Sacar la basura", "Hoy es día de recolección", "Maria"),
            Task("Sacar la basura2", "Hoy es día de recolección", "Maria")
        ), false))
    }

    private fun getDiasMostrados(): List<Day> {
        return if (mostrarSoloHoy) {
            listaDias.take(1)
        } else {
            listaDias
        }
    }

    private class DayAdapter(private val context: Context, private var dias: List<Day>) : BaseAdapter() {
        //Lo estoy poniendo para que actualice los dias dependiendo el switch
        fun updateList(newList: List<Day>) {
            dias = newList
            notifyDataSetChanged()
        }

        override fun getCount(): Int = dias.size
        override fun getItem(position: Int): Any = dias[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            Log.d("DayAdapter", "getView called with position: $position")
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_day, parent, false)

            val tvDia: TextView = view.findViewById(R.id.tv_dia)
            val progressBar: ProgressBar = view.findViewById(R.id.progressBar)

            val dia = dias[position]
            tvDia.text = dia.nombre

            progressBar.max = dia.tareas.size
            progressBar.progress = dia.tareas.count { false }

            val tareaAdapter = TaskAdapter(context, dia.tareas)
            Log.d("DayAdapter", "TaskAdapter created with ${dia.tareas.size} items")

            val listViewTareas: ListView = view.findViewById(R.id.lv_tareas)
            listViewTareas.adapter = tareaAdapter
            setListViewHeightBasedOnChildren(listViewTareas)

            return view
        }
        private fun setListViewHeightBasedOnChildren(listView: ListView) {
            val listAdapter = listView.adapter ?: return
            var totalHeight = 0
            for (i in 0 until listAdapter.count) {
                Log.d("DayAdapter", "Setting height for item $i")
                val listItem = listAdapter.getView(i, null, listView)
                listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.UNSPECIFIED
                )
                totalHeight += listItem.measuredHeight
            }
            val params = listView.layoutParams
            params.height = totalHeight + (listView.dividerHeight * (listAdapter.count - 1))
            listView.layoutParams = params
            listView.requestLayout()
        }
    }

    private class TaskAdapter(private val context: Context, private val tareas: List<Task>) : BaseAdapter() {
        override fun getCount(): Int = tareas.size
        override fun getItem(position: Int): Any = tareas[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            Log.d("TaskAdapter", "getView called with position: $position")
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_task_list, parent, false)

            val tvMiembro: TextView = view.findViewById<TextView>(R.id.tv_miembro)
            val tvTitulo: TextView= view.findViewById<TextView>(R.id.tv_titulotarea)
            val tvDescripcion: TextView = view.findViewById<TextView>(R.id.tv_descripciontarea)
            val llContainer: LinearLayout = view.findViewById<LinearLayout>(R.id.ll_container)

            val tarea = tareas[position]
            tvMiembro.text = tarea.miembro
            tvTitulo.text = tarea.title
            tvDescripcion.text = tarea.description
            Log.d("TaskAdapter", "Task title: ${tarea.title}")

            llContainer.setOnClickListener{
                val intent = Intent(context, TaskDetail::class.java).apply {
                    putExtra("TAREA_NOMBRE", tarea.title)
                    putExtra("TAREA_DESCRIPCION", tarea.description)
                    putExtra("TAREA_MIEMBRO", tarea.miembro)
                }
                context.startActivity(intent)
            }
            return view
        }
    }
}
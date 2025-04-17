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
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import zamora.jorge.taskfam.data.Day
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.data.Task
import zamora.jorge.taskfam.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val listaDias = mutableListOf<Day>()
    private var mostrarSoloHoy = false
    private var home: Home? = null
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        home = intent.getParcelableExtra<Home>("HOME")

        window.statusBarColor = Color.BLACK

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //llenarListaDias()
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
            intent.putExtra("Home", home)
            intent.putExtra("Accion", "AGREGAR")
            startActivity(intent)
        }

        binding.ivSettings.setOnClickListener {
            val intent = Intent(this, Settings::class.java)
            intent.putExtra("HOME", home)
            startActivity(intent)
        }

        println("hola desde oncreate")
        cargarTareasDelHogarActual()
    }


    private fun cargarTareasDelHogarActual() {
        val db = FirebaseDatabase.getInstance().reference
        val homeIdActual = home?.id ?: return
        println("hola tareas")

        db.child("tasks").orderByChild("homeId").equalTo(homeIdActual)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d("TAREAS", "No se encontraron tareas en Firebase")
                    }
                    val tareasPorDia = mutableMapOf<String, MutableList<Task>>() // día -> tareas

                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null) {
                            for ((memberId, diasYEstado) in task.assignments) {
                                for ((dia, estado) in diasYEstado) {
                                    tareasPorDia.getOrPut(dia) { mutableListOf() }.add(task)
                                }
                            }
                        }
                    }
                    Log.d("TAREAS", "Tareas cargadas: ${tareasPorDia.size}")

                    // Aquí podrías convertir el mapa a una lista ordenada si quieres
                    val diasOrdenados = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
                    val listaAgrupada = tareasPorDia.map { (dia, tareas) ->
                        Day(dia, tareas.map { it.id })
                    }


                    listaDias.clear()
                    listaDias.addAll(listaAgrupada)
                    Log.d("TAREAS", "Tareas cargadas: ${listaDias.size}")

                    // Actualizamos la UI
                    (binding.lvDias.adapter as DayAdapter).updateList(getDiasMostrados())

                    // Si tienes una nueva forma de mostrar estos datos, aquí puedes actualizar el adapter
                    // Por ejemplo: myAdapter.updateDias(listaAgrupada)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TAREAS", "Error al obtener tareas", error.toException())
                }
            })
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
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_day, parent, false)

            val tvDia: TextView = view.findViewById(R.id.tv_dia)
            val progressBar: ProgressBar = view.findViewById(R.id.progressBar)

            val dia = dias[position]
            tvDia.text = dia.nombre

            // Actualiza el progreso dependiendo de las tareas
            progressBar.max = dia.tareas.size
            progressBar.progress = dia.tareas.count { false } // Lógica de estado de las tareas

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


    // TODO actualizar para adpatación de la nueva estructura de las tareas
//    private class TaskAdapter(private val context: Context, private val tareas: List<Task>) : BaseAdapter() {
//        override fun getCount(): Int = tareas.size
//        override fun getItem(position: Int): Any = tareas[position]
//        override fun getItemId(position: Int): Long = position.toLong()
//
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//            Log.d("TaskAdapter", "getView called with position: $position")
//            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_task_list, parent, false)
//
//            val tvMiembro: TextView = view.findViewById<TextView>(R.id.tv_miembro)
//            val tvTitulo: TextView= view.findViewById<TextView>(R.id.tv_titulotarea)
//            val tvDescripcion: TextView = view.findViewById<TextView>(R.id.tv_descripciontarea)
//            val llContainer: LinearLayout = view.findViewById<LinearLayout>(R.id.ll_container)
//
//            val tarea = tareas[position]
//            tvMiembro.text = tarea.miembro
//            tvTitulo.text = tarea.title
//            tvDescripcion.text = tarea.description
//            Log.d("TaskAdapter", "Task title: ${tarea.title}")
//
//            llContainer.setOnClickListener{
//                val intent = Intent(context, TaskDetail::class.java).apply {
//                    putExtra("TAREA_NOMBRE", tarea.title)
//                    putExtra("TAREA_DESCRIPCION", tarea.description)
//                    putExtra("TAREA_MIEMBRO", tarea.miembro)
//                }
//                context.startActivity(intent)
//            }
//            return view
//        }
//    }
}
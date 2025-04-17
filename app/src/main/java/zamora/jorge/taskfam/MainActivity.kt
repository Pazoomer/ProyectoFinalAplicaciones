package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.data.Task
import zamora.jorge.taskfam.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tareasPorDia: MutableMap<String, MutableList<Task>> = mutableMapOf()

    private var mostrarSoloHoy = false
    private var home: Home? = null
    private lateinit var dayAdapter: DayAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        home = intent.getParcelableExtra("HOME")
        window.statusBarColor = Color.BLACK

        dayAdapter = DayAdapter(this, emptyList(), tareasPorDia)
        binding.lvDias.layoutManager = LinearLayoutManager(this)
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
            startActivity(Intent(this, CreateJoinHome::class.java))
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

        cargarTareasDelHogarActual()
    }


    private fun cargarTareasDelHogarActual() {
        val db = FirebaseDatabase.getInstance().reference
        val homeIdActual = home?.id ?: return

        // Inicializamos todos los días con listas vacías
        val diasSemana = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
        tareasPorDia.clear()
        for (dia in diasSemana) {
            tareasPorDia[dia] = mutableListOf()
        }

        db.child("tasks").orderByChild("homeId").equalTo(homeIdActual)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null) {
                            for ((_, diasYEstado) in task.assignments) {
                                for ((diaRaw, _) in diasYEstado) {
                                    val diaNormalizado = diaRaw.lowercase()
                                        .replace("á", "a")
                                        .replace("é", "e")
                                        .replace("í", "i")
                                        .replace("ó", "o")
                                        .replace("ú", "u")

                                    tareasPorDia[diaNormalizado]?.add(task)
                                    Log.d("TAREAS", "Tarea agregada al día $diaNormalizado: $task")
                                    Log.d("TAREAS", "Esta tarea es de: ${task.assignments.toString()}")
                                }
                            }
                        }
                    }

                    // Mostrar todos los días, con tareas o sin ellas
                    dayAdapter.updateList(diasSemana)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TAREAS", "Error al obtener tareas", error.toException())
                }
            })
    }




    private fun getDiasMostrados(): List<String> {
        return if (mostrarSoloHoy) {
            val diaActual = obtenerDiaActual()
            listOf(diaActual)
        } else {
            tareasPorDia.keys.toList()
        }
    }

    private fun obtenerDiaActual(): String {
        val dias = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")
        val calendar = Calendar.getInstance()
        return dias[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    }

    class DayAdapter(
        private val context: Context,
        private var dias: List<String>,
        private val tareasPorDia: Map<String, List<Task>>
    ) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

        class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombreDia: TextView = view.findViewById(R.id.tv_dia)
            val rvTareas: RecyclerView = view.findViewById(R.id.lv_tareas)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_day, parent, false)
            return DayViewHolder(view)
        }

        override fun getItemCount(): Int = dias.size

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val dia = dias[position]
            holder.tvNombreDia.text = dia

            val tareasDelDia = tareasPorDia[dia] ?: emptyList()
            val taskAdapter = TaskAdapter(context, tareasDelDia, dia)

            holder.rvTareas.layoutManager = LinearLayoutManager(context)
            holder.rvTareas.adapter = taskAdapter
        }

        fun updateList(nuevaLista: List<String>) {
            dias = nuevaLista
            notifyDataSetChanged()
        }
    }




    class TaskAdapter(
        private val context: Context,
        private val tareas: List<Task>,
        private val dia: String
    ) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

        class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTituloTarea: TextView = view.findViewById(R.id.tv_titulotarea)
            val tvDescripcionTarea: TextView = view.findViewById(R.id.tv_descripciontarea)
            val tvMiembroTarea: TextView = view.findViewById(R.id.tv_miembro)
        }

        fun obtenerNombreMiembroPorId(miembroId: String, callback: (String?) -> Unit) {
            val db = FirebaseDatabase.getInstance().reference
            db.child("members").child(miembroId).child("name")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val nombre = snapshot.getValue(String::class.java)
                        callback(nombre) // Devuelve el nombre por callback
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FIREBASE", "Error al obtener nombre del miembro", error.toException())
                        callback(null)
                    }
                })
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_task_list, parent, false)
            return TaskViewHolder(view)
        }

        override fun getItemCount(): Int = tareas.size

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val tarea = tareas[position]
            if (!tarea.titulo.isNullOrBlank()) {
                holder.tvTituloTarea.text = tarea.titulo
            } else {
                holder.tvTituloTarea.text = "(Tarea sin título)"
            }

            val miembroId = tarea.assignments.keys.firstOrNull()

            if (miembroId != null) {

                obtenerNombreMiembroPorId(miembroId) { nombre ->

                    holder.tvMiembroTarea.text = nombre ?: "(Miembro no encontrado)"
                }
            } else {
                holder.tvMiembroTarea.text = "(No hay miembro asignado)"
            }
        }

    }
}


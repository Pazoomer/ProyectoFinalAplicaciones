package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import zamora.jorge.taskfam.data.AsignacionPorDia
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.data.Member
import zamora.jorge.taskfam.data.Task
import zamora.jorge.taskfam.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    //private val tareasPorDia: MutableMap<String, MutableList<Task>> = mutableMapOf()
    private val tareasPorDia: MutableMap<String, MutableList<AsignacionPorDia>> = mutableMapOf()
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
        val auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid

        home = intent.getParcelableExtra("HOME")

        // Admin?
        if (home?.adminId != currentUserId) {
            binding.ivSettings.visibility = View.GONE
        } else {
            binding.ivSettings.visibility = View.VISIBLE
        }

        binding.tvCasanombreMain.text = home?.nombre ?: "(Sin nombre)"

        window.statusBarColor = Color.BLACK

        dayAdapter = DayAdapter(this, emptyList(), tareasPorDia, home)
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

        val esAdmin = home?.adminId == currentUserId
        val esEditable = home?.editable == true

        // Editable?
        if (!esEditable && !esAdmin) {
            binding.addTask.visibility = View.GONE
        } else {
            binding.addTask.visibility = View.VISIBLE
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

        val diasSemana = listOf(
            "Lunes", "Martes", "Miércoles",
            "Jueves", "Viernes", "Sábado", "Domingo"
        )

        tareasPorDia.clear()
        for (dia in diasSemana) {
            tareasPorDia[dia] = mutableListOf()
        }

        db.child("tasks")
            .orderByChild("homeId")
            .equalTo(homeIdActual)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (dia in diasSemana) {
                        tareasPorDia[dia]?.clear()
                    }

                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null) {
                            for ((miembroId, diasYEstado) in task.assignments) {
                                for ((diaRaw, _) in diasYEstado) {
                                    val diaLimpio = diaRaw.trim()
                                    tareasPorDia[diaLimpio]?.add(
                                        AsignacionPorDia(
                                            tarea     = task,
                                            miembroId = miembroId
                                        )
                                    )
                                    Log.d(
                                        "TAREAS",
                                        "Asignación: tarea='${task.id}' → miembro='$miembroId' en día='$diaLimpio'"
                                    )
                                }
                            }
                        }
                    }

                    dayAdapter.updateList(getDiasMostrados())
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
        val dias = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        val calendar = Calendar.getInstance()
        return dias[calendar.get(Calendar.DAY_OF_WEEK) - 2]
    }


    class DayAdapter(
        private val context: Context,
        private var dias: List<String>,
        private val tareasPorDia: Map<String, List<AsignacionPorDia>>,
        private val home: Home?
    ) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

        class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombreDia: TextView = view.findViewById(R.id.tv_dia)
            val rvTareas: RecyclerView = view.findViewById(R.id.lv_tareas)
            val progressBar: ProgressBar = view.findViewById(R.id.pbTareasCompletadas)
            val tvProgresoTexto: TextView = view.findViewById(R.id.tvProgresoTexto)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_day, parent, false)
            return DayViewHolder(view)
        }

        override fun getItemCount(): Int = dias.size

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val dia = dias[position]
            holder.tvNombreDia.text = dia

            val asignaciones = tareasPorDia[dia] ?: emptyList()

            val taskAdapter = TaskAdapter(context, asignaciones, dia, home)
            holder.rvTareas.layoutManager = LinearLayoutManager(context)
            holder.rvTareas.adapter = taskAdapter

            val totalAsignaciones = asignaciones.size
            val completadas = asignaciones.count { asign ->
                asign.tarea.assignments[asign.miembroId]?.get(dia) == true
            }

            holder.progressBar.max = if (totalAsignaciones == 0) 1 else totalAsignaciones
            holder.progressBar.progress = completadas
            holder.tvProgresoTexto.text = "$completadas/$totalAsignaciones"
        }

        fun updateList(nuevaLista: List<String>) {
            dias = nuevaLista
            notifyDataSetChanged()
        }
    }


    class TaskAdapter(
        private val context: Context,
        private val asignaciones: List<AsignacionPorDia>,
        private val dia: String,
        private val home: Home?
    ) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

        class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTituloTarea: TextView = view.findViewById(R.id.tv_titulotarea)
            val tvDescripcionTarea: TextView = view.findViewById(R.id.tv_descripciontarea)
            val tvMiembroTarea: TextView = view.findViewById(R.id.tv_miembro)
            val btnTarea: View = view.findViewById(R.id.btnCompletarTarea)
            val tareaElemento: LinearLayout = view.findViewById(R.id.tarea)
            val ivMembercolor: ImageView = view.findViewById(R.id.ivMembercolor)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_task_list, parent, false)
            return TaskViewHolder(view)
        }

        override fun getItemCount(): Int = asignaciones.size

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val asignacion = asignaciones[position]
            val tarea = asignacion.tarea
            val miembroId = asignacion.miembroId

            holder.tvTituloTarea.text = tarea.titulo.ifBlank { "(Tarea sin título)" }
            holder.tvDescripcionTarea.text = tarea.descripcion.ifBlank { "(Tarea sin descripción)" }

            obtenerMiembroPorId(miembroId) { miembro ->
                holder.tvMiembroTarea.text = miembro?.name ?: "(Miembro no encontrado)"
                val color = miembro?.color ?: 0xFF888888.toInt()
                (holder.ivMembercolor.background as? GradientDrawable)
                    ?.setColor(color)
            }

            val completada = tarea.assignments[miembroId]?.get(dia) == true
            if (completada) {
                holder.tareaElemento.setBackgroundResource(R.drawable.background_green)
                holder.btnTarea.isEnabled = false
                holder.btnTarea.alpha = 0f
            } else {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val isOwnerOrAdmin =
                    home?.adminId == currentUserId || miembroId == currentUserId

                if (home?.editable == true || isOwnerOrAdmin) {
                    holder.btnTarea.isEnabled = true
                    holder.btnTarea.alpha = 1f
                } else {
                    holder.btnTarea.isEnabled = false
                    holder.btnTarea.alpha = 0f
                }
            }

            holder.btnTarea.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("¿Completar tarea?")
                    .setMessage("¿Deseas marcar esta tarea como completada?")
                    .setPositiveButton("Sí") { _, _ ->
                        completarTarea(tarea.id, miembroId, dia)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            holder.itemView.setOnClickListener {
                Log.d("Tarea info:", "Tarea:\n$tarea")
                val intent = Intent(context, TaskDetail::class.java)
                intent.putExtra("TASK", tarea)
                intent.putExtra("HOME", home)
                context.startActivity(intent)
            }
        }

        private fun obtenerMiembroPorId(miembroId: String, callback: (Member?) -> Unit) {
            val db = FirebaseDatabase.getInstance().reference
            db.child("members").child(miembroId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val miembro = snapshot.getValue(Member::class.java)
                            ?.copy(id = miembroId)
                        callback(miembro)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FIREBASE", "Error al obtener el miembro", error.toException())
                        callback(null)
                    }
                })
        }

        private fun completarTarea(taskId: String, miembroId: String, dia: String) {
            val db = FirebaseDatabase.getInstance().reference
            db.child("tasks")
                .child(taskId)
                .child("assignments")
                .child(miembroId)
                .child(dia)
                .setValue(true)
                .addOnSuccessListener {
                    Toast.makeText(context, "Tarea completada", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al completar tarea", Toast.LENGTH_SHORT).show()
                }
        }
    }
}


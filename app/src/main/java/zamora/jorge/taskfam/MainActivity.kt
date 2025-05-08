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

        // Obtiene la instancia del servicio de autenticación de Firebase
        val auth = FirebaseAuth.getInstance()

        // Obtiene el ID del usuario actualmente, en caso queel usuario si este logueado regresa su ID
        val currentUserId = auth.currentUser?.uid

        // Se obtiene la casa que se selecciono anteriormente para llegar a la pantalla principal
        home = intent.getParcelableExtra("HOME")

        // Se verifica si el usuario actual es el creador/administrador de la casa
        if (home?.adminId != currentUserId) {
            //En caso de no ser el administrador de la casa se ocultan los ajustes de la casa para el usuario
            binding.ivSettings.visibility = View.GONE
        } else {
            //En caso de ser el administrador de la casa, puede acceder a los ajustes de la misma casa
            binding.ivSettings.visibility = View.VISIBLE
        }

        //Se refleja el nombre de la casa en el TextView, en caso de no tener se configura para solo aparecer "Sin nombre"
        binding.tvCasanombreMain.text = home?.nombre ?: "(Sin nombre)"

        //Barra de arriba del dispositivo ovil cambiado a color negro
        window.statusBarColor = Color.BLACK

        // Inicializa el DayAdapter con los datos iniciales (lista de días vacía por ahora) y los mapas de tareas
        dayAdapter = DayAdapter(this, emptyList(), tareasPorDia, home)
        binding.lvDias.layoutManager = LinearLayoutManager(this)
        binding.lvDias.adapter = dayAdapter

        // Listener para pdoer configurar si solo se desea mostrar el día actual con las tareas o todos los días y sus tareas
        binding.switchOnOff.setOnCheckedChangeListener { _, isChecked ->
            // Cambia el estado de mostrarSoloHoy en caso que se haya seleccionado en el switch
            mostrarSoloHoy = isChecked
            //Actualiza la lista de los días mostrados en el adapter
            dayAdapter.updateList(getDiasMostrados())

            //Se cambia el color del texto en las etiquetas del switch para indicar que opción esta activa, vista diaria o semanak
            if (isChecked) {
                binding.tvSwitchSemanal.setTextColor(resources.getColor(android.R.color.black))
                binding.tvSwitchHoy.setTextColor(resources.getColor(android.R.color.white))
            } else {
                binding.tvSwitchHoy.setTextColor(resources.getColor(android.R.color.black))
                binding.tvSwitchSemanal.setTextColor(resources.getColor(android.R.color.white))
            }
        }

        // Listener para regresar a la actividad anterior
        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, CreateJoinHome::class.java))
        }

        // Se verifica si el usuario actual es administrador de la casa
        val esAdmin = home?.adminId == currentUserId

        //Se verifica si la casa es editable
        val esEditable = home?.editable == true

        // En caso que el usuario no sea admin y la casa no sea editable
        if (!esEditable && !esAdmin) {
            // El usuario no puede acceder a la opción de añadir tareas
            binding.addTask.visibility = View.GONE
        } else {
            // El usuario puede acceder a la opción de añadir tareas
            binding.addTask.visibility = View.VISIBLE
        }

        // Listener para agregar tarea
        binding.addTask.setOnClickListener {
            // Crea el Intent para ir a la actividad de AddEdit
            val intent = Intent(this, AddEdit::class.java)
            // Se le pasa el objeto Home
            intent.putExtra("Home", home)
            // Se le asigna la accion agregar
            intent.putExtra("Accion", "AGREGAR")
            // Inicia la actividad
            startActivity(intent)
        }

        // Listener para los ajustes de la casa
        binding.ivSettings.setOnClickListener {
            // Crea el Intent para la actividad Setting
            val intent = Intent(this, Settings::class.java)
            // Se le pasa el objeto Home
            intent.putExtra("HOME", home)
            //Se inicia la actividad
            startActivity(intent)
        }

        // Se cargan las tareas desde la Firebase para la casa actual
        cargarTareasDelHogarActual()
    }

    /**
     * Carga las tareas asignadas para el hogar actual desde la base de datos
     * Establece un listener para recibir actualizaciones en tiempo real de las tareas
     */
    private fun cargarTareasDelHogarActual() {
        // Obtiene la referencia de la base de datos
        val db = FirebaseDatabase.getInstance().reference
        // Obtiene el ID de la casa seleccionada actual, en caso que sea null sale de la función
        val homeIdActual = home?.id ?: return

        // Lista de los días de la semana
        val diasSemana = listOf(
            "Lunes", "Martes", "Miércoles",
            "Jueves", "Viernes", "Sábado", "Domingo"
        )

        // Limpia el mapa de tareas y lo reinicializa con listas vacías para cada día.
        tareasPorDia.clear()
        for (dia in diasSemana) {
            tareasPorDia[dia] = mutableListOf()
        }

        // Se consultan las tareas en la firebase filtrandolas por el homeId de la casa actual
        // Se agrega un ValueEventListener para poder escuchar los cambios de los datos
        db.child("tasks") //Se accede al nodo de tasks
            .orderByChild("homeId") // Se ordena por el homeId
            .equalTo(homeIdActual) // Solo obtiene tareas donde el homeId sea igual al de la casa actual
            .addValueEventListener(object : ValueEventListener { // Listener para escuchar los cambios

                /**
                 *  Se llama cuando hay un cambo en los datos en la ubicación que se especifico, en este caso las tareas de la casa seleccionada
                 */
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Limpia las listas de tareas para cada día antes de volver a llenarlas
                    for (dia in diasSemana) {
                        tareasPorDia[dia]?.clear()
                    }

                    // Itera sobre cada tarea encontrada
                    for (taskSnapshot in snapshot.children) {
                        // Convierte el DataSnapshot de la tarea a un objeto Task
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null) {
                            // Itera sobre las asignaciones de la tarea (miembroId a díasYEstado).
                            for ((miembroId, diasYEstado) in task.assignments) {
                                for ((diaRaw, _) in diasYEstado) {
                                    val diaLimpio = diaRaw.trim()
                                    // Agrega una nueva AsignacionPorDia a la lista correspondiente en el mapa 'tareasPorDia'
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
                    // Actualiza el adaptador para reflejar los datos
                    dayAdapter.updateList(getDiasMostrados())
                }

                /**
                 * Se llama si la operación de lectura de datos es cancelada.
                 */
                override fun onCancelled(error: DatabaseError) {
                    Log.e("TAREAS", "Error al obtener tareas", error.toException())
                }
            })
    }

    /**
     * Determina la lista de días que se deben mostrar
     * Si mostrarSoloHoy es true, retorna una lista con solo el día actual
     * Si mostrarSoloHoy es false, retorna la lista completa de días disponibles en tareasPorDia
     * @return Una lista de Strings con los nombres de los días a mostrar
     */
    private fun getDiasMostrados(): List<String> {
        return if (mostrarSoloHoy) {
            val diaActual = obtenerDiaActual()
            listOf(diaActual)
        } else {
            tareasPorDia.keys.toList()
        }
    }

    /**
     * Obtiene el nombre del día actual de la semana
     * @return El nombre del día actual
     */
    private fun obtenerDiaActual(): String {
        val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado","Domingo")
        val calendar = Calendar.getInstance()
        return dias[calendar.get(Calendar.DAY_OF_WEEK) - 2]
    }

    /**
     * Adaptador para el RecyclerView que muestra los días de la semana
     * Cada elemento de día contiene un RecyclerView anidado para mostrar las tareas de ese día
     * @param context El contexto de la actividad
     * @param dias La lista de nombres de los días a mostrar
     * @param tareasPorDia Mapa que contiene las listas de tareas organizadas por día
     * @param home El objeto Home asociado
     */
    class DayAdapter(
        private val context: Context,
        private var dias: List<String>,
        private val tareasPorDia: Map<String, List<AsignacionPorDia>>,
        private val home: Home?
    ) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

        /**
         * ViewHolder para un solo elemento (día) en el DayAdapter
         * Referencia a las vistas dentro del layout item_day.xml
         */
        class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombreDia: TextView = view.findViewById(R.id.tv_dia)
            val rvTareas: RecyclerView = view.findViewById(R.id.lv_tareas)
            val progressBar: ProgressBar = view.findViewById(R.id.pbTareasCompletadas)
            val tvProgresoTexto: TextView = view.findViewById(R.id.tvProgresoTexto)
        }

        /**
         * Crea y devuelve un nuevo DayViewHolder inflando el layout item_day.xml
         * @param parent El ViewGroup al que se adjuntará la nueva vista
         * @param viewType El tipo de vista del nuevo elemento
         * @return Un nuevo DayViewHolder.
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_day, parent, false)
            return DayViewHolder(view)
        }

        /**
         * Retorna el número total de elementos (días) en el conjunto de datos del adaptador
         * @return El número de días a mostrar
         */
        override fun getItemCount(): Int = dias.size

        /**
         * Vincula los datos de un día específico a las vistas del DayViewHolder
         * Configura el nombre del día, inicializa el TaskAdapter para las tareas de ese día, calcula y muestra el progreso de las tareas completadas.
         * @param holder El ViewHolder que debe ser actualizado para representar el contenido en la posición dada
         * @param position La posición del elemento dentro del conjunto de datos del adaptador
         */
        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            // Se obtiene el nombre del día de la posición actual
            val dia = dias[position]
            // Establece el nombre del día en el TextView correspondiente.
            holder.tvNombreDia.text = dia

            // Obtiene la lista de asignaciones de tareas para este día del mapa tareasPorDia
            val asignaciones = tareasPorDia[dia] ?: emptyList()

            // Crea un TaskAdapter para las tareas de este día específico.
            val taskAdapter = TaskAdapter(context, asignaciones, dia, home)
            // Configura el RecyclerView anidado para las tareas con un LinearLayoutManager
            holder.rvTareas.layoutManager = LinearLayoutManager(context)
            // Asigna el TaskAdapter al RecyclerView anidado
            holder.rvTareas.adapter = taskAdapter

            // Calcula el progreso de las tareas completadas para este día
            val totalAsignaciones = asignaciones.size
            // Cuenta cuántas asignaciones para este día están marcadas como completadas
            val completadas = asignaciones.count { asign ->
                asign.tarea.assignments[asign.miembroId]?.get(dia) == true
            }

            // Configura la ProgressBar: el máximo es el total de tareas, el progreso es el número de completadas
            // Si no hay tareas, establece el máximo a 1 para evitar división por cero o ProgressBar vacía
            holder.progressBar.max = if (totalAsignaciones == 0) 1 else totalAsignaciones
            holder.progressBar.progress = completadas
            // Muestra el texto de progreso
            holder.tvProgresoTexto.text = "$completadas/$totalAsignaciones"

            holder.rvTareas.layoutManager = LinearLayoutManager(context)
            holder.rvTareas.adapter = taskAdapter
            holder.rvTareas.setHasFixedSize(false)
            holder.rvTareas.isNestedScrollingEnabled = false
        }

        /**
         * Actualiza la lista de días que el adaptador debe mostrar y notifica a la UI del cambio
         * @param nuevaLista La nueva lista de nombres de días a mostrar
         */
        fun updateList(nuevaLista: List<String>) {
            dias = nuevaLista
            notifyDataSetChanged()
        }
    }

    /**
     * Adaptador para el RecyclerView anidado que muestra la lista de tareas para un día específico
     * @param context El contexto
     * @param asignaciones La lista de asignaciones de tareas para este día
     * @param dia El nombre del día  para el que se muestran estas tareas.
     * @param home El objeto Home asociado.
     */
    class TaskAdapter(
        private val context: Context,
        private val asignaciones: List<AsignacionPorDia>,
        private val dia: String,
        private val home: Home?
    ) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

        /**
         * ViewHolder para un solo elemento (tarea) en el TaskAdapter.
         * Referencia a la vista dentro del layout item_task_list.xml.
         */
        class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTituloTarea: TextView = view.findViewById(R.id.tv_titulotarea)
            val tvDescripcionTarea: TextView = view.findViewById(R.id.tv_descripciontarea)
            val tvMiembroTarea: TextView = view.findViewById(R.id.tv_miembro)
            val btnTarea: View = view.findViewById(R.id.btnCompletarTarea)
            val tareaElemento: LinearLayout = view.findViewById(R.id.tarea)
            val ivMembercolor: ImageView = view.findViewById(R.id.ivMembercolor)
        }

        /**
         * Crea y devuelve un nuevo TaskViewHolder inflando el layout item_task_list.xml
         * @param parent El ViewGroup al que se adjuntará la nueva vista
         * @param viewType El tipo de vista del nuevo elemento
         * @return Un nuevo TaskViewHolder
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_task_list, parent, false)
            return TaskViewHolder(view)
        }

        /**
         * Retorna el número total de tareas en el conjunto de datos del adaptador para este día
         * @return El número de asignaciones de tareas para el día.
         */
        override fun getItemCount(): Int = asignaciones.size

        /**
         * Vincula los datos de una asignación de tarea específica a las vistas del TaskViewHolder
         * Muestra los detalles de la tarea y configura los listeners de clic
         * @param holder El ViewHolder que debe ser actualizado
         * @param position La posición de la asignación de tarea en la lista para este día
         */
        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            // Obtiene la asignación (tarea y miembro) para la posición actual.
            val asignacion = asignaciones[position]
            val tarea = asignacion.tarea
            val miembroId = asignacion.miembroId

            // Establece cual es el nombre del titulo y descripción de la tarea
            holder.tvTituloTarea.text = tarea.titulo.ifBlank { "(Tarea sin título)" }
            holder.tvDescripcionTarea.text = tarea.descripcion.ifBlank { "(Tarea sin descripción)" }

            // Obtiene la información del miembro asignado usando su ID.
            obtenerMiembroPorId(miembroId) { miembro ->
                // Cuando se obtiene el miembro, actualiza el TextView del nombre del miembro
                holder.tvMiembroTarea.text = miembro?.name ?: "(Miembro no encontrado)"
                val color = miembro?.color ?: 0xFF888888.toInt()
                (holder.ivMembercolor.background as? GradientDrawable)
                    ?.setColor(color)
            }

            // Verifica si la tarea está completada para este miembro en este día específico
            val completada = tarea.assignments[miembroId]?.get(dia) == true

            // Configura la apariencia y clicabilidad del elemento de tarea según su estado de completado
            if (completada) {
                // Si está completada, cambia el fondo a verde y deshabilita/oculta el botón de completar.
                holder.tareaElemento.setBackgroundResource(R.drawable.background_green)
                holder.btnTarea.isEnabled = false
                holder.btnTarea.alpha = 0f
            } else {
                // Si no está completada, verifica si el usuario actual puede completar la tarea.
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                // El usuario puede completar la tarea si es el administrador de la casa o si es el miembro asignado a la tarea
                val isOwnerOrAdmin =
                    home?.adminId == currentUserId || miembroId == currentUserId

                // Muestra el botón de completar si la casa es editable o si el usuario actual es el administrador.
                if (home?.editable == true || isOwnerOrAdmin) {
                    holder.btnTarea.isEnabled = true
                    holder.btnTarea.alpha = 1f
                } else {
                    // Si la casa no es editable y el usuario no es el asignado ni el admin, oculta el botón.
                    holder.btnTarea.isEnabled = false
                    holder.btnTarea.alpha = 0f
                }
            }

            // Listener para el botón de completar tarea
            holder.btnTarea.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("¿Completar tarea?")
                    .setMessage("¿Deseas marcar esta tarea como completada?")
                    .setPositiveButton("Sí") { _, _ ->
                        completarTarea(tarea.id, miembroId, dia, context)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            // Listener de la tarea
            holder.itemView.setOnClickListener {
                Log.d("Tarea info:", "Tarea:\n$tarea")
                // Se crea un intent para ir a la actividad de TaskDetail
                val intent = Intent(context, TaskDetail::class.java)
                // Pasa el objeto Task y el objeto Home a la actividad de detalle
                intent.putExtra("TASK", tarea)
                intent.putExtra("HOME", home)
                // Inicia el intent
                context.startActivity(intent)
            }
        }

        /**
         * Obtiene el un objeto memter de Firebase por medio del ID
         * @param miembroId id del miembro a buscar
         * @param callback Función lambda que se llamará con el objeto Member encontrado
         */
        private fun obtenerMiembroPorId(miembroId: String, callback: (Member?) -> Unit) {
            // Referencia a la BD de firebase
            val db = FirebaseDatabase.getInstance().reference
            // Se accede al nodo de members y luego al nodo del miembroId especifico
            db.child("members").child(miembroId)
                // Listener para obtener el dato member
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    /**
                     * Se llama cuando se obtienen los datos del miembro
                     */
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Convierte el snapshot a un objeto Member
                        val miembro = snapshot.getValue(Member::class.java)
                            ?.copy(id = miembroId)
                        // Llama al callback con el objeto Member
                        callback(miembro)
                    }

                    /**
                     * Si la operación es cancelada
                     */
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FIREBASE", "Error al obtener el miembro", error.toException())
                        callback(null)
                    }
                })
        }

        /**
         * Marca una tarea especifica como completada en la bd de firebase
         * @param taskId id de la tarea a completar
         * @param miembroId id del mimebro que completo la tarea
         * @param dia dia especifico en que se completo la tarea
         * @param context contexto
         */
        private fun completarTarea(taskId: String, miembroId: String, dia: String, context: Context) {
            // Mapeo de nombres de día a números (Lunes=1, ..., Domingo=7)
            val diasSemanaMap = mapOf(
                "lunes" to 1,
                "martes" to 2,
                "miércoles" to 3,
                "jueves" to 4,
                "viernes" to 5,
                "sábado" to 6,
                "domingo" to 7
            )

            // Obtiene el valor numérico del día de la tarea. Usamos lowercase() - CORREGIDO
            val diaTareaNum = diasSemanaMap[dia.lowercase()] // <-- Línea Corregida aquí

            // Verifica si el día de la tarea es válido según nuestro mapeo
            if (diaTareaNum == null) {
                Toast.makeText(context, "Día de tarea inválido: $dia", Toast.LENGTH_SHORT).show()
                return // Sale de la función si el día no es válido
            }

            // Obtiene el día actual de la semana usando Calendar
            val calendar = Calendar.getInstance()
            // Calendar.DAY_OF_WEEK da 1=Domingo, 2=Lunes, ..., 7=Sábado.
            // Necesitamos convertir esto a nuestro mapeo (1=Lunes, ..., 7=Domingo).
            val diaActualCalendar = calendar.get(Calendar.DAY_OF_WEEK)

            // Mapea el día de Calendar al número según nuestra lógica (1=Lunes, ..., 7=Domingo)
            val diaActualNum = when (diaActualCalendar) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> -1 // Caso de error inesperado
            }

            // Verifica que pudimos obtener el día actual correctamente
            if (diaActualNum == -1) {
                Toast.makeText(context, "Error al obtener el día actual.", Toast.LENGTH_SHORT).show()
                return // Sale de la función
            }


            // Compara el día de la tarea con el día actual
            if (diaTareaNum <= diaActualNum) {
                // Si el día de la tarea es hoy o un día anterior, procede a marcar como completada

                // Refrencia de la bd en firebase
                val db = FirebaseDatabase.getInstance().reference
                db.child("tasks")
                    .child(taskId)
                    .child("assignments")
                    .child(miembroId)
                    .child(dia) // Usamos el nombre original del día para la ruta de Firebase
                    // Establece el valor a true apra marcar como completada
                    .setValue(true)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Tarea completada", Toast.LENGTH_SHORT).show()
                    }
                    // Agrega un listener para manejar el fallo de la operación
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al completar tarea", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Si el día de la tarea es un día futuro, no permite completar
                Toast.makeText(context, "No puedes completar tareas futuras.", Toast.LENGTH_LONG).show()
            }
        }
    }
}


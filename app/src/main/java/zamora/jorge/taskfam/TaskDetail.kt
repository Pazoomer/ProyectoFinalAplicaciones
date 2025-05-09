package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvInputManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.data.Member
import zamora.jorge.taskfam.data.Task
import zamora.jorge.taskfam.databinding.ActivityTaskDetailBinding

class TaskDetail : AppCompatActivity() {
    private lateinit var binding: ActivityTaskDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        window.statusBarColor = Color.BLACK


        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtenemos la informacion desde los extras, casa y tarea
        val tarea = intent.getParcelableExtra<Task>("TASK")
        val home = intent.getParcelableExtra<Home>("HOME")

        binding.tvTitulotarea.text = tarea?.titulo ?: "Tarea sin título"
        binding.tvDescripcion.text = tarea?.descripcion ?: "Tarea sin descripción"

        val miembrosAsignados = tarea?.assignments?.keys?.toList() ?: emptyList()

        // Configura el adaptador para el ListView que muestra los miembros asignados y sus días
        binding.lvTaskDetail.adapter = MiembroTareaAdapter(this, tarea!!, home!!)

        // Listener para el botón de retroceso que regresa a la actividad MainActivity
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("HOME", home)
            startActivity(intent)
        }

        // Obtiene el ID del usuario actual
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val esAdmin = currentUserId == home?.adminId
        val esEditable = home?.editable ?: false

        //Muestra o oculta el boton de editar dependiendo de los permisos
        if (esEditable || esAdmin) {
            binding.editTask.visibility = View.VISIBLE
        } else {
            binding.editTask.visibility = View.GONE
        }

        // Listener para el botón de editar la tarea, que navega a la actividad AddEdit en modo "EDITAR"
        binding.editTask.setOnClickListener {
            val intent = Intent(this, AddEdit::class.java)
            intent.putExtra("Home", home)
            intent.putExtra("TASK", tarea)
            intent.putExtra("Accion", "EDITAR")
            startActivity(intent)
        }

    }
}

/**
 * Adaptador personalizado para mostrar la lista de miembros asignados a una tarea
 * y los días de la semana en los que están asignados.
 * @param context El contexto de la actividad.
 * @param tarea El objeto [Task] cuyos detalles se están mostrando.
 * @param home El objeto [Home] al que pertenece la tarea.
 */
class MiembroTareaAdapter(
    private val context: Context,
    private val tarea: Task,
    private val home: Home
) : BaseAdapter() {

    // Ontenemos la lista de ids de los miembros asignados a la tarea
    private val miembros = tarea.assignments.keys.toList()
    // Lista mutable de nombre de miembros
    private val miembrosMap = mutableMapOf<String, Member?>()
    private var dataLoaded = false

    /**
     * Bloque de inicialización que carga los nombres de los miembros asignados.
     */
    init {
        cargarDatosMiembros()
    }

    /**
     * Carga el nombre de cada miembro asignado desde la base de datos de Firebase
     * y lo almacena en el mapa `nombresMiembros`. Notifica al adaptador cuando un nombre es cargado
     * para actualizar la vista.
     */
    private fun cargarDatosMiembros() {
        val db = FirebaseDatabase.getInstance().reference
        var miembrosCargados = 0
        miembros.forEach { miembroId ->
            db.child("members").child(miembroId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Deserializa el snapshot a un objeto Member
                        val miembro = snapshot.getValue(Member::class.java)?.copy(id = miembroId)
                        miembrosMap[miembroId] = miembro
                        miembrosCargados++
                        if (miembrosCargados == miembros.size) {
                            dataLoaded = true
                            notifyDataSetChanged()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MiembroTareaAdapter", "Error al cargar datos del miembro: $miembroId", error.toException())
                        miembrosMap[miembroId] = null
                        miembrosCargados++
                        if (miembrosCargados == miembros.size) {
                            dataLoaded = true
                            notifyDataSetChanged()
                        }
                    }
                })
        }
    }

    override fun getCount(): Int = miembros.size
    override fun getItem(position: Int): Any = miembros[position]
    override fun getItemId(position: Int): Long = position.toLong()

    /**
     * Proporciona una vista para cada elemento en el ListView de miembros asignados a la tarea.
     * @param position La posición del miembro en la lista.
     * @param convertView La vista antigua para reutilizar, si está disponible.
     * @param parent El ViewGroup al que se adjuntará la vista.
     * @return La vista que muestra el nombre del miembro y los checkboxes de los días asignados.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_task_detail, parent, false)

        // Id del miembro acutual a mostrar
        val miembroId = miembros[position]

        // Obtiene el mapa de días asignados para el miembro actual
        val dias = tarea.assignments[miembroId] ?: emptyMap()

        val tvMiembro = view.findViewById<TextView>(R.id.tvMiembroDetail)
        val ivColor = view.findViewById<ImageView>(R.id.iv_color)

        val checkBoxes = mapOf(
            "Lunes" to view.findViewById<CheckBox>(R.id.cbLunes),
            "Martes" to view.findViewById<CheckBox>(R.id.cbMartes),
            "Miércoles" to view.findViewById<CheckBox>(R.id.cbMiercoles),
            "Jueves" to view.findViewById<CheckBox>(R.id.cbJueves),
            "Viernes" to view.findViewById<CheckBox>(R.id.cbViernes),
            "Sábado" to view.findViewById<CheckBox>(R.id.cbSabado),
            "Domingo" to view.findViewById<CheckBox>(R.id.cbDomingo)
        )

        // Rellena o no los checkboxes
        checkBoxes.values.forEach { it.isChecked = false }

        // Marcamos los dias aignados
        checkBoxes.forEach { (diaEsperado, checkBox) ->
            checkBox.isChecked = dias.containsKey(diaEsperado)
        }
        // Obtiene y muestra el nombre del miembro utilizando su ID
        obtenerNombreMiembroPorId(miembroId) { miembro ->
            tvMiembro.text = miembro?.name ?: "(Miembro no encontrado)"
            val color = miembro?.color ?: Color.BLACK

            // Creamos un circulo con GradientDrawable, para que se muestre correctamente en el detalle
            val circleDrawable = GradientDrawable()
            circleDrawable.shape = GradientDrawable.OVAL
            circleDrawable.setColor(color)
            ivColor.background = circleDrawable;
        }


        return view
    }

    /**
     * Obtiene un miembro específico por su ID desde la base de datos de Firebase.
     * @param miembroId El ID del miembro cuyo nombre se desea obtener.
     * @param callback Una función lambda que recibe el objeto Member (o null si no se encuentra).
     */
    fun obtenerNombreMiembroPorId(miembroId: String, callback: (Member?) -> Unit) {
        val db = FirebaseDatabase.getInstance().reference
        db.child("members").child(miembroId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Intenta convertir el DataSnapshot directamente a la clase Member
                    val miembro = snapshot.getValue(Member::class.java)?.copy(id = miembroId)
                    callback(miembro)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MiembroTareaAdapter", "Error al obtener miembro: $miembroId", error.toException())
                    callback(null)
                }
            })
    }


}




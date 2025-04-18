package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.media.tv.TvInputManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
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
import zamora.jorge.taskfam.data.Task
import zamora.jorge.taskfam.databinding.ActivityTaskDetailBinding

class TaskDetail : AppCompatActivity() {
    // TODO: ATENCIOOOOOOOON
    // TODO: hola chuy
// TODO: Deje una base que es prácticamente el código anterior, pero te dejeré esa chamba, que funcione bien los datos de la pantalla,
    //
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

        val tarea = intent.getParcelableExtra<Task>("TASK")
        val home = intent.getParcelableExtra<Home>("HOME")
        binding.tvTitulotarea.text = tarea?.titulo ?: "Tarea sin título"
        binding.tvDescripcion.text = tarea?.descripcion ?: "Tarea sin descripción"


        //Esta harcodeado el día, pero no debe de ser así
        binding.lvTaskDetail.adapter = TaskDetailAdapter(this, listOf(tarea!!), home!!, dia = "Lunes")



        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


    }


}

class TaskDetailAdapter(
    private val context: Context,
    private val tasks: List<Task>,
    private val home: Home,
    private val dia: String) : BaseAdapter() {
    override fun getCount(): Int = tasks.size
    override fun getItem(position: Int): Any = tasks[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_task_detail, parent, false)

        val task = tasks[position]

        val btnTarea = view.findViewById<ImageView>(R.id.btnCompletarTareaDetail)
        val tvMiembro = view.findViewById<TextView>(R.id.tvMiembroDetail)
        if(home?.editable == true){
            btnTarea.isEnabled = true
            btnTarea.alpha = 1.0f
        }else{

            //Se obtiene el usuario actual de le sesion, para poder tansformar su id
            // En el nombre que sal en cada tarea
            val currentUser = FirebaseAuth.getInstance().currentUser
            val uid = currentUser?.uid
            Log.d("UID", "UID del usuario actual: $uid")

            //valida que sea el mismo usuario que creo la tarea
            if (uid != null && task.assignments.containsKey(uid)) {
                Log.d("TAREAENCONTRADA","Se encontró el mismo uid")
                val diasUsuario = task.assignments[uid]
                val estado = diasUsuario?.get(dia)

                if (estado == true) {
                    Log.d("TAREA_COMPLETADA", "La tarea '${task.titulo}' está completada.")
                    btnTarea.isEnabled = false
                    btnTarea.alpha = 0.0f
                } else {
                    btnTarea.isEnabled = true
                    btnTarea.alpha = 1.0f
                    Log.d("TAREA_NO_COMPLETADA", "La tarea '${task.titulo}' NO está completada.")
                }


            } else {
                btnTarea.isEnabled = false
                btnTarea.alpha = 0.0f
                Log.d("TAREANOENCONTRADA","No se encontró el mismo uid")
            }
        }
        btnTarea.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("¿Completar tarea?")
                .setMessage("¿Deseas marcar esta tarea como completada?")
                .setPositiveButton("Sí") { _, _ ->
                    completarTarea(task, dia)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        val assignedMemberId = task.assignments.entries.find { (_, dias) -> dias.containsKey(dia) }?.key

        if (assignedMemberId != null) {
            obtenerNombreMiembroPorId(assignedMemberId) { nombre ->
                tvMiembro.text = nombre ?: "(Miembro no encontrado)"
            }
        } else {
            tvMiembro.text = "(Sin asignar)"
        }

        return view
    }

    fun completarTarea(tarea: Task, dia: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: return

        val db = FirebaseDatabase.getInstance().reference
        val tareaRef = db.child("tasks").child(tarea.id)

        // Cambiar el estado de la tarea a true para ese usuario y ese día
        tareaRef.child("assignments").child(uid).child(dia).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(context, "Tarea completada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al completar tarea", Toast.LENGTH_SHORT).show()
            }
    }

    fun obtenerNombreMiembroPorId(miembroId: String, callback: (String?) -> Unit) {
        val db = FirebaseDatabase.getInstance().reference
        db.child("members").child(miembroId).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombre = snapshot.getValue(String::class.java)
                    callback(nombre)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FIREBASE", "Error al obtener nombre del miembro", error.toException())
                    callback(null)
                }
            })
    }


}


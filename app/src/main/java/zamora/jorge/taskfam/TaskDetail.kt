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
import zamora.jorge.taskfam.data.Task
import zamora.jorge.taskfam.databinding.ActivityTaskDetailBinding

class TaskDetail : AppCompatActivity() {
    // TODO: ATENCIOOOOOOOON
    // TODO: hola chuy
    // TODO: Deje una base que es prácticamente el código anterior, pero te dejeré esa chamba, que funcione bien los datos de la pantalla,

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

        val miembrosAsignados = tarea?.assignments?.keys?.toList() ?: emptyList()

        //Esta harcodeado el día, pero no debe de ser así
        binding.lvTaskDetail.adapter = MiembroTareaAdapter(this, tarea!!, home!!)

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("HOME", home)
            startActivity(intent)
        }

        binding.editTask.setOnClickListener {
            val intent = Intent(this, AddEdit::class.java)
            intent.putExtra("Home", home)
            intent.putExtra("TASK", tarea)
            intent.putExtra("Accion", "EDITAR")
            startActivity(intent)
        }

    }
}

class MiembroTareaAdapter(
    private val context: Context,
    private val tarea: Task,
    private val home: Home
) : BaseAdapter() {

    private val miembros = tarea.assignments.keys.toList()
    private val nombresMiembros = mutableMapOf<String, String?>()

    init {
        cargarNombresMiembros()
    }

    private fun cargarNombresMiembros() {
        val db = FirebaseDatabase.getInstance().reference
        miembros.forEach { miembroId ->
            db.child("members").child(miembroId).child("name")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        nombresMiembros[miembroId] = snapshot.getValue(String::class.java)
                        notifyDataSetChanged() // <- Esto actualiza la vista cuando llegue el nombre
                    }

                    override fun onCancelled(error: DatabaseError) {
                        nombresMiembros[miembroId] = "(Error)"
                        notifyDataSetChanged()
                    }
                })
        }
    }

    override fun getCount(): Int = miembros.size
    override fun getItem(position: Int): Any = miembros[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_task_detail, parent, false)

        val miembroId = miembros[position]
        val dias = tarea.assignments[miembroId] ?: emptyMap()

        val tvMiembro = view.findViewById<TextView>(R.id.tvMiembroDetail)
        val checkBoxes = mapOf(
            "Lunes" to view.findViewById<CheckBox>(R.id.cbLunes),
            "Martes" to view.findViewById<CheckBox>(R.id.cbMartes),
            "Miércoles" to view.findViewById<CheckBox>(R.id.cbMiercoles),
            "Jueves" to view.findViewById<CheckBox>(R.id.cbJueves),
            "Viernes" to view.findViewById<CheckBox>(R.id.cbViernes),
            "Sábado" to view.findViewById<CheckBox>(R.id.cbSabado),
            "Domingo" to view.findViewById<CheckBox>(R.id.cbDomingo)
        )

        checkBoxes.values.forEach { it.isChecked = false }

        // Marcamos los dias aignados
        checkBoxes.forEach { (diaEsperado, checkBox) ->
            checkBox.isChecked = dias.containsKey(diaEsperado)
        }
        obtenerNombreMiembroPorId(miembroId) { nombre ->
            tvMiembro.text = nombre ?: "(Miembro no encontrado)"
        }

        val btnCompletar =
            view.findViewById<ImageButton>(R.id.btnCompletarTareaDetail) // Nada por q aqui no se usa pero por estetica lo dejo

        return view
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
                    callback(null)
                }
            })
    }


}




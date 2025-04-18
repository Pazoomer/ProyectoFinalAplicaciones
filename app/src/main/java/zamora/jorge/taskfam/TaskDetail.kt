package zamora.jorge.taskfam

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import zamora.jorge.taskfam.data.Task
import zamora.jorge.taskfam.databinding.ActivityLoginBinding
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

        val tarea = intent.getParcelableExtra<Task>("TASK")
        binding.tvTitulotarea.text = tarea?.titulo ?: "Tarea sin título"
        binding.tvDescripcion.text = tarea?.descripcion ?: "Tarea sin descripción"

        //Esto es para que el usuario de su tarea pueda editar las tareas


        val miembroId = tarea?.assignments?.keys?.firstOrNull()
        if (miembroId != null) {
            obtenerNombreMiembroPorId(miembroId) { nombre ->
                binding.tvMiembro.text = nombre ?: "(Miembro no encontrado)"
            }
        } else {
            binding.tvMiembro.text = "(No hay miembro asignado)"
        }

        Log.d("TAREA", tarea.toString())

        binding.tvTitulotarea.text = tarea?.titulo ?: "Tarea sin título"
        binding.tvDescripcion.text = tarea?.descripcion ?: "Tarea sin descripción"

        Log.d("TAREA", tarea.toString())

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
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
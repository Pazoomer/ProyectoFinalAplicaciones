package zamora.jorge.taskfam

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import yuku.ambilwarna.AmbilWarnaDialog
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.databinding.ActivityCreateHomeBinding

class CreateHome : AppCompatActivity() {

    private var defaultColor: Int = Color.WHITE
    private lateinit var binding: ActivityCreateHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        window.statusBarColor = Color.BLACK

        defaultColor = ContextCompat.getColor(this, R.color.blue_brilliant)

        binding = ActivityCreateHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSeleccionColor.setOnClickListener(){
            openColorPicker()
        }

        binding.btnAceptarCrearHogar.setOnClickListener(){
            createHome()
        }

        binding.ivBackArrow.setOnClickListener(){
            val intent = Intent(this, CreateJoinHome::class.java)
            startActivity(intent)
        }

    }

    private fun openColorPicker() {
        val ambilWarnaDialog = AmbilWarnaDialog(this, defaultColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
            }

            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                defaultColor = color
                binding.btnSeleccionColor.setBackgroundColor(defaultColor)
            }
        })
        ambilWarnaDialog.show()
    }

    private fun createHome() {
        val nombre = binding.etNombreHogar.text.toString().trim()
        val color = defaultColor
        val edit = binding.rbEdit.isChecked

        if (nombre.isEmpty()) {
            showError("Por favor ingrese el nombre del hogar")
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            showError("Error: Usuario no autenticado")
            return
        }

        generarCodigoUnico { codigoGenerado ->
            val database = FirebaseDatabase.getInstance().reference.child("homes")
            val homeId = database.push().key

            if (homeId == null) {
                return@generarCodigoUnico
            }

            val nuevaCasa = Home(
                id = homeId,
                nombre = nombre,
                code = codigoGenerado,
                color = color,
                editable = edit,
                members = listOf(userId)
            )

            database.child(homeId).setValue(nuevaCasa)
                .addOnSuccessListener {
                    Toast.makeText(this, "Hogar creado exitosamente", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, CreateJoinHome::class.java))
                }
                .addOnFailureListener { e ->
                    showError("Error al crear el hogar: ${e.message}")
                }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        println("Error: $message")
    }

    fun generarCodigoUnico(callback: (String) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference.child("homes")
        val caracteres = "0123456789"

        fun intentarGenerarCodigo() {
            val codigo = (1..7).map { caracteres.random() }.joinToString("")

            database.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    callback(codigo)
                    return@addOnSuccessListener
                }

                database.orderByChild("code").equalTo(codigo).get()
                    .addOnSuccessListener { result ->
                        if (result.exists()) {
                            intentarGenerarCodigo()
                        } else {
                            callback(codigo)
                        }
                    }
                    .addOnFailureListener { e ->
                        intentarGenerarCodigo()
                    }
            }.addOnFailureListener { e ->
                intentarGenerarCodigo()
            }
        }

        intentarGenerarCodigo()
    }
}
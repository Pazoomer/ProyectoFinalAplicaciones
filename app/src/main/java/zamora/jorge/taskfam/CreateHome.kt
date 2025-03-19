package zamora.jorge.taskfam

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import yuku.ambilwarna.AmbilWarnaDialog
import zamora.jorge.taskfam.databinding.ActivityCreateHomeBinding

class CreateHome : AppCompatActivity() {

    private var defaultColor: Int = Color.WHITE
    private lateinit var binding: ActivityCreateHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
            val intent = Intent(this, CrearUnirseHogar::class.java)
            startActivity(intent)
        }

    }

    private fun openColorPicker() {
        val ambilWarnaDialog = AmbilWarnaDialog(this, defaultColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
                // No hacer nada en caso de cancelar
            }

            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                defaultColor = color
                binding.btnSeleccionColor.setBackgroundColor(defaultColor)
            }
        })
        ambilWarnaDialog.show()
    }

    private fun createHome(){
        //Obtener datos
        val nombre=binding.etNombreHogar.text
        val color=binding.btnSeleccionColor.solidColor
        val edit=binding.rbEdit.isSelected

        // Validar campos vacíos
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese el nombre del hogar", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("NOMBRE", nombre.toString())
        intent.putExtra("COLOR", color)
        intent.putExtra("EDIT", edit)
        startActivity(intent)
    }

}
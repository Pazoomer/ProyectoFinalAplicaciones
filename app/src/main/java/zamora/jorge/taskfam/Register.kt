package zamora.jorge.taskfam

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Toast
import zamora.jorge.taskfam.databinding.ActivityRegisterBinding
import java.security.Principal

class Register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvYaTienesCuenta.setOnClickListener {
            yaTienesCuenta()
        }

        binding.btnRegistrarse.setOnClickListener {
            registrarse()
        }
    }

    fun yaTienesCuenta() {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
    }

    fun registrarse() {
        //Obtener datos
        val nombre=binding.etNombre
        val correo=binding.etCorreo
        val contrasena=binding.etContrasena
        val confirmarContrasena=binding.etConfirmarContrasena

        //Validar datos
        if(nombre.text.toString().isEmpty() || correo.text.toString().isEmpty() || contrasena.text.toString().isEmpty() || confirmarContrasena.text.toString().isEmpty()) {
            Toast.makeText(this, "Por favor ingrese sus datos", Toast.LENGTH_SHORT).show()
            return
        }

        //TODO: Revisar credenciales repetidas en la base de datos y registrarlo si no es asi

        //Cambiar actividad
        val intent = Intent(this, CrearUnirseHogar::class.java)
        intent.putExtra("correo", correo.text.toString())
        startActivity(intent)
    }
}
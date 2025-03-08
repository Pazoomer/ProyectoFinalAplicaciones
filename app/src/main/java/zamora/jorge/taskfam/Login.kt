package zamora.jorge.taskfam

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import zamora.jorge.taskfam.databinding.ActivityLoginBinding

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvNoTienesCuenta.setOnClickListener {
            noTienesCuenta()
        }

        binding.btnIniciarSesion.setOnClickListener {
            iniciarSesion()
        }
    }

    fun noTienesCuenta() {
        val intent = Intent(this, Register::class.java)
        startActivity(intent)
    }

    fun iniciarSesion() {
        //Obtener datos
        val correo=binding.etCorreo
        val contrasena=binding.etContrasena

        // Validar campos vacíos
        if ( correo.text.isEmpty() || contrasena.text.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese todos sus datos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar correo
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo.text).matches()) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        //TODO: Revisar credenciales en la base de datos

        //Cambiar actividad
        val intent = Intent(this, CrearUnirseHogar::class.java)
        intent.putExtra("correo", correo.text.toString())
        startActivity(intent)
    }
}
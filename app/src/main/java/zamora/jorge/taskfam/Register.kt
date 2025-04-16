package zamora.jorge.taskfam

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import zamora.jorge.taskfam.databinding.ActivityRegisterBinding
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import zamora.jorge.taskfam.data.Member

class Register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        window.statusBarColor = Color.BLACK

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvYaTienesCuenta.setOnClickListener {
            yaTienesCuenta()
        }

        binding.btnRegistrarse.setOnClickListener {
            registrarse()
        }


        auth = Firebase.auth
        val nombre: EditText =findViewById(R.id.etNombre)
        val email: EditText =findViewById(R.id.etCorreo)
        val password: EditText =findViewById(R.id.etContrasena)
        val confirmPassword: EditText =findViewById(R.id.etConfirmarContrasena)
        val error: TextView= findViewById(R.id.tvError)
        val button: Button =findViewById(R.id.btnRegistrarse)

        error.visibility= View.INVISIBLE

        button.setOnClickListener{
            if(email.text.isEmpty()||password.text.isEmpty()||confirmPassword.text.isEmpty()){
                error.text="Por favor ingrese todos los campos"
                error.visibility= View.VISIBLE
            }else if(password.text.toString()!=confirmPassword.text.toString()){
                error.text="Las contraseñas no coinciden"
                error.visibility= View.VISIBLE
            }else{
                sigIn(email.text.toString(),password.text.toString(), nombre.text.toString(), error)
            }
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

        // Validar campos vacíos
        if (nombre.text.isEmpty() || correo.text.isEmpty() || contrasena.text.isEmpty() || confirmarContrasena.text.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese todos sus datos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar nombre (máximo 12 caracteres,solo letras y numeros)
        val nombreRegex = Regex("^[a-zA-Z0-9\\s]{1,12}\$")
        if (!nombre.text.matches(nombreRegex) || nombre.text.isBlank()) {
            Toast.makeText(this, "Nombre inválido: máximo 12 caracteres, solo letras y numeros", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar correo
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo.text).matches()) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar contraseñas iguales
        if (contrasena != confirmarContrasena) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        //TODO: Revisar credenciales repetidas en la base de datos y registrarlo si no es asi

        //Cambiar actividad
        val intent = Intent(this, CreateJoinHome::class.java)
        intent.putExtra("correo", correo.text.toString())
        startActivity(intent)
    }

    private fun sigIn(email: String, password: String, name: String, error: TextView) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.let {
                    val uid = user.uid
                    val member = Member(id = uid, name = name, email = email)

                    val database = FirebaseDatabase.getInstance().reference
                    database.child("members").child(uid).setValue(member)
                        .addOnSuccessListener {
                            val intent = Intent(this, CreateJoinHome::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .addOnFailureListener {
                            error.text = "Error al guardar los datos"
                            error.visibility = View.VISIBLE
                        }
                }
            } else {
                error.text = "Ingrese datos válidos"
                error.visibility = View.VISIBLE
            }
        }
    }

}
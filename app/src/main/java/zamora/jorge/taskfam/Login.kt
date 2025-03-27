package zamora.jorge.taskfam

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import zamora.jorge.taskfam.databinding.ActivityLoginBinding

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        window.statusBarColor = Color.BLACK

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvNoTienesCuenta.setOnClickListener {
            noTienesCuenta()
        }

        binding.btnIniciarSesion.setOnClickListener {
            //iniciarSesion()
        }
        auth = Firebase.auth
        val email: EditText =findViewById(R.id.etCorreo)
        val password: EditText =findViewById(R.id.etContrasena)
        val error: TextView =findViewById(R.id.tvError)
        val buttonLogin: Button =findViewById(R.id.btnIniciarSesion)
        //val buttonRegister: TextView =findViewById(R.id.tvNoTienesCuenta)

        error.visibility= View.INVISIBLE

        buttonLogin.setOnClickListener{
            if(email.text.isEmpty()||password.text.isEmpty()){
                error.text="Por favor ingrese todos los campos"
                error.visibility=View.VISIBLE
            }else{
                error.visibility=View.INVISIBLE
                login(email.text.toString(),password.text.toString())
            }
        }
        /*
        buttonRegister.setOnClickListener{
            val intent: Intent = Intent(this,SignInActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }*/
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
        val intent = Intent(this, CreateJoinHome::class.java)
        intent.putExtra("correo", correo.text.toString())
        startActivity(intent)
    }

    public override fun onStart()
    {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
            goToMain(currentUser)
        }
    }

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    showError(visible = false)
                    goToMain(user!!)
                }
                else if (password.toString().length<6){
                    showError(text = "La contraseña debe tener al menos 6 caracteres", visible = true)
                }
                else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    showError(text = "Correo o contraseña incorrectas", visible = true)
                }
            }
    }

    private fun goToMain(user: FirebaseUser){
        val intent: Intent = Intent(this,CreateJoinHome::class.java)
        intent.putExtra("user",user.email)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun showError(text: String="", visible: Boolean){
        val error: TextView =findViewById(R.id.tvError)



        error.text=text

        error.visibility= if(visible) View.VISIBLE else View.INVISIBLE
    }
}
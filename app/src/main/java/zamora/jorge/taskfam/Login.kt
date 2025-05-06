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

        // Maneja el registro
        binding.tvNoTienesCuenta.setOnClickListener {
            noTienesCuenta()
        }

        // Maneja el inicio de sesion
        binding.btnIniciarSesion.setOnClickListener {
            iniciarSesion()
        }

        // Instancia de servicio de autenticacion de firebase
        auth = Firebase.auth

        // Instancia de elemenos visuales
        val email: EditText =findViewById(R.id.etCorreo)
        val password: EditText =findViewById(R.id.etContrasena)
        val error: TextView =findViewById(R.id.tvError)
        val buttonLogin: Button =findViewById(R.id.btnIniciarSesion)
        val buttonRegister: TextView =findViewById(R.id.tvNoTienesCuenta)

        error.visibility= View.INVISIBLE

        // Manejamos el boton de registro, si hay un erro lo notifica en el TextView Error
        buttonLogin.setOnClickListener{
            if(email.text.isEmpty()||password.text.isEmpty()){
                error.text="Por favor ingrese todos los campos"
                error.visibility=View.VISIBLE
            }else{
                error.visibility=View.INVISIBLE
                // Se realiza la logica de inicio de sesion
                login(email.text.toString(),password.text.toString())
            }
        }

        // Nos envia a la actividad de Registro
        buttonRegister.setOnClickListener{
            val intent: Intent = Intent(this,Register::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    /**
     * Navega a la actividad de registro.
     */
    fun noTienesCuenta() {
        val intent = Intent(this, Register::class.java)
        startActivity(intent)
    }

    /**
     * Intenta iniciar sesión con el correo electrónico y la contraseña proporcionados.
     * Realiza validaciones básicas en los campos antes de intentar el inicio de sesión.
     * Después del inicio de sesión exitoso, navega a la actividad CreateJoinHome.
     */
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

        //Cambiar actividad
        val intent = Intent(this, CreateJoinHome::class.java)
        intent.putExtra("correo", correo.text.toString())
        startActivity(intent)
    }

    /**
     * Se llama cuando la actividad se vuelve visible para el usuario.
     * Verifica si hay un usuario actualmente autenticado y, si lo hay,
     * navega directamente a la actividad CreateJoinHome.
     */
    public override fun onStart()
    {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
            goToMain(currentUser)
        }
    }

    /**
     * Intenta iniciar sesión en Firebase Authentication con el correo electrónico y la contraseña proporcionados.
     * Si el inicio de sesión es exitoso, llama a `goToMain` para navegar a la siguiente actividad.
     * Si falla, muestra un mensaje de error en el TextView.
     *
     * @param email El correo electrónico del usuario.
     * @param password La contraseña del usuario.
     */
    fun login(email: String, password: String) {
        // Autenticamos al usuario con sus credenciales
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso, obtener el usuario actual y navegar a la siguiente actividad
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

    /**
     * Navega a la actividad CreateJoinHome después de un inicio de sesión exitoso.
     * Pasa el correo electrónico del usuario como un extra al Intent.
     * Limpia la pila de actividades para evitar volver a la actividad de inicio de sesión con el botón de retroceso.
     *
     * @param user El objeto FirebaseUser del usuario autenticado.
     */
    private fun goToMain(user: FirebaseUser){
        val intent: Intent = Intent(this,CreateJoinHome::class.java)
        intent.putExtra("user",user.email)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Muestra u oculta el TextView de error con el mensaje proporcionado.
     *
     * @param text El texto del error a mostrar. Si está vacío, se usará el texto actual.
     * @param visible Booleano que indica si el TextView de error debe ser visible (true) u oculto (false).
     */
    private fun showError(text: String="", visible: Boolean){
        val error: TextView =findViewById(R.id.tvError)
        error.text=text
        error.visibility= if(visible) View.VISIBLE else View.INVISIBLE
    }
}
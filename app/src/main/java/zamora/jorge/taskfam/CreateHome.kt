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

        // Se maneja la seleccion de color del hogar
        binding.btnSeleccionColor.setOnClickListener(){
            openColorPicker()
        }

        // Se maneja el crear un hogar
        binding.btnAceptarCrearHogar.setOnClickListener(){
            createHome()
        }

        // Se vuelve a la actividad anterior
        binding.ivBackArrow.setOnClickListener(){
            val intent = Intent(this, CreateJoinHome::class.java)
            startActivity(intent)
        }

    }

    /**
     * Abre el diálogo de selección de color AmbilWarna para permitir al usuario elegir un color para el hogar.
     * Actualiza el color predeterminado y el color de fondo del botón de selección de color.
     */
    private fun openColorPicker() {
        val ambilWarnaDialog = AmbilWarnaDialog(this, defaultColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
            }

            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                // Actualiza el color predeterminado con el color seleccionado.
                defaultColor = color
                // Establece el color de fondo del botón con el color seleccionado.
                binding.btnSeleccionColor.setBackgroundColor(defaultColor)
            }
        })
        ambilWarnaDialog.show() // Lo muestra
    }

    /**
     * Crea un nuevo hogar con el nombre, color y configuración de edición proporcionados por el usuario.
     * Genera un código único para el hogar y lo guarda en la base de datos de Firebase.
     * El usuario actual se establece como administrador y primer miembro del hogar.
     */
    private fun createHome() {
        // Obtiene los valores con binding
        val nombre = binding.etNombreHogar.text.toString().trim()
        val color = defaultColor
        val edit = binding.rbEdit.isChecked

        // Verificamos que el nombre no este vacio
        if (nombre.isEmpty()) {
            showError("Por favor ingrese el nombre del hogar")
            return
        }

        // Verificamos que le usuario este autenticado
        val userId = auth.currentUser?.uid ?: run {
            showError("Error: Usuario no autenticado")
            return
        }

        // Generamos un codigo unico para el hogar
        generarCodigoUnico { codigoGenerado ->
            // Instancia de la bd
            val database = FirebaseDatabase.getInstance().reference.child("homes")
            // Genera una nueva clave única para el nuevo hogar
            val homeId = database.push().key

            // Si no se puede generar una clave única, termina la función.
            if (homeId == null) {
                return@generarCodigoUnico
            }

            // Crea un nuevo objeto Home con la información recopilada
            val nuevaCasa = Home(
                id = homeId,
                nombre = nombre,
                code = codigoGenerado,
                color = color,
                editable = edit,
                adminId = userId,
                members = listOf(userId),
                adminsId = listOf(userId)
            )

            // Guarda el nuevo hogar en la bd
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

    /**
     * Muestra un Toast con el mensaje de error proporcionado y loguea el error en la consola.
     * @param message El mensaje de error a mostrar.
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        println("Error: $message")
    }

    /**
     * Genera un código único de 7 caracteres alfanuméricos para el hogar.
     * Asegura que el código generado no exista ya en la base de datos.
     * @param callback Una función lambda que recibe el código único generado como parámetro.
     */
    fun generarCodigoUnico(callback: (String) -> Unit) {
        // Referencia de la bd, enfocada en hogares
        val database = FirebaseDatabase.getInstance().reference.child("homes")
        val caracteres = "0123456789"

        /**
         * Intenta generar un código único de forma recursiva.
         */
        fun intentarGenerarCodigo() {
            val codigo = (1..7).map { caracteres.random() }.joinToString("")

            // Verifica si la tabla homes existe en la bd
            database.get().addOnSuccessListener { snapshot ->
                // Si la tabla no existe, el código generado es único
                if (!snapshot.exists()) {
                    callback(codigo)
                    return@addOnSuccessListener
                }

                // Busca si ya existe un hogar con el código generado.
                database.orderByChild("code").equalTo(codigo).get()
                    .addOnSuccessListener { result ->
                        // Si ya existe un hogar con este código, intenta generar uno nuevo.
                        if (result.exists()) {
                            intentarGenerarCodigo()
                        } else {
                            // Si el código no existe, llama al callback con el código generado.
                            callback(codigo)
                        }
                    }
                    .addOnFailureListener { e ->
                        // Si hay un error en la consulta, intenta generar un nuevo código.
                        intentarGenerarCodigo()
                    }
            }.addOnFailureListener { e ->
                // Si hay un error al verificar la existencia de la tabla, intenta generar un nuevo código.
                intentarGenerarCodigo()
            }
        }

        // Inicia el proceso de generación del código único.
        intentarGenerarCodigo()
    }
}
package zamora.jorge.taskfam

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import zamora.jorge.taskfam.databinding.ActivityJoinHouseBinding

private lateinit var binding: ActivityJoinHouseBinding

class JoinHouse : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        binding = ActivityJoinHouseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener(){
            startActivity(Intent(this, CreateJoinHome::class.java))
        }

        binding.btnAdd.setOnClickListener(){
            joinHouse()
        }
    }

    fun joinHouse(){
        //Obtener datos
        val codigo=binding.inputCode

        // Validar campos vacíos
        if ( codigo.text.isEmpty()) {
            Toast.makeText(this, "Ingrese el codigo del hogar", Toast.LENGTH_SHORT).show()
            return
        }

        //TODO: BUSCAR EN LA BASE DE DATOS UN HOGAR CON ESE CODIGO

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("CODIGO", codigo.text.toString())
        startActivity(intent)
    }
}
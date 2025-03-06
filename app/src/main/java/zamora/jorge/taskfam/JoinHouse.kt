package zamora.jorge.taskfam

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import zamora.jorge.taskfam.databinding.ActivityCrearUnirseHogarBinding
import zamora.jorge.taskfam.databinding.ActivityJoinHouseBinding

private lateinit var binding: ActivityJoinHouseBinding

class JoinHouse : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        binding = ActivityJoinHouseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener(){
            startActivity(Intent(this, CrearUnirseHogar::class.java))
        }

        binding.btnAdd.setOnClickListener(){
            if (validarCampo()){
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }

    fun validarCampo(): Boolean{

        return !binding.inputCode.text.toString().isBlank()

    }
}
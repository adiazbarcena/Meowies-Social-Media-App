package com.example.meowiesproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;


public class MainActivity extends AppCompatActivity {
    //Declaramos las variables
    Button loginBoton,
            registrarBoton;
    EditText usuarioEditText,
            contraEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Inicializamos los componentes de la interfaz
        loginBoton = findViewById(R.id.loginBoton);
        usuarioEditText = findViewById(R.id.editTextUsuario);
        contraEditText = findViewById(R.id.editTextContra);
        registrarBoton = findViewById(R.id.registrarBoton);

        //Configuramos OnClickListener para el botón de inicio de sesión
        loginBoton.setOnClickListener(v -> {
            String usuario = usuarioEditText.getText().toString();
            String contra = contraEditText.getText().toString();
            signIn(usuario, contra);
        });

        //Configuramos OnClickListener para el botón de registro
        registrarBoton.setOnClickListener(v -> {
            // Crear un Intent para iniciar la nueva actividad de registro
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });
    }

    //Método para iniciar sesión usando un correo electrónico o un nickname
    private void signIn(String usernameOrEmail, String password) {
        //Ciframos la contraseña ingresada
        String contraEncriptada = Seguridad.cifrarContraseña(password);

        //Verificamos si el valor ingresado es un correo electrónico
        if (Patterns.EMAIL_ADDRESS.matcher(usernameOrEmail).matches()) {
            // Si es un correo electrónico, iniciar sesión directamente
            signInWithEmail(usernameOrEmail, contraEncriptada);
        } else {
            //Si es un nickname, consultamos la base de datos para obtener el correo electrónico
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Query nicknameQuery = db.collection("usuarios").whereEqualTo("nickname", usernameOrEmail);
            nicknameQuery.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    String email = task.getResult().getDocuments().get(0).getString("e-mail");
                    //Iniciamos sesión con el correo electrónico obtenido
                    signInWithEmail(email, contraEncriptada);
                } else {
                    //Si no se encuentra el alias, mostramos un mensaje de error
                    Toast.makeText(MainActivity.this, "Usuario no encontrado. Verifica el nickname ingresado.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    //Método proporcionado por Firebase para iniciar sesión usando un correo electrónico y una contraseña
    private void signInWithEmail(String email, String password) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(MainActivity.this, FeedActivity.class));
                    } else {
                        //Si el inicio de sesión falla, mostramos un mensaje de error
                        Toast.makeText(MainActivity.this, "¡Parece que hay algún error! Revisa la información.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

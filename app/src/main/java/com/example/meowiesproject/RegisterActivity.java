package com.example.meowiesproject;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    //Declaramos las variables necesarias
    private FirebaseAuth mAuth;

    EditText editTextFechaNacimiento, editTextNombre,
            editTextApellidos, editTextNickname,
            editTextContraseña, verificacionContra;

    Button registrarBoton;
    CheckBox checkBox;
    TextView textViewTerms;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        //Buscamos todos nuestros items
        editTextFechaNacimiento = findViewById(R.id.editTextFechaNacimiento);
        editTextNombre = findViewById(R.id.editTextNombre);
        editTextApellidos = findViewById(R.id.editTextApellidos);
        editTextNickname = findViewById(R.id.editTextNickname);
        editTextContraseña = findViewById(R.id.editTextContra);
        registrarBoton = findViewById(R.id.loginBoton);
        verificacionContra = findViewById(R.id.editTextContra2);
        checkBox = findViewById(R.id.checkBox);

        //Configuramos el listener para mostrar el DatePicker cuando se hace clic en el campo de fecha
        editTextFechaNacimiento.setOnClickListener(v -> mostrarDatePicker());

        //Configuramos el listener para mostrar el diálogo de términos y condiciones cuando se marca el CheckBox
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showTermsDialog();
            }
        });

        //Configuramos el listener para el botón de registro
        registrarBoton.setOnClickListener(v -> {
            //Obtenemos los datos de los campos de entrada justo antes de registrar
            String email = editTextNombre.getText().toString();
            String apellidos = editTextApellidos.getText().toString();
            String nickname = editTextNickname.getText().toString();
            String fechaNacimiento = editTextFechaNacimiento.getText().toString();
            String contraseña = editTextContraseña.getText().toString();
            String verificacion = verificacionContra.getText().toString();

            //Validamos los campos antes de proceder con el registro
            if (!Seguridad.validarEmail(RegisterActivity.this, email)
                    || !Seguridad.validarContra(RegisterActivity.this, contraseña)
                    || !Seguridad.matchingPasswords(RegisterActivity.this,contraseña, verificacion)
                    || !Seguridad.validarNickname(RegisterActivity.this, nickname)) {
                return;
            }
            //Verificamos si el CheckBox de términos y condiciones está marcado
            if (!checkBox.isChecked()) {
                Toast.makeText(RegisterActivity.this, "Debe aceptar los términos y condiciones para registrarse.", Toast.LENGTH_SHORT).show();
                return;
            }

            //Verificamos si el email o el nickname ya existen en la base de datos
            checkIfEmailOrNicknameExists(email, nickname, () -> createAccount(email, apellidos, nickname, fechaNacimiento, contraseña));
        });
    }

    //Método para mostrar el DatePickerDialog
    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(RegisterActivity.this,
                (view, year1, month1, dayOfMonth1) -> {
                    //Establece la fecha seleccionada en el EditText
                    String selectedDate = dayOfMonth1 + "/" + (month1 + 1) + "/" + year1;
                    editTextFechaNacimiento.setText(selectedDate);
                }, year, month, dayOfMonth);

        // çEstablece el año máximo y mínimo para permitir que el usuario navegue
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis()); //Establece la fecha máxima como la fecha actual
        datePickerDialog.getDatePicker().setSpinnersShown(true); //Muestra los spinners de año y mes
        datePickerDialog.show();
    }


    //Método para verificar si el email o nickname ya existen en la base de datos
    private void checkIfEmailOrNicknameExists(String email, String nickname, Runnable onSuccess) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        //Consultamos si el email ya existe
        Query emailQuery = db.collection("usuarios").whereEqualTo("e-mail", email);
        emailQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                Toast.makeText(RegisterActivity.this, "El correo electrónico ya está registrado.", Toast.LENGTH_SHORT).show();
            } else {
                //Consultamos si el nickname ya existe
                Query nicknameQuery = db.collection("usuarios").whereEqualTo("nickname", nickname);
                nicknameQuery.get().addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful() && !task2.getResult().isEmpty()) {
                        Toast.makeText(RegisterActivity.this, "El nickname ya está registrado.", Toast.LENGTH_SHORT).show();
                    } else {
                        //Ambos son únicos así que registramos
                        onSuccess.run();
                    }
                });
            }
        });
    }

    //Método proporcionado por Firebase para la creación de una cuenta
    protected void createAccount(String email, String apellidos, String nickname, String fechaNacimiento, String contraseña) {
        mAuth = FirebaseAuth.getInstance();
        //Ciframos la contraseña
        String contraseñaCifrada = Seguridad.cifrarContraseña(contraseña);
        mAuth.createUserWithEmailAndPassword(email, contraseñaCifrada)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        //Creamos un mapa para almacenar los datos del usuario
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("e-mail", email);
                        userData.put("nombre_apellidos", apellidos);
                        userData.put("nickname", nickname);
                        userData.put("fecha_nacimiento", fechaNacimiento);
                        userData.put("contraseña", contraseñaCifrada);

                        //Agregamos los datos del usuario a la colección "usuarios" usando el UID del usuario como ID de documento
                        db.collection("usuarios").document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    String frase = "¡Te has registrado correctamente!¡Ya eres parte de Meowies!";
                                    // Show a Toast or perform any other action with the frase variable
                                    Toast.makeText(RegisterActivity.this, frase, Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(e -> {
                                    String frase = "Error al registrarse.";
                                    //Mostramos un Toast para indicar que ha habido un error de registro
                                    Toast.makeText(RegisterActivity.this, frase, Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(RegisterActivity.this, "¡Parece que hay algún error! Revisa la información. " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Método para mostrar un pop up de los términos y condiciones
    private void showTermsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Términos y Condiciones");

        //Obtenemos el texto de los términos y condiciones
        String termsConditions = getString(R.string.terms_conditions);

        //Usamos Html.fromHtml() para convertir el HTML en texto con formato
        builder.setMessage(Html.fromHtml(termsConditions, Html.FROM_HTML_MODE_LEGACY));

        //Configuramos el botón "Aceptar" para marcar el CheckBox
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkBox.setChecked(true);
            }
        });

        //Configuramos el botón "Cancelar" para desmarcar el CheckBox y cerrar el diálogo
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkBox.setChecked(false);
                dialog.dismiss();
            }
        });
        //Creamos y mostramos el AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}

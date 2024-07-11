package com.example.meowiesproject;

import android.content.Context;
import android.util.Patterns;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Seguridad {

    public static boolean validarEmail(Context context, String email) {
        //Validamos el formato del correo electrónico
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Formato de correo electrónico inválido.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static boolean validarContra(Context context, String contraseña) {
        //Validamos la seguridad de la contraseña
        if (!contraseña.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$")) {
            Toast.makeText(context, "La contraseña no es segura. Debe tener al menos 6 caracteres, una letra mayúscula, una letra minúscula, un número y un carácter especial.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public static boolean matchingPasswords(Context context, String contraseña, String verificacion) {
        //Verificamos que las contraseñas coincidan
        if (!contraseña.equals(verificacion)) {
            Toast.makeText(context, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static boolean validarNickname(Context context, String nickname) {
        //Verificamos que el nickname solo contenga letras y números
        if (!nickname.matches("[a-zA-Z0-9]+")) {
            Toast.makeText(context, "El nickname solo puede contener letras y números.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static String cifrarContraseña(String contra) {
        try {
            //Obtenemos la instancia de MessageDigest para SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //Aplicamos el algoritmo de hash a la contraseña
            byte[] hash = digest.digest(contra.getBytes(StandardCharsets.UTF_8));
            //Convertimos el hash de bytes a una representación hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}


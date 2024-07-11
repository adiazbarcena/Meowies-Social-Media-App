package com.example.meowiesproject;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class UploadFileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    StorageReference storageReference;
    LinearProgressIndicator barraProgreso;
    Uri video;
    Button seleccionarVideo, subirVideo;
    ImageView previewVideo, casaBarraNav, camaraBarraNav, perfilBarraNav;
    EditText titulo, descripcion;
    String tituloContent, descripcionContent, videoURL;

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                if (result.getData() != null) {
                    subirVideo.setEnabled(true);
                    video = result.getData().getData();
                    Glide.with(UploadFileActivity.this).load(video).into(previewVideo);
                }
            } else {
                Toast.makeText(UploadFileActivity.this, "Parece que se te ha olvidado seleccionar un vídeo, ¡Selecciona uno!", Toast.LENGTH_SHORT).show();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_file);

        FirebaseApp.initializeApp(this);
        storageReference = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String currentUserUid = currentUser.getUid().toString();
        //Buscar nuestros objetos
        previewVideo = findViewById(R.id.previewVideo);
        barraProgreso = findViewById(R.id.progreso);
        seleccionarVideo = findViewById(R.id.seleccionarVideoBoton);
        subirVideo = findViewById(R.id.subirButton);
        titulo = findViewById(R.id.tituloEditText);
        descripcion = findViewById(R.id.descripcionEditText);
        casaBarraNav = findViewById(R.id.casaBarraNav);
        camaraBarraNav = findViewById(R.id.camaraBarraNav);
        perfilBarraNav = findViewById(R.id.perfilBarraNav);

        //Configuramos el click listener para seleccionar un video
        seleccionarVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                Intent chooser = Intent.createChooser(intent, "Selecciona un video");
                if (chooser.resolveActivity(getPackageManager()) != null) {
                    activityResultLauncher.launch(chooser);
                } else {
                    //Manejamos el caso de que no haya aplicaciones que tengan el tipo de contenido permitido
                    Toast.makeText(getApplicationContext(), "No hay aplicaciones disponibles para elegir vídeos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Configuracmos el click listener para subir el video seleccionado
        subirVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Verificación añadida para comprobar si el usuario ha seleccionado un video
                if (video == null) {
                    Toast.makeText(UploadFileActivity.this, "¡Uy, primero necesitas seleccionar un vídeo para subirlo!", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    subirVideo(video);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        casaBarraNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Iniciamos la actividad FeedActivity
                Intent intent = new Intent(UploadFileActivity.this, FeedActivity.class);
                startActivity(intent);
            }
        });

        //Configuramos los click listener para los íconos de la barra de navegación
        camaraBarraNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UploadFileActivity.this, UploadFileActivity.class));
            }
        });

        perfilBarraNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onProfileClick(currentUserUid);
            }
        });
    }

    //Método para subir un video al almacenamiento Firebase Storage
    private void subirVideo(Uri uri) throws IOException {
        //Referencia a Firebase Storage donde se subirá el video con un nombre único
        StorageReference reference = storageReference.child("videos/" + UUID.randomUUID().toString());

        //Obtenemos la duración del video en formato adecuado
        MediaPlayer mp = MediaPlayer.create(this, uri);
        int duration = mp.getDuration();
        mp.release();
        String duracionFormato = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
        //Subimos el video
        reference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //Obtenemos la URL del vídeo subido
                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        videoURL = uri.toString(); // URL del vídeo subido
                        FirebaseUser user = mAuth.getCurrentUser();
                        //Obtenemos la fecha y hora actual
                        Date currentTime = Calendar.getInstance().getTime();
                        tituloContent = titulo.getText().toString();
                        descripcionContent = descripcion.getText().toString();

                        //Creamos mapa para almacenar los datos del video
                        Map<String, Object> videoData = new HashMap<>();
                        videoData.put("usuario_id", user.getUid());
                        videoData.put("video_id", reference.getName()); // ID del video
                        videoData.put("duracion", duracionFormato);
                        videoData.put("fecha_subida", currentTime.toString()); // Fecha de subida
                        videoData.put("titulo", tituloContent);
                        videoData.put("descripcion", descripcionContent);
                        videoData.put("video_url", videoURL); // URL del vídeo

                        //Obtenemos una referencia a la colección 'videos' en la base de datos
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        CollectionReference coleccionVideos = db.collection("videos");

                        //Verificamos si el usuario ya tiene una colección de videos
                        coleccionVideos.document(user.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.exists()) {
                                    //Si el usuario ya tiene una colección de videos, añadimos el nuevo video
                                    coleccionVideos.document(user.getUid()).update(reference.getName(), videoData);
                                } else {
                                    //Si el usuario no tiene una colección de videos, creamos y añadimos el video
                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put(reference.getName(), videoData);
                                    coleccionVideos.document(user.getUid()).set(userData);
                                }
                            }
                        });
                        Toast.makeText(UploadFileActivity.this, "¡Tú vídeo ya está de camino a la nube de Meowies!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UploadFileActivity.this, "Vaya...parece que ha habido un error al subir el vídeo.", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                barraProgreso.setVisibility(View.VISIBLE);
                barraProgreso.setMax(Math.toIntExact(snapshot.getTotalByteCount()));
                barraProgreso.setProgress(Math.toIntExact(snapshot.getBytesTransferred()));
            }
        });
    }

    //Método para manejar clics en el perfil del usuario
    public void onProfileClick(String userId) {
        Intent intent = new Intent(UploadFileActivity.this, ProfileActivity.class);
        intent.putExtra("usuarioId", userId);
        startActivity(intent);
    }
}
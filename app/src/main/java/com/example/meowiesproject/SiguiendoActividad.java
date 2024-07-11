package com.example.meowiesproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SiguiendoActividad extends AppCompatActivity implements VideoAdapterActivity.OnProfileClickListener {
    //Declaramos las variables

    ViewPager2 viewPager;
    VideoAdapterActivity adaptadorVideo;
    ImageView casaBarraNav, camaraBarraNav, perfilBarraNav;
    TextView siguiendoBarraNav, paraTiBarraNav;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        //Inicializamos FirebaseAuth para obtener el usuario actual
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        String usuarioActualUid = usuarioActual.getUid();

        //Inicializamos los componentes de la interfaz
        viewPager = findViewById(R.id.MainViewPager);
        casaBarraNav = findViewById(R.id.casaBarraNav);
        camaraBarraNav = findViewById(R.id.camaraBarraNav);
        perfilBarraNav = findViewById(R.id.perfilBarraNav);
        siguiendoBarraNav = findViewById(R.id.siguiendoBarraNav);
        paraTiBarraNav = findViewById(R.id.paraTiBarraNav);

        //Iniciamos la actividad FeedActivity al hacer clic sobre la casa en la barra de navegación
        casaBarraNav.setOnClickListener(v -> {
            Intent intent = new Intent(SiguiendoActividad.this, FeedActivity.class);
            startActivity(intent);
        });

        //Iniciamos la actividad UploadActivity al hacer clic sobre la cámara en la barra de navegación
        camaraBarraNav.setOnClickListener(v -> startActivity(new Intent(SiguiendoActividad.this, UploadFileActivity.class)));

        perfilBarraNav.setOnClickListener(v -> onProfileClick(usuarioActualUid));

        //Iniciamos la actividad Siguiendo al hacer clic sobre "siguiendo" en la barra de navegación
        siguiendoBarraNav.setOnClickListener(v -> {
            Intent intent = new Intent(SiguiendoActividad.this, SiguiendoActividad.class);
            startActivity(intent);
        });

        //Iniciamos la actividad FeedActivity al hacer clic sobre "para ti" en la barra de navegación
        paraTiBarraNav.setOnClickListener(v -> {
            // Iniciar la actividad FeedActivity
            Intent intent = new Intent(SiguiendoActividad.this, FeedActivity.class);
            startActivity(intent);
        });

        siguiendoVideosUsuarios(usuarioActualUid);
    }

    //Método para manejar clics en el perfil del usuario
    public void onProfileClick(String userId) {
        Intent intent = new Intent(SiguiendoActividad.this, ProfileActivity.class);
        intent.putExtra("usuarioId", userId);
        startActivity(intent);
    }

    //Método para obtener y mostrar los vídeos de los usuarios seguidos
    public void siguiendoVideosUsuarios(String currentUserUid) {

        //Referencias a las colecciones de Firebase
        CollectionReference followingRef = db.collection("usuarios").document(currentUserUid).collection("siguiendo");
        CollectionReference videosRef = db.collection("videos");

        //Obtenemos la lista de usuarios seguidos del usuario actual
        followingRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                List<String> videoIds = new ArrayList<>();

                //Iteramos sobre los documentos de los usuarios seguidos
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    String usuarioIdSiguiendo = document.getId();
                    Log.e("TAG", "ID USUARIOS " + usuarioIdSiguiendo);

                    //Obtenemos el documento de vídeos del usuario seguido
                    videosRef.document(usuarioIdSiguiendo).get().addOnSuccessListener(videoDocumentSnapshot -> {
                        if (videoDocumentSnapshot.exists()) {
                            Log.e("TAG", "Entra");

                            //Obtenemos los IDs de los vídeos del usuario seguido
                            Map<String, Object> videoData = videoDocumentSnapshot.getData();
                            if (videoData != null) {
                                List<String> videosIdsSiguiendo = new ArrayList<>(videoData.keySet());
                                videoIds.addAll(videosIdsSiguiendo);
                            } else {
                                Log.e("TAG", "No hay datos en el documento de video para el usuario seguido: " + usuarioIdSiguiendo);
                            }
                        } else {
                            Log.e("TAG", "No existe el documento de vídeos para el usuario seguido: " + usuarioIdSiguiendo);
                        }

                        //Creamos y configuramos el adaptador solo después de obtener todos los IDs de video
                        adaptadorVideo = new VideoAdapterActivity(SiguiendoActividad.this, videoIds, SiguiendoActividad.this);
                        viewPager.setAdapter(adaptadorVideo);
                    }).addOnFailureListener(e -> {
                        Log.e("TAG", "Error al obtener el documento de vídeos para el usuario seguido: " + e.getMessage());
                    });

                }
            } else {
                String aviso = "¡Aún no sigues a nadie!";
                //Mostramos un Toast indicando que el usuario no sigue a nadie aún
                Toast.makeText(SiguiendoActividad.this, aviso, Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("TAG", "Error al obtener los usuarios seguidos del usuario actual: " + e.getMessage());
        });
    }


}

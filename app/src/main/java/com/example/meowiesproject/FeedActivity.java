package com.example.meowiesproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

//Clase encargada de mostrar un feed de vídeos
public class FeedActivity extends AppCompatActivity implements VideoAdapterActivity.OnProfileClickListener {

    ViewPager2 viewPager;
    VideoAdapterActivity adaptadorVideo;
    ImageView casaBarraNav, camaraBarraNav, perfilBarraNav;
    TextView siguiendoBarraNav, paraTiBarraNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        //Inicializamos FirebaseAuth y obtenemos el usuario actual
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

        //Configuramos los listeners para los iconos de la barra de navegación
        casaBarraNav.setOnClickListener(v -> {
            // Iniciar la actividad FeedActivity
            Intent intent = new Intent(FeedActivity.this, FeedActivity.class);
            startActivity(intent);
        });

        camaraBarraNav.setOnClickListener(v -> startActivity(new Intent(FeedActivity.this, UploadFileActivity.class)));

        perfilBarraNav.setOnClickListener(v -> onProfileClick(usuarioActualUid));

        siguiendoBarraNav.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, SiguiendoActividad.class);
            startActivity(intent);
        });

        paraTiBarraNav.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, FeedActivity.class);
            startActivity(intent);
        });

        //Inicializamos FirebaseStorage y accedemos a la carpeta "videos"
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference videosRef = storage.getReference().child("videos");

        //Creamos una lista para almacenar los IDs de los vídeos disponibles
        videosRef.listAll().addOnSuccessListener(listResult -> {
            List<String> videoIds = new ArrayList<>();

            //Recorremos la lista y obtenemos el nombre de cada vídeo (su ID)
            for (StorageReference item : listResult.getItems()) {
                String videoId = item.getName();
                videoIds.add(videoId);
            }

            //Creamos un nuevo adaptador y le pasamos los IDs de los vídeos y el contexto de FeedActivity
            adaptadorVideo = new VideoAdapterActivity(FeedActivity.this, videoIds, FeedActivity.this);
            viewPager.setAdapter(adaptadorVideo);
        }).addOnFailureListener(e -> {
            Log.e("TAG", "Error listing items in 'videos' directory: " + e.getMessage());
        });

    }

    //Método para iniciar la actividad ProfileActivity al hacer clic en el icono de perfil de la barra de navegación
    public void onProfileClick(String userId) {
        Intent intent = new Intent(FeedActivity.this, ProfileActivity.class);
        intent.putExtra("usuarioId", userId);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adaptadorVideo != null) {
            adaptadorVideo.notifyDataSetChanged();
        }
    }
}

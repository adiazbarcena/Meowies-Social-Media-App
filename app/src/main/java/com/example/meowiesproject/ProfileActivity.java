package com.example.meowiesproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.app.Activity;

import android.content.Intent;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class ProfileActivity extends AppCompatActivity {

    //Declaramos las variables
    private static final int REQUEST_IMAGE_GET = 1;
    private ImageView imagenPerfil, seguirBoton, unfollowBoton,
            casaBarraNav, cameraNavBar, perfilBarraNav;
    private FirebaseFirestore bd;
    private StorageReference storageReference;
    private String usuarioId;
    private TextView usuarioNickname, seguidoresContador, siguiendoContador;
    private VideoSummaryAdapter mVideoAdapter;
    private final ArrayList<VideoSummary> videoIdsSummary = new ArrayList<>();
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser usuarioActualFirebase = mAuth.getCurrentUser();
    String usuarioActualId = usuarioActualFirebase.getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        //Inicializamos las instancias de Firestore y Storage
        bd = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        //Inicializamos los componentes de la interfaz
        imagenPerfil = findViewById(R.id.profile_default);
        usuarioNickname = findViewById(R.id.userNickname);
        seguirBoton = findViewById(R.id.followButton);
        unfollowBoton = findViewById(R.id.unfollowButton);
        seguidoresContador = findViewById(R.id.followersCounter);
        siguiendoContador = findViewById(R.id.followingCounter);
        casaBarraNav = findViewById(R.id.casaBarraNav);
        cameraNavBar = findViewById(R.id.camaraBarraNav);
        perfilBarraNav = findViewById(R.id.perfilBarraNav);

        //Obtenemos el ID del usuario del Intent
        usuarioId = getIntent().getStringExtra("usuarioId");

        //Configuramos el RecyclerView para mostrar los videos en una cuadrícula de 3 columnas
        RecyclerView recyclerView = findViewById(R.id.recycle_view_video_summary);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3); // 3 columnas
        recyclerView.setLayoutManager(layoutManager);

        //Creamos la instancia del adaptador y la asignamos al RecyclerView
        mVideoAdapter = new VideoSummaryAdapter(this, videoIdsSummary); // Pasar la lista de IDs de video
        recyclerView.setAdapter(mVideoAdapter);

        //Llamamos al método para obtener la lista de videos
        listaVideos();

        //Configuramos los listeners para los botones de la barra de navegación
        casaBarraNav.setOnClickListener(v -> {
            // Iniciar la actividad FeedActivity
            Intent intent = new Intent(ProfileActivity.this, FeedActivity.class);
            startActivity(intent);
        });

        cameraNavBar.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, UploadFileActivity.class)));
        perfilBarraNav.setOnClickListener(v -> onProfileClick(usuarioActualId));

        //Comprobamos si el usuario actual es el propietario del perfil
        if (usuarioActualFirebase != null) {
            usuarioId = getIntent().getStringExtra("usuarioId");

            if (usuarioActualId.equals(usuarioId)) {
                // Si el usuario logueado es el propietario del perfil, permite cambiar la imagen de perfil
                imagenPerfil.setOnClickListener(v -> abrirGaleria());

            }
            cargarImagenPerfil();
            getUsuarioNickname();

        }

        //Verificamos si el usuario ya sigue a este perfil para mostrar el botón correspondiente
        verificarSeguimientoUsuario();
        obtenerContadorSeguidores();
        obtenerContadorSiguiendo();

        //Configuramos el listener para el botón de seguir
        seguirBoton.setOnClickListener(v -> {

            //Obtenemos el nickname del usuario actual
            DocumentReference usuarioActualDocRef = bd.collection("usuarios").document(usuarioActualId);
            usuarioActualDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String usuarioActualNickname = documentSnapshot.getString("nickname");
                    seguirUsuario(usuarioActualNickname);
                } else {
                    Log.d("USER_DOC_NOT_FOUND", "El documento del usuario actual no existe");
                }
            }).addOnFailureListener(e -> Log.e("QUERY_ERROR", "Error al obtener el documento del usuario actual: " + e.getMessage()));
        });

        //Configuramos el listener para el botón de dejar de seguir
        unfollowBoton.setOnClickListener(v -> {
            dejarDeSeguirUsuario();
            unfollowBoton.setVisibility(View.GONE);
            seguirBoton.setVisibility(View.VISIBLE);
        });

        //Configuramos el listener para los items del RecyclerView
        mVideoAdapter.setOnItemClickListener(new VideoSummaryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                //Obtenemos la URL del video en la posición dada
                String videoUrl = videoIdsSummary.get(position).getVideoUrl();
                //Abrimos la URL del video en un navegador
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                startActivity(browserIntent);
            }
        });





    }

    //Método para abrir la galería de imágenes
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Seleccionar imagen"), REQUEST_IMAGE_GET);
    }

    //Método para manejar el resultado de la selección de imagen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_GET && resultCode == Activity.RESULT_OK && data != null) {
            Uri imagenSeleccionadaUri = data.getData();
            seleccionarImagenPerfil(imagenSeleccionadaUri);
        }
    }

    //Método para seleccionar la imagen de perfil
    private void seleccionarImagenPerfil(Uri selectedImageUri) {

        if (usuarioActualFirebase != null && usuarioActualId.equals(usuarioId)) {
            // Solo permite cambiar la imagen de perfil si el usuario actual es el propietario del perfil
            Glide.with(this)
                    .asBitmap()
                    .load(selectedImageUri)
                    .circleCrop()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Bitmap roundedBitmap = redondearBitmap(resource);
                            imagenPerfil.setImageBitmap(roundedBitmap);
                            subirImagenFirebase(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Método opcional, puede ser ignorado si no se necesita
                        }
                    });
        } else {
            Toast.makeText(this, "No tienes permiso para cambiar la imagen de perfil de este usuario", Toast.LENGTH_SHORT).show();
        }
    }

    //Método para redondear un Bitmap
    private Bitmap redondearBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap bitmapRedondeado = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapRedondeado);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float radius = Math.min(width, height) / 2f;
        canvas.drawCircle(width / 2f, height / 2f, radius, paint);

        return bitmapRedondeado;
    }

    //Método para subir una imagen a Firebase Storage
    private void subirImagenFirebase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        StorageReference imagenPerfilRef = storageReference.child("imagenes_perfil/" + usuarioActualId + ".jpg");

        imagenPerfilRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> imagenPerfilRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> guardarUrlImagenPerfilFirestore(uri.toString()))
                        .addOnFailureListener(e -> e.printStackTrace()))
                .addOnFailureListener(e -> e.printStackTrace());
    }

    //Método para guardar la URL de la imagen de perfil en Firestore
    private void guardarUrlImagenPerfilFirestore(String imageUrl) {
        DocumentReference userRef = bd.collection("imagenes_perfil").document(usuarioActualId);
        Date currentTime = Calendar.getInstance().getTime();

        Map<String, Object> imageData = new HashMap<>();
        imageData.put("usuario_id", usuarioActualId);
        imageData.put("imagen_url", imageUrl);
        imageData.put("fecha_subida", currentTime.toString());

        userRef.set(imageData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Documento actualizado exitosamente"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al actualizar el documento", e));
    }

    //Método para cargar la imagen de perfil desde Firebase Storage
    private void cargarImagenPerfil() {
        DocumentReference userRef = bd.collection("imagenes_perfil").document(usuarioId);
        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String urlImagen = documentSnapshot.getString("imagen_url");
                        if (urlImagen != null) {
                            Glide.with(this)
                                    .load(urlImagen)
                                    .circleCrop()
                                    .into(imagenPerfil);
                        } else {
                            Log.d("URL_IMAGEN_PERFIL", "La URL de la imagen de perfil es nula");
                        }
                    } else {
                        Log.d("URL_IMAGEN_PERFIL", "El documento no existe en Firestore");
                    }
                })
                .addOnFailureListener(e -> Log.e("URL_IMAGEN_PERFIL", "Error al obtener la URL de la imagen de perfil", e));
    }

    //Método para obtener el nickname del usuario
    private void getUsuarioNickname() {
        DocumentReference usuarioDocRef = bd.collection("usuarios").document(usuarioId);

        usuarioDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Obtener el nickname del documento
                String nickname = documentSnapshot.getString("nickname");

                // Mostrar el nickname en el TextField
                usuarioNickname.setText(nickname);
            } else {
                Log.d("USER_DOC_NOT_FOUND", "El documento del usuario no existe");
            }
        }).addOnFailureListener(e -> {
            // Manejar errores de consulta
            Log.e("QUERY_ERROR", "Error al obtener el documento del usuario: " + e.getMessage());
        });
    }

    private void listaVideos() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = getIntent().getStringExtra("usuarioId");
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userVideosRef = db.collection("videos").document(userId);

            userVideosRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    videoIdsSummary.clear(); // Limpiar la lista existente antes de agregar los nuevos VideoSummary

                    Map<String, Object> userData = documentSnapshot.getData();
                    if (userData != null) {
                        for (Map.Entry<String, Object> entry : userData.entrySet()) {
                            String videoId = entry.getKey();
                            Map<String, Object> videoData = (Map<String, Object>) entry.getValue();
                            String videoUrl = (String) videoData.get("video_url");

                            // Aquí puedes utilizar la URL del vídeo (videoUrl) para reproducirlo
                            Log.d("VIDEO_INFO", "Video ID: " + videoId + ", Video URL: " + videoUrl);

                            // Agregar el resumen del video a la lista
                            VideoSummary videoSummary = new VideoSummary(videoId, videoUrl, videoUrl);
                            videoIdsSummary.add(videoSummary);
                        }

                        // Notificar al adaptador de que los datos han cambiado
                        mVideoAdapter.notifyDataSetChanged();
                    } else {
                        Log.d("USER_VIDEO_EMPTY", "No hay vídeos para el usuario con ID: " + userId);
                    }

                } else {
                    Log.d("USER_VIDEO_EMPTY", "No hay vídeos para el usuario con ID: " + userId);
                }
            }).addOnFailureListener(e -> {
                Log.e("USER_VIDEO_ERROR", "Error al obtener los vídeos del usuario con ID: " + userId, e);
            });
        } else {
            Log.d("CURRENT_USER_NULL", "El usuario actual es nulo");
        }
    }

    //Método para seguir a un usuario
    private void seguirUsuario(String currentUserNickname) {
        // Verificar si el usuario actual está intentando seguirse a sí mismo
        if (usuarioActualId.equals(usuarioId)) {
            Toast.makeText(getApplicationContext(), "No puedes seguirte a ti mismo", Toast.LENGTH_SHORT).show();
            return; // Salir del método sin hacer nada más
        }

        // Verificar si el usuario actual ya está siguiendo al usuario especificado
        DocumentReference followerRef = bd.collection("usuarios").document(usuarioId).collection("seguidores").document(usuarioActualId);
        followerRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // El usuario ya sigue a esta cuenta
                    Toast.makeText(getApplicationContext(), "Ya sigues a este usuario", Toast.LENGTH_SHORT).show();
                } else {
                    // El usuario no sigue a esta cuenta, seguir al usuario
                    Map<String, Object> followerData = new HashMap<>();
                    followerData.put("usuario_id", usuarioActualId);
                    followerData.put("nickname", currentUserNickname); // Agregar el nickname del usuario actual

                    // Agregar al usuario a la colección "followers" del usuario seguido
                    bd.collection("usuarios").document(usuarioId).collection("seguidores").document(usuarioActualId)
                            .set(followerData)
                            .addOnSuccessListener(aVoid -> {
                                // Agregar al usuario seguido a la colección "following" del usuario actual
                                DocumentReference usuarioActualRef = bd.collection("usuarios").document(usuarioActualId);
                                CollectionReference followingRef = usuarioActualRef.collection("siguiendo");
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("nickname", currentUserNickname);
                                followingRef.document(usuarioId)
                                        .set(userData)
                                        .addOnSuccessListener(aVoid1 -> {
                                            // El usuario se ha seguido correctamente
                                            Toast.makeText(getApplicationContext(), "Ahora sigues a este usuario", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            // Manejar cualquier error que ocurra al seguir al usuario
                                            Toast.makeText(getApplicationContext(), "Error al seguir al usuario", Toast.LENGTH_SHORT).show();
                                        });

                                obtenerContadorSeguidores();
                                obtenerContadorSiguiendo();
                                seguirBoton.setVisibility(View.INVISIBLE);
                                unfollowBoton.setVisibility(View.VISIBLE);
                            })
                            .addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Error al seguir al usuario", Toast.LENGTH_SHORT).show());
                }
            } else {
                Toast.makeText(getApplicationContext(), "Error al verificar el seguidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Método para dejar de seguir a un usuario
    private void dejarDeSeguirUsuario() {
        // Eliminar al seguidor de la colección "followers" del usuario seguido
        bd.collection("usuarios").document(usuarioId).collection("seguidores").document(usuarioActualId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // El usuario ya no sigue a este usuario
                    Toast.makeText(getApplicationContext(), "¡Has dejado de seguir a este usuario!", Toast.LENGTH_SHORT).show();

                    // Eliminar al usuario seguido de la colección "following" del usuario actual
                    bd.collection("usuarios").document(usuarioActualId).collection("siguiendo").document(usuarioId)
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {
                                // El usuario seguido se ha eliminado de la colección "following" correctamente
                                obtenerContadorSeguidores();
                                obtenerContadorSiguiendo();
                            })
                            .addOnFailureListener(e -> {
                                // Manejar cualquier error que ocurra al eliminar al usuario seguido de la colección "following"
                            });
                })
                .addOnFailureListener(e -> {
                    // Manejar cualquier error que ocurra al dejar de seguir al usuario
                    Toast.makeText(getApplicationContext(), "Error al dejar de seguir al usuario", Toast.LENGTH_SHORT).show();
                });
    }

    //Método para verificar si el usuario ya sigue al perfil
    private void verificarSeguimientoUsuario() {
        //Obtenemos la referencia al documento de seguimiento del usuario actual hacia el usuario de este perfil
        DocumentReference seguimientoRef = bd.collection("usuarios").document(usuarioId)
                .collection("seguidores").document(usuarioActualId);

        seguimientoRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // El usuario actual ya sigue al usuario de este perfil
                    // Cambiar la imagen del botón a "Unfollow"
                    seguirBoton.setVisibility(View.INVISIBLE);
                    unfollowBoton.setVisibility(View.VISIBLE);
                }
            } else {
                Log.d("VERIFICACION_SEGUIR", "Error al verificar el seguimiento del usuario: ", task.getException());
            }
        });
    }

    //Método para obtener el contador de seguidos
    private void obtenerContadorSeguidores() {
        //Obtenemos la referencia a la colección "followers" del usuario
        CollectionReference seguidoresRef = bd.collection("usuarios").document(usuarioId).collection("seguidores");
        //Obtenemos el número de documentos en la colección "followers"
        seguidoresRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int contadorSeguidores = queryDocumentSnapshots.size();
            //Mostramos el contador de seguidores en la interfaz de usuario
            if (contadorSeguidores == 0) {
                seguidoresContador.setText("0");
            } else {
                seguidoresContador.setText(String.valueOf(contadorSeguidores));
            }
        }).addOnFailureListener(e -> {
            Log.e("COUNT_FOLLOWERS_ERROR", "Error al obtener el contador de seguidores", e);
        });
    }

    //Método para obtener el contador de seguidos
    private void obtenerContadorSiguiendo() {
        //Obtenemos la referencia a la colección "siguiendo" del usuario
        CollectionReference siguiendoRef = bd.collection("usuarios").document(usuarioId).collection("siguiendo");
        //Obtenemos el número de documentos en la colección "siguiendo"
        siguiendoRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int contadorSiguiendo = queryDocumentSnapshots.size();
            //Mostramos el contador de siguiendo en la interfaz de usuario
            if (contadorSiguiendo == 0) {
                siguiendoContador.setText("0");
            } else {
                siguiendoContador.setText(String.valueOf(contadorSiguiendo));
            }
        }).addOnFailureListener(e -> {
            Log.e("COUNT_FOLLOWING_ERROR", "Error al obtener el contador de siguiendo", e);
        });
    }
    //Método para manejar el clic en el perfil
    public void onProfileClick(String userId) {
        Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
        intent.putExtra("usuarioId", userId);
        startActivity(intent);
    }
}

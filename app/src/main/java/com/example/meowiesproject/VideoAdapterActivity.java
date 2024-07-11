package com.example.meowiesproject;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class VideoAdapterActivity extends RecyclerView.Adapter<VideoAdapterActivity.VideoViewHolder> {

    private List<String> videoIds;
    private static List<String> videoUrls;
    private static Context context;


    //Constructor del adaptador
    public VideoAdapterActivity(Context context, List<String> videoIds, OnProfileClickListener listener) {
        this.context = context;
        this.videoIds = videoIds;
        this.profileClickListener = listener;
        this.videoUrls = new ArrayList<>(); // Inicializar la lista de URLs
    }

    //Interfaz para manejar clics en el perfil de usuario
    public interface OnProfileClickListener {
        void onProfileClick(String userId);
    }

    private static OnProfileClickListener profileClickListener;

    //Método para inflar el diseño de cada elemento de la lista
    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_video_row, parent, false);
        return new VideoViewHolder(view);
    }

    //Método para vincular los datos del video con la vista correspondiente
    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String videoId = videoIds.get(position);
        String userID = currentUser.getUid().toString();
        holder.mostrarDatosVideo(videoId, context);

        //Configuramos el OnClickListener para el botón de comentarios
        holder.comentariosView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.mostrarComentarios(context, videoId); // Llama al método para mostrar el popup cuando se hace clic en el commentView

            }
        });

        //Configuramos el OnClickListener para el botón de like
        holder.likeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("LIKE_CLICK", "Botón de like clickeado para el videoId: " + videoId);

                // Llama al método handleLikeClick(videoId) del ViewHolder para manejar el clic en el likeView
                holder.handleLikeClick(videoId, userID);
            }
        });

    }

    //Método para obtener la cantidad total de elementos en la lista de videos
    @Override
    public int getItemCount() {
        return videoIds.size();
    }


    static class VideoViewHolder extends RecyclerView.ViewHolder {

        VideoView videoView;
        TextView tituloVideo, descripcionVideo, contadorLikes;
        ProgressBar ProgressVideo;
        ImageView perfilImage, likeView, comentariosView, compartirView, likeRojoView;
        Button agregarComentarioButton;


        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            tituloVideo = itemView.findViewById(R.id.VideoTitle);
            descripcionVideo = itemView.findViewById(R.id.videoDescp);
            ProgressVideo = itemView.findViewById(R.id.ProgreesVideo);
            perfilImage = itemView.findViewById(R.id.perfilView);
            contadorLikes = itemView.findViewById(R.id.likeCount);
            likeView = itemView.findViewById(R.id.likeView);
            comentariosView = itemView.findViewById(R.id.commentView);
            compartirView = itemView.findViewById(R.id.shareView);
            agregarComentarioButton = itemView.findViewById(R.id.agregarComentarioButton);
            likeRojoView = itemView.findViewById(R.id.like_rojo);

        }

        //Método para mostrar los datos del video en la vista correspondiente
        public void mostrarDatosVideo(String videoId, Context context) {
            //Creamos una instancia de FirebaseFirestore para interactuar con la base de datos
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            //Creamos una referencia a la colección "videos" de Firestore Database
            CollectionReference videosRef = db.collection("videos");

            //Realizamos una consulta para obtener los detalles del vídeo que contenga el video_id especificado
            videosRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
                //Procesamos los documentos obtenidos
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    //Obtenemos los datos del documento
                    Map<String, Object> userData = document.getData();
                    if (userData != null) {
                        //Verificamos si el documento contiene un array con el mismo nombre que el video_id
                        if (userData.containsKey(videoId) && userData.get(videoId) instanceof Map) {
                            //Obtenemos el array de datos del video
                            Map<String, Object> video = (Map<String, Object>) userData.get(videoId);

                            //Extraemos los detalles relevantes del video
                            String titulo = (String) video.get("titulo");
                            String descripcion = (String) video.get("descripcion");
                            String videoUrl = (String) video.get("video_url");
                            String usuarioId = (String) video.get("usuario_id");
                            //Añadimos la URL del video a la lista de URLs
                            videoUrls.add(videoUrl);
                            //Actualizamos la interfaz de usuario con los detalles del video
                            tituloVideo.setText(titulo);
                            descripcionVideo.setText(descripcion);
                            // Cargamos la imagen de perfil del usuario que ha subido el video
                            cargarImagenPerfil(usuarioId, context);

                            //Configuramos el listener para el botón de compartir
                            compartirView.setOnClickListener(v -> {
                                // Compartir la URL del video actual
                                compartirUrlVideo(videoUrl);
                            });

                            //Configuramos el listener para hacer clic en la imagen de perfil
                            perfilImage.setOnClickListener(v -> {
                                if (profileClickListener != null) {
                                    profileClickListener.onProfileClick(usuarioId);
                                }
                            });

                            //Mostramos el video en el VideoView
                            Uri videoUri = Uri.parse(videoUrl);
                            videoView.setVideoURI(videoUri);
                            videoView.start();

                            //Realizamos una consulta adicional para obtener el número de likes del video
                            DocumentReference videoLikesRef = db.collection("likes").document(videoId);
                            videoLikesRef.get().addOnSuccessListener(likesSnapshot -> {
                                if (likesSnapshot.exists()) {
                                    //Si el documento de likes existe, obtenemos el número de likes y actualizamos el contador
                                    long likesCount = likesSnapshot.getLong("me_gusta");
                                    contadorLikes.setText(String.valueOf(likesCount));
                                } else {
                                    //Si el documento de likes no existe, lo establecemos a 0
                                    contadorLikes.setText("0");
                                }
                            }).addOnFailureListener(e -> {
                                //Manejar cualquier error al obtener el contador de likes
                                Log.e("LIKE_COUNT_ERROR", "Error al obtener el contador de Me gusta: " + e.getMessage());
                            });

                            //Salir del bucle una vez que se haya encontrado el video correspondiente
                            return;
                        }
                    }
                }
            }).addOnFailureListener(e -> {
                // Manejar cualquier error durante la consulta
                Log.e("DETAILS_ERROR", "Error al obtener los detalles del video: " + e.getMessage());
            });
        }

        //Método para cargar la imagen de perfil del usuario que subió el video
        private void cargarImagenPerfil(String userId, Context context) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("imagenes_perfil").document(userId);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String imageUrl = documentSnapshot.getString("imagen_url");
                    if (imageUrl != null && context != null) {
                        CircleImageView perfilImage = itemView.findViewById(R.id.perfilView);
                        Picasso.get().load(imageUrl).fit().centerCrop().into(perfilImage);
                    } else {
                        Log.e("PROFILE_IMAGE_NULL", "Profile image is null or context is null");
                    }
                } else {
                    Log.d("PROFILE_IMAGE", "Profile image document does not exist in Firestore");
                }
            }).addOnFailureListener(e -> {
                Log.e("PROFILE_IMAGE_ERROR", "Error loading profile image: " + e.getMessage());
            });
        }

        //Método para manejar clics en el botón de like
        private void handleLikeClick(String videoId, String userId) {
            Log.d("HANDLE_LIKE_CLICK", "Manejando clic en el botón de like para el videoId: " + videoId);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference videoLikesRef = db.collection("likes").document(videoId);

            //Obtenemos el documento del usuario para obtener el nickname
            DocumentReference userRef = db.collection("usuarios").document(userId);

            //Obtenemos el nickname del usuario
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String userNickname = documentSnapshot.getString("nickname");

                    //Actualizamos el contador de me gusta y los datos del usuario que dio like
                    db.runTransaction(transaction -> {
                        DocumentSnapshot snapshot = transaction.get(videoLikesRef);

                        if (!snapshot.exists()) {
                            //Si el documento no existe, lo creamos con el contador de me gusta en 1
                            Map<String, Object> data = new HashMap<>();
                            data.put("me_gusta", 1L);
                            data.put("usuarios", Arrays.asList(userId)); //Agregamos el ID del usuario que dio like
                            data.put("nicknames", Arrays.asList(userNickname)); //Agregamos el nickname del usuario que dio like
                            transaction.set(videoLikesRef, data);
                            return 1L; //Devolvemos el nuevo valor de me gusta
                        }

                        //Si el documento existe, actualizamos el contador de me gusta y los datos del usuario
                        long currentLikes = snapshot.getLong("me_gusta");
                        List<String> usuarios = (List<String>) snapshot.get("usuarios");
                        List<String> nicknames = (List<String>) snapshot.get("nicknames");

                        //Verificamos si el usuario ya dio like al video
                        if (usuarios.contains(userId)) {
                            // El usuario ya ha dado like, quitamos el like
                            currentLikes--;
                            usuarios.remove(userId);
                            nicknames.remove(userNickname);

                        } else {
                            //El usuario no ha dado like, agregamos el like
                            currentLikes++;
                            usuarios.add(userId);
                            nicknames.add(userNickname);
                            
                        }

                        //Actualizamos el documento con los nuevos datos
                        transaction.update(videoLikesRef, "me_gusta", currentLikes);
                        transaction.update(videoLikesRef, "usuarios", usuarios);
                        transaction.update(videoLikesRef, "nicknames", nicknames);

                        return currentLikes; //Devolvemos el nuevo valor de me gusta
                    }).addOnSuccessListener(new OnSuccessListener<Long>() {
                        @Override
                        public void onSuccess(Long newLikes) {
                            //Actualización correcta, mostramos el nuevo contador en la interfaz de usuario
                            Log.d("HANDLE_LIKE_CLICK", "Contador de Me gusta actualizado exitosamente en Firestore: " + newLikes);
                            contadorLikes.setText(String.valueOf(newLikes));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Manejar errores de actualización
                            Log.e("HANDLE_LIKE_CLICK", "Error al actualizar el contador de Me gusta en Firestore: " + e.getMessage());
                        }
                    });
                } else {
                    Log.e("HANDLE_LIKE_CLICK", "Error: No se encontró el usuario con ID: " + userId);
                }
            }).addOnFailureListener(e -> {
                // Manejar errores al obtener el documento del usuario
                Log.e("HANDLE_LIKE_CLICK", "Error al obtener el documento del usuario en Firestore: " + e.getMessage());
            });
        }



        private void compartirUrlVideo(String videoUrl) {
            //Creamos un Intent con ACTION_SEND
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            //Establecemos el contenido como texto plano
            shareIntent.setType("text/plain");
            //Agregamos la URL del vídeo al intent
            shareIntent.putExtra(Intent.EXTRA_TEXT, videoUrl);
            //Iniciamos el intent y mostramos el diálogo de compartir
            Intent chooser = Intent.createChooser(shareIntent, "Compartir vídeo mediante");
            context.startActivity(chooser);
        }


        private void mostrarComentarios(Context context, String videoId) {
            //Inflamos el layout
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.comentarios, null); // Aquí se infla el layout del popup

            //Obtenemos una referencia al ListView dentro del layout inflado
            ListView comentariosListView = popupView.findViewById(R.id.comentariosListView);

            //Cargamos los comentarios en él
            cargarComentarios(videoId, context, comentariosListView);

            //Obtenemos una referencia al EditText dentro del layout inflado
            EditText nuevoComentarioEditText = popupView.findViewById(R.id.nuevoComentarioEditText);

            //Obtenemos una referencia al botón agregarComentarioButton dentro del layout inflado
            Button agregarComentarioButton = popupView.findViewById(R.id.agregarComentarioButton);

            //Agregamos un OnClickListener para el botón agregarComentarioButton
            agregarComentarioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Llamar al método agregarComentario con el videoId correspondiente y la referencia al EditText
                    agregarComentario(videoId, nuevoComentarioEditText);
                    cargarComentarios(videoId,context,comentariosListView);
                }
            });

            int width = LinearLayout.LayoutParams.MATCH_PARENT;
            int height = LinearLayout.LayoutParams.MATCH_PARENT;
            boolean focusable = true;
            PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
            popupWindow.showAtLocation(itemView, Gravity.CENTER, 0, 0);
        }



        private void cargarComentarios(String videoId, Context context, ListView comentariosListView) {
            //Obtenemos una referencia a la colección "comentarios"
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference comentariosRef = db.collection("comentarios").document(videoId).collection("lista_comentarios");

            //Realizamos una consulta para obtener todos los comentarios específicos para el vídeo actual
            comentariosRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
                // Crear una lista para almacenar los comentarios junto con los nicknames
                List<String> comentariosConNicknameList = new ArrayList<>();

                //Recorremos todos los documentos
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    // Obtener la lista de comentarios del documento
                    List<String> comentarios = (List<String>) document.get("comentarios");
                    // Obtener el ID del usuario que ha realizado el comentario
                    String userId = document.getId();

                    //Iteramos sobre cada comentario en la lista y agrégalo a la lista de comentarios
                    for (String comentario : comentarios) {
                        //Obtenemos el nickname del usuario a partir de su ID
                        DocumentReference usuarioRef = db.collection("usuarios").document(userId);
                        usuarioRef.get().addOnSuccessListener(usuarioSnapshot -> {
                            if (usuarioSnapshot.exists()) {
                                String nickname = usuarioSnapshot.getString("nickname");
                                //Unimos el nickname con el comentario y lo agregamos a la lista
                                String comentarioConNickname = nickname + ": " + comentario;
                                comentariosConNicknameList.add(comentarioConNickname);

                                //Notificamos al adaptador de comentarios que los datos han cambiado
                                ArrayAdapter<String> comentarioAdapter = new ArrayAdapter<>(context, R.layout.comentario_nickname, comentariosConNicknameList);
                                comentariosListView.setAdapter(comentarioAdapter);
                                comentarioAdapter.notifyDataSetChanged();
                            } else {
                                Log.e("CARGAR_COMENTARIOS", "El documento de usuario no existe para el ID: " + userId);
                            }
                        }).addOnFailureListener(e -> {
                            //Manejamos los posibles errores al obtener el documento de usuario
                            Log.e("CARGAR_COMENTARIOS", "Error al obtener el documento de usuario: " + e.getMessage());
                        });
                    }
                }
            }).addOnFailureListener(e -> {
                //Manejamos los posibles errores de carga de comentarios
                Log.e("CARGAR_COMENTARIOS", "Error al cargar los comentarios: " + e.getMessage());
            });
        }

        //Método para añadir comentarios
        private void agregarComentario(String videoId, EditText nuevoComentarioEditText) {
            //Obtenemos el texto del comentario del EditText
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            String cUsuario = currentUser.getUid();
            String comentario = nuevoComentarioEditText.getText().toString();

            //Verificmos si el comentario está vacío
            if (comentario.isEmpty()) {
                Log.d("AGREGAR_COMENTARIO", "El comentario está vacío");
                return;
            }
            //Obtenemos una instancia de Firebase
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            //Obtenemos una referencia al documento de comentarios para el usuario actual en la colección "lista_comentarios" del video especificado
            DocumentReference usuarioComentariosRef = db.collection("comentarios").document(videoId).collection("lista_comentarios").document(cUsuario);
            //Creamos un mapa para almacenar los datos del comentario
            Map<String, Object> nuevoComentario = new HashMap<>();
            nuevoComentario.put("comentarios", FieldValue.arrayUnion(comentario)); // Agregar el nuevo comentario al array existente
            //Agregamos el nuevo comentario al documento de comentarios para el usuario actual
            usuarioComentariosRef
                    .set(nuevoComentario, SetOptions.merge()) //Utilizamos SetOptions.merge() para agregar el nuevo comentario al array sin sobrescribir otros campos
                    .addOnSuccessListener(documentReference -> {
                        Log.d("AGREGAR_COMENTARIO", "Comentario agregado para el usuario " + cUsuario);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AGREGAR_COMENTARIO", "Error al agregar comentario: " + e.getMessage());
                    });
        }

    }

}
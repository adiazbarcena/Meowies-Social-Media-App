package com.example.meowiesproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class VideoSummaryAdapter extends RecyclerView.Adapter<VideoSummaryAdapter.ViewHolder> {
    private ArrayList<VideoSummary> mData;
    private LayoutInflater mInflater;

    private Context mainContext;

    private OnItemClickListener mListener;

    //Interfaz para manejar los clics en los elementos del RecyclerView
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    //Constructor del adaptador
    public VideoSummaryAdapter(Context context, ArrayList<VideoSummary> data) {
        this.mainContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }


    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.prueba, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each cell
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        setThumbnailImage(holder, mData.get(position).getThumbnailUri());

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                int positionClicked = holder.getAdapterPosition();
                if (positionClicked != RecyclerView.NO_POSITION) {
                    mListener.onItemClick(positionClicked);
                }
            }
        });
        holder.setOnItemClickListener(new ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(view.getContext(), ProfileActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("videoId", mData.get(position).getVideoId());
                intent.putExtras(bundle);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);
            }
        });

    }

    //Método para cargar la imagen de thumbnail usando Glide
    public void setThumbnailImage(ViewHolder holder, String thumbnailUri) {
        Log.i("url to get: ", "message: " + thumbnailUri);
        Glide.with(holder.itemView.getContext())
                .load(thumbnailUri) // Cargar la URL de la imagen del thumbnail
                .into(holder.thumbnail);
    }


    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView thumbnail;
        private ItemClickListener itemClickListener;

        ViewHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.image_thumbnail);
            itemView.setOnClickListener(this);
        }

        //Método para configurar el listener de clics en la vista
        void setOnItemClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            itemClickListener.onItemClick(view,getBindingAdapterPosition());
        }
    }

    //Interfaz para manejar los clics en la vista de la celda
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }}

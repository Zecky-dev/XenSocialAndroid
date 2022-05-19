package com.zecky_dev.xensocial;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.juliomarcos.ImageViewPopUpHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class CustomPostAdapter extends RecyclerView.Adapter<CustomPostAdapter.ViewHolder>{

    private ArrayList<QueryDocumentSnapshot> posts;
    private LayoutInflater mInflater;
    private AdapterView.OnItemClickListener mOnItemClickListener;
    private Context context;

    public CustomPostAdapter(Context context, ArrayList<QueryDocumentSnapshot> posts)
    {
        this.posts = posts;
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.little_post_layout,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String postImageURL = (String)posts.get(position).get("PostImageURL");
        if(postImageURL!=null){
            Picasso.get().load(postImageURL).into(holder.littlePostIW);
        }
        else{
            holder.littlePostIW.setImageResource(R.drawable.no_image_selected);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView littlePostIW;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            littlePostIW = itemView.findViewById(R.id.littlePostImageIW);
            littlePostIW.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getLayoutPosition();
                    Dialog dialog = new Dialog(context);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    dialog.setContentView(R.layout.custom_popup_dialog);
                    String postImageURL,postComment;
                    postImageURL = (String)posts.get(position).get("PostImageURL");
                    Picasso.get().load(postImageURL).into((ImageView)dialog.findViewById(R.id.popupImageIW));
                    dialog.findViewById(R.id.closePopup).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                    return true;
                }
            });

        }
    }

}

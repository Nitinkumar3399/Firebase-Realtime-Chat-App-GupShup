package com.appeteria.training.gupshup.chats;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appeteria.training.gupshup.Common.Constants;
import com.appeteria.training.gupshup.Common.Extras;
import com.appeteria.training.gupshup.Common.Util;
import com.appeteria.training.gupshup.R;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {

    private Context context;
    private List<ChatListModel> chatListModelList;

    public ChatListAdapter(Context context, List<ChatListModel> chatListModelList) {
        this.context = context;
        this.chatListModelList = chatListModelList;
    }


    @NonNull
    @Override
    public ChatListAdapter.ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_list_layout, parent, false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ChatListAdapter.ChatListViewHolder holder, int position) {

        final ChatListModel chatListModel = chatListModelList.get(position);

        holder.tvFullName.setText(chatListModel.getUserName());

        StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(Constants.IMAGES_FOLDER + "/" + chatListModel.getPhotoName());
        fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(context)
                        .load(uri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(holder.ivProfile);

            }
        });

        String lastMessage  = chatListModel.getLastMessage();
        lastMessage = lastMessage.length()>30?lastMessage.substring(0,30):lastMessage;
        holder.tvLastMessage.setText(lastMessage);

        String lastMessageTime = chatListModel.getLastMessageTime();
        if(lastMessageTime==null) lastMessageTime="";
        if(!TextUtils.isEmpty(lastMessageTime))
            holder.tvLastMessageTime.setText(Util.getTimeAgo(Long.parseLong(lastMessageTime)));


        if(!chatListModel.getUnreadCount().equals("0"))
        {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(chatListModel.getUnreadCount());
        }
        else
            holder.tvUnreadCount.setVisibility(View.GONE);


        holder.llChatList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(Extras.USER_KEY, chatListModel.getUserId());
                intent.putExtra(Extras.USER_NAME, chatListModel.getUserName());
                intent.putExtra(Extras.PHOTO_NAME, chatListModel.getPhotoName());
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return chatListModelList.size();
    }

    public class ChatListViewHolder  extends  RecyclerView.ViewHolder{

        private LinearLayout llChatList;
        private TextView tvFullName, tvLastMessage, tvLastMessageTime, tvUnreadCount;
        private ImageView ivProfile;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);

            llChatList = itemView.findViewById(R.id.llChatList);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMessageTime);
            tvUnreadCount= itemView.findViewById(R.id.tvUnreadCount);
            ivProfile = itemView.findViewById(R.id.ivProfile);
        }
    }
}

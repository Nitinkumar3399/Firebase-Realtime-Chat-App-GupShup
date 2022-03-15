package com.appeteria.training.gupshup.chats;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.appeteria.training.gupshup.Common.NodeNames;
import com.appeteria.training.gupshup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {

    private RecyclerView rvChatList;
    private View progressBar;
    private TextView tvEmptyChatList;
    private  ChatListAdapter chatListAdapter;
    private List<ChatListModel> chatListModelList;

    private DatabaseReference databaseReferenceChats, databaseReferenceUsers;
    private FirebaseUser currentUser;

    private ChildEventListener childEventListener;
    private Query query;

    private  List<String> userIds;

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChatList = view.findViewById(R.id.rvChats);
        tvEmptyChatList = view.findViewById(R.id.tvEmptyChatList);

        userIds = new ArrayList<>();
        chatListModelList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(getActivity(), chatListModelList);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        rvChatList.setLayoutManager(linearLayoutManager);

        rvChatList.setAdapter(chatListAdapter);

        progressBar = view.findViewById(R.id.progressBar);

        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceChats= FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS).child(currentUser.getUid());

        query = databaseReferenceChats.orderByChild(NodeNames.TIME_STAMP);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                updateList(dataSnapshot, true, dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                updateList(dataSnapshot, false, dataSnapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        query.addChildEventListener(childEventListener);

        progressBar.setVisibility(View.VISIBLE);
        tvEmptyChatList.setVisibility(View.VISIBLE);

    }


    private  void updateList(DataSnapshot dataSnapshot, final boolean isNew, final String userId)
    {
        progressBar.setVisibility(View.GONE);
        tvEmptyChatList.setVisibility(View.GONE);

        final String  lastMessage, lastMessageTime, unreadCount;

        if(dataSnapshot.child(NodeNames.LAST_MESSAGE).getValue()!=null)
            lastMessage = dataSnapshot.child(NodeNames.LAST_MESSAGE).getValue().toString();
        else
            lastMessage = "";

        if(dataSnapshot.child(NodeNames.LAST_MESSAGE_TIME).getValue()!=null)
            lastMessageTime = dataSnapshot.child(NodeNames.LAST_MESSAGE_TIME).getValue().toString();
        else
            lastMessageTime="";

        unreadCount=dataSnapshot.child(NodeNames.UNREAD_COUNT).getValue()==null?
                "0":dataSnapshot.child(NodeNames.UNREAD_COUNT).getValue().toString();

        databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            String fullName = dataSnapshot.child(NodeNames.NAME).getValue()!=null?
                    dataSnapshot.child(NodeNames.NAME).getValue().toString():"";

                /*String photoName = dataSnapshot.child(NodeNames.PHOTO).getValue()!=null?
                        dataSnapshot.child(NodeNames.PHOTO).getValue().toString():"";*/
                String photoName  = userId +".jpg";

                ChatListModel chatListModel = new ChatListModel(userId, fullName, photoName,unreadCount,lastMessage,lastMessageTime);

                if(isNew) {
                    chatListModelList.add(chatListModel);
                    userIds.add(userId);
                }
                else {
                    int indexOfClickedUser = userIds.indexOf(userId) ;
                    chatListModelList.set(indexOfClickedUser, chatListModel);
                }

                chatListAdapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                Toast.makeText(getActivity(),  getActivity().getString(R.string.failed_to_fetch_chat_list, databaseError.getMessage())
                        , Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        query.removeEventListener(childEventListener);
    }
}


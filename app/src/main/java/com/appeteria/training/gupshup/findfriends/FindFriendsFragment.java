package com.appeteria.training.gupshup.findfriends;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.appeteria.training.gupshup.Common.Constants;
import com.appeteria.training.gupshup.Common.NodeNames;
import com.appeteria.training.gupshup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
public class FindFriendsFragment extends Fragment {

    private RecyclerView rvFindFriends;
    private FindFriendAdapter findFriendAdapter;
    private List<FindFriendModel> findFriendModelList;
    private TextView tvEmptyFriendsList;

    private DatabaseReference databaseReference, databaseReferenceFriendRequests;
    private FirebaseUser currentUser;
    private  View progressBar;


    public FindFriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFindFriends = view.findViewById(R.id.rvFindFriends);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyFriendsList = view.findViewById(R.id.tvEmptyFriendsList);

        rvFindFriends.setLayoutManager(new LinearLayoutManager(getActivity()));

        findFriendModelList =new ArrayList<>();
        findFriendAdapter = new FindFriendAdapter(getActivity(), findFriendModelList);
        rvFindFriends.setAdapter(findFriendAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        currentUser  = FirebaseAuth.getInstance().getCurrentUser();

        databaseReferenceFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());

        tvEmptyFriendsList.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        Query query = databaseReference.orderByChild(NodeNames.NAME);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                findFriendModelList.clear();
                for (final DataSnapshot ds : dataSnapshot.getChildren())
                {
                    final String userId= ds.getKey();

                    if(userId.equals(currentUser.getUid()))
                        continue;

                    if(ds.child(NodeNames.NAME).getValue()!=null)
                    {
                        final String fullName = ds.child(NodeNames.NAME).getValue().toString();
                        final String photoName = ds.child(NodeNames.PHOTO).getValue()!=null? ds.child(NodeNames.PHOTO).getValue().toString():"";

                        databaseReferenceFriendRequests.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists())
                                {
                                    String requestType =  dataSnapshot.child(NodeNames.REQUEST_TYPE).getValue().toString();
                                    if(requestType.equals(Constants.REQUEST_STATUS_SENT))
                                    {
                                        findFriendModelList.add(new FindFriendModel(fullName, photoName, userId, true));
                                        findFriendAdapter.notifyDataSetChanged();

                                    }
                                }
                                else{
                                    findFriendModelList.add(new FindFriendModel(fullName, photoName, userId, false));
                                    findFriendAdapter.notifyDataSetChanged();

                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                progressBar.setVisibility(View.GONE);
                            }
                        });


                       tvEmptyFriendsList.setVisibility(View.GONE);
                       progressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                    progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), getContext().getString( R.string.failed_to_fetch_friends, databaseError.getMessage())
                        , Toast.LENGTH_SHORT).show();
            }
        });

    }
}














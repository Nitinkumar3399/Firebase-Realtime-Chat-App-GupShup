package com.appeteria.training.gupshup.requests;

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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private RecyclerView rvRequests;
    private RequestAdapter adapter;
    private List<RequestModel> requestModelList;
    private TextView tvEmptyRequestsList;

    private DatabaseReference databaseReferenceRequests, databaseReferenceUsers;
    private FirebaseUser currentUser;
    private  View progressBar;


    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvRequests = view.findViewById(R.id.rvRequests);
        tvEmptyRequestsList = view.findViewById(R.id.tvEmptyRequestsList);

        progressBar = view.findViewById(R.id.progressBar);

        rvRequests.setLayoutManager( new LinearLayoutManager(getActivity()));
        requestModelList = new ArrayList<>();
        adapter = new RequestAdapter(getActivity(), requestModelList);
        rvRequests.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

        databaseReferenceRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());

        tvEmptyRequestsList.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);


        databaseReferenceRequests.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE);
                requestModelList.clear();

                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    if(ds.exists()){
                        String requestType = ds.child(NodeNames.REQUEST_TYPE).getValue().toString();
                        if(requestType.equals(Constants.REQUEST_STATUS_RECEIVED))
                        {
                            final String userId= ds.getKey();
                            databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String userName = dataSnapshot.child(NodeNames.NAME).getValue().toString();

                                    String photoName="";
                                    if(dataSnapshot.child(NodeNames.PHOTO).getValue()!=null)
                                    {
                                        photoName = dataSnapshot.child(NodeNames.PHOTO).getValue().toString();
                                    }

                                    RequestModel requestModel = new RequestModel(userId, userName, photoName);
                                    requestModelList.add(requestModel);
                                    adapter.notifyDataSetChanged();
                                    tvEmptyRequestsList.setVisibility(View.GONE);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Toast.makeText(getActivity(),  getActivity().getString( R.string.failed_to_fetch_friend_requests, databaseError.getMessage())
                                            , Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(),  getActivity().getString( R.string.failed_to_fetch_friend_requests, databaseError.getMessage())
                        , Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });


    }

}
















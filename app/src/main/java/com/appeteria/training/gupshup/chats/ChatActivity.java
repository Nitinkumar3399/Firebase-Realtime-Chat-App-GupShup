package com.appeteria.training.gupshup.chats;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appeteria.training.gupshup.Common.Constants;
import com.appeteria.training.gupshup.Common.Extras;
import com.appeteria.training.gupshup.Common.NodeNames;
import com.appeteria.training.gupshup.Common.Util;
import com.appeteria.training.gupshup.R;
import com.appeteria.training.gupshup.selectfriend.SelectFriendActivity;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView ivSend, ivAttachment, ivProfile;
    private TextView tvUserName, tvUserStatus;

    private EditText etMessage;
    private DatabaseReference mRootRef;
    private FirebaseAuth firebaseAuth;
    private String currentUserId, chatUserId;

    private RecyclerView rvMessages;
    private SwipeRefreshLayout srlMessages;
    private MessagesAdapter messagesAdapter;
    private List<MessageModel> messagesList;

    private int currentPage = 1;
    private static final int RECORD_PER_PAGE = 30;

    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    private static final int REQUEST_CODE_PICK_VIDEO = 103;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 102;

    private static final int REQUEST_CODE_FORWARD_MESSAGE = 104;


    private DatabaseReference databaseReferenceMessages;
    private ChildEventListener childEventListener;

    private BottomSheetDialog bottomSheetDialog;

    private LinearLayout llProgress;
    private String userName, photoName;

    private ChipGroup cgSmartReplies;
    private List<TextMessage> conversation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
            ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_action_bar, null);

            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setElevation(0);

            actionBar.setCustomView(actionBarLayout);
            actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        // initialization of variables inside onCreate Method
        cgSmartReplies = findViewById(R.id.cgSmartReplies);
        conversation = new ArrayList<>();

        ivProfile = findViewById(R.id.ivProfile);
        tvUserName = findViewById(R.id.tvUserName);

        tvUserStatus = findViewById(R.id.tvUserStatus);

        ivSend = findViewById(R.id.ivSend);
        ivAttachment = findViewById(R.id.ivAttachment);
        etMessage = findViewById(R.id.etMessage);

        llProgress = findViewById(R.id.llProgress);

        ivSend.setOnClickListener(this);
        ivAttachment.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (getIntent().hasExtra(Extras.USER_KEY)) {
            chatUserId = getIntent().getStringExtra(Extras.USER_KEY);
            photoName = chatUserId + ".jpg";
        }
        if (getIntent().hasExtra(Extras.USER_NAME))
            userName = getIntent().getStringExtra(Extras.USER_NAME);

        //if (getIntent().hasExtra(Extras.PHOTO_NAME))
            //getIntent().getStringExtra(Extras.PHOTO_NAME);


        tvUserName.setText(userName);


        if(!TextUtils.isEmpty(photoName) && photoName!=null) {
            StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(Constants.IMAGES_FOLDER + "/" + photoName);

            photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Glide.with(ChatActivity.this)
                            .load(uri)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(ivProfile);
                }
            });
        }


        rvMessages = findViewById(R.id.rvMessages);
        srlMessages = findViewById(R.id.srlMessages);

        messagesList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(this, messagesList);

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messagesAdapter);

        loadMessages();

        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);

        rvMessages.scrollToPosition(messagesList.size() - 1);

        srlMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                currentPage++;
                loadMessages();
            }
        });

        bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.chat_file_options, null);
        view.findViewById(R.id.llCamera).setOnClickListener(this);
        view.findViewById(R.id.llGallery).setOnClickListener(this);
        view.findViewById(R.id.llVideo).setOnClickListener(this);
        view.findViewById(R.id.ivClose).setOnClickListener(this);
        bottomSheetDialog.setContentView(view);

        if(getIntent().hasExtra(Extras.MESSAGE) && getIntent().hasExtra(Extras.MESSAGE_ID) && getIntent().hasExtra(Extras.MESSAGE_TYPE) )
        {
            String message = getIntent().getStringExtra(Extras.MESSAGE);
            String messageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
            final String messageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);

            DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
            final String newMessageId = messageRef.getKey();

            if(messageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
                sendMessage(message, messageType, newMessageId);
            }
            else{
                StorageReference rootRef = FirebaseStorage.getInstance().getReference();
                String folder = messageType.equals( Constants.MESSAGE_TYPE_VIDEO)? Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;
                String oldFileName = messageType.equals( Constants.MESSAGE_TYPE_VIDEO)?messageId + ".mp4": messageId+".jpg";
                String newFileName = messageType.equals( Constants.MESSAGE_TYPE_VIDEO)?newMessageId + ".mp4": newMessageId+".jpg";

                final String localFilePath = getExternalFilesDir(null).getAbsolutePath() + "/" + oldFileName;
                final File localFile = new File(localFilePath);

                final StorageReference newFileRef = rootRef.child(folder).child(newFileName);
                        rootRef.child(folder).child(oldFileName).getFile(localFile)
                                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                UploadTask uploadTask = newFileRef.putFile(Uri.fromFile(localFile));
                                uploadProgress(uploadTask, newFileRef, newMessageId, messageType);
                            }
                        });
                    }

            }



        DatabaseReference databaseReferenceUsers = mRootRef.child(NodeNames.USERS).child(chatUserId);
        databaseReferenceUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String status="";
                if(dataSnapshot.child(NodeNames.ONLINE).getValue()!=null)
                    status = dataSnapshot.child(NodeNames.ONLINE).getValue().toString();

                if(status.equals("true"))
                    tvUserStatus.setText(Constants.STATUS_ONLINE);
                else
                    tvUserStatus.setText(Constants.STATUS_OFFLINE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                DatabaseReference currentUserRef = mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId);
                if(editable.toString().matches(""))
                {
                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STOPPED);
                }
                else
                {
                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STARTED);
                }

            }
        });


        DatabaseReference chatUserRef = mRootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUserId);
        chatUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.child(NodeNames.TYPING).getValue()!=null)
                {
                    String typingStatus = dataSnapshot.child(NodeNames.TYPING).getValue().toString();

                    if(typingStatus.equals(Constants.TYPING_STARTED))
                        tvUserStatus.setText(Constants.STATUS_TYPING);
                    else
                        tvUserStatus.setText(Constants.STATUS_ONLINE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        }


    private void sendMessage(final String msg, final String msgType, String pushId) {
        try {
            if (!msg.equals("")) {

                HashMap messageMap = new HashMap();
                messageMap.put(NodeNames.MESSAGE_ID, pushId);
                messageMap.put(NodeNames.MESSAGE, msg);
                messageMap.put(NodeNames.MESSAGE_TYPE, msgType);
                messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
                messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);

                String currentUserRef = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
                String chatUserRef = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;

                HashMap messageUserMap = new HashMap();
                messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
                messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

                etMessage.setText("");
                if(msgType.equals(Constants.MESSAGE_TYPE_TEXT))
                {
                    conversation.add(TextMessage.createForLocalUser(msg, System.currentTimeMillis()));
                }

                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send_message, databaseError.getMessage())
                                    , Toast.LENGTH_SHORT).show();
                        }
                        {
                            Toast.makeText(ChatActivity.this, R.string.message_sent_successfully, Toast.LENGTH_SHORT).show();
                            String title="";

                            if(msgType.equals(Constants.MESSAGE_TYPE_TEXT))
                                title = "New Message";
                            else if(msgType.equals(Constants.MESSAGE_TYPE_IMAGE))
                                title = "New Image";
                            else if(msgType.equals(Constants.MESSAGE_TYPE_VIDEO))
                                title = "New Video";

                            Util.sendNotification(ChatActivity.this, title, msg, chatUserId);

                            String lastMessage= !title.equals("New Message")?title:msg;

                            Util.updateChatDetails(ChatActivity.this, currentUserId, chatUserId, lastMessage);


                        }
                    }
                });
            }
        } catch (Exception ex) {
            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send_message, ex.getMessage())
                    , Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMessages() {
        messagesList.clear();

        conversation.clear();
        cgSmartReplies.removeAllViews();
        databaseReferenceMessages = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);

        Query messageQuery = databaseReferenceMessages.limitToLast(currentPage * RECORD_PER_PAGE);

        if (childEventListener != null)
            messageQuery.removeEventListener(childEventListener);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                MessageModel message = dataSnapshot.getValue(MessageModel.class);

                messagesList.add(message);
                messagesAdapter.notifyDataSetChanged();
                rvMessages.scrollToPosition(messagesList.size() - 1);
                srlMessages.setRefreshing(false);

                showSmartReplies(message);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    loadMessages();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                srlMessages.setRefreshing(false);
            }
        };

        messageQuery.addChildEventListener(childEventListener);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.ivSend:
                if (Util.connectionAvailable(this)) {
                    DatabaseReference userMessagePush = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
                    String pushId = userMessagePush.getKey();
                    sendMessage(etMessage.getText().toString().trim(), Constants.MESSAGE_TYPE_TEXT, pushId);
                } else {
                    Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.ivAttachment:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    if (bottomSheetDialog != null)
                        bottomSheetDialog.show();


                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }


                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (inputMethodManager != null)
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                break;

            case R.id.llCamera:
                bottomSheetDialog.dismiss();
                Intent intentCamera = new Intent(ACTION_IMAGE_CAPTURE);
                startActivityForResult(intentCamera, REQUEST_CODE_CAPTURE_IMAGE);
                break;

            case R.id.llGallery:
                bottomSheetDialog.dismiss();
                Intent intentImage = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentImage, REQUEST_CODE_PICK_IMAGE);
                break;

            case R.id.llVideo:
                bottomSheetDialog.dismiss();
                Intent intentVideo = new Intent(Intent.ACTION_PICK,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentVideo, REQUEST_CODE_PICK_VIDEO);
                break;
            case R.id.ivClose:
                bottomSheetDialog.dismiss();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAPTURE_IMAGE)//Camera
            {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

                uploadBytes(bytes, Constants.MESSAGE_TYPE_IMAGE);

            } else if (requestCode == REQUEST_CODE_PICK_IMAGE) { //Gallery
                Uri uri = data.getData();
                uploadFile(uri, Constants.MESSAGE_TYPE_IMAGE);
            } else if (requestCode == REQUEST_CODE_PICK_VIDEO)//Video
            {
                Uri uri = data.getData();
                uploadFile(uri, Constants.MESSAGE_TYPE_VIDEO);
            }
            else  if(requestCode==REQUEST_CODE_FORWARD_MESSAGE){

                Intent intent = new Intent( this, ChatActivity.class);
                intent.putExtra(Extras.USER_KEY, data.getStringExtra(Extras.USER_KEY));
                intent.putExtra(Extras.USER_NAME, data.getStringExtra(Extras.USER_NAME));
                intent.putExtra(Extras.PHOTO_NAME, data.getStringExtra(Extras.PHOTO_NAME));

                intent.putExtra(Extras.MESSAGE, data.getStringExtra(Extras.MESSAGE));
                intent.putExtra(Extras.MESSAGE_ID, data.getStringExtra(Extras.MESSAGE_ID));
                intent.putExtra(Extras.MESSAGE_TYPE, data.getStringExtra(Extras.MESSAGE_TYPE));

                startActivity(intent);
                finish();
            }
        }

    }

    private void uploadFile(Uri uri, String messageType) {
        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
        String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? pushId + ".mp4" : pushId + ".jpg";

        StorageReference fileRef = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileRef.putFile(uri);

        uploadProgress(uploadTask, fileRef, pushId, messageType);


    }

    private void uploadBytes(ByteArrayOutputStream bytes, String messageType) {
        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
        String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? pushId + ".mp4" : pushId + ".jpg";

        StorageReference fileRef = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileRef.putBytes(bytes.toByteArray());
        uploadProgress(uploadTask, fileRef, pushId, messageType);
    }


    private void uploadProgress(final UploadTask task, final StorageReference filePath, final String pushId, final String messageType) {

        final View view = getLayoutInflater().inflate(R.layout.file_progress, null);
        final ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
        final TextView tvProgress = view.findViewById(R.id.tvFileProgress);
        final ImageView ivPlay = view.findViewById(R.id.ivPlay);
        final ImageView ivPause = view.findViewById(R.id.ivPause);
        ImageView ivCancel = view.findViewById(R.id.ivCancel);

        ivPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.pause();
                ivPlay.setVisibility(View.VISIBLE);
                ivPause.setVisibility(View.GONE);
            }
        });

        ivPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.resume();
                ivPause.setVisibility(View.VISIBLE);
                ivPlay.setVisibility(View.GONE);
            }
        });

        ivCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.cancel();
            }
        });

        llProgress.addView(view);
        tvProgress.setText(getString(R.string.upload_progress, messageType, "0"));

        task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                pbProgress.setProgress((int) progress);
                tvProgress.setText(getString(R.string.upload_progress, messageType, String.valueOf(pbProgress.getProgress())));

            }
        });

        task.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                llProgress.removeView(view);
                if (task.isSuccessful()) {
                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String downloadUrl = uri.toString();
                            sendMessage(downloadUrl, messageType, pushId);
                        }
                    });
                }
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                llProgress.removeView(view);
                Toast.makeText(ChatActivity.this, getString(R.string.failed_to_upload, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (bottomSheetDialog != null)
                    bottomSheetDialog.show();
            } else {
                Toast.makeText(this, "Permission required to access files", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                finish();
                break;

            default:
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void deleteMessage(final String messageId, final String messageType){

            DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES)
                    .child(currentUserId).child(chatUserId).child(messageId);

            databaseReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if(task.isSuccessful())
                    {
                        DatabaseReference databaseReferenceChatUser = mRootRef.child(NodeNames.MESSAGES)
                                .child(chatUserId).child(currentUserId).child(messageId);

                        databaseReferenceChatUser.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                if(task.isSuccessful())
                                {
                                    Toast.makeText(ChatActivity.this, R.string.message_deleted_successfully, Toast.LENGTH_SHORT).show();
                                    if(!messageType.equals(Constants.MESSAGE_TYPE_TEXT))
                                    {
                                        StorageReference rootRef = FirebaseStorage.getInstance().getReference();
                                        String folder = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;
                                        String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?messageId +".mp4": messageId+".jpg";
                                        StorageReference fileRef = rootRef.child(folder).child(fileName);

                                        fileRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                           if(!task.isSuccessful())
                                           {
                                               Toast.makeText(ChatActivity.this,
                                                       getString(R.string.failed_to_delete_file, task.getException()), Toast.LENGTH_SHORT).show();
                                           }
                                            }
                                        });
                                    }
                                }
                                else
                                {
                                    Toast.makeText(ChatActivity.this, getString( R.string.failed_to_delete_message, task.getException()),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, getString( R.string.failed_to_delete_message, task.getException()),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });




    }

    public  void  downloadFile(String messageId, final String messageType, final boolean isShare){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        else
        {
                String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
                String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?messageId + ".mp4": messageId + ".jpg";

                StorageReference fileRef= FirebaseStorage.getInstance().getReference().child(folderName).child(fileName);
                final String localFilePath = getExternalFilesDir(null).getAbsolutePath() + "/" + fileName;

                File localFile = new File(localFilePath);

                try {
                    if(localFile.exists() || localFile.createNewFile())
                    {
                        final FileDownloadTask downloadTask =  fileRef.getFile(localFile);

                        final View view = getLayoutInflater().inflate(R.layout.file_progress, null);
                        final ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
                        final TextView tvProgress = view.findViewById(R.id.tvFileProgress);
                        final ImageView ivPlay = view.findViewById(R.id.ivPlay);
                        final ImageView ivPause = view.findViewById(R.id.ivPause);
                        ImageView ivCancel = view.findViewById(R.id.ivCancel);

                        ivPause.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                downloadTask.pause();
                                ivPlay.setVisibility(View.VISIBLE);
                                ivPause.setVisibility(View.GONE);
                            }
                        });

                        ivPlay.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                downloadTask.resume();
                                ivPause.setVisibility(View.VISIBLE);
                                ivPlay.setVisibility(View.GONE);
                            }
                        });

                        ivCancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                downloadTask.cancel();
                            }
                        });

                        llProgress.addView(view);
                        tvProgress.setText(getString(R.string.download_progress, messageType, "0"));

                        downloadTask.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                                pbProgress.setProgress((int) progress);
                                tvProgress.setText(getString(R.string.download_progress, messageType, String.valueOf(pbProgress.getProgress())));
                            }
                        });

                        downloadTask.addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                                llProgress.removeView(view);
                                if (task.isSuccessful()) {

                                    if(isShare){
                                        Intent intentShare = new Intent();
                                        intentShare.setAction(Intent.ACTION_SEND);
                                        intentShare.putExtra(Intent.EXTRA_STREAM, Uri.parse(localFilePath));
                                        if(messageType.equals(Constants.MESSAGE_TYPE_VIDEO))
                                            intentShare.setType("video/mp4");
                                        if(messageType.equals(Constants.MESSAGE_TYPE_IMAGE))
                                            intentShare.setType("image/jpg");
                                        startActivity(Intent.createChooser(intentShare, getString(R.string.share_with)));

                                    }
                                    else {
                                        Snackbar snackbar = Snackbar.make(llProgress, getString(R.string.file_downloaded_successfully)
                                                , Snackbar.LENGTH_INDEFINITE);

                                        snackbar.setAction(R.string.view, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                Uri uri = Uri.parse(localFilePath);
                                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                                if (messageType.equals(Constants.MESSAGE_TYPE_VIDEO))
                                                    intent.setDataAndType(uri, "video/mp4");
                                                else if (messageType.equals(Constants.MESSAGE_TYPE_IMAGE))
                                                    intent.setDataAndType(uri, "image/jpg");

                                                startActivity(intent);
                                            }
                                        });


                                        snackbar.show();
                                    }

                                }
                            }
                        });

                        downloadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                llProgress.removeView(view);
                                Toast.makeText(ChatActivity.this, getString(R.string.failed_to_download, e.getMessage()), Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                    else
                    {
                        Toast.makeText(this, R.string.failed_to_store_file, Toast.LENGTH_SHORT).show();
                    }
                }
                catch(Exception ex){
                    Toast.makeText(ChatActivity.this, getString(R.string.failed_to_download, ex.getMessage()), Toast.LENGTH_SHORT).show();
                }

        }
    }

    public void forwardMessage(String selectedMessageId, String selectedMessage, String selectedMessageType) {
        Intent intent = new Intent(this, SelectFriendActivity.class);
        intent.putExtra(Extras.MESSAGE, selectedMessage);
        intent.putExtra(Extras.MESSAGE_ID, selectedMessageId);
        intent.putExtra(Extras.MESSAGE_TYPE, selectedMessageType);
        startActivityForResult(intent , REQUEST_CODE_FORWARD_MESSAGE);
    }

    @Override
    public void onBackPressed() {
        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);
        super.onBackPressed();

    }


    private void showSmartReplies(final MessageModel messageModel){

        conversation.clear();
        cgSmartReplies.removeAllViews();

        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);
        Query lastQuery = databaseReference.orderByKey().limitToLast(1);

        lastQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()){
                    MessageModel message = data.getValue(MessageModel.class);

                    if(message.getMessageFrom().equals(chatUserId) && messageModel.getMessageId().equals(message.getMessageId())
                            && message.getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)){
                        conversation.add(TextMessage.createForRemoteUser(message.getMessage(), System.currentTimeMillis(), chatUserId));

                        if(!conversation.isEmpty()){
                            // generating the smart replies using the smart reply generator
                            SmartReplyGenerator smartReply = SmartReply.getClient();
                            smartReply.suggestReplies(conversation)
                                    .addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
                                        @Override
                                        public void onSuccess(SmartReplySuggestionResult result) {
                                            if(result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE){
                                                Toast.makeText(ChatActivity.this, "Language Not supported", Toast.LENGTH_SHORT).show();
                                            }
                                            else if(result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS){
                                                for(SmartReplySuggestion suggestion : result.getSuggestions()){
                                                    String replyText = suggestion.getText();

                                                    Chip chip = new Chip(ChatActivity.this);
                                                    ChipDrawable drawable = ChipDrawable.createFromAttributes(ChatActivity.this,
                                                            null, 0, R.style.Widget_MaterialComponents_Chip_Action);

                                                    chip.setChipDrawable(drawable);
                                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                                    params.setMargins(16,16,16,16);
                                                    chip.setLayoutParams(params);
                                                    chip.setText(replyText);
                                                    chip.setTag(replyText);

                                                    // apply onclick listener to the chip so that whenever user clicks on the CHIP, it will
                                                    // send as the "Reply Message"
                                                    chip.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES)
                                                                    .child(currentUserId).child(chatUserId).push();

                                                            String newMessageId = messageRef.getKey();
                                                            sendMessage(v.getTag().toString(), Constants.MESSAGE_TYPE_TEXT, newMessageId);
                                                        }
                                                    });
                                                    cgSmartReplies.addView(chip);
                                                }
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(ChatActivity.this, "Something went wrong : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }
}











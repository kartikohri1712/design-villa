package udacity.designvilla;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.splashscreenjava.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;

import io.github.kbiakov.codeview.CodeView;

public class DesignDetails extends AppCompatActivity {

    private FirebaseStorage firebaseStorage;
    private FirebaseUser user;
    private FirebaseDatabase database;
    private DatabaseReference reference;
    private CodeView codeView;
    String layoutUID;
    private boolean isFavourite =false;
    private Menu menu;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_design_details);

        Toolbar mToolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(mToolbar);

        layoutUID = getIntent().getStringExtra("layout_uid");

        String text = getIntent().getStringExtra("title") + "-" + getIntent().getStringExtra("author");
        getSupportActionBar().setTitle(text);

        firebaseStorage = FirebaseStorage.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference().child("favourites").child(user.getUid());

        checkFavourite();

        codeView = findViewById(R.id.code_view);
        Glide.with(getApplicationContext())
                .load(Uri.parse(getIntent().getStringExtra("image_url")))
                .into((ImageView) findViewById(R.id.design_image));
        StorageReference storageReference = firebaseStorage.getReferenceFromUrl(getIntent().getStringExtra("xml"));
        Log.d("Storage Reference",storageReference.getName());
        try {
            final File localFile = File.createTempFile("layout", "xml");
            Toast.makeText(this, localFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            storageReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(localFile));
                        String line,code="";
                        while ((line = reader.readLine())!=null){
                            code = code + line + "\n";
                        }
                        reader.close();
                        setCodeViewText(code);
                        System.out.println(code);
                    }catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error in", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e("firebase ",";local tem file not created  created " +exception.toString());
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "Error Out", Toast.LENGTH_SHORT).show();
        }
    }
    public void setCodeViewText(String code){
        codeView.setCode(code,"xml");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_favourites,menu);
        menu.getItem(0).setShowAsAction(1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.save_to_favourites:
                item.setIcon(R.drawable.ic_star);
                addToFavourites();
        }
        return true;
    }

    public void addToFavourites(){
        reference.push().setValue(layoutUID).addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(getApplicationContext(),"Added to favourites",Toast.LENGTH_SHORT).show();
                }else {
                    Log.e("Firebase Error",task.getException().toString());
                }
            }
        });
    }

    public void checkFavourite(){
        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot:dataSnapshot.getChildren()){
                    String id = snapshot.getValue().toString();
                    if(id.equals(layoutUID)){
                        setFavourite(true);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        reference.addListenerForSingleValueEvent(eventListener);
    }
    public void setFavourite(boolean b){
        isFavourite = b;
        menu.getItem(0).setIcon(R.drawable.ic_star);
        menu.getItem(0).setEnabled(false);
    }
}
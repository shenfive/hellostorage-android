package android.idv.sjw.hellostorage;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mPicDataReference;
    private FirebaseStorage mFirebaseStorage;

    ProgressBar progressBar;
    Button button;
    ImageView imageView;
    GridView gridView;
    ArrayList<PictureItem> pictureItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化
        button = (Button)findViewById(R.id.button2);
        imageView = (ImageView)findViewById(R.id.imageView);
        gridView = (GridView)findViewById(R.id.gridView);
        progressBar = (ProgressBar)findViewById(R.id.progressBar2);
        pictureItems = new ArrayList<>();
        progressBar.setVisibility(View.INVISIBLE);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // 匿名登入 Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseAuth.signInAnonymously();
        mFirebaseAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (mFirebaseAuth.getCurrentUser() != null){
                    // 取得 pic 資料庫參考位置
                    mPicDataReference = FirebaseDatabase.getInstance().getReference().child(mFirebaseAuth.getCurrentUser().getUid());
                    Log.d("user ID",mFirebaseAuth.getCurrentUser().getUid());
                }
            }
        });
        mFirebaseStorage = FirebaseStorage.getInstance();


        //擷取照片按鈕監聽器
        button.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                //讀取圖片
                Intent intent = new Intent();
                //開啟Pictures畫面Type設定為image
                intent.setType("image/*");
                //使用Intent.ACTION_GET_CONTENT這個Action
                intent.setAction(Intent.ACTION_GET_CONTENT);
                //取得照片後返回此畫面
                startActivityForResult(intent, 0);
            }

        });

        //測試讀檔案
        File imagefile = new File(getApplicationContext().getFilesDir(),"temp2.jpg");
        try {
            InputStream inputStream = new FileInputStream(imagefile);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imageView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }



        StorageReference mountainsRef = mFirebaseStorage
                .getReference()
                .child("pic")
                .child("test.jpg");

//        //下載到
//        File localFile = new File(getApplicationContext().getFilesDir(),"temp2.jpg");
//        FileDownloadTask task = mountainsRef.getFile(localFile);
//        task.addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                // 下載成功後續
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception exception) {
//                // 下載失敗處理
//            }
//        });


          //下載到 記憶體
//        final long ONE_MEGABYTE = 1024 * 1024;
//        mountainsRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
//            @Override
//            public void onSuccess(byte[] bytes) {
//                // Data for "images/island.jpg" is returns, use this as needed
//                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                imageView.setImageBitmap(bitmap);
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception exception) {
//                // Handle any errors
//            }
//        });

//         //由 URL 下載
//        mountainsRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//            @SuppressLint("StaticFieldLeak")
//            @Override
//            public void onSuccess(Uri uri) {
//                String path = uri.toString();
//                DownloadImageTask downloadImageTask = new DownloadImageTask();
//                downloadImageTask.execute(path);
//            }
//        });

        //Firebase UI
        FirebaseImageLoader firebaseImageLoader= new FirebaseImageLoader();
        Glide.with(this /* context */)
                .using(firebaseImageLoader)
                .load(mountainsRef)
                .into(imageView);

    }

    private class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {

        protected Bitmap doInBackground(String... strings) {
            File file = new File(getCacheDir(),"temp1.jpg");
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                URL url = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
            saveImageToInternalStorage(bitmap);
        }
    }


    public boolean saveImageToInternalStorage(Bitmap image) {
        try {
            FileOutputStream fos = MainActivity.this.openFileOutput("temp.jpg", Context.MODE_PRIVATE);
            image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return true;
        } catch (Exception e) {
            Log.e("saveToInternalStorage()", e.getMessage());
            return false;
        }
    }


    class DownloadFileFromURL extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // 放入開始下載的訊息
        }

        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
            // 放入完成下載的訊息
        }

        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                String filename = "test_filename.test";
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);
                // Output stream
                OutputStream output = new FileOutputStream(Environment
                        .getExternalStorageDirectory().toString()
                        + filename);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return null;
        }
    }




        @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //當使用者按下確定後
        if (resultCode == RESULT_OK) {
            //取得圖檔的路徑位置
            Uri uri = data.getData();
//            //寫log
//            Log.e("uri", uri.toString());
            //抽象資料的接口
            ContentResolver cr = this.getContentResolver();
            try {
                //由抽象資料接口轉換圖檔路徑為Bitmap
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));

                // 將Bitmap設定到ImageView
                imageView.setImageBitmap(bitmap);
                PictureItem pictureItem = new PictureItem(bitmap);
                pictureItems.add(pictureItem);
                MyPictureListAdapter myPictureListAdapter = new MyPictureListAdapter();
                myPictureListAdapter.pictureItems = pictureItems;
                gridView.setAdapter(myPictureListAdapter);

                //使用 UUID
                String str= UUID.randomUUID().toString();
                String fileName=str.substring(0, 8) + str.substring(9, 13) + str.substring(14, 18) + str.substring(19, 23) + str.substring(24);
                Log.d("the uuid",fileName);


                ContextWrapper cw = new ContextWrapper(getApplicationContext());
                // path to /data/data/yourapp/app_data/imageDir
                File directory = cw.getDir("imagTemp", Context.MODE_PRIVATE);


                File outFile =  new File(directory,"x_test.jpg") ;//创建压缩后的image文件


                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] imageData = baos.toByteArray();

                StorageMetadata storageMetadata = new StorageMetadata
                        .Builder()
                        .setCustomMetadata("MyKey","MyValue")
                        .setCustomMetadata("data2","data2")
                        .build();

                StorageReference mountainsRef = mFirebaseStorage
                        .getReference()
                        .child("pic")
                        .child("test.jpg");
                        //.child(mFirebaseAuth.getCurrentUser().getUid())
                        //.child(fileName+"_test.jpg");

                UploadTask uploadTask = mountainsRef.putBytes(imageData,storageMetadata);
                progressBar.setVisibility(View.VISIBLE);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    //監聽上傳成功訊息
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        
                        Toast.makeText(MainActivity.this,"上傳成功!",Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    //監聽上傳失敗訊息
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(MainActivity.this,"上傳成!",Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    //監聽上傳過程訊息
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        int progressPersentage = (int)((taskSnapshot.getBytesTransferred()*100)/(taskSnapshot.getTotalByteCount()));
                        Log.d("uploading",progressPersentage+"%");
                        progressBar.setProgress(progressPersentage);
                    }
                }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    }
                });

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    //Gird View 用的 Adapter

    private class MyPictureListAdapter extends BaseAdapter{
        public ArrayList<PictureItem> pictureItems;

        @Override
        public int getCount() {
            return pictureItems.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater mInflater = getLayoutInflater();
            View myView = mInflater.inflate(R.layout.image_grid,null);
            final PictureItem pictureItem = pictureItems.get(position);

            ImageView imageView = (ImageView)myView.findViewById(R.id.imageView2);
            imageView.setImageBitmap(pictureItem.bitmap);

            return myView;
        }
    }

}

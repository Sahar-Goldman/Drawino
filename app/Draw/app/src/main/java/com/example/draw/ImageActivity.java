package com.example.draw;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.Manifest;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class ImageActivity extends AppCompatActivity {

    private FloatingActionButton btn;
    private ImageView imageview;
    private int GALLERY = 1;
    private int BL = 2;
    private boolean insert_image;
    private boolean print;
    private Bitmap bitmap, blake_bitmap, copy_blake_bitmap;
    private String email;
    private static Uri contentURI;
    private GridView imageGrid;
    private ArrayList<Bitmap> images;
    private GridAdapter gridAdapter;
    private ProgressDialog progress;
    private String file_path;
    private boolean servo_start;

    private List<String> list_for_print ;
    BluetoothAdapter myBluetooth = null;
    public static BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");
    static String address = "98:D3:31:F5:2D:64";
    private byte[] mmBuffer;

    public MenuItem _item;

    //TODO: change to your Connection String
    public static final String storageConnectionString =
            "DefaultEndpointsProtocol=https;" +
                    "AccountName=???;" +
                    "AccountKey=???";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        SharedPreferences login = getSharedPreferences("login", MODE_PRIVATE);
        email = login.getString("email","");

        btn = (FloatingActionButton) findViewById(R.id.btn);
        imageview = (ImageView) findViewById(R.id.iv);
        imageGrid = (GridView) findViewById(R.id.grid);
        images = new ArrayList<Bitmap>();
        gridAdapter = new GridAdapter(ImageActivity.this, images);
        imageGrid.setAdapter(gridAdapter);

        if (savedInstanceState == null) {
            print = false;
            insert_image = false;
            bitmap = null;
        }

        imageview.setVisibility(View.GONE);

        new GetMyImages().execute();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askPermission();
            }
        });

        imageGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                blake_bitmap = images.get(position);
                imageview.setImageBitmap(blake_bitmap);
                imageview.setVisibility(View.VISIBLE);
                insert_image = true;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.image_menu, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.print:
                _item = item;
                if(!print){
                    if(!insert_image){
                        Toast.makeText(ImageActivity.this, "Chose a image first", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        print = true;

                        //for bluetooth
                        //if the device has bluetooth
                        myBluetooth = BluetoothAdapter.getDefaultAdapter();

                        if(myBluetooth == null)
                        {
                            //Show a message. that the device has no bluetooth adapter
                            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();

                            //finish apk
                            finish();
                        }
                        else if(!myBluetooth.isEnabled())
                        {
                            //Ask to the user turn the bluetooth on
                            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(turnBTon,BL);
                        }
                        else {
                            new ImageActivity.ConnectBT().execute(); //Call the class to connect
                        }

                    }
                }
                else {
                    item.setIcon(R.drawable.check);
                    print = false;
                    insert_image = false;
                    list_for_print.clear();
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if( requestCode == GALLERY) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, GALLERY);

            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == this.RESULT_CANCELED) {
            finish();
        }
        if (requestCode == BL) {
            new ImageActivity.ConnectBT().execute(); //Call the class to connect
        }

        if (requestCode == GALLERY) {
            if (data != null) {
                contentURI = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    progress = ProgressDialog.show(ImageActivity.this, "Proses image...", "Please wait");
                    //loading.setVisibility(View.VISIBLE);
                    imageview.setVisibility(View.GONE);

                    //Thread for convert picture to black and white
                    new Thread(new Runnable() {
                        public void run() {
                            while(bitmap == null){} // wait for upload picture
                            blake_bitmap = ConvertToBlackAndWhite(bitmap);
                            progress.dismiss();
                            insert_image = true;
                            print = false;
                            new UploadImage().execute();

                            //displays the converted image on the screen
                            imageview.post(new Runnable() {
                                public void run() {
                                    imageview.setImageBitmap(blake_bitmap);
                                    //loading.setVisibility(View.GONE);
                                    imageview.setVisibility(View.VISIBLE);
                                    images.add(blake_bitmap);
                                    gridAdapter.notifyDataSetChanged();
                                }
                            });


                        }
                    }).start();
                    //End thread

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ImageActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    private void askPermission(){
        if (ActivityCompat.checkSelfPermission(ImageActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(ImageActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ActivityCompat.requestPermissions(ImageActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        GALLERY);
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(ImageActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        GALLERY);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, GALLERY);
        }
    }





    public Bitmap ConvertToBlackAndWhite(Bitmap original){
        int width = original.getWidth();
        int height = original.getHeight();
        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(width, height, original.getConfig());
        // color information
        int A, R, G, B;
        int pixel;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // get pixel color
                pixel = original.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                int gray = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);
                // use 120 as threshold, above -> white, below -> black
                if (gray < 120) {
                    gray = Color.BLACK;
                }
                else{
                    gray = Color.WHITE;
                }
                // set new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray));
            }
        }
        return bmOut;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new  Intent(ImageActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }







    /* ----------------------- upload and get image from azure blob -------------------------*/


    static String randomString( int len ){
        String validChars = "abcdefghijklmnopqrstuvwxyz";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( validChars.charAt( rnd.nextInt(validChars.length()) ) );

        return sb.toString();
    }


    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        file_path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(file_path);
    }

    public void uploadImage() {
        try {
            // Retrieve storage account from connection-string
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

            // Create the blob client
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

            // Get a reference to a container
            // The container name must be lower case
            String containerName = email.replaceAll("[^a-zA-Z1-9]","").toLowerCase();
            CloudBlobContainer container = blobClient.getContainerReference(containerName);

            // Create the container if it does not exist
            container.createIfNotExists();

            // Create a permissions object
            BlobContainerPermissions containerPermissions = new BlobContainerPermissions();

            // Include public access in the permissions object
            containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);

            // Set the permissions on the container
            container.uploadPermissions(containerPermissions);


            String imageName = randomString(10);
            CloudBlockBlob blob = container.getBlockBlobReference(imageName);
            Uri uri = getImageUri(ImageActivity.this,blake_bitmap);
            InputStream imageStream = getContentResolver().openInputStream(uri);
            int imageLength = imageStream.available();
            blob.upload(imageStream,imageLength);
            getContentResolver().delete(uri, null,null);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void GetImage(String name, OutputStream imageStream, long imageLength) throws Exception {
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        String containerName = email.replaceAll("[^a-zA-Z1-9]","").toLowerCase();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);

        CloudBlockBlob blob = container.getBlockBlobReference(name);

        if(blob.exists()){
            blob.downloadAttributes();
            imageLength = blob.getProperties().getLength();
            blob.download(imageStream);
        }
    }

    public void ListImages() throws Exception{

        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        String containerName = email.replaceAll("[^a-zA-Z1-9]","").toLowerCase();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        container.createIfNotExists();

        Iterable<ListBlobItem> blobs = container.listBlobs();

        for(ListBlobItem blob: blobs) {
            final ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
            GetImage(((CloudBlockBlob) blob).getName(), imageStream, 0);
            byte[] buffer = imageStream.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
            images.add(bitmap);


            new Thread(new Runnable() {
                public void run() {
                    //displays the converted image on the screen
                    imageGrid.post(new Runnable() {
                        public void run() {
                            gridAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }
    }



    class GetMyImages extends AsyncTask<Void, Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                ListImages();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(ImageActivity.this, "Loading images...", "Please wait, this can take a while");
        }
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            progress.dismiss();
        }

    }


    class UploadImage extends AsyncTask<Void, Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            uploadImage();
            return null;
        }

        @Override
        protected void onPreExecute()
        {
        }
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
        }

    }



    /* ----------------------- send bitmap to print list -------------------------*/

    private void print(){
        list_for_print = new LinkedList<String>();

        copy_blake_bitmap = blake_bitmap.copy(blake_bitmap.getConfig(), true);
        copy_blake_bitmap = scaleBitmap(copy_blake_bitmap,300,300);

        int width = copy_blake_bitmap.getWidth() ;
        int hight = copy_blake_bitmap.getHeight();

        float adjusted ;
        if(width > hight)
            adjusted = width/30 ;
        else
            adjusted = hight/15 ;

        servo_start = false ;
        for (int y = 0 ; y < hight; y++) {
            for (int x = 0; x < width; x++) {
                if(copy_blake_bitmap.getPixel(x,y) <= Color.DKGRAY ){
                    find_path( x, y, adjusted);
                    if(servo_start){
                        list_for_print.add("_");
                        servo_start = false;
                    }
                }

            }
            if(servo_start){
                list_for_print.add("_");
                servo_start = false;
            }
        }
    }

    private void find_path( int x, int y, float adjusted) {
        if( !isInBound(x,y,copy_blake_bitmap) ) {
            return;
        }
        if(copy_blake_bitmap.getPixel(x,y) == Color.WHITE){
            return;
        }

        if(copy_blake_bitmap.getPixel(x,y) <= Color.DKGRAY){
            float x_bord = 30 + (x/adjusted) ;
            float y_bord = 10 + (y/adjusted) ;

            list_for_print.add(x_bord + "," + y_bord + "@");
            copy_blake_bitmap.setPixel(x,y,Color.WHITE);

            if(!servo_start){
                list_for_print.add("-");
                servo_start = true;
            }
            copy_blake_bitmap.setPixel(x,y,Color.WHITE);

            if(isBlackNear(x,y,copy_blake_bitmap) ){
                find_path(x, y-1,adjusted);

                find_path(x+1, y-1,adjusted);
                find_path( x+1, y, adjusted);
                find_path(x+1, y+1,adjusted);

                find_path(x, y+1, adjusted);

                find_path(x-1, y+1,adjusted);
                find_path(x-1, y,adjusted);
                find_path(x-1, y-1,adjusted);

             }
            else{
                if(servo_start){
                    list_for_print.add("_");
                    servo_start = false ;
                }
            }
        }
    }

    private boolean isInBound(int x, int y, Bitmap image) {
        boolean bol = false;
        if(x < image.getWidth() && x >= 0 && y < image.getHeight() && y >= 0){
            bol = true;
        }
        return bol;
    }


    private boolean isBlackNear(int x, int y, Bitmap image){

        if( isInBound(x+1, y, image)  && image.getPixel(x+1,y) <= Color.DKGRAY)
            return  true;
        if( isInBound(x+1, y+1, image)  && image.getPixel(x+1,y+1) <= Color.DKGRAY)
            return  true;
        if( isInBound(x+1, y-1, image)  && image.getPixel(x+1,y-1) <= Color.DKGRAY)
            return  true;
        if( isInBound(x-1, y, image)  && image.getPixel(x-1,y) <= Color.DKGRAY)
            return  true;
        if( isInBound(x-1, y+1, image)  && image.getPixel(x-1,y+1) <= Color.DKGRAY)
            return  true;
        if( isInBound(x-1, y-1, image)  && image.getPixel(x-1,y-1) <= Color.DKGRAY)
            return  true;
        if( isInBound(x, y+1, image)  && image.getPixel(x,y+1) <= Color.DKGRAY)
            return  true;
        if( isInBound(x, y-1, image)  && image.getPixel(x,y-1) <= Color.DKGRAY)
            return  true;

        return false;
    }

    private Bitmap scaleBitmap(Bitmap bm, int maxWidth, int maxHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        if (width > height) {
            // landscape
            float ratio = (float) width / maxWidth;
            width = maxWidth;
            height = (int)(height / ratio);
        } else if (height > width) {
            // portrait
            float ratio = (float) height / maxHeight;
            height = maxHeight;
            width = (int)(width / ratio);
        } else {
            // square
            height = maxHeight;
            width = maxWidth;
        }

        bm = Bitmap.createScaledBitmap(bm, width, height, true);
        return bm;
    }


    /* ----------------------- for bluetooth -------------------------*/
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(ImageActivity.this, "Connecting...", "Please wait.");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed");
                print = false;
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
                print();
                _item.setIcon(R.drawable.ic_cancel_black_24dp);

                mmBuffer = new byte[1];

                //thread for send commend to Arduino
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            while(true) {
                             //   android.os.Debug.waitForDebugger();
                                if(btSocket != null && !list_for_print.isEmpty()){
                                    btSocket.getInputStream().read(mmBuffer);
                                    if(mmBuffer[0]=='!'){
                                        btSocket.getOutputStream().write((list_for_print.get(0)).getBytes());
                                        list_for_print.remove(0);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };

                thread.start();
            }
            progress.dismiss();
        }
    }

}

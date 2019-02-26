package com.hbindustries.hotdognothotdog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


// Guide from:
// https://developer.android.com/training/camera/photobasics#java


public class MainActivity extends AppCompatActivity {

    static final int REQUEST_TAKE_PHOTO = 1;
    ImageView imageView;
    Button btnCamera;
    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageView);
        btnCamera = (Button) findViewById(R.id.btnCamera);

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // Ensure that there's an activity availablt to handle the intent
                if (intent.resolveActivity(getPackageManager()) != null) {

                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred when creating file
                    }

                    // Continue if file was created
                    if (photoFile != null) {
                        Uri photoUri = FileProvider.getUriForFile(getApplicationContext(), "com.example.android.fileprovider", photoFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                    }
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Image View dimensions
        final int viewWidth = imageView.getWidth()/2;
        final int viewHeight = imageView.getHeight()/2;

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            try {

                // Retrieve file info
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File f = new File(currentPhotoPath);
                Uri imageUri = Uri.fromFile(f);

                // Broadcast image gallery update
                mediaScanIntent.setData(imageUri);
                this.sendBroadcast(mediaScanIntent);


                // Discover image orietnation
                ExifInterface ei = new ExifInterface(f.getPath());
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);


                // See https://developer.android.com/topic/performance/graphics/load-bitmap
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
                int imageHeight = bmOptions.outHeight;
                int imageWidth = bmOptions.outWidth;
                String imageType = bmOptions.outMimeType;

                // Scale image to imageView
                bmOptions.inSampleSize =  calculateInSampleSize(bmOptions, viewWidth, viewHeight);
                bmOptions.inJustDecodeBounds = false;
                Bitmap scaledBitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

                switch(orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        scaledBitmap = rotateImage(scaledBitmap, 90);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        scaledBitmap = rotateImage(scaledBitmap, 180);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        scaledBitmap = rotateImage(scaledBitmap, 270);
                        break;

                    case ExifInterface.ORIENTATION_NORMAL:
                    default:
                        ; // Do nothing
                }

                // Display image
                imageView.setImageBitmap(scaledBitmap);

            } catch (Exception ex) {
                Log.e("HotdogNotHotdog", "Exception: " + ex.toString());
            }

        }

    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}

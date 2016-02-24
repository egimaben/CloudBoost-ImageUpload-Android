package com.example.cloudboostimageapp;

import io.cloudboost.CloudApp;
import io.cloudboost.CloudException;
import io.cloudboost.CloudFile;
import io.cloudboost.CloudFileCallback;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
TextView info;
ImageView image;
private static final int SELECT_PICTURE = 0;
private static String CLOUDBOOST_IMAGE_URL;
// Progress dialog type (0 - for Horizontal progress bar)
public static final int PROGRESS_BAR = 0; 
private ProgressDialog pDialog;
private static String DIALOG_MESSAGE;
private static boolean UPLOAD=true;
public static String FILE_LOCATION;
private static int RESULT_LOAD_IMG = 1;


private static File imageFile;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CloudApp.init("bengi123", "mLiJB380x9fhPRCjCGmGRg==");
		setContentView(R.layout.activity_main);
		info=(TextView) findViewById(R.id.info);
		image=(ImageView) findViewById(R.id.image);
	}
	public void fetchImage(View view){
		// Create intent to Open Image applications like Gallery, Google Photos
		Intent galleryIntent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		// Start the Intent
		startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
	}
	public void uploadImage(View view){
		new uploadTask().execute(new String[]{});
	}
	public void downloadImage(View view){
		new downloadTask().execute(new String[]{});
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if(requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK
				&& null != data) {
    		Bitmap bitmap=getImageFromPath(data.getData());
	    		Log.d("ACTIVITRESULT", "bitmap:"+bitmap);


    		imageFile=bitmapToFile(bitmap, "cloudboostimage");
    		if(imageFile!=null){
    			info.setText(imageFile.getPath());
    		}
    		else Toast.makeText(this, "Failed to get image", Toast.LENGTH_SHORT).show();
//    		imageView.setImageBitmap(bitmap);
    	}
    }
    private Bitmap getImageFromPath(Uri selectedImage) {

        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        // Get the cursor
        Cursor cursor = getContentResolver().query(selectedImage,
                filePathColumn, null, null, null);
        // Move to first row
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String imgDecodableString = cursor.getString(columnIndex);
        cursor.close();
        Bitmap bitmap=BitmapFactory
                .decodeFile(imgDecodableString);
		
		return bitmap;
	}
    private File bitmapToFile(Bitmap bitmap,String fileName){
    	File filesDir = getApplicationContext().getFilesDir();
    	File imageFile = new File(filesDir, fileName + ".jpg");
    	  OutputStream os;
    	  try {
    	    os = new FileOutputStream(imageFile);
    	    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
    	    os.flush();
    	    os.close();
    	    return imageFile;
    	  } catch (Exception e) {
    	    Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
    	  }
    	  return null;
    	
    }
    /**
     * background task to do file download
     * 
     * @author new
     *
     */
	class downloadTask extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread
		 * Show Progress Bar Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			UPLOAD=false;
			DIALOG_MESSAGE="downloading from CloudBoost";
			pDialog=new ProgressDialog(MainActivity.this);
			pDialog.setMax(100);
			pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pDialog.setMessage(DIALOG_MESSAGE);
			pDialog.setCancelable(true);
			pDialog.setIndeterminate(false);
			pDialog.show();
//			showDialog(PROGRESS_BAR);
		}

		/**
		 * Downloading file in background thread
		 * */
		@Override
		protected String doInBackground(String... f_url) {
			int count;
	        try {
	        	
	           ContextWrapper cw = new ContextWrapper(getApplicationContext());
	            // path to /data/data/yourapp/app_data/imageDir
	           File directory = cw.getDir("CloudBoost", Context.MODE_PRIVATE);
	           // Create imageDir
	           File mypath=new File(directory,"cloudboostimage.jpg");
	           if(CLOUDBOOST_IMAGE_URL==null)
	           {
	        	   Toast.makeText(MainActivity.this, "No image URL found", Toast.LENGTH_SHORT).show();
	        	   return null;
	           }
	           FILE_LOCATION=mypath.getPath();

	            URL url = new URL(CLOUDBOOST_IMAGE_URL);
	            URLConnection conection = url.openConnection();
	            conection.connect();
	            // getting file length
	            int lenghtOfFile = conection.getContentLength();

	            // input stream to read file - with 8k buffer
	            InputStream input = new BufferedInputStream(url.openStream(), 8192);
	            
	            // Output stream to write file
	            OutputStream output = new FileOutputStream(mypath);

	            byte data[] = new byte[1024];

	            long total = 0;

	            while ((count = input.read(data)) != -1) {
	                total += count;
	                // publishing the progress....
	                // After this onProgressUpdate will be called
	                publishProgress(""+(int)((total*100)/lenghtOfFile));
	                
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
		
		/**
		 * Updating progress bar
		 * */
		protected void onProgressUpdate(String... progress) {
			// setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
       }

		/**
		 * After completing background task
		 * Dismiss the progress dialog
		 * **/
		@Override
		protected void onPostExecute(String file_url) {
			// dismiss the dialog after the file was downloaded
			pDialog.dismiss();
			   try {
			        File f=new File(FILE_LOCATION);
			        Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
			        image.setImageBitmap(b);
			    } 
			    catch (FileNotFoundException e) 
			    {
			        e.printStackTrace();
			    }
		}

	}
	
    /**
     * background task to do file upload
     * 
     * @author new
     *
     */
	class uploadTask extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread
		 * Show Progress Bar Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			UPLOAD=true;
			DIALOG_MESSAGE="uploading to CloudBoost";
			showDialog(PROGRESS_BAR);
		}

		/**
		 * uploading file in background thread
		 * */
		@Override
		protected String doInBackground(String... filename) {
			if(imageFile==null)
			{
				Toast.makeText(MainActivity.this, "No image file selected", Toast.LENGTH_SHORT).show();
				return null;
			}
	        try {
	        	CloudFile file=new CloudFile(imageFile);
	        	file.save(new CloudFileCallback() {
					
					@Override
					public void done(CloudFile arg0, CloudException arg1) throws CloudException {
						if(arg0!=null){
						Log.d("UPLOAD TASK","file upload complete url="+arg0.getFileUrl());
						CLOUDBOOST_IMAGE_URL=arg0.getFileUrl();
						runOnUiThread(new Runnable() {
							public void run() {
								info.setText(CLOUDBOOST_IMAGE_URL);
							}
						});
						}
						else Log.d("UPLOAD TASK", "File upload failed:"+arg1.getMessage());
						
					}
				});
	            
	        } catch (Exception e) {
	        	Log.e("Error: ", e.getMessage());
	        }
	        
	        return null;
		}
		
		/**
		 * Updating progress bar
		 * */
		protected void onProgressUpdate(String... progress) {
			// setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
       }

		/**
		 * After completing background task
		 * Dismiss the progress dialog
		 * **/
		@Override
		protected void onPostExecute(String file_url) {
			// dismiss the dialog after the file was downloaded
			dismissDialog(PROGRESS_BAR);
		}

	}
	/**
	 * Showing Dialog
	 * */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_BAR:
			pDialog = new ProgressDialog(this);
			pDialog.setMessage(DIALOG_MESSAGE);
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
			return pDialog;
		default:
			return null;
		}
	}

}

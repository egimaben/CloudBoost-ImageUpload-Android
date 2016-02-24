### CloudBoost-ImageUpload-Android
Upload and download images on android with CloudBoost using the JavaSDK
In this tutorial, we are going to create a simple android app that does 5 things:
<ul>
<li>Picks an image from the phone gallery</li>
<li>Uploads this image to CloudBoost</li>
<li>Downloads the image from the cloudboost</li>
<li>saves the downloaded image to internal storage</li>
<li>fetches the image from internal storage and shows it in an imageview</li>
</ul>
###Tools used
<ul>
<li>eclipse indigo 3.7 with ADT 23.0.6</li>
<li>CloudBoost JavaSDK</li>
<li>okhttp-2.4.0</li>
<li>okhttp-ws-2.4.0</li>
<li>okio-1.4</li>
<li>socket.io client for java</li>
</ul>
All the jars listed above are available to you in the libs folder when you clone the SDK repo.

###create the app
We want a simple interface so that we focus on the core 5 items listed above, so lets take a look at our main activity screen and understand the different views used.
<p>&nbsp;</p>
<img class="center-img" alt="main activity screen" src="https://github.com/egimaben/CloudBoost-ImageUpload-Android/blob/master/main_activity_screen.PNG">
<p>&nbsp;</p>
<ul>
<li>This is just a TextView with static text declared in the activity_main.xml to act as the heading for our app.</li>
<li>This is an empty ImageView area. When we pick our image from internal storage in item 5 of our specs, we shall set it to appear in this area.</li>
<li>Another TextView, but this time its not static. We set text to it according to what stage we are at. At the beginning, it indicates "no image selected", when you have selected an image from gallery, its text changes to the URL of the selected file on the local file system. When you successfully upload the image to CloudBoost, its text changes to the URL of the image on cloudboost. We may also use it to show any error messages.</li>
<li>Clicking this button calls the fetchImage() method which creates an intent to open the gallery so that the user can select an image of his choice, after selecting, we are brought back to this screen.</li>
<li>Clicking this button uploads the image selected to cloudboost.</li>
<li>Clicking this button downloads the uploaded image from cloudboost, and sets it to area #2 which contains an ImageView.</li>
</ul>

####activity_main.xml
Lets take a look at the layout xml that gives rise to this screen.
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:layout_width="fill_parent"
    android:layout_height="fill_parent"
    >
<TextView  
    android:layout_width="fill_parent" 
    android:layout_height="wrap_content" 
    android:text="Upload and download image"
    />
	<ImageView android:id="@+id/image" android:layout_width="fill_parent"
		android:layout_height="wrap_content" android:layout_weight="1" />
	<TextView  
	android:id="@+id/info"
    android:layout_width="fill_parent" 
    android:layout_height="wrap_content" 
    android:text="no image selected"
    />
	<LinearLayout
    android:orientation="horizontal"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    >
	<Button android:text="pick image" android:layout_width="fill_parent"
		android:layout_height="wrap_content" android:onClick="fetchImage" />
	<Button android:text="Upload image" android:layout_width="fill_parent"
		android:layout_height="wrap_content" android:onClick="uploadImage" />
	</LinearLayout>
		<Button android:text="Download from cloudboost" android:layout_width="fill_parent"
		android:layout_height="wrap_content" android:onClick="downloadImage" />
</LinearLayout>
```
It's pretty self explanatory, so we shall look at the MainActivity.java class.

MainActivity
In the onCreate method, we have to initialize our CloudApp with app_id and client_key, otherwise the SDk will tell use "app_id not found".
```java
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CloudApp.init("bengi123", "mLiJB380x9fhPRxnmalk==");
		setContentView(R.layout.activity_main);
		info=(TextView) findViewById(R.id.info);
		image=(ImageView) findViewById(R.id.image);
	}
```
Please replace the ##app_id## and ##client_key## with your own from the dashboard.

###Fetching Image from gallery
<p>&nbsp;</p>
<img class="center-img" alt="fetch from gallery" src="https://github.com/egimaben/CloudBoost-ImageUpload-Android/blob/master/select_image.PNG">
<p>&nbsp;</p>
This stage needs 2 methods, namely:
```java
	public void fetchImage(View view){
		// Create intent to Open Image applications like Gallery, Google Photos
		Intent galleryIntent = new Intent(Intent.ACTION_PICK,				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		// Start the Intent
		startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
	}
```
This method creates an intent to select an image in the gallery and calls startActivityForResult.

When the selection is completed, onActivityResult is called, so you have to override it as well
```java
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
```
<p>&nbsp;</p>
<img class="center-img" alt="selecte state" src="https://github.com/egimaben/CloudBoost-ImageUpload-Android/blob/master/image_selected.PNG">
<p>&nbsp;</p>
###uploading image to cloudboost
<p>&nbsp;</p>
<img class="center-img" alt="image uploading" src="https://github.com/egimaben/CloudBoost-ImageUpload-Android/blob/master/uploading_to.PNG">
<p>&nbsp;</p>
We create an async task to perform this operation in a background thread:
```java
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
```

<p>&nbsp;</p>
<img class="center-img" alt="finished uploading" src="https://github.com/egimaben/CloudBoost-ImageUpload-Android/blob/master/uploaded.PNG">
<p>&nbsp;</p>
Notice how we retrieve the saved image's URL and save it to a global variable with the following line
```java
CLOUDBOOST_IMAGE_URL=arg0.getFileUrl();
```
This URL will be used in the file download phase:

###download image from cloudboost
This stage is the easiest when it comes to the cloudboost part of it. Its everyday file download code, you just use CLOUDBOOST_IMAGE_URL.
<p>&nbsp;</p>
<img class="center-img" alt="downloading" src="https://github.com/egimaben/CloudBoost-ImageUpload-Android/blob/master/downloading.PNG">
<p>&nbsp;</p>
<p>&nbsp;</p>
<img class="center-img" alt="downloaded" src="https://github.com/egimaben/CloudBoost-ImageUpload-Android/blob/master/downloaded.PNG">
<p>&nbsp;</p>
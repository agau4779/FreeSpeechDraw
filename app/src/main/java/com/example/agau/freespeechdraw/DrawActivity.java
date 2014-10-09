package com.example.agau.freespeechdraw;
import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;

import android.location.LocationManager;
import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.Random;
import android.content.SharedPreferences;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.Card;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.ParcelableUtil;

//Flickr libraries
import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.REST;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoList;
import com.googlecode.flickrjandroid.photos.PhotosInterface;
import com.googlecode.flickrjandroid.photos.SearchParameters;
import org.json.JSONException;


public class DrawActivity extends Activity implements OnClickListener, LocationListener {
    //Drawing-related instance variables
    private DrawingView dv;
    private ImageButton drawBtn, eraseBtn, newBtn, saveBtn, colorBtn;
    private String last_drawing;

    //Location-related variables
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Context context;
    protected Location here, FSM;
    protected boolean gps_enabled,network_enabled;

    //Toq watch methods
    private final static String PREFS_FILE= "prefs_file";
    private final static String DECK_OF_CARDS_KEY= "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY= "deck_of_cards_version_key";

    private DeckOfCardsManager mDeckOfCardsManager;
    private RemoteDeckOfCards mRemoteDeckOfCards;
    private RemoteResourceStore mRemoteResourceStore;
    private CardImage[] mCardImages;
    private ToqBroadcastReceiver toqReceiver;

    private String[] FSM_names;
    private String[] FSM_prompts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        dv = (DrawingView)findViewById(R.id.drawing);
        context = this.getApplicationContext();

        drawBtn = (ImageButton)findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);
        eraseBtn = (ImageButton)findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);

        //new button
        newBtn = (ImageButton)findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);

        //save button
        saveBtn = (ImageButton)findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);

        //color picker
        colorBtn = (ImageButton)findViewById(R.id.color_picker);
        colorBtn.setOnClickListener(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, this);

		private static final String API_KEY = "bc003a9bc0dc589320334ddf2d52abd3"; //$NON-NLS-1$
		public static final String API_SEC = "9b055a14088b048e";
		
        here = new Location("here");
        FSM = new Location("FSM");
        FSM.setLatitude(37.86965);
        FSM.setLongitude(-122.25914);

        toqReceiver = new ToqBroadcastReceiver();
        FSM_names = new String[] {"Art Goldberg", "Jack Weinberg", "Jackie Goldberg", "Joan Baez", "Michael Rossman", "Mario Savio"};
        FSM_prompts = new String[] {"Draw 'Now'", "Draw 'SLATE'", "Draw 'FSM'", "Draw a Megaphone", "Express your own view of Free Speech in a drawing", "Draw 'Free Speech'"};
        mDeckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        init();
    }
    protected void onStart(){
        super.onStart();

        Log.d(Constants.TAG, "FreeSpeechDrawToq.onStart");
        // If not connected, try to connect
        if (!mDeckOfCardsManager.isConnected()){
            try{
                mDeckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View view) {
        //Dialog for choosing stroke width, shared by both the Brush and Eraser options.
        final Dialog brushDialog = new Dialog(this);
        brushDialog.setTitle("Brush Size");
        brushDialog.setContentView(R.layout.brush_picker);
        final SeekBar seekbar = (SeekBar)brushDialog.findViewById(R.id.seekBar);
        seekbar.setProgress(dv.strokeWidth);
        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int value = dv.strokeWidth;
            TextView brushSize = (TextView) brushDialog.findViewById(R.id.brushSize);
            @Override
            public void onProgressChanged(SeekBar seekBar, int newValue, boolean fromUser) {
                value = newValue;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                brushSize.setText("Brush Size: " + value);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                brushSize.setText("Brush Size: " + value);
                Toast.makeText(getApplicationContext(), "Brush value set to " + value + ".", Toast.LENGTH_SHORT).show();
            }
        });
        Button confirm = (Button) brushDialog.findViewById(R.id.confirmBrushSize);
        confirm.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                dv.changeStrokeWidth(seekbar.getProgress());
                brushDialog.dismiss();
            }
        });

        //Set OnClick listeners for different buttons.
        int id = view.getId();
        switch(id) {
        case R.id.new_btn:
            dv.startNew();
            break;
        case R.id.draw_btn:
            brushDialog.show();
            dv.eraser(false);
            break;
        case R.id.erase_btn:
            brushDialog.show();
            dv.eraser(true);
            break;
        case R.id.save_btn:
            //save drawing
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle("Save drawing");
            saveDialog.setMessage("Save drawing to device?");
            saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    //save drawing
                    dv.setDrawingCacheEnabled(true);
                    last_drawing = UUID.randomUUID().toString() + ".png";
                    String imgSaved = MediaStore.Images.Media.insertImage(
                            DrawActivity.this.getContentResolver(), dv.getDrawingCache(),
                            last_drawing, "drawing");
                    //feedback
                    if(imgSaved!=null){
                        Toast savedToast = Toast.makeText(getApplicationContext(),
                                "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                        savedToast.show();
                    }
                    else{
                        Toast unsavedToast = Toast.makeText(getApplicationContext(),
                                "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
                        unsavedToast.show();
                    }
                    dv.destroyDrawingCache();
                }
            });
            saveDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            saveDialog.show();
            break;
        case R.id.color_picker:
            Context context = this.getApplicationContext();
            ColorPicker c = new ColorPicker( context, new ColorPicker.OnColorChangedListener() {
                @Override
                    public void colorChanged(int color) {
                    }
                }, dv.getPaint().getColor());
            break;
        case R.id.submit_btn:
            if (last_drawing!=null) {
				Intent intent = new Intent(getApplicationContext(), FlickrjActivity.class);
				intent.putExtra(last_drawing, fileUri.getAbsolutePath());
				startActivity(intent);
            } else {
                Toast.makeText(this.getApplicationContext(), "Please save your image first!", Toast.LENGTH_SHORT).show();
            }
            break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.install_toq:
                install();
                return true;
            case R.id.uninstall_toq:
                uninstall();
                return true;
            case R.id.trigger_notification:
                sendNotification();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.draw, menu);
        return true;
    }

    //Location Methods
    @Override
    public void onLocationChanged(Location location) {
        here.setLatitude(location.getLatitude());
        here.setLongitude(location.getLongitude());

        if (here.distanceTo(FSM) < 50) {
            ////trigger watch event when within 50 meters of Latitude: N 37.86965 and Longitude: W -122.25914 .
            sendNotification();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    //Toq Watch Methods

    private void sendNotification() {
        int random = new Random().nextInt(5);
        String[] message = new String[2];
        message[0] = FSM_names[random];
        message[1] = FSM_prompts[random];
        // Create a NotificationTextCard
        NotificationTextCard notificationCard = new NotificationTextCard(System.currentTimeMillis(),
                "Draw Request", message);
        // Vibrate to alert user when showing the notification
        notificationCard.setVibeAlert(true);
        // Create a notification with the NotificationTextCard we made
        RemoteToqNotification notification = new RemoteToqNotification(this, notificationCard);

        try {
            // Send the notification
            mDeckOfCardsManager.sendNotification(notification);
            Toast.makeText(this, "Sent Notification", Toast.LENGTH_SHORT).show();
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send Notification", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Installs applet to Toq watch if app is not yet installed
     */
    private void install() {
        boolean isInstalled = true;

        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }

        if (!isInstalled) {
            try {
                mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
            } catch (RemoteDeckOfCardsException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: Cannot install application", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "App is already installed!", Toast.LENGTH_SHORT).show();
        }

        try{
            storeDeckOfCards();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void uninstall() {
        boolean isInstalled = true;

        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }

        if (isInstalled) {
            try{
                mDeckOfCardsManager.uninstallDeckOfCards();
            }
            catch (RemoteDeckOfCardsException e){
                Toast.makeText(this, getString(R.string.error_uninstalling_deck_of_cards), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.already_uninstalled), Toast.LENGTH_SHORT).show();
        }
    }

    private void init(){
        // Create the resource store for icons and images
        mRemoteResourceStore= new RemoteResourceStore();
        DeckOfCardsLauncherIcon whiteIcon = null;
        DeckOfCardsLauncherIcon colorIcon = null;
        // Get the launcher icons
        try{
            whiteIcon= new DeckOfCardsLauncherIcon("white.launcher.icon", getBitmap("bw.png"), DeckOfCardsLauncherIcon.WHITE);
            colorIcon= new DeckOfCardsLauncherIcon("color.launcher.icon", getBitmap("color.png"), DeckOfCardsLauncherIcon.COLOR);
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Can't get launcher icon");
            return;
        }

        //populate images with FSM protestor images
        mCardImages = new CardImage[6];
        try{
            mCardImages[0]= new CardImage("card.image.1", getBitmap("art_goldberg_toq.png"));
            mCardImages[1]= new CardImage("card.image.2", getBitmap("jack_weinberg_toq.png"));
            mCardImages[2]= new CardImage("card.image.3", getBitmap("jackie_goldberg_toq.png"));
            mCardImages[3]= new CardImage("card.image.4", getBitmap("joan_baez_toq.png"));
            mCardImages[4]= new CardImage("card.image.5", getBitmap("mario_savio_toq.png"));
            mCardImages[5]= new CardImage("card.image.6", getBitmap("michael_rossman_toq.png"));
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Can't get picture icon");
            return;
        }

        // Try to retrieve a stored deck of cards
        try {
            // If there is no stored deck of cards or it is unusable, then create new and store
            if ((mRemoteDeckOfCards = getStoredDeckOfCards()) == null){
                mRemoteDeckOfCards = createDeckOfCards();
                storeDeckOfCards();
            }
        }
        catch (Throwable th){
            th.printStackTrace();
            mRemoteDeckOfCards = null; // Reset to force recreate
        }

        // Make sure in usable state
        if (mRemoteDeckOfCards == null){
            mRemoteDeckOfCards = createDeckOfCards();
        }

        // Set the custom launcher icons, adding them to the resource store
        mRemoteDeckOfCards.setLauncherIcons(mRemoteResourceStore, new DeckOfCardsLauncherIcon[]{whiteIcon, colorIcon});

        // Re-populate the resource store with any card images being used by any of the cards
        for (Iterator<Card> it= mRemoteDeckOfCards.getListCard().iterator(); it.hasNext();){

            String cardImageId= ((SimpleTextCard)it.next()).getCardImageId();

            if ((cardImageId != null) && !mRemoteResourceStore.containsId(cardImageId)){
                for (int i=0;i<6;i++) {
                    mRemoteResourceStore.addResource(mCardImages[i]);
                }
            }
        }
    }

    private Bitmap getBitmap(String fileName) throws Exception{

//        try{
            InputStream is= this.getAssets().open(fileName);
            return BitmapFactory.decodeStream(is);
//        }
//        catch (Exception e){
//            throw new Exception("An error occurred getting the bitmap: " + fileName, e);
//        }
    }

    private RemoteDeckOfCards getStoredDeckOfCards() throws Exception{
        if (!isValidDeckOfCards()){
            //Log.w(Constants.TAG, "Stored deck of cards not valid for this version of the demo, recreating...");
            return null;
        }
        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        String deckOfCardsStr= prefs.getString(DECK_OF_CARDS_KEY, null);

        if (deckOfCardsStr == null){
            return null;
        }
        else{
            return ParcelableUtil.unmarshall(deckOfCardsStr, RemoteDeckOfCards.CREATOR);
        }
    }

    private void storeDeckOfCards() throws Exception{
        // Retrieve and hold the contents of PREFS_FILE, or create one when you retrieve an editor (SharedPreferences.edit())
        SharedPreferences prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Create new editor with preferences above
        SharedPreferences.Editor editor = prefs.edit();
        // Store an encoded string of the deck of cards with key DECK_OF_CARDS_KEY
        editor.putString(DECK_OF_CARDS_KEY, ParcelableUtil.marshall(mRemoteDeckOfCards));
        // Store the version code with key DECK_OF_CARDS_VERSION_KEY
        editor.putInt(DECK_OF_CARDS_VERSION_KEY, Constants.VERSION_CODE);
        // Commit these changes
        editor.commit();
    }

    // Check if the stored deck of cards is valid for this version of the demo
    private boolean isValidDeckOfCards(){

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Return 0 if DECK_OF_CARDS_VERSION_KEY isn't found
        int deckOfCardsVersion= prefs.getInt(DECK_OF_CARDS_VERSION_KEY, 0);

        return deckOfCardsVersion >= Constants.VERSION_CODE;
    }

    // Create some cards with example content
    private RemoteDeckOfCards createDeckOfCards(){

        ListCard listCard= new ListCard();
        for (int i=0;i<6;i++) {
            SimpleTextCard simpleTextCard = new SimpleTextCard(FSM_names[i]);
            simpleTextCard.setCardImage(mRemoteResourceStore, mCardImages[i]);
            simpleTextCard.setTitleText(FSM_names[i]);
            String[] message = new String[2];
            message[0] = "Draw Request: ";
            message[1] = FSM_prompts[i];
            simpleTextCard.setMessageText(message);
            listCard.add(simpleTextCard);
        }
        return new RemoteDeckOfCards(this, listCard);
    }
}

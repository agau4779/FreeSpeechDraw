package com.example.agau.freespeechdraw;
import java.util.UUID;

import android.location.LocationManager;
import android.media.Image;
import android.content.Context;
import android.os.Bundle;
import java.io.FileOutputStream;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.Menu;
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



public class DrawActivity extends Activity implements OnClickListener, LocationListener {
    //Drawing-related instance variables
    private DrawingView dv;
    private ImageButton drawBtn, eraseBtn, newBtn, saveBtn, colorBtn;

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
    private ToqReceiver toqReceiver;

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
        if (view.getId()==R.id.new_btn) {
            dv.startNew();
        } else if (view.getId()==R.id.draw_btn) {
            brushDialog.show();
            dv.eraser(false);
        } else if(view.getId()==R.id.erase_btn) {
            brushDialog.show();
            dv.eraser(true);
        } else if(view.getId()==R.id.save_btn){
            //save drawing
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle("Save drawing");
            saveDialog.setMessage("Save drawing to device?");
            saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                private FileOutputStream fOut;

                public void onClick(DialogInterface dialog, int which){
                    //save drawing
                    dv.setDrawingCacheEnabled(true);
                    String imgSaved = MediaStore.Images.Media.insertImage(
                            DrawActivity.this.getContentResolver(), dv.getDrawingCache(),
                            UUID.randomUUID().toString() + ".png", "drawing");
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
        } else if (view.getId()==R.id.color_picker) {
            Context context = this.getApplicationContext();
            ColorPicker c = new ColorPicker( context, new ColorPicker.OnColorChangedListener() {
                @Override
                    public void colorChanged(int color) {
                    }
                }, dv.getPaint().getColor());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        dv = (DrawingView)findViewById(R.id.drawing);

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

        colorBtn = (ImageButton)findViewById(R.id.color_picker);
        colorBtn.setOnClickListener(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, this);

        here = new Location("here");
        FSM = new Location("FSM");
        FSM.setLatitude(37.86965);
        FSM.setLongitude(-122.25914);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.draw, menu);
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        here.setLatitude(location.getLatitude());
        here.setLongitude(location.getLongitude());

        if (here.distanceTo(FSM) < 50) {
            ////trigger watch event when within 50 meters of Latitude: N 37.86965 and Longitude: W -122.25914 .
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
}

package com.example.agau.freespeechdraw;
import java.util.UUID;

import android.media.Image;
import android.content.Context;
import android.os.Bundle;
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



public class DrawActivity extends Activity implements OnClickListener {
    private DrawingView dv;
    private ImageButton drawBtn, eraseBtn, newBtn, saveBtn, colorBtn;
    private SeekBar seekbar;

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
                public void onClick(DialogInterface dialog, int which){
                    //save drawing
                    Bitmap screenshot;
                    dv.setDrawingCacheEnabled(true);
                    screenshot = Bitmap.createBitmap(dv.getDrawingCache());
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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.draw, menu);
        return true;
    }
}

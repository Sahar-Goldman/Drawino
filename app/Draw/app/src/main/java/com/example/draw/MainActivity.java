package com.example.draw;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button painter = (Button)findViewById(R.id.painter);
        painter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new  Intent(MainActivity.this, PaintActivity.class);
                startActivity(intent);
            }
        });

        Button picture = (Button)findViewById(R.id.picture);
        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new  Intent(MainActivity.this,ImageActivity.class);
                startActivity(intent);
            }
        });

    }
}

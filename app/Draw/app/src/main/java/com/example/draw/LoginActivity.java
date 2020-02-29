package com.example.draw;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    public ProgressBar progress;
    public String email;
    public String password;
    private RequestQueue mQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        progress = findViewById(R.id.progressBar);
        progress.setVisibility(View.GONE);
        mQueue = VolleySingleton.getInstance(this).getRequestQueue();

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( !isEmailValid()){
                    emailEditText.setError( getString(R.string.invalid_email));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( !isPasswordValid()){
                    passwordEditText.setError(getString(R.string.invalid_password));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isEmailValid()){
                    emailEditText.setError( getString(R.string.invalid_email));
                }
                if( !isPasswordValid()){
                    passwordEditText.setError(getString(R.string.invalid_password));
                }

                if( validate() ){
                    email = emailEditText.getText().toString();
                    password = passwordEditText.getText().toString();

                    SharedPreferences login = getSharedPreferences("login", MODE_PRIVATE);
                    SharedPreferences.Editor editor = login.edit();
                    editor.putString("email", email);
                    editor.putString("password", password);
                    editor.apply();

                    jsonParse();
                }
            }
        });
    }


    private boolean validate () {
        return  isEmailValid() && isPasswordValid();
    }

    private boolean isEmailValid() {
        return !(emailEditText.getText().toString().isEmpty()) &&
                Patterns.EMAIL_ADDRESS.matcher(emailEditText.getText().toString()).matches();
    }

    private boolean isPasswordValid() {
        return !(passwordEditText.getText().toString().isEmpty()) && passwordEditText.getText().toString().length() > 0;
    }


    public class MailObject {

        @SerializedName("email")
        public String _email;

        @SerializedName("password")
        public String _password;

        MailObject(String email, String password){
            _email =  email;
            _password = password;
        }
    }


   private void jsonParse() {
        progress.setVisibility(View.VISIBLE);
        String url = "https://drawinologin.azurewebsites.net/api/login?code=4hiQkRvEfJge9vY8hDn2FoYaIYjdxnaRwdsiIvknaP6aH5z8bRyFZA==";
        Gson gson = new Gson();
        MailObject mailObject = new MailObject(email,password);
        String jsonString = gson.toJson(mailObject);

        try {
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(jsonString),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Boolean res = (Boolean)response.get("res");
                                if(res){
                                    Intent intent = new  Intent(LoginActivity.this,MainActivity.class);
                                    startActivity(intent);
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.welcome) , Toast.LENGTH_LONG).show();
                                    finish();
                                }
                                else{
                                    progress.setVisibility(View.GONE);
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.login_failed) , Toast.LENGTH_LONG).show();
                                }

                            } catch (JSONException e) {
                                progress.setVisibility(View.GONE);
                                e.printStackTrace();
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.connection_error) , Toast.LENGTH_LONG).show();
                    error.printStackTrace();
                }
            });

            mQueue.add(request);
        }catch (JSONException e) {
            progress.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }

}

package com.example.salva.certificaciontwiiter;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.StrictMode;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class MainActivity extends AppCompatActivity {


    private Button buttonConecar, buttonEnviar;
    private EditText editTweet;
    private ImageView imageView;

    private SharedPreferences sharedPreferencesTwitter;

    private static Twitter twitter;
    private static RequestToken requestToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonConecar = (Button) findViewById(R.id.btnConectar);
        buttonEnviar = (Button) findViewById(R.id.btnEnviar);
        editTweet = (EditText) findViewById(R.id.edtTwit);
        imageView = (ImageView) findViewById(R.id.imgTwitter);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        sharedPreferencesTwitter = getSharedPreferences(Constantes.ARCHIVO_PREFERENCIAS, MODE_PRIVATE);

        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(Constantes.CALLBACK_URL)) {
            String verificador = uri.getQueryParameter(Constantes.VERIFICADOR_OAUTH);

            try {
                AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verificador);
                SharedPreferences.Editor editor = sharedPreferencesTwitter.edit();
                editor.putString(Constantes.PREFERENCIA_KEY_TOKEN, accessToken.getToken());
                editor.putString(Constantes.PREFERENCIA_KEY_SECRET, accessToken.getTokenSecret());
                editor.commit();

                User user = twitter.showUser(twitter.getId());
                URL urlImage = new URL(user.getOriginalProfileImageURL());
                imageView.setImageDrawable(Drawable.createFromStream((InputStream) urlImage.getContent(),""));

            } catch (TwitterException e) {
                e.printStackTrace();
                DialogoTwitter dialogoTwitter = new DialogoTwitter();
                dialogoTwitter.setMensaje("Error al conectarse con Twitter.");
                dialogoTwitter.show(getSupportFragmentManager(), "twitter");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        buttonConecar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (estadoConexion()) {
                    cerrarSesion();
                    buttonConecar.setText("Conectar");
                } else {
                    obtenerAutorizacion();
                }
            }
        });

        buttonEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarTweet();
            }
        });
    }

    private boolean estadoConexion() {
        if (sharedPreferencesTwitter.getString(Constantes.PREFERENCIA_KEY_TOKEN, null) != null) {
            return true;
        } else {
            return false;
        }
    }

    private void cerrarSesion() {
        SharedPreferences.Editor editor = sharedPreferencesTwitter.edit();
        editor.remove(Constantes.PREFERENCIA_KEY_SECRET);
        editor.remove(Constantes.PREFERENCIA_KEY_TOKEN);
        editor.commit();
        imageView.setImageDrawable(null);
    }

    private void obtenerAutorizacion() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setOAuthConsumerKey(Constantes.CONSUMER_KEY);
        configurationBuilder.setOAuthConsumerSecret(Constantes.CONSUMER_SECRET);

        Configuration configuration = configurationBuilder.build();
        twitter = new TwitterFactory(configuration).getInstance();

        try {
            requestToken = twitter.getOAuthRequestToken(Constantes.CALLBACK_URL);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL()));
            startActivity(intent);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    private void enviarTweet() {
        String oauthAccessToken = sharedPreferencesTwitter.getString(Constantes.PREFERENCIA_KEY_TOKEN, "");
        String oauthAccessTokenSecret = sharedPreferencesTwitter.getString(Constantes.PREFERENCIA_KEY_SECRET, "");

        ConfigurationBuilder confbuilder = new ConfigurationBuilder();
        Configuration configuration = confbuilder.setDebugEnabled(true)
                .setOAuthConsumerKey(Constantes.CONSUMER_KEY)
                .setOAuthConsumerSecret(Constantes.CONSUMER_SECRET)
                .setOAuthAccessToken(oauthAccessToken)
                .setOAuthAccessTokenSecret(oauthAccessTokenSecret)
                .build();

        Twitter twitter = new TwitterFactory(configuration).getInstance();
        try {
            twitter.updateStatus(editTweet.getText().toString());
            Toast.makeText(getApplicationContext(),
                    "Tweet publicado",
                    Toast.LENGTH_LONG).show();
        } catch (TwitterException e) {
            DialogoTwitter dialogoTwitter = new DialogoTwitter();
            dialogoTwitter.setMensaje("Error al publicar el Tweet.");
            dialogoTwitter.show(getSupportFragmentManager(), "twitter");
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (estadoConexion()) {
            buttonConecar.setText("Desconectar");
        } else {
            buttonConecar.setText("Conectar");
        }
    }


    static class DialogoTwitter extends DialogFragment {

        private String mensaje;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(mensaje);
            builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });
            return builder.create();
        }

        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }
    }


}

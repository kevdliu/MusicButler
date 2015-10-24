package twinblade.musicbutler;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class Main extends AppCompatActivity {

    public static EditText ip;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ip = (EditText) findViewById(R.id.ip);

        Button cali = (Button) findViewById(R.id.cali);
        cali.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Main.this.sendBroadcast(new Intent(SignalService.INTENT_CALIBRATE));
            }
        });

        Button closest = (Button) findViewById(R.id.closest);
        closest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Main.this.sendBroadcast(new Intent(SignalService.INTENT_GET_CLOSEST));
            }
        });
        Intent service = new Intent(this, SignalService.class);
        startService(service);

        RestAdapter restAdapter1 = new RestAdapter.Builder()
                .setEndpoint("http://192.168.1.13:8090/")
                .build();
        APIService service1 = restAdapter1.create(APIService.class);

        RestAdapter restAdapter2 = new RestAdapter.Builder()
                .setEndpoint("http://192.168.1.26:8090/")
                .build();
        APIService service2 = restAdapter1.create(APIService.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

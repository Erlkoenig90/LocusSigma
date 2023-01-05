package de.niklas_guertler.locussigma;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

enum FileFormat {
    FIT, GPX, KML, TCX
}

public class MainActivity extends Activity {
    private static final String INTENT_ITEM_TRACK_TOOLS = "locus.api.android.INTENT_ITEM_TRACK_TOOLS";
    private static final String INTENT_EXTRA_ITEM_ID = "INTENT_EXTRA_ITEM_ID";
    private static final String ACTION_GET_TRACK_AS_FILE = "com.asamm.locus.api.GET_TRACK_AS_FILE";
    private static final String INTENT_EXTRA_PACKAGE_NAME = "INTENT_EXTRA_PACKAGE_NAME";
    private static final int REQ_CODE = 42;
    private static final String TAG = "MainActivity";

    private static boolean isAction(Intent intent, String action) {
        return action.equals(intent.getAction());
    }

    private static boolean isIntentTrackTools(Intent intent) {
        return isAction(intent, INTENT_ITEM_TRACK_TOOLS);
    }

    private static Long getItemId(Intent intent) {
        return intent.getLongExtra(INTENT_EXTRA_ITEM_ID, -1L);
    }

    private static Intent prepareTrackInFormatIntent(String action, String locusPackageName, Long trackId, FileFormat format, String formatExtra) {
        Intent intent = new Intent(action);
        intent.setPackage(locusPackageName);
        intent.putExtra("trackId", trackId);
        intent.putExtra("format", format.name().toLowerCase());
        intent.putExtra("formatExtra", formatExtra);
        return intent;
    }

    private static Intent prepareTrackInFormatIntent(String action, String locusPackageName, Long trackId, FileFormat format) {
        return prepareTrackInFormatIntent(action, locusPackageName, trackId, format, "");
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if (isIntentTrackTools(intent)) {
            Long trackId = getItemId(intent);
            String packageName = intent.getStringExtra(INTENT_EXTRA_PACKAGE_NAME);

            startActivityForResult(prepareTrackInFormatIntent(
                    ACTION_GET_TRACK_AS_FILE,
                    packageName, trackId, FileFormat.GPX), REQ_CODE);
        }
    }

    private void sendToSigma(Uri file) {
        ComponentName comp = new ComponentName("com.sigmasport.link2", "com.sigmasport.link2.ui.routes.RoutesActivity");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setComponent(comp);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.setDataAndType(file, "application/gpx+xml");

        startActivity(intent);
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                try {
                    sendToSigma(data.getData());

                } catch (Exception e) {
                    Log.e(TAG, "onActivityResult", e);
                }
            } else {
                Toast.makeText(this, "Process unsuccessful", Toast.LENGTH_SHORT).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
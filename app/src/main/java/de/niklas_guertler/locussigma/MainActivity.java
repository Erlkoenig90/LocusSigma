package de.niklas_guertler.locussigma;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    private static void removeWptElements(InputStream in, OutputStream out)
            throws IOException, XmlPullParserException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(in, null);

        XmlSerializer writer = Xml.newSerializer();
        writer.setOutput(out, "UTF-8");

        boolean skipping = false;
        int skipDepth = 0;

        for (int type = parser.getEventType(); type != XmlPullParser.END_DOCUMENT; type = parser.next()) {
            switch (type) {
                case XmlPullParser.START_DOCUMENT:
                    writer.startDocument("UTF-8", null);
                    break;
                case XmlPullParser.START_TAG: {
                    if (skipping) {
                        skipDepth++;
                        break;
                    }
                    if ("wpt".equals(parser.getName())) {
                        skipping = true;
                        skipDepth = 1;
                        break;
                    }
                    int depth = parser.getDepth();
                    int prevNs = depth > 1 ? parser.getNamespaceCount(depth - 1) : 0;
                    for (int i = prevNs; i < parser.getNamespaceCount(depth); i++) {
                        String prefix = parser.getNamespacePrefix(i);
                        writer.setPrefix(prefix != null ? prefix : "", parser.getNamespaceUri(i));
                    }
                    writer.startTag(parser.getNamespace(), parser.getName());
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        writer.attribute(parser.getAttributeNamespace(i),
                                parser.getAttributeName(i), parser.getAttributeValue(i));
                    }
                    break;
                }
                case XmlPullParser.END_TAG:
                    if (skipping) {
                        if (--skipDepth == 0) skipping = false;
                    } else {
                        writer.endTag(parser.getNamespace(), parser.getName());
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (!skipping) {
                        writer.text(parser.getText());
                    }
                    break;
            }
        }
        writer.endDocument();
    }

    private Uri buildCleanGpxUri(Uri sourceUri) throws IOException, XmlPullParserException {
        File shareDir = new File(getCacheDir(), "sharegpx");
        shareDir.mkdirs();
        File outFile = File.createTempFile("route", ".gpx", shareDir);
//        outFile.deleteOnExit();
        try (InputStream in = getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(outFile)) {
            removeWptElements(in, out);
        }
        return Uri.parse("content://de.niklas_guertler.locussigma.provider/" + outFile.getName());
    }

    private void sendToSigma(Uri sourceUri) {
        try {
            Uri cleanUri = buildCleanGpxUri(sourceUri);
            ComponentName comp = new ComponentName("com.sigmasport.link2", "com.sigmasport.link2.ui.FileImportActivity");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setComponent(comp);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_HISTORY
                    /* | Intent.FLAG_ACTIVITY_NEW_TASK */ /* | Intent.FLAG_ACTIVITY_CLEAR_TOP */ );

            intent.setDataAndType(cleanUri, "application/gpx+xml");
//            Log.i ("MainActivity", "Sending to SIGMA: " + cleanUri.toString());
            startActivity(intent);
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "sendToSigma: failed to process GPX", e);
            Toast.makeText(this, "Process unsuccessful", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                sendToSigma(data.getData());
            } else {
                Toast.makeText(this, "Process unsuccessful", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}

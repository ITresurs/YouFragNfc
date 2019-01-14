package com.example.a001272.youfragnfc;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.example.a001272.youfragnfc.utils.Constants;


import java.util.ArrayList;
import java.util.Collections;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.NoSuchElementException;

/*
 * todo: spara undan tag-koder. Koppla nya tag-koder till innehÃ¥ll. styrmetoder, pÃ¥/av, volym? browser för youtubefiler m. möjlighet att spara videoadress till tag. Video bryts när orienteringen på telefonen ändras. Lås orientering till.. stående??
 *
 *
 * */
public class MainActivity extends AppCompatActivity {
// public class MainActivity extends Activity

    //YOUTU
    private static final String TAG = MainActivity.class.getSimpleName();

    //youtube player fragment
    private YouTubePlayerSupportFragment youTubePlayerFragment;
    private ArrayList<String> youtubeVideoArrayList;

    //youtube player to play video when new video selected
    private YouTubePlayer youTubePlayer;
    //YOUTU

    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";
    public String videoattspela = "a";
    public String videosomspelades = "b";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    Context context;
int playlistindex =0;
int errorcount = 0;
    TextView tvNFCContent;
    TextView message;
    Button btnWrite;

    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putString("videon", videoattspela);
        Log.d("Test","bundeln är uppdaterad onSave med " + videoattspela);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState !=null){
            videoattspela = savedInstanceState.getString("videon");
            Log.d("Test", "Nu försökte vi återskapa videoattspela med " + videoattspela);
        }
        setContentView(R.layout.activity_main);
        context = this;

        //YOUTU
        generateDummyVideoList();
        //initializeYoutubePlayer("8GW6sLrK40k");
        Log.d("Test", "EttEttEtt "); // alltid null??
        //if (videoattspela != null) {initializeYoutubePlayer(videoattspela);} // funkar inte, varken här eller längre ner. Förmodligen läser appen inte taggen när taggen aktiverar appen när den inte har fokus, alt. återinitialiseras hela appen när den återfår fokus/vänds. Testa skriva ut tag/videoattspela till konsoll!!!!
        //else { initializeYoutubePlayer("m4jkQJ2xTjQ");}
        //initializeYoutubePlayer("m4jkQJ2xTjQ");
        //YOUTU

        tvNFCContent = (TextView) findViewById(R.id.nfc_contents);
        message = (TextView) findViewById(R.id.edit_message);
        btnWrite = (Button) findViewById(R.id.button);

        btnWrite.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    if(myTag ==null) {
                        Toast.makeText(context, ERROR_DETECTED, Toast.LENGTH_LONG).show();
                    } else {
                        write(message.getText().toString(), myTag);
                        Toast.makeText(context, WRITE_SUCCESS, Toast.LENGTH_LONG ).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                }
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }
        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };
    }


    /******************************************************************************
     **********************************Read From NFC Tag***************************
     ******************************************************************************/
    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }
    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
//        String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        tvNFCContent.setText("NFC Content: " + text);
        videoattspela = text;
       /*if (youTubePlayer != null){
        try {
       youTubePlayer.release();} catch (IllegalStateException e){}
       }*/

       //if ("stopp".equals(text)){Log.d("Test", "DETFÖREFALLERVARASÅATTTEXTÄRLIKAMEDSTOPP");}
       // if (!"stopp".equals(text)){Log.d("Test", "jodå de är samma !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!XXXXXXXXXXXXXXXXXXXXXXXX");
       //    videoattspela = text;}
//initializeYoutubePlayer(text);}
        /*if (youTubePlayer != null){
            try {
                youTubePlayer.loadVideo(text); Log.d("Test", "Härifrån borde youtube startas vid ny tag!!!!");} catch (IllegalStateException e){}
        }
        if (youTubePlayer == null) {
            Log.d("Test", "youtubeplayer existerar inte!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }*/

    // spela();
        // youTubePlayer.loadVideo(youtubeVideoArrayList.get(0));
        // youTubePlayer.play();

       // setContentView(R.layout.youtube_start); // försök att gå till youtube_start kraschar alltihop!!


        // watchYoutubeVideo(this, "8GW6sLrK40k");

        /*
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
        startActivity(browserIntent);
        */

    }
/* //för att titta i webbläsaren(?)
    public void watchYoutubeVideo(Context context, String id){
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + id));

        try {
            context.startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            context.startActivity(webIntent);
        }
    }
*/



    /******************************************************************************
     **********************************Write to NFC Tag****************************
     ******************************************************************************/
    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }



    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
        Log.d("Test", "onPaus numera...");
        if (youTubePlayer != null) {
            try {
                youTubePlayer.release();} catch (IllegalStateException e){}
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        WriteModeOn();
        if (youTubePlayer != null) {
        try {
            youTubePlayer.release();} catch (IllegalStateException e){}
                                    }
        // initializeYoutubePlayer(0);
        Log.d("Test", "TwoTwoTwo " + videoattspela);
        //if (!"stopp".equals(videoattspela)){Log.d("Test", "jodå de är samma !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!XXXXXXXXXXXXXXXXXXXXXXXX");
        //    initializeYoutubePlayer(videoattspela);}
        if (videoattspela != null && !"stopp".equals(videoattspela)) {initializeYoutubePlayer(videoattspela);}
        //else { initializeYoutubePlayer("8GW6sLrK40k");}
    }

    /******************************************************************************
     **********************************Enable Write********************************
     ******************************************************************************/
    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }
    /******************************************************************************
     **********************************Disable Write*******************************
     ******************************************************************************/
    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }

    /**
     * initialize youtube player via Fragment and get instance of YoutubePlayer
     * @param x
     */
    public void initializeYoutubePlayer(final String x) {

        youTubePlayerFragment = (YouTubePlayerSupportFragment) getSupportFragmentManager()
                .findFragmentById(R.id.youtube_player_fragment);

        if (youTubePlayerFragment == null)
            return;

        youTubePlayerFragment.initialize(Constants.DEVELOPER_KEY, new YouTubePlayer.OnInitializedListener() {

            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                                boolean wasRestored) {
                if (!wasRestored) {
                    youTubePlayer = player;

                    //set the player style default
                    youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);

                    youTubePlayer.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
                        @Override
                        public void onPlaying() {
                        }

                        @Override
                        public void onPaused() {

                        }

                        @Override
                        public void onStopped() {

                        }

                        @Override
                        public void onBuffering(boolean b) {

                        }

                        @Override
                        public void onSeekTo(int i) {

                        }
                    }); //TEST
                    youTubePlayer.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
                        @Override
                        public void onLoading() {

                        }

                        @Override
                        public void onLoaded(String s) {

                        }

                        @Override
                        public void onAdStarted() {

                        }

                        @Override
                        public void onVideoStarted() {
                            errorcount =0;
                                if (youTubePlayer.hasNext()) {
                                    // try {youTubePlayer.next();} catch (NoSuchElementException e){Log.d("Test","Det funkade inte att spela nästa video!!");}
                                    playlistindex++;
                                    Log.d("Test","Nu ökas index med ett!! " + playlistindex);
                                } else {playlistindex = 0; Log.d("Test","Vad händer??!! " + playlistindex);}
                        }

                        @Override
                        public void onVideoEnded() {

                        }

                        @Override
                        public void onError(YouTubePlayer.ErrorReason errorReason) {
                            Log.d("Test", "felorsak" + errorReason);
                           if (errorReason.INTERNAL_ERROR.equals(errorReason)) {
                               errorcount++;
                               if (videoattspela.equals(videosomspelades)){Log.d("Test","Titta: " + videoattspela + " är samma som " + x);}

                               if (errorcount > 1){videoattspela = "stopp"; errorcount = 0;}
                               else {
                               if (videoattspela.equals(videosomspelades))
                               { Log.d("Test","ETT " + playlistindex);
                                   // try {youTubePlayer.next();} catch (NoSuchElementException e){Log.d("Test","Det funkade inte att spela nästa video!!");}
                                       youTubePlayer.loadPlaylist(x, playlistindex, 0);
                                   }
                                else {

                                   videosomspelades = videoattspela;
                                   youTubePlayer.loadPlaylist(x);
                                   playlistindex = 0;
                                   Log.d("Test","Och vad händer här??!! " + playlistindex);
                                    }}

                               Log.d("Test","FAAAASIKKKEEEENNNNN!!!! " + playlistindex);

                           }
                        }
                    }); //TEST
                    youTubePlayer.setPlaylistEventListener(new YouTubePlayer.PlaylistEventListener() {
                        @Override
                        public void onPrevious() {

                        }

                        @Override
                        public void onNext() {

                        }

                        @Override
                        public void onPlaylistEnded() {

                        }
                    }); //TEST

                    //cue the 1st video by default
                    // youTubePlayer.cueVideo(youtubeVideoArrayList.get(x));

                  // youTubePlayer.cueVideo(x);

                    // youTubePlayer.play();
                    // youTubePlayer.loadVideo(youtubeVideoArrayList.get(x));

                //try {youTubePlayer.loadVideo(x);} catch (Exception e){youTubePlayer.loadPlaylist(x);} //No exceptions given!!??

                /*if (x.length() == 11){youTubePlayer.loadVideo(x);}

                    else if (x.length() == 13)
                {youTubePlayer.loadPlaylist(x);}*/


                  youTubePlayer.loadVideo(x);

                }
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider arg0, YouTubeInitializationResult arg1) {

                //print or show error if initialization failed
                Log.e(TAG, "Youtube Player View initialization failed");
            }
        });
    }
public void spela(){

    youTubePlayer.loadVideo(youtubeVideoArrayList.get(1));
}
    /**
     * method to generate dummy array list of videos
     */
    private void generateDummyVideoList() {
        youtubeVideoArrayList = new ArrayList<>();

        //get the video id array from strings.xml
        String[] videoIDArray = getResources().getStringArray(R.array.video_id_array);

        //add all videos to array list
        Collections.addAll(youtubeVideoArrayList, videoIDArray);

    }

}


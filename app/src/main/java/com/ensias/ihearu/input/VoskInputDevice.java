package com.ensias.ihearu.input;

import static com.ensias.ihearu.util.LocaleUtils.LocaleResolutionResult;
import static com.ensias.ihearu.util.LocaleUtils.UnsupportedLocaleException;
import static com.ensias.ihearu.util.LocaleUtils.resolveSupportedLocale;
import static com.ensias.ihearu.util.StringUtils.isNullOrEmpty;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;

import com.ensias.ihearu.BuildConfig;
import com.ensias.ihearu.R;
import com.ensias.ihearu.Sections;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class VoskInputDevice extends SpeechInputDevice {

    public static final String TAG = VoskInputDevice.class.getSimpleName();
    public static final String MODEL_PATH = "/vosk-model";
    public static final String MODEL_ZIP_FILENAME = "model.zip";
    public static final float SAMPLE_RATE = 44100.0f;

    //getting vosk zip for every language
    public static final Map<String, String> MODEL_URLS = new HashMap<String, String>() {{
        put("en",    "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip");
        put("en-in", "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip");
        put("fr",    "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip");

    }};


    private Activity activity;
    private final CompositeDisposable disposables = new CompositeDisposable();
    @Nullable private BroadcastReceiver downloadingBroadcastReceiver = null;
    private Long currentModelDownloadId = null;
    @Nullable private SpeechService speechService = null;

    private boolean currentlyInitializingRecognizer = false;
    private boolean startListeningOnLoaded = false;
    private boolean currentlyListening = false;


    public VoskInputDevice(final Activity activity) {
        this.activity = activity;
    }

    @Override
    public void load() {
        load(false); // the user did not press on a button, so manual=false
    }


    private void load(final boolean manual) {
        if (speechService == null && !currentlyInitializingRecognizer) {
            if (new File(getModelDirectory(), "ivector").exists()) {
                // one directory is in the correct place, so everything should be ok
                Log.d(TAG, "Vosk model in place");

                currentlyInitializingRecognizer = true;
                onLoading();

                disposables.add(Completable.fromAction(this::initializeRecognizer)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                            currentlyInitializingRecognizer = false;
                            if (startListeningOnLoaded) {
                                startListeningOnLoaded = false;
                                tryToGetInput(manual);
                            } else {
                                onInactive();
                            }
                        }, throwable -> {
                            currentlyInitializingRecognizer = false;
                            if ("Failed to initialize recorder. Microphone might be already in use."
                                    .equals(throwable.getMessage())) {
                                notifyError(new UnableToAccessMicrophoneException());
                            } else {
                                notifyError(throwable);
                            }
                            onInactive();
                        }));

            } else {
                Log.d(TAG, "Vosk model not in place");
                final DownloadManager downloadManager =
                        (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);

                if (currentModelDownloadId == null) {
                    Log.d(TAG, "Vosk model is not already being downloaded");

                    if (manual) {
                        // the model needs to be downloaded and no download has already started;
                        // the user manually triggered the input device, so he surely wants the
                        // model to be downloaded, so we can proceed
                        onLoading();
                        try {
                            final LocaleResolutionResult result = resolveSupportedLocale(
                                    LocaleListCompat.create(Sections.getCurrentLocale()),
                                    MODEL_URLS.keySet());
                            startDownloadingModel(downloadManager, result.supportedLocaleString);
                        } catch (final UnsupportedLocaleException e) {
                            asyncMakeToast(R.string.vosk_model_unsupported_language);
                            e.printStackTrace();
                            onRequiresDownload();
                        }

                    } else {
                        // loading the model would require downloading it, but the user didn't
                        // explicitly tell the voice recognizer to download files, so notify them
                        // that a download is required
                        onRequiresDownload();
                    }

                } else {
                    Log.d(TAG, "Vosk model already being downloaded: " + currentModelDownloadId);
                }
            }
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        disposables.clear();
        if (speechService != null) {
             speechService.shutdown();
             speechService = null;
        }

        if (currentModelDownloadId != null) {
            final DownloadManager downloadManager =
                    (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadManager.remove(currentModelDownloadId);
            updateCurrentDownloadId(activity, null);
        }

        if (downloadingBroadcastReceiver != null) {
            activity.unregisterReceiver(downloadingBroadcastReceiver);
            downloadingBroadcastReceiver = null;
        }
        activity = null;
    }

    @Override
    public synchronized void tryToGetInput(final boolean manual) {
        if (currentlyInitializingRecognizer) {
            startListeningOnLoaded = true;
            return;
        } else if (speechService == null) {
            startListeningOnLoaded = true;
            load(manual); // not loaded before, retry
            return; // recognizer not ready
        }

        if (currentlyListening) {
            return;
        }
        currentlyListening = true;
        super.tryToGetInput(manual);

        Log.d(TAG, "starting recognizer");

        speechService.startListening(new RecognitionListener() {

            @Override
            public void onPartialResult(final String s) {
                Log.d(TAG, "onPartialResult called with s = " + s);
                if (!currentlyListening) {
                    return;
                }

                String partialInput = null;
                try {
                    partialInput = new JSONObject(s).getString("partial");
                } catch (final JSONException e) {
                    e.printStackTrace();
                }

                if (!isNullOrEmpty(partialInput)) {
                    notifyPartialInputReceived(partialInput);
                }
            }

            @Override
            public void onResult(final String s) {
                Log.d(TAG, "onResult called with s = " + s);
                if (!currentlyListening) {
                    return;
                }

                stopRecognizer();

                final ArrayList<String> inputs = new ArrayList<>();
                try {
                    final JSONObject jsonResult = new JSONObject(s);
                    final int size = jsonResult.getJSONArray("alternatives").length();
                    for (int i = 0; i < size; i++) {
                        final String text = jsonResult.getJSONArray("alternatives")
                                .getJSONObject(i).getString("text");
                        if (!isNullOrEmpty(text)) {
                            inputs.add(text);
                        }
                    }
                } catch (final JSONException e) {
                    e.printStackTrace();
                }

                if (inputs.isEmpty()) {
                    notifyNoInputReceived();
                } else {
                    notifyInputReceived(inputs);
                }
            }

            @Override
            public void onFinalResult(final String s) {
                Log.d(TAG, "onFinalResult called with s = " + s);
                // TODO
            }

            @Override
            public void onError(final Exception e) {
                Log.d(TAG, "onError called");
                stopRecognizer();
                notifyError(e);
            }

            @Override
            public void onTimeout() {
                Log.d(TAG, "onTimeout called");
                stopRecognizer();
                notifyNoInputReceived();
            }
        });
        onListening();
    }

    @Override
    public void cancelGettingInput() {
        if (currentlyListening) {
            if (speechService != null) {
                speechService.stop();
            }
            notifyNoInputReceived();

            // call onInactive() only if we really were listening, so that the SpeechInputDevice
            // state icon is preserved if something different from "microphone on" was being shown
            onInactive();
        }

        startListeningOnLoaded = false;
        currentlyListening = false;
    }


    public static void deleteCurrentModel(final Context context) {
        final DownloadManager downloadManager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final Long modelDownloadId = getDownloadIdFromPreferences(context, downloadManager);
        if (modelDownloadId != null) {
            downloadManager.remove(modelDownloadId);
        }

        deleteFolder(new File(context.getFilesDir(), MODEL_PATH));
    }

  //initializing

    private synchronized void initializeRecognizer() throws IOException {
        Log.d(TAG, "initializing recognizer");

        LibVosk.setLogLevel(BuildConfig.DEBUG ? LogLevel.DEBUG : LogLevel.WARNINGS);
        final Model model = new Model(getModelDirectory().getAbsolutePath());
        final Recognizer recognizer = new Recognizer(model, SAMPLE_RATE);
        recognizer.setMaxAlternatives(5);
        this.speechService = new SpeechService(recognizer, SAMPLE_RATE);
    }

    private void stopRecognizer() {
        currentlyListening = false;

        if (speechService != null) {
            speechService.stop();
        }

        onInactive();
    }


    //downloading models

    private void startDownloadingModel(final DownloadManager downloadManager,
                                       final String language) {
        asyncMakeToast(R.string.vosk_model_downloading);
        final File modelZipFile = getModelZipFile();
        modelZipFile.delete(); // if existing, delete the model zip file (should never happen)

        // build download manager request
        final String modelUrl = MODEL_URLS.get(language);
        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(modelUrl))
                .setTitle(activity.getString(R.string.vosk_model_notification_title))
                .setDescription(activity.getString(
                        R.string.vosk_model_notification_description, language))
                .setDestinationUri(Uri.fromFile(modelZipFile));

        // setup download completion listener
        final IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        downloadingBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                Log.d(TAG, "Got intent for downloading broadcast receiver: " + intent);
                if (downloadingBroadcastReceiver == null) {
                    return; // just to be sure there are no issues with threads
                }

                if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                    final long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

                    if (currentModelDownloadId == null || id != currentModelDownloadId) {
                        Log.w(TAG, "Download complete listener notified with unknown id: " + id);
                        return; // do not unregister broadcast receiver
                    }

                    if (downloadingBroadcastReceiver != null) {
                        Log.d(TAG, "Unregistering downloading broadcast receiver");
                        activity.unregisterReceiver(downloadingBroadcastReceiver);
                        downloadingBroadcastReceiver = null;
                    }

                    if (downloadManager.getMimeTypeForDownloadedFile(currentModelDownloadId)
                            == null) {
                        Log.e(TAG, "Failed to download vosk model");
                        asyncMakeToast(R.string.vosk_model_download_error);
                        downloadManager.remove(currentModelDownloadId);
                        updateCurrentDownloadId(activity, null);
                        onInactive();
                        return;
                    }

                    Log.d(TAG, "Vosk model download complete, extracting from zip");
                    disposables.add(Completable
                            .fromAction(VoskInputDevice.this::extractModelZip)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> {
                                        asyncMakeToast(R.string.vosk_model_ready);
                                        downloadManager.remove(currentModelDownloadId);
                                        updateCurrentDownloadId(activity, null);

                                        // surely the user pressed a button a while ago that
                                        // triggered the download process, so manual=true
                                        load(true);
                                    },
                                    throwable -> {
                                        asyncMakeToast(R.string.vosk_model_extraction_error);
                                        throwable.printStackTrace();
                                        downloadManager.remove(currentModelDownloadId);
                                        updateCurrentDownloadId(activity, null);
                                        onInactive();
                                    }));
                }
            }
        };
        activity.registerReceiver(downloadingBroadcastReceiver, filter);

        // launch download
        Log.d(TAG, "Starting vosk model download: " + request);
        updateCurrentDownloadId(activity, downloadManager.enqueue(request));
    }

    private void extractModelZip() throws IOException {
        asyncMakeToast(R.string.vosk_model_extracting);

        try (ZipInputStream zipInputStream =
                     new ZipInputStream(new FileInputStream(getModelZipFile()))) {
            ZipEntry entry; // cycles through all entries
            while ((entry = zipInputStream.getNextEntry()) != null) {
                final File destinationFile = getDestinationFile(entry.getName());

                if (entry.isDirectory()) {
                    // create directory
                    if (!destinationFile.mkdirs()) {
                        throw new IOException("mkdirs failed: " + destinationFile);
                    }

                } else {
                    // copy file
                    try (BufferedOutputStream outputStream = new BufferedOutputStream(
                            new FileOutputStream(destinationFile))) {
                        final byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                        outputStream.flush();
                    }
                }

                zipInputStream.closeEntry();
            }
        }
    }


    //files utilities

    private File getDestinationFile(final String entryName) throws IOException {
        // model files are under a subdirectory, so get the path after the first /
        final String filePath = entryName.substring(entryName.indexOf('/') + 1);
        final File destinationDirectory = getModelDirectory();

        // protect from Zip Slip vulnerability (!)
        final File destinationFile = new File(destinationDirectory, filePath);
        if (!destinationDirectory.getCanonicalPath().equals(destinationFile.getCanonicalPath())
                && !destinationFile.getCanonicalPath().startsWith(
                        destinationDirectory.getCanonicalPath() + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + entryName);
        }

        return destinationFile;
    }

    private File getModelDirectory() {
        return new File(activity.getFilesDir(), MODEL_PATH);
    }

    private File getModelZipFile() {
        return new File(activity.getExternalFilesDir(null), MODEL_ZIP_FILENAME);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void deleteFolder(final File file) {
        final File[] subFiles = file.listFiles();
        if (subFiles != null) {
            for (final File subFile : subFiles) {
                if (subFile.isDirectory()) {
                    deleteFolder(subFile);
                } else {
                    subFile.delete();
                }
            }
        }
        file.delete();
    }


    // Download id utilities

    private static Long getDownloadIdFromPreferences(final Context context,
                                                     final DownloadManager manager) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String downloadIdKey = context.getString(R.string.pref_key_vosk_download_id);
        if (prefs.contains(downloadIdKey)) {
            final long id = prefs.getLong(downloadIdKey, 0);
            if (manager.query(new DownloadManager.Query().setFilterById(id)).getCount() == 0) {
                // no download in progress or being extracted with this id, reset setting
                return null;
            } else {
                return id;
            }

        } else {
            return null;
        }
    }

    private void updateCurrentDownloadId(final Context context, final Long id) {
        // this field is used anywhere except in static contexts, where the preference is used
        currentModelDownloadId = id;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String downloadIdKey = context.getString(R.string.pref_key_vosk_download_id);
        if (id == null) {
            // remove completely, used to notify of null values, check getDownloadIdFromPreferences
            prefs.edit().remove(downloadIdKey).apply();
        } else {
            prefs.edit().putLong(downloadIdKey, id).apply();
        }
    }


    // Other utilities

    private void asyncMakeToast(@StringRes final int message) {
        activity.runOnUiThread(() ->
                Toast.makeText(activity, activity.getString(message), Toast.LENGTH_SHORT).show());
    }
}

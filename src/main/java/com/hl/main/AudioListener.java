package com.hl.main;

import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

import javax.sound.sampled.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioListener {

    // ======== Public API (unchanged) ========
    static String content = "";
    static boolean isRunning = false;

    // ======== Config ========
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 4096;
    private static final int PAUSE_TIMEOUT_MS = 800;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter LOG_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Toggle this to false to silence logs:
    private static final boolean DEBUG = true;

    // ======== State ========
    private static final AtomicBoolean recording = new AtomicBoolean(false);
    private static volatile boolean streamActive = false;

    private static ExecutorService audioExecutor;
    private static ScheduledExecutorService scheduler;

    private static TargetDataLine systemAudioLine;

    private static final StringBuilder currentLine = new StringBuilder();
    private static final Object textLock = new Object(); // guards content/currentLine

    private static ScheduledFuture<?> pauseFuture;

    // ======== Public API ========

    public static void start() {
        if (isRunning) {
            d("start() ignored: already running");
            return;
        }
        d("start() called: initializing services");
        isRunning = true;
        recording.set(true);

        audioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AudioListener-AudioThread");
            t.setDaemon(true);
            return t;
        });
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AudioListener-Scheduler");
            t.setDaemon(true);
            return t;
        });

        audioExecutor.submit(() -> {
            d("Audio thread entered beginAudioStreaming()");
            beginAudioStreaming();
            d("Audio thread exiting beginAudioStreaming()");
        });
    }

    public static String close() {
        d("close() called");
        if (!isRunning) {
            d("close() ignored: not running");
            return content;
        }

        // stop flags
        recording.set(false);
        streamActive = false;

        // cancel pending finalize task
        ScheduledFuture<?> pf = pauseFuture;
        if (pf != null) {
            d("Cancelling pauseFuture");
            pf.cancel(false);
        }

        // finalize any partial line
        finalizeLine();

        // stop audio promptly
        if (systemAudioLine != null) {
            try { systemAudioLine.stop(); d("TargetDataLine.stop() ok"); } catch (Exception ex) { d("TargetDataLine.stop() error: " + ex); }
            try { systemAudioLine.close(); d("TargetDataLine.close() ok"); } catch (Exception ex) { d("TargetDataLine.close() error: " + ex); }
            systemAudioLine = null;
        }

        // shutdown executors
        if (audioExecutor != null) {
            d("Shutting down audioExecutor");
            audioExecutor.shutdownNow();
        }
        if (scheduler != null) {
            d("Shutting down scheduler");
            scheduler.shutdownNow();
        }

        isRunning = false;
        // ensure final UI sync
        safeUpdateUI(content);
        d("close() finished");
        return content;
    }

    // ======== Core loop ========

    private static void beginAudioStreaming() {
        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            systemAudioLine = findAndPrepareAudioLine();
            d("TargetDataLine acquired and opened");
            systemAudioLine.start();
            d("TargetDataLine started");

            int backoffMs = 1000;

            while (recording.get()) {
                streamActive = true;
                d("Starting new SpeechClient streaming session");
                try (SpeechClient client = SpeechClient.create()) {
                    ClientStream<StreamingRecognizeRequest> stream =
                            client.streamingRecognizeCallable().splitCall(createObserver());

                    StreamingRecognitionConfig streamingConfig =
                            StreamingRecognitionConfig.newBuilder()
                                    .setConfig(RecognitionConfig.newBuilder()
                                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                            .setLanguageCode("en-US")
                                            .setSampleRateHertz(SAMPLE_RATE)
                                            .setEnableAutomaticPunctuation(true)
                                            .build())
                                    .setInterimResults(true)
                                    .build();

                    stream.send(StreamingRecognizeRequest.newBuilder()
                            .setStreamingConfig(streamingConfig).build());

                    d("Streaming session started (interim=true, punctuation=true)");
                    backoffMs = 1000; // reset backoff on successful start

                    long totalBytes = 0;
                    long lastLog = System.currentTimeMillis();

                    while (recording.get() && streamActive) {
                        int n = systemAudioLine.read(buffer, 0, buffer.length);
                        if (n > 0) {
                            totalBytes += n;
                            stream.send(StreamingRecognizeRequest.newBuilder()
                                    .setAudioContent(ByteString.copyFrom(buffer, 0, n))
                                    .build());
                        }
                        long now = System.currentTimeMillis();
                        if (DEBUG && now - lastLog >= 2000) { // throttle
                            d("Audio feeding... bytesSent=" + totalBytes);
                            lastLog = now;
                        }
                    }

                    d("Leaving audio feed loop (recording=" + recording.get() + ", streamActive=" + streamActive + ")");
                    try {
                        stream.closeSend();
                        d("stream.closeSend() ok");
                    } catch (Exception e) {
                        d("stream.closeSend() error: " + e);
                    }

                } catch (Exception e) {
                    d("Streaming session error: " + e);
                    try {
                        d("Backing off " + backoffMs + " ms before retry");
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        d("Backoff sleep interrupted");
                        Thread.currentThread().interrupt();
                    }
                    backoffMs = Math.min(backoffMs * 2, 15000);
                }

                if (recording.get()) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } catch (Exception fatal) {
            d("Fatal audio setup error: " + fatal);
            synchronized (textLock) {
                content += (content.isEmpty() ? "" : "\n") + "[Audio Error] " + fatal.getMessage();
            }
            safeUpdateUI(content);
        } finally {
            if (systemAudioLine != null) {
                try { systemAudioLine.stop(); d("TargetDataLine.stop() in finally ok"); } catch (Exception ignored) {}
                try { systemAudioLine.close(); d("TargetDataLine.close() in finally ok"); } catch (Exception ignored) {}
                systemAudioLine = null;
            }
        }
    }

    // ======== Observer ========

    private static ResponseObserver<StreamingRecognizeResponse> createObserver() {
        return new ResponseObserver<>() {
            @Override
            public void onStart(StreamController controller) {
                d("Observer.onStart()");
            }

            @Override
            public void onResponse(StreamingRecognizeResponse response) {
                if (response.getResultsCount() == 0) {
                    d("Observer.onResponse(): no results");
                    return;
                }
                StreamingRecognitionResult result = response.getResults(0);
                if (result.getAlternativesCount() == 0) {
                    d("Observer.onResponse(): no alternatives");
                    return;
                }

                String transcript = result.getAlternatives(0).getTranscript().trim();
                String shortTxt = truncateForLog(transcript, 120);

                if (result.getIsFinal()) {
                    d("Observer.onResponse(): FINAL -> \"" + shortTxt + "\"");
                    synchronized (textLock) {
                        currentLine.append(transcript).append(' ');
                        d("currentLine length=" + currentLine.length());
                    }
                    scheduleFinalize();
                    safeUpdateUI(buildPreview());
                } else {
                    d("Observer.onResponse(): interim -> \"" + shortTxt + "\"");
                    synchronized (textLock) {
                        String preview = content + (content.isEmpty() ? "" : "\n")
                                + currentLine.toString() + transcript;
                        safeUpdateUI(preview);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                d("Observer.onError(): " + t);
                streamActive = false;
            }

            @Override
            public void onComplete() {
                d("Observer.onComplete()");
                streamActive = false;
            }
        };
    }

    // ======== Finalization / Debounce ========

    private static void scheduleFinalize() {
        ScheduledFuture<?> old = pauseFuture;
        if (old != null) {
            d("scheduleFinalize(): cancel previous");
            old.cancel(false);
        }
        d("scheduleFinalize(): scheduling in " + PAUSE_TIMEOUT_MS + "ms");
        pauseFuture = scheduler.schedule(AudioListener::maybeFinalizeByHeuristic,
                PAUSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private static void maybeFinalizeByHeuristic() {
        String line;
        synchronized (textLock) {
            line = currentLine.toString();
        }
        long sentences = line.chars().filter(c -> c == '.' || c == '?' || c == '!').count();
        d("maybeFinalizeByHeuristic(): sentences=" + sentences + ", lineLen=" + line.length());
        if (sentences >= 1 || !line.isBlank()) {
            finalizeLine();
        }
    }

    private static void finalizeLine() {
        final String commit;
        synchronized (textLock) {
            commit = currentLine.toString().trim();
            currentLine.setLength(0);
            if (commit.isEmpty()) {
                d("finalizeLine(): nothing to commit");
                return;
            }

            String ts = LocalTime.now().format(TIME_FMT);
            content = content + (content.isEmpty() ? "" : "\n") + "[" + ts + "] " + commit;
            d("finalizeLine(): committed len=" + commit.length() + ", contentLen=" + content.length());
        }
        safeUpdateUI(content);
    }

    private static String buildPreview() {
        synchronized (textLock) {
            String preview = currentLine.toString().trim();
            if (preview.isEmpty()) return content;
            return content + (content.isEmpty() ? "" : "\n") + preview;
        }
    }

    // ======== Audio device ========

    private static AudioFormat desiredFormat() {
        return new AudioFormat(16000.0F, 16, 1, true, false);
    }

    private static TargetDataLine findAndPrepareAudioLine() throws LineUnavailableException {
        AudioFormat format = desiredFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        d("Enumerating mixers for 'Stereo Mix'...");
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            String name = mi.getName() == null ? "" : mi.getName();
            String desc = mi.getDescription() == null ? "" : mi.getDescription();
            d("Mixer: " + name + " | " + desc);
            if (name.toLowerCase().contains("stereo mix")) {
                Mixer m = AudioSystem.getMixer(mi);
                d("Found candidate 'Stereo Mix' -> isLineSupported=" + m.isLineSupported(info));
                if (m.isLineSupported(info)) {
                    try {
                        TargetDataLine l = (TargetDataLine) m.getLine(info);
                        l.open(format);
                        d("Opened 'Stereo Mix' successfully");
                        return l;
                    } catch (LineUnavailableException e) {
                        d("Stereo Mix found but unavailable: " + e);
                    }
                }
            }
        }

        d("Falling back to any mixer that supports TargetDataLine...");
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            Mixer m = AudioSystem.getMixer(mi);
            if (m.isLineSupported(info)) {
                d("Fallback mixer supports line: " + mi.getName());
                TargetDataLine l = (TargetDataLine) m.getLine(info);
                l.open(format);
                d("Opened fallback line successfully");
                return l;
            }
        }

        throw new LineUnavailableException("No compatible audio capture device found (enable 'Stereo Mix' or a microphone).");
    }

    // ======== UI bridge ========

    private static void safeUpdateUI(String text) {
        try {
            Main.s.changeContent(text);
            d("UI updated: len=" + text.length());
        } catch (Throwable t) {
            d("UI update failed: " + t);
        }
    }

    // ======== Debug util ========

    private static void d(String msg) {
//        if (!DEBUG) return;
//        String ts = LocalDateTime.now().format(LOG_TS);
//        System.out.println("[AudioListener " + ts + "] " + msg);
    }

    private static String truncateForLog(String s, int max) {
        if (s == null) return "null";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...(" + s.length() + ")";
    }
}

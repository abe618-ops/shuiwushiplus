package com.mingyang.webtovideo;

import android.app.*;
import android.content.*;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.*;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecordService extends Service {
    private static final int NOTIFY_ID = 2608;
    private static final String CHANNEL = "web_to_video_recording";
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private volatile boolean recording;
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private MediaCodec videoEncoder, audioEncoder;
    private AudioRecord audioRecord;
    private Surface inputSurface;
    private Thread videoDrain, audioDrain, audioFeed;
    private MuxerState mux;
    private Uri outputUri;
    private ParcelFileDescriptor pfd;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopAll();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFY_ID, buildNotification("正在录制网页视频…"));
        if (recording) return START_NOT_STICKY;
        int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
        Intent resultData = intent.getParcelableExtra("resultData");
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            MediaProjectionManager m = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            projection = m.getMediaProjection(resultCode, resultData);
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() { stopAll(); }
            }, new Handler(Looper.getMainLooper()));
            startCapture();
        } catch (Throwable t) {
            postStatus("录制启动失败");
            stopAll();
        }
        return START_NOT_STICKY;
    }

    private void startCapture() throws Exception {
        int[] size = chooseSize();
        int width = size[0], height = size[1], dpi = size[2];

        ContentValues cv = new ContentValues();
        String name = "WebToVideo_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4";
        cv.put(MediaStore.Video.Media.DISPLAY_NAME, name);
        cv.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        cv.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/WebToVideo");
        cv.put(MediaStore.Video.Media.IS_PENDING, 1);
        outputUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv);
        if (outputUri == null) throw new IllegalStateException("无法创建输出文件");
        pfd = getContentResolver().openFileDescriptor(outputUri, "rw");
        if (pfd == null) throw new IllegalStateException("无法打开输出文件");

        MediaFormat vf = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        vf.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        vf.setInteger(MediaFormat.KEY_BIT_RATE, width * height >= 1500000 ? 8_000_000 : 5_000_000);
        vf.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        vf.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        videoEncoder.configure(vf, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        inputSurface = videoEncoder.createInputSurface();

        boolean audioOk = setupPlaybackAudio();
        mux = new MuxerState(new MediaMuxer(pfd.getFileDescriptor(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4), audioOk);

        videoEncoder.start();
        if (audioOk) audioEncoder.start();
        virtualDisplay = projection.createVirtualDisplay("WebToVideo", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, inputSurface, null, null);

        recording = true;
        videoDrain = new Thread(() -> drainEncoder(videoEncoder, true), "video-drain");
        videoDrain.start();
        if (audioOk) {
            audioRecord.startRecording();
            audioDrain = new Thread(() -> drainEncoder(audioEncoder, false), "audio-drain");
            audioFeed = new Thread(this::feedAudio, "audio-feed");
            audioDrain.start();
            audioFeed.start();
        }
        postStatus(audioOk ? "录制中：已启用内部播放音频" : "录制中：该设备/网页不允许内部音频，将保存画面");
    }

    private boolean setupPlaybackAudio() {
        try {
            int sampleRate = 44100;
            int channelMask = AudioFormat.CHANNEL_IN_MONO;
            int min = AudioRecord.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT);
            AudioPlaybackCaptureConfiguration cfg = new AudioPlaybackCaptureConfiguration.Builder(projection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build();
            AudioFormat inFormat = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask).build();
            audioRecord = new AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(cfg)
                    .setAudioFormat(inFormat)
                    .setBufferSizeInBytes(Math.max(min * 4, 32768))
                    .build();
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) return false;

            MediaFormat af = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1);
            af.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            af.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            af.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 32768);
            audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            audioEncoder.configure(af, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            return true;
        } catch (Throwable t) {
            try { if (audioRecord != null) audioRecord.release(); } catch (Throwable ignored) {}
            audioRecord = null;
            try { if (audioEncoder != null) audioEncoder.release(); } catch (Throwable ignored) {}
            audioEncoder = null;
            return false;
        }
    }

    private void feedAudio() {
        byte[] buffer = new byte[16384];
        boolean eosSent = false;
        while (recording) {
            int n = audioRecord.read(buffer, 0, buffer.length);
            if (n <= 0) continue;
            int idx = audioEncoder.dequeueInputBuffer(10000);
            if (idx >= 0) {
                ByteBuffer b = audioEncoder.getInputBuffer(idx);
                if (b != null) {
                    b.clear();
                    b.put(buffer, 0, n);
                    audioEncoder.queueInputBuffer(idx, 0, n, System.nanoTime() / 1000L, 0);
                }
            }
        }
        for (int i = 0; i < 20 && !eosSent; i++) {
            int idx = audioEncoder.dequeueInputBuffer(10000);
            if (idx >= 0) {
                audioEncoder.queueInputBuffer(idx, 0, 0, System.nanoTime() / 1000L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                eosSent = true;
            }
        }
    }

    private void drainEncoder(MediaCodec codec, boolean video) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        try {
            while (true) {
                int out = codec.dequeueOutputBuffer(info, 10000);
                if (out == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!recording && video) continue;
                    continue;
                }
                if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mux.addTrack(video, codec.getOutputFormat());
                    continue;
                }
                if (out >= 0) {
                    ByteBuffer data = codec.getOutputBuffer(out);
                    if (data != null && info.size > 0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        data.position(info.offset);
                        data.limit(info.offset + info.size);
                        mux.write(video, data, info);
                    }
                    boolean eos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    codec.releaseOutputBuffer(out, false);
                    if (eos) break;
                }
            }
        } catch (Throwable ignored) {}
    }

    private int[] chooseSize() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int w, h, dpi;
        if (Build.VERSION.SDK_INT >= 30) {
            Rect b = wm.getCurrentWindowMetrics().getBounds();
            w = b.width(); h = b.height();
            dpi = getResources().getDisplayMetrics().densityDpi;
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            w = dm.widthPixels; h = dm.heightPixels; dpi = dm.densityDpi;
        }
        if (w > 1080) { double s = 1080.0 / w; w = 1080; h = (int)(h * s); }
        if ((w & 1) == 1) w--; if ((h & 1) == 1) h--;
        return new int[]{w, h, dpi};
    }

    private void stopAll() {
        if (!stopping.compareAndSet(false, true)) return;
        recording = false;
        new Thread(() -> {
            try { if (audioRecord != null) audioRecord.stop(); } catch (Throwable ignored) {}
            try { if (videoEncoder != null) videoEncoder.signalEndOfInputStream(); } catch (Throwable ignored) {}
            join(audioFeed); join(audioDrain); join(videoDrain);
            try { if (virtualDisplay != null) virtualDisplay.release(); } catch (Throwable ignored) {}
            try { if (inputSurface != null) inputSurface.release(); } catch (Throwable ignored) {}
            try { if (videoEncoder != null) { videoEncoder.stop(); videoEncoder.release(); } } catch (Throwable ignored) {}
            try { if (audioEncoder != null) { audioEncoder.stop(); audioEncoder.release(); } } catch (Throwable ignored) {}
            try { if (audioRecord != null) audioRecord.release(); } catch (Throwable ignored) {}
            try { if (mux != null) mux.close(); } catch (Throwable ignored) {}
            try { if (pfd != null) pfd.close(); } catch (Throwable ignored) {}
            if (outputUri != null) {
                ContentValues cv = new ContentValues(); cv.put(MediaStore.Video.Media.IS_PENDING, 0);
                try { getContentResolver().update(outputUri, cv, null, null); } catch (Throwable ignored) {}
            }
            try { if (projection != null) projection.stop(); } catch (Throwable ignored) {}
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }, "record-stop").start();
    }

    private void join(Thread t) { if (t != null) try { t.join(2500); } catch (InterruptedException ignored) {} }

    private class MuxerState {
        final MediaMuxer muxer; final boolean expectAudio;
        int v = -1, a = -1; boolean started = false;
        MuxerState(MediaMuxer m, boolean expectAudio) { this.muxer = m; this.expectAudio = expectAudio; }
        synchronized void addTrack(boolean video, MediaFormat f) {
            if (video && v < 0) v = muxer.addTrack(f);
            if (!video && a < 0) a = muxer.addTrack(f);
            if (!started && v >= 0 && (!expectAudio || a >= 0)) { muxer.start(); started = true; notifyAll(); }
        }
        synchronized void write(boolean video, ByteBuffer b, MediaCodec.BufferInfo i) {
            long end = System.currentTimeMillis() + 2500;
            while (!started && System.currentTimeMillis() < end) try { wait(50); } catch (InterruptedException ignored) {}
            if (started) muxer.writeSampleData(video ? v : a, b, i);
        }
        synchronized void close() {
            if (started) try { muxer.stop(); } catch (Throwable ignored) {}
            try { muxer.release(); } catch (Throwable ignored) {}
        }
    }

    private void createChannel() {
        NotificationChannel c = new NotificationChannel(CHANNEL, "网页转视频录制", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(c);
    }
    private Notification buildNotification(String text) {
        Intent stop = new Intent(this, RecordService.class).setAction("STOP");
        PendingIntent pi = PendingIntent.getService(this, 2, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("网页转视频")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "停止并保存", pi).build())
                .build();
    }
    private void postStatus(String text) {
        getSystemService(NotificationManager.class).notify(NOTIFY_ID, buildNotification(text));
    }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}

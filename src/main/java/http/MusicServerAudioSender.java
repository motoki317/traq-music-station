package http;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import music.MusicServerAudioProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class MusicServerAudioSender {
    private final static long interval = 20;

    private final MusicServerAudioProvider provider;
    private final OutputStream out;
    private final ScheduledExecutorService executor;
    private boolean headersSent;
    private final ScheduledFuture<?> canceller;

    MusicServerAudioSender(AudioPlayer player, OutputStream out) {
        this.provider = new MusicServerAudioProvider(player);
        this.out = out;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.canceller = this.executor.scheduleAtFixedRate(this::provide, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void provide() {
        try {
            if (!provider.canProvide()) return;
            if (!headersSent) {
                byte[] h0 = provider.headerPageZero();
                out.write(h0);
                byte[] h1 = provider.headerPageOne();
                out.write(h1);
                headersSent = true;
            }
            byte[] p = provider.provide20MsAudio();
            if (p.length == 0) return;
            out.write(p);
        } catch (IOException e) {
            e.printStackTrace();
            canceller.cancel(false);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

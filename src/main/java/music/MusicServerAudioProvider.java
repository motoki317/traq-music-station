package music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.chenliang.oggus.ogg.OggPage;
import org.chenliang.oggus.opus.*;

import java.util.Random;
import java.util.zip.CRC32;

public class MusicServerAudioProvider {
    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;
    private long seq;
    private long granulePosition;
    private final long serialNum;
    private OggPage page;
    private int bufferedPackets;
    private final int internalBufferPackets;

    public MusicServerAudioProvider(AudioPlayer audioPlayer, int internalBufferPackets) {
        this.audioPlayer = audioPlayer;
        this.seq = 2;
        this.granulePosition = 0;
        this.serialNum = new Random().nextLong();
        this.page = OggPage.empty();
        this.bufferedPackets = 0;
        this.internalBufferPackets = internalBufferPackets;
    }

    public int internalBufferedPackets() {
        return this.bufferedPackets;
    }

    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    private static void setCheckSum(OggPage page) {
        CRC32 crc32 = new CRC32();
        crc32.update(page.dump());
        page.setCheckSum((int) crc32.getValue());
    }

    /**
     * Generates bitstream for the ID Header and Comment Header for OGG Opus stream.
     * Should be called AFTER the first true returned by {@link #canProvide()},
     * and sent before providing audio.
     * @return Bytes.
     */
    public byte[] headerPageZero() {
        // https://tools.ietf.org/html/rfc7845
        var headerPage = OggPage.empty();
        var header = IdHeader.emptyHeader();
        var format = lastFrame.getFormat();
        header.setChannelCount(format.channelCount);
        header.setInputSampleRate(format.sampleRate);
        header.setMinorVersion(1);
        headerPage.addDataPacket(header.dump());
        headerPage.setSeqNum(0);
        headerPage.setBOS();
        headerPage.setSerialNum(serialNum);

        setCheckSum(headerPage);
        return headerPage.dump();
    }

    public byte[] headerPageOne() {
        var commentPage = OggPage.empty();
        var comment = CommentHeader.emptyHeader();
        comment.setVendor("libopus 1.1");
        byte[] buf = new byte[764];
        byte[] comm = comment.dump();
        System.arraycopy(comm, 0, buf, 0, comm.length);

        commentPage.addDataPacket(buf);
        commentPage.setSeqNum(1);
        commentPage.setSerialNum(serialNum);

        setCheckSum(commentPage);
        return commentPage.dump();
    }

    public byte[] provide20MsAudio() {
        OpusPacket packet = OpusPackets.from(lastFrame.getData());

        var audioDataPacket = AudioDataPacket.empty();
        audioDataPacket.addOpusPacket(packet);

        this.page.addDataPacket(audioDataPacket.dump());
        // 48000 Hz / 1000 * 20ms = 960
        this.granulePosition += 960;
        this.bufferedPackets++;

        if (this.bufferedPackets < this.internalBufferPackets) {
            return new byte[]{};
        }

        byte[] data = dumpPage();
        this.page = OggPage.empty();
        this.bufferedPackets = 0;
        return data;
    }

    private byte[] dumpPage() {
        page.setGranulePosition(this.granulePosition);
        page.setSeqNum(this.seq);
        page.setSerialNum(this.serialNum);
        this.seq++;

        setCheckSum(page);
        return page.dump();
    }

    public byte[] flush() {
        System.out.println("[Music Provider] flushed");
        this.page.setEOS();
        byte[] data = dumpPage();
        this.page = OggPage.empty();
        this.bufferedPackets = 0;
        return data;
    }
}

package db.model.musicInterruptedChannel;

public class MusicInterruptedChannel implements MusicInterruptedChannelId {
    private final String vcId;
    private final String textChannelId;

    public MusicInterruptedChannel(String vcId, String textChannelId) {
        this.vcId = vcId;
        this.textChannelId = textChannelId;
    }

    public String getVcId() {
        return vcId;
    }

    public String getTextChannelId() {
        return textChannelId;
    }
}

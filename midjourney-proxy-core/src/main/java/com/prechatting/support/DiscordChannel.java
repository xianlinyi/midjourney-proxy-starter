package com.prechatting.support;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DiscordChannel {

    private String guildId;

    private String channelId;

    private String userToken;

    private String sessionId = "9c4055428e13bcbf2248a6b36084c5f3";

    private String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";
}

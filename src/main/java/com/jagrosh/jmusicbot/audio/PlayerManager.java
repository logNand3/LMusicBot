/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.audio;

import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.github.topi314.lavasrc.applemusic.AppleMusicSourceManager;
import com.github.topi314.lavasrc.spotify.SpotifySourceManager;
import com.github.topi314.lavasrc.yandexmusic.YandexMusicSourceManager;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.BotConfig;
import com.jagrosh.jmusicbot.utils.YouTubeUtil;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.planner.BalancingIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.planner.NanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv4Block;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.Web;
import net.dv8tion.jda.api.entities.Guild;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PlayerManager extends DefaultAudioPlayerManager
{
    private final Bot bot;
    
    private final BotConfig config;

    public PlayerManager(Bot bot, BotConfig config)
    {
        this.bot = bot;
        this.config = config;
    }

    public void init()
    {
        TransformativeAudioSourceManager.createTransforms(bot.getConfig().getTransforms()).forEach(t -> registerSourceManager(t));

        if (config.getYTPoToken() != null && config.getYTVisitorData() != null)
            Web.setPoTokenAndVisitorData(config.getYTPoToken(), config.getYTVisitorData());

        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager(true);
        if (config.getYTRoutingPlanner() != YouTubeUtil.RoutingPlanner.NONE)
        {
            AbstractRoutePlanner routePlanner = YouTubeUtil.createRouterPlanner(config.getYTRoutingPlanner(), config.getYTIpBlocks());
            YoutubeIpRotatorSetup rotator = new YoutubeIpRotatorSetup(routePlanner);

            rotator.forConfiguration(yt.getHttpInterfaceManager(), false)
                    .withMainDelegateFilter(yt.getContextFilter())
                    .setup();
        }

        yt.setPlaylistPageCount(bot.getConfig().getMaxYTPlaylistPages());
        registerSourceManager(yt);

        if(!"NONE".equals(bot.getConfig().getSpotifyID()) && !"NONE".equals(bot.getConfig().getSpotifySecret()))
        {
            this.registerSourceManager(new SpotifySourceManager(null, bot.getConfig().getSpotifyID(), bot.getConfig().getSpotifySecret(), "NL", this));
        }

        if(!"NONE".equals(bot.getConfig().getAppleAPI()))
        {
            this.registerSourceManager(new AppleMusicSourceManager(null, bot.getConfig().getAppleAPI() , "us", this));
        }

        if(!"NONE".equals(bot.getConfig().getYandexAPI()))
        {
            this.registerSourceManager(new YandexMusicSourceManager(bot.getConfig().getYandexAPI()));
        }

        registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        registerSourceManager(new BandcampAudioSourceManager());
        registerSourceManager(new VimeoAudioSourceManager());
        registerSourceManager(new TwitchStreamAudioSourceManager());
        registerSourceManager(new BeamAudioSourceManager());
        registerSourceManager(new GetyarnAudioSourceManager());
        registerSourceManager(new NicoAudioSourceManager());
        registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));

        AudioSourceManagers.registerLocalSource(this);
        AudioSourceManagers.registerRemoteSources(this);

        DuncteBotSources.registerAll(this, "en-US");

        source(YoutubeAudioSourceManager.class).setPlaylistPageCount(10);
    }
    
    public Bot getBot()
    {
        return bot;
    }
    
    public boolean hasHandler(Guild guild)
    {
        return guild.getAudioManager().getSendingHandler()!=null;
    }
    
    public AudioHandler setUpHandler(Guild guild)
    {
        AudioHandler handler;
        if(guild.getAudioManager().getSendingHandler()==null)
        {
            AudioPlayer player = createPlayer();
            player.setVolume(bot.getSettingsManager().getSettings(guild).getVolume());
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);
            guild.getAudioManager().setSendingHandler(handler);
        }
        else
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        return handler;
    }
}

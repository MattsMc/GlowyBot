package me.codingmatt.twitch;

import me.codingmatt.twitch.objects.IRCConnection;
import me.codingmatt.twitch.objects.MySQLConnection;
import me.codingmatt.twitch.utils.*;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

import org.pircbotx.hooks.Listener;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.krb5.Config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Copyright (c) 2015 Matthew Smeets - http://www.codingmatt.me
 */
public class TwitchBot {

    public static final Logger logger = LoggerFactory.getLogger(TwitchBot.class.getName());
    public static final Reflections reflection = new Reflections("me.codingmatt.twitch");

    public static ConfigParser configParser;
    public static MySQLConnection mySQLConnection;

    public static boolean databaseFallback=false;

    public static Channels channels;
    public static ArrayList<String> channelsJoined = new ArrayList<String>();

    public static String VERSION;

    public static void main(String[] args){
        configParser = new ConfigParser("config.xml");

        VERSION = ConfigParser.getVersion();

        new Updater();

        mySQLConnection = configParser.setupSQLFromConfig();
        mySQLConnection.testConnection();

        channels = new Channels();
        try {
            channelsJoined = channels.channelsToJoin();
        } catch (Exception e){
            e.printStackTrace();
        }
        Viewers.updateViewers.start();

        new TwitchBot();

    }

    public TwitchBot() {
            IRCConnection config = configParser.servers.get(0); //TODO: Set-up multibot

            Configuration.Builder confi = new Configuration.Builder();
            confi.setName(config.getName());
            confi.setLogin(config.getLogin());
            confi.setVersion(config.getVersion());
            confi.setAutoNickChange(config.doAutoNickChange());
            confi.setServerHostname(config.getServerHost());
            confi.setServerPort(config.getServerPort());
            confi.setServerPassword(config.getServerPassword());
            confi.addAutoJoinChannel(config.getDefaultChannel());
            confi.setEncoding(Charset.isSupported("UTF-8") ? Charset.forName("UTF-8") : Charset.defaultCharset());

            Registry.setBaseListeners();

            for (Listener listen : Registry.listeners) {
                confi.addListener(listen);
            }

            for (String channel : channelsJoined) {
                confi.addAutoJoinChannel(channel);
            }

            PircBotX bot = new PircBotX(confi.buildConfiguration());

            try {
                logger.info("Starting " + confi.getName() + " | version " + confi.getVersion());
                bot.startBot();
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            } catch (IrcException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
    }

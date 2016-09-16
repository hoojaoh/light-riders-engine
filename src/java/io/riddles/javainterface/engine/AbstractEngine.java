/*
 * Copyright 2016 riddles.io (developers@riddles.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     For the full copyright and license information, please view the LICENSE
 *     file that was distributed with this source code.
 */

package io.riddles.javainterface.engine;

import com.mongodb.BasicDBObject;

import io.riddles.javainterface.configuration.Configuration;
import io.riddles.javainterface.exception.TerminalException;

import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.riddles.javainterface.game.player.AbstractPlayer;
import io.riddles.javainterface.game.processor.AbstractProcessor;
import io.riddles.javainterface.game.state.AbstractState;
import io.riddles.javainterface.io.IOHandler;
import io.riddles.javainterface.io.IOInterface;
import io.riddles.javainterface.theaigames.connections.Amazon;
import io.riddles.javainterface.theaigames.connections.Database;
import io.riddles.javainterface.theaigames.io.AIGamesBotIOHandler;
import io.riddles.javainterface.theaigames.io.AIGamesIOHandler;

/**
 * io.riddles.javainterface.engine.AbstractEngine - Created on 2-6-16
 *
 * DO NOT EDIT THIS FILE
 *
 * The engine in the main project should extend this abstract class.
 * This class handles everything the game engine needs to do to start, run and finish.
 * Quite a lot of methods have already been implemented, but some need to
 * be Overridden in the Subclass. An object of the Subclass of AbstractEngine should
 * be created in the Main method of the project and then the engine is started with
 * engine.run()
 *
 * @author Jim van Eeden - jim@riddles.io
 */
public abstract class AbstractEngine<Pr extends AbstractProcessor,
        Pl extends AbstractPlayer, S extends AbstractState> {

    protected final static Logger LOGGER = Logger.getLogger(AbstractEngine.class.getName());
    public final static Configuration configuration = new Configuration();

    protected String[] botInputFiles;

    protected IOInterface ioHandler;
    protected ArrayList<Pl> players;
    protected Pr processor;

    // Can be overridden in subclass constructor
    protected GameLoop gameLoop;

    // AIGames
    private ArrayList<String> botCommands;
    private ArrayList<String> mongoIds;
    private String aigamesIdString;
    private final String runBotCommand = "/opt/aigames/scripts/run_bot.sh";

    protected AbstractEngine() {
        this.players = new ArrayList<>();
        this.gameLoop = new SimpleGameLoop();
        this.ioHandler = new IOHandler();
    }

    // with args is a TheAIGames engine
    protected AbstractEngine(String args[]) throws TerminalException {
        this.players = new ArrayList<>();
        this.gameLoop = new SimpleGameLoop();

        this.botCommands = new ArrayList<>();
        this.mongoIds = new ArrayList<>();

        parseAIGamesArguments(args);

        this.ioHandler = new AIGamesIOHandler((args.length - 1) / 2);
    }

    /**
     * Initializes the engine in debug mode
     * @param wrapperInputFile Input file from the wrapper
     * @param botInputFiles Input files for the bots
     */
    protected AbstractEngine(String wrapperInputFile, String[] botInputFiles) {
        this.players = new ArrayList<>();
        this.gameLoop = new SimpleGameLoop();
        this.ioHandler = new IOHandler(wrapperInputFile);
        this.botInputFiles = botInputFiles;
    }

    /**
     * This method starts the engine. Should be called from the main
     * method in the project.
     */
    public void run() throws TerminalException, InterruptedException {
        LOGGER.info("Starting...");

        setup();

        if (this.processor == null) {
            throw new TerminalException("Processor has not been set");
        }

        LOGGER.info("Running pre-game phase...");

        this.processor.preGamePhase();


        LOGGER.info("Starting game loop...");

        S initialState = getInitialState();
        this.gameLoop.run(initialState, this.processor);

        if (this.botCommands == null) {
            finish(initialState);
        } else {
            finishAIGames(initialState);
        }
    }

    /**
     * Does everything needed before the game can start, such as
     * getting the amount of players, setting the processor and sending
     * the game settings to the bots.
     */
    protected void setup() {
        LOGGER.info("Setting up engine. Waiting for initialize...");

        this.ioHandler.waitForMessage("initialize");
        this.ioHandler.sendMessage("ok");

        LOGGER.info("Got initialize. Parsing settings...");

        try {
            String line = "";
            while (!line.equals("start")) { // from "start", setup is done
                line = this.ioHandler.getNextMessage();
                parseSetupInput(line);
            }
        } catch(IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }

        this.processor = createProcessor();

        LOGGER.info("Got start. Sending game settings to bots...");

        this.players.forEach(this::sendGameSettings);

        LOGGER.info("Settings sent. Setting up engine done...");
    }

    /**
     * Does everything needed to send the GameWrapper the results of
     * the game.
     * @param initialState The start-of-game state
     */
    protected void finish(S initialState) {

        // let the wrapper know the game has ended
        this.ioHandler.sendMessage("end");

        // send game details
        this.ioHandler.waitForMessage("details");

        AbstractPlayer winner = this.processor.getWinner();
        String winnerId = "null";
        if (winner != null) {
            winnerId = winner.getId() + "";
        }

        JSONObject details = new JSONObject();
        details.put("winner", winnerId);
        details.put("score", this.processor.getScore());

        this.ioHandler.sendMessage(details.toString());

        // send the game file
        this.ioHandler.waitForMessage("game");
        this.ioHandler.sendMessage(getPlayedGame(initialState));
    }

    /**
     * Parses everything the engine wrapper API sends
     * we need to start the engine, like IDs of the bots
     * @param input Input from engine wrapper
     */
    protected void parseSetupInput(String input) throws IOException {
        String[] split = input.split(" ");
        String command = split[0];
        switch (command) {
            case "bot_ids":
                String[] ids = split[1].split(",");
                for (int i = 0; i < ids.length; i++) {
                    int id = Integer.parseInt(ids[i]);
                    Pl player = createPlayer(id);

                    if (this.botCommands != null) {
                        Process botProcess = createBotProcess(id);
                        String botMongoId = this.mongoIds.get(id - 1);

                        player.setAsAIGamesPlayer(botProcess, botMongoId);
                        sendAIGamesSettings(player, ids);
                    } else if (this.botInputFiles != null) {
                        player.setInputFile(this.botInputFiles[i]);
                    }

                    this.players.add(player);
                }
                break;
            case "configuration":
            case "config":
                JSONObject config = new JSONObject(split[1].trim());
                Iterator<String> keys = config.keys();

                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject configValue = config.getJSONObject(key);
                    configuration.put(key, configValue);
                }
                break;
        }
    }

    /**
     * Implement this to return the initial (mostly empty) game state.
     * @return The initial state of the game, should be Subclass of AbstractState
     */
    protected abstract S getInitialState();

    /**
     * Implement this to return a player in the game.
     * @param id Id of the player
     * @return Object that is Subclass of AbstractPlayer
     */
    protected abstract Pl createPlayer(int id);

    /**
     * Implement this to return the processor for the game.
     * @return Object that is Subclass of AbstractProcessor
     */
    protected abstract Pr createProcessor();

    /**
     * Send the settings to the player (bot) that are specific to this game
     * @param player Player to send the settings to
     */
    protected abstract void sendGameSettings(Pl player);

    /**
     * Return the string representation of the entire game to use in
     * the visualizer
     * @param initialState The initial state of the game (can be used
     *                     to go the next game states).
     * @return String representation of the entire game
     */
    protected abstract String getPlayedGame(S initialState);

    /**
     * @return The players for the game
     */
    public ArrayList<Pl> getPlayers() {
        return this.players;
    }

    /**
     * @return The processor for the game
     */
    public Pr getProcessor() {
        return this.processor;
    }


    // Stuff for TheAIGames engine

    private void parseAIGamesArguments(String args[]) throws TerminalException {

        if (args.length <= 0 || args.length % 2 == 0) {
            throw new TerminalException("AIGames engine: Wrong number of argument provided.");
        }

        this.aigamesIdString = args[0];

        int halfIndex = (args.length - 1) / 2;
        for (int i = 1; i <= halfIndex; i++) {
            this.mongoIds.add(args[i]);
            this.botCommands.add(args[i + halfIndex]);
        }
    }

    private Process createBotProcess(int id) throws IOException {
        String command = String.format(
                "%s aiplayer%d %s", this.runBotCommand, id, this.botCommands.get(id - 1));

        return Runtime.getRuntime().exec(command);
    }

    private void sendAIGamesSettings(AbstractPlayer player, String[] ids) {
        String playerNames = "";
        String connector = "";
        for (String id : ids) {
            playerNames += String.format("%splayer%s", connector, id);
            connector = ",";
        }

        AIGamesBotIOHandler bot = (AIGamesBotIOHandler) player.getIoHandler();
        bot.sendMessage(String.format("settings player_names %s", playerNames));
        bot.sendMessage(String.format("settings your_bot player%d", player.getId()));
        bot.sendMessage(String.format("settings timebank %d", bot.getMaxTimebank()));
        bot.sendMessage(String.format("settings time_per_move %d", bot.getTimePerMove()));
    }

    private void finishAIGames(S initialState) throws InterruptedException {
        for (AbstractPlayer player : this.players) {
            ((AIGamesBotIOHandler) player.getIoHandler()).finish();
        }

        Thread.sleep(100);

        saveToAIGames(initialState);

        System.err.println("Done.");
        System.exit(0);
    }

    private void saveToAIGames(S initialState) {
        int score = (int) this.processor.getScore();
        String playedGame = getPlayedGame(initialState);
        AbstractPlayer winner = this.processor.getWinner();
        String gamePath = "games/" + this.aigamesIdString;

        BasicDBObject errors = new BasicDBObject();
        BasicDBObject dumps = new BasicDBObject();

        ObjectId winnerId = null;
        if (winner != null) {
            System.err.println("winner: " + winner.getName());
            winnerId = new ObjectId(this.mongoIds.get(winner.getId() - 1));
        } else {
            System.err.println("winner: draw");
        }

        System.err.println("Saving the game...");

        // Save visualization file to Amazon
        Amazon.connectToAmazon();
        String visualizationFile = Amazon.saveToAmazon(playedGame, gamePath + "/visualization");

        // Save errors and dumps to Amazon and create object for database
        for (AbstractPlayer player : this.players) {
            String botId = this.mongoIds.get(player.getId() - 1);
            AIGamesBotIOHandler ioHandler = (AIGamesBotIOHandler) player.getIoHandler();

            String errorPath = String.format("%s/bot%dErrors", gamePath, player.getId());
            String dumpPath = String.format("%s/bot%dDump", gamePath, player.getId());

            String errorLink = Amazon.saveToAmazon(ioHandler.getStderr(), errorPath);
            String dumpLink = Amazon.saveToAmazon(ioHandler.getDump(), dumpPath);

            errors.append(botId, errorLink);
            dumps.append(botId, dumpLink);
        }

        // store everything in the database
        Database.connectToDatabase();
        Database.storeGameInDatabase(this.aigamesIdString, winnerId,
                score, visualizationFile, errors, dumps);
    }
}

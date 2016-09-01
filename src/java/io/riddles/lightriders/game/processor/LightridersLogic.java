package io.riddles.lightriders.game.processor;

import io.riddles.lightriders.game.board.LightridersBoard;
import io.riddles.lightriders.game.player.LightridersPlayer;

import io.riddles.lightriders.game.move.LightridersMove;
import io.riddles.lightriders.game.state.LightridersState;

import java.awt.*;

/**
 * io.riddles.lightriders.game.processor.LightridersLogic - Created on 6/27/16
 *
 * Provides logic to transform a LightridersState with a LightridersMove.
 *
 * @author Joost - joost@riddles.io, Jim van Eeden - jim@riddles.io
 */
public class LightridersLogic {


    public LightridersLogic() {
    }

    /**
     * Takes a LightridersState and transforms it with a LightridersMove.
     *
     * Return
     * Returns nothing, but transforms the given LightridersState.
     * @param LightridersState The initial state
     * @param LightridersPlayer The player involved
     * @param LightridersMove The move of the player
     * @return
     */
    public void transform(LightridersState state, LightridersPlayer player, LightridersMove move) {

        int pId = player.getId();
        LightridersBoard board = state.getBoard();

        if (move.getMoveType() != null) { /* MoveType = null when it has an Exception */
            switch (move.getMoveType()) {
                case UP:
                    player.setDirection(move.getMoveType());
                    break;
                case DOWN:
                    player.setDirection(move.getMoveType());
                    break;
                case RIGHT:
                    player.setDirection(move.getMoveType());
                    break;
                case LEFT:
                    player.setDirection(move.getMoveType());
                    break;
            }
        }

        Point c = player.getCoordinate();
        Point newC = c;

        switch(player.getDirection()) {
            case UP:
                if (c.getY() > 0) {
                    if (board.isEmpty(new Point(c.x, c.y - 1))) {
                        newC = new Point(c.x, c.y - 1);
                    }
                } else {
                    player.kill();
                }
                break;
            case DOWN:
                if (c.getY() < board.getHeight()-1) {
                    if (board.isEmpty(new Point(c.x, c.y + 1))) {
                        newC = new Point(c.x, c.y + 1);
                    }
                } else {
                    player.kill();
                }
                break;
            case RIGHT:
                if (c.getX() < board.getWidth()-1) {
                    if (board.isEmpty(new Point(c.x+1, c.y))) {
                        newC = new Point(c.x + 1, c.y);
                    }
                } else {
                    player.kill();
                }
                break;
            case LEFT:
                if (c.getX() > 0) {
                    if (board.isEmpty(new Point(c.x - 1, c.y))) {
                        newC = new Point(c.x - 1, c.y);
                    }
                } else {
                    player.kill();
                }
                break;
        }

        if (board.isEmpty(newC)) {
            board.setFieldAt(newC, player.getColor().toString().substring(0,1));
            player.setCoordinate(newC);
        } else {
            player.kill();
        }
    }
}

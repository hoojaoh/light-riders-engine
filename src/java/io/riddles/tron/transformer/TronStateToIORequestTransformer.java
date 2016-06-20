package io.riddles.tron.transformer;

import io.riddles.boardgame.model.*;
import io.riddles.game.io.IORequest;
import io.riddles.game.transformer.Transformer;
import io.riddles.tron.TronLogic;
import io.riddles.tron.TronPiece;
import io.riddles.tron.TronPiece.PieceColor;
import io.riddles.tron.TronState;
import io.riddles.tron.io.TronIORequest;
import io.riddles.tron.io.TronIORequestType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Transforms a ChessState into an IORequest
 */
public class TronStateToIORequestTransformer implements Transformer<TronState, IORequest> {

    public TronStateToIORequestTransformer() {
    }

    /**
     * @inheritDoc
     */
    @Override
    public IORequest transform(TronState state) {

        Optional<Move> optionalPreviousMove = state.getMove();

        // Handle game start
        if (!optionalPreviousMove.isPresent()) {
            return createInitialMoveRequest(state);
        }

        // Everything else is a simple move request
        return createMoveRequest(state);
    }

    /**
     * Creates an IORequest for the next player to move a piece,
     * based on the passed state and move
     * @param state The state upon which to base the next move
     * @return IORequest for the next player to move a piece
     */
    protected IORequest createMoveRequest(TronState state) {

    	PieceColor colorToMove = null;
    	
    	/* Figure out next player */
    	colorToMove = state.getActivePieceColor();
    	
    	return new TronIORequest(colorToMove, TronIORequestType.MOVE);
    }

	public PieceColor getColorToMove(TronState state) {
		
		List<PieceColor> currentLivingPieceColors = new TronLogic().getLivingPieceColors(state);
		
		if (!state.getPreviousState().isPresent()) {
			if (currentLivingPieceColors.size() > 1) {
				return currentLivingPieceColors.get(1);
			} else {
				throw new IllegalArgumentException("Not enough players left.");
			}
		}
		
		TronState previousState = state.getPreviousState().get();
		
		List<PieceColor> livingPieceColors = new TronLogic().getLivingPieceColors(previousState);
    	PieceColor c = state.getActivePieceColor();
    	int i = livingPieceColors.indexOf(c);
    	List<PieceColor> beforePieceColors;
    	
    	try {
	    	beforePieceColors = livingPieceColors.subList(0, i);
    	} catch (IllegalArgumentException e ) {
    		throw new IllegalArgumentException("No players left alive");
    	}
    	List<PieceColor> afterPieceColors = livingPieceColors.subList(i+1, livingPieceColors.size());
    	
    	List<PieceColor> remainingActivePieceColors = new ArrayList<>();
    	remainingActivePieceColors.addAll(afterPieceColors);
    	remainingActivePieceColors.addAll(beforePieceColors);
    	
    	for (PieceColor remainingAlive : remainingActivePieceColors) {
    		if (currentLivingPieceColors.indexOf(remainingAlive) != -1) {
    			return remainingAlive;
    		}
    	}
		throw new IllegalArgumentException("No players left alive");
    }
    
    /**
     * Creates an IORequest for YELLOW to move a piece (ie. first move of the game)
     * @return IORequest for YELLOW to move a piece
     */
    protected IORequest createInitialMoveRequest(TronState state) {
    	
    	PieceColor c = getColorToMove(state);
        return new TronIORequest(c, TronIORequestType.MOVE);
    }


    /**
     * Returns the PieceColor of the last moved piece
     * @param state The state from which to retrieve the last moved piece
     * @return ChessPieceColor of the last moved piece
     */
    protected PieceColor getColorOfMovedPiece(TronState state) {

        Piece piece = getMovedPiece(state);
        return (PieceColor) piece.getColor();
    }

    /**
     * Get the piece which was moved in TronState
     * @param state The state from which to retrieve the last moved piece
     * @return The last moved piece
     */
    protected TronPiece getMovedPiece(TronState state) {

        Move move = getLastMove(state);
        Coordinate coordinate = move.getTo();

        Optional<Piece> optionalPiece = state
                .getBoard()
                .getFieldAt(coordinate)
                .getPiece();

        if(!optionalPiece.isPresent()) {
            throw new RuntimeException("No piece present at target coordinate");
        }

        return (TronPiece) optionalPiece.get();
    }

    /**
     * Gets the last executed move
     * @param state The state from which to retrieve the last move
     * @return The last executed move
     */
    protected Move getLastMove(TronState state) {

        Optional<Move> optionalPreviousMove = state.getMove();

        if(!optionalPreviousMove.isPresent()) {
            throw new RuntimeException("No move present in given state");
        }

        return optionalPreviousMove.get();
    }
}
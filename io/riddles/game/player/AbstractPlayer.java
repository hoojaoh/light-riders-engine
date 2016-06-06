// Copyright 2016 theaigames.com (developers@theaigames.com)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//	
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

package io.riddles.game.player;

import io.riddles.game.io.Identifier;

/**
 * AbstractPlayer class
 * 
 * DO NOT EDIT THIS FILE.
 * 
 * Extend this abstract class to store information about the player, to get 
 * bot responses and to send the bot information.
 * Extra methods and variables can be added to handle game specific stuff.
 * 
 * @author Jim van Eeden <jim@starapple.nl>
 */

public abstract class AbstractPlayer {
	
	private String name;
	
	public AbstractPlayer(String name, Identifier id) {
		this.name = name;
	}
	
	/**
	 * @return : The String name of this Player
	 */
	public String getName() {
		return name;
	}

	/**
	 * Asks the bot for given move type and returns the answer
	 * @param moveType : type of move the bot has to return
	 * @return : the bot's output
	 */
	public String requestMove(String moveType) {
		/*
		long startTime = System.currentTimeMillis();
		
		// write the request to the bot
		sendLine(String.format("action %s %d", moveType, this.timeBank));

		// wait for the bot to return his response
		String response = this.bot.getResponse(this.timeBank);
		
		// update the timebank
		long timeElapsed = System.currentTimeMillis() - startTime;
		updateTimeBank(timeElapsed);
		
		return response;
		*/
		return "";
	}
}

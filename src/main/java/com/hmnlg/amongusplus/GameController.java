/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.Instant;

/**
 *
 * @author maikotui
 * @param <T>
 */
public class GameController<T extends GameDisplay> {

	private final T display;

	private final GameRoleManager roleManager;

	private GameState state;

	private Instant lastGameStateChange;

	private boolean vetoUsed;
	private boolean executeUsed;
	private boolean detectUsed;

	public GameController(T d, List<Long> playerIDs, List<GameRole> roles) {
		roleManager = new GameRoleManager(playerIDs, roles);

		String[] roleNames = new String[roles.size()];
		String[] roleDescriptions = new String[roles.size()];
		for (int i = 0; i < roles.size(); i++) {
			GameRole role = roles.get(i);
			roleNames[i] = role.name;
			roleDescriptions[i] = role.description;
		}
		
		state = GameState.NEW;
		lastGameStateChange = new Instant();

		d.showStart(playerIDs, roleNames, roleDescriptions);
		display = d;
	}

	public void restartGame() {
		roleManager.resetRoles();

		vetoUsed = false;
		executeUsed = false;
		detectUsed = false;
		state = GameState.NEW;
		lastGameStateChange = new Instant();

		String[] roleNames = new String[roleManager.playableRoles.size()];
		String[] roleDescriptions = new String[roleManager.playableRoles.size()];
		for (int i = 0; i < roleManager.playableRoles.size(); i++) {
			GameRole role = roleManager.playableRoles.get(i);
			roleNames[i] = role.name;
			roleDescriptions[i] = role.description;
		}

		display.showStart(roleManager.getAllPlayers(), roleNames, roleDescriptions);
	}

	public void moveToReadyUp() throws GeneralGameException {
		if (state == GameState.READYUP || state == GameState.ACTIVE) {
			throw new GeneralGameException(
					"Among Us+ game is " + state.toString().toLowerCase() + ". Please run stop command first.");
		}

		state = GameState.READYUP;
		lastGameStateChange = new Instant();

		List<Long> playersNotReady = roleManager.getPlayersWithoutRoles();
		List<Long> allPlayers = roleManager.getAllPlayers();
		allPlayers.removeAll(playersNotReady);
		display.showReadyUp(allPlayers, playersNotReady);
	}

	public void readyUp(Long user, GameRole chosenRole) {
		if (chosenRole != null) {
			if (roleManager.tryAssignRole(user, chosenRole)) {
				if (roleManager.allPlayersAreReady()) {

					try {
						// Game has started, distribute non default roles
						roleManager.distributeNonDefaultRoles();
					} catch (GeneralGameException ex) {
						Logger.getLogger(GameController.class.getName()).log(Level.SEVERE, null, ex);
						return;
					}

					// Send role assignment message for non default roles and send everyone a game
					// starting message
					roleManager.getAllPlayers().forEach(playerID -> {
						roleManager.getRolesForPlayer(playerID).stream().filter(role -> (!role.isDefault))
								.forEachOrdered(role -> {
									display.showPlayerMessage(playerID, role.assignmentMessage);
								});
					});

					// Get a list of all the roles
					String[] roleNames = new String[roleManager.playableRoles.size()];
					String[] roleDescriptions = new String[roleManager.playableRoles.size()];
					for (int i = 0; i < roleManager.playableRoles.size(); i++) {
						GameRole role = roleManager.playableRoles.get(i);
						roleNames[i] = role.name;
						roleDescriptions[i] = role.description;
					}

					state = GameState.ACTIVE;
					lastGameStateChange = new Instant();

					display.showActiveGame(roleManager.getAllPlayers(), roleNames, roleDescriptions);
				} else {
					List<Long> playersNotReady = roleManager.getPlayersWithoutRoles();
					List<Long> allPlayers = roleManager.getAllPlayers();
					allPlayers.removeAll(playersNotReady);
					display.showReadyUp(allPlayers, playersNotReady);
				}
			}
		}
	}

	public void redisplayGame() {
		display.reshowDisplay(GameState.ACTIVE);
	}

	public void endGame() {
		display.showGameEnded();
	}

	public Instant getLastGameStateChangeTime() {
		return lastGameStateChange;
	}

	public List<Long> getAllPlayers() {
		return roleManager.getAllPlayers();
	}

	public boolean hasPlayer(Long userID) {
		return roleManager.getAllPlayers().contains(userID);
	}

	public boolean addPlayer(Long userID) {
		boolean changeOccurred = state == GameState.NEW && roleManager.addPlayer(userID);
		
		if(changeOccurred) {
			String[] roleNames = new String[roleManager.playableRoles.size()];
			String[] roleDescriptions = new String[roleManager.playableRoles.size()];
			for (int i = 0; i < roleManager.playableRoles.size(); i++) {
				GameRole r = roleManager.playableRoles.get(i);
				roleNames[i] = r.name;
				roleDescriptions[i] = r.description;
			}

			display.showStart(roleManager.getAllPlayers(), roleNames, roleDescriptions);
		}
		
		return changeOccurred;
	}

	public boolean removePlayer(Long userID) {
		boolean changeOccurred = state == GameState.NEW && roleManager.removePlayer(userID);
		
		if(changeOccurred) {
			String[] roleNames = new String[roleManager.playableRoles.size()];
			String[] roleDescriptions = new String[roleManager.playableRoles.size()];
			for (int i = 0; i < roleManager.playableRoles.size(); i++) {
				GameRole r = roleManager.playableRoles.get(i);
				roleNames[i] = r.name;
				roleDescriptions[i] = r.description;
			}

			display.showStart(roleManager.getAllPlayers(), roleNames, roleDescriptions);
		}
		
		return changeOccurred;
	}

	public boolean addRole(GameRole role) {
		boolean changeOccurred = state == GameState.NEW && roleManager.addRole(role);
		
		if(changeOccurred) {
			String[] roleNames = new String[roleManager.playableRoles.size()];
			String[] roleDescriptions = new String[roleManager.playableRoles.size()];
			for (int i = 0; i < roleManager.playableRoles.size(); i++) {
				GameRole r = roleManager.playableRoles.get(i);
				roleNames[i] = r.name;
				roleDescriptions[i] = r.description;
			}

			display.showStart(roleManager.getAllPlayers(), roleNames, roleDescriptions);
		}
		
		return changeOccurred;

	}

	public boolean removeRole(GameRole role) {
		boolean changeOccurred = state == GameState.NEW && roleManager.removeRole(role);
		
		if(changeOccurred) {
			String[] roleNames = new String[roleManager.playableRoles.size()];
			String[] roleDescriptions = new String[roleManager.playableRoles.size()];
			for (int i = 0; i < roleManager.playableRoles.size(); i++) {
				GameRole r = roleManager.playableRoles.get(i);
				roleNames[i] = r.name;
				roleDescriptions[i] = r.description;
			}

			display.showStart(roleManager.getAllPlayers(), roleNames, roleDescriptions);
		}
		
		return changeOccurred;
	}

	public boolean useVeto(Long userID) {
		if (!vetoUsed) {
			for (GameRole role : roleManager.getRolesForPlayer(userID)) {
				if (role.id == 3) {
					vetoUsed = true;
					return true;
				}
			}
		}

		return false;
	}

	public boolean useExecute(Long userID, String nameToExecute) {
		if (!executeUsed) {
			for (GameRole role : roleManager.getRolesForPlayer(userID)) {
				if (role.id == 4) {
					executeUsed = true;
					return true;
				}
			}
		}

		return false;
	}

	public boolean useDetect(Long userID, Long userToDetect, GameRole roleToDetect) {
		if (!detectUsed) {
			for (GameRole role : roleManager.getRolesForPlayer(userID)) {
				if (role.id == 7) {
					detectUsed = true;
					return true;
				}
			}
		}

		return false;
	}

	public void stopGame() {
		display.showGameEnded();
	}

	public GameState getState() {
		return state;
	}
}

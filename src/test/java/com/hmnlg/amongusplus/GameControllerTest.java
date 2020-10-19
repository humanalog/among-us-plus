/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hmnlg.amongusplus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author maikotui
 */
@TestMethodOrder(OrderAnnotation.class)
public class GameControllerTest {
	final List<GameRole> allRoles;
	GameController<DirectStringGameDisplay> gc;
	DirectStringGameDisplay out;

	List<Long> defaultPlayerIDs;
	List<GameRole> defaultGameRoles;

	public GameControllerTest() {
		// Get the list of usable game roles
		final Yaml yaml = new Yaml();
		Map<Object, List<GameRole>> temp;
		try (InputStream in = Main.class.getClassLoader().getResourceAsStream("roles.yml")) {
			temp = yaml.load(in);
		} catch (IOException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
			allRoles = new ArrayList<>();
			return;
		}
		allRoles = temp.get("roles");
	}

	@BeforeEach
	public void setUp() {
		defaultPlayerIDs = new ArrayList<>(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L));
		defaultGameRoles = new ArrayList<>();
		defaultGameRoles.add(getRole(1));
		defaultGameRoles.add(getRole(2));

		out = new DirectStringGameDisplay();

		gc = new GameController<>(out, defaultPlayerIDs, defaultGameRoles);
	}

	private GameRole getRole(int roleID) {
		for (GameRole role : allRoles) {
			if (role.id == roleID) {
				return role;
			}
		}

		return null;
	}

	@Test
	@Order(1)
	public void successfulInitialization() {
		assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), out.playerIDs);
		for (GameRole role : defaultGameRoles) {
			assertNotEquals(-1, Arrays.binarySearch(out.roleNames, role.name));
			assertNotEquals(-1, Arrays.binarySearch(out.roleDescriptions, role.description));
		}
	}

	@Test
	public void successfulRestartFromBlankSlate() {
		gc.restartGame();
		assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), out.playerIDs);
		for (GameRole role : defaultGameRoles) {
			assertNotEquals(-1, Arrays.binarySearch(out.roleNames, role.name));
			assertNotEquals(-1, Arrays.binarySearch(out.roleDescriptions, role.description));
		}
	}

	/* ---------------- PLAYER MANAGEMENT TESTS --------------- */

	@Test
	public void addOnePlayer() {
		assertTrue(gc.addPlayer(10L));

		assertListsAreEqual(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L), out.playerIDs);
		for (GameRole role : defaultGameRoles) {
			assertNotEquals(-1, Arrays.binarySearch(out.roleNames, role.name));
			assertNotEquals(-1, Arrays.binarySearch(out.roleDescriptions, role.description));
		}
	}

	@Test
	public void addPlayerAlreadyInGame() {
		assertFalse(gc.addPlayer(1L));

		assertListsAreEqual(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), out.playerIDs);
		for (GameRole role : defaultGameRoles) {
			assertNotEquals(-1, Arrays.binarySearch(out.roleNames, role.name));
			assertNotEquals(-1, Arrays.binarySearch(out.roleDescriptions, role.description));
		}
	}

	@Test
	public void addMultiplePlayers() {
		assertTrue(gc.addPlayer(10L));
		assertTrue(gc.addPlayer(12L));
		assertTrue(gc.addPlayer(150L));

		assertListsAreEqual(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 12L, 150L), out.playerIDs);
		for (GameRole role : defaultGameRoles) {
			assertNotEquals(-1, Arrays.binarySearch(out.roleNames, role.name));
			assertNotEquals(-1, Arrays.binarySearch(out.roleDescriptions, role.description));
		}
	}

	@Test
	public void removeOnePlayer() {
		assertTrue(gc.removePlayer(4L));

		assertListsAreEqual(Arrays.asList(0L, 1L, 2L, 3L, 5L, 6L, 7L, 8L, 9L), out.playerIDs);
		for (GameRole role : defaultGameRoles) {
			assertNotEquals(-1, Arrays.binarySearch(out.roleNames, role.name));
			assertNotEquals(-1, Arrays.binarySearch(out.roleDescriptions, role.description));
		}
	}

	@Test
	public void removeOnePlayerNotInGame() {
		assertFalse(gc.removePlayer(10L));

		assertListsAreEqual(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), out.playerIDs);
		for (GameRole role : defaultGameRoles) {
			assertNotEquals(-1, Arrays.binarySearch(out.roleNames, role.name));
			assertNotEquals(-1, Arrays.binarySearch(out.roleDescriptions, role.description));
		}
	}

	@Test
	public void removeMultiplePlayers() {
		assertTrue(gc.removePlayer(4L));
		assertTrue(gc.removePlayer(7L));
		assertTrue(gc.removePlayer(3L));
		assertTrue(gc.removePlayer(9L));

		assertListsAreEqual(Arrays.asList(0L, 1L, 2L, 5L, 6L, 8L), out.playerIDs);
		for (GameRole role : defaultGameRoles) {
			assertNotEquals(-1, Arrays.binarySearch(out.roleNames, role.name));
			assertNotEquals(-1, Arrays.binarySearch(out.roleDescriptions, role.description));
		}
	}

	@Test
	public void addAndRemovePlayers() {
		assertTrue(gc.addPlayer(10L));
		assertTrue(gc.removePlayer(4L));
		assertTrue(gc.removePlayer(7L));
		assertTrue(gc.addPlayer(150L));
		assertTrue(gc.removePlayer(3L));
		assertTrue(gc.removePlayer(9L));
		assertTrue(gc.addPlayer(12L));
		assertTrue(gc.addPlayer(18L));
		assertTrue(gc.addPlayer(4L));

		// TODO: Make a better list equals test
		assertListsAreEqual(Arrays.asList(0L, 1L, 2L, 4L, 5L, 6L, 8L, 10L, 12L, 18L, 150L), out.playerIDs);
		for (GameRole role : defaultGameRoles) {
			assertNotEquals(-1, Arrays.binarySearch(out.roleNames, role.name));
			assertNotEquals(-1, Arrays.binarySearch(out.roleDescriptions, role.description));
		}
	}
	
	/* ------------ HELPER METHODS -------------- */

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void assertListsAreEqual(List a, List b) {
		Collections.sort(a);
		Collections.sort(b);
		assertEquals(a, b);
	}
}

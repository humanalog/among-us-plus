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
    GameController gc;
    DirectStringGameDisplay out;

    List<Long> defaultPlayerIDs;
    List<GameRole> defaultGameRoles;

    public GameControllerTest() {
        // Get the list of usable game roles
        final Yaml yaml = new Yaml();
        Map<Object, List<GameRole>> temp;
        try ( InputStream in = Main.class.getClassLoader().getResourceAsStream("roles.yml")) {
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
        System.out.println("TEST");
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
    public void testInitialization() {
        assertEquals(out.playerIDs, defaultPlayerIDs);
        for (GameRole role : defaultGameRoles) {
            assertNotEquals(-1, Arrays.binarySearch(out.roleNames, role.name));
            assertNotEquals(-1, Arrays.binarySearch(out.roleDescriptions, role.description));
        }
    }
}

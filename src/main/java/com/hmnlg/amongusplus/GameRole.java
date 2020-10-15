/*
 * Copyright (C) 2020 maikotui
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.hmnlg.amongusplus;

/**
 * Holds information for available an extra Among Us role. Found in roles.yml.
 *
 * @author maikotui
 */
public class GameRole {

    public int id;
    public String name;
    public String[] aliases;
    public String description;
    public boolean isDefault;
    public String assignmentMessage;
    public int[] unstackableRoleIds;

    /**
     * Intentionally empty
     */
    public GameRole() {
    
    }

    /**
     * Makes toString more easily readable
     * @return 
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Checks if the object o has the same ID as this one.
     * @param o
     * @return 
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof GameRole)) {
            return false;
        }

        GameRole role = (GameRole) o;
        return role.id == this.id;
    }
    
    /**
     * Hash code is built using the role ID.
     * @return 
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + this.id;
        return hash;
    }
}

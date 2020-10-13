/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
     * Empty initializer
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
     * Hashcode is built using the role ID.
     * @return 
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + this.id;
        return hash;
    }
}

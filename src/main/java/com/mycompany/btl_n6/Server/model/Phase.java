package com.mycompany.btl_n6.Server.model;

/**
 * Minimal Phase placeholder used by GameRoom. Extend later with real fields.
 */
public class Phase {
    private String name;

    public Phase() { this.name = "default"; }

    public Phase(String name) { this.name = name; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

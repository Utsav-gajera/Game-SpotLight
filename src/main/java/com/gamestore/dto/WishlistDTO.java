package com.gamestore.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class WishlistDTO {
    private String username;
    private String name;
    private Set<String> gameTitles;




    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getGameTitles() {
        return gameTitles;
    }

    public void setGameTitles(Set<String> gameTitles) {
        this.gameTitles = gameTitles;
    }
}

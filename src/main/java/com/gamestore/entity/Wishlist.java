package com.gamestore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
@Entity
// Unique constraint to prevent two wishlists with same name for same user
@Table(name = "wishlist", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "name"})
})
public class Wishlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-wishlist")
    private User user;

    /*
      ManyToMany to Game:
      - Do NOT use CascadeType.REMOVE here — a wishlist should never delete a game entity.
      - We could allow PERSIST/MERGE if you sometimes create new Game through wishlist (usually not needed).
    */
    @ManyToMany
    @JoinTable(
            name = "wishlist_games",
            joinColumns = @JoinColumn(name = "wishlist_id"),
            inverseJoinColumns = @JoinColumn(name = "game_id")
    )
    private Set<Game> games = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Game> getGames() {
        return games;
    }

    public void setGames(Set<Game> games) {
        this.games = games;
    }
}

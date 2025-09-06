package com.gamestore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // If a User is deleted -> all Wishlists are deleted too (cascade + orphanRemoval)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("user-wishlist")
    private Set<Wishlist> wishlist = new HashSet<>();

    // If a User is deleted -> all Purchases are deleted too
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("user-purchase")
    private Set<Purchase> purchases = new HashSet<>();

    // Developer -> Games mapping. If developer (user) is deleted -> their games deleted.
    @OneToMany(mappedBy = "developer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("user-games")
    private Set<Game> games = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Set<Wishlist> getWishlist() {
        return wishlist;
    }

    public void setWishlist(Set<Wishlist> wishlist) {
        this.wishlist = wishlist;
    }

    public Set<Purchase> getPurchases() {
        return purchases;
    }

    public void setPurchases(Set<Purchase> purchases) {
        this.purchases = purchases;
    }

    public Set<Game> getGames() {
        return games;
    }

    public void setGames(Set<Game> games) {
        this.games = games;
    }
}

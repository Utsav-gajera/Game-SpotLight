package com.gamestore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(nullable = false)
    private String genre;

    @Column(nullable = false)
    private double price;

    // Developer (User) owning this game.
    // When a developer (User) is removed, cascade remove their games (because of User.games mapping).
    @ManyToOne
    @JoinColumn(name = "developer_id", nullable = false)
    @JsonBackReference("user-games")
    private User developer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public User getDeveloper() {
        return developer;
    }

    public void setDeveloper(User developer) {
        this.developer = developer;
    }
}

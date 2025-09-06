package com.gamestore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // When a user is deleted -> purchases are deleted (handled by User.purchases cascade/orphanRemoval)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-purchase")
    private User user;

    /*
      If a Game is deleted:
      - We keep the Purchase row but set game_id = NULL so purchase history remains.
      - @OnDelete(action = OnDeleteAction.SET_NULL) is a Hibernate-level instruction that
        adds "ON DELETE SET NULL" to the FK in the DB (if the DB supports it).
      - Also make the FK nullable in JPA mapping.
    */
    @ManyToOne
    @JoinColumn(name = "game_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JsonBackReference // not pairing a ManagedReference in Game for this relation
    private Game game;

    @Column(nullable = false)
    private LocalDateTime purchaseDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
}

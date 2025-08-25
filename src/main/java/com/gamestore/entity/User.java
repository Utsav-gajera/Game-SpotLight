	package com.gamestore.entity;
	
	import jakarta.persistence.*;
	
	import lombok.Getter;
	import lombok.Setter;
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
	
	    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	    @JsonManagedReference
	    private Set<Wishlist> wishlist;
	
	    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	    @JsonManagedReference
	    private Set<Purchase> purchases;
	
	    public void setPassword(String password) {
	        this.password = password;
	        
	    }
	
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
	
		public String getPassword() {
			return password;
		}
	    
	}

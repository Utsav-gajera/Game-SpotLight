package com.gamestore.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameDTO {
    private String title;
    private String genre;
    private double price;
    private String developerUsername;


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
	public String getDeveloperUsername() {
		return developerUsername;
	}
	public void setDeveloperUsername(String developerUsername) {
		this.developerUsername = developerUsername;
	}
    
    
}

package com.github.Namrufus.Henweigh;

public class Terrain {
	// the speed modifier for a bit of terrain is 
	// m = a * E^e where E is the net Encumbrance score of a player
	private double a;
	private double e;
	
	public Terrain(double a, double e) {
		this.a = a;
		this.e = e;
	}
	
	public double speedModifier(double encumbrance) {
		return a * Math.pow(encumbrance, e);
	}
}

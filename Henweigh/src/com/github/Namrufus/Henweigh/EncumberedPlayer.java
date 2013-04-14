package com.github.Namrufus.Henweigh;

public class EncumberedPlayer {
	double encumbrance;
	double targetEncumbrance;
	
	public EncumberedPlayer(double encumbrance) {
		this.encumbrance = encumbrance;
		targetEncumbrance = encumbrance;
	}
	
	public void setTargetEncumbrance(double targetEncumbrance) {
		this.targetEncumbrance = targetEncumbrance;
	}
	
	public double getEncumbrance() {
		return targetEncumbrance;
	}
}

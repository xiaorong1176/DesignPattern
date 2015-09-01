package com.echo.pattern.cor.handler;

public class VicePresident extends PriceHandler {

	@Override
	public void processDiscount(float discount) {
		if(discount <= 0.5){
			System.out.format("%sÅú×¼ÁËÕÛ¿Û£º%.2f%n", this.getClass().getName(), discount);
		}else{
			successor.processDiscount(discount);
		}

	}

}

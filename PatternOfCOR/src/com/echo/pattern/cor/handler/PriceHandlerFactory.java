package com.echo.pattern.cor.handler;

public class PriceHandlerFactory {

	/*
	  * 创建PriceHandler的工厂方法
	  */
	 public static PriceHandler createPriceHandler(){
		 
		 PriceHandler sales= new Sales();
		 PriceHandler man = new Manager();
		 PriceHandler dir = new Director();
		 PriceHandler vp = new VicePresident();
		 PriceHandler ceo = new CEO();
		 
		 sales.setSuccessor(man);
		 man.setSuccessor(dir);
		 dir.setSuccessor(vp);
		 vp.setSuccessor(ceo);
		 	 
		 return sales;
	 }

}

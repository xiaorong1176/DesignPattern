package com.echo.singleton;

public class SingletonThree {
	private SingletonThree(){}
	private static SingletonThree instance = null;
	private static SingletonThree getInstance(){
		if(instance == null){
			synchronized (SingletonThree.class) {
				if(instance == null){
					instance = new SingletonThree();
				}
			}
		}
		return instance;
	}

}

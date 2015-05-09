package com.echo.singleton;

public class SingletonTwo {
	private SingletonTwo() {
		System.out.println("懒汉模式中的构造函数");
	}
	private static SingletonTwo instance = null;
	public static synchronized SingletonTwo newInstance(){
		if(instance ==  null){
			instance = new SingletonTwo();
		}
		return instance;
	}

}

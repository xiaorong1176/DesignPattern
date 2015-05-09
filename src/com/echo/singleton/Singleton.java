package com.echo.singleton;

public class Singleton {
	private Singleton(){
		System.out.println("饿汉模式的构造函数");
	}
	private static Singleton instance = new Singleton();
	public static Singleton newInstance(){
		return instance;
	}
}

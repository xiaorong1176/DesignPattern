package com.echo.singleton;

/*
 * 同步方法 的单例模式，Lazy模式，线程安全
 * 优点：
 * 1、lazy，初次使用时实例化单例，避免资源浪费
 * 2、线程安全
 * 缺点：
 * 1、lazy，如果实例初始化非常耗时，初始使用时，可能造成性能问题
 * 2、每次调用getInstance()都要获得同步锁，性能消耗。
 */
public class SingletonTwo {
	private SingletonTwo(){}
	private static SingletonTwo instance= null;
	public static synchronized SingletonTwo getInstance(){
		if(instance == null)
			instance = new SingletonTwo();
		return instance;
	}
}

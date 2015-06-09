package com.echo.singleton;

/*
 * 基础的单例模式，Lazy模式，非线程安全
 * 优点：lazy，初次使用时实例化单例，避免资源浪费
 * 缺点：1、lazy，如果实例初始化非常耗时，初始使用时，可能造成性能问题
 *     2、非线程安全。多线程下可能会有多个实例被初始化。
 */
public class SingletonOne {
	
	//私有化构造方法，保证外部类不能通过构造器来访问
	private SingletonOne() {
		System.out.println("懒汉模式中的构造函数");
	}
	
	//单例实例变量
	private static SingletonOne instance = null;
	
	//获取单例对象实例
	public static SingletonOne newInstance(){
		if(instance ==  null){
			instance = new SingletonOne();
		}
		return instance;
	}
}

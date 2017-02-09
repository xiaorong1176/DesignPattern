#java单例模式详解

## 单例模式一

>原文地址：http://www.cnblogs.com/coffee/archive/2011/12/05/inside-java-singleton.html

关于单例模式的文章，其实网上早就已经泛滥了。但一个小小的单例，里面却是有着许多的变化。网上的文章大多也是提到了其中的一个或几个点，很少有比较全面且脉络清晰的文章，于是，我便萌生了写这篇文章的念头。企图把这个单例说透，说深入。但愿我不会做的太差。

首先来看一下简单的实现

``` java
/**
 * 基础的单例模式，Lazy模式，非线程安全
 * 优点：lazy，初次使用时实例化单例，避免资源浪费
 * 缺点：1、lazy，如果实例初始化非常耗时，初始使用时，可能造成性能问题
 * 2、非线程安全。多线程下可能会有多个实例被初始化。
 * 
 * @author laichendong
 * @since 2011-12-5
 */
public class SingletonOne {
    
    /** 单例实例变量 */
    private static SingletonOne instance = null;
    
    /**
     * 私有化的构造方法，保证外部的类不能通过构造器来实例化。
17*/
    private SingletonOne() {
        
    }
    
    /**
     * 获取单例对象实例
     * 
     * @return 单例对象
26*/
    public static SingletonOne getInstance() {
        if (instance == null) { // 1
            instance = new SingletonOne(); // 2
        }
        return instance;
    }    
}
```

注释中已经有简单的分析了。接下来分析一下关于“非线程安全”的部分。
>　　1、当线程A进入到第28行（#1）时，检查instance是否为空，此时是空的。

>　　2、此时，线程B也进入到28行（#1）。切换到线程B执行。同样检查instance为空，于是往下执行29行（#2），创建了一个实例。接着返回了。

>　　3、在切换回线程A，由于之前检查到instance为空。所以也会执行29行（#2）创建实例。返回。

>　　4、至此，已经有两个实例被创建了，这不是我们所希望的。 

### 怎么解决线程安全问题？

#### 方法一：同步方法。即在getInstance()方法上加上synchronized关键字。这时单例变成了

使用同步方法的单例
``` java
/**
 * copyright © sf-express Inc
 */
package com.something.singleton;

/**
 * 同步方法 的单例模式，Lazy模式，线程安全
 * 优点：
 * 1、lazy，初次使用时实例化单例，避免资源浪费
 * 2、线程安全
 * 缺点：
 * 1、lazy，如果实例初始化非常耗时，初始使用时，可能造成性能问题
 * 2、每次调用getInstance()都要获得同步锁，性能消耗。
 * 
 * @author laichendong
 * @since 2011-12-5
 */
public class SingletonTwo {
    
    /** 单例实例变量 */
    private static SingletonTwo instance = null;
    
    /**
     * 私有化的构造方法，保证外部的类不能通过构造器来实例化。
*/
    private SingletonTwo() {
        
    }
    
    /**
     * 获取单例对象实例
     * 同步方法，实现线程互斥访问，保证线程安全。
     * @return 单例对象
*/
    public static synchronized SingletonTwo getInstance() {
        if (instance == null) { // 1
            instance = new SingletonTwo(); // 2
        }
        return instance;
    }    
}
```
加上synchronized后确实实现了线程的互斥访问getInstance()方法。从而保证了线程安全。但是这样就完美了么？我们看。其实在典型实现里，会导致问题的只是当instance还没有被实例化的时候，多个线程访问#1的代码才会导致问题。而当instance已经实例化完成后。每次调用getInstance()，其实都是直接返回的。即使是多个线程访问，也不会出问题。但给方法加上synchronized后。所有getInstance()的调用都要同步了。其实我们只是在第一次调用的时候要同步。而同步需要消耗性能。这就是问题。

#### 方法二：双重检查加锁Double-checked locking。

其实经过分析发现，我们只要保证 instance = new SingletonOne(); 是线程互斥访问的就可以保证线程安全了。那把同步方法加以改造，只用synchronized块包裹这一句。就得到了下面的代码：

``` java
public static SingletonThree getInstance() {
        if (instance == null) { // 1
            synchronized (SingletonThree.class) {
                instance = new SingletonThree(); // 2
            }
        }
        return instance;
    }
```
    
这个方法可行么？分析一下发现是不行的！
>　1、线程A和线程B同时进入//1的位置。这时instance是为空的。

>　2、线程A进入synchronized块，创建实例，线程B等待。

>　3、线程A返回，线程B继续进入synchronized块，创建实例。。。

>　4、这时已经有两个实例创建了。 

为了解决这个问题。我们需要在//2的之前，再加上一次检查instance是否被实例化。（双重检查加锁）接下来，代码变成了这样：
``` java
public static SingletonThree getInstance() {
        if (instance == null) { // 1
            synchronized (SingletonThree.class) {
                if (instance == null) { 
                    instance = new SingletonThree(); // 2
                }
            }
        }
        return instance;
    }
```
这样，当线程A返回，线程B进入synchronized块后，会先检查一下instance实例是否被创建，这时实例已经被线程A创建过了。所以线程B不会再创建实例，而是直接返回。貌似！到此为止，这个问题已经被我们完美的解决了。遗憾的是，事实完全不是这样！这个方法在单核和 多核的cpu下都不能保证很好的工作。导致这个方法失败的原因是当前java平台的内存模型。java平台内存模型中有一个叫“无序写”（out-of-order writes）的机制。正是这个机制导致了双重检查加锁方法的失效。这个问题的关键在上面代码上的第5行：instance = new SingletonThree(); 这行其实做了两个事情：1、调用构造方法，创建了一个实例。2、把这个实例赋值给instance这个实例变量。可问题就是，这两步jvm是不保证顺序的。也就是说。可能在调用构造方法之前，instance已经被设置为非空了。

下面我们看一下出问题的过程：
>　 1、线程A进入getInstance()方法。

>　　2、因为此时instance为空，所以线程A进入synchronized块。

>　　3、线程A执行 instance = new SingletonThree(); 把实例变量instance设置成了非空。（注意，是在调用构造方法之前。）

>　　4、线程A退出，线程B进入。

>　　5、线程B检查instance是否为空，此时不为空（第三步的时候被线程A设置成了非空）。线程B返回instance的引用。（问题出现了，这时instance的引用并不是SingletonThree的实例，因为没有调用构造方法。） 

>　　6、线程B退出，线程A进入。

>　　7、线程A继续调用构造方法，完成instance的初始化，再返回。 

好吧，继续努力，解决由“无序写”带来的问题。

``` java
public static SingletonThree getInstance() {
        if (instance == null) { 
            synchronized (SingletonThree.class) {           // 1
                SingletonThree temp = instance;             // 2
                if (temp == null) {
                    synchronized (SingletonThree.class) {   // 3
                        temp = new SingletonThree();        // 4
                    }
                    instance = temp;                        // 5
                }
            }
        }
        return instance;
    }
```
解释一下执行步骤。
> 　1、线程A进入getInstance()方法。

>　　2、因为instance是空的 ，所以线程A进入位置//1的第一个synchronized块。

>　　3、线程A执行位置//2的代码，把instance赋值给本地变量temp。instance为空，所以temp也为空。 

>　　4、因为temp为空，所以线程A进入位置//3的第二个synchronized块。

>　　5、线程A执行位置//4的代码，把temp设置成非空，但还没有调用构造方法！（“无序写”问题） 

>　　6、线程A阻塞，线程B进入getInstance()方法。

>　　7、因为instance为空，所以线程B试图进入第一个synchronized块。但由于线程A已经在里面了。所以无法进入。线程B阻塞。

>　　8、线程A激活，继续执行位置//4的代码。调用构造方法。生成实例。

>　　9、将temp的实例引用赋值给instance。退出两个synchronized块。返回实例。

>　　10、线程B激活，进入第一个synchronized块。

>　　11、线程B执行位置//2的代码，把instance实例赋值给temp本地变量。

>　　12、线程B判断本地变量temp不为空，所以跳过if块。返回instance实例。

　　好吧，问题终于解决了，线程安全了。但是我们的代码由最初的3行代码变成了现在的一大坨~。于是又有了下面的方法。

#### 方法三：预先初始化static变量。

``` java
/**
 * 预先初始化static变量 的单例模式  非Lazy  线程安全
 * 优点：
 * 1、线程安全 
 * 缺点：
 * 1、非懒加载，如果构造的单例很大，构造完又迟迟不使用，会导致资源浪费。
 * 
 * @author laichendong
 * @since 2011-12-5
 */
public class SingletonFour {
    
    /** 单例变量 ,static的，在类加载时进行初始化一次，保证线程安全 */
    private static SingletonFour instance = new SingletonFour();
    
    /**
     * 私有化的构造方法，保证外部的类不能通过构造器来实例化。
     */
    private SingletonFour() {
        
    }
    
    /**
     * 获取单例对象实例
     * 
     * @return 单例对象
     */
    public static SingletonFour getInstance() {
        return instance;
    }
    
}
```
看到这个方法，世界又变得清净了。由于java的机制，static的成员变量只在类加载的时候初始化一次，且类加载是线程安全的。所以这个方法实现的单例是线程安全的。但是这个方法却牺牲了Lazy的特性。单例类加载的时候就实例化了。如注释所述：非懒加载，如果构造的单例很大，构造完又迟迟不使用，会导致资源浪费。

那到底有没有完美的办法？懒加载，线程安全，代码简单。

#### 方法四：使用内部类。

``` java
/**
 * 基于内部类的单例模式  Lazy  线程安全
 * 优点：
 * 1、线程安全
 * 2、lazy
 * 缺点：
 * 1、待发现
 * 
 * @author laichendong
 * @since 2011-12-5
 */
public class SingletonFive {
    
    /**
     * 内部类，用于实现lzay机制
*/
    private static class SingletonHolder{
        /** 单例变量  */
        private static SingletonFive instance = new SingletonFive();
    }
    
    /**
     * 私有化的构造方法，保证外部的类不能通过构造器来实例化。
*/
    private SingletonFive() {
        
    }
    
    /**
     * 获取单例对象实例
     * 
     * @return 单例对象
*/
    public static SingletonFive getInstance() {
        return SingletonHolder.instance;
    }
    
}
```

解释一下，因为java机制规定，内部类SingletonHolder只有在getInstance()方法第一次调用的时候才会被加载（实现了lazy），而且其加载过程是线程安全的（实现线程安全）。内部类加载的时候实例化一次instance。

　　**最后，总结一下：**
> 　1、如果单例对象不大，允许非懒加载，可以使用方法三。

>　　2、如果需要懒加载，且允许一部分性能损耗，可以使用方法一。（官方说目前高版本的synchronized已经比较快了）

>　　3、如果需要懒加载，且不怕麻烦，可以使用方法二。

>　　4、如果需要懒加载，没有且！推荐使用方法四。 

## 单例模式二

原文地址：http://www.jfox.info/java-dan-li-mo-shi-de-ji-zhong-xie-fa

### 第一种（懒汉，线程不安全）：

``` java
  public class Singleton {  
      private static Singleton instance;  
      private Singleton (){}   
      public static Singleton getInstance() {  
      if (instance == null) {  
          instance = new Singleton();  
      }  
      return instance;  
      }  
 }  
```

这种写法lazy loading很明显，但是致命的是在多线程不能正常工作。

### 第二种（懒汉，线程安全）：
``` java
 1 public class Singleton {  
 2     private static Singleton instance;  
 3     private Singleton (){}
 4     public static synchronized Singleton getInstance() {  
 5     if (instance == null) {  
 6         instance = new Singleton();  
 7     }  
 8     return instance;  
 9     }  
10 }  
11
```
这种写法能够在多线程中很好的工作，而且看起来它也具备很好的lazy loading，但是，遗憾的是，效率很低，99%情况下不需要同步。

### 第三种（饿汉）：
``` java
1 public class Singleton {  
2     private static Singleton instance = new Singleton();  
3     private Singleton (){}
4     public static Singleton getInstance() {  
5     return instance;  
6     }  
7 }  
8
```
这种方式基于classloder机制避免了多线程的同步问题，不过，instance在类装载时就实例化，虽然导致类装载的原因有很多种，在单例模式中大多数都是调用getInstance方法， 但是也不能确定有其他的方式（或者其他的静态方法）导致类装载，这时候初始化instance显然没有达到lazy loading的效果。

### 第四种（饿汉，变种）：

``` java
 1 public class Singleton {  
 2     private Singleton instance = null;  
 3     static {  
 4     instance = new Singleton();  
 5     }  
 6     private Singleton (){}
 7     public static Singleton getInstance() {  
 8     return this.instance;  
 9     }  
10 }  
11
```
表面上看起来差别挺大，其实更第三种方式差不多，都是在类初始化即实例化instance。
### 第五种（静态内部类）：
``` java
 1 public class Singleton {  
 2     private static class SingletonHolder {  
 3     private static final Singleton INSTANCE = new Singleton();  
 4     }  
 5     private Singleton (){}
 6     public static final Singleton getInstance() {  
 7         return SingletonHolder.INSTANCE;  
 8     }  
 9 }  
10
```
这种方式同样利用了classloder的机制来保证初始化instance时只有一个线程，它跟第三种和第四种方式不同的是（很细微的差别）：第三种和第四种方式是只要Singleton类被装载了，那么instance就会被实例化（没有达到lazy loading效果），而这种方式是Singleton类被装载了，instance不一定被初始化。因为SingletonHolder类没有被主动使用，只有显示通过调用getInstance方法时，才会显示装载SingletonHolder类，从而实例化instance。想象一下，如果实例化instance很消耗资源，我想让他延迟加载，另外一方面，我不希望在Singleton类加载时就实例化，因为我不能确保Singleton类还可能在其他的地方被主动使用从而被加载，那么这个时候实例化instance显然是不合适的。这个时候，这种方式相比第三和第四种方式就显得很合理。
### 第六种（枚举）：
``` java
1 public enum Singleton {  
2     INSTANCE;  
3     public void whateverMethod() {  
4     }  
5 }  
6
```
这种方式是Effective Java作者Josh Bloch 提倡的方式，它不仅能避免多线程同步问题，而且还能防止反序列化重新创建新的对象，可谓是很坚强的壁垒啊，不过，个人认为由于1.5中才加入enum特性，用这种方式写不免让人感觉生疏，在实际工作中，我也很少看见有人这么写过。
 
### 第七种（双重校验锁）：
``` java
 1 public class Singleton {  
 2     private volatile static Singleton singleton;  
 3     private Singleton (){}   
 4     public static Singleton getSingleton() {  
 5     if (singleton == null) {  
 6         synchronized (Singleton.class) {  
 7         if (singleton == null) {  
 8             singleton = new Singleton();  
 9         }  
10         }  
11     }  
12     return singleton;  
13     }  
14 }  
15
```
这个是第二种方式的升级版，俗称双重检查锁定，详细介绍请查看：http://www.ibm.com/developerworks/cn/java/j-dcl.html
在JDK1.5之后，双重检查锁定才能够正常达到单例效果。
**总结**
有两个问题需要注意

> 1、如果单例由不同的类装载器装入，那便有可能存在多个单例类的实例。假定不是远端存取，例如一些servlet容器对每个servlet使用完全不同的类  装载器，这样的话如果有两个servlet访问一个单例类，它们就都会有各自的实例。

>  2、如果Singleton实现了java.io.Serializable接口，那么这个类的实例就可能被序列化和复原。不管怎样，如果你序列化一个单例类的对象，接下来复原多个那个对象，那你就会有多个单例类的实例。

对第一个问题修复的办法是：
``` java
 1 private static Class getClass(String classname)      
 2                throws ClassNotFoundException {     
 3       ClassLoader classLoader = Thread.currentThread().getContextClassLoader();     
 4       
 5       if(classLoader == null)     
 6          classLoader = Singleton.class.getClassLoader();     
 7       
 8       return (classLoader.loadClass(classname));     
 9    }     
10 }  
11
```
 
 对第二个问题修复的办法是：
``` java
 1 public class Singleton implements java.io.Serializable {     
 2    public static Singleton INSTANCE = new Singleton();     
 3       
 4    protected Singleton() {     
 5         
 6    }     
 7    private Object readResolve() {     
 8             return INSTANCE;     
 9       }    
10 }   
11
```
对我来说，我比较喜欢第三种和第五种方式，简单易懂，而且在JVM层实现了线程安全（如果不是多个类加载器环境），一般的情况下，我会使用第三种方式，只有在要明确实现lazy loading效果时才会使用第五种方式，另外，如果涉及到反序列化创建对象时我会试着使用枚举的方式来实现单例，不过，我一直会保证我的程序是线程安全的，而且我永远不会使用第一种和第二种方式，如果有其他特殊的需求，我可能会使用第七种方式，毕竟，JDK1.5已经没有双重检查锁定的问题了。

## 单例模式三

原文地址：http://blog.csdn.net/baimy1985/article/details/8553788

设计模式相关知识在面试中经常被问到，其中的单例模式几乎是每次必问，同时还会要求手写单例模式的代码。至于为什么也不难理解，它的实现代码简短，用较短的时间就能完成，同时代码中也不乏一些细节可以考察面试者的基本功。简单啰嗦一下单例模式的基本知识，借用下网络搜索的结果：
      
概念上可理解成一个类只有一个实例，实现上是要注意以下三点：
- 单例模式的类只提供私有的构造函数，
- 类定义中含有一个该类的静态私有对象，
- 该类提供了一个静态公有函数用于创建或获取它的静态私有对象
 
理解好上面这几句话，即使面试的时候一时忘代码怎么写，也可以根据原理重新写出来的，理解原理要比记住代码更重要。用自己的话去描述给面试官听相信效果也是不错的，当然作为最后一关手写代码也很关键，在面试略有压力的情况，敢不敢保证写下的代码没有编译错误，真机测试一次通过。如果可以，那恭喜你面试暂时上岸，面试官要换题了。下面还是看两段代码吧。

``` java
//懒汉式  
public class SingletonA {  
     public static SingletonA instance = null;  
      
     private SingletonA(){     }  
      
     public static SingletonA getSingletonA(){           
          if(instance == null){  
               instance = new SingletonA();  
          }  
          return instance;  
     }      
}  
```
  
//饿汉式  

``` java
public class SingletonB {  
     private static SingletonB instanceB = new SingletonB();  
      
     private SingletonB(){}  
      
     public static synchronized SingletonB getInstance(){  
          return instanceB;  
     }  
}  
```

``` java
//双重锁定  
public class SingletonC {  
     private static SingletonC instance = null;  
     private SingletonC(){};  
      
     public static SingletonC getInstance(){  
          if(instance == null){  
               synchronized (SingletonC.class) {  
                    if(null == instance){  
                         instance = new SingletonC();  
                    }  
               }  
          }  
          return instance;  
     }  
}  
```
写了这么多，感觉是不是有点孔乙已了。好吧，其实面试官只是想知道下面几点
- 面试者是否了解单例，
- 是否知道懒汉式和饿汉式的区别，
- 是不是还知道有双重锁定这么一回事，
- 是否会注意到饿汉式需要同步操作才好。

实际的面试过程中一般不会让你三种都写出来，前两种比较常考察，同时有经验的面试官还会在写代码过程留意你的表现，借一斑以窥全豹，顺便看看有没有可以引出接下来问题的考察点。不过最重要还是对概念理解程度的考察，至于代码中的一些小错误，比如命名是否规范这种类似错误是不会过多纠缠的。

就写这些，虽说一个挺简单的面试题，不过可以问的东西还是有一些的，文中提到一些个人觉得应该注意的地方不知道是否还有遗漏呢，如果有留言告诉我，非常感谢。

## 单例模式四

原文地址：http://nanjingjiangbiao-t.iteye.com/blog/1794520
**1) 哪些类是单例模式的后续类？在Java中哪些类会成为单例？**

这里它们将检查面试者是否有对使用单例模式有足够的使用经验。他是否熟悉单例模式的优点和缺点。

**2)你能在Java中编写单例里的getInstance()的代码？**

很多面试者都在这里失败。然而如果不能编写出这个代码，那么后续的很多问题都不能被提及。

**3)在getInstance()方法上同步有优势还是仅同步必要的块更优优势？你更喜欢哪个方式？**

这确实是一个非常好的问题，我几乎每次都会提该问题，用于检查面试者是否会考虑由于锁定带来的性能开销。因为锁定仅仅在创建实例时才有意义，然后其他时候实例仅仅是只读访问的，因此只同步必要的块的性能更优，并且是更好的选择。

**4)什么是单例模式的延迟加载或早期加载？你如何实现它？**

这是和Java中类加载的载入和性能开销的理解的又一个非常好的问题。我面试过的大部分面试者对此并不熟悉，但是最好理解这个概念。

**5) Java平台中的单例模式的实例有哪些？**

这是个完全开放的问题，如果你了解JDK中的单例类，请共享给我。

**6) 单例模式的两次检查锁是什么？**

**7)你如何阻止使用clone()方法创建单例实例的另一个实例？**

该类型问题有时候会通过如何破坏单例或什么时候Java中的单例模式不是单例来被问及。

**8)如果阻止通过使用反射来创建单例类的另一个实例？**

开放的问题。在我的理解中，从构造方法中抛出异常可能是一个选项。

**9)如果阻止通过使用序列化来创建单例类的另一个实例？**

又一个非常好的问题，这需要Java中的序列化知识并需要理解如何使用它来序列化单例类。该问题是开放问题。

**10) Java中的单例模式什么时候是非单例？**

## 单例模式五

**静态类和单例模式区别**
原文链接：http://sooxin.iteye.com/blog/796987
### 观点一：（单例）
单例模式比静态方法有很多优势：
首先，单例可以继承类，实现接口，而静态类不能（可以集成类，但不能集成实例成员）；
其次，单例可以被延迟初始化，静态类一般在第一次加载是初始化；
再次，单例类可以被继承，他的方法可以被覆写；
最后，或许最重要的是，单例类可以被用于多态而无需强迫用户只假定唯一的实例。举个例子，你可能在开始时只写一个配置，但是以后你可能需要支持超过一个配置集，或者可能需要允许用户从外部从外部文件中加载一个配置对象，或者编写自己的。你的代码不需要关注全局的状态，因此你的代码会更加灵活。

### 观点二：（静态方法）
静态方法中产生的对象，会随着静态方法执行完毕而释放掉，而且执行类中的静态方法时，不会实例化静态方法所在的类。如果是用singleton, 产生的那一个唯一的实例，会一直在内存中，不会被GC清除的(原因是静态的属性变量不会被GC清除)，除非整个JVM退出了。这个问题我之前也想几天，并且自己写代码来做了个实验。

### 观点三：（Good！）
由于DAO的初始化，会比较占系统资源的，如果用静态方法来取，会不断地初始化和释放，所以我个人认为如果不存在比较复杂的事务管理，用singleton会比较好。个人意见，欢迎各位高手指正。 
  
>总结：大家对这个问题都有一个共识：那就是实例化方法更多被使用和稳妥，静态方法少使用。

有时候我们对静态方法和实例化方法会有一些误解。

**1、大家都以为“ 静态方法常驻内存，实例方法不是，所以静态方法效率高但占内存。”**

事实上，他们都是一样的，在加载时机和占用内存上，静态方法和实例方法是一样的，在类型第一次被使用时加载。调用的速度基本上没有差别。

**2、大家都以为“ 静态方法在堆上分配内存，实例方法在堆栈上”**

事实上所有的方法都不可能在堆或者堆栈上分配内存，方法作为代码是被加载到特殊的代码内存区域，这个内存区域是不可写的。

方法占不占用更多内存，和它是不是static没什么关系。   

因为字段是用来存储每个实例对象的信息的，所以字段会占有内存，并且因为每个实例对象的状态都不一致（至少不能认为它们是一致的），所以每个实例对象的所以字段都会在内存中有一分拷贝，也因为这样你才能用它们来区分你现在操作的是哪个对象。 
  
但方法不一样，不论有多少个实例对象，它的方法的代码都是一样的，所以只要有一份代码就够了。因此无论是static还是non-static的方法，都只存在一份代码，也就是只占用一份内存空间。
   
同样的代码，为什么运行起来表现却不一样？这就依赖于方法所用的数据了。主要有两种数据来源，一种就是通过方法的参数传进来，另一种就是使用class的成员变量的值……

**3、大家都以为“实例方法需要先创建实例才可以调用，比较麻烦，静态方法不用，比较简单”**

事实上如果一个方法与他所在类的实例对象无关，那么它就应该是静态的，而不应该把它写成实例方法。所以所有的实例方法都与实例有关，既然与实例有关，那么创建实例就是必然的步骤，没有麻烦简单一说。
当然你完全可以把所有的实例方法都写成静态的，将实例作为参数传入即可，一般情况下可能不会出什么问题。

从面向对象的角度上来说，在抉择使用实例化方法或静态方法时，应该根据是否该方法和实例化对象具有逻辑上的相关性，如果是就应该使用实例化对象 反之使用静态方法。这只是从面向对象角度上来说的。

如果从线程安全、性能、兼容性上来看，也是选用实例化方法为宜。

我们为什么要把方法区分为：静态方法和实例化方法 ？

如果我们继续深入研究的话，就要脱离技术谈理论了。早期的结构化编程，几乎所有的方法都是“静态方法”，引入实例化方法概念是面向对象概念出现以后的事情了，区分静态方法和实例化方法不能单单从性能上去理解，创建c++,java,c#这样面向对象语言的大师引入实例化方法一定不是要解决什么性能、内存的问题，而是为了让开发更加模式化、面向对象化。这样说的话，静态方法和实例化方式的区分是为了解决模式的问题。

拿别人一个例子说事：
比如说“人”这个类，每个人都有姓名、年龄、性别、身高等，这些属性就应该是非静态的，因为每个人都的这些属性都不相同；但人在生物学上属于哪个门哪个纲哪个目等，这个属性是属于整个人类，所以就应该是静态的——它不依赖与某个特定的人，不会有某个人是“脊椎动物门哺乳动物纲灵长目”而某个人却是“偶蹄目”的。

## 单例模式面试题&类加载机制
``` java
class SingleTon {
	private static SingleTon singleTon = new SingleTon();
	public static int count1;
	public static int count2 = 0;


	private SingleTon() {
		count1++;
		count2++;
	}


	public static SingleTon getInstance() {
		return singleTon;
	}
}

public class Test {
	public static void main(String[] args) {
		SingleTon singleTon = SingleTon.getInstance();
		System.out.println("count1=" + singleTon.count1);
		System.out.println("count2=" + singleTon.count2);
	}
}
```
错误答案
count1=1
count2=1

 正确答案
count1=1
count2=0

> 分析:

> 1、SingleTon singleTon = SingleTon.getInstance();调用了类的SingleTon调用了类的静态方法，触发类的初始化

>2、类加载的时候在准备过程中为类的静态变量分配内存并初始化默认值 singleton=null count1=0,count2=0

>3、类初始化化，为类的静态变量赋值和执行静态代码快。singleton赋值为new SingleTon()调用类的构造方法

>4、调用类的构造方法后count1=1;count2=1

>5、继续为count1与count2赋值,此时count1没有赋值操作,所有count1为1,但是count2执行赋值操作就变为0 

package org.siphonlab.ago.study;

class Animal {}

class Dog extends Animal {}

interface Pet {}

class MyDog extends Dog implements Pet {}

class Dispatcher {
    void f(Object o) {
        System.out.println("f(Object)");
    }

    void f(Animal a) {
        System.out.println("f(Animal)");
    }

    void f(Object o, int i) {
        System.out.println("f(Object) int");
    }

    void f(Animal a, double d) {
        System.out.println("f(Animal) double");
    }

//    void f(Dog d) {
//        System.out.println("f(Dog)");
//    }
//
//    void f(Pet p) {
//        System.out.println("f(Pet)");
//    }
}

public class JavaMatchOrder {
    public static void main(String[] args) {
        Dispatcher d = new Dispatcher();

        Object obj = new MyDog();
        Animal ani = new MyDog();
        Dog dog = new MyDog();
        Pet pet = new MyDog();
        MyDog myDog = new MyDog();

        d.f(obj);   // 1
        d.f(ani);   // 2
        d.f(dog);   // 3    Animal
        d.f(pet);   // 4    Object

//        d.f(dog, 1);        //  ambiguous
        d.f(dog, 1.0);
//        d.f(obj, 1.0);    // ambiguous, double -> int not allowed

        //d.f(myDog); // 5
        /*
        Ambiguous method call. Both
        f
        (Dog)
        in Dispatcher and
        f
        (Pet)
        in Dispatcher match
        */
    }
}

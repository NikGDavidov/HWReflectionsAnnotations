package org.example.lesson2.hw;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TestProcessor {

  /**
   * Данный метод находит все void методы без аргументов в классе, и запускеет их.
   * <p>
   * Для запуска создается тестовый объект с помощью конструткора без аргументов.
   */
  public static void runTest(Class<?> testClass) {
    final Constructor<?> declaredConstructor;
    try {
      declaredConstructor = testClass.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Для класса \"" + testClass.getName() + "\" не найден конструктор без аргументов");
    }

    final Object testObj;
    try {
      testObj = declaredConstructor.newInstance();
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось создать объект класса \"" + testClass.getName() + "\"");
    }

    List<Method> methods = new ArrayList<>();
    Method beforeEach = null;
    Method afterEach = null;
    for (Method method : testClass.getDeclaredMethods()) {

      if (method.isAnnotationPresent(BeforeEach.class)) beforeEach = method;
      if (method.isAnnotationPresent(AfterEach.class)) afterEach = method;
      if (method.isAnnotationPresent(Test.class) && !method.isAnnotationPresent(Skip.class)) {
        checkTestMethod(method);
        methods.add(method);
      }
    }
    //запускаем beforeEach
    if (beforeEach!=null) {
      checkTestMethod(beforeEach);
      runTest(beforeEach,testObj);
    }
    //запускаем тестовые методы
    methods = methods.stream().sorted((m1, m2)  ->{
      Test t1= m1.getAnnotation(Test.class);
      int order1 = t1.order();
      Test t2 = m2.getAnnotation(Test.class);
      int order2 = t2.order();
      return order1-order2;
    }).collect(Collectors.toList());

    methods.forEach(it -> runTest(it, testObj));

    //запускаем afterEach
    if (afterEach!=null) {
      checkTestMethod(afterEach);
      runTest(afterEach,testObj);
    }
  }


  private static void checkTestMethod(Method method) {
    if (!method.getReturnType().isAssignableFrom(void.class) || method.getParameterCount() != 0) {
      throw new IllegalArgumentException("Метод \"" + method.getName() + "\" должен быть void и не иметь аргументов");
    }
  }

  private static void runTest(Method testMethod, Object testObj) {
    try {
      testMethod.invoke(testObj);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось запустить тестовый метод \"" + testMethod.getName() + "\"");
    } catch (AssertionError e) {

    }
  }

}

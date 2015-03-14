# Introduction #

Microsoft introduced a nice concept in VS2005. t4 a template engine allows you to generate some code, xml or any other text-based for from ASP or PHP-like language.

Java has plenty of template engines but some are over-complicated and some
just not powerful enough, or better say I was not able to find one that worked for me in my current project, so I decided to create one.

# Details #

The idea is simple you write your template file like that

```

public class MyGeneratedClass{
<%%
for(int i = 0; i < 10; i++){
%>    private int mVar<%=i %>;

      public getMvar<%=i %>(){
          return mVar<%=i %>;
      }
      public setMvar<%=i %>(int value){
          this.mVar<%=i %> = value;
      }
<%%
}
%>
}

```

Save it as template.java
And after you run
%JAVA\_HOME%/bin/java -jar template.jar template.java:MyGeneratedClass.java

You get generated code

```

public class MyGeneratedClass{

    private int mVar0;
      public getMvar0(){
          return mVar0;
      }
      public setMvar0(int value){
          this.mVar0 = value;
      }

    private int mVar1;
      public getMvar1(){
          return mVar1;
      }
      public setMvar1(int value){
          this.mVar1 = value;
      }

    private int mVar2;
      public getMvar2(){
          return mVar2;
      }
      public setMvar2(int value){
          this.mVar2 = value;
      }

    private int mVar3;
      public getMvar3(){
          return mVar3;
      }
      public setMvar3(int value){
          this.mVar3 = value;
      }

    private int mVar4;
      public getMvar4(){
          return mVar4;
      }
      public setMvar4(int value){
          this.mVar4 = value;
      }

    private int mVar5;
      public getMvar5(){
          return mVar5;
      }
      public setMvar5(int value){
          this.mVar5 = value;
      }

    private int mVar6;
      public getMvar6(){
          return mVar6;
      }
      public setMvar6(int value){
          this.mVar6 = value;
      }

    private int mVar7;
      public getMvar7(){
          return mVar7;
      }
      public setMvar7(int value){
          this.mVar7 = value;
      }

    private int mVar8;
      public getMvar8(){
          return mVar8;
      }
      public setMvar8(int value){
          this.mVar8 = value;
      }

    private int mVar9;
      public getMvar9(){
          return mVar9;
      }
      public setMvar9(int value){
          this.mVar9 = value;
      }

}

```

You can use all java framework in that template engine.

in case you need to use some extra package, you import it like that:
```
<%@ import java.util.Calendar; %>
```

to crate a class or a method you want to use in preprocessor

```
<%+
void myMethod(){ ... }
class MyClass{
}
%>

```

when you want something to be in the output

```
<%=variable %>

```

Simple as that.

Go to Downloads section or to Source to get one.
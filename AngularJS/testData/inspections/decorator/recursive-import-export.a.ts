// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.


import {MyModule1, MyComponent1} from "./recursive-import-export.b";
import {NgModule, Component} from "@angular/core";


@NgModule({
    exports: [<error descr="Cyclic dependency between:  MyModule3  ->  MyModule1  ->  MyComponent1  ->  MyModule2  ->  MyModule3 ">MyModule1</error>]
})
export class MyModule3 {

}

const THE_IMPORT = [
    MyModule3
]

@NgModule({
    imports: [
        <error descr="Cyclic dependency between:  MyModule2  ->  MyModule3  ->  MyModule1  ->  MyComponent1  ->  MyModule2 ">THE_IMPORT</error>,
        <error descr="Module  MyModule2  imports itself">MyModule2</error>
    ]
})
export class MyModule2 {

}

@Component({
  standalone: true,
  imports: [
    <error descr="Cyclic dependency between:  MyComponent2  ->  MyComponent1  ->  MyComponent2 ">MyComponent1</error>,
    <error descr="Component  MyComponent2  imports itself">MyComponent2</error>
  ]
})
export class MyComponent2 {

}

export const MY_IMPORTS = [
    MyModule2, MyComponent2
]

@NgModule({
    imports: [MyModule3],
    exports: [<error descr="Module  MyModule4  exports itself">MyModule4</error>]
})
export class MyModule4 {
}
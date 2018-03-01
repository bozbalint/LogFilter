# IndexFile-exercise

It is a Doctusoft entry exercise. 

Készíts Java programot (standalone, main függvénnyel), amely egy bemeneti könyvtárban levő fájlok sorait válogatja le egy kimeneti könyvtárba, egy konfigurációs fájlban található reguláris kifejezések szerint. A konfigurációs fájlban egy sor ad meg egy mintát és annak a nevét. Például apache access log válogatás esetén a mintafájlban “notfound: .*HTTP\s404.*” szerepel egy sorban. A kimeneti könyvtárban a minták nevei szerint gyűjtjük a sorokat, tehát pl a “notfound” nevű fájlban lesz az összes ennek megfelelő sor (az összes bemeneti fájlból egységesen).

A megoldáshoz tetszés szerint Java 7 vagy 8 is használható. Fontos, hogy a program a lehető leghamarabb végezzen a feladattal, felhasználva az összes processzormagot, és optimálisan kihasználva a diszket. Sok memória áll rendelkezésre, de a bemeneti halmaz nem korlátos, tehát nem feltételezhető, hogy az egész adatmennyiség egyszerre elfér a memóriában. A konfigurációs sorok száma 10-es nagyságrendű. A megoldáshoz kérjük, hogy csak sztenderd, beépített függvénykönyvtárakat használj.

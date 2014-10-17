<h1>Fewlaps loves Bixolon</h1>
<h2>printing things with your Android</h2>
===================

The Bixolon SDK includes a sample project to showcase what's the Bixolon printer capable of. It also showcase that Bixolon's engineer are great writing low-level code, but they can't spend the time to fulfill the reality on the Android world: things must be simple.

For example, the Bixolon sample project require that the user pairs the printer and, then, connects to it. If the printer goes down, the user needs to connect to it manually, again. That's not pretty cool for the real world...

In the way to integrate the Bixolon SDK to our projects, we created an automated way to connect to the printer. The only thing the user needs to do is to pair the phone with the printer the first time that it's going to print. When the phone and the printer are paired, the app will automagically search for the printer. If the phone finds the printer, the phone will try to connect to it.

Well, and here's and small video to explain the idea with some frames put one each another:

[![Fewlaps loves Bixolon](http://img.youtube.com/vi/V4q1c5oexzM/0.jpg)](http://www.youtube.com/watch?v=V4q1c5oexzM)

You'll also find some fancy lines in the project to print data in two columns, an image, a QR... and some tricks that you should know if printing with one of this (lovely) printers.
